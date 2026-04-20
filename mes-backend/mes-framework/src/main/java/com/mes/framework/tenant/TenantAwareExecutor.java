package com.mes.framework.tenant;

import com.mes.framework.tenant.TenantContextHolder.TenantContextSnapshot;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * 为业务线程池提供"租户上下文透传"的薄装饰器。
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * @Bean
 * public ExecutorService apsExecutor(@Qualifier("apsExecutor") ExecutorService raw) {
 *     return new TenantAwareExecutor(raw);
 * }
 * }</pre>
 *
 * <p>规则：</p>
 * <ul>
 *   <li>提交任务时从调用方线程抓取租户 ID 快照；</li>
 *   <li>任务执行前将快照写入工作线程，任务结束后恢复（包括异常分支）；</li>
 *   <li>若调用方线程本身没有租户上下文，子任务也将以 null 上下文执行——
 *       这在业务代码里取 {@link TenantContextHolder#requireTenantId()} 时会立即暴露问题。</li>
 * </ul>
 */
public final class TenantAwareExecutor implements ExecutorService {

    private final ExecutorService delegate;

    public TenantAwareExecutor(ExecutorService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate is null");
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(snapshot().wrap(command));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(snapshot().wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(snapshot().wrap(task), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(snapshot().wrap(task));
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public java.util.List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> java.util.List<Future<T>> invokeAll(java.util.Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        TenantContextSnapshot snap = snapshot();
        java.util.List<Callable<T>> wrapped = tasks.stream().map(snap::wrap).toList();
        return delegate.invokeAll(wrapped);
    }

    @Override
    public <T> java.util.List<Future<T>> invokeAll(java.util.Collection<? extends Callable<T>> tasks,
                                                   long timeout, java.util.concurrent.TimeUnit unit)
            throws InterruptedException {
        TenantContextSnapshot snap = snapshot();
        java.util.List<Callable<T>> wrapped = tasks.stream().map(snap::wrap).toList();
        return delegate.invokeAll(wrapped, timeout, unit);
    }

    @Override
    public <T> T invokeAny(java.util.Collection<? extends Callable<T>> tasks)
            throws InterruptedException, java.util.concurrent.ExecutionException {
        TenantContextSnapshot snap = snapshot();
        java.util.List<Callable<T>> wrapped = tasks.stream().map(snap::wrap).toList();
        return delegate.invokeAny(wrapped);
    }

    @Override
    public <T> T invokeAny(java.util.Collection<? extends Callable<T>> tasks,
                           long timeout, java.util.concurrent.TimeUnit unit)
            throws InterruptedException, java.util.concurrent.ExecutionException,
            java.util.concurrent.TimeoutException {
        TenantContextSnapshot snap = snapshot();
        java.util.List<Callable<T>> wrapped = tasks.stream().map(snap::wrap).toList();
        return delegate.invokeAny(wrapped, timeout, unit);
    }

    private TenantContextSnapshot snapshot() {
        return TenantContextHolder.snapshot();
    }
}
