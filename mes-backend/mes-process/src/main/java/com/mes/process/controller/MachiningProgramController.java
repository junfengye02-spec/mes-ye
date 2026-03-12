package com.mes.process.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.process.domain.dto.MachiningProgramDTO;
import com.mes.process.domain.query.MachiningProgramQuery;
import com.mes.process.domain.vo.MachiningProgramVO;
import com.mes.process.service.IMachiningProgramService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 机械加工程序 Controller
 */
@Tag(name = "机械加工程序", description = "机械加工程序管理接口")
@RestController
@RequestMapping("/process/machining-program")
@RequiredArgsConstructor
public class MachiningProgramController {

    private final IMachiningProgramService machiningProgramService;

    @Operation(summary = "分页查询机械加工程序")
    @GetMapping("/page")
    public R<PageResult<MachiningProgramVO>> page(MachiningProgramQuery query) {
        return R.ok(machiningProgramService.page(query));
    }

    @Operation(summary = "获取机械加工程序详情")
    @GetMapping("/{id}")
    public R<MachiningProgramVO> getDetail(
            @Parameter(description = "机械加工程序ID") @PathVariable Long id) {
        return R.ok(machiningProgramService.getDetail(id));
    }

    @Operation(summary = "新增机械加工程序")
    @PostMapping
    public R<Long> create(@Valid @RequestBody MachiningProgramDTO dto) {
        return R.ok("新增成功", machiningProgramService.create(dto));
    }

    @Operation(summary = "修改机械加工程序")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "机械加工程序ID") @PathVariable Long id,
            @Valid @RequestBody MachiningProgramDTO dto) {
        machiningProgramService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除机械加工程序")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "机械加工程序ID") @PathVariable Long id) {
        machiningProgramService.delete(id);
        return R.ok();
    }
}
