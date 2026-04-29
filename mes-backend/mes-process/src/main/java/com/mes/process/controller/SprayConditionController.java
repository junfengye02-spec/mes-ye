package com.mes.process.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.process.domain.dto.SprayConditionDTO;
import com.mes.process.domain.query.SprayConditionQuery;
import com.mes.process.domain.vo.SprayConditionVO;
import com.mes.process.service.ISprayConditionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 喷涂条件 Controller
 */
@Tag(name = "喷涂条件", description = "喷涂条件管理接口")
@RestController
@RequestMapping("/process/spray-condition")
@RequiredArgsConstructor
public class SprayConditionController {

    private final ISprayConditionService sprayConditionService;

    @Operation(summary = "分页查询喷涂条件")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('process:sprayCondition:list')")
    public R<PageResult<SprayConditionVO>> page(SprayConditionQuery query) {
        return R.ok(sprayConditionService.page(query));
    }

    @Operation(summary = "获取喷涂条件详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('process:sprayCondition:detail')")
    public R<SprayConditionVO> getDetail(
            @Parameter(description = "喷涂条件ID") @PathVariable Long id) {
        return R.ok(sprayConditionService.getDetail(id));
    }

    @Operation(summary = "新增喷涂条件")
    @PostMapping
    @PreAuthorize("hasAuthority('process:sprayCondition:create')")
    public R<Long> create(@Valid @RequestBody SprayConditionDTO dto) {
        return R.ok("新增成功", sprayConditionService.create(dto));
    }

    @Operation(summary = "修改喷涂条件")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('process:sprayCondition:update')")
    public R<Void> update(
            @Parameter(description = "喷涂条件ID") @PathVariable Long id,
            @Valid @RequestBody SprayConditionDTO dto) {
        sprayConditionService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除喷涂条件")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('process:sprayCondition:delete')")
    public R<Void> delete(
            @Parameter(description = "喷涂条件ID") @PathVariable Long id) {
        sprayConditionService.delete(id);
        return R.ok();
    }
}
