package com.mes.process.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.process.domain.dto.WorkInstructionDTO;
import com.mes.process.domain.query.WorkInstructionQuery;
import com.mes.process.domain.vo.WorkInstructionVO;
import com.mes.process.service.IWorkInstructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 指导书 Controller
 */
@Tag(name = "指导书", description = "指导书管理接口")
@RestController
@RequestMapping("/process/work-instruction")
@RequiredArgsConstructor
public class WorkInstructionController {

    private final IWorkInstructionService workInstructionService;

    @Operation(summary = "分页查询指导书")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('process:workInstruction:list')")
    public R<PageResult<WorkInstructionVO>> page(WorkInstructionQuery query) {
        return R.ok(workInstructionService.page(query));
    }

    @Operation(summary = "获取指导书详情（含人员列表）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('process:workInstruction:detail')")
    public R<WorkInstructionVO> getDetail(
            @Parameter(description = "指导书ID") @PathVariable Long id) {
        return R.ok(workInstructionService.getDetail(id));
    }

    @Operation(summary = "新增指导书（含人员列表）")
    @PostMapping
    @PreAuthorize("hasAuthority('process:workInstruction:create')")
    public R<Long> create(@Valid @RequestBody WorkInstructionDTO dto) {
        return R.ok("新增成功", workInstructionService.create(dto));
    }

    @Operation(summary = "修改指导书（含人员列表）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('process:workInstruction:update')")
    public R<Void> update(
            @Parameter(description = "指导书ID") @PathVariable Long id,
            @Valid @RequestBody WorkInstructionDTO dto) {
        workInstructionService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除指导书（级联删除人员）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('process:workInstruction:delete')")
    public R<Void> delete(
            @Parameter(description = "指导书ID") @PathVariable Long id) {
        workInstructionService.delete(id);
        return R.ok();
    }
}
