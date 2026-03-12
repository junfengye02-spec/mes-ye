package com.mes.material.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReceiptType {
    NEW("NEW", "新制品"),
    REPAIR("REPAIR", "维修品"),
    UNREPAIRABLE("UNREPAIRABLE", "不可维修品");

    private final String code;
    private final String desc;
}
