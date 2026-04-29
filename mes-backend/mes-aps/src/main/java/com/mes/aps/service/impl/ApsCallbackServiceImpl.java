package com.mes.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.aps.domain.entity.ApsSyncLog;
import com.mes.aps.domain.entity.WorkOrderTaskSegment;
import com.mes.aps.domain.vo.ApsScheduleCallbackVO;
import com.mes.aps.domain.vo.ApsScheduleCallbackVO.TaskSummary;
import com.mes.aps.enums.SyncDirection;
import com.mes.aps.mapper.WorkOrderTaskSegmentMapper;
import com.mes.aps.service.ApsCallbackIdempotencyService;
import com.mes.aps.service.IApsCallbackService;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApsCallbackServiceImpl implements IApsCallbackService {

    private final OrderPlanMapper orderPlanMapper;
    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderTaskSegmentMapper taskSegmentMapper;
    private final IApsSyncLogService syncLogService;
    private final ApsCallbackIdempotencyService idempotencyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleScheduleResult(ApsScheduleCallbackVO callback) {
        log.info("收到 APS 排程回调: requestId={}, status={}", callback.getRequestId(), callback.getStatus());
        if (!idempotencyService.tryAcquire("scheduleResult", callback.getRequestId())) {
            log.warn("重复回调忽略: type=scheduleResult, requestId={}", callback.getRequestId());
            return;
        }

        String batchId = UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), "CALLBACK");

        int totalCount = 0, successCount = 0, failCount = 0;

        List<TaskSummary> tasks = callback.getTaskSummaries();
        if (tasks == null || tasks.isEmpty()) {
            log.warn("排程回调无任务数据: requestId={}", callback.getRequestId());
            syncLogService.completeLog(syncLog.getId(), 0, 0, 0, "回调无任务数据");
            return;
        }

        // 按 apsOrderId 分组
        Map<Long, List<TaskSummary>> groupedByOrder = tasks.stream()
                .filter(t -> t.getApsOrderId() != null)
                .collect(Collectors.groupingBy(TaskSummary::getApsOrderId));

        for (Map.Entry<Long, List<TaskSummary>> entry : groupedByOrder.entrySet()) {
            Long apsOrderId = entry.getKey();
            List<TaskSummary> orderTasks = entry.getValue();
            totalCount++;

            try {
                processOrderTasks(apsOrderId, orderTasks);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("处理排程回调失败: apsOrderId={}, error={}", apsOrderId, e.getMessage());
            }
        }

        syncLogService.completeLog(syncLog.getId(), totalCount, successCount, failCount,
                "FAILED".equals(callback.getStatus()) ? callback.getMessage() : null);
        log.info("APS 排程回调处理完成: requestId={}, orders={}, success={}, fail={}",
                callback.getRequestId(), totalCount, successCount, failCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRequestRejected(ApsScheduleCallbackVO callback) {
        log.warn("APS 请求被拒绝: requestId={}, reason={}", callback.getRequestId(), callback.getReason());
        if (!idempotencyService.tryAcquire("requestRejected", callback.getRequestId())) {
            log.warn("重复回调忽略: type=requestRejected, requestId={}", callback.getRequestId());
            return;
        }

        String batchId = UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.DOWNSTREAM.getCode(), "CALLBACK_REJECTED");
        String errorMsg = String.format("请求被拒绝 [requestId=%s]: %s",
                callback.getRequestId(), callback.getReason());
        syncLogService.completeLog(syncLog.getId(), 0, 0, 1, errorMsg);
    }

    private void processOrderTasks(Long apsOrderId, List<TaskSummary> tasks) {
        // 1. 通过 apsOrderId 查找 OrderPlan
        OrderPlan orderPlan = orderPlanMapper.selectOne(
                new LambdaQueryWrapper<OrderPlan>()
                        .eq(OrderPlan::getApsOrderId, apsOrderId));
        if (orderPlan == null) {
            throw new RuntimeException("未找到对应的 OrderPlan: apsOrderId=" + apsOrderId);
        }

        // 2. 查找该订单下所有 WorkOrder
        List<WorkOrder> workOrders = workOrderMapper.selectList(
                new LambdaQueryWrapper<WorkOrder>()
                        .eq(WorkOrder::getOrderPlanNo, orderPlan.getOrderNo()));

        // 3. 计算该订单所有任务的最早开始和最晚结束时间
        LocalDateTime earliest = tasks.stream()
                .map(TaskSummary::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime latest = tasks.stream()
                .map(TaskSummary::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        // 4. 更新 WorkOrder 的计划时间
        for (WorkOrder wo : workOrders) {
            wo.setPlanStartTime(earliest);
            wo.setPlanEndTime(latest);
            workOrderMapper.updateById(wo);
        }

        // 5. 更新/插入 WorkOrderTaskSegment
        for (TaskSummary task : tasks) {
            if (task.getTaskId() == null) continue;

            Integer segIndex = task.getSegmentIndex() != null ? task.getSegmentIndex() : 1;

            WorkOrderTaskSegment existing = taskSegmentMapper.selectOne(
                    new LambdaQueryWrapper<WorkOrderTaskSegment>()
                            .eq(WorkOrderTaskSegment::getWorkOrderTaskId, task.getTaskId())
                            .eq(WorkOrderTaskSegment::getSegmentIndex, segIndex));

            if (existing != null) {
                existing.setSegmentStartTime(task.getStartTime());
                existing.setSegmentEndTime(task.getEndTime());
                existing.setSegmentDuration(task.getDuration());
                existing.setUpdatedTime(LocalDateTime.now());
                taskSegmentMapper.updateById(existing);
            } else {
                WorkOrderTaskSegment segment = new WorkOrderTaskSegment();
                segment.setWorkOrderTaskId(task.getTaskId());
                segment.setSegmentIndex(segIndex);
                segment.setSegmentStartTime(task.getStartTime());
                segment.setSegmentEndTime(task.getEndTime());
                segment.setSegmentDuration(task.getDuration());
                segment.setShiftName(task.getResourceCode());
                segment.setStatus("PENDING");
                segment.setCreatedTime(LocalDateTime.now());
                segment.setUpdatedTime(LocalDateTime.now());
                taskSegmentMapper.insert(segment);
            }
        }

        log.debug("排程回调处理: apsOrderId={}, 更新{}个工单, {}个任务分段",
                apsOrderId, workOrders.size(), tasks.size());
    }
}
