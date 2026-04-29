package com.mes.admin.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.mes.admin.domain.dto.LoginDTO;
import com.mes.admin.domain.dto.RefreshTokenDTO;
import com.mes.admin.domain.vo.CaptchaVO;
import com.mes.admin.domain.vo.LoginVO;
import com.mes.admin.domain.vo.UserInfoVO;
import com.mes.admin.service.IAuthService;
import com.mes.common.result.R;
import com.mes.framework.security.SecurityUtils;
import com.mes.framework.sentinel.MesRateLimit;
import com.mes.framework.sentinel.SentinelBlockHandlers;
import com.mes.framework.sentinel.SentinelResources;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "登录/登出/刷新令牌/图形验证码")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final IAuthService authService;

    @Operation(summary = "登录",
            description = "连续失败 3 次后要求验证码；连续失败 5 次后账号锁定 15 分钟（返回 code=423）；每 IP 10 QPS 限流")
    @PostMapping("/login")
    @SentinelResource(value = SentinelResources.AUTH_LOGIN,
            blockHandler = "handleR", blockHandlerClass = SentinelBlockHandlers.class)
    @MesRateLimit(resource = SentinelResources.AUTH_LOGIN, key = MesRateLimit.Key.IP, count = 10)
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @Operation(summary = "图形验证码（P1-14）", description = "返回 base64 PNG + captchaKey，TTL 5 分钟")
    @GetMapping("/captcha")
    public R<CaptchaVO> captcha() {
        return R.ok(authService.generateCaptcha());
    }

    @Operation(summary = "刷新令牌", description = "一次性轮换：旧 refresh 立即失效；重放则吊销该用户全部会话")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return R.ok(authService.refreshToken(dto.getRefreshToken()));
    }

    @Operation(summary = "登出", description = "将当前 access token 加入黑名单，立即失效")
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public R<Void> logout(HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            authService.logout(userId, extractAccessToken(request));
        }
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/user-info")
    @PreAuthorize("isAuthenticated()")
    public R<UserInfoVO> getUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(authService.getUserInfo(userId));
    }

    /**
     * 从 Authorization 头提取 Bearer token
     */
    private String extractAccessToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
