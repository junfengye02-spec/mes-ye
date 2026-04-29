package com.mes.framework.audit;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 审计事件数据载体，由业务层构造后交给 {@code AuditLogService.log()}。
 */
@Data
@Builder
public class AuditEvent {

    /** 操作对象所属租户（一般就是当前 TenantContext） */
    private Long tenantId;

    /** 发起者所在租户（超管跨租户操作时 ≠ tenantId） */
    private Long operatorTenantId;

    /** 发起者 userId */
    private Long operatorUserId;
    private String operatorUsername;

    /** 动作：LOGIN / LOGIN_FAIL / ROLE_CHANGE / DELETE / EXPORT / TENANT_SUSPEND ... */
    private String action;

    /** 被操作对象类型（USER / ROLE / WORK_ORDER ...） */
    private String targetType;
    /** 被操作对象 ID（字符串，兼容复合主键） */
    private String targetId;

    /** 请求 trace id */
    private String traceId;
    private String ip;
    private String userAgent;

    /** 结果：OK / FAIL */
    private String result;
    private String errorMessage;

    /** 关键上下文（before/after、diff 等），JSON 序列化入库 */
    private Map<String, Object> payload;

    /**
     * P3-12：超过 {@code @AuditLog.payloadMaxSize} 阈值、需要落入子表 {@code sys_audit_log_payload}
     * 的大响应体原文（未截断）。为 null 表示不需要切片存储。
     *
     * <p>字段命名按「oversized」强调仅在超阈值时使用；主表 {@link #payload} 里会保留
     * 首段截断版 + {@code payloadRef=&lt;audit_log_id&gt;} 指针，以便查询时能串起来。</p>
     */
    private String oversizedResponse;

    /** 同上：大请求体原文（少见，多为批量上传场景）。为 null 表示不切片。 */
    private String oversizedRequest;

    /**
     * 切片存储单片大小（字节）。0 或负值表示使用全局默认值
     * （{@code mes.audit.chunk-size}，默认 64 KB）。
     */
    private int payloadChunkSize;
}
