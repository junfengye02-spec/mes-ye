package com.mes.plan.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 完工状态枚举
 */
@Getter
@AllArgsConstructor
public enum CompletionStatus {

    NOT_STARTED("NOT_STARTED", "未开始"),
    APPROVED("APPROVED", "审批完成");

    private final String code;
    private final String desc;
}
