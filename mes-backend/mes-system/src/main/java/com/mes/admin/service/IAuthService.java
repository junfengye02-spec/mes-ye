package com.mes.admin.service;

import com.mes.admin.domain.dto.LoginDTO;
import com.mes.admin.domain.vo.LoginVO;
import com.mes.admin.domain.vo.UserInfoVO;

public interface IAuthService {
    LoginVO login(LoginDTO dto);
    LoginVO refreshToken(String refreshToken);
    void logout(Long userId);
    UserInfoVO getUserInfo(Long userId);
}
