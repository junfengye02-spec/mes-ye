package com.mes.admin.controller;

import com.mes.admin.domain.dto.SysRoleDTO;
import com.mes.admin.domain.query.SysRoleQuery;
import com.mes.admin.domain.vo.SysRoleVO;
import com.mes.admin.service.ISysRoleService;
import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色管理", description = "系统角色 CRUD")
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final ISysRoleService roleService;

    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    public R<PageResult<SysRoleVO>> page(SysRoleQuery query) {
        return R.ok(roleService.page(query));
    }

    @Operation(summary = "角色列表（下拉选择用）")
    @GetMapping("/list")
    public R<List<SysRoleVO>> listAll() {
        return R.ok(roleService.listAll());
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public R<SysRoleVO> getDetail(@PathVariable Long id) {
        return R.ok(roleService.getDetail(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    public R<Long> create(@Valid @RequestBody SysRoleDTO dto) {
        return R.ok("新增成功", roleService.create(dto));
    }

    @Operation(summary = "修改角色")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysRoleDTO dto) {
        roleService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @Operation(summary = "分配菜单权限")
    @PutMapping("/{id}/menus")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return R.ok();
    }

    @Operation(summary = "获取角色已分配菜单ID")
    @GetMapping("/{id}/menus")
    public R<List<Long>> getRoleMenuIds(@PathVariable Long id) {
        return R.ok(roleService.getRoleMenuIds(id));
    }
}
