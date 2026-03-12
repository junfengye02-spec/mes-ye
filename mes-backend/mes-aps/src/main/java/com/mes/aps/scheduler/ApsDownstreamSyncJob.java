package com.mes.aps.scheduler;

import com.mes.aps.domain.vo.ApsSyncResultVO;
import com.mes.aps.service.IApsDownstreamSyncService;
import com.mes.aps.service.IApsSyncConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * APS 下行同步定时任务（APS → MES）
 * <p>每 5 分钟执行一次，从 APS 拉取排程数据</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApsDownstreamSyncJob {

    private final IApsDownstreamSyncService downstreamSyncService;
    private final IApsSyncConfigService configService;

    /**
     * 定时下行同步（每 5 分钟）
     */
    @Scheduled(fixedDelayString = "${aps.sync.interval-ms:300000}", initialDelay = 60000)
    public void executeDownstreamSync() {
        if (!configService.getBooleanConfig("aps.sync.enabled", true)) {
            return;
        }
        if (!configService.getBooleanConfig("aps.sync.downstream.enabled", true)) {
            return;
        }

        log.info("[定时任务] APS 下行同步开始");
        try {
            ApsSyncResultVO result = downstreamSyncService.syncAll();
            log.info("[定时任务] APS 下行同步完成: status={}, total={}, success={}, fail={}",
                    result.getStatus(), result.getTotalCount(), result.getSuccessCount(), result.getFailCount());
        } catch (Exception e) {
            log.error("[定时任务] APS 下行同步异常: {}", e.getMessage(), e);
        }
    }
}
