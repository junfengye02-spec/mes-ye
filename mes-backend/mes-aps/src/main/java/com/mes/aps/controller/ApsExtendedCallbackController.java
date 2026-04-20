package com.mes.aps.controller;

import com.mes.aps.domain.vo.*;
import com.mes.aps.service.IApsExtendedCallbackService;
import com.mes.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * APS 扩展回调接收 Controller
 * <p>接收 APS 下发的 MRP、资源分配、甘特图、产能负荷、排程变更等数据</p>
 */
@Tag(name = "APS扩展回调接收", description = "接收APS下发的扩展排程数据")
@RestController
@RequestMapping("/aps/callback")
@RequiredArgsConstructor
public class ApsExtendedCallbackController {

    private final IApsExtendedCallbackService callbackService;

    @Operation(summary = "接收物料需求计划(MRP)")
    @PostMapping("/mrp-result")
    public R<Void> receiveMrpResult(@RequestBody ApsMrpCallbackVO mrpData) {
        callbackService.handleMrpResult(mrpData);
        return R.ok();
    }

    @Operation(summary = "接收资源分配计划")
    @PostMapping("/resource-allocation")
    public R<Void> receiveResourceAllocation(@RequestBody ApsResourceAllocationVO allocationData) {
        callbackService.handleResourceAllocation(allocationData);
        return R.ok();
    }

    @Operation(summary = "接收排程甘特图数据")
    @PostMapping("/gantt-data")
    public R<Void> receiveGanttData(@RequestBody ApsGanttDataVO ganttData) {
        callbackService.handleGanttData(ganttData);
        return R.ok();
    }

    @Operation(summary = "接收产能负荷数据")
    @PostMapping("/capacity-load")
    public R<Void> receiveCapacityLoad(@RequestBody ApsCapacityLoadVO capacityData) {
        callbackService.handleCapacityLoad(capacityData);
        return R.ok();
    }

    @Operation(summary = "接收排程变更通知")
    @PostMapping("/schedule-change")
    public R<Void> receiveScheduleChange(@RequestBody ApsScheduleChangeVO changeData) {
        callbackService.handleScheduleChange(changeData);
        return R.ok();
    }
}
