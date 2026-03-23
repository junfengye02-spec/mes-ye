package com.mes.common.mq;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryChangeEvent(
        Long inventoryId,
        String materialCode,
        String changeType,
        BigDecimal quantity,
        String operator,
        Long relatedOrderId,
        LocalDateTime timestamp
) {}
