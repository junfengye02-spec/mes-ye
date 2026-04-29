package com.mes.framework.security;

import com.mes.framework.cache.CacheKeys;
import com.mes.framework.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器：
 * <ol>
 *   <li>从 Authorization 头提取 Bearer token；</li>
 *   <li>校验有效性与类型（必须为 access）；</li>
 *   <li>将 tenantId / userId / username 写入 {@link TenantContextHolder} 与 {@link SecurityContextHolder}；</li>
 *   <li>向 {@link MDC} 注入 tenantId / userId / traceId，便于日志可观测性与跨租户排障；</li>
 *   <li>从 Redis 加载权限（key 带租户前缀）。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_TRACE_ID = "X-Request-Id";

    private static final String MDC_TENANT_ID = "tenantId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_USERNAME = "username";
    private static final String MDC_TRACE_ID = "traceId";

    /** Redis 权限缓存的模块名，最终 key 形如 tenant:{tid}:auth:permissions:{uid} */
    public static final String PERMISSIONS_MODULE = "auth:permissions";

    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final JwtBlacklistService jwtBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(MDC_TRACE_ID, traceId);
        try {
            String token = extractToken(request);
            if (token != null && tokenProvider.validateToken(token)) {
                String tokenType = tokenProvider.getTokenType(token);
                if (!"access".equals(tokenType)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // 黑名单校验（P1-22）：
                // 1) 当前 access token 被主动登出加入黑名单
                // 2) refresh 被重放时，用户所有会话被强制吊销
                String jti = tokenProvider.getJti(token);
                if (jti != null && jwtBlacklistService.isBlacklisted(jti)) {
                    log.info("[JWT] token 已被加入黑名单，忽略认证: jti={}", jti);
                    filterChain.doFilter(request, response);
                    return;
                }

                Long userId = tokenProvider.getUserId(token);
                String username = tokenProvider.getUsername(token);
                Long tenantId = tokenProvider.getTenantId(token);
                String accountType = tokenProvider.getAccountType(token);

                long issuedAt = tokenProvider.getIssuedAt(token).getTime();
                if (jwtBlacklistService.isRevokedForUser(tenantId, userId, issuedAt)) {
                    log.info("[JWT] token 早于用户级吊销时间，忽略认证: tenantId={}, userId={}, iat={}",
                            tenantId, userId, issuedAt);
                    filterChain.doFilter(request, response);
                    return;
                }

                TenantContextHolder.setTenantId(tenantId);
                MDC.put(MDC_TENANT_ID, String.valueOf(tenantId));
                MDC.put(MDC_USER_ID, String.valueOf(userId));
                if (username != null) {
                    MDC.put(MDC_USERNAME, username);
                }

                Set<SimpleGrantedAuthority> authorities = loadPermissions(tenantId, userId);

                LoginUser loginUser = new LoginUser();
                loginUser.setUserId(userId);
                loginUser.setUsername(username);
                loginUser.setEnabled(true);
                loginUser.setTenantId(tenantId);
                loginUser.setAccountType(accountType);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_USERNAME);
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    private String resolveTraceId(HttpServletRequest request) {
        String existing = request.getHeader(HEADER_TRACE_ID);
        if (StringUtils.hasText(existing)) {
            return existing;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private Set<SimpleGrantedAuthority> loadPermissions(Long tenantId, Long userId) {
        try {
            String key = CacheKeys.tenant(tenantId, PERMISSIONS_MODULE, userId);
            Set<String> members = redisTemplate.opsForSet().members(key);
            if (members != null && !members.isEmpty()) {
                return members.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
            }
        } catch (Exception e) {
            log.warn("Redis 读取权限失败，降级为空权限: {}", e.getMessage());
        }
        return Collections.emptySet();
    }
}
