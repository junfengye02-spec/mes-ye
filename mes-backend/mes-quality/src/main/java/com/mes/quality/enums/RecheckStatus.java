package com.mes.quality.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 复检申请状态枚举
 */
@Getter
@AllArgsConstructor
public enum RecheckStatus {

    CREATED("CREATED", "已创建");

    private final String code;
    private final String desc;
}
