package com.mes.basic.controller;

import com.mes.basic.domain.dto.MaterialDTO;
import com.mes.basic.domain.query.MaterialQuery;
import com.mes.basic.domain.vo.MaterialVO;
import com.mes.basic.service.IMaterialService;
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
 * 物料档案 Controller
 */
@Tag(name = "物料档案", description = "物料档案管理接口")
@RestController
@RequestMapping("/basic/material")
@RequiredArgsConstructor
public class MaterialController {

    private final IMaterialService materialService;

    @Operation(summary = "分页查询物料")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('basic:material:list')")
    public R<PageResult<MaterialVO>> page(MaterialQuery query) {
        return R.ok(materialService.page(query));
    }

    @Operation(summary = "获取物料详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('basic:material:detail')")
    public R<MaterialVO> getDetail(
            @Parameter(description = "物料ID") @PathVariable Long id) {
        return R.ok(materialService.getDetail(id));
    }

    @Operation(summary = "新增物料")
    @PostMapping
    @PreAuthorize("hasAuthority('basic:material:create')")
    public R<Long> create(@Valid @RequestBody MaterialDTO dto) {
        return R.ok("新增成功", materialService.create(dto));
    }

    @Operation(summary = "修改物料")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('basic:material:update')")
    public R<Void> update(
            @Parameter(description = "物料ID") @PathVariable Long id,
            @Valid @RequestBody MaterialDTO dto) {
        materialService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除物料")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('basic:material:delete')")
    public R<Void> delete(
            @Parameter(description = "物料ID") @PathVariable Long id) {
        materialService.delete(id);
        return R.ok();
    }
}
