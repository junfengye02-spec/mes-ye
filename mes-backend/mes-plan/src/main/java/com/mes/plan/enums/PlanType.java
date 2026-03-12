package com.mes.plan.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 计划类型枚举（用于状态日志）
 */
@Getter
@AllArgsConstructor
public enum PlanType {

    ORDER("ORDER", "订单计划"),
    PRODUCTION("PRODUCTION", "生产计划");

    private final String code;
    private final String desc;
}
