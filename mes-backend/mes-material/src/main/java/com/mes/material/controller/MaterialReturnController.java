package com.mes.material.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.material.domain.dto.MaterialReturnDTO;
import com.mes.material.domain.query.MaterialReturnQuery;
import com.mes.material.domain.vo.MaterialReturnVO;
import com.mes.material.service.IMaterialReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 生产退料 Controller
 */
@Tag(name = "生产退料", description = "生产退料申请管理接口")
@RestController
@RequestMapping("/material/return")
@RequiredArgsConstructor
public class MaterialReturnController {

    private final IMaterialReturnService materialReturnService;

    @Operation(summary = "分页查询生产退料")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('material:return:list')")
    public R<PageResult<MaterialReturnVO>> page(MaterialReturnQuery query) {
        return R.ok(materialReturnService.page(query));
    }

    @Operation(summary = "获取生产退料详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('material:return:detail')")
    public R<MaterialReturnVO> getDetail(
            @Parameter(description = "生产退料ID") @PathVariable Long id) {
        return R.ok(materialReturnService.getDetail(id));
    }

    @Operation(summary = "新增生产退料")
    @PostMapping
    @PreAuthorize("hasAuthority('material:return:create')")
    public R<Long> create(@Valid @RequestBody MaterialReturnDTO dto) {
        return R.ok("新增成功", materialReturnService.create(dto));
    }

    @Operation(summary = "修改生产退料")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('material:return:update')")
    public R<Void> update(
            @Parameter(description = "生产退料ID") @PathVariable Long id,
            @Valid @RequestBody MaterialReturnDTO dto) {
        materialReturnService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除生产退料")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('material:return:delete')")
    public R<Void> delete(
            @Parameter(description = "生产退料ID") @PathVariable Long id) {
        materialReturnService.delete(id);
        return R.ok();
    }
}
