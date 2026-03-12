package com.mes.aps.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SyncDirection {
    DOWNSTREAM("DOWNSTREAM", "下行同步（APS→MES）"),
    UPSTREAM("UPSTREAM", "上行同步（MES→APS）");

    private final String code;
    private final String desc;
}
