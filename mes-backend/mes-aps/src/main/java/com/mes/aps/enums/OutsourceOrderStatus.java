package com.mes.aps.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OutsourceOrderStatus {
    PENDING("PENDING", "待处理"),
    SHIPPED("SHIPPED", "已发出"),
    RECEIVED("RECEIVED", "已收货"),
    QC_PASSED("QC_PASSED", "质检合格"),
    QC_FAILED("QC_FAILED", "质检不合格"),
    COMPLETED("COMPLETED", "已完成");

    private final String code;
    private final String desc;
}
