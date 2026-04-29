package com.mes.basic.controller;

import com.mes.basic.domain.dto.WorkCenterDTO;
import com.mes.basic.domain.query.WorkCenterQuery;
import com.mes.basic.domain.vo.WorkCenterVO;
import com.mes.basic.service.IWorkCenterService;
import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 工作中心 Controller
 */
@Tag(name = "工作中心", description = "工作中心管理接口")
@RestController
@RequestMapping("/basic/work-center")
@RequiredArgsConstructor
public class WorkCenterController {

    private final IWorkCenterService workCenterService;

    @Operation(summary = "分页查询工作中心")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('basic:workCenter:list')")
    public R<PageResult<WorkCenterVO>> page(WorkCenterQuery query) {
        return R.ok(workCenterService.page(query));
    }

    @Operation(summary = "获取工作中心详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('basic:workCenter:detail')")
    public R<WorkCenterVO> getDetail(
            @Parameter(description = "工作中心ID") @PathVariable Long id) {
        return R.ok(workCenterService.getDetail(id));
    }

    @Operation(summary = "新增工作中心")
    @PostMapping
    @PreAuthorize("hasAuthority('basic:workCenter:create')")
    public R<Long> create(@Valid @RequestBody WorkCenterDTO dto) {
        return R.ok("新增成功", workCenterService.create(dto));
    }

    @Operation(summary = "修改工作中心")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('basic:workCenter:update')")
    public R<Void> update(
            @Parameter(description = "工作中心ID") @PathVariable Long id,
            @Valid @RequestBody WorkCenterDTO dto) {
        workCenterService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除工作中心")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('basic:workCenter:delete')")
    public R<Void> delete(
            @Parameter(description = "工作中心ID") @PathVariable Long id) {
        workCenterService.delete(id);
        return R.ok();
    }

    @Operation(summary = "批量编辑工作中心")
    @PutMapping("/batch")
    @PreAuthorize("hasAuthority('basic:workCenter:update')")
    public R<Void> batchUpdate(@Valid @RequestBody java.util.List<WorkCenterDTO> dtoList,
                               @Parameter(description = "ID列表") @RequestParam java.util.List<Long> ids) {
        workCenterService.batchUpdate(dtoList, ids);
        return R.ok();
    }
}
