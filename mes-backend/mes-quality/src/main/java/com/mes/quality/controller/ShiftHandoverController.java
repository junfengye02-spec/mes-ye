package com.mes.quality.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.quality.domain.dto.ShiftHandoverDTO;
import com.mes.quality.domain.query.ShiftHandoverQuery;
import com.mes.quality.domain.vo.ShiftHandoverVO;
import com.mes.quality.service.IShiftHandoverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "交班记录", description = "交班记录管理接口")
@RestController
@RequestMapping("/quality/shift-handover")
@RequiredArgsConstructor
public class ShiftHandoverController {

    private final IShiftHandoverService shiftHandoverService;

    @Operation(summary = "分页查询交班记录")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('quality:shiftHandover:list')")
    public R<PageResult<ShiftHandoverVO>> page(ShiftHandoverQuery query) {
        return R.ok(shiftHandoverService.page(query));
    }

    @Operation(summary = "获取交班记录详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('quality:shiftHandover:detail')")
    public R<ShiftHandoverVO> getDetail(@Parameter(description = "ID") @PathVariable Long id) {
        return R.ok(shiftHandoverService.getDetail(id));
    }

    @Operation(summary = "新增交班记录")
    @PostMapping
    @PreAuthorize("hasAuthority('quality:shiftHandover:create')")
    public R<Long> create(@Valid @RequestBody ShiftHandoverDTO dto) {
        return R.ok("新增成功", shiftHandoverService.create(dto));
    }

    @Operation(summary = "修改交班记录")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('quality:shiftHandover:update')")
    public R<Void> update(@Parameter(description = "ID") @PathVariable Long id,
                          @Valid @RequestBody ShiftHandoverDTO dto) {
        shiftHandoverService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "接收交班")
    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('quality:shiftHandover:receive')")
    public R<Void> receive(@Parameter(description = "ID") @PathVariable Long id) {
        shiftHandoverService.receive(id);
        return R.ok();
    }
}
