package com.mes.aps.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.aps.domain.entity.ApsSyncConfig;
import com.mes.aps.mapper.ApsSyncConfigMapper;
import com.mes.framework.tenant.TenantContextHolder;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * APS REST 客户端封装
 * <p>集成 API Key 认证、熔断器、重试机制</p>
 */
@Slf4j
@Component
public class ApsClient {

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final ApsSyncConfigMapper configMapper;

    public ApsClient(
            @Qualifier("apsRestTemplate") RestTemplate restTemplate,
            @Qualifier(ApsClientConfig.APS_CIRCUIT_BREAKER) CircuitBreaker circuitBreaker,
            @Qualifier(ApsClientConfig.APS_RETRY) Retry retry,
            ApsSyncConfigMapper configMapper) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.configMapper = configMapper;
    }

    /**
     * GET 请求
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String path, Class<T> responseType) {
        String url = getBaseUrl() + path;
        HttpHeaders headers = buildHeaders();

        Supplier<T> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
            ResponseEntity<T> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), responseType);
            return response.getBody();
        });

        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("APS GET 请求失败: path={}, cbState={}, error={}", path, getCircuitBreakerState(), e.getMessage());
            return (T) getFallbackValue(responseType);
        }
    }

    /**
     * GET 请求（支持泛型类型）
     */
    public <T> T get(String path, ParameterizedTypeReference<T> typeReference) {
        String url = getBaseUrl() + path;
        HttpHeaders headers = buildHeaders();

        Supplier<T> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
            ResponseEntity<T> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), typeReference);
            return response.getBody();
        });

        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("APS GET 请求失败: path={}, cbState={}, error={}", path, getCircuitBreakerState(), e.getMessage());
            return null;
        }
    }

    /**
     * POST 请求
     */
    @SuppressWarnings("unchecked")
    public <T> T post(String path, Object body, Class<T> responseType) {
        String url = getBaseUrl() + path;
        HttpHeaders headers = buildHeaders();

        Supplier<T> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            ResponseEntity<T> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, responseType);
            return response.getBody();
        });

        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("APS POST 请求失败: path={}, cbState={}, error={}", path, getCircuitBreakerState(), e.getMessage());
            return (T) getFallbackValue(responseType);
        }
    }

    /**
     * PUT 请求
     */
    @SuppressWarnings("unchecked")
    public <T> T put(String path, Object body, Class<T> responseType) {
        String url = getBaseUrl() + path;
        HttpHeaders headers = buildHeaders();

        Supplier<T> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            ResponseEntity<T> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, responseType);
            return response.getBody();
        });

        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("APS PUT 请求失败: path={}, cbState={}, error={}", path, getCircuitBreakerState(), e.getMessage());
            return (T) getFallbackValue(responseType);
        }
    }

    /**
     * 异步 POST — 处理 202 Accepted 响应，返回 requestId
     */
    public String postAsync(String path, Object body) {
        String url = getBaseUrl() + path;
        HttpHeaders headers = buildHeaders();

        Supplier<String> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object data = response.getBody().get("data");
                if (data instanceof Map<?, ?> dataMap) {
                    Object reqId = dataMap.get("requestId");
                    return reqId != null ? String.valueOf(reqId) : null;
                }
            }
            return null;
        });

        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("APS 异步 POST 请求失败: path={}, cbState={}, error={}", path, getCircuitBreakerState(), e.getMessage());
            return null;
        }
    }

    /**
     * 健康检查：APS 是否可用
     */
    public boolean isAvailable() {
        try {
            String url = getBaseUrl() + "/api/health";
            HttpHeaders headers = buildHeaders();
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.warn("APS 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取熔断器状态
     */
    public String getCircuitBreakerState() {
        return circuitBreaker.getState().name();
    }

    // ==================== 私有方法 ====================

    private Object getFallbackValue(Class<?> responseType) {
        if (java.util.List.class.isAssignableFrom(responseType)) {
            return Collections.emptyList();
        }
        if (java.util.Map.class.isAssignableFrom(responseType)) {
            return Collections.emptyMap();
        }
        return null;
    }

    private String getBaseUrl() {
        return getConfigValue("aps.base.url", "http://localhost:8081");
    }

    private String getApiKey() {
        return getConfigValue("aps.api.key", "mes-default-api-key");
    }

    /**
     * P1-34：所有 MES &rarr; APS 请求都携带外部请求追踪头，便于 APS 侧日志与幂等关联。
     * <pre>X-External-Request-Id: MES-{tenantId}-{UUID}</pre>
     * 其中 tenantId 缺失（平台级或定时任务）时退化为 0。
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", getApiKey());

        Long tenantId = TenantContextHolder.getTenantId();
        String externalRequestId = "MES-" + (tenantId != null ? tenantId : 0L)
                + "-" + UUID.randomUUID();
        headers.set("X-External-Request-Id", externalRequestId);
        log.debug("APS 外呼请求头 X-External-Request-Id={}", externalRequestId);
        return headers;
    }

    private String getConfigValue(String key, String defaultValue) {
        try {
            ApsSyncConfig config = configMapper.selectOne(
                    new LambdaQueryWrapper<ApsSyncConfig>()
                            .eq(ApsSyncConfig::getConfigKey, key)
                            .eq(ApsSyncConfig::getEnabled, 1));
            return config != null ? config.getConfigValue() : defaultValue;
        } catch (Exception e) {
            log.warn("读取 APS 配置失败: key={}, 使用默认值: {}", key, defaultValue);
            return defaultValue;
        }
    }
}
