package com.mes.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 派工任务完工事件
 */
@Getter
public class DispatchTaskCompletedEvent extends ApplicationEvent {

    private final Long dispatchTaskId;
    private final Long workOrderId;
    private final Long workOrderTaskId;
    private final String workOrderNo;
    private final String workNo;
    private final String workName;
    private final String projectName;
    private final String serialNo;
    private final BigDecimal actualQty;
    private final String qualityResult;
    private final LocalDateTime actualEndTime;
    private final String remark;
    private final String operator;

    public DispatchTaskCompletedEvent(Object source,
                                      Long dispatchTaskId,
                                      Long workOrderId,
                                      Long workOrderTaskId,
                                      String workOrderNo,
                                      String workNo,
                                      String workName,
                                      String projectName,
                                      String serialNo,
                                      BigDecimal actualQty,
                                      String qualityResult,
                                      LocalDateTime actualEndTime,
                                      String remark,
                                      String operator) {
        super(source);
        this.dispatchTaskId = dispatchTaskId;
        this.workOrderId = workOrderId;
        this.workOrderTaskId = workOrderTaskId;
        this.workOrderNo = workOrderNo;
        this.workNo = workNo;
        this.workName = workName;
        this.projectName = projectName;
        this.serialNo = serialNo;
        this.actualQty = actualQty;
        this.qualityResult = qualityResult;
        this.actualEndTime = actualEndTime;
        this.remark = remark;
        this.operator = operator;
    }
}
