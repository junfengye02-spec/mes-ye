package com.mes.aps.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TransferOrderStatus {
    PENDING("PENDING", "待处理"),
    SHIPPED("SHIPPED", "已发出"),
    IN_TRANSIT("IN_TRANSIT", "运输中"),
    ARRIVED("ARRIVED", "已到达"),
    RECEIVED("RECEIVED", "已接收");

    private final String code;
    private final String desc;
}
