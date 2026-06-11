package com.mes.aps.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum SyncType {
    // ===== MES <-> APS 真实集成合同类型 =====
    ORDER("ORDER", "排程订单", false, false, null, false),
    TASK("TASK", "排程任务", false, false, null, false),
    RESOURCE("RESOURCE", "资源", false, false, null, false),
    CALENDAR("CALENDAR", "资源日历", false, false, null, false),
    WORKORDER("WORKORDER", "工单状态", true, false, "/api/mes/status/sync", false),
    INVENTORY("INVENTORY", "库存数据", true, false, "/api/mes/inventory/sync", false),
    QUALITY("QUALITY", "质量数据", true, false, "/api/mes/quality/sync", false),
    ABNORMAL("ABNORMAL", "异常重排", true, true, "/api/mes/reschedule", true),
    OUTSOURCE("OUTSOURCE", "外协订单", true, false, "/api/mes/outsource/status", false),
    TRANSFER("TRANSFER", "转厂订单", true, false, "/api/mes/transfer/status", false),

    // ===== 主数据同步（MES→APS） =====
    WORK_CENTER("WORK_CENTER", "工作中心主数据", false, false, "/api/mes/master-data/work-centers", false),
    PROCESS_ROUTE("PROCESS_ROUTE", "工艺路线/工序时间", false, false, "/api/mes/master-data/process-routes", false),
    BOM("BOM", "制造BOM", false, false, "/api/mes/master-data/boms", false),
    MATERIAL_MASTER("MATERIAL_MASTER", "物料主数据", false, false, "/api/mes/master-data/materials", false),
    TEAM("TEAM", "班组信息", false, false, "/api/mes/master-data/teams", false),

    // ===== APS下发（APS→MES） =====
    MRP("MRP", "物料需求计划", false, false, null, false),
    RESOURCE_ALLOCATION("RESOURCE_ALLOCATION", "资源分配计划", false, false, null, false),
    GANTT("GANTT", "排程甘特图数据", false, false, null, false),
    CAPACITY_LOAD("CAPACITY_LOAD", "产能负荷数据", false, false, null, false),
    SCHEDULE_CHANGE("SCHEDULE_CHANGE", "排程变更通知", false, false, null, false);

    private final String code;
    private final String desc;
    private final boolean upstreamQueueSupported;
    private final boolean rescheduleTrigger;
    private final String upstreamContractEndpoint;
    private final boolean asyncUpstreamContractCall;

    public static Optional<SyncType> fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst();
    }

    public static boolean isUpstreamQueueSupported(String code) {
        return fromCode(code)
                .map(SyncType::isUpstreamQueueSupported)
                .orElse(false);
    }

    public static boolean isRescheduleTrigger(String code) {
        return fromCode(code)
                .map(SyncType::isRescheduleTrigger)
                .orElse(false);
    }

    public boolean hasUpstreamContractEndpoint() {
        return upstreamContractEndpoint != null && !upstreamContractEndpoint.isBlank();
    }

    public String requireUpstreamContractEndpoint() {
        if (!hasUpstreamContractEndpoint()) {
            throw new IllegalStateException("当前 SyncType 未声明 MES→APS 合同端点: " + code);
        }
        return upstreamContractEndpoint;
    }
}
