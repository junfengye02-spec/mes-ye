package com.mes.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.mes.process.domain.entity.Route;
import com.mes.process.domain.entity.RouteStep;
import com.mes.process.enums.RouteStatus;
import com.mes.process.mapper.ManufacturingBomItemMapper;
import com.mes.process.mapper.ManufacturingBomMapper;
import com.mes.process.mapper.ProcessInfoMapper;
import com.mes.process.mapper.RouteMapper;
import com.mes.process.mapper.RouteStepMapper;
import com.mes.team.domain.entity.ProductionTeam;
import com.mes.team.mapper.ProductionTeamMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final RouteMapper routeMapper;
    private final RouteStepMapper routeStepMapper;
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
                map.put("resourceSubtype", wc.getResourceSubtype());
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
            List<Route> routes = queryActiveRoutes();
            List<Map<String, Object>> payload = new ArrayList<>();
            Map<Long, ProcessInfo> processInfoCache = new HashMap<>();
            Map<Long, WorkCenter> workCenterCache = new HashMap<>();

            for (Route route : routes) {
                List<RouteStep> steps = queryRouteSteps(route.getId());
                if (steps.isEmpty()) {
                    log.warn("工艺路线未配置工序步骤，跳过同步: routeId={}, routeCode={}",
                            route.getId(), route.getRouteCode());
                    continue;
                }

                Map<Long, Integer> stepSequenceMap = buildStepSequenceMap(steps);
                for (RouteStep step : steps) {
                    payload.add(toProcessRoutePayload(route, step, stepSequenceMap,
                            processInfoCache, workCenterCache));
                }
            }

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

    private List<Route> queryActiveRoutes() {
        LocalDate today = LocalDate.now();
        List<Route> routes = routeMapper.selectList(new LambdaQueryWrapper<Route>()
                .eq(Route::getStatus, RouteStatus.ACTIVE.getCode())
                .and(w -> w.isNull(Route::getEffectiveDate).or().le(Route::getEffectiveDate, today))
                .and(w -> w.isNull(Route::getExpiryDate).or().ge(Route::getExpiryDate, today))
                .orderByDesc(Route::getUpdatedTime)
                .orderByDesc(Route::getCreatedTime)
                .orderByAsc(Route::getId));
        return routes == null ? List.of() : routes;
    }

    private List<RouteStep> queryRouteSteps(Long routeId) {
        List<RouteStep> steps = routeStepMapper.selectList(new LambdaQueryWrapper<RouteStep>()
                .eq(RouteStep::getRouteId, routeId)
                .orderByAsc(RouteStep::getSequenceNo)
                .orderByAsc(RouteStep::getId));
        if (steps == null) {
            return List.of();
        }
        return steps.stream()
                .sorted(Comparator
                        .comparing(RouteStep::getSequenceNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RouteStep::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private Map<Long, Integer> buildStepSequenceMap(List<RouteStep> steps) {
        Map<Long, Integer> sequenceMap = new HashMap<>();
        for (RouteStep step : steps) {
            if (step.getId() != null && step.getSequenceNo() != null) {
                sequenceMap.put(step.getId(), step.getSequenceNo());
            }
        }
        return sequenceMap;
    }

    private Map<String, Object> toProcessRoutePayload(Route route, RouteStep step,
                                                      Map<Long, Integer> stepSequenceMap,
                                                      Map<Long, ProcessInfo> processInfoCache,
                                                      Map<Long, WorkCenter> workCenterCache) {
        ProcessInfo processInfo = resolveProcessInfo(step.getProcessId(), processInfoCache);

        Long resolvedWorkCenterId = step.getWorkCenterId();
        if (resolvedWorkCenterId == null && processInfo != null) {
            resolvedWorkCenterId = processInfo.getWorkCenterId();
        }
        WorkCenter workCenter = resolveWorkCenter(resolvedWorkCenterId, workCenterCache);

        String processNo = firstNonBlank(step.getProcessNo(), processInfo == null ? null : processInfo.getProcessNo());
        String processName = firstNonBlank(step.getProcessName(), processInfo == null ? null : processInfo.getProcessName());
        Integer predecessorSequenceNo = step.getPredecessorStepId() == null
                ? null
                : stepSequenceMap.get(step.getPredecessorStepId());
        BigDecimal cycleTime = step.getHandleTime() != null
                ? step.getHandleTime()
                : (processInfo == null ? null : processInfo.getHandleTime());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("routeId", route.getId());
        map.put("routeCode", route.getRouteCode());
        map.put("routeName", route.getRouteName());
        map.put("productCode", route.getProductCode());
        map.put("productCategory", route.getProductCategory());
        map.put("machineModel", route.getMachineModel());
        map.put("productType", route.getProductType());
        map.put("routeStatus", route.getStatus());
        map.put("effectiveDate", route.getEffectiveDate());
        map.put("expiryDate", route.getExpiryDate());
        map.put("stepId", step.getId());
        map.put("sequenceNo", step.getSequenceNo());
        map.put("processSequence", step.getSequenceNo());
        map.put("processId", step.getProcessId());
        map.put("processNo", processNo);
        map.put("processName", processName);
        map.put("processCode", processInfo == null ? null : processInfo.getProcessCode());
        map.put("processType", processInfo == null ? null : processInfo.getProcessType());
        map.put("workCenterId", resolvedWorkCenterId);
        map.put("workCenterCode", workCenter == null ? null : workCenter.getWorkCenterCode());
        map.put("workCenterName", workCenter == null ? null : workCenter.getWorkCenterName());
        map.put("resourceCode", workCenter == null ? null : workCenter.getWorkCenterCode());
        map.put("resourceId", resolvedWorkCenterId);
        map.put("cycleTime", toDouble(cycleTime));
        map.put("handleTime", toDouble(cycleTime));
        map.put("setupTime", 0D);
        map.put("yieldRate", 1D);
        map.put("predecessorStepId", step.getPredecessorStepId());
        map.put("predecessorSequenceNo", predecessorSequenceNo);
        map.put("dependencySequenceNos", predecessorSequenceNo == null
                ? List.of()
                : List.of(predecessorSequenceNo));
        map.put("parallelFlag", step.getParallelFlag());
        map.put("optionalFlag", step.getOptionalFlag());
        map.put("remark", firstNonBlank(step.getRemark(), route.getRemark()));
        return map;
    }

    private ProcessInfo resolveProcessInfo(Long processId, Map<Long, ProcessInfo> cache) {
        if (processId == null) {
            return null;
        }
        if (cache.containsKey(processId)) {
            return cache.get(processId);
        }

        ProcessInfo processInfo = processInfoMapper.selectById(processId);
        cache.put(processId, processInfo);
        return processInfo;
    }

    private WorkCenter resolveWorkCenter(Long workCenterId, Map<Long, WorkCenter> cache) {
        if (workCenterId == null) {
            return null;
        }
        if (cache.containsKey(workCenterId)) {
            return cache.get(workCenterId);
        }

        WorkCenter workCenter = workCenterMapper.selectById(workCenterId);
        cache.put(workCenterId, workCenter);
        return workCenter;
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
