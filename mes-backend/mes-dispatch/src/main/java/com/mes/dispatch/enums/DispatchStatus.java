package com.mes.dispatch.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 派工任务状态枚举
 */
@Getter
@AllArgsConstructor
public enum DispatchStatus {

    UNASSIGNED("UNASSIGNED", "未分派"),
    ASSIGNED("ASSIGNED", "已分派"),
    REVOKED("REVOKED", "已撤销");

    private final String code;
    private final String desc;
}
