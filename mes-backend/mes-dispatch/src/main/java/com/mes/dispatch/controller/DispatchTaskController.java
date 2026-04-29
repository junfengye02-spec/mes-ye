package com.mes.dispatch.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.dispatch.domain.dto.DispatchTaskAssignDTO;
import com.mes.dispatch.domain.dto.DispatchTaskCompleteDTO;
import com.mes.dispatch.domain.dto.DispatchTaskCreateDTO;
import com.mes.dispatch.domain.dto.DispatchTaskUpdateDTO;
import com.mes.dispatch.domain.query.DispatchTaskQuery;
import com.mes.dispatch.domain.vo.DispatchTaskVO;
import com.mes.dispatch.service.IDispatchTaskService;
import com.mes.framework.sentinel.MesRateLimit;
import com.mes.framework.sentinel.SentinelBlockHandlers;
import com.mes.framework.sentinel.SentinelResources;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 派工任务 Controller
 * <p>读接口：/page、/&#123;id&#125;、/generate/&#123;workOrderId&#125;</p>
 * <p>写接口（P0-03 补齐）：/create、/update、/cancel/&#123;id&#125;、/assign、/unassign/&#123;id&#125;、/start/&#123;id&#125;、/complete/&#123;id&#125;</p>
 */
@Tag(name = "派工任务", description = "派工任务管理接口")
@RestController
@RequestMapping("/dispatch/task")
@RequiredArgsConstructor
public class DispatchTaskController {

    private final IDispatchTaskService dispatchTaskService;

    // ==================== 读接口 ====================

    @Operation(summary = "分页查询派工任务", description = "单机 200 QPS 限流，防止扫表")
    @PreAuthorize("hasAuthority('dispatch:task:list')")
    @GetMapping("/page")
    @SentinelResource(value = SentinelResources.DISPATCH_TASK_PAGE,
            blockHandler = "handleR", blockHandlerClass = SentinelBlockHandlers.class)
    @MesRateLimit(resource = SentinelResources.DISPATCH_TASK_PAGE, key = MesRateLimit.Key.DEFAULT, count = 200)
    public R<PageResult<DispatchTaskVO>> page(DispatchTaskQuery query) {
        return R.ok(dispatchTaskService.page(query));
    }

    @Operation(summary = "获取派工任务详情")
    @PreAuthorize("hasAuthority('dispatch:task:detail')")
    @GetMapping("/{id}")
    public R<DispatchTaskVO> getDetail(
            @Parameter(description = "派工任务ID") @PathVariable Long id) {
        return R.ok(dispatchTaskService.getDetail(id));
    }

    @Operation(summary = "从工单生成派工任务")
    @PreAuthorize("hasAuthority('dispatch:task:generate')")
    @PostMapping("/generate/{workOrderId}")
    public R<Void> generateFromWorkOrder(
            @Parameter(description = "工单ID") @PathVariable Long workOrderId) {
        dispatchTaskService.generateFromWorkOrder(workOrderId);
        return R.ok();
    }

    // ==================== P0-03 新增写接口 ====================

    @Operation(summary = "手动创建派工单")
    @PreAuthorize("hasAuthority('dispatch:task:create')")
    @PostMapping("/create")
    public R<Long> create(@Valid @RequestBody DispatchTaskCreateDTO dto) {
        return R.ok(dispatchTaskService.create(dto));
    }

    @Operation(summary = "更新派工单（仅未开工状态可改）")
    @PreAuthorize("hasAuthority('dispatch:task:update')")
    @PutMapping("/update")
    public R<Void> update(@Valid @RequestBody DispatchTaskUpdateDTO dto) {
        dispatchTaskService.update(dto);
        return R.ok();
    }

    @Operation(summary = "撤销派工（必须记录撤销原因）")
    @PreAuthorize("hasAuthority('dispatch:task:cancel')")
    @PostMapping("/cancel/{id}")
    public R<Void> cancel(
            @Parameter(description = "派工任务ID") @PathVariable Long id,
            @Parameter(description = "撤销原因", required = true) @RequestParam String cancelReason) {
        dispatchTaskService.cancel(id, cancelReason);
        return R.ok();
    }

    @Operation(summary = "派工指派（支持按人员/设备/班组批量指派）")
    @PreAuthorize("hasAuthority('dispatch:task:assign')")
    @PostMapping("/assign")
    public R<Void> assign(@Valid @RequestBody DispatchTaskAssignDTO dto) {
        dispatchTaskService.assign(dto);
        return R.ok();
    }

    @Operation(summary = "取消指派（按分配记录 ID 撤销）")
    @PreAuthorize("hasAuthority('dispatch:task:unassign')")
    @PostMapping("/unassign/{id}")
    public R<Void> unassign(
            @Parameter(description = "分配记录ID（mes_dispatch_assignment.id）") @PathVariable Long id,
            @Parameter(description = "取消原因", required = true) @RequestParam String reason) {
        dispatchTaskService.unassign(id, reason);
        return R.ok();
    }

    @Operation(summary = "开工（ASSIGNED → IN_PROGRESS）")
    @PreAuthorize("hasAuthority('dispatch:task:start')")
    @PostMapping("/start/{id}")
    public R<Void> start(
            @Parameter(description = "派工任务ID") @PathVariable Long id) {
        dispatchTaskService.start(id);
        return R.ok();
    }

    @Operation(summary = "完工（IN_PROGRESS → COMPLETED）")
    @PreAuthorize("hasAuthority('dispatch:task:complete')")
    @PostMapping("/complete/{id}")
    public R<Void> complete(
            @Parameter(description = "派工任务ID") @PathVariable Long id,
            @Valid @RequestBody DispatchTaskCompleteDTO dto) {
        dispatchTaskService.complete(id, dto);
        return R.ok();
    }
}
