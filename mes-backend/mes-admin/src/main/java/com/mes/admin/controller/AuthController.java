package com.mes.admin.controller;

import com.mes.admin.domain.dto.LoginDTO;
import com.mes.admin.domain.dto.RefreshTokenDTO;
import com.mes.admin.domain.vo.LoginVO;
import com.mes.admin.domain.vo.UserInfoVO;
import com.mes.admin.service.IAuthService;
import com.mes.common.result.R;
import com.mes.framework.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "登录/登出/刷新令牌")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return R.ok(authService.refreshToken(dto.getRefreshToken()));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            authService.logout(userId);
        }
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/user-info")
    public R<UserInfoVO> getUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(authService.getUserInfo(userId));
    }
}
