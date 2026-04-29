package com.mes.admin.service;

import com.mes.admin.domain.entity.SysRole;
import com.mes.admin.domain.entity.SysRoleMenu;
import com.mes.admin.mapper.SysMenuMapper;
import com.mes.admin.mapper.SysRoleMapper;
import com.mes.admin.mapper.SysRoleMenuMapper;
import com.mes.admin.service.impl.SysRoleServiceImpl;
import com.mes.common.exception.BusinessException;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P3 多租户硬化：{@link SysRoleServiceImpl#assignMenus(Long, java.util.List)}
 * 在给角色分配菜单时，必须保证新插入的 {@link SysRoleMenu} 的 tenantId 与
 * 所挂 {@link SysRole#getTenantId()} 一致——防止"租户 A 的角色"挂上
 * "租户 B 的菜单"。
 *
 * <p>本用例通过两个独立 tenant 上下文切换 + ArgumentCaptor 捕获实际插入
 * 的 SysRoleMenu 行，证明 tenant_id 被正确绑定，且租户 A/B 的菜单分配
 * 互不串位。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysRoleMenuTenantIsolationTest {

    @Mock private SysRoleMapper roleMapper;
    @Mock private SysRoleMenuMapper roleMenuMapper;
    @Mock private SysMenuMapper menuMapper;

    @InjectMocks private SysRoleServiceImpl sysRoleService;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("assignMenus 租户 A：SysRoleMenu 的 tenantId 必须等于 role.tenantId=A")
    void assignMenus_shouldBindTenantIdFromRole_tenantA() {
        // given: 租户 A 下一个角色 roleId=11
        SysRole role = new SysRole();
        role.setId(11L);
        role.setTenantId(101L);
        when(roleMapper.selectById(11L)).thenReturn(role);
        when(roleMenuMapper.delete(any())).thenReturn(0);
        when(roleMenuMapper.insert(any(SysRoleMenu.class))).thenReturn(1);

        // when: 分配 2 个菜单
        sysRoleService.assignMenus(11L, List.of(1001L, 1002L));

        // then: 两条 sys_role_menu 的 tenantId 都必须等于 101
        ArgumentCaptor<SysRoleMenu> captor = ArgumentCaptor.forClass(SysRoleMenu.class);
        verify(roleMenuMapper, times(2)).insert(captor.capture());
        List<SysRoleMenu> actual = captor.getAllValues();

        assertEquals(101L, actual.get(0).getTenantId(),
                "租户 A 角色分配的菜单关联必须记到 A 的 tenant_id");
        assertEquals(11L, actual.get(0).getRoleId());
        assertEquals(1001L, actual.get(0).getMenuId());

        assertEquals(101L, actual.get(1).getTenantId());
        assertEquals(11L, actual.get(1).getRoleId());
        assertEquals(1002L, actual.get(1).getMenuId());
    }

    @Test
    @DisplayName("assignMenus 租户 B：换成另一租户后 tenantId 必须隔离为 B，不能串到 A")
    void assignMenus_shouldIsolateBetweenTenants_tenantB() {
        // given: 租户 B 下另一个角色 roleId=22
        SysRole role = new SysRole();
        role.setId(22L);
        role.setTenantId(202L);
        when(roleMapper.selectById(22L)).thenReturn(role);
        when(roleMenuMapper.delete(any())).thenReturn(0);
        when(roleMenuMapper.insert(any(SysRoleMenu.class))).thenReturn(1);

        sysRoleService.assignMenus(22L, List.of(3001L));

        ArgumentCaptor<SysRoleMenu> captor = ArgumentCaptor.forClass(SysRoleMenu.class);
        verify(roleMenuMapper).insert(captor.capture());
        SysRoleMenu inserted = captor.getValue();

        assertEquals(202L, inserted.getTenantId(),
                "租户 B 角色分配的菜单关联必须记到 B 的 tenant_id，不能串到 A=101");
        assertNotEquals(101L, inserted.getTenantId(),
                "跨租户隔离保证：B 租户的关联 tenant_id 绝不能出现 A=101");
        assertEquals(22L, inserted.getRoleId());
        assertEquals(3001L, inserted.getMenuId());
    }

    @Test
    @DisplayName("assignMenus 兜底：role.tenantId=null 时用 TenantContextHolder 补齐")
    void assignMenus_shouldFallbackToContext_whenRoleTenantNull() {
        SysRole role = new SysRole();
        role.setId(33L);
        role.setTenantId(null);
        when(roleMapper.selectById(33L)).thenReturn(role);
        when(roleMenuMapper.delete(any())).thenReturn(0);
        when(roleMenuMapper.insert(any(SysRoleMenu.class))).thenReturn(1);

        TenantContextHolder.setTenantId(909L);
        try {
            sysRoleService.assignMenus(33L, List.of(5001L));
        } finally {
            TenantContextHolder.clear();
        }

        ArgumentCaptor<SysRoleMenu> captor = ArgumentCaptor.forClass(SysRoleMenu.class);
        verify(roleMenuMapper).insert(captor.capture());
        assertEquals(909L, captor.getValue().getTenantId(),
                "role.tenantId 缺失时应兜底采用当前请求 TenantContext=909");
    }

    @Test
    @DisplayName("assignMenus 硬失败：role 不存在且无 TenantContext 时必须抛业务异常，禁止写库")
    void assignMenus_shouldFailFast_whenNoTenantAvailable() {
        when(roleMapper.selectById(44L)).thenReturn(null);
        // 无 TenantContext
        assertThrows(BusinessException.class,
                () -> sysRoleService.assignMenus(44L, List.of(6001L)),
                "既无 role 也无上下文时必须抛 BusinessException，拒绝写脏数据");
        verify(roleMenuMapper, never()).insert(any(SysRoleMenu.class));
    }

    @Test
    @DisplayName("assignMenus 空 menu 列表：不应触发任何 insert，也不应做租户解析")
    void assignMenus_emptyMenuIds_noop() {
        sysRoleService.assignMenus(55L, List.of());
        verify(roleMenuMapper, never()).insert(any(SysRoleMenu.class));
        verify(roleMapper, never()).selectById(55L);
    }
}
