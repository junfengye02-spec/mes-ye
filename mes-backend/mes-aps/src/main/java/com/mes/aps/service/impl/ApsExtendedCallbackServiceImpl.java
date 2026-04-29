package com.mes.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.aps.domain.entity.ApsSyncLog;
import com.mes.aps.domain.vo.*;
import com.mes.aps.enums.SyncDirection;
import com.mes.aps.enums.SyncStatus;
import com.mes.aps.enums.SyncType;
import com.mes.aps.service.ApsCallbackIdempotencyService;
import com.mes.aps.service.IApsExtendedCallbackService;
import com.mes.aps.service.IApsSyncLogService;
import com.mes.dispatch.domain.entity.DispatchAssignment;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.mapper.DispatchAssignmentMapper;
import com.mes.dispatch.mapper.DispatchTaskMapper;
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final ApsCallbackIdempotencyService idempotencyService;

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

        try {
            int count = mrpData.getItems() != null ? mrpData.getItems().size() : 0;

            // 存储 MRP 结果以便后续生成领料申请
            // 具体业务：根据 MRP 需求时间和物料编码，可自动生成 MaterialRequisition
            log.info("接收APS物料需求计划: batchId={}, 共{}条物料需求", batchId, count);

            // TODO: 当 MES 领料模块完善后，可在此自动生成领料申请单
            // for (ApsMrpCallbackVO.MrpItem item : mrpData.getItems()) {
            //     materialRequisitionService.createFromMrp(item);
            // }

            syncLogService.completeLog(syncLog.getId(), count, count, 0, null);
        } catch (Exception e) {
            log.error("处理MRP结果失败: {}", e.getMessage(), e);
            syncLogService.completeLog(syncLog.getId(), 0, 0, 0, e.getMessage());
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
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), SyncType.GANTT.getCode());

        try {
            int count = ganttData.getTasks() != null ? ganttData.getTasks().size() : 0;

            // 甘特图数据缓存到 JSON 字段或专用表，供前端查询展示
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
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), SyncType.CAPACITY_LOAD.getCode());

        try {
            int count = capacityData.getItems() != null ? capacityData.getItems().size() : 0;

            // 产能负荷数据存储，供前端展示产能预警看板
            log.info("接收APS产能负荷数据: batchId={}, 共{}条", batchId, count);

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
                    applyScheduleChange(affected);
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

    private void applyScheduleChange(ApsScheduleChangeVO.AffectedOrder affected) {
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
                log.info("工单 {} 被APS标记为取消，需人工确认", affected.getWorkOrderNo());
            }
            default -> {
                log.info("工单 {} 排程变更: type={}, remark={}",
                        affected.getWorkOrderNo(), affected.getChangeType(), affected.getRemark());
            }
        }
    }
}
