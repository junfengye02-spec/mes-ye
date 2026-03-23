package com.mes.framework.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.common.result.R;
import com.mes.common.result.ResultCode;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * 基于 Redis 滑动窗口的限流过滤器
 */
@Slf4j
@Component
@Order(1)
@ConditionalOnProperty(prefix = "mes.rate-limit", name = "enabled", havingValue = "true")
public class RateLimitFilter implements Filter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${mes.rate-limit.default-qps:100}")
    private int defaultQps;

    @Value("${mes.rate-limit.window-seconds:1}")
    private int windowSeconds;

    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local window_start = now - window * 1000
            redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)
            local count = redis.call('ZCARD', key)
            if count < limit then
                redis.call('ZADD', key, now, now .. '-' .. math.random(1000000))
                redis.call('PEXPIRE', key, window * 1000)
                return 1
            end
            return 0
            """;

    private final DefaultRedisScript<Long> redisScript;

    public RateLimitFilter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.redisScript = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String clientIp = getClientIp(request);
        String key = "rate_limit:" + clientIp;

        try {
            Long allowed = redisTemplate.execute(
                    redisScript,
                    Collections.singletonList(key),
                    String.valueOf(defaultQps),
                    String.valueOf(windowSeconds),
                    String.valueOf(System.currentTimeMillis())
            );

            if (allowed == null || allowed == 0) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(objectMapper.writeValueAsString(
                        R.fail(ResultCode.FAIL.getCode(), "请求过于频繁，请稍后再试")));
                return;
            }
        } catch (Exception e) {
            log.error("Rate limit check failed, allowing request: {}", e.getMessage());
        }

        chain.doFilter(servletRequest, servletResponse);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
