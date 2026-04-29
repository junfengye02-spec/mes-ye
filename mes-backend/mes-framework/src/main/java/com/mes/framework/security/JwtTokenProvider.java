package com.mes.framework.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * JWT 令牌工具：生成、解析、验证
 *
 * <p>所有令牌都携带 {@code tenantId}（数值）与 {@code tenantCode}（字符串）两个 claims。
 * 服务端鉴权只信 {@code tenantId}，{@code tenantCode} 仅用于前端显示与审计日志。</p>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /** 历史默认值黑名单，命中即视为未配置 */
    private static final Set<String> INSECURE_DEFAULTS = Set.of(
            "MesSystemDefaultSecretKeyThatIsAtLeast256BitsLong!!",
            "MesSystemDefaultSecretKeyThatIsAtLeast256BitsLong",
            "dev-only-NOT-FOR-PRODUCTION-pls-override-me-with-random-32bytes+",
            "ChangeMe",
            "changeme"
    );

    /** 最低密钥长度（HS256 要求 ≥256 位，即 32 字节） */
    private static final int MIN_SECRET_LENGTH = 32;

    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtTokenProvider(
            @Value("${mes.jwt.secret:}") String secret,
            @Value("${mes.jwt.access-expiration:7200000}") long accessExpiration,
            @Value("${mes.jwt.refresh-expiration:604800000}") long refreshExpiration,
            Environment env) {
        String resolved = validateSecret(secret, env);
        this.key = Keys.hmacShaKeyFor(resolved.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    private String validateSecret(String secret, Environment env) {
        boolean isProd = env != null && env.getActiveProfiles() != null
                && Set.of(env.getActiveProfiles()).contains("prod");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "mes.jwt.secret 未配置。请在环境变量 MES_JWT_SECRET 或配置文件中注入强随机密钥（>=32 字节）");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "mes.jwt.secret 长度不足 " + MIN_SECRET_LENGTH + " 字节，HS256 要求 256 位以上强随机密钥");
        }
        if (INSECURE_DEFAULTS.contains(secret)) {
            if (isProd) {
                throw new IllegalStateException("mes.jwt.secret 命中历史默认值，生产环境禁止使用");
            }
            log.warn("[JWT] 检测到默认 / 弱密钥，请仅在本地开发使用。生产必须通过 MES_JWT_SECRET 注入强随机密钥。");
        }
        return secret;
    }

    public String createAccessToken(Long userId, String username, Long tenantId, String tenantCode, String accountType) {
        return buildToken(userId, username, tenantId, tenantCode, accountType, accessExpiration, "access");
    }

    public String createRefreshToken(Long userId, String username, Long tenantId, String tenantCode, String accountType) {
        return buildToken(userId, username, tenantId, tenantCode, accountType, refreshExpiration, "refresh");
    }

    private String buildToken(Long userId, String username, Long tenantId, String tenantCode,
                              String accountType, long expiration, String tokenType) {
        if (tenantId == null) {
            throw new IllegalStateException(
                    "签发 JWT 时必须提供 tenantId（平台超管请传 TenantContextHolder.PLATFORM_TENANT_ID = 0）");
        }
        Date now = new Date();
        String at = accountType != null ? accountType : "ADMIN";
        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("tenantId", tenantId)
                .claim("accountType", at)
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration));
        if (tenantCode != null && !tenantCode.isBlank()) {
            builder.claim("tenantCode", tenantCode);
        }
        return builder.signWith(key).compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    public String getTokenType(String token) {
        return parseToken(token).get("type", String.class);
    }

    /**
     * 返回 JWT 中的 tenantId；若 token 中不含 tenantId 直接拒绝，不再 fallback 到 1。
     */
    public Long getTenantId(String token) {
        Long t = parseToken(token).get("tenantId", Long.class);
        if (t == null) {
            throw new IllegalStateException("JWT 缺少 tenantId claim，拒绝接受");
        }
        return t;
    }

    public String getTenantCode(String token) {
        return parseToken(token).get("tenantCode", String.class);
    }

    /** ADMIN=管理端+现场端均可；STAFF=仅现场端 */
    public String getAccountType(String token) {
        String a = parseToken(token).get("accountType", String.class);
        return a != null ? a : "ADMIN";
    }

    /**
     * 获取 token 的 jti（唯一 ID），用于黑名单 / refresh 轮换（P1-22）
     */
    public String getJti(String token) {
        return parseToken(token).getId();
    }

    /**
     * 获取 token 的过期时间
     */
    public Date getExpiration(String token) {
        return parseToken(token).getExpiration();
    }

    /**
     * 获取 token 的签发时间
     */
    public Date getIssuedAt(String token) {
        return parseToken(token).getIssuedAt();
    }
}
