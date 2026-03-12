package com.mes.dispatch.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分派类型枚举
 */
@Getter
@AllArgsConstructor
public enum AssignType {

    PERSON("PERSON", "人员"),
    DEVICE("DEVICE", "设备"),
    TEAM("TEAM", "班组");

    private final String code;
    private final String desc;
}
