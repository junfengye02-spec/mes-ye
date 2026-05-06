package com.mes.admin.service;

import com.mes.admin.domain.dto.LoginDTO;
import com.mes.admin.domain.entity.SysRole;
import com.mes.admin.domain.entity.SysTenant;
import com.mes.admin.domain.entity.SysUser;
import com.mes.admin.domain.vo.LoginVO;
import com.mes.admin.domain.vo.UserInfoVO;
import com.mes.admin.mapper.SysRoleMapper;
import com.mes.admin.mapper.SysTenantMapper;
import com.mes.admin.mapper.SysUserMapper;
import com.mes.admin.service.CaptchaService;
import com.mes.admin.service.LoginLockoutService;
import com.mes.admin.service.impl.AuthServiceImpl;
import com.mes.common.exception.BusinessException;
import com.mes.framework.security.JwtBlacklistService;
import com.mes.framework.security.JwtTokenProvider;
import com.mes.framework.security.LoginUser;
import com.mes.framework.security.StaffPortalRestrictionFilter;
import com.mes.framework.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link AuthServiceImpl} 单元测试：登录、刷新、登出、用户信息、权限缓存
 */
@ExtendWith(MockitoExtension.class)
// P2 后 AuthServiceImpl 新增了若干协作依赖（lockoutService / captchaService / jwtBlacklistService），
// 为避免在各用例分散补 stub 引发 UnnecessaryStubbing 噪音，整体放宽为 LENIENT；
// 各用例依旧通过 verify 精确断言关键行为。
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthServiceTest {

    /** 带租户前缀的 Redis key 样式：tenant:{tid}:auth:permissions:{uid} */
    private static final String PERM_KEY_PREFIX = "tenant:1:auth:permissions:";

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysTenantMapper tenantMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private Authentication authentication;
    // P2 升级后 AuthServiceImpl 新增依赖：登录锁定 / 验证码 / JWT 黑名单
    @Mock
    private LoginLockoutService lockoutService;
    @Mock
    private CaptchaService captchaService;
    @Mock
    private JwtBlacklistService jwtBlacklistService;

    @InjectMocks
    private AuthServiceImpl authService;

    // ==================== 1. 登录 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 正常登录 - ADMIN 客户端显式指定")
    void login_whenAdminClient_shouldReturnTokensAndUserInfo() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("secret");
        dto.setLoginClient("ADMIN");

        LoginUser loginUser = buildLoginUser(1L, "admin", "管理员", 1L, "ADMIN");
        when(authentication.getPrincipal()).thenReturn(loginUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.createAccessToken(eq(1L), eq("admin"), eq(1L), anyString(), eq("ADMIN"))).thenReturn("access-1");
        when(tokenProvider.createRefreshToken(eq(1L), eq("admin"), eq(1L), anyString(), eq("ADMIN"))).thenReturn("refresh-1");
        stubPermissionCacheRedis();
        stubUserInfoRows(1L, "admin", "管理员");

        LoginVO vo = authService.login(dto);

        assertNotNull(vo);
        assertEquals("access-1", vo.getAccessToken());
        assertEquals("refresh-1", vo.getRefreshToken());
        assertNotNull(vo.getUserInfo());
        assertEquals("admin", vo.getUserInfo().getUsername());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @Order(2)
    @DisplayName("1.2 STAFF 账号不能从管理端登录")
    void login_whenStaffUsesAdminClient_shouldThrow() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("worker");
        dto.setPassword("secret");
        dto.setLoginClient("ADMIN");

        LoginUser loginUser = buildLoginUser(2L, "worker", "现场", 1L, StaffPortalRestrictionFilter.ACCOUNT_STAFF);
        when(authentication.getPrincipal()).thenReturn(loginUser);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(dto));
        assertTrue(ex.getMessage().contains("现场端"));
    }

    @Test
    @Order(3)
    @DisplayName("1.3 loginClient 无效")
    void login_whenLoginClientInvalid_shouldThrow() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("secret");
        dto.setLoginClient("PORTAL_X");

        LoginUser loginUser = buildLoginUser(1L, "admin", "管理员", 1L, "ADMIN");
        when(authentication.getPrincipal()).thenReturn(loginUser);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(dto));
        assertTrue(ex.getMessage().contains("loginClient"));
    }

    // ==================== 2. 刷新 Token ====================

    @Test
    @Order(10)
    @DisplayName("2.1 refreshToken 正常刷新")
    void refreshToken_whenValidRefresh_shouldReturnNewPair() {
        String old = "old-refresh";
        when(tokenProvider.validateToken(old)).thenReturn(true);
        when(tokenProvider.getTokenType(old)).thenReturn("refresh");
        when(tokenProvider.getUserId(old)).thenReturn(10L);
        when(tokenProvider.getUsername(old)).thenReturn("u10");
        when(tokenProvider.getTenantId(old)).thenReturn(2L);
        when(tokenProvider.getAccountType(old)).thenReturn("ADMIN");
        when(tokenProvider.getTenantCode(old)).thenReturn("east");
        when(tokenProvider.getJti(old)).thenReturn("jti-old");
        // refresh token 过期时间：当前时间 + 1 小时
        when(tokenProvider.getExpiration(old)).thenReturn(new java.util.Date(System.currentTimeMillis() + 3600_000L));
        when(tokenProvider.createAccessToken(eq(10L), eq("u10"), eq(2L), anyString(), eq("ADMIN"))).thenReturn("new-access");
        when(tokenProvider.createRefreshToken(eq(10L), eq("u10"), eq(2L), anyString(), eq("ADMIN"))).thenReturn("new-refresh");
        stubPermissionCacheRedis();

        LoginVO vo = authService.refreshToken(old);

        assertEquals("new-access", vo.getAccessToken());
        assertEquals("new-refresh", vo.getRefreshToken());
        assertNull(vo.getUserInfo());
    }

    @Test
    @Order(11)
    @DisplayName("2.2 refreshToken 无效或已过期")
    void refreshToken_whenInvalidOrExpired_shouldThrow() {
        when(tokenProvider.validateToken("bad")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.refreshToken("bad"));
        assertEquals("refreshToken 无效或已过期", ex.getMessage());
    }

    @Test
    @Order(12)
    @DisplayName("2.3 非法 token 类型（非 refresh）")
    void refreshToken_whenTokenTypeNotRefresh_shouldThrow() {
        when(tokenProvider.validateToken("access-like")).thenReturn(true);
        when(tokenProvider.getTokenType("access-like")).thenReturn("access");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.refreshToken("access-like"));
        assertTrue(ex.getMessage().contains("类型") || ex.getMessage().contains("token"));
    }

    // ==================== 3. 登出 ====================

    @Test
    @Order(20)
    @DisplayName("3.1 登出时清除 Redis 权限缓存（带租户前缀）")
    void logout_shouldDeletePermissionKeyInRedis() {
        TenantContextHolder.setTenantId(1L);
        when(redisTemplate.delete(PERM_KEY_PREFIX + 5L)).thenReturn(true);

        // logout 签名升级为 (userId, accessToken)；测试场景不关心 token，传 null 即可走到权限清理分支
        authService.logout(5L, null);

        verify(redisTemplate).delete(PERM_KEY_PREFIX + 5L);
    }

    @Test
    @Order(21)
    @DisplayName("3.2 登出时若 TenantContext 为空则跳过缓存清理，不抛异常")
    void logout_whenTenantMissing_shouldSkipWithoutThrowing() {
        // 不设置 TenantContextHolder，模拟未经 Filter 的场景
        // logout 签名升级为 (userId, accessToken)；此用例验证 tenant 缺失分支
        authService.logout(5L, null);
        verify(redisTemplate, never()).delete(anyString());
    }

    // ==================== 4. 用户信息 ====================

    @Test
    @Order(30)
    @DisplayName("4.1 获取用户信息 - 用户存在")
    void getUserInfo_whenUserExists_shouldReturnVo() {
        stubUserInfoRows(7L, "bob", "鲍勃");

        UserInfoVO info = authService.getUserInfo(7L);

        assertNotNull(info);
        assertEquals(7L, info.getId());
        assertEquals("bob", info.getUsername());
    }

    @Test
    @Order(31)
    @DisplayName("4.2 获取用户信息 - 用户不存在")
    void getUserInfo_whenUserMissing_shouldThrow() {
        when(userMapper.selectById(404L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> authService.getUserInfo(404L));
    }

    // ==================== 5. 权限写入 Redis ====================

    @Test
    @Order(40)
    @DisplayName("5.1 登录成功后权限列表缓存到 Redis")
    void login_shouldWritePermissionsToRedisSet() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("secret");
        dto.setLoginClient("ADMIN");

        LoginUser loginUser = buildLoginUser(3L, "admin", "管理员", 1L, "ADMIN");
        loginUser.setPermissions(Set.of("perm:a", "perm:b"));
        when(authentication.getPrincipal()).thenReturn(loginUser);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.createAccessToken(anyLong(), anyString(), anyLong(), any(), anyString())).thenReturn("a");
        when(tokenProvider.createRefreshToken(anyLong(), anyString(), anyLong(), any(), anyString())).thenReturn("r");
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add(anyString(), any(String[].class))).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        stubUserInfoRows(3L, "admin", "管理员");

        authService.login(dto);

        verify(redisTemplate).delete(PERM_KEY_PREFIX + 3L);
        ArgumentCaptor<String[]> permCaptor = ArgumentCaptor.forClass(String[].class);
        verify(setOperations).add(eq(PERM_KEY_PREFIX + 3L), permCaptor.capture());
        assertEquals(Set.of("perm:a", "perm:b"), Set.of(permCaptor.getValue()));
        verify(redisTemplate).expire(eq(PERM_KEY_PREFIX + 3L), eq(2L), eq(TimeUnit.HOURS));
        verify(userMapper, never()).selectPermissionsByUserIdScoped(eq(3L), anyLong());
    }

    // ==================== 辅助 ====================

    private LoginUser buildLoginUser(Long userId, String username, String realName, Long tenantId, String accountType) {
        LoginUser u = new LoginUser();
        u.setUserId(userId);
        u.setUsername(username);
        u.setRealName(realName);
        u.setTenantId(tenantId);
        u.setAccountType(accountType);
        u.setPermissions(Set.of("system:user:list"));
        u.setMustChangePwd(Boolean.FALSE);
        return u;
    }

    private void stubPermissionCacheRedis() {
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add(anyString(), any(String[].class))).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(userMapper.selectPermissionsByUserIdScoped(anyLong(), anyLong())).thenReturn(List.of("p1"));
    }

    private void stubUserInfoRows(Long userId, String username, String realName) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername(username);
        user.setRealName(realName);
        user.setTenantId(1L);
        user.setAccountType("ADMIN");
        when(userMapper.selectById(userId)).thenReturn(user);

        SysRole role = new SysRole();
        role.setRoleCode("admin");
        when(roleMapper.selectRolesByUserIdScoped(eq(userId), anyLong())).thenReturn(List.of(role));
        lenient().when(userMapper.selectPermissionsByUserIdScoped(eq(userId), anyLong()))
                .thenReturn(List.of("system:user:list"));

        SysTenant tenant = new SysTenant();
        tenant.setTenantCode("DEFAULT");
        when(tenantMapper.selectById(1L)).thenReturn(tenant);
    }
}
