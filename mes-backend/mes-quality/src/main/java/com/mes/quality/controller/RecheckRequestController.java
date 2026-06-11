package com.mes.quality.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.quality.domain.dto.RecheckApproveDTO;
import com.mes.quality.domain.dto.RecheckRequestDTO;
import com.mes.quality.domain.dto.RecheckReviewDTO;
import com.mes.quality.domain.query.RecheckRequestQuery;
import com.mes.quality.domain.vo.RecheckRequestVO;
import com.mes.quality.service.IRecheckRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "复检申请", description = "成品复检申请管理接口")
@RestController
@RequestMapping("/quality/recheck")
@RequiredArgsConstructor
public class RecheckRequestController {

    private final IRecheckRequestService recheckRequestService;

    @Operation(summary = "分页查询复检申请")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('quality:recheck:list')")
    public R<PageResult<RecheckRequestVO>> page(RecheckRequestQuery query) {
        return R.ok(recheckRequestService.page(query));
    }

    @Operation(summary = "获取复检申请详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('quality:recheck:detail')")
    public R<RecheckRequestVO> getDetail(@Parameter(description = "ID") @PathVariable Long id) {
        return R.ok(recheckRequestService.getDetail(id));
    }

    @Operation(summary = "新增复检申请")
    @PostMapping
    @PreAuthorize("hasAuthority('quality:recheck:create')")
    public R<Long> create(@Valid @RequestBody RecheckRequestDTO dto) {
        return R.ok("新增成功", recheckRequestService.create(dto));
    }

    @Operation(summary = "修改复检申请")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('quality:recheck:update')")
    public R<Void> update(@Parameter(description = "ID") @PathVariable Long id,
                          @Valid @RequestBody RecheckRequestDTO dto) {
        recheckRequestService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "提交复检申请")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('quality:recheck:submit')")
    public R<Void> submit(@Parameter(description = "ID") @PathVariable Long id) {
        recheckRequestService.submit(id);
        return R.ok();
    }

    @Operation(summary = "审核复检申请")
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('quality:recheck:review')")
    public R<Void> review(@Parameter(description = "ID") @PathVariable Long id,
                          @RequestBody RecheckReviewDTO dto) {
        recheckRequestService.review(id, dto);
        return R.ok();
    }

    @Operation(summary = "审批复检申请")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('quality:recheck:approve')")
    public R<Void> approve(@Parameter(description = "ID") @PathVariable Long id,
                           @Valid @RequestBody RecheckApproveDTO dto) {
        recheckRequestService.approve(id, dto);
        return R.ok();
    }

    @Operation(summary = "完结复检申请")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('quality:recheck:complete')")
    public R<Void> complete(@Parameter(description = "ID") @PathVariable Long id) {
        recheckRequestService.complete(id);
        return R.ok();
    }

    @Operation(summary = "删除复检申请")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('quality:recheck:delete')")
    public R<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        recheckRequestService.delete(id);
        return R.ok();
    }
}
