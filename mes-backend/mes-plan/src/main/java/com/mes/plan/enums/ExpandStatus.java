package com.mes.plan.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 展开状态枚举
 */
@Getter
@AllArgsConstructor
public enum ExpandStatus {

    UNEXPANDED("UNEXPANDED", "未展开"),
    EXPANDED("EXPANDED", "全部展开");

    private final String code;
    private final String desc;
}
