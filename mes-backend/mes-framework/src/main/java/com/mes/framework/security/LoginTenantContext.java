package com.mes.framework.security;

/**
 * 登录过程中（调用 AuthenticationManager.authenticate → DaoAuthenticationProvider →
 * UserDetailsService.loadUserByUsername 之间）临时携带租户信息的 ThreadLocal。
 *
 * <p>和 {@link com.mes.framework.tenant.TenantContextHolder} 的区别：</p>
 * <ul>
 *   <li>{@code TenantContextHolder}：请求级上下文，由 JwtAuthenticationFilter 写入，
 *       代表"已经认证完成"的租户身份；</li>
 *   <li>{@code LoginTenantContext}：登录动作内的短临时上下文，仅在 AuthService.login
 *       调用 authenticate 之前 set、之后 remove；仅用于把前端传的 tenantCode 透传给
 *       {@code UserDetailsServiceImpl.loadUserByUsername}，让其按 (tenant, username) 查用户。</li>
 * </ul>
 */
public final class LoginTenantContext {

    private static final ThreadLocal<String> TENANT_CODE = new ThreadLocal<>();

    private LoginTenantContext() {}

    public static void setTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            TENANT_CODE.remove();
        } else {
            TENANT_CODE.set(tenantCode.trim());
        }
    }

    public static String getTenantCode() {
        return TENANT_CODE.get();
    }

    public static void clear() {
        TENANT_CODE.remove();
    }
}
