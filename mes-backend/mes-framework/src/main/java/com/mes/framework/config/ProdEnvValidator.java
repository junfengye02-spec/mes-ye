package com.mes.framework.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * 生产环境必填环境变量校验器（P0-05 安全整改 + P1-35/36 扩展）
 *
 * <p>场景：为防止生产环境因为 YAML 里写了弱密码 fallback（如 12345678、mes_rabbitmq_2024、mes-default-api-key）
 * 导致环境变量漏配时仍能用弱密码启动，在应用启动时对 prod profile 下的关键配置做 fail-fast 校验。</p>
 *
 * <p>校验规则：
 * <ul>
 *   <li>数据库密码（spring.datasource.password）必须非空且长度 ≥ 12</li>
 *   <li>RabbitMQ 密码（spring.rabbitmq.password）必须非空且长度 ≥ 12（若配置了 rabbitmq 才校验）</li>
 *   <li>JWT 密钥（mes.jwt.secret）长度必须 ≥ 32 字节（已有框架校验，此处兜底）</li>
 *   <li>APS API Key（mes.aps.api-key）长度必须 ≥ 16 且不能是已知弱值 mes-default-api-key（P1-35）</li>
 *   <li>APS HMAC Key（mes.aps.hmac-key）必须非空且长度 ≥ 32 字节（P1-36，若 mes.aps.enabled=true）</li>
 * </ul>
 * </p>
 *
 * <p>检出任何一项违规直接抛出 IllegalStateException，阻止应用启动，让运维知道必须修配置。</p>
 *
 * @author mcp7
 */
@Slf4j
@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class ProdEnvValidator {

    private static final int MIN_PASSWORD_LEN = 12;
    private static final int MIN_JWT_SECRET_LEN = 32;
    private static final int MIN_APS_API_KEY_LEN = 16;
    private static final int MIN_APS_HMAC_KEY_LEN = 32;

    /** 已知的 APS API Key 弱默认值，绝对不允许出现在生产环境 */
    private static final String[] WEAK_APS_API_KEYS = {
            "mes-default-api-key",
            "default",
            "test",
            "123456",
    };

    private final Environment env;

    @Value("${spring.datasource.password:}")
    private String dataSourcePassword;

    @Value("${spring.rabbitmq.password:}")
    private String rabbitmqPassword;

    @Value("${mes.jwt.secret:}")
    private String jwtSecret;

    @Value("${mes.aps.api-key:}")
    private String apsApiKey;

    @Value("${mes.aps.hmac-key:}")
    private String apsHmacKey;

    @Value("${mes.aps.enabled:true}")
    private boolean apsEnabled;

    /**
     * 应用启动后立即执行的环境校验。
     *
     * @throws IllegalStateException 关键环境变量缺失或过弱时抛出，阻止应用启动
     */
    @PostConstruct
    public void validateProdEnv() {
        List<String> errors = new ArrayList<>();

        if (isBlank(dataSourcePassword) || dataSourcePassword.length() < MIN_PASSWORD_LEN) {
            errors.add("环境变量 SPRING_DATASOURCE_PASSWORD 必须注入且长度 >= " + MIN_PASSWORD_LEN + " 位");
        }

        if (isRabbitmqConfigured() && (isBlank(rabbitmqPassword) || rabbitmqPassword.length() < MIN_PASSWORD_LEN)) {
            errors.add("环境变量 SPRING_RABBITMQ_PASSWORD 必须注入且长度 >= " + MIN_PASSWORD_LEN + " 位");
        }

        if (!isBlank(jwtSecret) && jwtSecret.length() < MIN_JWT_SECRET_LEN) {
            errors.add("配置 mes.jwt.secret 长度必须 >= " + MIN_JWT_SECRET_LEN + " 位");
        }

        // P1-35：APS API Key 强度校验（仅在启用 APS 集成时生效）
        if (apsEnabled) {
            if (isBlank(apsApiKey)) {
                errors.add("[P1-35] 启用 APS 集成时必须设置 mes.aps.api-key（环境变量 MES_APS_API_KEY），长度 >= " + MIN_APS_API_KEY_LEN);
            } else {
                if (apsApiKey.length() < MIN_APS_API_KEY_LEN) {
                    errors.add("[P1-35] mes.aps.api-key 长度不足 " + MIN_APS_API_KEY_LEN + " 位（当前 " + apsApiKey.length() + "）");
                }
                for (String weak : WEAK_APS_API_KEYS) {
                    if (weak.equalsIgnoreCase(apsApiKey.trim())) {
                        errors.add("[P1-35] mes.aps.api-key 命中已知弱值 \"" + weak + "\"，必须更换为随机字符串");
                        break;
                    }
                }
            }

            // P1-36：HMAC 密钥强度校验（仅在启用 APS 集成时生效）
            if (isBlank(apsHmacKey)) {
                errors.add("[P1-36] 启用 APS 集成时必须设置 mes.aps.hmac-key（环境变量 MES_APS_HMAC_KEY），长度 >= " + MIN_APS_HMAC_KEY_LEN);
            } else if (apsHmacKey.getBytes().length < MIN_APS_HMAC_KEY_LEN) {
                errors.add("[P1-36] mes.aps.hmac-key 长度不足 " + MIN_APS_HMAC_KEY_LEN + " 字节（当前 " + apsHmacKey.getBytes().length + "）");
            }
        }

        if (!errors.isEmpty()) {
            String msg = "生产环境配置校验失败（P0-05/P1-35/P1-36 安全策略）：\n  - " + String.join("\n  - ", errors)
                    + "\n请在启动前通过环境变量注入，或检查 CI/CD 部署脚本。";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        log.info("[P0-05/P1-35/P1-36] 生产环境密码与密钥校验通过");
    }

    /**
     * 判断项目是否配置了 RabbitMQ，避免没用 MQ 的部署也被强制要求密码。
     *
     * @return true 表示配置了 host（说明使用了 rabbitmq）
     */
    private boolean isRabbitmqConfigured() {
        String host = env.getProperty("spring.rabbitmq.host");
        return host != null && !host.isBlank();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
