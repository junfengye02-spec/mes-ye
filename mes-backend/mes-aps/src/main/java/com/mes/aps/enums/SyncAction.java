package com.mes.aps.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SyncAction {
    CREATE("CREATE", "新增"),
    UPDATE("UPDATE", "更新"),
    DELETE("DELETE", "删除");

    private final String code;
    private final String desc;
}
