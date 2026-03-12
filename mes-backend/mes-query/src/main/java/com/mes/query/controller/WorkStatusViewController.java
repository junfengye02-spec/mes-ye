package com.mes.query.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.query.domain.query.WorkStatusViewQuery;
import com.mes.query.domain.vo.WorkStatusViewVO;
import com.mes.query.service.IWorkStatusViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "工作状态查看", description = "工作六状态查看接口")
@RestController
@RequestMapping("/query/work-status-view")
@RequiredArgsConstructor
public class WorkStatusViewController {

    private final IWorkStatusViewService workStatusViewService;

    @Operation(summary = "分页查询工作状态（支持六状态Tab过滤）")
    @GetMapping("/page")
    public R<PageResult<WorkStatusViewVO>> page(WorkStatusViewQuery query) {
        return R.ok(workStatusViewService.page(query));
    }
}
