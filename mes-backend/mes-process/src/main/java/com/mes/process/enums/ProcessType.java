package com.mes.process.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工序类型枚举
 */
@Getter
@AllArgsConstructor
public enum ProcessType {

    PRODUCTION("PRODUCTION", "生产工序"),
    INSPECTION("INSPECTION", "检验工序");

    private final String code;
    private final String desc;
}
