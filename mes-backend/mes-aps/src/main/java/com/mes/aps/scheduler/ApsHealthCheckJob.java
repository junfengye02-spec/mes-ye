package com.mes.aps.scheduler;

import com.mes.aps.client.ApsClient;
import com.mes.aps.service.IApsSyncConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * APS 健康检查定时任务
 * <p>每 30 秒检查一次 APS 可用性</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApsHealthCheckJob {

    private final ApsClient apsClient;
    private final IApsSyncConfigService configService;

    private volatile boolean lastAvailable = true;

    /**
     * 健康检查（每 30 秒）
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void checkHealth() {
        if (!configService.getBooleanConfig("aps.sync.enabled", true)) {
            return;
        }

        boolean available = apsClient.isAvailable();
        String cbState = apsClient.getCircuitBreakerState();

        if (available && !lastAvailable) {
            log.info("[健康检查] APS 服务已恢复可用, 熔断器状态: {}", cbState);
        } else if (!available && lastAvailable) {
            log.warn("[健康检查] APS 服务不可用, 熔断器状态: {}", cbState);
        }

        if (log.isDebugEnabled()) {
            log.debug("[健康检查] APS available={}, circuitBreaker={}", available, cbState);
        }

        lastAvailable = available;
    }

    /**
     * 获取上次检查的可用状态
     */
    public boolean isLastAvailable() {
        return lastAvailable;
    }
}
