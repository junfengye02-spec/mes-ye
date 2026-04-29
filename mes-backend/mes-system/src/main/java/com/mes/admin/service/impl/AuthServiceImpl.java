package com.mes.admin.service.impl;

import com.mes.admin.domain.dto.LoginDTO;
import com.mes.admin.domain.entity.SysRole;
import com.mes.admin.domain.entity.SysTenant;
import com.mes.admin.domain.entity.SysUser;
import com.mes.admin.domain.vo.CaptchaVO;
import com.mes.admin.domain.vo.LoginVO;
import com.mes.admin.domain.vo.UserInfoVO;
import com.mes.admin.mapper.SysRoleMapper;
import com.mes.admin.mapper.SysTenantMapper;
import com.mes.admin.mapper.SysUserMapper;
import com.mes.admin.service.CaptchaService;
import com.mes.admin.service.IAuthService;
import com.mes.admin.service.LoginLockoutService;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.framework.cache.CacheKeys;
import com.mes.framework.security.JwtAuthenticationFilter;
import com.mes.framework.security.JwtBlacklistService;
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
import org.springframework.security.core.AuthenticationException;
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

    /** user-revoke 标记的 TTL 建议 ≥ refresh 最大有效期，取 14 天作为安全冗余 */
    private static final int USER_REVOKE_TTL_DAYS = 14;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysTenantMapper tenantMapper;
    private final StringRedisTemplate redisTemplate;
    private final LoginLockoutService lockoutService;
    private final CaptchaService captchaService;
    private final JwtBlacklistService jwtBlacklistService;

    // ==================== 登录（含锁定 + 验证码） ====================

    @Override
    public LoginVO login(LoginDTO dto) {
        String tenantCode = dto.getTenantCode();
        String username = dto.getUsername();

        // 1. 锁定检查
        if (lockoutService.isLocked(tenantCode, username)) {
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED);
        }

        // 2. 验证码检查（失败 3 次后要求）
        if (lockoutService.isCaptchaRequired(tenantCode, username)) {
            if (!StringUtils.hasText(dto.getCaptchaKey())
                    || !StringUtils.hasText(dto.getCaptchaCode())) {
                throw new BusinessException(ResultCode.CAPTCHA_REQUIRED);
            }
            if (!captchaService.verify(dto.getCaptchaKey(), dto.getCaptchaCode())) {
                throw new BusinessException(ResultCode.CAPTCHA_INVALID);
            }
        }

        // 3. 认证
        LoginTenantContext.setTenantCode(tenantCode);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, dto.getPassword()));
        } catch (AuthenticationException ae) {
            long count = lockoutService.recordFailure(tenantCode, username);
            if (count >= LoginLockoutService.THRESHOLD_LOCK) {
                throw new BusinessException(ResultCode.ACCOUNT_LOCKED);
            }
            // 保留原始认证异常消息，便于前端区分密码错 / 用户不存在
            throw new BusinessException(ae.getMessage() != null ? ae.getMessage() : "用户名或密码错误");
        } finally {
            LoginTenantContext.clear();
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
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
        Long tenantId = loginUser.getTenantId();
        String loginUsername = loginUser.getUsername();
        String resolvedTenantCode = resolveTenantCode(tenantId);

        String accessToken = tokenProvider.createAccessToken(
                userId, loginUsername, tenantId, resolvedTenantCode, loginUser.getAccountType());
        String refreshToken = tokenProvider.createRefreshToken(
                userId, loginUsername, tenantId, resolvedTenantCode, loginUser.getAccountType());

        cachePermissions(tenantId, userId);

        // 登录成功清空失败计数与验证码标记
        lockoutService.recordSuccess(tenantCode, username);

        UserInfoVO userInfo = buildUserInfo(tenantId, userId, loginUsername, loginUser.getRealName());
        // P0-06：登录时回带"是否必须改密"标识，前端需据此阻断业务入口并弹出改密框
        LoginVO vo = new LoginVO(accessToken, refreshToken, userInfo);
        vo.setMustChangePwd(userInfo.getMustChangePwd());
        if (Boolean.TRUE.equals(userInfo.getMustChangePwd())) {
            log.warn("[P0-06] 用户登录时命中弱口令审计 userId={} username={}，已标记必须改密",
                    userId, loginUsername);
        }
        return vo;
    }

    // ==================== Refresh 一次性轮换 ====================

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("refreshToken 无效或已过期");
        }
        if (!"refresh".equals(tokenProvider.getTokenType(refreshToken))) {
            throw new BusinessException("非法的 token 类型");
        }

        String jti = tokenProvider.getJti(refreshToken);
        Long userId = tokenProvider.getUserId(refreshToken);
        Long tenantId = tokenProvider.getTenantId(refreshToken);

        // 重放检测：若该 refresh jti 已被使用过，视为令牌被盗
        if (jti != null && jwtBlacklistService.isRefreshUsed(jti)) {
            log.warn("[Auth] 检测到 refresh token 重放: tenantId={}, userId={}, jti={}",
                    tenantId, userId, jti);
            jwtBlacklistService.invalidateAllForUser(tenantId, userId, USER_REVOKE_TTL_DAYS);
            throw new BusinessException(ResultCode.REFRESH_TOKEN_REPLAYED);
        }

        long remainingMs = tokenProvider.getExpiration(refreshToken).getTime() - System.currentTimeMillis();
        if (remainingMs > 0 && jti != null) {
            jwtBlacklistService.markRefreshUsed(jti, remainingMs);
        }

        String username = tokenProvider.getUsername(refreshToken);
        String tenantCode = tokenProvider.getTenantCode(refreshToken);
        if (!StringUtils.hasText(tenantCode)) {
            tenantCode = resolveTenantCode(tenantId);
        }
        String accountType = tokenProvider.getAccountType(refreshToken);

        String newAccessToken = tokenProvider.createAccessToken(userId, username, tenantId, tenantCode, accountType);
        String newRefreshToken = tokenProvider.createRefreshToken(userId, username, tenantId, tenantCode, accountType);

        // 刷新后也刷新权限缓存，避免权限变更后 2 小时才生效的边界
        TenantContextHolder.setTenantId(tenantId);
        cachePermissions(tenantId, userId);

        return new LoginVO(newAccessToken, newRefreshToken, null);
    }

    // ==================== Logout：加黑名单 ====================

    @Override
    public void logout(Long userId, String accessToken) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (StringUtils.hasText(accessToken) && tokenProvider.validateToken(accessToken)
                && "access".equals(tokenProvider.getTokenType(accessToken))) {
            String jti = tokenProvider.getJti(accessToken);
            long remainingMs = tokenProvider.getExpiration(accessToken).getTime() - System.currentTimeMillis();
            if (remainingMs > 0 && jti != null) {
                jwtBlacklistService.addToBlacklist(jti, remainingMs);
            }
        }
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

    // ==================== 验证码生成 ====================

    @Override
    public CaptchaVO generateCaptcha() {
        return captchaService.generate();
    }

    // ==================== UserInfo ====================

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
            // P0-06：must_change_password=1 的账号在 /auth/login 和 /auth/user-info 都必须返回 true
            info.setMustChangePwd(row.getMustChangePassword() != null && row.getMustChangePassword() == 1);
        } else {
            info.setMustChangePwd(Boolean.FALSE);
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
