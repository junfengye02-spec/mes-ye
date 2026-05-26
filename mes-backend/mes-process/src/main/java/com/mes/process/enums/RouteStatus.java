package com.mes.process.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工艺路线状态。
 */
@Getter
@AllArgsConstructor
public enum RouteStatus {
    DRAFT("DRAFT", "草稿"),
    ACTIVE("ACTIVE", "启用"),
    DISABLED("DISABLED", "停用");

    private final String code;
    private final String desc;
}
