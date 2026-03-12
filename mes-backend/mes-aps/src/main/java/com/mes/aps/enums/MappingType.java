package com.mes.aps.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MappingType {
    MATERIAL("MATERIAL", "物料"),
    WORK_CENTER("WORK_CENTER", "工作中心"),
    STATUS("STATUS", "状态"),
    FACTORY("FACTORY", "工厂"),
    PROCESS("PROCESS", "工序"),
    SUPPLIER("SUPPLIER", "供应商");

    private final String code;
    private final String desc;
}
