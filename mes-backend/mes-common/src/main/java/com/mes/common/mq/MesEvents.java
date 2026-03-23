package com.mes.common.mq;

/**
 * MES 系统事件常量定义
 */
public interface MesEvents {
    String EXCHANGE = "mes.topic";

    String WORKORDER_STATUS_CHANGED = "workorder.status.changed";
    String WORKORDER_RELEASED = "workorder.released";
    String WORKORDER_STARTED = "workorder.started";
    String WORKORDER_COMPLETED = "workorder.completed";

    String INVENTORY_CHANGED = "inventory.changed";
    String INVENTORY_LOCKED = "inventory.locked";
    String INVENTORY_DEDUCTED = "inventory.deducted";
    String INVENTORY_RELEASED = "inventory.released";

    String PLAN_CREATED = "plan.created";
    String PLAN_UPDATED = "plan.updated";

    String APS_SYNC_REQUEST = "aps.sync.request";
    String APS_SYNC_COMPLETED = "aps.sync.completed";

    String QUALITY_CHECK_COMPLETED = "quality.check.completed";
    String ABNORMAL_CREATED = "abnormal.created";
}
