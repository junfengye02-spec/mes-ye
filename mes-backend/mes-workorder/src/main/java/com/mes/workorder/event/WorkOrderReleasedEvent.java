package com.mes.workorder.event;

import org.springframework.context.ApplicationEvent;

/**
 * 工单下发事件
 * <p>工单下发后触发此事件，由派工模块监听并自动生成派工任务</p>
 */
public class WorkOrderReleasedEvent extends ApplicationEvent {

    private final Long workOrderId;

    public WorkOrderReleasedEvent(Object source, Long workOrderId) {
        super(source);
        this.workOrderId = workOrderId;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }
}
