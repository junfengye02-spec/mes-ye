package com.mes.framework.sentinel;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sentinel 框架装配配置
 *
 * <p>提供以下 Bean：
 * <ul>
 *   <li>{@link SentinelResourceAspect}：支持 {@code @SentinelResource} 原生注解</li>
 * </ul>
 * 其他能力（规则加载、热点维度限流、BlockException 处理）分别由
 * {@link SentinelRuleInitializer}、{@link MesRateLimitAspect}、
 * {@link SentinelBlockExceptionHandler} 提供。
 * </p>
 *
 * <p>开关：{@code mes.sentinel.enabled}（默认 true）。关闭后所有 Sentinel 相关 Bean 都不装配，
 * 方便在 Sentinel 不可用时快速降级。</p>
 */
@Slf4j
@Configuration
@ConditionalOnClass({BlockException.class, SentinelResourceAspect.class})
@ConditionalOnProperty(prefix = "mes.sentinel", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SentinelConfig {

    /**
     * 支持 {@code @SentinelResource} 注解生效的 AOP 切面
     *
     * <p>Sentinel 官方要求业务使用方自行声明该 Bean，否则注解不会被 Spring AOP 解析。</p>
     *
     * @return SentinelResourceAspect
     */
    @Bean
    @ConditionalOnMissingBean
    public SentinelResourceAspect sentinelResourceAspect() {
        log.info("[Sentinel] @SentinelResource AOP 切面已启用");
        return new SentinelResourceAspect();
    }
}
