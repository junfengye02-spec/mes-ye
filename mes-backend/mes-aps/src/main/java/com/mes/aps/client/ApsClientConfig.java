package com.mes.aps.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * APS 客户端配置
 * - RestTemplate（超时 30s）
 * - Resilience4j CircuitBreaker（5 次失败阈值 / 30s 开启超时）
 * - Resilience4j Retry（3 次重试，5s/15s/30s 间隔）
 */
@Configuration
public class ApsClientConfig {

    public static final String APS_CIRCUIT_BREAKER = "apsCircuitBreaker";
    public static final String APS_RETRY = "apsRetry";

    @Bean("apsRestTemplate")
    public RestTemplate apsRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(5)
                .slidingWindowSize(10)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean(APS_CIRCUIT_BREAKER)
    public CircuitBreaker apsCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(APS_CIRCUIT_BREAKER);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(5))
                .retryExceptions(Exception.class)
                .build();

        return RetryRegistry.of(config);
    }

    @Bean(APS_RETRY)
    public Retry apsRetry(RetryRegistry registry) {
        return registry.retry(APS_RETRY);
    }
}
