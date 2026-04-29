package com.mes.material.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.material.domain.dto.FinishedGoodsReceiptDTO;
import com.mes.material.domain.dto.ReceiptRequestDTO;
import com.mes.material.domain.query.FinishedGoodsReceiptQuery;
import com.mes.material.domain.query.ReceiptRequestQuery;
import com.mes.material.domain.vo.FinishedGoodsReceiptVO;
import com.mes.material.domain.vo.ReceiptRequestVO;
import com.mes.material.service.IFinishedGoodsReceiptService;
import com.mes.material.service.IReceiptRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 完工入库 Controller
 */
@Tag(name = "完工入库", description = "完工入库管理接口")
@RestController
@RequestMapping("/material/receipt")
@RequiredArgsConstructor
public class FinishedGoodsReceiptController {

    private final IFinishedGoodsReceiptService finishedGoodsReceiptService;
    private final IReceiptRequestService receiptRequestService;

    @Operation(summary = "分页查询完工入库")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('material:receipt:list')")
    public R<PageResult<FinishedGoodsReceiptVO>> page(FinishedGoodsReceiptQuery query) {
        return R.ok(finishedGoodsReceiptService.page(query));
    }

    @Operation(summary = "获取完工入库详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('material:receipt:detail')")
    public R<FinishedGoodsReceiptVO> getDetail(
            @Parameter(description = "完工入库ID") @PathVariable Long id) {
        return R.ok(finishedGoodsReceiptService.getDetail(id));
    }

    @Operation(summary = "新增完工入库")
    @PostMapping
    @PreAuthorize("hasAuthority('material:receipt:create')")
    public R<Long> create(@Valid @RequestBody FinishedGoodsReceiptDTO dto) {
        return R.ok("新增成功", finishedGoodsReceiptService.create(dto));
    }

    @Operation(summary = "修改完工入库")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('material:receipt:update')")
    public R<Void> update(
            @Parameter(description = "完工入库ID") @PathVariable Long id,
            @Valid @RequestBody FinishedGoodsReceiptDTO dto) {
        finishedGoodsReceiptService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除完工入库")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('material:receipt:delete')")
    public R<Void> delete(
            @Parameter(description = "完工入库ID") @PathVariable Long id) {
        finishedGoodsReceiptService.delete(id);
        return R.ok();
    }

    @Operation(summary = "分页查询入库申请")
    @GetMapping("/request/page")
    @PreAuthorize("hasAuthority('material:receiptRequest:list')")
    public R<PageResult<ReceiptRequestVO>> pageRequest(ReceiptRequestQuery query) {
        return R.ok(receiptRequestService.page(query));
    }

    @Operation(summary = "获取入库申请详情")
    @GetMapping("/request/{id}")
    @PreAuthorize("hasAuthority('material:receiptRequest:detail')")
    public R<ReceiptRequestVO> getRequestDetail(
            @Parameter(description = "入库申请ID") @PathVariable Long id) {
        return R.ok(receiptRequestService.getDetail(id));
    }

    @Operation(summary = "新增入库申请")
    @PostMapping("/request")
    @PreAuthorize("hasAuthority('material:receiptRequest:create')")
    public R<Long> createRequest(@Valid @RequestBody ReceiptRequestDTO dto) {
        return R.ok("新增成功", receiptRequestService.create(dto));
    }

    @Operation(summary = "修改入库申请")
    @PutMapping("/request/{id}")
    @PreAuthorize("hasAuthority('material:receiptRequest:update')")
    public R<Void> updateRequest(
            @Parameter(description = "入库申请ID") @PathVariable Long id,
            @Valid @RequestBody ReceiptRequestDTO dto) {
        receiptRequestService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除入库申请")
    @DeleteMapping("/request/{id}")
    @PreAuthorize("hasAuthority('material:receiptRequest:delete')")
    public R<Void> deleteRequest(
            @Parameter(description = "入库申请ID") @PathVariable Long id) {
        receiptRequestService.delete(id);
        return R.ok();
    }
}
