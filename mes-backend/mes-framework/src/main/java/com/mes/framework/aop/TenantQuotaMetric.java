package com.mes.framework.aop;

/**
 * 配额维度枚举，与 {@code sys_tenant} 表字段一一对应。
 */
public enum TenantQuotaMetric {

    /** 用户数上限（对应 sys_tenant.quota_users） */
    USERS("quota_users"),

    /** 文件存储上限（MB；对应 sys_tenant.quota_storage_mb） */
    STORAGE_MB("quota_storage_mb"),

    /** 接口 QPS 上限（对应 sys_tenant.quota_qps；通常由网关做前置限流，这里主要是做业务软限） */
    QPS("quota_qps");

    private final String column;

    TenantQuotaMetric(String column) {
        this.column = column;
    }

    public String column() {
        return column;
    }
}
