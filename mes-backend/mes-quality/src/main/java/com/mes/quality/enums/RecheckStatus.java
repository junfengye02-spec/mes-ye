package com.mes.quality.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 复检申请状态枚举
 */
@Getter
@AllArgsConstructor
public enum RecheckStatus {

    CREATED("CREATED", "已创建"),
    SUBMITTED("SUBMITTED", "已提交"),
    IN_REVIEW("IN_REVIEW", "审核中"),
    APPROVED("APPROVED", "已批准"),
    REJECTED("REJECTED", "已驳回"),
    COMPLETED("COMPLETED", "已完成");

    private final String code;
    private final String desc;
}
