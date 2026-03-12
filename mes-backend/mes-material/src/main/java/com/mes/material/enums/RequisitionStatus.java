package com.mes.material.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RequisitionStatus {
    CREATED("CREATED", "已创建");

    private final String code;
    private final String desc;
}
