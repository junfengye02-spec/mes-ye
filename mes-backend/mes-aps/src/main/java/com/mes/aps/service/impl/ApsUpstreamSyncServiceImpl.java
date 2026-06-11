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
import com.mes.plan.domain.entity.OrderPlan;
import com.mes.plan.mapper.OrderPlanMapper;
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
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
    private final OrderPlanMapper orderPlanMapper;
    private final WorkOrderMapper workOrderMapper;

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
                if (isUnsupportedUpstreamType(item)) {
                    failCount++;
                    markUnsupportedTypeFailed(item);
                    log.warn("跳过APS不支持的同步类型: id={}, type={}, dataNo={}",
                            item.getId(), item.getSyncType(), item.getDataNo());
                    continue;
                }

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
        if (!SyncType.isUpstreamQueueSupported(syncType)) {
            log.info("APS暂不支持该上行同步类型，跳过入队: type={}, dataNo={}", syncType, dataNo);
            return;
        }

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

    private boolean isUnsupportedUpstreamType(ApsSyncQueue item) {
        return !SyncType.isUpstreamQueueSupported(item.getSyncType());
    }

    private void markUnsupportedTypeFailed(ApsSyncQueue item) {
        item.setSyncStatus(SyncStatus.FAILED.getCode());
        item.setErrorMessage("APS不支持当前同步类型: " + item.getSyncType());
        item.setUpdatedTime(LocalDateTime.now());
        syncQueueMapper.updateById(item);
    }

    private void pushToAps(ApsSyncQueue item) {
        SyncType syncType = SyncType.fromCode(item.getSyncType())
                .orElseThrow(() -> new RuntimeException("未知的同步类型: " + item.getSyncType()));
        Map<String, Object> payload = buildPayload(item);

        if (syncType.isAsyncUpstreamContractCall()) {
            apsClient.postAsync(syncType.requireUpstreamContractEndpoint(), payload);
        } else {
            apsClient.post(syncType.requireUpstreamContractEndpoint(), payload, Map.class);
        }
    }

    private Map<String, Object> buildPayload(ApsSyncQueue item) {
        try {
            if (item.getPayload() != null) {
                Map<String, Object> parsed = objectMapper.readValue(item.getPayload(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                enrichPayload(parsed, item);
                return parsed;
            }
        } catch (Exception e) {
            log.warn("解析同步载荷失败: {}", e.getMessage());
        }
        return buildFallbackPayload(item);
    }

    private void enrichPayload(Map<String, Object> payload, ApsSyncQueue item) {
        switch (item.getSyncType()) {
            case "WORKORDER" -> {
                if (!payload.containsKey("apsOrderId") && payload.containsKey("workOrderNo")) {
                    Long apsOrderId = lookupApsOrderId(String.valueOf(payload.get("workOrderNo")));
                    if (apsOrderId != null) {
                        payload.put("apsOrderId", apsOrderId);
                    }
                }
            }
            default -> { /* 其他类型暂不做额外补充 */ }
        }
    }

    private Long lookupApsOrderId(String workOrderNo) {
        try {
            WorkOrder wo = workOrderMapper.selectOne(
                    new LambdaQueryWrapper<WorkOrder>()
                            .eq(WorkOrder::getWorkOrderNo, workOrderNo)
                            .last("LIMIT 1"));
            if (wo == null || wo.getOrderPlanNo() == null) return null;

            OrderPlan plan = orderPlanMapper.selectOne(
                    new LambdaQueryWrapper<OrderPlan>()
                            .eq(OrderPlan::getOrderNo, wo.getOrderPlanNo())
                            .last("LIMIT 1"));
            return plan != null ? plan.getApsOrderId() : null;
        } catch (Exception e) {
            log.warn("查询 apsOrderId 失败: workOrderNo={}", workOrderNo);
            return null;
        }
    }

    private Map<String, Object> buildFallbackPayload(ApsSyncQueue item) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("dataType", item.getDataType());
        fallback.put("dataId", item.getDataId() != null ? item.getDataId() : 0);
        fallback.put("dataNo", item.getDataNo() != null ? item.getDataNo() : "");
        return fallback;
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
