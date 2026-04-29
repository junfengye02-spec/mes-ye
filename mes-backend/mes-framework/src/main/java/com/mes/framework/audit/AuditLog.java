package com.mes.framework.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解（P1-13）。
 *
 * <p>标注在 Controller 方法上可显式声明需要被 {@link AuditLogAspect} 记录的接口，
 * 也可用于 {@code record=false} 来显式关闭某个接口的默认记录（例如高频 list/page/detail）。</p>
 *
 * <p>默认规则（不写注解时）：</p>
 * <ul>
 *   <li>@RestController 下的 POST / PUT / DELETE 接口都会被默认记录；</li>
 *   <li>GET 接口默认不记录（避免日志爆炸）；</li>
 *   <li>方法上存在 {@code @AuditLog(record=false)} 时，即便是写接口也跳过。</li>
 * </ul>
 *
 * <p>用法示例：</p>
 * <pre>{@code
 * // 显式声明审计语义，便于日志检索
 * @AuditLog(action = "CREATE_WORK_ORDER", resource = "WORK_ORDER")
 * @PostMapping
 * public R<Long> create(@RequestBody WorkOrderDTO dto) { ... }
 *
 * // 关闭某个写接口的审计（例如内部心跳上报）
 * @AuditLog(record = false)
 * @PostMapping("/heartbeat")
 * public R<Void> heartbeat() { ... }
 *
 * // 关闭请求/响应体记录（敏感业务）
 * @AuditLog(action = "EXPORT_SALARY", recordInput = false, recordOutput = false)
 * @GetMapping("/salary/export")
 * public void export() { ... }
 * }</pre>
 *
 * @author mcp24
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 动作语义，如 {@code CREATE_WORK_ORDER / DELETE_USER / EXPORT_MATERIAL}。
     *
     * <p>留空时由切面自动用 {@code HTTP_METHOD + Controller.SimpleName + MethodName} 兜底生成，
     * 但强烈建议显式声明，便于审计检索。</p>
     */
    String action() default "";

    /**
     * 被操作的资源类型，如 {@code WORK_ORDER / USER / TENANT}。
     *
     * <p>留空时由切面用 Controller 所在的业务模块推断（取 Controller 类名去掉 Controller 后缀）。</p>
     */
    String resource() default "";

    /**
     * 是否落库记录本次调用。
     *
     * <p>默认 {@code true}。显式设为 {@code false} 可关闭某个接口的审计
     * （例如高频 GET 查询、健康检查接口）。</p>
     */
    boolean record() default true;

    /**
     * 是否记录请求体（payload.request）。
     *
     * <p>默认 {@code true}。对包含大文件上传或敏感明文的接口可关闭；
     * 敏感字段（password / secret / token / creditCard ...）始终由切面自动脱敏，
     * 一般情况下无需关闭。</p>
     */
    boolean recordInput() default true;

    /**
     * 是否记录响应体（payload.response）。
     *
     * <p>默认 {@code true}。响应体过大时切面会按 {@link #payloadMaxSize} 分段存入
     * {@code sys_audit_log_payload} 子表，{@code payload_json} 主字段保留首段 + 指向子表的引用，
     * 以便导出 / 批量操作等大响应体场景可溯源。</p>
     */
    boolean recordOutput() default true;

    /**
     * 本接口单条审计记录允许落入主表 {@code sys_audit_log.payload_json} 的最大字节数。
     *
     * <p>P3-12：差异化阈值。针对大导出 / 批量上传 / 报表接口可按需提升到 64KB / 256KB，
     * 而对普通 CRUD 保持默认 10KB。{@code 0} 或负值表示沿用全局配置
     * {@code mes.audit.max-payload-bytes}（默认 10KB）。</p>
     *
     * <p>超过阈值的部分不会被丢弃，而是由 {@link AuditLogAspect} 切面拆分为固定大小分片，
     * 异步写入 {@code sys_audit_log_payload} 子表（{@code payload_json} 中只会写入截断版
     * 以及 {@code payloadRef}=audit_log_id 的引用）。大响应体也可选 MinIO
     * （{@code mes.audit.payload-storage=minio}）作为后端。</p>
     *
     * <p>用法示例：</p>
     * <pre>{@code
     * @AuditLog(action = "EXPORT_SALARY", payloadMaxSize = 64 * 1024)   // 64KB
     * @GetMapping("/salary/export")
     * public void export() { ... }
     *
     * @AuditLog(action = "BULK_IMPORT_USERS", payloadMaxSize = 256 * 1024)
     * @PostMapping("/users/bulk-import")
     * public R<?> bulkImport(@RequestBody List<UserDTO> users) { ... }
     * }</pre>
     */
    int payloadMaxSize() default 0;
}
