package com.mes.process.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.process.domain.dto.ManufacturingBomDTO;
import com.mes.process.domain.query.ManufacturingBomQuery;
import com.mes.process.domain.vo.ManufacturingBomItemVO;
import com.mes.process.domain.vo.ManufacturingBomVO;
import com.mes.process.service.IManufacturingBomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 制造BOM Controller
 */
@Tag(name = "制造BOM", description = "制造BOM管理接口")
@RestController
@RequestMapping("/process/manufacturing-bom")
@RequiredArgsConstructor
public class ManufacturingBomController {

    private final IManufacturingBomService manufacturingBomService;

    @Operation(summary = "分页查询制造BOM")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('process:bom:list')")
    public R<PageResult<ManufacturingBomVO>> page(ManufacturingBomQuery query) {
        return R.ok(manufacturingBomService.page(query));
    }

    @Operation(summary = "获取制造BOM详情（含树形明细）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('process:bom:detail')")
    public R<ManufacturingBomVO> getDetail(
            @Parameter(description = "BOM ID") @PathVariable Long id) {
        return R.ok(manufacturingBomService.getDetail(id));
    }

    @Operation(summary = "新增制造BOM")
    @PostMapping
    @PreAuthorize("hasAuthority('process:bom:create')")
    public R<Long> create(@Valid @RequestBody ManufacturingBomDTO dto) {
        return R.ok("新增成功", manufacturingBomService.create(dto));
    }

    @Operation(summary = "修改制造BOM")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('process:bom:update')")
    public R<Void> update(
            @Parameter(description = "BOM ID") @PathVariable Long id,
            @Valid @RequestBody ManufacturingBomDTO dto) {
        manufacturingBomService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除制造BOM")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('process:bom:delete')")
    public R<Void> delete(
            @Parameter(description = "BOM ID") @PathVariable Long id) {
        manufacturingBomService.delete(id);
        return R.ok();
    }

    @Operation(summary = "BOM版本升级")
    @PostMapping("/{id}/upgrade")
    @PreAuthorize("hasAuthority('process:bom:upgrade')")
    public R<Long> upgrade(
            @Parameter(description = "BOM ID") @PathVariable Long id) {
        return R.ok("升级成功", manufacturingBomService.upgrade(id));
    }

    @Operation(summary = "发布BOM（DRAFT→PUBLISHED）")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('process:bom:publish')")
    public R<Void> publish(
            @Parameter(description = "BOM ID") @PathVariable Long id) {
        manufacturingBomService.publish(id);
        return R.ok();
    }

    @Operation(summary = "停用BOM（PUBLISHED→DISABLED）")
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('process:bom:disable')")
    public R<Void> disable(
            @Parameter(description = "BOM ID") @PathVariable Long id) {
        manufacturingBomService.disable(id);
        return R.ok();
    }

    @Operation(summary = "获取BOM明细树")
    @GetMapping("/{id}/items/tree")
    @PreAuthorize("hasAuthority('process:bom:detail')")
    public R<List<ManufacturingBomItemVO>> getItemTree(
            @Parameter(description = "BOM ID") @PathVariable Long id) {
        return R.ok(manufacturingBomService.getItemTree(id));
    }
}
