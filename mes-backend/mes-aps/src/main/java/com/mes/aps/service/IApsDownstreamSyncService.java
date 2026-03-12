package com.mes.aps.service;

import com.mes.aps.domain.vo.ApsSyncResultVO;

/**
 * APS 下行同步服务（APS → MES）
 */
public interface IApsDownstreamSyncService {

    /** 同步排程订单 */
    ApsSyncResultVO syncOrders();

    /** 同步排程任务（含分段） */
    ApsSyncResultVO syncTasks();

    /** 同步资源 */
    ApsSyncResultVO syncResources();

    /** 同步资源日历 */
    ApsSyncResultVO syncCalendars();

    /** 同步外协订单 */
    ApsSyncResultVO syncOutsourceOrders();

    /** 同步转厂订单 */
    ApsSyncResultVO syncTransferOrders();

    /** 全量同步（依次执行所有下行同步） */
    ApsSyncResultVO syncAll();
}
