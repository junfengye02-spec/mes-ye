package com.mes.aps.service;

import com.mes.aps.domain.vo.ApsSyncResultVO;

/**
 * APS 上行同步服务（MES → APS）
 */
public interface IApsUpstreamSyncService {

    /** 处理同步队列（消费 mes_aps_sync_queue） */
    ApsSyncResultVO processQueue();

    /** 写入同步队列 */
    void enqueue(String syncType, String dataType, Long dataId, String dataNo, int priority, String payload);

    /** 获取待处理队列数量 */
    long getPendingCount();
}
