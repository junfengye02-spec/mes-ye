package com.mes.framework.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配额校验注解。加在创建资源的接口上，由 {@code TenantQuotaAspect}
 * 在调用前把当前租户用量与 {@code sys_tenant.quota_*} 做比对，超限直接拒绝。
 *
 * <pre>{@code
 * @TenantQuota(metric = TenantQuotaMetric.USERS, delta = 1)
 * public SysUser createUser(...) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantQuota {

    /** 配额维度。 */
    TenantQuotaMetric metric();

    /** 本次调用打算消耗的量（默认 1）。 */
    long delta() default 1L;

    /** 对超限操作自定义错误提示（留空则用默认文案）。 */
    String errorMessage() default "";
}
