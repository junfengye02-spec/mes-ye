package com.mes.aps.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.aps.client.ApsClient;
import com.mes.aps.domain.entity.ApsSyncLog;
import com.mes.aps.domain.vo.ApsSyncResultVO;
import com.mes.aps.enums.SyncDirection;
import com.mes.aps.enums.SyncStatus;
import com.mes.aps.enums.SyncType;
import com.mes.aps.service.IApsMasterDataSyncService;
import com.mes.aps.service.IApsSyncConfigService;
import com.mes.aps.service.IApsSyncLogService;
import com.mes.basic.domain.entity.Material;
import com.mes.basic.domain.entity.WorkCenter;
import com.mes.basic.mapper.MaterialMapper;
import com.mes.basic.mapper.WorkCenterMapper;
import com.mes.process.domain.entity.ManufacturingBom;
import com.mes.process.domain.entity.ManufacturingBomItem;
import com.mes.process.domain.entity.ProcessInfo;
import com.mes.process.mapper.ManufacturingBomItemMapper;
import com.mes.process.mapper.ManufacturingBomMapper;
import com.mes.process.mapper.ProcessInfoMapper;
import com.mes.team.domain.entity.ProductionTeam;
import com.mes.team.mapper.ProductionTeamMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApsMasterDataSyncServiceImpl implements IApsMasterDataSyncService {

    private final ApsClient apsClient;
    private final IApsSyncConfigService configService;
    private final IApsSyncLogService syncLogService;
    private final ObjectMapper objectMapper;
    private final WorkCenterMapper workCenterMapper;
    private final ProcessInfoMapper processInfoMapper;
    private final ManufacturingBomMapper bomMapper;
    private final ManufacturingBomItemMapper bomItemMapper;
    private final MaterialMapper materialMapper;
    private final ProductionTeamMapper teamMapper;

    @Override
    public ApsSyncResultVO syncWorkCenters() {
        String batchId = UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.UPSTREAM.getCode(), SyncType.WORK_CENTER.getCode());

        try {
            List<WorkCenter> workCenters = workCenterMapper.selectList(null);
            List<Map<String, Object>> payload = workCenters.stream().map(wc -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("workCenterCode", wc.getWorkCenterCode());
                map.put("workCenterName", wc.getWorkCenterName());
                map.put("workCenterCategory", wc.getWorkCenterCategory());
                map.put("businessUnit", wc.getBusinessUnit());
                map.put("workCalendar", wc.getWorkCalendar());
                map.put("resourceOrder", wc.getResourceOrder());
                map.put("efficiency", wc.getEfficiency());
                map.put("resourceCapacity", wc.getResourceCapacity());
                map.put("batchQty", wc.getBatchQty());
                map.put("resourceType", wc.getResourceType());
                map.put("furnaceResourceType", wc.getFurnaceResourceType());
                map.put("processNoInterrupt", wc.getProcessNoInterrupt());
                map.put("processNoCrossDay", wc.getProcessNoCrossDay());
                map.put("fixedTaktProduction", wc.getFixedTaktProduction());
                return map;
            }).toList();

            apsClient.post("/api/mes/master-data/work-centers", Map.of("data", payload), Map.class);
            syncLogService.completeLog(syncLog.getId(), payload.size(), payload.size(), 0, null);
            log.info("工作中心主数据同步完成: {}条", payload.size());
            return buildResult(batchId, SyncStatus.SUCCESS.getCode(), payload.size(), payload.size(), 0);
        } catch (Exception e) {
            log.error("工作中心主数据同步失败: {}", e.getMessage(), e);
            syncLogService.completeLog(syncLog.getId(), 0, 0, 0, e.getMessage());
            return buildResult(batchId, SyncStatus.FAIL.getCode(), 0, 0, 0);
        }
    }

    @Override
    public ApsSyncResultVO syncProcessRoutes() {
        String batchId = UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.UPSTREAM.getCode(), SyncType.PROCESS_ROUTE.getCode());

        try {
            List<ProcessInfo> processes = processInfoMapper.selectList(null);
            List<Map<String, Object>> payload = processes.stream().map(p -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("processNo", p.getProcessNo());
                map.put("processName", p.getProcessName());
                map.put("processCode", p.getProcessCode());
                map.put("processType", p.getProcessType());
                map.put("product", p.getProduct());
                map.put("productCategory", p.getProductCategory());
                map.put("machineModel", p.getMachineModel());
                map.put("workCenterId", p.getWorkCenterId());
                map.put("workshopArea", p.getWorkshopArea());
                map.put("teamId", p.getTeamId());
                map.put("handleTime", p.getHandleTime());
                map.put("disassembleTime", p.getDisassembleTime());
                map.put("installTime", p.getInstallTime());
                map.put("needStrip", p.getNeedStrip());
                return map;
            }).toList();

            apsClient.post("/api/mes/master-data/process-routes", Map.of("data", payload), Map.class);
            syncLogService.completeLog(syncLog.getId(), payload.size(), payload.size(), 0, null);
            log.info("工艺路线同步完成: {}条", payload.size());
            return buildResult(batchId, SyncStatus.SUCCESS.getCode(), payload.size(), payload.size(), 0);
        } catch (Exception e) {
            log.error("工艺路线同步失败: {}", e.getMessage(), e);
            syncLogService.completeLog(syncLog.getId(), 0, 0, 0, e.getMessage());
            return buildResult(batchId, SyncStatus.FAIL.getCode(), 0, 0, 0);
        }
    }

    @Override
    public ApsSyncResultVO syncBoms() {
        String batchId = UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.UPSTREAM.getCode(), SyncType.BOM.getCode());

        try {
            List<ManufacturingBom> boms = bomMapper.selectList(null);
            List<Map<String, Object>> payload = new ArrayList<>();

            for (ManufacturingBom bom : boms) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("bomCode", bom.getBomCode());
                map.put("bomName", bom.getBomName());
                map.put("productCode", bom.getProductCode());
                map.put("productName", bom.getProductName());
                map.put("productCategory", bom.getProductCategory());
                map.put("machineModel", bom.getMachineModel());
                map.put("bomVersion", bom.getBomVersion());
                map.put("status", bom.getStatus());
                map.put("effectiveDate", bom.getEffectiveDate());
                map.put("expiryDate", bom.getExpiryDate());
                map.put("factoryOrg", bom.getFactoryOrg());

                List<ManufacturingBomItem> items = bomItemMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ManufacturingBomItem>()
                                .eq(ManufacturingBomItem::getBomId, bom.getId()));

                List<Map<String, Object>> itemList = items.stream().map(item -> {
                    Map<String, Object> itemMap = new LinkedHashMap<>();
                    itemMap.put("materialCode", item.getMaterialCode());
                    itemMap.put("materialName", item.getMaterialName());
                    itemMap.put("materialSpec", item.getMaterialSpec());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("lossRate", item.getLossRate());
                    itemMap.put("unit", item.getUnit());
                    itemMap.put("supplyType", item.getSupplyType());
                    itemMap.put("processNo", item.getProcessNo());
                    itemMap.put("isKeyPart", item.getIsKeyPart());
                    return itemMap;
                }).toList();
                map.put("items", itemList);
                payload.add(map);
            }

            apsClient.post("/api/mes/master-data/boms", Map.of("data", payload), Map.class);
            syncLogService.completeLog(syncLog.getId(), payload.size(), payload.size(), 0, null);
            log.info("制造BOM同步完成: {}条", payload.size());
            return buildResult(batchId, SyncStatus.SUCCESS.getCode(), payload.size(), payload.size(), 0);
        } catch (Exception e) {
            log.error("制造BOM同步失败: {}", e.getMessage(), e);
            syncLogService.completeLog(syncLog.getId(), 0, 0, 0, e.getMessage());
            return buildResult(batchId, SyncStatus.FAIL.getCode(), 0, 0, 0);
        }
    }

    @Override
    public ApsSyncResultVO syncMaterials() {
        String batchId = UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.UPSTREAM.getCode(), SyncType.MATERIAL_MASTER.getCode());

        try {
            List<Material> materials = materialMapper.selectList(null);
            List<Map<String, Object>> payload = materials.stream().map(m -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("materialCode", m.getMaterialCode());
                map.put("materialName", m.getMaterialName());
                map.put("materialType", m.getMaterialType());
                map.put("baseUnit", m.getBaseUnit());
                map.put("categoryLevel1", m.getCategoryLevel1());
                map.put("categoryLevel2", m.getCategoryLevel2());
                map.put("productCategory", m.getProductCategory());
                map.put("machineModel", m.getMachineModel());
                map.put("traceMode", m.getTraceMode());
                return map;
            }).toList();

            apsClient.post("/api/mes/master-data/materials", Map.of("data", payload), Map.class);
            syncLogService.completeLog(syncLog.getId(), payload.size(), payload.size(), 0, null);
            log.info("物料主数据同步完成: {}条", payload.size());
            return buildResult(batchId, SyncStatus.SUCCESS.getCode(), payload.size(), payload.size(), 0);
        } catch (Exception e) {
            log.error("物料主数据同步失败: {}", e.getMessage(), e);
            syncLogService.completeLog(syncLog.getId(), 0, 0, 0, e.getMessage());
            return buildResult(batchId, SyncStatus.FAIL.getCode(), 0, 0, 0);
        }
    }

    @Override
    public ApsSyncResultVO syncTeams() {
        String batchId = UUID.randomUUID().toString();
        ApsSyncLog syncLog = syncLogService.createLog(
                batchId, SyncDirection.UPSTREAM.getCode(), SyncType.TEAM.getCode());

        try {
            List<ProductionTeam> teams = teamMapper.selectList(null);
            List<Map<String, Object>> payload = teams.stream().map(t -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("teamCode", t.getTeamCode());
                map.put("teamName", t.getTeamName());
                map.put("orgCode", t.getOrgCode());
                map.put("orgName", t.getOrgName());
                map.put("enabled", t.getEnabled());
                map.put("description", t.getDescription());
                return map;
            }).toList();

            apsClient.post("/api/mes/master-data/teams", Map.of("data", payload), Map.class);
            syncLogService.completeLog(syncLog.getId(), payload.size(), payload.size(), 0, null);
            log.info("班组信息同步完成: {}条", payload.size());
            return buildResult(batchId, SyncStatus.SUCCESS.getCode(), payload.size(), payload.size(), 0);
        } catch (Exception e) {
            log.error("班组信息同步失败: {}", e.getMessage(), e);
            syncLogService.completeLog(syncLog.getId(), 0, 0, 0, e.getMessage());
            return buildResult(batchId, SyncStatus.FAIL.getCode(), 0, 0, 0);
        }
    }

    @Override
    public ApsSyncResultVO syncAllMasterData() {
        log.info("========== APS 主数据全量同步开始 ==========");
        int totalSuccess = 0, totalFail = 0, totalCount = 0;

        for (var result : List.of(
                syncWorkCenters(), syncProcessRoutes(), syncBoms(),
                syncMaterials(), syncTeams())) {
            totalCount += result.getTotalCount();
            totalSuccess += result.getSuccessCount();
            totalFail += result.getFailCount();
        }

        log.info("========== APS 主数据全量同步完成: total={}, success={}, fail={} ==========",
                totalCount, totalSuccess, totalFail);

        return ApsSyncResultVO.builder()
                .batchId(UUID.randomUUID().toString())
                .status(totalFail == 0 ? SyncStatus.SUCCESS.getCode() :
                        (totalSuccess == 0 ? SyncStatus.FAIL.getCode() : SyncStatus.PARTIAL.getCode()))
                .totalCount(totalCount).successCount(totalSuccess).failCount(totalFail)
                .build();
    }

    private ApsSyncResultVO buildResult(String batchId, String status,
                                         int totalCount, int successCount, int failCount) {
        return ApsSyncResultVO.builder()
                .batchId(batchId).status(status)
                .totalCount(totalCount).successCount(successCount).failCount(failCount)
                .build();
    }
}
