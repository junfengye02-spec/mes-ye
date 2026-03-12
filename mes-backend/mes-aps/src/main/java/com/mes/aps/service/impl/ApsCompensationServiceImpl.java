package com.mes.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.aps.client.ApsClient;
import com.mes.aps.domain.entity.ApsSyncQueue;
import com.mes.aps.domain.vo.ApsSyncResultVO;
import com.mes.aps.enums.SyncStatus;
import com.mes.aps.mapper.ApsSyncQueueMapper;
import com.mes.aps.service.IApsCompensationService;
import com.mes.aps.service.IApsSyncConfigService;
import com.mes.aps.service.IApsUpstreamSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * APS 补偿同步服务实现
 * <p>APS 恢复后按优先级批量处理积压数据，包含去重逻辑</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApsCompensationServiceImpl implements IApsCompensationService {

    private final ApsSyncQueueMapper syncQueueMapper;
    private final ApsClient apsClient;
    private final IApsUpstreamSyncService upstreamSyncService;
    private final IApsSyncConfigService configService;

    @Override
    public ApsSyncResultVO compensate() {
        if (!apsClient.isAvailable()) {
            log.warn("[补偿同步] APS 仍不可用，跳过补偿");
            return ApsSyncResultVO.builder()
                    .batchId(UUID.randomUUID().toString())
                    .status("SKIPPED")
                    .message("APS不可用")
                    .build();
        }

        long pendingCount = getPendingCount();
        long failedCount = getFailedCount();
        log.info("[补偿同步] 开始处理积压数据: pending={}, failed={}", pendingCount, failedCount);

        // 将 FAILED 状态重置为 PENDING，让上行同步重新处理
        if (failedCount > 0) {
            resetFailedItems();
            log.info("[补偿同步] 已重置 {} 条失败记录为待处理", failedCount);
        }

        // 调用上行同步处理队列
        ApsSyncResultVO result = upstreamSyncService.processQueue();
        log.info("[补偿同步] 完成: status={}, total={}, success={}, fail={}",
                result.getStatus(), result.getTotalCount(), result.getSuccessCount(), result.getFailCount());

        return result;
    }

    @Override
    public long getPendingCount() {
        return syncQueueMapper.selectCount(
                new LambdaQueryWrapper<ApsSyncQueue>()
                        .eq(ApsSyncQueue::getSyncStatus, SyncStatus.PENDING.getCode()));
    }

    private long getFailedCount() {
        return syncQueueMapper.selectCount(
                new LambdaQueryWrapper<ApsSyncQueue>()
                        .eq(ApsSyncQueue::getSyncStatus, SyncStatus.FAILED.getCode()));
    }

    private void resetFailedItems() {
        ApsSyncQueue update = new ApsSyncQueue();
        update.setSyncStatus(SyncStatus.PENDING.getCode());
        update.setRetryCount(0);
        update.setNextRetryTime(null);
        update.setErrorMessage(null);

        syncQueueMapper.update(update,
                new LambdaQueryWrapper<ApsSyncQueue>()
                        .eq(ApsSyncQueue::getSyncStatus, SyncStatus.FAILED.getCode()));
    }
}
