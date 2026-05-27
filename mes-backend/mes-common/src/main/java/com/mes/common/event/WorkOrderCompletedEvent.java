package com.mes.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工单完工事件
 */
@Getter
public class WorkOrderCompletedEvent extends ApplicationEvent {

    private final Long workOrderId;
    private final String workOrderNo;
    private final String productionPlanNo;
    private final String orderPlanNo;
    private final BigDecimal completedQty;
    private final LocalDateTime actualEndTime;

    public WorkOrderCompletedEvent(Object source,
                                   Long workOrderId,
                                   String workOrderNo,
                                   String productionPlanNo,
                                   String orderPlanNo,
                                   BigDecimal completedQty,
                                   LocalDateTime actualEndTime) {
        super(source);
        this.workOrderId = workOrderId;
        this.workOrderNo = workOrderNo;
        this.productionPlanNo = productionPlanNo;
        this.orderPlanNo = orderPlanNo;
        this.completedQty = completedQty;
        this.actualEndTime = actualEndTime;
    }
}
