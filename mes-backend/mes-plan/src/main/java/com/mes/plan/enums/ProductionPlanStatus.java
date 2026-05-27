package com.mes.plan.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 生产计划状态枚举
 */
@Getter
@AllArgsConstructor
public enum ProductionPlanStatus {

    CREATED("CREATED", "创建"),
    RELEASED("RELEASED", "已下达"),
    COMPLETED("COMPLETED", "已完成");

    private final String code;
    private final String desc;
}
