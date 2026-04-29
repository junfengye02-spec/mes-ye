package com.mes.framework.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * APS 集成安全相关配置（HMAC 签名校验）
 * <p>配置前缀：{@code mes.aps}</p>
 * <ul>
 *   <li>{@code api-key}        外部 APS 调用时携带的 API Key</li>
 *   <li>{@code hmac-key}       HMAC-SHA256 共享密钥（生产环境强制 &ge; 32 字节）</li>
 *   <li>{@code timestamp-skew-seconds} 允许的时间偏差（秒），默认 300 秒</li>
 *   <li>{@code enabled}        是否启用签名校验（默认 {@code true}，本地调试可关闭）</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "mes.aps")
public class ApsSecurityProperties {

    /** APS 外部调用携带的 API Key（与请求头 X-API-Key 比对） */
    private String apiKey;

    /** HMAC-SHA256 共享密钥；生产环境强制 &ge; 32 字节 */
    private String hmacKey;

    /** 时间偏差允许范围（秒），超过则拒绝；默认 300 秒 = 5 分钟 */
    private long timestampSkewSeconds = 300L;

    /** 是否启用签名校验 */
    private boolean enabled = true;
}
