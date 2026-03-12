package com.mes.workorder.controller;

import com.mes.common.core.PageQuery;
import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.workorder.domain.dto.WorkOrderDTO;
import com.mes.workorder.domain.query.WorkOrderQuery;
import com.mes.workorder.domain.vo.WorkOrderStatusLogVO;
import com.mes.workorder.domain.vo.WorkOrderVO;
import com.mes.workorder.service.IWorkOrderService;
import com.mes.workorder.service.IWorkOrderStatusLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 生产工单 Controller
 */
@Tag(name = "生产工单", description = "生产工单管理接口")
@RestController
@RequestMapping("/workorder/work-order")
@RequiredArgsConstructor
public class WorkOrderController {

    private final IWorkOrderService workOrderService;
    private final IWorkOrderStatusLogService statusLogService;

    @Operation(summary = "分页查询工单")
    @GetMapping("/page")
    public R<PageResult<WorkOrderVO>> page(WorkOrderQuery query) {
        return R.ok(workOrderService.page(query));
    }

    @Operation(summary = "获取工单详情")
    @GetMapping("/{id}")
    public R<WorkOrderVO> getDetail(
            @Parameter(description = "工单ID") @PathVariable Long id) {
        return R.ok(workOrderService.getDetail(id));
    }

    @Operation(summary = "新增工单")
    @PostMapping
    public R<Long> create(@Valid @RequestBody WorkOrderDTO dto) {
        return R.ok("新增成功", workOrderService.create(dto));
    }

    @Operation(summary = "修改工单")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "工单ID") @PathVariable Long id,
            @Valid @RequestBody WorkOrderDTO dto) {
        workOrderService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除工单")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "工单ID") @PathVariable Long id) {
        workOrderService.delete(id);
        return R.ok();
    }

    @Operation(summary = "下发工单")
    @PostMapping("/{id}/release")
    public R<Void> release(
            @Parameter(description = "工单ID") @PathVariable Long id) {
        workOrderService.release(id);
        return R.ok();
    }

    @Operation(summary = "开工")
    @PostMapping("/{id}/start")
    public R<Void> start(
            @Parameter(description = "工单ID") @PathVariable Long id) {
        workOrderService.start(id);
        return R.ok();
    }

    @Operation(summary = "完工")
    @PostMapping("/{id}/complete")
    public R<Void> complete(
            @Parameter(description = "工单ID") @PathVariable Long id) {
        workOrderService.complete(id);
        return R.ok();
    }

    @Operation(summary = "强制完工")
    @PostMapping("/{id}/force-complete")
    public R<Void> forceComplete(
            @Parameter(description = "工单ID") @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            return R.fail("强制完工原因不能为空");
        }
        workOrderService.forceComplete(id, reason);
        return R.ok();
    }

    @Operation(summary = "查询工单状态日志")
    @GetMapping("/{id}/status-logs")
    public R<PageResult<WorkOrderStatusLogVO>> getStatusLogs(
            @Parameter(description = "工单ID") @PathVariable Long id,
            PageQuery query) {
        return R.ok(statusLogService.getLogsByWorkOrderId(id, query));
    }
}
