package com.mes.aps.scheduler;

import com.mes.aps.domain.vo.ApsSyncResultVO;
import com.mes.aps.service.IApsUpstreamSyncService;
import com.mes.aps.service.IApsSyncConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * APS 上行同步定时任务（MES → APS）
 * <p>每 5 分钟执行一次，消费同步队列</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApsUpstreamSyncJob {

    private final IApsUpstreamSyncService upstreamSyncService;
    private final IApsSyncConfigService configService;

    /**
     * 定时上行同步（每 5 分钟）
     */
    @Scheduled(fixedDelayString = "${aps.sync.interval-ms:300000}", initialDelay = 90000)
    public void executeUpstreamSync() {
        if (!configService.getBooleanConfig("aps.sync.enabled", true)) {
            return;
        }
        if (!configService.getBooleanConfig("aps.sync.upstream.enabled", true)) {
            return;
        }

        long pendingCount = upstreamSyncService.getPendingCount();
        if (pendingCount == 0) {
            log.debug("[定时任务] APS 上行同步：队列为空，跳过");
            return;
        }

        log.info("[定时任务] APS 上行同步开始: 待处理 {} 条", pendingCount);
        try {
            ApsSyncResultVO result = upstreamSyncService.processQueue();
            log.info("[定时任务] APS 上行同步完成: status={}, total={}, success={}, fail={}",
                    result.getStatus(), result.getTotalCount(), result.getSuccessCount(), result.getFailCount());
        } catch (Exception e) {
            log.error("[定时任务] APS 上行同步异常: {}", e.getMessage(), e);
        }
    }
}
