package com.mes.framework.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.mes.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sentinel 限流/熔断异常统一处理
 *
 * <p>拦截：
 * <ul>
 *   <li>{@link RateLimitBlockException}：由 {@link MesRateLimitAspect} 抛出的业务限流异常</li>
 *   <li>{@link BlockException}：由原生 {@code @SentinelResource}（若未指定 blockHandler）或
 *       CommonFilter 直接抛到 Controller 层的异常</li>
 * </ul>
 * </p>
 *
 * <p>统一转成 HTTP 429 + R.fail(429, "请求太快，稍后再试")，便于前端统一兜底。</p>
 *
 * <p>优先级 HIGHEST_PRECEDENCE + 1：确保早于 {@link com.mes.common.exception.GlobalExceptionHandler}
 * 命中，避免被兜底的 500 处理覆盖。</p>
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnClass(BlockException.class)
public class SentinelBlockExceptionHandler {

    /**
     * 处理自定义的 {@link RateLimitBlockException}
     *
     * @param e       异常对象，包含被限流的资源名
     * @param request HTTP 请求对象，用于记录日志定位
     * @return 统一的 429 响应
     */
    @ExceptionHandler(RateLimitBlockException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public R<Void> handleRateLimitBlock(RateLimitBlockException e, HttpServletRequest request) {
        log.warn("[Sentinel] 限流拦截: resource={}, uri={}, method={}",
                e.getResource(), request.getRequestURI(), request.getMethod());
        return R.fail(429, "请求太快，稍后再试");
    }

    /**
     * 处理原生 Sentinel {@link BlockException}（例如 SphU.entry 抛出而未被 Aspect 捕获）
     *
     * @param e       Sentinel BlockException
     * @param request HTTP 请求对象
     * @return 统一的 429 响应
     */
    @ExceptionHandler(BlockException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public R<Void> handleBlockException(BlockException e, HttpServletRequest request) {
        log.warn("[Sentinel] 原生 BlockException: uri={}, rule={}",
                request.getRequestURI(),
                e.getRule() == null ? "?" : e.getRule().getResource());
        return R.fail(429, "请求太快，稍后再试");
    }
}
