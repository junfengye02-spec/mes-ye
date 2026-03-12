package com.mes.aps.scheduler;

import com.mes.aps.client.ApsClient;
import com.mes.aps.service.IApsCompensationService;
import com.mes.aps.service.IApsSyncConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * APS 补偿同步定时任务
 * <p>每 10 分钟检查一次，当 APS 恢复可用且有积压数据时自动补偿</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApsCompensationSyncJob {

    private final IApsCompensationService compensationService;
    private final IApsSyncConfigService configService;
    private final ApsClient apsClient;

    /**
     * 补偿同步（每 10 分钟）
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 120000)
    public void executeCompensation() {
        if (!configService.getBooleanConfig("aps.sync.enabled", true)) {
            return;
        }

        long pending = compensationService.getPendingCount();
        if (pending == 0) {
            log.debug("[补偿同步] 无积压数据，跳过");
            return;
        }

        if (!apsClient.isAvailable()) {
            log.debug("[补偿同步] APS 不可用，等待恢复（积压 {} 条）", pending);
            return;
        }

        log.info("[补偿同步] 检测到积压数据 {} 条，APS 可用，开始补偿", pending);
        try {
            compensationService.compensate();
        } catch (Exception e) {
            log.error("[补偿同步] 执行异常: {}", e.getMessage(), e);
        }
    }
}
