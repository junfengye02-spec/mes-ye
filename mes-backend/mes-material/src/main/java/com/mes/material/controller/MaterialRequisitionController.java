package com.mes.material.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.material.domain.dto.MaterialRequisitionDTO;
import com.mes.material.domain.query.MaterialRequisitionQuery;
import com.mes.material.domain.vo.MaterialRequisitionVO;
import com.mes.material.service.IMaterialRequisitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 生产领料 Controller
 */
@Tag(name = "生产领料", description = "生产领料申请管理接口")
@RestController
@RequestMapping("/material/requisition")
@RequiredArgsConstructor
public class MaterialRequisitionController {

    private final IMaterialRequisitionService materialRequisitionService;

    @Operation(summary = "分页查询生产领料")
    @GetMapping("/page")
    public R<PageResult<MaterialRequisitionVO>> page(MaterialRequisitionQuery query) {
        return R.ok(materialRequisitionService.page(query));
    }

    @Operation(summary = "获取生产领料详情")
    @GetMapping("/{id}")
    public R<MaterialRequisitionVO> getDetail(
            @Parameter(description = "生产领料ID") @PathVariable Long id) {
        return R.ok(materialRequisitionService.getDetail(id));
    }

    @Operation(summary = "新增生产领料")
    @PostMapping
    public R<Long> create(@Valid @RequestBody MaterialRequisitionDTO dto) {
        return R.ok("新增成功", materialRequisitionService.create(dto));
    }

    @Operation(summary = "修改生产领料")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "生产领料ID") @PathVariable Long id,
            @Valid @RequestBody MaterialRequisitionDTO dto) {
        materialRequisitionService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除生产领料")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "生产领料ID") @PathVariable Long id) {
        materialRequisitionService.delete(id);
        return R.ok();
    }
}
