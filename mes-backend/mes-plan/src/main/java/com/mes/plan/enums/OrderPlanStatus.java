package com.mes.plan.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单计划状态枚举
 */
@Getter
@AllArgsConstructor
public enum OrderPlanStatus {

    CREATED("CREATED", "创建"),
    RELEASED("RELEASED", "已下达"),
    COMPLETED("COMPLETED", "已完成"),
    TERMINATED("TERMINATED", "终止");

    private final String code;
    private final String desc;
}
