package com.mes.aps.controller;

import com.mes.aps.domain.dto.ApsSyncConfigDTO;
import com.mes.aps.domain.query.ApsSyncConfigQuery;
import com.mes.aps.domain.vo.ApsSyncConfigVO;
import com.mes.aps.service.IApsSyncConfigService;
import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * APS 同步配置管理 Controller
 */
@Tag(name = "APS同步配置", description = "APS同步配置管理接口")
@RestController
@RequestMapping("/aps/config")
@RequiredArgsConstructor
public class ApsSyncConfigController {

    private final IApsSyncConfigService apsSyncConfigService;

    @Operation(summary = "分页查询同步配置")
    @GetMapping("/page")
    public R<PageResult<ApsSyncConfigVO>> page(ApsSyncConfigQuery query) {
        return R.ok(apsSyncConfigService.page(query));
    }

    @Operation(summary = "获取全部同步配置")
    @GetMapping("/list")
    public R<List<ApsSyncConfigVO>> listAll() {
        return R.ok(apsSyncConfigService.listAll());
    }

    @Operation(summary = "获取配置详情")
    @GetMapping("/{id}")
    public R<ApsSyncConfigVO> getDetail(
            @Parameter(description = "配置ID") @PathVariable Long id) {
        return R.ok(apsSyncConfigService.getDetail(id));
    }

    @Operation(summary = "新增配置")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ApsSyncConfigDTO dto) {
        return R.ok("新增成功", apsSyncConfigService.create(dto));
    }

    @Operation(summary = "修改配置")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "配置ID") @PathVariable Long id,
            @Valid @RequestBody ApsSyncConfigDTO dto) {
        apsSyncConfigService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除配置")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "配置ID") @PathVariable Long id) {
        apsSyncConfigService.delete(id);
        return R.ok();
    }
}
