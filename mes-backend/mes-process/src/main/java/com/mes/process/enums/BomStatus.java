package com.mes.process.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BOM 状态枚举
 */
@Getter
@AllArgsConstructor
public enum BomStatus {

    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    DISABLED("DISABLED", "已停用");

    private final String code;
    private final String desc;
}
