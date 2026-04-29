package com.mes.framework.sentinel;

/**
 * 限流阻塞异常
 *
 * <p>当 Sentinel 判定请求需要被限流时，{@link MesRateLimitAspect} 会将
 * 原生的 {@code BlockException} 转封装为本异常抛出，方便
 * {@link SentinelBlockExceptionHandler} 以统一方式处理（HTTP 429 + R.fail）。</p>
 */
public class RateLimitBlockException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 被限流的 Sentinel 资源名 */
    private final String resource;

    public RateLimitBlockException(String resource, Throwable cause) {
        super("请求触发限流: " + resource, cause);
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
