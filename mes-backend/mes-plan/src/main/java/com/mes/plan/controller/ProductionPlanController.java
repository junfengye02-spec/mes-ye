package com.mes.plan.controller;

import com.mes.common.core.PageQuery;
import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.plan.domain.dto.ProductionPlanDTO;
import com.mes.plan.domain.query.ProductionPlanQuery;
import com.mes.plan.domain.vo.PlanStatusLogVO;
import com.mes.plan.domain.vo.ProductionPlanVO;
import com.mes.plan.enums.PlanType;
import com.mes.plan.service.IPlanStatusLogService;
import com.mes.plan.service.IProductionPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 生产计划 Controller
 */
@Tag(name = "生产计划", description = "生产计划管理接口")
@RestController
@RequestMapping("/plan/production-plan")
@RequiredArgsConstructor
public class ProductionPlanController {

    private final IProductionPlanService productionPlanService;
    private final IPlanStatusLogService planStatusLogService;

    @Operation(summary = "分页查询生产计划")
    @GetMapping("/page")
    public R<PageResult<ProductionPlanVO>> page(ProductionPlanQuery query) {
        return R.ok(productionPlanService.page(query));
    }

    @Operation(summary = "获取生产计划详情")
    @GetMapping("/{id}")
    public R<ProductionPlanVO> getDetail(
            @Parameter(description = "生产计划ID") @PathVariable Long id) {
        return R.ok(productionPlanService.getDetail(id));
    }

    @Operation(summary = "新增生产计划")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ProductionPlanDTO dto) {
        return R.ok("新增成功", productionPlanService.create(dto));
    }

    @Operation(summary = "修改生产计划")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "生产计划ID") @PathVariable Long id,
            @Valid @RequestBody ProductionPlanDTO dto) {
        productionPlanService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除生产计划")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "生产计划ID") @PathVariable Long id) {
        productionPlanService.delete(id);
        return R.ok();
    }

    @Operation(summary = "下达生产计划")
    @PostMapping("/{id}/release")
    public R<Void> release(
            @Parameter(description = "生产计划ID") @PathVariable Long id) {
        productionPlanService.release(id);
        return R.ok();
    }

    @Operation(summary = "查询生产计划状态日志")
    @GetMapping("/{id}/status-logs")
    public R<PageResult<PlanStatusLogVO>> getStatusLogs(
            @Parameter(description = "生产计划ID") @PathVariable Long id,
            PageQuery query) {
        return R.ok(planStatusLogService.getLogsByPlanId(PlanType.PRODUCTION.getCode(), id, query));
    }
}
