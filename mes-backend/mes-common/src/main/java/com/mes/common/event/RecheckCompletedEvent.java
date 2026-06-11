package com.mes.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 复检申请完结事件
 */
@Getter
public class RecheckCompletedEvent extends ApplicationEvent {

    private final Long recheckId;
    private final Long workOrderId;
    private final Long dispatchTaskId;
    private final String workOrderNo;
    private final String materialCode;
    private final String materialName;
    private final LocalDateTime completedTime;

    public RecheckCompletedEvent(Object source,
                                 Long recheckId,
                                 Long workOrderId,
                                 Long dispatchTaskId,
                                 String workOrderNo,
                                 String materialCode,
                                 String materialName,
                                 LocalDateTime completedTime) {
        super(source);
        this.recheckId = recheckId;
        this.workOrderId = workOrderId;
        this.dispatchTaskId = dispatchTaskId;
        this.workOrderNo = workOrderNo;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.completedTime = completedTime;
    }
}
