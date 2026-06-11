package com.mes.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.aps.domain.entity.ApsCapacityLoad;
import com.mes.aps.domain.entity.ApsGanttCache;
import com.mes.aps.domain.entity.ApsSyncLog;
import com.mes.aps.domain.vo.*;
import com.mes.aps.enums.SyncDirection;
import com.mes.aps.enums.SyncStatus;
import com.mes.aps.enums.SyncType;
import com.mes.aps.mapper.ApsCapacityLoadMapper;
import com.mes.aps.mapper.ApsGanttCacheMapper;
import com.mes.aps.service.ApsCallbackIdempotencyService;
import com.mes.aps.service.IApsExtendedCallbackService;
import com.mes.aps.service.IApsSyncLogService;
import com.mes.basic.domain.entity.Material;
import com.mes.basic.mapper.MaterialMapper;
import com.mes.common.utils.AssertUtil;
import com.mes.dispatch.domain.entity.DispatchAssignment;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.enums.DispatchStatus;
import com.mes.dispatch.mapper.DispatchAssignmentMapper;
import com.mes.dispatch.mapper.DispatchTaskMapper;
import com.mes.dispatch.service.IDispatchTaskService;
import com.mes.material.domain.dto.MaterialRequisitionDTO;
import com.mes.material.domain.dto.MaterialRequisitionItemDTO;
import com.mes.material.service.IMaterialRequisitionService;
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApsExtendedCallbackServiceImpl implements IApsExtendedCallbackService {

    private final IApsSyncLogService syncLogService;
    private final ObjectMapper objectMapper;
    private final WorkOrderMapper workOrderMapper;
    private final DispatchTaskMapper dispatchTaskMapper;
    private final DispatchAssignmentMapper dispatchAssignmentMapper;
    private final IDispatchTaskService dispatchTaskService;
    private final ApsCallbackIdempotencyService idempotencyService;
    private final ApsGanttCacheMapper ganttCacheMapper;
    private final ApsCapacityLoadMapper capacityLoadMapper;
    private final MaterialMapper materialMapper;
    private final IMaterialRequisitionService materialRequisitionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleMrpResult(ApsMrpCallbackVO mrpData) {
        if (!idempotencyService.tryAcquire("mrpResult", mrpData.getRequestId())) {
            log.warn("重复回调忽略: type=mrpResult, requestId={}", mrpData.getRequestId());
            return;
        }
        String batchId = mrpData.getRequestId() != null ? mrpData.getRequestId() : UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), SyncType.MRP.getCode());

        int totalCount = mrpData.getItems() != null ? mrpData.getItems().size() : 0;
        int successCount = 0;
        int failCount = 0;

        try {
            if (mrpData.getItems() == null || mrpData.getItems().isEmpty()) {
                syncLogService.completeLog(syncLog.getId(), 0, 0, 0, null);
                return;
            }

            Map<String, WorkOrder> workOrderCache = new HashMap<>();
            Map<String, Material> materialCache = new HashMap<>();
            Map<String, List<ApsMrpCallbackVO.MrpItem>> groupedItems = groupMrpItemsByWorkOrder(mrpData.getItems());

            for (Map.Entry<String, List<ApsMrpCallbackVO.MrpItem>> entry : groupedItems.entrySet()) {
                List<ApsMrpCallbackVO.MrpItem> items = entry.getValue();
                try {
                    MaterialRequisitionDTO dto = buildMrpRequisition(entry.getKey(), items, workOrderCache, materialCache);
                    materialRequisitionService.createFromMrp(dto);
                    successCount += items.size();
                } catch (Exception e) {
                    failCount += items.size();
                    log.error("APS MRP生成领料申请失败: workOrderNo={}, error={}",
                            entry.getKey(), e.getMessage(), e);
                }
            }

            log.info("接收APS物料需求计划: batchId={}, 共{}条物料需求, 成功{}, 失败{}",
                    batchId, totalCount, successCount, failCount);
            syncLogService.completeLog(syncLog.getId(), totalCount, successCount, failCount, null);
        } catch (Exception e) {
            log.error("处理MRP结果失败: {}", e.getMessage(), e);
            syncLogService.completeLog(syncLog.getId(), totalCount, successCount,
                    Math.max(failCount, totalCount - successCount), e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleResourceAllocation(ApsResourceAllocationVO allocationData) {
        if (!idempotencyService.tryAcquire("resourceAllocation", allocationData.getRequestId())) {
            log.warn("重复回调忽略: type=resourceAllocation, requestId={}", allocationData.getRequestId());
            return;
        }
        String batchId = allocationData.getRequestId() != null ? allocationData.getRequestId() : UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), SyncType.RESOURCE_ALLOCATION.getCode());

        int successCount = 0, failCount = 0;

        try {
            if (allocationData.getItems() == null) {
                syncLogService.completeLog(syncLog.getId(), 0, 0, 0, null);
                return;
            }

            for (ApsResourceAllocationVO.AllocationItem item : allocationData.getItems()) {
                try {
                    applyResourceAllocation(item);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("资源分配处理失败: workOrderNo={}, error={}", item.getWorkOrderNo(), e.getMessage());
                }
            }

            log.info("资源分配计划处理完成: success={}, fail={}", successCount, failCount);
            syncLogService.completeLog(syncLog.getId(), successCount + failCount, successCount, failCount, null);
        } catch (Exception e) {
            log.error("处理资源分配计划失败: {}", e.getMessage(), e);
            syncLogService.completeLog(syncLog.getId(), 0, 0, 0, e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleGanttData(ApsGanttDataVO ganttData) {
        if (!idempotencyService.tryAcquire("ganttData", ganttData.getRequestId())) {
            log.warn("重复回调忽略: type=ganttData, requestId={}", ganttData.getRequestId());
            return;
        }
        String batchId = ganttData.getRequestId() != null ? ganttData.getRequestId() : UUID.randomUUID().toString();
        String scheduleBatchId = resolveScheduleBatchId(ganttData.getScheduleBatchId(), batchId);
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), SyncType.GANTT.getCode());

        try {
            List<ApsGanttCache> caches = buildGanttCaches(ganttData, scheduleBatchId);
            int count = caches.size();

            ganttCacheMapper.delete(new LambdaQueryWrapper<ApsGanttCache>()
                    .eq(ApsGanttCache::getScheduleBatchId, scheduleBatchId));
            for (ApsGanttCache cache : caches) {
                ganttCacheMapper.insert(cache);
            }

            log.info("接收APS甘特图数据: batchId={}, 共{}条任务, 范围: {} ~ {}",
                    batchId, count, ganttData.getRangeStart(), ganttData.getRangeEnd());

            syncLogService.completeLog(syncLog.getId(), count, count, 0, null);
        } catch (Exception e) {
            log.error("处理甘特图数据失败: {}", e.getMessage(), e);
            syncLogService.completeLog(syncLog.getId(), 0, 0, 0, e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCapacityLoad(ApsCapacityLoadVO capacityData) {
        if (!idempotencyService.tryAcquire("capacityLoad", capacityData.getRequestId())) {
            log.warn("重复回调忽略: type=capacityLoad, requestId={}", capacityData.getRequestId());
            return;
        }
        String batchId = capacityData.getRequestId() != null ? capacityData.getRequestId() : UUID.randomUUID().toString();
        String scheduleBatchId = resolveScheduleBatchId(capacityData.getScheduleBatchId(), batchId);
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), SyncType.CAPACITY_LOAD.getCode());

        try {
            List<ApsCapacityLoad> loads = buildCapacityLoads(capacityData, scheduleBatchId);
            int count = loads.size();
            long overloadedCount = loads.stream().filter(item -> Boolean.TRUE.equals(item.getOverloaded())).count();

            capacityLoadMapper.delete(new LambdaQueryWrapper<ApsCapacityLoad>()
                    .eq(ApsCapacityLoad::getScheduleBatchId, scheduleBatchId));
            for (ApsCapacityLoad load : loads) {
                capacityLoadMapper.insert(load);
            }

            log.info("接收APS产能负荷数据: batchId={}, 共{}条, 超负荷{}条",
                    batchId, count, overloadedCount);
            if (overloadedCount > 0) {
                log.warn("APS产能负荷存在超负荷工作中心: batchId={}, overloaded={}",
                        batchId, overloadedCount);
            }

            syncLogService.completeLog(syncLog.getId(), count, count, 0, null);
        } catch (Exception e) {
            log.error("处理产能负荷数据失败: {}", e.getMessage(), e);
            syncLogService.completeLog(syncLog.getId(), 0, 0, 0, e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleScheduleChange(ApsScheduleChangeVO changeData) {
        if (!idempotencyService.tryAcquire("scheduleChange", changeData.getRequestId())) {
            log.warn("重复回调忽略: type=scheduleChange, requestId={}", changeData.getRequestId());
            return;
        }
        String batchId = changeData.getRequestId() != null ? changeData.getRequestId() : UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), SyncType.SCHEDULE_CHANGE.getCode());

        int successCount = 0, failCount = 0;

        try {
            if (changeData.getAffectedOrders() == null) {
                syncLogService.completeLog(syncLog.getId(), 0, 0, 0, null);
                return;
            }

            for (ApsScheduleChangeVO.AffectedOrder affected : changeData.getAffectedOrders()) {
                try {
                    applyScheduleChange(changeData, affected);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("排程变更处理失败: workOrderNo={}, error={}", affected.getWorkOrderNo(), e.getMessage());
                }
            }

            log.info("排程变更通知处理完成: reason={}, success={}, fail={}",
                    changeData.getChangeReason(), successCount, failCount);
            syncLogService.completeLog(syncLog.getId(), successCount + failCount, successCount, failCount, null);
        } catch (Exception e) {
            log.error("处理排程变更通知失败: {}", e.getMessage(), e);
            syncLogService.completeLog(syncLog.getId(), 0, 0, 0, e.getMessage());
        }
    }

    private void applyResourceAllocation(ApsResourceAllocationVO.AllocationItem item) {
        DispatchTask task = dispatchTaskMapper.selectOne(
                new LambdaQueryWrapper<DispatchTask>()
                        .eq(DispatchTask::getOrderNo, item.getWorkOrderNo())
                        .eq(DispatchTask::getProcessNo, item.getProcessNo())
                        .last("LIMIT 1"));

        if (task == null) {
            log.warn("未找到对应派工任务: workOrderNo={}, processNo={}", item.getWorkOrderNo(), item.getProcessNo());
            return;
        }

        task.setPlanStartTime(item.getPlanStartTime());
        task.setPlanEndTime(item.getPlanEndTime());
        task.setUpdatedTime(LocalDateTime.now());
        dispatchTaskMapper.updateById(task);

        if (item.getAssigneeCode() != null) {
            DispatchAssignment assignment = new DispatchAssignment();
            assignment.setDispatchTaskId(task.getId());
            assignment.setAssignType(item.getAssignType());
            assignment.setAssigneeCode(item.getAssigneeCode());
            assignment.setAssigneeName(item.getAssigneeName());
            assignment.setAssignedQty(item.getAssignedQty());
            assignment.setStatus("ACTIVE");
            assignment.setAssignedBy("APS");
            assignment.setAssignedTime(LocalDateTime.now());
            dispatchAssignmentMapper.insert(assignment);
        }
    }

    private void applyScheduleChange(ApsScheduleChangeVO changeData, ApsScheduleChangeVO.AffectedOrder affected) {
        WorkOrder wo = workOrderMapper.selectOne(
                new LambdaQueryWrapper<WorkOrder>()
                        .eq(WorkOrder::getWorkOrderNo, affected.getWorkOrderNo())
                        .last("LIMIT 1"));

        if (wo == null) {
            log.warn("排程变更: 未找到工单 {}", affected.getWorkOrderNo());
            return;
        }

        switch (affected.getChangeType()) {
            case "TIME_CHANGED" -> {
                if (affected.getNewStartTime() != null) wo.setPlanStartTime(affected.getNewStartTime());
                if (affected.getNewEndTime() != null) wo.setPlanEndTime(affected.getNewEndTime());
                workOrderMapper.updateById(wo);
                log.info("工单 {} 计划时间已更新", affected.getWorkOrderNo());
            }
            case "CANCELLED" -> {
                applyScheduleCancellation(changeData, wo, affected);
            }
            default -> {
                log.info("工单 {} 排程变更: type={}, remark={}",
                        affected.getWorkOrderNo(), affected.getChangeType(), affected.getRemark());
            }
        }
    }

    private void applyScheduleCancellation(ApsScheduleChangeVO changeData,
                                           WorkOrder workOrder,
                                           ApsScheduleChangeVO.AffectedOrder affected) {
        String cancelReason = buildScheduleCancellationReason(changeData, affected);

        List<DispatchTask> tasks = dispatchTaskMapper.selectList(
                new LambdaQueryWrapper<DispatchTask>()
                        .eq(DispatchTask::getOrderNo, workOrder.getWorkOrderNo()));
        int cancelledTaskCount = 0;
        for (DispatchTask task : tasks) {
            if (DispatchStatus.COMPLETED.getCode().equals(task.getDispatchStatus())
                    || DispatchStatus.CANCELLED.getCode().equals(task.getDispatchStatus())) {
                continue;
            }
            dispatchTaskService.cancel(task.getId(), cancelReason);
            cancelledTaskCount++;
        }

        workOrder.setPlanStartTime(null);
        workOrder.setPlanEndTime(null);
        workOrder.setRemark(appendRemark(workOrder.getRemark(), cancelReason));
        workOrderMapper.updateById(workOrder);

        log.info("工单 {} APS取消排程已处理: cancelledDispatchTasks={}",
                workOrder.getWorkOrderNo(), cancelledTaskCount);
    }

    private List<ApsGanttCache> buildGanttCaches(ApsGanttDataVO ganttData, String scheduleBatchId) {
        if (ganttData.getTasks() == null || ganttData.getTasks().isEmpty()) {
            return Collections.emptyList();
        }

        List<ApsGanttCache> caches = new ArrayList<>(ganttData.getTasks().size());
        for (ApsGanttDataVO.GanttTask task : ganttData.getTasks()) {
            ApsGanttCache cache = new ApsGanttCache();
            cache.setScheduleBatchId(scheduleBatchId);
            cache.setTaskId(task.getTaskId());
            cache.setWorkOrderNo(task.getWorkOrderNo());
            cache.setOrderNo(task.getOrderNo());
            cache.setProductCode(task.getProductCode());
            cache.setProductName(task.getProductName());
            cache.setProcessNo(task.getProcessNo());
            cache.setProcessName(task.getProcessName());
            cache.setResourceCode(task.getResourceCode());
            cache.setResourceName(task.getResourceName());
            cache.setStartTime(task.getStartTime());
            cache.setEndTime(task.getEndTime());
            cache.setDuration(task.getDuration());
            cache.setStatus(task.getStatus());
            cache.setPriority(task.getPriority());
            cache.setPredecessors(writeJson(task.getPredecessors()));
            cache.setRangeStart(ganttData.getRangeStart());
            cache.setRangeEnd(ganttData.getRangeEnd());
            cache.setCreatedTime(LocalDateTime.now());
            caches.add(cache);
        }
        return caches;
    }

    private List<ApsCapacityLoad> buildCapacityLoads(ApsCapacityLoadVO capacityData, String scheduleBatchId) {
        if (capacityData.getItems() == null || capacityData.getItems().isEmpty()) {
            return Collections.emptyList();
        }

        List<ApsCapacityLoad> loads = new ArrayList<>(capacityData.getItems().size());
        for (ApsCapacityLoadVO.CapacityItem item : capacityData.getItems()) {
            ApsCapacityLoad load = new ApsCapacityLoad();
            load.setScheduleBatchId(scheduleBatchId);
            load.setWorkCenterCode(item.getWorkCenterCode());
            load.setWorkCenterName(item.getWorkCenterName());
            load.setLoadDate(item.getDate());
            load.setAvailableCapacity(item.getAvailableCapacity());
            load.setScheduledCapacity(item.getScheduledCapacity());
            load.setLoadRate(item.getLoadRate());
            load.setOverloaded(Boolean.TRUE.equals(item.getOverloaded()));
            load.setCalculatedAt(capacityData.getCalculatedAt());
            load.setCreatedTime(LocalDateTime.now());
            loads.add(load);
        }
        return loads;
    }

    private Map<String, List<ApsMrpCallbackVO.MrpItem>> groupMrpItemsByWorkOrder(List<ApsMrpCallbackVO.MrpItem> items) {
        Map<String, List<ApsMrpCallbackVO.MrpItem>> grouped = new LinkedHashMap<>();
        for (ApsMrpCallbackVO.MrpItem item : items) {
            grouped.computeIfAbsent(item.getWorkOrderNo(), key -> new ArrayList<>()).add(item);
        }
        return grouped;
    }

    private MaterialRequisitionDTO buildMrpRequisition(String workOrderNo,
                                                       List<ApsMrpCallbackVO.MrpItem> items,
                                                       Map<String, WorkOrder> workOrderCache,
                                                       Map<String, Material> materialCache) {
        WorkOrder workOrder = findWorkOrderByNo(workOrderNo, workOrderCache);

        MaterialRequisitionDTO dto = new MaterialRequisitionDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setWorkOrderNo(workOrder.getWorkOrderNo());
        dto.setProductCode(workOrder.getProductCode());
        dto.setProductName(workOrder.getProductName());
        dto.setPlanQty(workOrder.getPlanQty());
        dto.setQtyUnit(workOrder.getQtyUnit());
        dto.setMainOrg(workOrder.getMainOrg());
        dto.setPlanStartTime(workOrder.getPlanStartTime());
        dto.setPlanEndTime(workOrder.getPlanEndTime());
        dto.setProjectName(workOrder.getProjectName());
        dto.setWbsElement(workOrder.getWbsElement());

        List<MaterialRequisitionItemDTO> requisitionItems = new ArrayList<>(items.size());
        for (ApsMrpCallbackVO.MrpItem item : items) {
            requisitionItems.add(buildMrpRequisitionItem(item, materialCache));
        }
        dto.setItems(requisitionItems);
        return dto;
    }

    private WorkOrder findWorkOrderByNo(String workOrderNo, Map<String, WorkOrder> workOrderCache) {
        AssertUtil.isTrue(StringUtils.hasText(workOrderNo), "APS MRP工单号不能为空");
        WorkOrder cached = workOrderCache.get(workOrderNo);
        if (cached != null) {
            return cached;
        }

        WorkOrder workOrder = workOrderMapper.selectOne(
                new LambdaQueryWrapper<WorkOrder>()
                        .eq(WorkOrder::getWorkOrderNo, workOrderNo)
                        .last("LIMIT 1"));
        AssertUtil.notNull(workOrder, "未找到对应工单: " + workOrderNo);
        workOrderCache.put(workOrderNo, workOrder);
        return workOrder;
    }

    private MaterialRequisitionItemDTO buildMrpRequisitionItem(ApsMrpCallbackVO.MrpItem item,
                                                               Map<String, Material> materialCache) {
        Material material = findMaterialByCode(item.getMaterialCode(), materialCache);

        MaterialRequisitionItemDTO dto = new MaterialRequisitionItemDTO();
        dto.setMaterialId(material.getId());
        dto.setMaterialCode(StringUtils.hasText(item.getMaterialCode()) ? item.getMaterialCode() : material.getMaterialCode());
        dto.setMaterialName(StringUtils.hasText(item.getMaterialName()) ? item.getMaterialName() : material.getMaterialName());
        dto.setDemandQty(item.getRequiredQty());
        dto.setUnit(StringUtils.hasText(item.getUnit()) ? item.getUnit() : material.getBaseUnit());
        dto.setDemandTime(item.getRequiredDate());
        dto.setDescription(buildMrpItemDescription(item));
        return dto;
    }

    private Material findMaterialByCode(String materialCode, Map<String, Material> materialCache) {
        AssertUtil.isTrue(StringUtils.hasText(materialCode), "APS MRP物料编码不能为空");
        Material cached = materialCache.get(materialCode);
        if (cached != null) {
            return cached;
        }

        Material material = materialMapper.selectOne(
                new LambdaQueryWrapper<Material>()
                        .eq(Material::getMaterialCode, materialCode)
                        .last("LIMIT 1"));
        AssertUtil.notNull(material, "未找到对应物料: " + materialCode);
        materialCache.put(materialCode, material);
        return material;
    }

    private String buildMrpItemDescription(ApsMrpCallbackVO.MrpItem item) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(item.getProcessNo())) {
            parts.add("APS MRP工序 " + item.getProcessNo());
        }
        if (item.getPriority() != null) {
            parts.add("优先级 " + item.getPriority());
        }
        return parts.isEmpty() ? null : String.join("，", parts);
    }

    private String buildScheduleCancellationReason(ApsScheduleChangeVO changeData,
                                                   ApsScheduleChangeVO.AffectedOrder affected) {
        List<String> parts = new ArrayList<>();
        parts.add("APS取消排程");
        if (StringUtils.hasText(changeData.getChangeReason())) {
            parts.add(changeData.getChangeReason());
        }
        if (StringUtils.hasText(affected.getRemark())) {
            parts.add(affected.getRemark());
        }
        return String.join("：", parts);
    }

    private String appendRemark(String existingRemark, String newRemark) {
        if (!StringUtils.hasText(existingRemark)) {
            return newRemark;
        }
        return existingRemark + "\n" + newRemark;
    }

    private String resolveScheduleBatchId(String scheduleBatchId, String fallbackBatchId) {
        if (scheduleBatchId != null && !scheduleBatchId.isBlank()) {
            return scheduleBatchId;
        }
        return fallbackBatchId;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("序列化 APS 回调数据失败", e);
        }
    }
}
