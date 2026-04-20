package com.mes.admin;

import com.mes.admin.domain.dto.LoginDTO;
import com.mes.admin.domain.entity.SysMenu;
import com.mes.admin.domain.entity.SysRole;
import com.mes.admin.domain.entity.SysTenant;
import com.mes.admin.domain.entity.SysUser;
import com.mes.admin.domain.vo.LoginVO;
import com.mes.admin.domain.vo.UserInfoVO;
import com.mes.admin.mapper.SysRoleMapper;
import com.mes.admin.mapper.SysTenantMapper;
import com.mes.admin.mapper.SysUserMapper;
import com.mes.admin.mapper.SysMenuMapper;
import com.mes.admin.service.impl.AuthServiceImpl;
import com.mes.admin.service.impl.SysUserServiceImpl;
import com.mes.admin.service.impl.SysRoleServiceImpl;
import com.mes.admin.service.impl.SysMenuServiceImpl;
import com.mes.common.exception.BusinessException;
import com.mes.framework.security.JwtTokenProvider;
import com.mes.framework.security.LoginUser;
import com.mes.framework.tenant.TenantContextHolder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 系统模块单元测试
 * 覆盖认证（登录/登出/刷新）、用户管理、角色管理、菜单管理
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SystemModuleTest {

    // ===== 认证相关 Mock =====
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private SysUserMapper userMapper;
    @Mock private SysRoleMapper roleMapper;
    @Mock private SysTenantMapper tenantMapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private SetOperations<String, String> setOperations;
    @Mock private SysMenuMapper menuMapper;

    @InjectMocks private AuthServiceImpl authService;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    // ==================== 1. 登录认证测试 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 管理员正常登录 - 默认 ADMIN 客户端")
    void testLogin_AdminSuccess() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("123456");

        LoginUser loginUser = buildLoginUser(1L, "admin", "管理员", 1L, "ADMIN");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(loginUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(tokenProvider.createAccessToken(anyLong(), anyString(), anyLong(), any(), anyString()))
                .thenReturn("access-token-123");
        when(tokenProvider.createRefreshToken(anyLong(), anyString(), anyLong(), any(), anyString()))
                .thenReturn("refresh-token-456");
        mockRedisForPermissionCache();
        mockUserInfoQuery(1L, "admin", "管理员");

        LoginVO result = authService.login(dto);

        assertNotNull(result);
        assertEquals("access-token-123", result.getAccessToken());
        assertEquals("refresh-token-456", result.getRefreshToken());
        assertNotNull(result.getUserInfo());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    @Order(2)
    @DisplayName("1.2 STAFF 账号尝试管理端登录 - 应拒绝")
    void testLogin_StaffCannotLoginAdmin() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("worker1");
        dto.setPassword("123456");
        dto.setLoginClient("ADMIN");

        LoginUser loginUser = buildLoginUser(2L, "worker1", "工人1", 1L, "STAFF");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(loginUser);
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        assertThrows(BusinessException.class, () -> authService.login(dto),
                "STAFF 账号不应被允许从管理端登录");
    }

    @Test
    @Order(3)
    @DisplayName("1.3 STAFF 账号现场端登录 - 应成功")
    void testLogin_StaffUserPortalSuccess() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("worker1");
        dto.setPassword("123456");
        dto.setLoginClient("USER");

        LoginUser loginUser = buildLoginUser(2L, "worker1", "工人1", 1L, "STAFF");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(loginUser);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenProvider.createAccessToken(anyLong(), anyString(), anyLong(), any(), anyString()))
                .thenReturn("staff-access-token");
        when(tokenProvider.createRefreshToken(anyLong(), anyString(), anyLong(), any(), anyString()))
                .thenReturn("staff-refresh-token");
        mockRedisForPermissionCache();
        mockUserInfoQuery(2L, "worker1", "工人1");

        LoginVO result = authService.login(dto);

        assertNotNull(result);
        assertEquals("staff-access-token", result.getAccessToken());
    }

    @Test
    @Order(4)
    @DisplayName("1.4 无效 loginClient - 应拒绝")
    void testLogin_InvalidLoginClient() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("123456");
        dto.setLoginClient("INVALID_CLIENT");

        LoginUser loginUser = buildLoginUser(1L, "admin", "管理员", 1L, "ADMIN");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(loginUser);
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        assertThrows(BusinessException.class, () -> authService.login(dto),
                "无效的 loginClient 应被拒绝");
    }

    // ==================== 2. Token 刷新测试 ====================

    @Test
    @Order(10)
    @DisplayName("2.1 有效 refreshToken 刷新 - 应返回新 token")
    void testRefreshToken_Success() {
        String oldRefreshToken = "old-refresh-token";

        when(tokenProvider.validateToken(oldRefreshToken)).thenReturn(true);
        when(tokenProvider.getTokenType(oldRefreshToken)).thenReturn("refresh");
        when(tokenProvider.getUserId(oldRefreshToken)).thenReturn(1L);
        when(tokenProvider.getUsername(oldRefreshToken)).thenReturn("admin");
        when(tokenProvider.getTenantId(oldRefreshToken)).thenReturn(1L);
        when(tokenProvider.getAccountType(oldRefreshToken)).thenReturn("ADMIN");
        when(tokenProvider.getTenantCode(oldRefreshToken)).thenReturn("default");
        when(tokenProvider.createAccessToken(anyLong(), anyString(), anyLong(), any(), anyString()))
                .thenReturn("new-access-token");
        when(tokenProvider.createRefreshToken(anyLong(), anyString(), anyLong(), any(), anyString()))
                .thenReturn("new-refresh-token");
        mockRedisForPermissionCache();

        LoginVO result = authService.refreshToken(oldRefreshToken);

        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
        assertNull(result.getUserInfo());
    }

    @Test
    @Order(11)
    @DisplayName("2.2 无效 refreshToken - 应拒绝")
    void testRefreshToken_InvalidToken() {
        when(tokenProvider.validateToken("invalid-token")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> authService.refreshToken("invalid-token"));
    }

    @Test
    @Order(12)
    @DisplayName("2.3 用 accessToken 冒充刷新 - 应拒绝")
    void testRefreshToken_WrongTokenType() {
        when(tokenProvider.validateToken("access-token")).thenReturn(true);
        when(tokenProvider.getTokenType("access-token")).thenReturn("access");

        assertThrows(BusinessException.class,
                () -> authService.refreshToken("access-token"),
                "非 refresh 类型的 token 不应被接受");
    }

    // ==================== 3. 登出测试 ====================

    @Test
    @Order(20)
    @DisplayName("3.1 正常登出 - 清除租户前缀的 Redis 权限缓存")
    void testLogout_Success() {
        TenantContextHolder.setTenantId(1L);
        when(redisTemplate.delete("tenant:1:auth:permissions:1")).thenReturn(true);

        authService.logout(1L);

        verify(redisTemplate).delete("tenant:1:auth:permissions:1");
    }

    @Test
    @Order(21)
    @DisplayName("3.2 登出时 Redis 不可用 - 不应抛异常（降级处理）")
    void testLogout_RedisUnavailable() {
        TenantContextHolder.setTenantId(1L);
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis 连接失败"));

        assertDoesNotThrow(() -> authService.logout(1L),
                "Redis 不可用时登出不应抛异常");
    }

    // ==================== 4. 获取用户信息测试 ====================

    @Test
    @Order(30)
    @DisplayName("4.1 获取用户信息 - 正常返回")
    void testGetUserInfo_Success() {
        mockUserInfoQuery(1L, "admin", "管理员");

        UserInfoVO info = authService.getUserInfo(1L);

        assertNotNull(info);
        assertEquals(1L, info.getId());
        assertEquals("admin", info.getUsername());
    }

    @Test
    @Order(31)
    @DisplayName("4.2 用户不存在 - 应抛异常")
    void testGetUserInfo_NotExist() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> authService.getUserInfo(999L));
    }

    // ==================== 辅助方法 ====================

    private LoginUser buildLoginUser(Long userId, String username, String realName,
                                      Long tenantId, String accountType) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setUsername(username);
        user.setRealName(realName);
        user.setTenantId(tenantId);
        user.setAccountType(accountType);
        user.setPermissions(Collections.emptySet());
        return user;
    }

    private void mockRedisForPermissionCache() {
        lenient().when(redisTemplate.delete(anyString())).thenReturn(true);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(setOperations.add(anyString(), any(String[].class))).thenReturn(1L);
        lenient().when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(userMapper.selectPermissionsByUserIdScoped(anyLong(), anyLong()))
                .thenReturn(List.of("system:user:list", "system:role:list"));
    }

    private void mockUserInfoQuery(Long userId, String username, String realName) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername(username);
        user.setRealName(realName);
        user.setTenantId(1L);
        user.setAccountType("ADMIN");
        lenient().when(userMapper.selectById(userId)).thenReturn(user);

        SysRole role = new SysRole();
        role.setRoleCode("admin");
        lenient().when(roleMapper.selectRolesByUserIdScoped(eq(userId), anyLong())).thenReturn(List.of(role));

        lenient().when(userMapper.selectPermissionsByUserIdScoped(eq(userId), anyLong()))
                .thenReturn(List.of("system:user:list"));

        SysTenant tenant = new SysTenant();
        tenant.setTenantCode("DEFAULT");
        lenient().when(tenantMapper.selectById(1L)).thenReturn(tenant);
    }
}
