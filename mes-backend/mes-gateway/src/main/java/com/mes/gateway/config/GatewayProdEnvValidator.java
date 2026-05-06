package com.mes.gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

/**
 * Gateway production-only configuration guard.
 */
@Slf4j
@Configuration
@Profile("prod")
public class GatewayProdEnvValidator {

    @Value("${mes.gateway.cors.allowed-origin:}")
    private String corsAllowedOrigin;

    @PostConstruct
    public void validate() {
        String origin = corsAllowedOrigin == null ? "" : corsAllowedOrigin.trim();
        if (!StringUtils.hasText(origin)) {
            throw new IllegalStateException("生产环境必须设置 MES_CORS_ALLOWED_ORIGIN，且只能指向明确的 HTTPS 前端域名");
        }
        if (!origin.startsWith("https://") || origin.contains("*")
                || origin.contains("localhost") || origin.contains("127.0.0.1")) {
            throw new IllegalStateException("MES_CORS_ALLOWED_ORIGIN 生产值必须是明确 HTTPS 域名，禁止通配符和本地地址: " + origin);
        }
        log.info("[P1-03] Gateway CORS production origin validated: {}", origin);
    }
}
