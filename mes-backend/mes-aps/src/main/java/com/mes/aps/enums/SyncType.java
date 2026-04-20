package com.mes.aps.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SyncType {
    // ===== 原有类型 =====
    ORDER("ORDER", "排程订单"),
    TASK("TASK", "排程任务"),
    RESOURCE("RESOURCE", "资源"),
    CALENDAR("CALENDAR", "资源日历"),
    WORKORDER("WORKORDER", "工单状态"),
    INVENTORY("INVENTORY", "库存数据"),
    QUALITY("QUALITY", "质量数据"),
    ABNORMAL("ABNORMAL", "异常重排"),
    OUTSOURCE("OUTSOURCE", "外协订单"),
    TRANSFER("TRANSFER", "转厂订单"),

    // ===== 主数据同步（MES→APS） =====
    WORK_CENTER("WORK_CENTER", "工作中心主数据"),
    PROCESS_ROUTE("PROCESS_ROUTE", "工艺路线/工序时间"),
    BOM("BOM", "制造BOM"),
    MATERIAL_MASTER("MATERIAL_MASTER", "物料主数据"),
    TEAM("TEAM", "班组信息"),

    // ===== 执行反馈（MES→APS） =====
    DISPATCH("DISPATCH", "派工分配结果"),
    START_CHECK("START_CHECK", "开工检查结果"),
    CONSTRAINT("CONSTRAINT", "工单约束关系"),
    SHIFT_OUTPUT("SHIFT_OUTPUT", "交班实际产出"),
    MATERIAL_SHORTAGE("MATERIAL_SHORTAGE", "物料齐套/短缺"),
    REQUISITION("REQUISITION", "领料进度"),
    SUPPLY_PROGRESS("SUPPLY_PROGRESS", "供应计划完成度"),
    STATUS_CHANGE("STATUS_CHANGE", "工单状态变更"),
    PROCESS_CHANGE("PROCESS_CHANGE", "工艺变更通知"),

    // ===== APS下发（APS→MES） =====
    MRP("MRP", "物料需求计划"),
    RESOURCE_ALLOCATION("RESOURCE_ALLOCATION", "资源分配计划"),
    GANTT("GANTT", "排程甘特图数据"),
    CAPACITY_LOAD("CAPACITY_LOAD", "产能负荷数据"),
    SCHEDULE_CHANGE("SCHEDULE_CHANGE", "排程变更通知");

    private final String code;
    private final String desc;
}
