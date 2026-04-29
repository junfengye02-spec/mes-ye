package com.mes.framework.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.mes.common.result.R;
import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel @SentinelResource 的 blockHandler 方法聚合类
 *
 * <p>Sentinel 对 {@code @SentinelResource(blockHandler=..., blockHandlerClass=...)}
 * 的约束：blockHandler 必须为 public static，方法签名除最后一个 {@link BlockException} 外
 * 其余参数必须与原方法完全一致（包括顺序和类型）。</p>
 *
 * <p>为避免每处业务都定义一份 blockHandler，此处提供统一的"抛出 {@link RateLimitBlockException}"
 * 实现，最终由 {@link SentinelBlockExceptionHandler} 转成 HTTP 429 响应。</p>
 *
 * <p>如果原方法返回值需要特别处理（比如不能抛异常只能返回 R.fail），可以在本类里追加更具体的处理器方法。</p>
 */
@Slf4j
public final class SentinelBlockHandlers {

    private SentinelBlockHandlers() {
    }

    /**
     * 通用 blockHandler：适配返回值为 {@link R} 的方法
     *
     * <p>实际使用时需要在 {@code @SentinelResource(blockHandler="handleR")} 指向本方法，
     * 且原方法必须是 {@code R<xxx> fn(...)} 形式。由于 Sentinel 的泛型擦除机制，
     * 只要返回类型是 {@code R} 即可兼容。</p>
     *
     * @param e 触发的 BlockException（Sentinel 自动注入）
     * @return 统一 429 响应
     */
    public static R<Void> handleR(BlockException e) {
        log.warn("[Sentinel] BlockHandler 触发: rule={}, resource={}",
                e.getRule() == null ? "?" : e.getRule().getResource(),
                e.getRuleLimitApp());
        return R.fail(429, "请求太快，稍后再试");
    }

    /**
     * 带任意前置参数的 blockHandler 适配器
     *
     * <p>Sentinel 会按"原方法参数 + BlockException"的签名反射匹配，因此对于
     * 任意参数数量的方法可统一透传 varargs 再退到 {@link #handleR(BlockException)}。</p>
     *
     * @param args 原方法参数（由 Sentinel 反射传入，此处仅用于签名匹配，不使用）
     * @param e    触发的 BlockException
     * @return 统一 429 响应
     */
    public static R<Void> handleWithArgs(Object[] args, BlockException e) {
        return handleR(e);
    }
}
