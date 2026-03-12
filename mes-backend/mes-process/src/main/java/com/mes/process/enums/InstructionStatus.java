package com.mes.process.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 指示书状态枚举
 */
@Getter
@AllArgsConstructor
public enum InstructionStatus {

    DRAFT("DRAFT", "草稿"),
    ACTIVE("ACTIVE", "生效"),
    SUPERSEDED("SUPERSEDED", "已替代"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;
}
