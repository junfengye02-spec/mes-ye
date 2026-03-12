package com.mes.dispatch.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.dispatch.domain.query.DispatchTaskQuery;
import com.mes.dispatch.domain.vo.DispatchTaskVO;
import com.mes.dispatch.service.IDispatchTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 派工任务 Controller
 */
@Tag(name = "派工任务", description = "派工任务管理接口")
@RestController
@RequestMapping("/dispatch/task")
@RequiredArgsConstructor
public class DispatchTaskController {

    private final IDispatchTaskService dispatchTaskService;

    @Operation(summary = "分页查询派工任务")
    @GetMapping("/page")
    public R<PageResult<DispatchTaskVO>> page(DispatchTaskQuery query) {
        return R.ok(dispatchTaskService.page(query));
    }

    @Operation(summary = "获取派工任务详情")
    @GetMapping("/{id}")
    public R<DispatchTaskVO> getDetail(
            @Parameter(description = "派工任务ID") @PathVariable Long id) {
        return R.ok(dispatchTaskService.getDetail(id));
    }

    @Operation(summary = "从工单生成派工任务")
    @PostMapping("/generate/{workOrderId}")
    public R<Void> generateFromWorkOrder(
            @Parameter(description = "工单ID") @PathVariable Long workOrderId) {
        dispatchTaskService.generateFromWorkOrder(workOrderId);
        return R.ok();
    }
}
