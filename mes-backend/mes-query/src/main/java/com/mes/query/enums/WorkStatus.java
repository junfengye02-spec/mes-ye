package com.mes.query.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工作六状态枚举
 */
@Getter
@AllArgsConstructor
public enum WorkStatus {

    CREATED("CREATED", "已创建"),
    RELEASED("RELEASED", "已下达"),
    ISSUED("ISSUED", "已下发"),
    IN_PROGRESS("IN_PROGRESS", "执行中"),
    COMPLETED("COMPLETED", "已完成"),
    PAUSED("PAUSED", "暂停");

    private final String code;
    private final String desc;
}
