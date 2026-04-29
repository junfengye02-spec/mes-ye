package com.mes.framework.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * JWT 黑名单与 Refresh Token 一次性校验服务（P1-22）
 *
 * <h3>Redis 键模型</h3>
 * <ul>
 *   <li>{@code jwt:blacklist:{jti}} — access token 登出黑名单；value=1，TTL=access 剩余有效期</li>
 *   <li>{@code jwt:refresh:used:{jti}} — refresh token 已使用标记；value=1，TTL=原 refresh 剩余有效期</li>
 *   <li>{@code jwt:user-revoke:{tenantId}:{userId}} — 用户级吊销时间戳（毫秒）；
 *       filter 校验 token 的 iat &lt; 此时间戳即视为失效。用于"refresh 被重放→强制登出"。</li>
 * </ul>
 *
 * <p>所有方法均做 try-catch，Redis 挂掉时降级为"放行并打印 WARN"，避免登出 / 重放场景把核心链路打挂。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    /** access token 黑名单 key 前缀 */
    private static final String KEY_BLACKLIST = "jwt:blacklist:";
    /** refresh token 已使用标记 key 前缀 */
    private static final String KEY_REFRESH_USED = "jwt:refresh:used:";
    /** 用户级吊销时间戳 key 前缀 */
    private static final String KEY_USER_REVOKE = "jwt:user-revoke:";

    private final StringRedisTemplate redisTemplate;

    // ==================== 黑名单（logout） ====================

    /**
     * 将 access token 加入黑名单
     *
     * @param jti        token 的 jti
     * @param ttlMillis  剩余有效期（毫秒），&le; 0 表示 token 已过期，不再写入
     */
    public void addToBlacklist(String jti, long ttlMillis) {
        if (jti == null || jti.isBlank() || ttlMillis <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(KEY_BLACKLIST + jti, "1", Duration.ofMillis(ttlMillis));
            log.info("[JWT] access token 已加入黑名单: jti={}, ttlMs={}", jti, ttlMillis);
        } catch (Exception e) {
            log.warn("[JWT] 写入黑名单失败，降级放行: jti={}, err={}", jti, e.getMessage());
        }
    }

    /**
     * 判断 access token 是否在黑名单
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_BLACKLIST + jti));
        } catch (Exception e) {
            log.warn("[JWT] 读取黑名单失败，降级放行: jti={}, err={}", jti, e.getMessage());
            return false;
        }
    }

    // ==================== Refresh 一次性轮换 ====================

    /**
     * 标记 refresh token 已被使用。返回 true 表示写入成功；false 表示已被标记过（即重放）。
     *
     * @param jti        refresh token 的 jti
     * @param ttlMillis  剩余有效期
     */
    public boolean markRefreshUsed(String jti, long ttlMillis) {
        if (jti == null || jti.isBlank() || ttlMillis <= 0) {
            return false;
        }
        try {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(
                    KEY_REFRESH_USED + jti, "1", Duration.ofMillis(ttlMillis));
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("[JWT] 标记 refresh 已使用失败: jti={}, err={}", jti, e.getMessage());
            // 失败时保守返回 true，避免无限轮转
            return true;
        }
    }

    /**
     * 判断 refresh token 是否已被使用过
     */
    public boolean isRefreshUsed(String jti) {
        if (jti == null || jti.isBlank()) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_REFRESH_USED + jti));
        } catch (Exception e) {
            log.warn("[JWT] 查询 refresh 使用状态失败，降级为 false: jti={}, err={}", jti, e.getMessage());
            return false;
        }
    }

    // ==================== 用户级吊销（refresh 重放场景） ====================

    /**
     * 吊销指定用户的所有现存 token：写入当前时间戳为"吊销时间"；
     * JWT 校验时比较 {@code iat < revokeTime} 即视为失效。
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @param ttlDays  该标记保留天数（≥ refresh 最大有效期即可）
     */
    public void invalidateAllForUser(Long tenantId, Long userId, int ttlDays) {
        if (tenantId == null || userId == null) return;
        try {
            redisTemplate.opsForValue().set(
                    userRevokeKey(tenantId, userId),
                    String.valueOf(System.currentTimeMillis()),
                    Duration.ofDays(Math.max(1, ttlDays)));
            log.warn("[JWT] 已吊销用户所有会话: tenantId={}, userId={}", tenantId, userId);
        } catch (Exception e) {
            log.warn("[JWT] 吊销用户会话失败: tenantId={}, userId={}, err={}",
                    tenantId, userId, e.getMessage());
        }
    }

    /**
     * 判断 token 是否早于用户级吊销时间戳
     *
     * @param tenantId  租户 ID
     * @param userId    用户 ID
     * @param issuedAt  token 签发时间（毫秒）
     * @return true 表示 token 在吊销时间之前签发，应被视为失效
     */
    public boolean isRevokedForUser(Long tenantId, Long userId, long issuedAt) {
        if (tenantId == null || userId == null) return false;
        try {
            String val = redisTemplate.opsForValue().get(userRevokeKey(tenantId, userId));
            if (val == null || val.isBlank()) return false;
            long revokedAt = Long.parseLong(val);
            return issuedAt < revokedAt;
        } catch (NumberFormatException | IllegalStateException e) {
            log.warn("[JWT] user-revoke 值异常，忽略: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("[JWT] 读取 user-revoke 失败，降级放行: tenantId={}, userId={}, err={}",
                    tenantId, userId, e.getMessage());
            return false;
        }
    }

    private String userRevokeKey(Long tenantId, Long userId) {
        return KEY_USER_REVOKE + tenantId + ":" + userId;
    }
}
