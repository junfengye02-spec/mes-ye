package com.mes.admin.controller;

import com.mes.admin.domain.dto.ChangeMyPasswordDTO;
import com.mes.admin.domain.dto.SysUserDTO;
import com.mes.admin.domain.query.SysUserQuery;
import com.mes.admin.domain.vo.SysUserVO;
import com.mes.admin.service.ISysUserService;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.R;
import com.mes.framework.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理", description = "系统用户 CRUD")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final ISysUserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<PageResult<SysUserVO>> page(SysUserQuery query) {
        return R.ok(userService.page(query));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:detail')")
    public R<SysUserVO> getDetail(@Parameter(description = "用户ID") @PathVariable Long id) {
        return R.ok(userService.getDetail(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:create')")
    public R<Long> create(@Valid @RequestBody SysUserDTO dto) {
        return R.ok("新增成功", userService.create(dto));
    }

    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:update')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysUserDTO dto) {
        userService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    @Operation(summary = "重置密码",
            description = "P0-07 安全整改：请求体中可传 newPassword；若不传则服务端生成 12 位随机强密码。" +
                    "返回值 data 为实际生效的密码明文，管理员需一次性告知用户（仅此一次返回，不入库不日志明文）。")
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user:resetPwd')")
    public R<String> resetPassword(@PathVariable Long id,
                                   @RequestBody(required = false) ResetPasswordRequest request) {
        String newPwd = request == null ? null : request.getNewPassword();
        String effective = userService.resetPasswordAndReturn(id, newPwd);
        return R.ok("重置成功，请一次性告知用户本次生效的密码", effective);
    }

    @Operation(summary = "当前用户自助修改密码",
            description = "P0-06 安全整改：弱口令审计命中后的强制改密入口。" +
                    "请求体需同时提供 oldPassword 与 newPassword；" +
                    "校验通过后服务端会自动清除 must_change_password 标记。" +
                    "权限：任何已登录用户都可改自己的密码，不需要额外菜单权限。")
    @PostMapping("/change-my-password")
    @PreAuthorize("isAuthenticated()")
    public R<Void> changeMyPassword(@Valid @RequestBody ChangeMyPasswordDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("未识别到当前登录用户");
        }
        userService.changeMyPassword(currentUserId, dto.getOldPassword(), dto.getNewPassword());
        return R.ok("密码修改成功，请使用新密码重新登录", null);
    }

    /**
     * 重置密码请求体。
     */
    @lombok.Data
    public static class ResetPasswordRequest {
        /**
         * 管理员指定的新密码；为空则服务端生成随机强密码。
         */
        private String newPassword;
    }
}
