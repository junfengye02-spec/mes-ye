package com.mes.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.aps.client.ApsClient;
import com.mes.aps.domain.entity.ApsSyncQueue;
import com.mes.aps.domain.entity.ApsSyncLog;
import com.mes.aps.domain.vo.ApsSyncResultVO;
import com.mes.aps.enums.SyncDirection;
import com.mes.aps.enums.SyncStatus;
import com.mes.aps.enums.SyncType;
import com.mes.aps.mapper.ApsSyncQueueMapper;
import com.mes.aps.service.IApsUpstreamSyncService;
import com.mes.aps.service.IApsSyncConfigService;
import com.mes.aps.service.IApsSyncLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * APS 上行同步服务实现
 * <p>消费同步队列，批量推送 MES 数据到 APS</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApsUpstreamSyncServiceImpl implements IApsUpstreamSyncService {

    private final ApsSyncQueueMapper syncQueueMapper;
    private final ApsClient apsClient;
    private final IApsSyncConfigService configService;
    private final IApsSyncLogService syncLogService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApsSyncResultVO processQueue() {
        String batchId = UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.UPSTREAM.getCode(), "QUEUE");

        if (!configService.getBooleanConfig("aps.sync.upstream.enabled", true)) {
            log.info("APS 上行同步已关闭，跳过队列处理");
            syncLogService.completeLog(syncLog.getId(), 0, 0, 0, "上行同步已关闭");
            return ApsSyncResultVO.builder().batchId(batchId).status("SKIPPED").message("上行同步已关闭").build();
        }

        int batchSize = configService.getIntConfig("aps.sync.batch.size", 200);

        // 获取待处理队列项（按优先级和时间排序）
        List<ApsSyncQueue> pendingItems = syncQueueMapper.selectList(
                new LambdaQueryWrapper<ApsSyncQueue>()
                        .eq(ApsSyncQueue::getSyncStatus, SyncStatus.PENDING.getCode())
                        .and(w -> w
                                .isNull(ApsSyncQueue::getNextRetryTime)
                                .or()
                                .le(ApsSyncQueue::getNextRetryTime, LocalDateTime.now()))
                        .orderByAsc(ApsSyncQueue::getPriority)
                        .orderByAsc(ApsSyncQueue::getCreatedTime)
                        .last("LIMIT " + batchSize));

        int totalCount = pendingItems.size();
        int successCount = 0, failCount = 0;

        for (ApsSyncQueue item : pendingItems) {
            try {
                // 标记为处理中
                item.setSyncStatus(SyncStatus.PROCESSING.getCode());
                item.setUpdatedTime(LocalDateTime.now());
                syncQueueMapper.updateById(item);

                // 根据同步类型推送到不同的 APS 端点
                pushToAps(item);

                // 标记为已同步
                item.setSyncStatus(SyncStatus.SYNCED.getCode());
                item.setUpdatedTime(LocalDateTime.now());
                syncQueueMapper.updateById(item);
                successCount++;
            } catch (Exception e) {
                failCount++;
                handleSyncFailure(item, e);
                log.error("上行同步失败: id={}, type={}, error={}", item.getId(), item.getSyncType(), e.getMessage());
            }
        }

        syncLogService.completeLog(syncLog.getId(), totalCount, successCount, failCount, null);

        log.info("APS 上行同步完成: total={}, success={}, fail={}", totalCount, successCount, failCount);
        return ApsSyncResultVO.builder()
                .batchId(batchId)
                .status(failCount == 0 ? SyncStatus.SUCCESS.getCode() :
                        (successCount == 0 ? SyncStatus.FAIL.getCode() : SyncStatus.PARTIAL.getCode()))
                .totalCount(totalCount)
                .successCount(successCount)
                .failCount(failCount)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enqueue(String syncType, String dataType, Long dataId, String dataNo, int priority, String payload) {
        ApsSyncQueue queue = new ApsSyncQueue();
        queue.setSyncDirection(SyncDirection.UPSTREAM.getCode());
        queue.setSyncType(syncType);
        queue.setDataType(dataType);
        queue.setDataId(dataId);
        queue.setDataNo(dataNo);
        queue.setPriority(priority);
        queue.setSyncStatus(SyncStatus.PENDING.getCode());
        queue.setRetryCount(0);
        queue.setMaxRetry(configService.getIntConfig("aps.sync.retry.max", 3));
        queue.setPayload(payload);
        queue.setCreatedTime(LocalDateTime.now());
        queue.setUpdatedTime(LocalDateTime.now());
        syncQueueMapper.insert(queue);

        log.debug("写入APS上行同步队列: type={}, dataNo={}", syncType, dataNo);
    }

    @Override
    public long getPendingCount() {
        return syncQueueMapper.selectCount(
                new LambdaQueryWrapper<ApsSyncQueue>()
                        .eq(ApsSyncQueue::getSyncStatus, SyncStatus.PENDING.getCode()));
    }

    // ==================== 私有方法 ====================

    private void pushToAps(ApsSyncQueue item) {
        String syncType = item.getSyncType();

        switch (syncType) {
            case "WORKORDER" -> {
                apsClient.post("/api/mes/status/sync",
                        buildPayload(item), Map.class);
            }
            case "INVENTORY" -> {
                apsClient.post("/api/mes/inventory/sync",
                        buildPayload(item), Map.class);
            }
            case "QUALITY" -> {
                apsClient.post("/api/mes/quality/sync",
                        buildPayload(item), Map.class);
            }
            case "OUTSOURCE" -> {
                apsClient.post("/api/mes/outsource/status",
                        buildPayload(item), Map.class);
            }
            case "TRANSFER" -> {
                apsClient.post("/api/mes/transfer/status",
                        buildPayload(item), Map.class);
            }
            case "ABNORMAL" -> {
                apsClient.post("/api/schedule/combined",
                        buildPayload(item), Map.class);
            }
            default -> {
                log.warn("未知的同步类型: {}", syncType);
                throw new RuntimeException("未知的同步类型: " + syncType);
            }
        }
    }

    private Map<String, Object> buildPayload(ApsSyncQueue item) {
        try {
            if (item.getPayload() != null) {
                return objectMapper.readValue(item.getPayload(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("解析同步载荷失败: {}", e.getMessage());
        }
        // 回退到基本信息
        return Map.of(
                "dataType", item.getDataType(),
                "dataId", item.getDataId() != null ? item.getDataId() : 0,
                "dataNo", item.getDataNo() != null ? item.getDataNo() : ""
        );
    }

    private void handleSyncFailure(ApsSyncQueue item, Exception e) {
        int retryCount = item.getRetryCount() + 1;
        item.setRetryCount(retryCount);
        item.setErrorMessage(e.getMessage());
        item.setUpdatedTime(LocalDateTime.now());

        if (retryCount >= item.getMaxRetry()) {
            // 超出最大重试次数，标记为失败
            item.setSyncStatus(SyncStatus.FAILED.getCode());
        } else {
            // 回退为待处理，设置下次重试时间（指数退避：5s, 15s, 30s）
            item.setSyncStatus(SyncStatus.PENDING.getCode());
            int[] intervals = {5, 15, 30};
            int delay = intervals[Math.min(retryCount - 1, intervals.length - 1)];
            item.setNextRetryTime(LocalDateTime.now().plusSeconds(delay));
        }

        syncQueueMapper.updateById(item);
    }
}
