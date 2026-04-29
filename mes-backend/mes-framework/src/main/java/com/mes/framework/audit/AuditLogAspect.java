package com.mes.framework.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 审计日志 AOP 切面（P1-13 等保三级合规；P3-12 差异化阈值与大 payload 分片）。
 *
 * <p>职责：自动拦截所有 @RestController 下的 POST / PUT / PATCH / DELETE 写接口，
 * 以及所有被 {@link AuditLog} 注解的方法，收集操作上下文后异步写入 sys_audit_log。</p>
 *
 * <p>切点策略：</p>
 * <ul>
 *   <li>拦截范围：{@code @within(RestController)} 的类下所有 public 方法；</li>
 *   <li>默认记录：方法上挂 POST/PUT/PATCH/DELETE 映射 或 {@link AuditLog}（record=true）；</li>
 *   <li>默认跳过：GET 映射（list/page/detail）且无 @AuditLog 的方法；</li>
 *   <li>显式关闭：方法上 {@code @AuditLog(record=false)} 始终跳过。</li>
 * </ul>
 *
 * <p>收集字段：</p>
 * <ul>
 *   <li>核心字段（走 sys_audit_log 列）：tenantId / operatorUserId / operatorUsername /
 *       action / targetType / traceId / ip / userAgent / result / errorMessage；</li>
 *   <li>扩展字段（走 payload_json）：httpMethod / uri / className / methodName /
 *       duration_ms / request / response / exception。</li>
 * </ul>
 *
 * <p>脱敏策略：</p>
 * <ul>
 *   <li>请求/响应里 JSON 字段名命中敏感关键词（password/secret/token/authorization/creditCard/pwd/apiKey）
 *       的值直接替换为 "***"，递归处理嵌套 Map；</li>
 *   <li>响应体序列化后大小 ≤ 阈值：原样进入 payload_json；</li>
 *   <li>P3-12：响应体 &gt; 阈值（注解 {@code payloadMaxSize} 优先，全局 {@code mes.audit.max-payload-bytes} 兜底）
 *       时，payload_json 里 response 字段保留首 N 字节 + {@code payloadRef=&lt;audit_log_id&gt;} 引用，
 *       原文由 {@link AuditLogService#savePayloadChunks} 分片存入 sys_audit_log_payload 子表。</li>
 * </ul>
 *
 * <p>线程安全：切面本身无状态，线程安全；AuditLogService 内部使用 @Async 线程池异步落库，
 * 业务线程零阻塞。</p>
 *
 * @author mcp24
 */
@Slf4j
@Aspect
@Component
@Order(100)
@RequiredArgsConstructor
public class AuditLogAspect {

    /**
     * 响应体最大序列化长度（字节）。
     *
     * <p>超过自动截断；可通过 {@code mes.audit.max-payload-bytes} 覆盖。</p>
     */
    private static final int DEFAULT_MAX_PAYLOAD_BYTES = 10 * 1024;

    /** 敏感字段关键词（忽略大小写、按子串匹配）。 */
    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile(
            "(?i)(password|passwd|pwd|secret|token|authorization|auth|credit[_-]?card|cardno|cvv|apikey|api[_-]?key|accesskey|sk|privatekey)"
    );

    private static final String MASK = "***";

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Value("${mes.audit.max-payload-bytes:" + DEFAULT_MAX_PAYLOAD_BYTES + "}")
    private int maxPayloadBytes;

    /**
     * 切点 1：所有 @RestController 下的方法。
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restControllerMethods() {
    }

    /**
     * 切点 2：显式挂 @AuditLog 的方法（即便不在 Controller 里也会被记录）。
     */
    @Pointcut("@annotation(com.mes.framework.audit.AuditLog)")
    public void auditLogAnnotatedMethods() {
    }

    /**
     * 主环绕通知。先执行业务方法，再根据结果写审计；业务异常也会被记录为 FAIL。
     *
     * @param pjp 切点
     * @return 业务方法原始返回值
     * @throws Throwable 业务方法抛出的异常原样向外抛
     */
    @Around("restControllerMethods() || auditLogAnnotatedMethods()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        AuditLog ann = AnnotationUtils.findAnnotation(method, AuditLog.class);

        String httpMethod = resolveHttpMethod(method);
        boolean shouldRecord = decideShouldRecord(ann, httpMethod);

        if (!shouldRecord) {
            return pjp.proceed();
        }

        long startNs = System.nanoTime();
        Object response = null;
        Throwable thrown = null;
        try {
            response = pjp.proceed();
            return response;
        } catch (Throwable t) {
            thrown = t;
            throw t;
        } finally {
            long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
            try {
                writeAuditAsync(pjp, method, ann, httpMethod, response, thrown, durationMs);
            } catch (Exception ex) {
                // 审计失败一律吞掉，绝不影响业务返回
                log.warn("[Audit] 构造审计事件失败（忽略）: method={}#{}, err={}",
                        method.getDeclaringClass().getSimpleName(), method.getName(), ex.getMessage());
            }
        }
    }

    /**
     * 决定当前方法是否需要被审计记录。
     *
     * @param ann        方法上的 @AuditLog 注解（可能为 null）
     * @param httpMethod 推断出的 HTTP 方法（POST/PUT/DELETE/PATCH/GET/null）
     * @return true=记录；false=跳过
     */
    private boolean decideShouldRecord(AuditLog ann, String httpMethod) {
        // 1. @AuditLog 显式关闭 → 永远不记录
        if (ann != null && !ann.record()) {
            return false;
        }
        // 2. @AuditLog 显式开启 → 无论 HTTP 方法都记录
        if (ann != null) {
            return true;
        }
        // 3. 没注解：仅对 POST/PUT/PATCH/DELETE 默认记录
        if (httpMethod == null) {
            return false;
        }
        return switch (httpMethod) {
            case "POST", "PUT", "PATCH", "DELETE" -> true;
            default -> false;
        };
    }

    /**
     * 从方法的 Spring MVC 映射注解推断 HTTP method。
     *
     * @param method 目标方法
     * @return HTTP method 大写形式；无法推断时返回 null
     */
    private String resolveHttpMethod(Method method) {
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (method.isAnnotationPresent(PatchMapping.class)) return "PATCH";
        if (method.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class)) return "GET";
        // @RequestMapping(method=...) 兜底
        RequestMapping rm = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        if (rm != null && rm.method().length > 0) {
            RequestMethod first = rm.method()[0];
            return first.name();
        }
        // 实在没有 → 从当前请求里取
        try {
            HttpServletRequest req = currentRequest();
            if (req != null) {
                return req.getMethod();
            }
        } catch (Exception ignore) {
            // 后台任务没有 RequestContext
        }
        return null;
    }

    /**
     * 组装审计事件并提交异步写入。
     */
    private void writeAuditAsync(ProceedingJoinPoint pjp,
                                 Method method,
                                 AuditLog ann,
                                 String httpMethod,
                                 Object response,
                                 Throwable thrown,
                                 long durationMs) {
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        // 1. 基础语义字段
        String action = (ann != null && !ann.action().isBlank())
                ? ann.action()
                : defaultAction(httpMethod, className, methodName);
        String resource = (ann != null && !ann.resource().isBlank())
                ? ann.resource()
                : defaultResource(className);

        // 2. 收集 HTTP 请求上下文（仅在 ServletRequest 可用时）
        String uri = null;
        HttpServletRequest req = currentRequest();
        if (req != null) {
            uri = req.getRequestURI();
            if (httpMethod == null) {
                httpMethod = req.getMethod();
            }
        }

        // 3. 组装 payload
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("httpMethod", httpMethod);
        payload.put("uri", uri);
        payload.put("class", className);
        payload.put("method", methodName);
        payload.put("durationMs", durationMs);

        // 3.1 差异化阈值：注解优先，否则走全局
        int effectiveMax = (ann != null && ann.payloadMaxSize() > 0)
                ? ann.payloadMaxSize()
                : maxPayloadBytes;

        String oversizedRequest = null;
        String oversizedResponse = null;

        boolean recordInput = ann == null || ann.recordInput();
        if (recordInput) {
            PreparedPayload prep = maskAndPrepare(extractArgs(method, pjp.getArgs()), effectiveMax);
            payload.put("request", prep.displayValue);
            if (prep.oversizedRaw != null) {
                oversizedRequest = prep.oversizedRaw;
            }
        }

        boolean recordOutput = ann == null || ann.recordOutput();
        if (thrown == null) {
            if (recordOutput) {
                PreparedPayload prep = maskAndPrepare(response, effectiveMax);
                payload.put("response", prep.displayValue);
                if (prep.oversizedRaw != null) {
                    oversizedResponse = prep.oversizedRaw;
                }
            }
        } else {
            payload.put("exception", thrown.getClass().getName() + ": " + thrown.getMessage());
        }

        // 4. 事件入库（异步）；tenantId / operatorUserId / ip / userAgent / traceId 由 AuditLogService.enrich() 补齐
        AuditEvent event = AuditEvent.builder()
                .action(action)
                .targetType(resource)
                .result(thrown == null ? "OK" : "FAIL")
                .errorMessage(thrown != null ? truncate(thrown.getMessage(), 1800) : null)
                .payload(payload)
                .oversizedRequest(oversizedRequest)
                .oversizedResponse(oversizedResponse)
                .build();
        auditLogService.recordAsync(event);
    }

    /**
     * 默认 action 名：HTTP_METHOD + Controller + MethodName（全大写，便于检索）。
     */
    private String defaultAction(String httpMethod, String className, String methodName) {
        String prefix = httpMethod != null ? httpMethod : "CALL";
        return (prefix + "_" + className + "_" + methodName).toUpperCase();
    }

    /**
     * 默认 resource 名：Controller 类名去掉 Controller 后缀再大写。
     */
    private String defaultResource(String className) {
        String base = className.endsWith("Controller")
                ? className.substring(0, className.length() - "Controller".length())
                : className;
        return base.toUpperCase();
    }

    /**
     * 把方法参数整理成 Map，丢弃不可序列化 / 不适合入库的类型。
     */
    private Map<String, Object> extractArgs(Method method, Object[] args) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (args == null) {
            return result;
        }
        String[] paramNames = methodParamNames(method);
        for (int i = 0; i < args.length; i++) {
            String name = (paramNames != null && i < paramNames.length) ? paramNames[i] : ("arg" + i);
            Object v = args[i];
            if (v == null) {
                result.put(name, null);
                continue;
            }
            // 跳过不适合序列化到审计日志的 Servlet / IO / 大对象
            if (v instanceof HttpServletRequest
                    || v instanceof jakarta.servlet.http.HttpServletResponse
                    || v instanceof MultipartFile
                    || v instanceof InputStream
                    || v instanceof java.io.OutputStream
                    || v instanceof jakarta.servlet.http.HttpSession
                    || v instanceof org.springframework.web.multipart.MultipartFile[]) {
                result.put(name, "[skipped:" + v.getClass().getSimpleName() + "]");
                continue;
            }
            result.put(name, v);
        }
        return result;
    }

    /**
     * 读取方法参数名（若未开编译参数则返回 null，由调用方兜底成 arg0/arg1）。
     */
    private String[] methodParamNames(Method method) {
        try {
            java.lang.reflect.Parameter[] parameters = method.getParameters();
            String[] names = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                names[i] = parameters[i].isNamePresent() ? parameters[i].getName() : ("arg" + i);
            }
            return names;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 对待落库的对象做脱敏 + 阈值化处理。
     *
     * <p>流程：对象 → JSON 字符串（脱敏过的 Map 版）→ 若 &le; {@code effectiveMax} 字节直接返回对象；
     * 否则 {@link PreparedPayload#displayValue} 改为带截断标记 + payloadRef 占位符的字符串，
     * 原始 JSON 保留在 {@link PreparedPayload#oversizedRaw} 供 {@code AuditLogService} 写子表。</p>
     *
     * @param raw          原始对象
     * @param effectiveMax 差异化阈值
     * @return 准备好的 payload（displayValue 进主表，oversizedRaw 进子表）
     */
    private PreparedPayload maskAndPrepare(Object raw, int effectiveMax) {
        if (raw == null) {
            return new PreparedPayload(null, null);
        }
        try {
            Object tree = objectMapper.convertValue(raw, Object.class);
            Object masked = maskSensitive(tree);
            String json = objectMapper.writeValueAsString(masked);
            int byteLen = json.getBytes(StandardCharsets.UTF_8).length;
            if (byteLen <= effectiveMax) {
                return new PreparedPayload(masked, null);
            }
            // 超阈值：截断后加引用提示，原文交给服务层分片写入子表（audit_log_id 作为反查键）
            String preview = safeSubstringByBytes(json, effectiveMax);
            String placeholder = preview
                    + "...[truncated " + preview.getBytes(StandardCharsets.UTF_8).length
                    + "/" + byteLen + " bytes; fullPayload -> sys_audit_log_payload WHERE audit_log_id=<this.id>]";
            return new PreparedPayload(placeholder, json);
        } catch (Exception e) {
            // 不可序列化对象（e.g. lambda、循环引用）→ 退化为类型标记
            return new PreparedPayload("[unserializable:" + raw.getClass().getSimpleName() + "]", null);
        }
    }

    /**
     * 按 UTF-8 字节数安全截断字符串，避免把多字节码点切半。
     */
    private String safeSubstringByBytes(String s, int maxBytes) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return s;
        }
        int cut = maxBytes;
        // UTF-8 码点：后续字节形如 10xxxxxx，回退到起始字节
        while (cut > 0 && (bytes[cut] & 0xC0) == 0x80) {
            cut--;
        }
        return new String(bytes, 0, cut, StandardCharsets.UTF_8);
    }

    /**
     * 递归遍历 Map / List，命中敏感字段名的值直接替换为 {@link #MASK}。
     *
     * @param node 结构化节点
     * @return 脱敏后的同构结构
     */
    @SuppressWarnings("unchecked")
    private Object maskSensitive(Object node) {
        if (node == null) {
            return null;
        }
        if (node instanceof Map<?, ?> m) {
            Map<String, Object> result = new HashMap<>(m.size());
            for (Map.Entry<?, ?> e : m.entrySet()) {
                String key = String.valueOf(e.getKey());
                if (isSensitiveKey(key)) {
                    result.put(key, MASK);
                } else {
                    result.put(key, maskSensitive(e.getValue()));
                }
            }
            return result;
        }
        if (node instanceof Iterable<?> it) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (Object item : it) {
                list.add(maskSensitive(item));
            }
            return list;
        }
        return node;
    }

    private boolean isSensitiveKey(String key) {
        if (key == null || key.isEmpty()) return false;
        return SENSITIVE_KEY_PATTERN.matcher(key).find();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private HttpServletRequest currentRequest() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                return sra.getRequest();
            }
        } catch (Exception ignore) {
            // pass
        }
        return null;
    }

    /**
     * 语义兜底：应对未来某些场景下 @AuditLog 存在但注解元数据丢失的情况。
     *
     * @param method 方法
     * @return 注解实例或 null
     */
    @SuppressWarnings("unused")
    private <A extends Annotation> A safeFindAnnotation(Method method, Class<A> annoType) {
        try {
            return AnnotationUtils.findAnnotation(method, annoType);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 内部包装：一次性携带 payload 的「展示版」和「原始版」。
     *
     * <ul>
     *   <li>{@code displayValue} 会被塞进主表 {@code payload_json}，必要时已截断；</li>
     *   <li>{@code oversizedRaw} 是完整 JSON 字符串，仅当超阈值时非空，交给服务层切片存子表。</li>
     * </ul>
     */
    private record PreparedPayload(Object displayValue, String oversizedRaw) {
    }
}
