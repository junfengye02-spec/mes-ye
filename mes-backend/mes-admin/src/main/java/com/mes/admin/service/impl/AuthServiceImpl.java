package com.mes.admin.service.impl;

import com.mes.admin.domain.dto.LoginDTO;
import com.mes.admin.domain.entity.SysRole;
import com.mes.admin.domain.vo.LoginVO;
import com.mes.admin.domain.vo.UserInfoVO;
import com.mes.admin.mapper.SysRoleMapper;
import com.mes.admin.mapper.SysUserMapper;
import com.mes.admin.service.IAuthService;
import com.mes.common.exception.BusinessException;
import com.mes.framework.security.JwtTokenProvider;
import com.mes.framework.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final String REDIS_PERMISSION_KEY = "auth:permissions:";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public LoginVO login(LoginDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        Long userId = loginUser.getUserId();
        String username = loginUser.getUsername();

        String accessToken = tokenProvider.createAccessToken(userId, username);
        String refreshToken = tokenProvider.createRefreshToken(userId, username);

        cachePermissions(userId);

        UserInfoVO userInfo = buildUserInfo(userId, username, loginUser.getRealName());
        return new LoginVO(accessToken, refreshToken, userInfo);
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("refreshToken 无效或已过期");
        }
        if (!"refresh".equals(tokenProvider.getTokenType(refreshToken))) {
            throw new BusinessException("非法的 token 类型");
        }
        Long userId = tokenProvider.getUserId(refreshToken);
        String username = tokenProvider.getUsername(refreshToken);

        String newAccessToken = tokenProvider.createAccessToken(userId, username);
        String newRefreshToken = tokenProvider.createRefreshToken(userId, username);

        cachePermissions(userId);

        return new LoginVO(newAccessToken, newRefreshToken, null);
    }

    @Override
    public void logout(Long userId) {
        try {
            redisTemplate.delete(REDIS_PERMISSION_KEY + userId);
        } catch (Exception e) {
            log.warn("Redis 删除权限缓存失败: {}", e.getMessage());
        }
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        var user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return buildUserInfo(userId, user.getUsername(), user.getRealName());
    }

    private UserInfoVO buildUserInfo(Long userId, String username, String realName) {
        List<SysRole> roles = roleMapper.selectRolesByUserId(userId);
        List<String> permissions = userMapper.selectPermissionsByUserId(userId);

        UserInfoVO info = new UserInfoVO();
        info.setId(userId);
        info.setUsername(username);
        info.setRealName(realName);
        info.setRoles(roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList()));
        info.setPermissions(new HashSet<>(permissions));
        return info;
    }

    private void cachePermissions(Long userId) {
        try {
            String key = REDIS_PERMISSION_KEY + userId;
            redisTemplate.delete(key);
            Set<String> perms = new HashSet<>(userMapper.selectPermissionsByUserId(userId));
            if (!perms.isEmpty()) {
                redisTemplate.opsForSet().add(key, perms.toArray(new String[0]));
                redisTemplate.expire(key, 2, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.warn("Redis 缓存权限失败，降级运行: {}", e.getMessage());
        }
    }
}
