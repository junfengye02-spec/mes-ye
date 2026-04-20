package com.mes.framework.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置：所有异步任务均透传租户上下文与 MDC（通过 {@link TenantContextHolder.TenantContextSnapshot}）。
 *
 * <p>任何 {@code @Async} 默认走 {@link #tenantProvisionExecutor}，保证：</p>
 * <ul>
 *   <li>租户上下文不丢失；</li>
 *   <li>日志 MDC（tenantId / userId / traceId）不丢失；</li>
 *   <li>拒绝策略为 CALLER_RUNS，避免吞任务。</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("tenantProvisionExecutor")
    public ThreadPoolTaskExecutor tenantProvisionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(256);
        executor.setThreadNamePrefix("tenant-provision-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(tenantContextDecorator());
        executor.initialize();
        return executor;
    }

    @Bean("mesDefaultExecutor")
    public ThreadPoolTaskExecutor mesDefaultExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(1024);
        executor.setThreadNamePrefix("mes-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(tenantContextDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * 任务装饰器：在调用线程抓 TenantContext + MDC 快照，在工作线程执行前恢复，结束后清理。
     */
    private TaskDecorator tenantContextDecorator() {
        return runnable -> {
            Long tenantId = TenantContextHolder.getTenantId();
            var mdc = org.slf4j.MDC.getCopyOfContextMap();
            return () -> {
                Long prev = TenantContextHolder.getTenantId();
                var prevMdc = org.slf4j.MDC.getCopyOfContextMap();
                if (tenantId != null) TenantContextHolder.setTenantId(tenantId);
                if (mdc != null) org.slf4j.MDC.setContextMap(mdc);
                try {
                    runnable.run();
                } finally {
                    if (prev == null) TenantContextHolder.clear();
                    else TenantContextHolder.setTenantId(prev);
                    if (prevMdc != null) org.slf4j.MDC.setContextMap(prevMdc);
                    else org.slf4j.MDC.clear();
                }
            };
        };
    }
}
