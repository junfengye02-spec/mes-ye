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
}
