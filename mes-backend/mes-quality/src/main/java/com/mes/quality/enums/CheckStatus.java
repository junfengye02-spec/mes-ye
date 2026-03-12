package com.mes.quality.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 开工检查状态枚举
 */
@Getter
@AllArgsConstructor
public enum CheckStatus {

    PASSED("PASSED", "通过"),
    FAILED("FAILED", "未通过");

    private final String code;
    private final String desc;
}
