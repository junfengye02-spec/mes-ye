package com.mes.basic.controller;

import com.mes.basic.domain.dto.MaterialPriceDTO;
import com.mes.basic.domain.query.MaterialPriceQuery;
import com.mes.basic.domain.vo.MaterialPriceVO;
import com.mes.basic.service.IMaterialPriceService;
import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 物料价格 Controller
 */
@Tag(name = "物料价格", description = "物料价格维护接口")
@RestController
@RequestMapping("/basic/material-price")
@RequiredArgsConstructor
public class MaterialPriceController {

    private final IMaterialPriceService materialPriceService;

    @Operation(summary = "分页查询物料价格")
    @GetMapping("/page")
    public R<PageResult<MaterialPriceVO>> page(MaterialPriceQuery query) {
        return R.ok(materialPriceService.page(query));
    }

    @Operation(summary = "获取物料价格详情")
    @GetMapping("/{id}")
    public R<MaterialPriceVO> getDetail(
            @Parameter(description = "价格ID") @PathVariable Long id) {
        return R.ok(materialPriceService.getDetail(id));
    }

    @Operation(summary = "新增物料价格")
    @PostMapping
    public R<Long> create(@Valid @RequestBody MaterialPriceDTO dto) {
        return R.ok("新增成功", materialPriceService.create(dto));
    }

    @Operation(summary = "修改物料价格")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "价格ID") @PathVariable Long id,
            @Valid @RequestBody MaterialPriceDTO dto) {
        materialPriceService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除物料价格")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "价格ID") @PathVariable Long id) {
        materialPriceService.delete(id);
        return R.ok();
    }
}
