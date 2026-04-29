package com.mes.admin.service;

import com.mes.admin.domain.dto.LoginDTO;
import com.mes.admin.domain.vo.CaptchaVO;
import com.mes.admin.domain.vo.LoginVO;
import com.mes.admin.domain.vo.UserInfoVO;

public interface IAuthService {

    /**
     * 登录（含登录失败锁定 / 验证码校验 / JWT 签发）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 刷新令牌（含 refresh 一次性轮换防重放）
     */
    LoginVO refreshToken(String refreshToken);

    /**
     * 登出（将当前 access token 加入黑名单）
     *
     * @param userId      当前用户 ID
     * @param accessToken 当前 access token（从 Authorization 头提取）
     */
    void logout(Long userId, String accessToken);

    /**
     * 当前用户信息
     */
    UserInfoVO getUserInfo(Long userId);

    /**
     * 生成图形验证码（P1-14）
     */
    CaptchaVO generateCaptcha();
}
