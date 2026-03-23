package com.mes.common.mq;

import java.time.LocalDateTime;

public record WorkOrderStatusEvent(
        Long workOrderId,
        String workOrderNo,
        String fromStatus,
        String toStatus,
        String operator,
        LocalDateTime timestamp
) {}
