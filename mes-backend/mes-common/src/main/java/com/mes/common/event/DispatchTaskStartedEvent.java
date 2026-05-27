package com.mes.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 派工任务开工事件
 */
@Getter
public class DispatchTaskStartedEvent extends ApplicationEvent {

    private final Long dispatchTaskId;
    private final Long workOrderId;
    private final Long workOrderTaskId;
    private final String workOrderNo;
    private final String workNo;
    private final String workName;

    public DispatchTaskStartedEvent(Object source,
                                    Long dispatchTaskId,
                                    Long workOrderId,
                                    Long workOrderTaskId,
                                    String workOrderNo,
                                    String workNo,
                                    String workName) {
        super(source);
        this.dispatchTaskId = dispatchTaskId;
        this.workOrderId = workOrderId;
        this.workOrderTaskId = workOrderTaskId;
        this.workOrderNo = workOrderNo;
        this.workNo = workNo;
        this.workName = workName;
    }
}
