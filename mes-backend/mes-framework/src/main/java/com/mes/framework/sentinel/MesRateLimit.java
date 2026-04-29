package com.mes.framework.sentinel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MES 限流注解：基于 Sentinel 热点参数限流实现
 *
 * <p>与原生 {@code @SentinelResource} 配合使用：
 * <ul>
 *   <li>{@code @SentinelResource} 负责资源埋点 + 单机 QPS 限流（FlowRule）</li>
 *   <li>{@code @MesRateLimit} 负责按 IP / 租户等业务维度的热点参数限流（ParamFlowRule）</li>
 * </ul>
 * </p>
 *
 * <p>示例：
 * <pre>
 *   &#64;SentinelResource(value = "auth:login", blockHandler = "loginBlock",
 *                         blockHandlerClass = SentinelBlockHandlers.class)
 *   &#64;MesRateLimit(resource = "auth:login", key = MesRateLimit.Key.IP, count = 10)
 *   public R&lt;LoginVO&gt; login(...) { ... }
 * </pre>
 * </p>
 *
 * <p>超限时会抛出 {@link RateLimitBlockException}，由
 * {@link SentinelBlockExceptionHandler} 统一转成 HTTP 429 + R.fail(429, …)。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MesRateLimit {

    /**
     * Sentinel 资源名（建议使用 {@link SentinelResources} 中的常量）
     */
    String resource();

    /**
     * 限流维度
     */
    Key key() default Key.DEFAULT;

    /**
     * QPS 阈值
     */
    int count();

    /**
     * 统计窗口（秒），默认 1 秒 = 每秒 N 次
     */
    int durationSec() default 1;

    /**
     * 限流维度枚举
     */
    enum Key {
        /** 不带参数维度，退化为 FlowRule 单机 QPS */
        DEFAULT,
        /** 按客户端 IP 维度（防爆破） */
        IP,
        /** 按租户 ID 维度（防租户爆推） */
        TENANT
    }
}
