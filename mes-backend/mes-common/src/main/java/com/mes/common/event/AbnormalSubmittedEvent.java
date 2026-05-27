package com.mes.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 异常联络单提交事件
 */
@Getter
public class AbnormalSubmittedEvent extends ApplicationEvent {

    private final Long contactId;
    private final String contactNo;
    private final Long workOrderId;
    private final Long dispatchTaskId;
    private final String orderNo;
    private final String eventCategory;
    private final String abnormalDesc;

    public AbnormalSubmittedEvent(Object source,
                                  Long contactId,
                                  String contactNo,
                                  Long workOrderId,
                                  Long dispatchTaskId,
                                  String orderNo,
                                  String eventCategory,
                                  String abnormalDesc) {
        super(source);
        this.contactId = contactId;
        this.contactNo = contactNo;
        this.workOrderId = workOrderId;
        this.dispatchTaskId = dispatchTaskId;
        this.orderNo = orderNo;
        this.eventCategory = eventCategory;
        this.abnormalDesc = abnormalDesc;
    }
}
