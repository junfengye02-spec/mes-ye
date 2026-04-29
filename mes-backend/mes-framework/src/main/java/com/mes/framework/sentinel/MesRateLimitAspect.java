package com.mes.framework.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.mes.framework.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * {@link MesRateLimit} 注解切面
 *
 * <p>在方法执行前按资源名 + 业务维度参数（IP/租户）进入 Sentinel 资源：
 * <ol>
 *   <li>{@link MesRateLimit.Key#IP} → 从当前 HTTP 上下文取 IP 作为热点参数</li>
 *   <li>{@link MesRateLimit.Key#TENANT} → 从 {@link TenantContextHolder} 取租户 ID 作为热点参数</li>
 *   <li>{@link MesRateLimit.Key#DEFAULT} → 无热点参数，退化为 FlowRule 的单机 QPS 限流</li>
 * </ol>
 * </p>
 *
 * <p>进入失败（超限）抛出 {@link RateLimitBlockException}，由
 * {@link SentinelBlockExceptionHandler} 统一处理为 HTTP 429 + 标准响应体。</p>
 *
 * <p>本切面通过 {@code @ConditionalOnClass(SphU.class)} 条件装配，
 * 当 classpath 中没有 Sentinel 时（降级使用 Resilience4j 场景）自动失效，
 * 此时注解将空转，业务请求正常放行。</p>
 */
@Slf4j
@Aspect
@Component
@Order(10)
@ConditionalOnClass(SphU.class)
@ConditionalOnProperty(prefix = "mes.sentinel", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MesRateLimitAspect {

    /**
     * 环绕切面：根据注解配置进入 Sentinel 资源，并在超限时抛出业务异常
     *
     * @param pjp AOP 连接点
     * @param rl  方法上的 {@link MesRateLimit} 注解
     * @return 原方法返回值
     * @throws Throwable 原方法抛出的异常 或 {@link RateLimitBlockException}
     */
    @Around("@annotation(rl)")
    public Object around(ProceedingJoinPoint pjp, MesRateLimit rl) throws Throwable {
        String resource = rl.resource();
        Object[] hotArgs = resolveHotArgs(rl.key());
        Entry entry = null;
        try {
            // P0 附带修复（mcp24）：sentinel-core 1.8.6 的 SphU 里没有
            // entryWithArgs(...) / entryWithArgs(String, int, Object...) 签名，
            // 实际可用的是 entry(String, EntryType, int, Object...)。
            // 该签名已内置 ParamFlowRule 支持（热点参数走 ParamFlowSlot），
            // 传 hotArgs 即可满足原始语义，同时保留 EntryType.IN。
            if (hotArgs.length > 0) {
                entry = SphU.entry(resource, EntryType.IN, 1, hotArgs);
            } else {
                entry = SphU.entry(resource, EntryType.IN);
            }
            return pjp.proceed();
        } catch (BlockException e) {
            log.warn("[Sentinel] 限流触发 resource={}, key={}, args={}",
                    resource, rl.key(), hotArgs.length == 0 ? "[]" : hotArgs[0]);
            throw new RateLimitBlockException(resource, e);
        } finally {
            if (entry != null) {
                if (hotArgs.length > 0) {
                    entry.exit(1, hotArgs);
                } else {
                    entry.exit();
                }
            }
        }
    }

    /**
     * 根据限流维度解析热点参数值
     *
     * @param key 限流维度枚举
     * @return 参数数组（DEFAULT 返回空数组）
     */
    private Object[] resolveHotArgs(MesRateLimit.Key key) {
        return switch (key) {
            case IP -> new Object[]{IpResolver.currentIp()};
            case TENANT -> new Object[]{resolveTenantId()};
            default -> new Object[0];
        };
    }

    /**
     * 解析当前租户 ID，未登录/跨租户场景下使用 0 作为哨兵值
     *
     * @return 租户 ID 字符串（Sentinel 热点参数需 equals 可比较）
     */
    private String resolveTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? "0" : String.valueOf(tenantId);
    }
}
