package com.mes.admin.service.impl;

import com.mes.admin.domain.dto.LoginDTO;
import com.mes.admin.domain.entity.SysRole;
import com.mes.admin.domain.entity.SysTenant;
import com.mes.admin.domain.entity.SysUser;
import com.mes.admin.domain.vo.LoginVO;
import com.mes.admin.domain.vo.UserInfoVO;
import com.mes.admin.mapper.SysRoleMapper;
import com.mes.admin.mapper.SysTenantMapper;
import com.mes.admin.mapper.SysUserMapper;
import com.mes.admin.service.IAuthService;
import com.mes.common.exception.BusinessException;
import com.mes.framework.cache.CacheKeys;
import com.mes.framework.security.JwtAuthenticationFilter;
import com.mes.framework.security.JwtTokenProvider;
import com.mes.framework.security.LoginTenantContext;
import com.mes.framework.security.LoginUser;
import com.mes.framework.security.StaffPortalRestrictionFilter;
import com.mes.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final String LOGIN_CLIENT_ADMIN = "ADMIN";
    private static final String LOGIN_CLIENT_USER = "USER";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysTenantMapper tenantMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public LoginVO login(LoginDTO dto) {
        // 透传 tenantCode 给 UserDetailsServiceImpl，支持 (tenant, username) 定位
        LoginTenantContext.setTenantCode(dto.getTenantCode());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        } finally {
            LoginTenantContext.clear();
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        // UserDetailsServiceImpl 在加载用户时已临时注入 TenantContextHolder，
        // 这里再显式保证（认证异常路径会被上面的 finally 清；这里续写租户上下文，供后续缓存写入使用）
        TenantContextHolder.setTenantId(loginUser.getTenantId());
        String client = !StringUtils.hasText(dto.getLoginClient())
                ? LOGIN_CLIENT_ADMIN
                : dto.getLoginClient().trim().toUpperCase();
        if (!LOGIN_CLIENT_ADMIN.equals(client) && !LOGIN_CLIENT_USER.equals(client)) {
            throw new BusinessException("loginClient 无效，请使用 ADMIN 或 USER");
        }
        if (LOGIN_CLIENT_ADMIN.equals(client)
                && StaffPortalRestrictionFilter.ACCOUNT_STAFF.equals(loginUser.getAccountType())) {
            throw new BusinessException("该账号仅供现场端使用，请从「现场端登录」入口登录");
        }

        Long userId = loginUser.getUserId();
        String username = loginUser.getUsername();
        Long tenantId = loginUser.getTenantId();
        String tenantCode = resolveTenantCode(tenantId);

        String accessToken = tokenProvider.createAccessToken(
                userId, username, tenantId, tenantCode, loginUser.getAccountType());
        String refreshToken = tokenProvider.createRefreshToken(
                userId, username, tenantId, tenantCode, loginUser.getAccountType());

        cachePermissions(tenantId, userId);

        UserInfoVO userInfo = buildUserInfo(tenantId, userId, username, loginUser.getRealName());
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
        Long tenantId = tokenProvider.getTenantId(refreshToken);
        String tenantCode = tokenProvider.getTenantCode(refreshToken);
        if (!StringUtils.hasText(tenantCode)) {
            tenantCode = resolveTenantCode(tenantId);
        }
        String accountType = tokenProvider.getAccountType(refreshToken);

        String newAccessToken = tokenProvider.createAccessToken(userId, username, tenantId, tenantCode, accountType);
        String newRefreshToken = tokenProvider.createRefreshToken(userId, username, tenantId, tenantCode, accountType);

        cachePermissions(tenantId, userId);

        return new LoginVO(newAccessToken, newRefreshToken, null);
    }

    @Override
    public void logout(Long userId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.warn("logout() 调用时未获取到 TenantContext，跳过权限缓存清理 (userId={})", userId);
            return;
        }
        try {
            redisTemplate.delete(buildPermissionKey(tenantId, userId));
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
        Long tenantId = user.getTenantId() != null
                ? user.getTenantId()
                : TenantContextHolder.requireTenantId();
        return buildUserInfo(tenantId, userId, user.getUsername(), user.getRealName());
    }

    private UserInfoVO buildUserInfo(Long tenantId, Long userId, String username, String realName) {
        SysUser row = userMapper.selectById(userId);
        List<SysRole> roles = roleMapper.selectRolesByUserIdScoped(userId, tenantId);
        List<String> permissions = userMapper.selectPermissionsByUserIdScoped(userId, tenantId);

        UserInfoVO info = new UserInfoVO();
        info.setId(userId);
        info.setUsername(username);
        info.setRealName(row != null && StringUtils.hasText(row.getRealName()) ? row.getRealName() : realName);
        if (row != null) {
            info.setPhone(row.getPhone());
            info.setEmail(row.getEmail());
            info.setFactoryCode(row.getFactoryCode());
            info.setTenantId(tenantId);
            info.setAccountType(StringUtils.hasText(row.getAccountType()) ? row.getAccountType() : "ADMIN");
            info.setTenantCode(resolveTenantCode(tenantId));
        }
        info.setRoles(roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList()));
        info.setPermissions(new HashSet<>(permissions));
        return info;
    }

    private void cachePermissions(Long tenantId, Long userId) {
        try {
            String key = buildPermissionKey(tenantId, userId);
            redisTemplate.delete(key);
            Set<String> perms = new HashSet<>(userMapper.selectPermissionsByUserIdScoped(userId, tenantId));
            if (!perms.isEmpty()) {
                redisTemplate.opsForSet().add(key, perms.toArray(new String[0]));
                redisTemplate.expire(key, 2, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.warn("Redis 缓存权限失败，降级运行: {}", e.getMessage());
        }
    }

    private String buildPermissionKey(Long tenantId, Long userId) {
        return CacheKeys.tenant(tenantId, JwtAuthenticationFilter.PERMISSIONS_MODULE, userId);
    }

    /**
     * 通过 tenantId 查出 tenant_code；涉及 sys_tenant 表，已在 MyBatis 租户拦截忽略名单中，
     * 不会被 TenantLineHandler 自动拼 where。
     */
    private String resolveTenantCode(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        return tenant != null ? tenant.getTenantCode() : null;
    }
}
