package com.mes.quality.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交班记录状态枚举
 */
@Getter
@AllArgsConstructor
public enum HandoverStatus {

    PENDING("PENDING", "待接收"),
    RECEIVED("RECEIVED", "已接收");

    private final String code;
    private final String desc;
}
