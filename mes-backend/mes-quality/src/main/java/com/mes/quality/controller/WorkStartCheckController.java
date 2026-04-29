package com.mes.quality.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.quality.domain.dto.WorkStartCheckDTO;
import com.mes.quality.domain.query.WorkStartCheckQuery;
import com.mes.quality.domain.vo.WorkStartCheckVO;
import com.mes.quality.service.IWorkStartCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "工作开工检查", description = "生产工作开工检查管理接口")
@RestController
@RequestMapping("/quality/work-start-check")
@RequiredArgsConstructor
public class WorkStartCheckController {

    private final IWorkStartCheckService workStartCheckService;

    @Operation(summary = "分页查询工作开工检查")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('quality:workStartCheck:list')")
    public R<PageResult<WorkStartCheckVO>> page(WorkStartCheckQuery query) {
        return R.ok(workStartCheckService.page(query));
    }

    @Operation(summary = "获取工作开工检查详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('quality:workStartCheck:detail')")
    public R<WorkStartCheckVO> getDetail(@Parameter(description = "ID") @PathVariable Long id) {
        return R.ok(workStartCheckService.getDetail(id));
    }

    @Operation(summary = "新增工作开工检查")
    @PostMapping
    @PreAuthorize("hasAuthority('quality:workStartCheck:create')")
    public R<Long> create(@Valid @RequestBody WorkStartCheckDTO dto) {
        return R.ok("新增成功", workStartCheckService.create(dto));
    }

    @Operation(summary = "修改工作开工检查")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('quality:workStartCheck:update')")
    public R<Void> update(@Parameter(description = "ID") @PathVariable Long id,
                          @Valid @RequestBody WorkStartCheckDTO dto) {
        workStartCheckService.update(id, dto);
        return R.ok();
    }
}
