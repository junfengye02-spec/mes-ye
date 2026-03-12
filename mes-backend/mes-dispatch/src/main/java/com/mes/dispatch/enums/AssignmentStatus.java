package com.mes.dispatch.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分配记录状态枚举
 */
@Getter
@AllArgsConstructor
public enum AssignmentStatus {

    ACTIVE("ACTIVE", "有效"),
    REVOKED("REVOKED", "已撤销");

    private final String code;
    private final String desc;
}
