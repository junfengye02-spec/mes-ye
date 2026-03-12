package com.mes.team.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.team.domain.dto.ProductionTeamDTO;
import com.mes.team.domain.query.ProductionTeamQuery;
import com.mes.team.domain.vo.ProductionTeamVO;
import com.mes.team.service.IProductionTeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 生产班组 Controller
 */
@Tag(name = "生产班组", description = "生产班组管理接口")
@RestController
@RequestMapping("/team/production-team")
@RequiredArgsConstructor
public class ProductionTeamController {

    private final IProductionTeamService productionTeamService;

    @Operation(summary = "分页查询班组")
    @GetMapping("/page")
    public R<PageResult<ProductionTeamVO>> page(ProductionTeamQuery query) {
        return R.ok(productionTeamService.page(query));
    }

    @Operation(summary = "获取班组详情")
    @GetMapping("/{id}")
    public R<ProductionTeamVO> getDetail(
            @Parameter(description = "班组ID") @PathVariable Long id) {
        return R.ok(productionTeamService.getDetail(id));
    }

    @Operation(summary = "新增班组")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ProductionTeamDTO dto) {
        return R.ok("新增成功", productionTeamService.create(dto));
    }

    @Operation(summary = "修改班组")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "班组ID") @PathVariable Long id,
            @Valid @RequestBody ProductionTeamDTO dto) {
        productionTeamService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除班组")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "班组ID") @PathVariable Long id) {
        productionTeamService.delete(id);
        return R.ok();
    }

    @Operation(summary = "启用/停用班组")
    @PutMapping("/{id}/toggle-enabled")
    public R<Void> toggleEnabled(
            @Parameter(description = "班组ID") @PathVariable Long id) {
        productionTeamService.toggleEnabled(id);
        return R.ok();
    }
}
