package com.mes.plan.controller;

import com.mes.common.core.PageQuery;
import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.plan.domain.dto.OrderPlanDTO;
import com.mes.plan.domain.query.OrderPlanQuery;
import com.mes.plan.domain.vo.OrderPlanVO;
import com.mes.plan.domain.vo.PlanStatusLogVO;
import com.mes.plan.enums.PlanType;
import com.mes.plan.service.IOrderPlanService;
import com.mes.plan.service.IPlanStatusLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 订单计划 Controller
 */
@Tag(name = "订单计划", description = "订单计划管理接口")
@RestController
@RequestMapping("/plan/order-plan")
@RequiredArgsConstructor
public class OrderPlanController {

    private final IOrderPlanService orderPlanService;
    private final IPlanStatusLogService planStatusLogService;

    @Operation(summary = "分页查询订单计划")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('plan:order:list')")
    public R<PageResult<OrderPlanVO>> page(OrderPlanQuery query) {
        return R.ok(orderPlanService.page(query));
    }

    @Operation(summary = "获取订单计划详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('plan:order:detail')")
    public R<OrderPlanVO> getDetail(
            @Parameter(description = "订单计划ID") @PathVariable Long id) {
        return R.ok(orderPlanService.getDetail(id));
    }

    @Operation(summary = "新增订单计划")
    @PostMapping
    @PreAuthorize("hasAuthority('plan:order:create')")
    public R<Long> create(@Valid @RequestBody OrderPlanDTO dto) {
        return R.ok("新增成功", orderPlanService.create(dto));
    }

    @Operation(summary = "修改订单计划")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('plan:order:update')")
    public R<Void> update(
            @Parameter(description = "订单计划ID") @PathVariable Long id,
            @Valid @RequestBody OrderPlanDTO dto) {
        orderPlanService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除订单计划")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('plan:order:delete')")
    public R<Void> delete(
            @Parameter(description = "订单计划ID") @PathVariable Long id) {
        orderPlanService.delete(id);
        return R.ok();
    }

    @Operation(summary = "下达订单计划")
    @PostMapping("/{id}/release")
    @PreAuthorize("hasAuthority('plan:order:release')")
    public R<Void> release(
            @Parameter(description = "订单计划ID") @PathVariable Long id) {
        orderPlanService.release(id);
        return R.ok();
    }

    @Operation(summary = "完成订单计划")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('plan:order:complete')")
    public R<Void> complete(
            @Parameter(description = "订单计划ID") @PathVariable Long id) {
        orderPlanService.complete(id);
        return R.ok();
    }

    @Operation(summary = "终止订单计划")
    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAuthority('plan:order:terminate')")
    public R<Void> terminate(
            @Parameter(description = "订单计划ID") @PathVariable Long id,
            @Parameter(description = "终止原因") @RequestParam String reason) {
        orderPlanService.terminate(id, reason);
        return R.ok();
    }

    @Operation(summary = "展开订单计划")
    @PostMapping("/{id}/expand")
    @PreAuthorize("hasAuthority('plan:order:expand')")
    public R<Void> expand(
            @Parameter(description = "订单计划ID") @PathVariable Long id) {
        orderPlanService.expand(id);
        return R.ok();
    }

    @Operation(summary = "查询订单计划状态日志")
    @GetMapping("/{id}/status-logs")
    @PreAuthorize("hasAuthority('plan:order:log')")
    public R<PageResult<PlanStatusLogVO>> getStatusLogs(
            @Parameter(description = "订单计划ID") @PathVariable Long id,
            PageQuery query) {
        return R.ok(planStatusLogService.getLogsByPlanId(PlanType.ORDER.getCode(), id, query));
    }
}
