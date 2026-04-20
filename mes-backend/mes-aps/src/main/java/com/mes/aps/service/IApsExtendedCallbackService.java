package com.mes.aps.service;

import com.mes.aps.domain.vo.*;

/**
 * APS 扩展回调服务
 * <p>处理 APS 下发的 MRP、资源分配、甘特图、产能负荷、排程变更等数据</p>
 */
public interface IApsExtendedCallbackService {

    void handleMrpResult(ApsMrpCallbackVO mrpData);

    void handleResourceAllocation(ApsResourceAllocationVO allocationData);

    void handleGanttData(ApsGanttDataVO ganttData);

    void handleCapacityLoad(ApsCapacityLoadVO capacityData);

    void handleScheduleChange(ApsScheduleChangeVO changeData);
}
