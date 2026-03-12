package com.mes.process.controller;

import com.mes.common.core.PageQuery;
import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.process.domain.dto.InstructionDTO;
import com.mes.process.domain.query.InstructionQuery;
import com.mes.process.domain.vo.InstructionFlowLogVO;
import com.mes.process.domain.vo.InstructionVO;
import com.mes.process.service.IInstructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 指示书 Controller
 */
@Tag(name = "指示书", description = "指示书管理接口")
@RestController
@RequestMapping("/process/instruction")
@RequiredArgsConstructor
public class InstructionController {

    private final IInstructionService instructionService;

    @Operation(summary = "分页查询指示书")
    @GetMapping("/page")
    public R<PageResult<InstructionVO>> page(InstructionQuery query) {
        return R.ok(instructionService.page(query));
    }

    @Operation(summary = "获取指示书详情（含阶段+序列号）")
    @GetMapping("/{id}")
    public R<InstructionVO> getDetail(
            @Parameter(description = "指示书ID") @PathVariable Long id) {
        return R.ok(instructionService.getDetail(id));
    }

    @Operation(summary = "新增指示书")
    @PostMapping
    public R<Long> create(@Valid @RequestBody InstructionDTO dto) {
        return R.ok("新增成功", instructionService.create(dto));
    }

    @Operation(summary = "修改指示书")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "指示书ID") @PathVariable Long id,
            @Valid @RequestBody InstructionDTO dto) {
        instructionService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除指示书")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "指示书ID") @PathVariable Long id) {
        instructionService.delete(id);
        return R.ok();
    }

    @Operation(summary = "版本升级")
    @PostMapping("/{id}/upgrade")
    public R<Long> upgrade(
            @Parameter(description = "指示书ID") @PathVariable Long id) {
        return R.ok("升级成功", instructionService.upgrade(id));
    }

    @Operation(summary = "查询流程日志")
    @GetMapping("/{id}/flow-logs")
    public R<PageResult<InstructionFlowLogVO>> getFlowLogs(
            @Parameter(description = "指示书ID") @PathVariable Long id,
            PageQuery query) {
        return R.ok(instructionService.getFlowLogs(id, query));
    }
}
