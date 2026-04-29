package com.mes.material.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.material.domain.dto.RequisitionOrderDTO;
import com.mes.material.domain.query.RequisitionOrderQuery;
import com.mes.material.domain.vo.RequisitionOrderVO;
import com.mes.material.service.IRequisitionOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 领料单管理（按物料）Controller
 */
@Tag(name = "领料单管理（按物料）", description = "生产领料按物料汇总管理接口")
@RestController
@RequestMapping("/material/requisition-order")
@RequiredArgsConstructor
public class RequisitionOrderController {

    private final IRequisitionOrderService requisitionOrderService;

    @Operation(summary = "分页查询领料单")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('material:requisitionOrder:list')")
    public R<PageResult<RequisitionOrderVO>> page(RequisitionOrderQuery query) {
        return R.ok(requisitionOrderService.page(query));
    }

    @Operation(summary = "获取领料单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('material:requisitionOrder:detail')")
    public R<RequisitionOrderVO> getDetail(
            @Parameter(description = "领料单ID") @PathVariable Long id) {
        return R.ok(requisitionOrderService.getDetail(id));
    }

    @Operation(summary = "新增领料单")
    @PostMapping
    @PreAuthorize("hasAuthority('material:requisitionOrder:create')")
    public R<Long> create(@Valid @RequestBody RequisitionOrderDTO dto) {
        return R.ok("新增成功", requisitionOrderService.create(dto));
    }

    @Operation(summary = "修改领料单")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('material:requisitionOrder:update')")
    public R<Void> update(
            @Parameter(description = "领料单ID") @PathVariable Long id,
            @Valid @RequestBody RequisitionOrderDTO dto) {
        requisitionOrderService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除领料单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('material:requisitionOrder:delete')")
    public R<Void> delete(
            @Parameter(description = "领料单ID") @PathVariable Long id) {
        requisitionOrderService.delete(id);
        return R.ok();
    }
}
