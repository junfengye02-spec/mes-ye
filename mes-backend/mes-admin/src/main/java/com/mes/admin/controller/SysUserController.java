package com.mes.admin.controller;

import com.mes.admin.domain.dto.SysUserDTO;
import com.mes.admin.domain.query.SysUserQuery;
import com.mes.admin.domain.vo.SysUserVO;
import com.mes.admin.service.ISysUserService;
import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理", description = "系统用户 CRUD")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final ISysUserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public R<PageResult<SysUserVO>> page(SysUserQuery query) {
        return R.ok(userService.page(query));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public R<SysUserVO> getDetail(@Parameter(description = "用户ID") @PathVariable Long id) {
        return R.ok(userService.getDetail(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public R<Long> create(@Valid @RequestBody SysUserDTO dto) {
        return R.ok("新增成功", userService.create(dto));
    }

    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysUserDTO dto) {
        userService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id, "123456");
        return R.ok();
    }
}
