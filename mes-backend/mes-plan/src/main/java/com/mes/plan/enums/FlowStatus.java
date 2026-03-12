package com.mes.plan.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程状态枚举
 */
@Getter
@AllArgsConstructor
public enum FlowStatus {

    RUNNING("RUNNING", "运行中"),
    COMPLETED("COMPLETED", "完成"),
    TERMINATED("TERMINATED", "终止");

    private final String code;
    private final String desc;
}
