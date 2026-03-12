package com.mes.process.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.process.domain.dto.ProcessInfoDTO;
import com.mes.process.domain.query.ProcessInfoQuery;
import com.mes.process.domain.vo.ProcessInfoVO;
import com.mes.process.service.IProcessInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工序信息 Controller
 */
@Tag(name = "工序信息", description = "工序信息管理接口")
@RestController
@RequestMapping("/process/process-info")
@RequiredArgsConstructor
public class ProcessInfoController {

    private final IProcessInfoService processInfoService;

    @Operation(summary = "分页查询工序信息")
    @GetMapping("/page")
    public R<PageResult<ProcessInfoVO>> page(ProcessInfoQuery query) {
        return R.ok(processInfoService.page(query));
    }

    @Operation(summary = "获取工序信息详情")
    @GetMapping("/{id}")
    public R<ProcessInfoVO> getDetail(
            @Parameter(description = "工序信息ID") @PathVariable Long id) {
        return R.ok(processInfoService.getDetail(id));
    }

    @Operation(summary = "新增工序信息")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ProcessInfoDTO dto) {
        return R.ok("新增成功", processInfoService.create(dto));
    }

    @Operation(summary = "修改工序信息")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "工序信息ID") @PathVariable Long id,
            @Valid @RequestBody ProcessInfoDTO dto) {
        processInfoService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除工序信息")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "工序信息ID") @PathVariable Long id) {
        processInfoService.delete(id);
        return R.ok();
    }

    @Operation(summary = "批量编辑工序信息")
    @PutMapping("/batch")
    public R<Void> batchUpdate(
            @Parameter(description = "ID列表") @RequestParam List<Long> ids,
            @Valid @RequestBody List<ProcessInfoDTO> dtoList) {
        processInfoService.batchUpdate(ids, dtoList);
        return R.ok();
    }
}
