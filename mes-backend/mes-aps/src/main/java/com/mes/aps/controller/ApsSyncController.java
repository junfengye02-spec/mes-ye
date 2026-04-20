package com.mes.aps.controller;

import com.mes.aps.client.ApsClient;
import com.mes.aps.domain.vo.ApsSyncResultVO;
import com.mes.aps.service.IApsCompensationService;
import com.mes.aps.service.IApsDownstreamSyncService;
import com.mes.aps.service.IApsMasterDataSyncService;
import com.mes.aps.service.IApsUpstreamSyncService;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * APS 同步操作 Controller
 * <p>提供手动触发同步、补偿同步、状态查询等接口</p>
 */
@Tag(name = "APS同步操作", description = "APS同步手动触发和状态查询接口")
@RestController
@RequestMapping("/aps/sync")
@RequiredArgsConstructor
public class ApsSyncController {

    private final IApsDownstreamSyncService downstreamSyncService;
    private final IApsUpstreamSyncService upstreamSyncService;
    private final IApsCompensationService compensationService;
    private final IApsMasterDataSyncService masterDataSyncService;
    private final ApsClient apsClient;

    @Operation(summary = "手动触发下行同步（全量）")
    @PostMapping("/downstream")
    public R<ApsSyncResultVO> triggerDownstreamSync() {
        return R.ok(downstreamSyncService.syncAll());
    }

    @Operation(summary = "手动触发下行同步（指定类型）")
    @PostMapping("/downstream/{syncType}")
    public R<ApsSyncResultVO> triggerDownstreamSyncByType(@PathVariable String syncType) {
        ApsSyncResultVO result = switch (syncType.toUpperCase()) {
            case "ORDER" -> downstreamSyncService.syncOrders();
            case "TASK" -> downstreamSyncService.syncTasks();
            case "RESOURCE" -> downstreamSyncService.syncResources();
            case "CALENDAR" -> downstreamSyncService.syncCalendars();
            case "OUTSOURCE" -> downstreamSyncService.syncOutsourceOrders();
            case "TRANSFER" -> downstreamSyncService.syncTransferOrders();
            default -> throw new BusinessException("不支持的同步类型: " + syncType);
        };
        return R.ok(result);
    }

    @Operation(summary = "手动触发上行同步（消费队列）")
    @PostMapping("/upstream")
    public R<ApsSyncResultVO> triggerUpstreamSync() {
        return R.ok(upstreamSyncService.processQueue());
    }

    @Operation(summary = "手动触发补偿同步")
    @PostMapping("/compensate")
    public R<ApsSyncResultVO> triggerCompensation() {
        return R.ok(compensationService.compensate());
    }

    @Operation(summary = "获取同步状态概览")
    @GetMapping("/status")
    public R<Map<String, Object>> getSyncStatus() {
        Map<String, Object> status = Map.of(
                "apsAvailable", apsClient.isAvailable(),
                "circuitBreakerState", apsClient.getCircuitBreakerState(),
                "pendingUpstreamCount", upstreamSyncService.getPendingCount(),
                "pendingCompensationCount", compensationService.getPendingCount()
        );
        return R.ok(status);
    }

    @Operation(summary = "APS 健康检查")
    @GetMapping("/health")
    public R<Map<String, Object>> healthCheck() {
        boolean available = apsClient.isAvailable();
        String cbState = apsClient.getCircuitBreakerState();
        Map<String, Object> health = Map.of(
                "apsAvailable", available,
                "circuitBreakerState", cbState
        );
        return R.ok(health);
    }

    // ==================== 主数据同步（MES→APS） ====================

    @Operation(summary = "手动触发主数据全量同步")
    @PostMapping("/master-data")
    public R<ApsSyncResultVO> triggerMasterDataSync() {
        return R.ok(masterDataSyncService.syncAllMasterData());
    }

    @Operation(summary = "手动触发主数据同步（指定类型）")
    @PostMapping("/master-data/{dataType}")
    public R<ApsSyncResultVO> triggerMasterDataSyncByType(@PathVariable String dataType) {
        ApsSyncResultVO result = switch (dataType.toUpperCase()) {
            case "WORK_CENTER" -> masterDataSyncService.syncWorkCenters();
            case "PROCESS_ROUTE" -> masterDataSyncService.syncProcessRoutes();
            case "BOM" -> masterDataSyncService.syncBoms();
            case "MATERIAL" -> masterDataSyncService.syncMaterials();
            case "TEAM" -> masterDataSyncService.syncTeams();
            default -> throw new BusinessException("不支持的主数据类型: " + dataType);
        };
        return R.ok(result);
    }
}
