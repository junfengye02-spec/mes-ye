package com.mes.framework.cache;

import com.mes.framework.tenant.TenantContextHolder;

import java.util.Objects;

/**
 * 统一的 Redis key 构建工具。所有 Redis 操作必须通过本工具拼 key，
 * 禁止在业务代码中手写字符串拼接。
 *
 * <p>两类命名空间：</p>
 * <ul>
 *   <li><strong>租户级</strong>：{@code tenant:{tenantId}:{module}:{...}}</li>
 *   <li><strong>平台级</strong>：{@code platform:{module}:{...}}（如租户元数据、限流令牌桶、分布式锁等跨租户基础设施）</li>
 * </ul>
 *
 * <p>租户级 key 缺失 {@link TenantContextHolder#getTenantId()} 时直接抛异常——
 * 这是一条"快速失败"规则，防止不同租户共用同一个 key。</p>
 */
public final class CacheKeys {

    /** 租户级前缀 */
    public static final String TENANT_PREFIX = "tenant";

    /** 平台级前缀 */
    public static final String PLATFORM_PREFIX = "platform";

    private CacheKeys() {}

    /**
     * 构建当前租户的 key：{@code tenant:{currentTenantId}:{module}:{parts}}。
     * 当前租户上下文为空时抛出 {@link IllegalStateException}。
     */
    public static String tenant(String module, Object... parts) {
        Long tid = TenantContextHolder.getTenantId();
        if (tid == null) {
            throw new IllegalStateException(
                    "构建租户级 Redis key 时未取到 tenantId；请确认请求已经过 JwtAuthenticationFilter/TenantContext 设置。module=" + module);
        }
        return tenant(tid, module, parts);
    }

    /**
     * 构建指定租户的 key：{@code tenant:{tenantId}:{module}:{parts}}。
     */
    public static String tenant(Long tenantId, String module, Object... parts) {
        Objects.requireNonNull(tenantId, "tenantId is null");
        Objects.requireNonNull(module, "module is null");
        StringBuilder sb = new StringBuilder(64);
        sb.append(TENANT_PREFIX).append(':').append(tenantId).append(':').append(module);
        appendParts(sb, parts);
        return sb.toString();
    }

    /**
     * 构建平台级 key：{@code platform:{module}:{parts}}。
     * 仅用于跨租户基础设施（租户元数据、审计、监控采样）。
     */
    public static String platform(String module, Object... parts) {
        Objects.requireNonNull(module, "module is null");
        StringBuilder sb = new StringBuilder(64);
        sb.append(PLATFORM_PREFIX).append(':').append(module);
        appendParts(sb, parts);
        return sb.toString();
    }

    /**
     * 当前租户级 key 的扫描通配符，用于按租户维度批量清理：
     * {@code tenant:{currentTenantId}:{module}:*}。
     */
    public static String tenantPattern(String module) {
        return tenant(module, "*");
    }

    /**
     * 指定租户级 key 的扫描通配符。
     */
    public static String tenantPattern(Long tenantId, String module) {
        return tenant(tenantId, module, "*");
    }

    private static void appendParts(StringBuilder sb, Object... parts) {
        if (parts == null) {
            return;
        }
        for (Object part : parts) {
            if (part == null) {
                throw new IllegalArgumentException("Redis key 分段不能为 null");
            }
            sb.append(':').append(part);
        }
    }
}
