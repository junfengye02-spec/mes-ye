package com.mes.framework.ratelimit;

import com.mes.common.exception.BusinessException;
import com.mes.framework.cache.CacheKeys;
import com.mes.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 单体网关内的"按租户令牌桶限流"过滤器。
 *
 * <p>每个租户每秒允许的请求数取自 {@code sys_tenant.quota_qps}，
 * Redis 端通过 Lua 原子地自增 + 过期，保证多节点一致。</p>
 *
 * <p>超限返回 {@code 429 Too Many Requests} + Retry-After 头。
 * 平台超管（tenant=0）与未认证请求（登录前）不受此切面约束。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantRateLimitFilter extends OncePerRequestFilter {

    private static final String LUA_INCR_WITH_TTL =
            "local current = redis.call('INCR', KEYS[1])\n" +
            "if current == 1 then\n" +
            "  redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))\n" +
            "end\n" +
            "return current";

    private static final DefaultRedisScript<Long> SCRIPT;
    static {
        DefaultRedisScript<Long> s = new DefaultRedisScript<>();
        s.setScriptText(LUA_INCR_WITH_TTL);
        s.setResultType(Long.class);
        SCRIPT = s;
    }

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    /** 本地缓存租户 QPS 上限，避免每个请求都查 DB（TTL 由 {@link #refreshIntervalMs} 控制） */
    private final ConcurrentMap<Long, CachedQps> limitCache = new ConcurrentHashMap<>();
    private final long refreshIntervalMs = 30_000L;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !TenantContextHolder.PLATFORM_TENANT_ID.equals(tenantId)) {
            int limit = resolveQpsLimit(tenantId);
            if (limit > 0) {
                String key = CacheKeys.tenant(tenantId, "rl:qps", System.currentTimeMillis() / 1000);
                Long current = null;
                try {
                    current = redisTemplate.execute(SCRIPT, List.of(key), "2");
                } catch (Exception e) {
                    log.warn("[RateLimit] Redis 不可用，降级放行: tenantId={}, err={}", tenantId, e.getMessage());
                }
                if (current != null && current > limit) {
                    log.info("[RateLimit] 超限拒绝: tenantId={}, current={}, limit={}", tenantId, current, limit);
                    response.setStatus(429);
                    response.setHeader("Retry-After", "1");
                    response.setContentType("application/json;charset=utf-8");
                    response.getWriter().write("{\"code\":429,\"message\":\"当前租户请求过于频繁，请稍后再试\"}");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private int resolveQpsLimit(Long tenantId) {
        CachedQps cached = limitCache.get(tenantId);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.fetchedAt < refreshIntervalMs) {
            return cached.value;
        }
        int limit = 0;
        try {
            Integer v = jdbcTemplate.queryForObject(
                    "SELECT quota_qps FROM sys_tenant WHERE id = ? AND deleted = 0",
                    Integer.class, tenantId);
            if (v != null) limit = v;
        } catch (Exception e) {
            log.warn("[RateLimit] 读取 sys_tenant.quota_qps 失败: {}", e.getMessage());
        }
        limitCache.put(tenantId, new CachedQps(limit, now));
        return limit;
    }

    /** 让调用方可以主动失效本地缓存（例如平台运营更新配额后发事件） */
    public void invalidate(Long tenantId) {
        if (tenantId != null) limitCache.remove(tenantId);
    }

    private record CachedQps(int value, long fetchedAt) {}

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 登录 / 公开接口不走限流（避免登录被锁死）
        String path = request.getRequestURI();
        return path != null && (path.startsWith("/auth/") || path.startsWith("/platform/tenants/register")
                || path.startsWith("/actuator"));
    }
}
