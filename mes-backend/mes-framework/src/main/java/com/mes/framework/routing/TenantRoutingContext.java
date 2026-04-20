package com.mes.framework.routing;

/**
 * 数据源路由上下文：默认 {@code null} 表示走共享库（POOL）。
 *
 * <p>一般在 {@code JwtAuthenticationFilter} 之后、业务执行之前，
 * 根据当前 {@link com.mes.framework.tenant.TenantContextHolder} 的 tenantId
 * 查 {@code sys_tenant.schema_mode}：</p>
 * <ul>
 *   <li>POOL → {@code set(null)}；</li>
 *   <li>SCHEMA → {@code set("SCHEMA_" + tenantId)}；</li>
 *   <li>DB → {@code set("TENANT_" + tenantId)}。</li>
 * </ul>
 *
 * <p>当前框架仅预留占位，真实的 RoutingFilter 在启用 Silo 模式时再接入。</p>
 */
public final class TenantRoutingContext {

    private static final ThreadLocal<String> KEY = new ThreadLocal<>();

    private TenantRoutingContext() {}

    public static void set(String routingKey) {
        if (routingKey == null || routingKey.isBlank()) {
            KEY.remove();
        } else {
            KEY.set(routingKey);
        }
    }

    public static String get() {
        return KEY.get();
    }

    public static void clear() {
        KEY.remove();
    }
}
