package com.mes.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

/**
 * 登录失败锁定与验证码触发服务（P1-14）
 *
 * <h3>Redis 键模型</h3>
 * <ul>
 *   <li>{@code auth:login:fail:{tenantCode}:{username}} — 15 分钟滑动窗口失败次数（INCR + EXPIRE）</li>
 *   <li>{@code auth:captcha:required:{tenantCode}:{username}} — 是否要求验证码（TTL 15 分钟）</li>
 * </ul>
 *
 * <h3>门槛</h3>
 * <ul>
 *   <li>失败 &ge; 3 次 &rarr; 要求验证码</li>
 *   <li>失败 &ge; 5 次 &rarr; 账号锁定（HTTP 423）</li>
 *   <li>登录成功 &rarr; 清空计数与验证码标记</li>
 * </ul>
 *
 * <p>租户 code 为空时用占位符 {@code _} 保证 key 唯一，避免空串导致的 Redis key 冲突。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLockoutService {

    public static final String KEY_FAIL_PREFIX = "auth:login:fail:";
    public static final String KEY_CAPTCHA_REQUIRED_PREFIX = "auth:captcha:required:";

    /** 要求验证码的失败阈值 */
    public static final int THRESHOLD_CAPTCHA = 3;
    /** 锁定账号的失败阈值 */
    public static final int THRESHOLD_LOCK = 5;
    /** 滑动窗口长度 */
    public static final Duration WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    /**
     * 登录前校验：是否已被锁定
     */
    public boolean isLocked(String tenantCode, String username) {
        try {
            String val = redisTemplate.opsForValue().get(failKey(tenantCode, username));
            return val != null && parseInt(val) >= THRESHOLD_LOCK;
        } catch (Exception e) {
            log.warn("[Lockout] 读取失败次数异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 登录前校验：是否需要验证码
     */
    public boolean isCaptchaRequired(String tenantCode, String username) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.hasKey(captchaRequiredKey(tenantCode, username)));
        } catch (Exception e) {
            log.warn("[Lockout] 读取验证码要求状态异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 登录失败：失败次数 +1，返回当前累计次数；同时按阈值触发验证码要求
     */
    public long recordFailure(String tenantCode, String username) {
        try {
            String key = failKey(tenantCode, username);
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) count = 1L;
            // 首次失败时设置 TTL；后续 increment 保留剩余 TTL
            redisTemplate.expire(key, WINDOW);
            if (count >= THRESHOLD_CAPTCHA) {
                redisTemplate.opsForValue().set(
                        captchaRequiredKey(tenantCode, username), "1", WINDOW);
            }
            if (count >= THRESHOLD_LOCK) {
                log.warn("[Lockout] 账号已锁定: tenant={}, username={}, fail={}",
                        tenantCode, username, count);
            }
            return count;
        } catch (Exception e) {
            log.warn("[Lockout] 记录失败次数异常: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 登录成功：清空失败次数与验证码要求
     */
    public void recordSuccess(String tenantCode, String username) {
        try {
            redisTemplate.delete(failKey(tenantCode, username));
            redisTemplate.delete(captchaRequiredKey(tenantCode, username));
        } catch (Exception e) {
            log.warn("[Lockout] 清理失败次数异常: {}", e.getMessage());
        }
    }

    // ==================== 私有 key 构造 ====================

    private String failKey(String tenantCode, String username) {
        return KEY_FAIL_PREFIX + normalize(tenantCode) + ":" + normalize(username);
    }

    private String captchaRequiredKey(String tenantCode, String username) {
        return KEY_CAPTCHA_REQUIRED_PREFIX + normalize(tenantCode) + ":" + normalize(username);
    }

    private static String normalize(String s) {
        return s == null || s.isBlank() ? "_" : Objects.requireNonNull(s).trim();
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
