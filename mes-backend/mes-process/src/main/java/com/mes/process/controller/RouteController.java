package com.mes.process.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.process.domain.dto.RouteDTO;
import com.mes.process.domain.query.RouteQuery;
import com.mes.process.domain.vo.RouteVO;
import com.mes.process.service.IRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 工艺路线 Controller。
 */
@Tag(name = "工艺路线", description = "工艺路线管理接口")
@RestController
@RequestMapping("/process/route")
@RequiredArgsConstructor
public class RouteController {

    private final IRouteService routeService;

    @Operation(summary = "分页查询工艺路线")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('process:route:list')")
    public R<PageResult<RouteVO>> page(RouteQuery query) {
        return R.ok(routeService.page(query));
    }

    @Operation(summary = "获取工艺路线详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('process:route:detail')")
    public R<RouteVO> getDetail(
            @Parameter(description = "工艺路线ID") @PathVariable Long id) {
        return R.ok(routeService.getDetail(id));
    }

    @Operation(summary = "新增工艺路线")
    @PostMapping
    @PreAuthorize("hasAuthority('process:route:create')")
    public R<Long> create(@Valid @RequestBody RouteDTO dto) {
        return R.ok("新增成功", routeService.create(dto));
    }

    @Operation(summary = "修改工艺路线")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('process:route:update')")
    public R<Void> update(
            @Parameter(description = "工艺路线ID") @PathVariable Long id,
            @Valid @RequestBody RouteDTO dto) {
        routeService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除工艺路线")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('process:route:delete')")
    public R<Void> delete(
            @Parameter(description = "工艺路线ID") @PathVariable Long id) {
        routeService.delete(id);
        return R.ok();
    }

    @Operation(summary = "启用工艺路线")
    @PostMapping("/activate/{id}")
    @PreAuthorize("hasAuthority('process:route:update')")
    public R<Void> activate(
            @Parameter(description = "工艺路线ID") @PathVariable Long id) {
        routeService.activate(id);
        return R.ok();
    }

    @Operation(summary = "停用工艺路线")
    @PostMapping("/disable/{id}")
    @PreAuthorize("hasAuthority('process:route:update')")
    public R<Void> disable(
            @Parameter(description = "工艺路线ID") @PathVariable Long id) {
        routeService.disable(id);
        return R.ok();
    }
}
