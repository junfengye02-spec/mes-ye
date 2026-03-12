package com.mes.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.aps.client.ApsClient;
import com.mes.aps.domain.entity.*;
import com.mes.aps.domain.vo.ApsSyncResultVO;
import com.mes.aps.enums.SyncDirection;
import com.mes.aps.enums.SyncStatus;
import com.mes.aps.enums.SyncType;
import com.mes.aps.mapper.*;
import com.mes.aps.service.IApsDataMappingService;
import com.mes.aps.service.IApsDownstreamSyncService;
import com.mes.aps.service.IApsSyncConfigService;
import com.mes.aps.service.IApsSyncLogService;
import com.mes.plan.domain.entity.OrderPlan;
import com.mes.plan.mapper.OrderPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * APS 下行同步服务实现
 * <p>负责从 APS 拉取排程数据并写入 MES 表</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApsDownstreamSyncServiceImpl implements IApsDownstreamSyncService {

    private final ApsClient apsClient;
    private final IApsSyncConfigService configService;
    private final IApsSyncLogService syncLogService;
    private final IApsDataMappingService dataMappingService;
    private final OrderPlanMapper orderPlanMapper;
    private final OutsourceOrderMapper outsourceOrderMapper;
    private final TransferOrderMapper transferOrderMapper;
    private final ResourceCalendarMapper resourceCalendarMapper;
    private final ResourceCalendarShiftMapper resourceCalendarShiftMapper;
    private final WorkOrderTaskSegmentMapper taskSegmentMapper;
    private final ApsSyncDetailMapper syncDetailMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApsSyncResultVO syncOrders() {
        String batchId = UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), SyncType.ORDER.getCode());

        int totalCount = 0, successCount = 0, failCount = 0;
        String errorMessage = null;

        try {
            if (!configService.getBooleanConfig("aps.sync.downstream.enabled", true)) {
                log.info("APS 下行同步已关闭，跳过排程订单同步");
                return buildSkippedResult(batchId);
            }

            // 从 APS 拉取排程订单
            JsonNode response = apsClient.get("/api/orders", JsonNode.class);
            if (response == null || !response.isArray()) {
                log.warn("APS 排程订单返回数据为空或格式错误");
                syncLogService.completeLog(syncLog.getId(), 0, 0, 0, "APS返回数据为空");
                return buildResult(batchId, SyncStatus.SUCCESS.getCode(), 0, 0, 0);
            }

            totalCount = response.size();

            for (JsonNode orderNode : response) {
                try {
                    syncSingleOrder(orderNode, batchId);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("同步排程订单失败: {}", e.getMessage());
                    // 记录明细
                    saveSyncDetail(batchId, SyncType.ORDER.getCode(),
                            orderNode.path("id").asLong(),
                            orderNode.path("orderNo").asText(),
                            SyncStatus.FAILED.getCode(), e.getMessage(), orderNode.toString());
                }
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("APS 排程订单同步异常: {}", e.getMessage(), e);
        }

        syncLogService.completeLog(syncLog.getId(), totalCount, successCount, failCount, errorMessage);
        return buildResult(batchId, determineStatus(successCount, failCount),
                totalCount, successCount, failCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApsSyncResultVO syncTasks() {
        String batchId = UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), SyncType.TASK.getCode());

        int totalCount = 0, successCount = 0, failCount = 0;
        String errorMessage = null;

        try {
            if (!configService.getBooleanConfig("aps.sync.downstream.enabled", true)) {
                return buildSkippedResult(batchId);
            }

            JsonNode response = apsClient.get("/api/tasks", JsonNode.class);
            if (response == null || !response.isArray()) {
                syncLogService.completeLog(syncLog.getId(), 0, 0, 0, "APS返回数据为空");
                return buildResult(batchId, SyncStatus.SUCCESS.getCode(), 0, 0, 0);
            }

            totalCount = response.size();

            for (JsonNode taskNode : response) {
                try {
                    syncSingleTask(taskNode, batchId);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("同步排程任务失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("APS 排程任务同步异常: {}", e.getMessage(), e);
        }

        syncLogService.completeLog(syncLog.getId(), totalCount, successCount, failCount, errorMessage);
        return buildResult(batchId, determineStatus(successCount, failCount),
                totalCount, successCount, failCount);
    }

    @Override
    public ApsSyncResultVO syncResources() {
        String batchId = UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), SyncType.RESOURCE.getCode());

        try {
            if (!configService.getBooleanConfig("aps.sync.downstream.enabled", true)) {
                return buildSkippedResult(batchId);
            }

            // 资源同步：拉取 APS 资源列表，更新数据映射
            JsonNode response = apsClient.get("/api/resources", JsonNode.class);
            int count = response != null && response.isArray() ? response.size() : 0;
            log.info("APS 资源同步完成: 获取{}条资源", count);

            syncLogService.completeLog(syncLog.getId(), count, count, 0, null);
            return buildResult(batchId, SyncStatus.SUCCESS.getCode(), count, count, 0);
        } catch (Exception e) {
            log.error("APS 资源同步异常: {}", e.getMessage(), e);
            syncLogService.completeLog(syncLog.getId(), 0, 0, 0, e.getMessage());
            return buildResult(batchId, SyncStatus.FAIL.getCode(), 0, 0, 0);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApsSyncResultVO syncCalendars() {
        String batchId = UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), SyncType.CALENDAR.getCode());

        int totalCount = 0, successCount = 0, failCount = 0;

        try {
            if (!configService.getBooleanConfig("aps.sync.downstream.enabled", true)) {
                return buildSkippedResult(batchId);
            }

            JsonNode response = apsClient.get("/api/resource-calendars", JsonNode.class);
            if (response == null || !response.isArray()) {
                syncLogService.completeLog(syncLog.getId(), 0, 0, 0, null);
                return buildResult(batchId, SyncStatus.SUCCESS.getCode(), 0, 0, 0);
            }

            totalCount = response.size();

            for (JsonNode calendarNode : response) {
                try {
                    syncSingleCalendar(calendarNode);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("同步资源日历失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("APS 资源日历同步异常: {}", e.getMessage(), e);
        }

        syncLogService.completeLog(syncLog.getId(), totalCount, successCount, failCount, null);
        return buildResult(batchId, determineStatus(successCount, failCount),
                totalCount, successCount, failCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApsSyncResultVO syncOutsourceOrders() {
        String batchId = UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), SyncType.OUTSOURCE.getCode());

        int totalCount = 0, successCount = 0, failCount = 0;

        try {
            if (!configService.getBooleanConfig("aps.sync.downstream.enabled", true)) {
                return buildSkippedResult(batchId);
            }

            JsonNode response = apsClient.get("/api/outsource-orders", JsonNode.class);
            if (response == null || !response.isArray()) {
                syncLogService.completeLog(syncLog.getId(), 0, 0, 0, null);
                return buildResult(batchId, SyncStatus.SUCCESS.getCode(), 0, 0, 0);
            }

            totalCount = response.size();

            for (JsonNode node : response) {
                try {
                    syncSingleOutsourceOrder(node);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("同步外协订单失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("APS 外协订单同步异常: {}", e.getMessage(), e);
        }

        syncLogService.completeLog(syncLog.getId(), totalCount, successCount, failCount, null);
        return buildResult(batchId, determineStatus(successCount, failCount),
                totalCount, successCount, failCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApsSyncResultVO syncTransferOrders() {
        String batchId = UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), SyncType.TRANSFER.getCode());

        int totalCount = 0, successCount = 0, failCount = 0;

        try {
            if (!configService.getBooleanConfig("aps.sync.downstream.enabled", true)) {
                return buildSkippedResult(batchId);
            }

            JsonNode response = apsClient.get("/api/transfer-orders", JsonNode.class);
            if (response == null || !response.isArray()) {
                syncLogService.completeLog(syncLog.getId(), 0, 0, 0, null);
                return buildResult(batchId, SyncStatus.SUCCESS.getCode(), 0, 0, 0);
            }

            totalCount = response.size();

            for (JsonNode node : response) {
                try {
                    syncSingleTransferOrder(node);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("同步转厂订单失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("APS 转厂订单同步异常: {}", e.getMessage(), e);
        }

        syncLogService.completeLog(syncLog.getId(), totalCount, successCount, failCount, null);
        return buildResult(batchId, determineStatus(successCount, failCount),
                totalCount, successCount, failCount);
    }

    @Override
    public ApsSyncResultVO syncAll() {
        log.info("========== APS 全量下行同步开始 ==========");
        int totalSuccess = 0, totalFail = 0, totalCount = 0;

        ApsSyncResultVO ordersResult = syncOrders();
        totalCount += ordersResult.getTotalCount();
        totalSuccess += ordersResult.getSuccessCount();
        totalFail += ordersResult.getFailCount();

        ApsSyncResultVO tasksResult = syncTasks();
        totalCount += tasksResult.getTotalCount();
        totalSuccess += tasksResult.getSuccessCount();
        totalFail += tasksResult.getFailCount();

        ApsSyncResultVO resourcesResult = syncResources();
        totalCount += resourcesResult.getTotalCount();
        totalSuccess += resourcesResult.getSuccessCount();
        totalFail += resourcesResult.getFailCount();

        ApsSyncResultVO calendarsResult = syncCalendars();
        totalCount += calendarsResult.getTotalCount();
        totalSuccess += calendarsResult.getSuccessCount();
        totalFail += calendarsResult.getFailCount();

        ApsSyncResultVO outsourceResult = syncOutsourceOrders();
        totalCount += outsourceResult.getTotalCount();
        totalSuccess += outsourceResult.getSuccessCount();
        totalFail += outsourceResult.getFailCount();

        ApsSyncResultVO transferResult = syncTransferOrders();
        totalCount += transferResult.getTotalCount();
        totalSuccess += transferResult.getSuccessCount();
        totalFail += transferResult.getFailCount();

        log.info("========== APS 全量下行同步完成: total={}, success={}, fail={} ==========",
                totalCount, totalSuccess, totalFail);

        return buildResult(UUID.randomUUID().toString(),
                determineStatus(totalSuccess, totalFail), totalCount, totalSuccess, totalFail);
    }

    // ==================== 单条同步逻辑 ====================

    private void syncSingleOrder(JsonNode orderNode, String batchId) {
        Long apsOrderId = orderNode.path("id").asLong();
        String orderNo = orderNode.path("orderNo").asText(null);

        // 查找是否已存在（增量同步）
        OrderPlan existing = orderPlanMapper.selectOne(
                new LambdaQueryWrapper<OrderPlan>()
                        .eq(OrderPlan::getApsOrderId, apsOrderId));

        if (existing != null) {
            // 更新现有订单
            existing.setProductCode(orderNode.path("productCode").asText(existing.getProductCode()));
            existing.setProductName(orderNode.path("productName").asText(existing.getProductName()));
            existing.setPlanQty(orderNode.has("planQty") ?
                    new java.math.BigDecimal(orderNode.path("planQty").asText("0")) : existing.getPlanQty());
            existing.setApsSyncBatchId(batchId);
            existing.setApsSyncStatus(SyncStatus.SYNCED.getCode());
            orderPlanMapper.updateById(existing);
            log.debug("更新排程订单: apsOrderId={}, orderNo={}", apsOrderId, orderNo);
        } else {
            // 新增订单
            OrderPlan newPlan = new OrderPlan();
            newPlan.setApsOrderId(apsOrderId);
            newPlan.setOrderNo(orderNo);
            newPlan.setProductCode(orderNode.path("productCode").asText(null));
            newPlan.setProductName(orderNode.path("productName").asText(null));
            newPlan.setPlanQty(orderNode.has("planQty") ?
                    new java.math.BigDecimal(orderNode.path("planQty").asText("0")) : null);
            newPlan.setDataSource("APS");
            newPlan.setStatus("CREATED");
            newPlan.setApsSyncBatchId(batchId);
            newPlan.setApsSyncStatus(SyncStatus.SYNCED.getCode());
            orderPlanMapper.insert(newPlan);
            log.debug("新增排程订单: apsOrderId={}, orderNo={}", apsOrderId, orderNo);
        }

        // 记录同步明细
        saveSyncDetail(batchId, SyncType.ORDER.getCode(), apsOrderId, orderNo,
                SyncStatus.SUCCESS.getCode(), null, orderNode.toString());
    }

    private void syncSingleTask(JsonNode taskNode, String batchId) {
        // 同步任务分段（TaskSegment）
        JsonNode segments = taskNode.path("segments");
        Long taskId = taskNode.path("id").asLong();

        if (segments.isArray()) {
            for (JsonNode seg : segments) {
                Integer segIndex = seg.path("segmentIndex").asInt(1);

                WorkOrderTaskSegment existing = taskSegmentMapper.selectOne(
                        new LambdaQueryWrapper<WorkOrderTaskSegment>()
                                .eq(WorkOrderTaskSegment::getWorkOrderTaskId, taskId)
                                .eq(WorkOrderTaskSegment::getSegmentIndex, segIndex));

                if (existing != null) {
                    existing.setShiftName(seg.path("shiftName").asText(existing.getShiftName()));
                    existing.setUpdatedTime(LocalDateTime.now());
                    taskSegmentMapper.updateById(existing);
                } else {
                    WorkOrderTaskSegment segment = new WorkOrderTaskSegment();
                    segment.setWorkOrderTaskId(taskId);
                    segment.setSegmentIndex(segIndex);
                    segment.setShiftName(seg.path("shiftName").asText(null));
                    segment.setSegmentDuration(seg.path("duration").asInt(0));
                    segment.setStatus("PENDING");
                    segment.setCreatedTime(LocalDateTime.now());
                    segment.setUpdatedTime(LocalDateTime.now());
                    taskSegmentMapper.insert(segment);
                }
            }
        }

        log.debug("同步排程任务: taskId={}", taskId);
    }

    private void syncSingleCalendar(JsonNode calendarNode) {
        Long apsCalendarId = calendarNode.path("id").asLong();

        ResourceCalendar existing = resourceCalendarMapper.selectOne(
                new LambdaQueryWrapper<ResourceCalendar>()
                        .eq(ResourceCalendar::getApsCalendarId, apsCalendarId));

        if (existing != null) {
            existing.setCalendarName(calendarNode.path("name").asText(existing.getCalendarName()));
            existing.setUpdatedTime(LocalDateTime.now());
            resourceCalendarMapper.updateById(existing);
        } else {
            ResourceCalendar calendar = new ResourceCalendar();
            calendar.setApsCalendarId(apsCalendarId);
            calendar.setCalendarName(calendarNode.path("name").asText(null));
            calendar.setCreatedTime(LocalDateTime.now());
            calendar.setUpdatedTime(LocalDateTime.now());
            resourceCalendarMapper.insert(calendar);
        }

        // 同步班次
        JsonNode shifts = calendarNode.path("shifts");
        if (shifts.isArray()) {
            Long calendarId = existing != null ? existing.getId() :
                    resourceCalendarMapper.selectOne(
                            new LambdaQueryWrapper<ResourceCalendar>()
                                    .eq(ResourceCalendar::getApsCalendarId, apsCalendarId)).getId();

            for (JsonNode shiftNode : shifts) {
                ResourceCalendarShift shift = new ResourceCalendarShift();
                shift.setCalendarId(calendarId);
                shift.setDayOfWeek(shiftNode.path("dayOfWeek").asText(null));
                shift.setShiftName(shiftNode.path("shiftName").asText(null));
                shift.setCreatedTime(LocalDateTime.now());
                shift.setUpdatedTime(LocalDateTime.now());
                resourceCalendarShiftMapper.insert(shift);
            }
        }
    }

    private void syncSingleOutsourceOrder(JsonNode node) {
        String orderNo = node.path("outsourceOrderNo").asText(null);

        OutsourceOrder existing = outsourceOrderMapper.selectOne(
                new LambdaQueryWrapper<OutsourceOrder>()
                        .eq(OutsourceOrder::getOutsourceOrderNo, orderNo));

        if (existing != null) {
            existing.setApsStatus(node.path("status").asText(existing.getApsStatus()));
            outsourceOrderMapper.updateById(existing);
        } else {
            OutsourceOrder order = new OutsourceOrder();
            order.setOutsourceOrderNo(orderNo);
            order.setParentOrderNo(node.path("parentOrderNo").asText(null));
            order.setApsOrderId(node.path("apsOrderId").asLong(0));
            order.setMaterialCode(node.path("materialCode").asText(null));
            order.setMaterialName(node.path("materialName").asText(null));
            order.setSupplierName(node.path("supplierName").asText(null));
            order.setApsStatus(node.path("status").asText(null));
            order.setMesStatus("PENDING");
            outsourceOrderMapper.insert(order);
        }
    }

    private void syncSingleTransferOrder(JsonNode node) {
        String transferNo = node.path("transferNo").asText(null);

        TransferOrder existing = transferOrderMapper.selectOne(
                new LambdaQueryWrapper<TransferOrder>()
                        .eq(TransferOrder::getTransferNo, transferNo));

        if (existing != null) {
            existing.setApsStatus(node.path("status").asText(existing.getApsStatus()));
            transferOrderMapper.updateById(existing);
        } else {
            TransferOrder order = new TransferOrder();
            order.setTransferNo(transferNo);
            order.setParentOrderNo(node.path("parentOrderNo").asText(null));
            order.setMaterialCode(node.path("materialCode").asText(null));
            order.setMaterialName(node.path("materialName").asText(null));
            order.setFromFactory(node.path("fromFactory").asText(null));
            order.setToFactory(node.path("toFactory").asText(null));
            order.setApsStatus(node.path("status").asText(null));
            order.setMesStatus("PENDING");
            transferOrderMapper.insert(order);
        }
    }

    // ==================== 辅助方法 ====================

    private void saveSyncDetail(String batchId, String dataType, Long dataId,
                                 String dataNo, String status, String errorMsg, String apsData) {
        ApsSyncDetail detail = new ApsSyncDetail();
        detail.setBatchId(batchId);
        detail.setDataType(dataType);
        detail.setDataId(dataId);
        detail.setDataNo(dataNo);
        detail.setSyncAction(dataId != null ? "UPDATE" : "CREATE");
        detail.setSyncStatus(status);
        detail.setApsData(apsData);
        detail.setErrorMessage(errorMsg);
        detail.setRetryCount(0);
        detail.setCreatedTime(LocalDateTime.now());
        detail.setUpdatedTime(LocalDateTime.now());
        syncDetailMapper.insert(detail);
    }

    private String determineStatus(int successCount, int failCount) {
        if (failCount == 0) return SyncStatus.SUCCESS.getCode();
        if (successCount == 0) return SyncStatus.FAIL.getCode();
        return SyncStatus.PARTIAL.getCode();
    }

    private ApsSyncResultVO buildResult(String batchId, String status,
                                         int totalCount, int successCount, int failCount) {
        return ApsSyncResultVO.builder()
                .batchId(batchId)
                .status(status)
                .totalCount(totalCount)
                .successCount(successCount)
                .failCount(failCount)
                .build();
    }

    private ApsSyncResultVO buildSkippedResult(String batchId) {
        return ApsSyncResultVO.builder()
                .batchId(batchId)
                .status("SKIPPED")
                .message("下行同步已关闭")
                .build();
    }
}
