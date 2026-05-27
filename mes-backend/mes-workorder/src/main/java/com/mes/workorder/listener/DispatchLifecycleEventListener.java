package com.mes.workorder.listener;

import com.mes.common.event.DispatchAllTasksCompletedEvent;
import com.mes.common.event.DispatchTaskStartedEvent;
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.enums.WorkOrderStatus;
import com.mes.workorder.mapper.WorkOrderMapper;
import com.mes.workorder.service.IWorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 派工事件驱动的工单状态级联监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DispatchLifecycleEventListener {

    private final WorkOrderMapper workOrderMapper;
    private final IWorkOrderService workOrderService;

    @EventListener
    public void onDispatchTaskStarted(DispatchTaskStartedEvent event) {
        WorkOrder workOrder = load(event.getWorkOrderId());
        if (workOrder == null) {
            return;
        }
        if (WorkOrderStatus.RELEASED.getCode().equals(workOrder.getStatus())) {
            try {
                workOrderService.start(workOrder.getId());
            } catch (Exception ex) {
                log.warn("派工开工驱动工单开工失败, workOrderId={}, err={}",
                        workOrder.getId(), ex.getMessage());
            }
        }
    }

    @EventListener
    public void onDispatchAllTasksCompleted(DispatchAllTasksCompletedEvent event) {
        WorkOrder workOrder = load(event.getWorkOrderId());
        if (workOrder == null) {
            return;
        }

        try {
            if (WorkOrderStatus.RELEASED.getCode().equals(workOrder.getStatus())) {
                workOrderService.start(workOrder.getId());
                workOrder = load(workOrder.getId());
            }
            if (workOrder != null && WorkOrderStatus.IN_PROGRESS.getCode().equals(workOrder.getStatus())) {
                workOrderService.complete(workOrder.getId());
            }
        } catch (Exception ex) {
            log.warn("派工完工驱动工单完工失败, workOrderId={}, err={}",
                    event.getWorkOrderId(), ex.getMessage());
        }
    }

    private WorkOrder load(Long workOrderId) {
        if (workOrderId == null) {
            return null;
        }
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            log.warn("派工事件关联工单不存在, workOrderId={}", workOrderId);
        }
        return workOrder;
    }
}
