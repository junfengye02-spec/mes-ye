package com.mes.process.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.process.domain.dto.ProcessTemplateDTO;
import com.mes.process.domain.query.ProcessTemplateQuery;
import com.mes.process.domain.vo.ProcessTemplateVO;
import com.mes.process.service.IProcessTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工序模板 Controller
 */
@Tag(name = "工序模板", description = "工序模板管理接口")
@RestController
@RequestMapping("/process/process-template")
@RequiredArgsConstructor
public class ProcessTemplateController {

    private final IProcessTemplateService processTemplateService;

    @Operation(summary = "分页查询工序模板")
    @GetMapping("/page")
    public R<PageResult<ProcessTemplateVO>> page(ProcessTemplateQuery query) {
        return R.ok(processTemplateService.page(query));
    }

    @Operation(summary = "树形结构查询工序模板")
    @GetMapping("/tree")
    public R<List<ProcessTemplateVO>> tree() {
        return R.ok(processTemplateService.tree());
    }

    @Operation(summary = "获取工序模板详情")
    @GetMapping("/{id}")
    public R<ProcessTemplateVO> getDetail(
            @Parameter(description = "工序模板ID") @PathVariable Long id) {
        return R.ok(processTemplateService.getDetail(id));
    }

    @Operation(summary = "新增工序模板")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ProcessTemplateDTO dto) {
        return R.ok("新增成功", processTemplateService.create(dto));
    }

    @Operation(summary = "修改工序模板")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "工序模板ID") @PathVariable Long id,
            @Valid @RequestBody ProcessTemplateDTO dto) {
        processTemplateService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除工序模板")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "工序模板ID") @PathVariable Long id) {
        processTemplateService.delete(id);
        return R.ok();
    }
}
