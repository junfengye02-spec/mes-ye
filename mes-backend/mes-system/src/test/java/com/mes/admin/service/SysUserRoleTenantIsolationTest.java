package com.mes.admin.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mes.admin.domain.entity.SysRole;
import com.mes.admin.domain.entity.SysUser;
import com.mes.admin.domain.entity.SysUserRole;
import com.mes.admin.mapper.SysRoleMapper;
import com.mes.admin.mapper.SysUserMapper;
import com.mes.admin.mapper.SysUserRoleMapper;
import com.mes.admin.service.impl.SysUserServiceImpl;
import com.mes.framework.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * P3 多租户硬化：{@link SysUserServiceImpl#bootstrapTenantAdmin(Long, String, String)}
 * 创建租户初始管理员时，写入的 {@link SysUserRole} 必须带上该租户的 tenant_id。
 *
 * <p>测试点：</p>
 * <ol>
 *   <li>租户 A：bootstrap 后插入的 SysUserRole.tenantId = A；</li>
 *   <li>租户 B：换租户再 bootstrap，tenantId = B，与 A 隔离；</li>
 *   <li>构造器一致性：{@code new SysUserRole(tid, uid, rid)} 严格按位赋值，
 *       防止参数顺序回退导致串 tenant。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysUserRoleTenantIsolationTest {

    @Mock private SysUserMapper userMapper;
    @Mock private SysRoleMapper roleMapper;
    @Mock private SysUserRoleMapper userRoleMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private SysUserServiceImpl sysUserService;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("bootstrapTenantAdmin 租户 A：SysUserRole 的 tenantId=101")
    void bootstrap_tenantA_shouldTagUserRoleWithTenantA() {
        // given: ADMIN 角色存在于租户 101 下
        SysRole adminRole = new SysRole();
        adminRole.setId(9001L);
        adminRole.setRoleCode("ADMIN");
        adminRole.setTenantId(101L);
        when(roleMapper.selectList(any())).thenReturn(List.of(adminRole));
        when(passwordEncoder.encode(anyString())).thenReturn("$encoded$");
        // userMapper.insert 会把自增 id 回填到 entity，这里模拟一下
        doAnswer(invocation -> {
            SysUser u = invocation.getArgument(0);
            u.setId(777L);
            return 1;
        }).when(userMapper).insert(any(SysUser.class));
        when(userRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);

        // when
        Long newUserId = sysUserService.bootstrapTenantAdmin(101L, "alice", "StrongP@ss1");

        // then
        assertEquals(777L, newUserId);
        ArgumentCaptor<SysUserRole> captor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(userRoleMapper).insert(captor.capture());
        SysUserRole ur = captor.getValue();
        assertEquals(101L, ur.getTenantId(),
                "租户 A 创建的 admin 绑定关系 tenant_id 必须=101");
        assertEquals(777L, ur.getUserId());
        assertEquals(9001L, ur.getRoleId());
    }

    @Test
    @DisplayName("bootstrapTenantAdmin 租户 B：SysUserRole 的 tenantId=202，不与 A 串")
    void bootstrap_tenantB_shouldIsolateFromTenantA() {
        SysRole adminRole = new SysRole();
        adminRole.setId(9002L);
        adminRole.setRoleCode("ADMIN");
        adminRole.setTenantId(202L);
        when(roleMapper.selectList(any())).thenReturn(List.of(adminRole));
        when(passwordEncoder.encode(anyString())).thenReturn("$encoded$");
        doAnswer(invocation -> {
            SysUser u = invocation.getArgument(0);
            u.setId(888L);
            return 1;
        }).when(userMapper).insert(any(SysUser.class));
        when(userRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);

        sysUserService.bootstrapTenantAdmin(202L, "bob", "StrongP@ss2");

        ArgumentCaptor<SysUserRole> captor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(userRoleMapper).insert(captor.capture());
        SysUserRole ur = captor.getValue();
        assertEquals(202L, ur.getTenantId(),
                "租户 B 创建的 admin 绑定关系 tenant_id 必须=202");
        assertNotEquals(101L, ur.getTenantId(),
                "跨租户隔离：B 的绑定关系里绝对不能出现 A=101");
        assertEquals(888L, ur.getUserId());
        assertEquals(9002L, ur.getRoleId());
    }

    @Test
    @DisplayName("bootstrapTenantAdmin 租户无 ADMIN 角色：不应插入 SysUserRole")
    void bootstrap_noAdminRole_shouldSkipUserRoleInsert() {
        when(roleMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(passwordEncoder.encode(anyString())).thenReturn("$encoded$");
        doAnswer(invocation -> {
            SysUser u = invocation.getArgument(0);
            u.setId(999L);
            return 1;
        }).when(userMapper).insert(any(SysUser.class));

        sysUserService.bootstrapTenantAdmin(303L, "carol", "StrongP@ss3");

        verify(userRoleMapper, never()).insert(any(SysUserRole.class));
    }

    @Test
    @DisplayName("SysUserRole 构造器顺序：new SysUserRole(tid, uid, rid) 必须按位赋值")
    void sysUserRoleConstructorArity_mustStayStable() {
        // 硬约束：防止后续重构把字段顺序颠倒导致 tenantId/userId/roleId 串位
        SysUserRole ur = new SysUserRole(101L, 10L, 99L);
        assertEquals(101L, ur.getTenantId(), "第 1 个参数必须写入 tenantId");
        assertEquals(10L, ur.getUserId(), "第 2 个参数必须写入 userId");
        assertEquals(99L, ur.getRoleId(), "第 3 个参数必须写入 roleId");
    }

    @Test
    @DisplayName("SysRoleMenu 构造器顺序：new SysRoleMenu(tid, rid, mid) 必须按位赋值")
    void sysRoleMenuConstructorArity_mustStayStable() {
        com.mes.admin.domain.entity.SysRoleMenu rm =
                new com.mes.admin.domain.entity.SysRoleMenu(202L, 88L, 7777L);
        assertEquals(202L, rm.getTenantId(), "第 1 个参数必须写入 tenantId");
        assertEquals(88L, rm.getRoleId(), "第 2 个参数必须写入 roleId");
        assertEquals(7777L, rm.getMenuId(), "第 3 个参数必须写入 menuId");
    }

    /** 抑制 IDE 对 Wrapper 泛型未使用的提示，保留给后续扩展（LambdaQueryWrapper 场景）。 */
    @SuppressWarnings("unused")
    private Wrapper<?> reservedForLambdaWrapperExtension;
}
