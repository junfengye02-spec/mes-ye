package com.mes.admin.controller;

import com.mes.admin.domain.dto.SysMenuDTO;
import com.mes.admin.domain.vo.SysMenuVO;
import com.mes.admin.service.ISysMenuService;
import com.mes.common.result.R;
import com.mes.framework.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "菜单管理", description = "系统菜单 CRUD + 菜单树")
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final ISysMenuService menuService;

    @Operation(summary = "完整菜单树（管理用）")
    @GetMapping("/tree")
    public R<List<SysMenuVO>> getTree() {
        return R.ok(menuService.getTree());
    }

    @Operation(summary = "当前用户菜单树（导航用）")
    @GetMapping("/user-tree")
    public R<List<SysMenuVO>> getUserTree() {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(menuService.getUserTree(userId));
    }

    @Operation(summary = "获取菜单详情")
    @GetMapping("/{id}")
    public R<SysMenuVO> getDetail(@PathVariable Long id) {
        return R.ok(menuService.getDetail(id));
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    public R<Long> create(@Valid @RequestBody SysMenuDTO dto) {
        return R.ok("新增成功", menuService.create(dto));
    }

    @Operation(summary = "修改菜单")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysMenuDTO dto) {
        menuService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok();
    }
}
