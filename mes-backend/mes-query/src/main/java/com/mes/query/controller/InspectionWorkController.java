package com.mes.query.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.query.domain.query.InspectionWorkQuery;
import com.mes.query.domain.vo.InspectionWorkVO;
import com.mes.query.service.IInspectionWorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "检验工作查询", description = "检验工作查询接口")
@RestController
@RequestMapping("/query/inspection-work")
@RequiredArgsConstructor
public class InspectionWorkController {

    private final IInspectionWorkService inspectionWorkService;

    @Operation(summary = "分页查询检验工作")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('query:inspectionWork:list')")
    public R<PageResult<InspectionWorkVO>> page(InspectionWorkQuery query) {
        return R.ok(inspectionWorkService.page(query));
    }

    @Operation(summary = "获取检验工作详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('query:inspectionWork:detail')")
    public R<InspectionWorkVO> getDetail(@Parameter(description = "ID") @PathVariable Long id) {
        return R.ok(inspectionWorkService.getDetail(id));
    }
}
