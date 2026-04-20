package com.mes.framework.tenant;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 当前请求的租户上下文（ThreadLocal 承载），由 {@code JwtAuthenticationFilter}
 * 等入口 Filter 负责在请求开始时 {@link #setTenantId(Long)}，请求结束时 {@link #clear()}。
 *
 * <p>多租户设计约定：</p>
 * <ol>
 *   <li>应用内除非显式使用 {@link #runAs(Long, Runnable)}，任何业务代码都
 *       <strong>不允许</strong>调用 {@link #setTenantId(Long)}——这是给入口 Filter 的 API。</li>
 *   <li>当业务需要知道当前租户而未得到时，应视为系统异常直接失败；
 *       请使用 {@link #requireTenantId()} 而非对 null 做兜底。</li>
 *   <li>异步任务 / 线程池必须通过 {@link TenantContextSnapshot} 手工透传上下文，
 *       或统一使用 {@code TenantAwareExecutor}。</li>
 * </ol>
 */
public final class TenantContextHolder {

    /** 平台级（跨租户运维）使用的保留值，用于超管、初始化、定时任务等场景。 */
    public static final Long PLATFORM_TENANT_ID = 0L;

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    private TenantContextHolder() {}

    /**
     * 入口 Filter / 异步桥接处使用。其余业务代码请改用 {@link #runAs}。
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * 当前租户 ID；未设置时返回 {@code null}——**业务层禁止直接 == null 判断**，
     * 请改用 {@link #requireTenantId()} 或 {@link #isPlatform()}。
     */
    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * 取当前租户 ID；未设置时抛 {@link IllegalStateException}，便于把问题 fail-fast 暴露到日志中。
     */
    public static Long requireTenantId() {
        Long tid = TENANT_ID.get();
        if (tid == null) {
            throw new IllegalStateException(
                    "当前请求缺少 TenantContext；请检查 JwtAuthenticationFilter 是否先于业务执行，"
                    + "或异步任务是否忘记通过 TenantContextSnapshot 透传上下文。");
        }
        return tid;
    }

    /** 是否当前处于平台（超管 / 跨租户）上下文。 */
    public static boolean isPlatform() {
        return PLATFORM_TENANT_ID.equals(TENANT_ID.get());
    }

    /**
     * 临时切换租户执行，块结束后自动恢复。用于：
     * <ul>
     *   <li>系统启动时的种子数据初始化；</li>
     *   <li>超管跨租户维护操作；</li>
     *   <li>定时任务按租户轮询执行。</li>
     * </ul>
     */
    public static void runAs(Long tenantId, Runnable action) {
        Objects.requireNonNull(action, "action is null");
        Long previous = TENANT_ID.get();
        TENANT_ID.set(tenantId);
        try {
            action.run();
        } finally {
            if (previous == null) {
                TENANT_ID.remove();
            } else {
                TENANT_ID.set(previous);
            }
        }
    }

    /** 同 {@link #runAs(Long, Runnable)} 的返回值版本。 */
    public static <T> T runAs(Long tenantId, Supplier<T> action) {
        Objects.requireNonNull(action, "action is null");
        Long previous = TENANT_ID.get();
        TENANT_ID.set(tenantId);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                TENANT_ID.remove();
            } else {
                TENANT_ID.set(previous);
            }
        }
    }

    /**
     * 类似 {@link #runAs(Long, Supplier)}，但允许 {@link Callable} 中抛出受检异常；
     * 受检异常会被包装为 {@link RuntimeException} 抛出，便于在不想声明 throws 的业务栈里使用。
     */
    public static <T> T callAsOrThrow(Long tenantId, Callable<T> action) {
        try {
            return callAs(tenantId, action);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@link Callable} 版本，支持抛出受检异常。 */
    public static <T> T callAs(Long tenantId, Callable<T> action) throws Exception {
        Objects.requireNonNull(action, "action is null");
        Long previous = TENANT_ID.get();
        TENANT_ID.set(tenantId);
        try {
            return action.call();
        } finally {
            if (previous == null) {
                TENANT_ID.remove();
            } else {
                TENANT_ID.set(previous);
            }
        }
    }

    /**
     * 抓取当前上下文快照，用于跨线程传递（例如 {@link java.util.concurrent.CompletableFuture}、
     * {@link java.util.concurrent.Executor}）。
     */
    public static TenantContextSnapshot snapshot() {
        return new TenantContextSnapshot(TENANT_ID.get());
    }

    public static void clear() {
        TENANT_ID.remove();
    }

    /**
     * ThreadLocal 快照，用于异步线程边界。
     * <pre>{@code
     * TenantContextSnapshot snap = TenantContextHolder.snapshot();
     * executor.submit(snap.wrap(() -> doStuff()));
     * }</pre>
     */
    public static final class TenantContextSnapshot {
        private final Long tenantId;

        private TenantContextSnapshot(Long tenantId) {
            this.tenantId = tenantId;
        }

        public Long tenantId() {
            return tenantId;
        }

        public Runnable wrap(Runnable delegate) {
            Objects.requireNonNull(delegate, "delegate is null");
            return () -> {
                Long previous = TENANT_ID.get();
                TENANT_ID.set(tenantId);
                try {
                    delegate.run();
                } finally {
                    if (previous == null) {
                        TENANT_ID.remove();
                    } else {
                        TENANT_ID.set(previous);
                    }
                }
            };
        }

        public <T> Callable<T> wrap(Callable<T> delegate) {
            Objects.requireNonNull(delegate, "delegate is null");
            return () -> {
                Long previous = TENANT_ID.get();
                TENANT_ID.set(tenantId);
                try {
                    return delegate.call();
                } finally {
                    if (previous == null) {
                        TENANT_ID.remove();
                    } else {
                        TENANT_ID.set(previous);
                    }
                }
            };
        }
    }
}
