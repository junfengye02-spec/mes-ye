package com.mes.workorder.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.mes.common.core.PageQuery;
import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.framework.sentinel.MesRateLimit;
import com.mes.framework.sentinel.SentinelBlockHandlers;
import com.mes.framework.sentinel.SentinelResources;
import com.mes.workorder.domain.dto.WorkOrderDTO;
import com.mes.workorder.domain.query.WorkOrderQuery;
import com.mes.workorder.domain.vo.WorkOrderStatusLogVO;
import com.mes.workorder.domain.vo.WorkOrderVO;
import com.mes.workorder.search.IWorkOrderSearchService;
import com.mes.workorder.service.IWorkOrderService;
import com.mes.workorder.service.IWorkOrderStatusLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final IWorkOrderSearchService workOrderSearchService;

    @Operation(summary = "分页查询工单", description = "单机 200 QPS 限流，防止扫表")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('workorder:workorder:list')")
    @SentinelResource(value = SentinelResources.WORKORDER_LIST,
            blockHandler = "handleR", blockHandlerClass = SentinelBlockHandlers.class)
    @MesRateLimit(resource = SentinelResources.WORKORDER_LIST, key = MesRateLimit.Key.DEFAULT, count = 200)
    public R<PageResult<WorkOrderVO>> page(WorkOrderQuery query) {
        return R.ok(workOrderService.page(query));
    }

    /**
     * 富查询（P2-28 ES 加速）
     * <p>业务语义与 {@link #page(WorkOrderQuery)} 一致，区别在于：</p>
     * <ul>
     *   <li>启用 ES（mes.es.enabled=true）时优先走 ES，支持中文分词 / 高性能模糊</li>
     *   <li>ES 未启用或查询异常时自动降级到 MyBatis，调用方无感知</li>
     * </ul>
     * <p>限流复用 {@link SentinelResources#WORKORDER_LIST}，防止大范围模糊扫索引。</p>
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Operation(summary = "富查询（ES 加速，失败自动降级 MyBatis）")
    @GetMapping("/query-rich")
    @PreAuthorize("hasAuthority('workorder:workorder:list')")
    @SentinelResource(value = SentinelResources.WORKORDER_LIST,
            blockHandler = "handleR", blockHandlerClass = SentinelBlockHandlers.class)
    @MesRateLimit(resource = SentinelResources.WORKORDER_LIST, key = MesRateLimit.Key.DEFAULT, count = 200)
    public R<PageResult<WorkOrderVO>> queryRich(WorkOrderQuery query) {
        return R.ok(workOrderSearchService.queryRich(query));
    }

    @Operation(summary = "获取工单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('workorder:workorder:detail')")
    public R<WorkOrderVO> getDetail(
            @Parameter(description = "工单ID") @PathVariable Long id) {
        return R.ok(workOrderService.getDetail(id));
    }

    @Operation(summary = "新增工单")
    @PostMapping
    @PreAuthorize("hasAuthority('workorder:workorder:create')")
    public R<Long> create(@Valid @RequestBody WorkOrderDTO dto) {
        return R.ok("新增成功", workOrderService.create(dto));
    }

    @Operation(summary = "修改工单")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('workorder:workorder:update')")
    public R<Void> update(
            @Parameter(description = "工单ID") @PathVariable Long id,
            @Valid @RequestBody WorkOrderDTO dto) {
        workOrderService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除工单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('workorder:workorder:delete')")
    public R<Void> delete(
            @Parameter(description = "工单ID") @PathVariable Long id) {
        workOrderService.delete(id);
        return R.ok();
    }

    @Operation(summary = "下发工单")
    @PostMapping("/{id}/release")
    @PreAuthorize("hasAuthority('workorder:workorder:release')")
    public R<Void> release(
            @Parameter(description = "工单ID") @PathVariable Long id) {
        workOrderService.release(id);
        return R.ok();
    }

    @Operation(summary = "开工")
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('workorder:workorder:start')")
    public R<Void> start(
            @Parameter(description = "工单ID") @PathVariable Long id) {
        workOrderService.start(id);
        return R.ok();
    }

    @Operation(summary = "完工")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('workorder:workorder:complete')")
    public R<Void> complete(
            @Parameter(description = "工单ID") @PathVariable Long id) {
        workOrderService.complete(id);
        return R.ok();
    }

    @Operation(summary = "强制完工")
    @PostMapping("/{id}/force-complete")
    @PreAuthorize("hasAuthority('workorder:workorder:forceComplete')")
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
    @PreAuthorize("hasAuthority('workorder:workorder:log')")
    public R<PageResult<WorkOrderStatusLogVO>> getStatusLogs(
            @Parameter(description = "工单ID") @PathVariable Long id,
            PageQuery query) {
        return R.ok(statusLogService.getLogsByWorkOrderId(id, query));
    }
}
