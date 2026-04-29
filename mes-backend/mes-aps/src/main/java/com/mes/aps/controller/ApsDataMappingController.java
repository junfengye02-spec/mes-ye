package com.mes.aps.controller;

import com.mes.aps.domain.dto.ApsDataMappingDTO;
import com.mes.aps.domain.query.ApsDataMappingQuery;
import com.mes.aps.domain.vo.ApsDataMappingVO;
import com.mes.aps.service.IApsDataMappingService;
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
 * APS 数据映射管理 Controller
 */
@Tag(name = "APS数据映射", description = "APS数据映射管理接口")
@RestController
@RequestMapping("/aps/mapping")
@RequiredArgsConstructor
public class ApsDataMappingController {

    private final IApsDataMappingService apsDataMappingService;

    @Operation(summary = "分页查询数据映射")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('aps:dataMapping:list')")
    public R<PageResult<ApsDataMappingVO>> page(ApsDataMappingQuery query) {
        return R.ok(apsDataMappingService.page(query));
    }

    @Operation(summary = "获取映射详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('aps:dataMapping:detail')")
    public R<ApsDataMappingVO> getDetail(
            @Parameter(description = "映射ID") @PathVariable Long id) {
        return R.ok(apsDataMappingService.getDetail(id));
    }

    @Operation(summary = "新增数据映射")
    @PostMapping
    @PreAuthorize("hasAuthority('aps:dataMapping:create')")
    public R<Long> create(@Valid @RequestBody ApsDataMappingDTO dto) {
        return R.ok("新增成功", apsDataMappingService.create(dto));
    }

    @Operation(summary = "修改数据映射")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('aps:dataMapping:update')")
    public R<Void> update(
            @Parameter(description = "映射ID") @PathVariable Long id,
            @Valid @RequestBody ApsDataMappingDTO dto) {
        apsDataMappingService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除数据映射")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('aps:dataMapping:delete')")
    public R<Void> delete(
            @Parameter(description = "映射ID") @PathVariable Long id) {
        apsDataMappingService.delete(id);
        return R.ok();
    }
}
