package com.mes.aps.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * APS 回调幂等服务（P1-33）
 *
 * <p>使用 Redis {@code SETNX + EX} 保证 APS 的重试不会导致 MES 侧重复落库。</p>
 *
 * <h3>Redis 键模型</h3>
 * <pre>mes:aps:callback:{callbackType}:{requestId}</pre>
 * <ul>
 *   <li>{@code callbackType} 示例：{@code scheduleResult} / {@code requestRejected} /
 *       {@code mrpResult} / {@code resourceAllocation} / {@code ganttData} /
 *       {@code capacityLoad} / {@code scheduleChange}</li>
 *   <li>{@code requestId} 由 APS 端生成，MES 侧只负责鉴别</li>
 *   <li>值固定为 {@code 1}，TTL 默认 {@link #DEFAULT_TTL}（24 小时，覆盖 APS 正常重试窗口）</li>
 * </ul>
 *
 * <h3>典型用法</h3>
 * <pre>{@code
 * if (!idempotency.tryAcquire("scheduleResult", callback.getRequestId())) {
 *     log.warn("重复回调忽略: requestId={}", callback.getRequestId());
 *     return;
 * }
 * // ... 业务处理
 * }</pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApsCallbackIdempotencyService {

    /** 幂等 key 前缀 */
    public static final String KEY_PREFIX = "mes:aps:callback:";

    /** 默认 TTL：24 小时，覆盖 APS 多数重试窗口 */
    public static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    /**
     * 尝试占位：返回 true 表示首次调用、应继续业务；false 表示重复调用、应直接忽略。
     *
     * @param callbackType 回调类型（如 scheduleResult）
     * @param requestId    APS 传入的请求 ID
     */
    public boolean tryAcquire(String callbackType, String requestId) {
        return tryAcquire(callbackType, requestId, DEFAULT_TTL);
    }

    /**
     * 带 TTL 的占位
     */
    public boolean tryAcquire(String callbackType, String requestId, Duration ttl) {
        if (!StringUtils.hasText(callbackType) || !StringUtils.hasText(requestId)) {
            // requestId 缺失直接视为不可幂等，放行以免阻断合法请求（业务层仍有状态机防御）
            log.warn("[Idempotency] callbackType 或 requestId 为空，跳过幂等校验: type={}, requestId={}",
                    callbackType, requestId);
            return true;
        }
        String key = KEY_PREFIX + callbackType + ":" + requestId;
        try {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
            if (Boolean.FALSE.equals(ok)) {
                log.info("[Idempotency] 重复回调：type={}, requestId={}", callbackType, requestId);
                return false;
            }
            return true;
        } catch (Exception e) {
            // Redis 故障时降级放行，避免把 APS 正常回调打挂；业务幂等性由状态机兜底
            log.warn("[Idempotency] Redis 异常，降级放行: type={}, requestId={}, err={}",
                    callbackType, requestId, e.getMessage());
            return true;
        }
    }
}
