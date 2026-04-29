package com.mes.framework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.common.result.R;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * APS 回调 HMAC 签名校验过滤器（P0-12）
 * <p>仅对 {@code /aps/callback/**} 路径生效；其它请求直接放行。</p>
 *
 * <h3>请求头要求</h3>
 * <ul>
 *   <li>{@code X-API-Key}    对端的 API Key，必须与 {@code mes.aps.api-key} 配置一致</li>
 *   <li>{@code X-Timestamp}  请求时间戳（秒或毫秒皆可），服务端与当前时间偏差不得超过 {@code mes.aps.timestamp-skew-seconds}</li>
 *   <li>{@code X-Signature}  签名值（小写 hex），计算方式：
 *       <pre>HMAC-SHA256(apiKey + "\n" + timestamp + "\n" + rawBody, secret)</pre></li>
 * </ul>
 *
 * <h3>失败响应</h3>
 * <p>任一校验失败均返回 401 + {@link R#fail(String)} 格式 JSON，{@code code=401}，不透出敏感细节。</p>
 *
 * <h3>启动期校验（fail-fast）</h3>
 * <p>当 Profile 不是 {@code dev} 且 {@code mes.aps.enabled=true} 时：</p>
 * <ul>
 *   <li>{@code mes.aps.hmac-key} 为空 &rarr; 启动失败</li>
 *   <li>{@code mes.aps.hmac-key} 长度 &lt; 32 字节 &rarr; 启动失败</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HmacSignatureFilter extends OncePerRequestFilter {

    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Signature";

    /** 仅拦截 APS 回调路径 */
    private static final String APS_CALLBACK_PREFIX = "/aps/callback/";

    private static final String HMAC_ALGO = "HmacSHA256";

    private final ApsSecurityProperties properties;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    /**
     * 启动期 fail-fast 校验：
     * <ul>
     *   <li>非 dev 环境强制要求 {@code mes.aps.hmac-key} 非空且 &ge; 32 字节</li>
     *   <li>dev 环境允许缺失，日志打印警告</li>
     * </ul>
     */
    @PostConstruct
    public void validateConfig() {
        if (!properties.isEnabled()) {
            log.warn("[HMAC] APS 回调签名校验已关闭（mes.aps.enabled=false），仅用于开发调试，严禁生产使用");
            return;
        }
        boolean isDev = isDevProfile();
        if (!StringUtils.hasText(properties.getHmacKey())) {
            if (isDev) {
                log.warn("[HMAC] mes.aps.hmac-key 未配置，开发环境允许放行 /aps/callback/**，生产环境必须配置");
            } else {
                throw new IllegalStateException(
                        "[HMAC] 启动失败：生产环境 mes.aps.hmac-key 必须配置（建议 &ge; 32 字节随机字符串）");
            }
            return;
        }
        int len = properties.getHmacKey().getBytes(StandardCharsets.UTF_8).length;
        if (len < 32 && !isDev) {
            throw new IllegalStateException(
                    "[HMAC] 启动失败：mes.aps.hmac-key 长度不足 32 字节（当前=" + len + "），生产环境请重新生成");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            log.warn("[HMAC] mes.aps.api-key 未配置，APS 回调将无法通过 API Key 校验");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 仅对 APS 回调路径生效
        String path = request.getRequestURI();
        if (path == null) return true;
        // 兼容 context-path=/api 的前缀
        return !path.contains(APS_CALLBACK_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 开发环境允许放行（hmac-key 未配置时）
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getHmacKey())) {
            log.warn("[HMAC] 签名校验未启用，放行请求：{}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // 先包装 body，保证后续 @RequestBody 仍能读取
        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);
        byte[] body = wrapper.getCachedBody();

        // 1. 校验必填请求头
        String apiKey = wrapper.getHeader(HEADER_API_KEY);
        String timestamp = wrapper.getHeader(HEADER_TIMESTAMP);
        String signature = wrapper.getHeader(HEADER_SIGNATURE);
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(timestamp) || !StringUtils.hasText(signature)) {
            writeUnauthorized(response, "APS 回调缺少必要请求头 X-API-Key / X-Timestamp / X-Signature");
            return;
        }

        // 2. API Key 校验（常量时间比较避免时序攻击）
        if (!StringUtils.hasText(properties.getApiKey())
                || !constantTimeEquals(properties.getApiKey(), apiKey)) {
            log.warn("[HMAC] API Key 校验失败，uri={}, apiKey=***{}",
                    wrapper.getRequestURI(), tail(apiKey));
            writeUnauthorized(response, "APS 回调 API Key 非法");
            return;
        }

        // 3. 时间戳偏差校验（支持秒或毫秒，按位数判定）
        long nowSeconds = Instant.now().getEpochSecond();
        long tsSeconds;
        try {
            long raw = Long.parseLong(timestamp.trim());
            tsSeconds = raw > 100_000_000_000L ? raw / 1000L : raw;
        } catch (NumberFormatException e) {
            writeUnauthorized(response, "APS 回调 X-Timestamp 非法，需为 Unix 秒或毫秒整数");
            return;
        }
        long skew = Math.abs(nowSeconds - tsSeconds);
        if (skew > properties.getTimestampSkewSeconds()) {
            log.warn("[HMAC] 时间戳偏差过大，uri={}, skew={}s > 允许 {}s",
                    wrapper.getRequestURI(), skew, properties.getTimestampSkewSeconds());
            writeUnauthorized(response, "APS 回调时间戳偏差超过允许范围");
            return;
        }

        // 4. 计算期望签名并常量时间比对
        String expected = computeHmac(properties.getHmacKey(), apiKey, timestamp, body);
        if (!constantTimeEquals(expected, signature.trim().toLowerCase())) {
            log.warn("[HMAC] 签名不匹配，uri={}, apiKey=***{}",
                    wrapper.getRequestURI(), tail(apiKey));
            writeUnauthorized(response, "APS 回调签名不匹配");
            return;
        }

        // 校验通过，记录审计日志后放行
        log.info("[HMAC] APS 回调签名校验通过，uri={}, ts={}, bodySize={}B",
                wrapper.getRequestURI(), tsSeconds, body.length);
        filterChain.doFilter(wrapper, response);
    }

    // ==================== 私有工具 ====================

    private boolean isDevProfile() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles == null || profiles.length == 0) {
            // 未显式配置时视为 dev
            return true;
        }
        return Arrays.asList(profiles).contains("dev");
    }

    /**
     * 计算 HMAC-SHA256 签名：
     * <pre>HMAC-SHA256(apiKey + "\n" + timestamp + "\n" + rawBody, secret)</pre>
     */
    private String computeHmac(String secret, String apiKey, String timestamp, byte[] body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            mac.update(apiKey.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '\n');
            mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '\n');
            if (body != null && body.length > 0) {
                mac.update(body);
            }
            byte[] digest = mac.doFinal();
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(x, y);
    }

    private String tail(String s) {
        if (!StringUtils.hasText(s)) return "";
        int n = Math.min(4, s.length());
        return s.substring(s.length() - n);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(401, message)));
    }

    // ==================== HTTP 请求体缓存包装 ====================

    /**
     * 将请求体读取到 {@code byte[]} 并在 {@link #getInputStream()} / {@link #getReader()} 复用，
     * 以便 Filter 计算 HMAC 后后续 {@code @RequestBody} 还能再次读取。
     */
    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        public byte[] getCachedBody() {
            return body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bis = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() { return bis.available() == 0; }

                @Override
                public boolean isReady() { return true; }

                @Override
                public void setReadListener(ReadListener readListener) { /* no-op */ }

                @Override
                public int read() { return bis.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
