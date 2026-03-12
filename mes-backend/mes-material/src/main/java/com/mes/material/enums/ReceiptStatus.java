package com.mes.material.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReceiptStatus {
    CREATED("CREATED", "已创建");

    private final String code;
    private final String desc;
}
