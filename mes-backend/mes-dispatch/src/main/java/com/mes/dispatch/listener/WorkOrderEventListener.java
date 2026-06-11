package com.mes.dispatch.listener;

import com.mes.dispatch.service.IDispatchTaskService;
import com.mes.workorder.event.WorkOrderReleasedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 工单事件监听器
 * <p>监听工单下发事件，自动生成派工任务</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderEventListener {

    private final IDispatchTaskService dispatchTaskService;

    @EventListener
    public void onWorkOrderReleased(WorkOrderReleasedEvent event) {
        Long workOrderId = event.getWorkOrderId();
        log.info("监听到工单下发事件, workOrderId={}, 开始自动生成派工任务", workOrderId);
        try {
            dispatchTaskService.generateFromWorkOrder(workOrderId);
        } catch (RuntimeException e) {
            log.error("自动生成派工任务失败, workOrderId={}", workOrderId, e);
            throw e;
        }
    }
}
