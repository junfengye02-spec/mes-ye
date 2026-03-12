package com.mes.workorder.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 生产工单状态枚举
 */
@Getter
@AllArgsConstructor
public enum WorkOrderStatus {

    CREATED("CREATED", "创建"),
    RELEASED("RELEASED", "已下发"),
    IN_PROGRESS("IN_PROGRESS", "执行中"),
    COMPLETED("COMPLETED", "已完工"),
    FORCE_COMPLETED("FORCE_COMPLETED", "强制完工");

    private final String code;
    private final String desc;
}
