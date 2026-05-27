package com.mes.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 工单下全部派工任务已完工事件
 */
@Getter
public class DispatchAllTasksCompletedEvent extends ApplicationEvent {

    private final Long workOrderId;
    private final String workOrderNo;

    public DispatchAllTasksCompletedEvent(Object source, Long workOrderId, String workOrderNo) {
        super(source);
        this.workOrderId = workOrderId;
        this.workOrderNo = workOrderNo;
    }
}
