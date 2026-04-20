package com.mes.aps.service;

import com.mes.aps.domain.vo.ApsSyncResultVO;

/**
 * APS 主数据同步服务
 * <p>将 MES 主数据推送到 APS（工作中心、工艺路线、BOM、物料、班组）</p>
 */
public interface IApsMasterDataSyncService {

    ApsSyncResultVO syncWorkCenters();

    ApsSyncResultVO syncProcessRoutes();

    ApsSyncResultVO syncBoms();

    ApsSyncResultVO syncMaterials();

    ApsSyncResultVO syncTeams();

    ApsSyncResultVO syncAllMasterData();
}
