package com.mes.aps.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SyncStatus {
    SUCCESS("SUCCESS", "成功"),
    FAIL("FAIL", "失败"),
    PARTIAL("PARTIAL", "部分成功"),
    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    SYNCED("SYNCED", "已同步"),
    FAILED("FAILED", "同步失败");

    private final String code;
    private final String desc;
}
