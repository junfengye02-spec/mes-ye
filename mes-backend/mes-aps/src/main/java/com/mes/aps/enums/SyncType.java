package com.mes.aps.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SyncType {
    ORDER("ORDER", "排程订单"),
    TASK("TASK", "排程任务"),
    RESOURCE("RESOURCE", "资源"),
    CALENDAR("CALENDAR", "资源日历"),
    WORKORDER("WORKORDER", "工单状态"),
    INVENTORY("INVENTORY", "库存数据"),
    QUALITY("QUALITY", "质量数据"),
    ABNORMAL("ABNORMAL", "异常重排"),
    OUTSOURCE("OUTSOURCE", "外协订单"),
    TRANSFER("TRANSFER", "转厂订单");

    private final String code;
    private final String desc;
}
