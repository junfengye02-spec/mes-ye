package com.mes.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.admin.domain.entity.SysMenu;
import com.mes.admin.domain.vo.SysMenuVO;
import com.mes.admin.mapper.SysMenuMapper;
import com.mes.admin.service.impl.SysMenuServiceImpl;
import com.mes.framework.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * sys_menu / sys_role_menu / sys_user_role are excluded from the MyBatis tenant
 * interceptor, so menu service code must scope those queries explicitly.
 */
@ExtendWith(MockitoExtension.class)
class SysMenuServiceTenantIsolationTest {

    @Mock private SysMenuMapper menuMapper;

    @InjectMocks private SysMenuServiceImpl sysMenuService;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("getTree 缺少 TenantContext 时必须 fail-fast")
    void getTree_shouldRequireTenantContext() {
        assertThrows(IllegalStateException.class,
                () -> sysMenuService.getTree(),
                "sys_menu 不走租户拦截器，管理菜单树不能在无租户上下文时查询全表");
    }

    @Test
    @DisplayName("getTree 必须显式按当前 tenant_id 查询")
    void getTree_shouldFilterByCurrentTenant() {
        TenantContextHolder.setTenantId(1L);
        when(menuMapper.selectList(any())).thenReturn(List.of(menu(3L, 0L, "工艺管理", "D")));

        sysMenuService.getTree();

        ArgumentCaptor<LambdaQueryWrapper<SysMenu>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verifySelectListWrapper(captor);
        assertFalse(captor.getValue().getExpression().getNormal().isEmpty(),
                "getTree 查询必须包含普通条件段，不能只做排序后查询 sys_menu 全表");
    }

    @Test
    @DisplayName("getUserTree 缺少 TenantContext 时必须 fail-fast")
    void getUserTree_shouldRequireTenantContext() {
        assertThrows(IllegalStateException.class,
                () -> sysMenuService.getUserTree(7L),
                "用户菜单树必须绑定当前租户，不能跨租户聚合 sys_role_menu");
    }

    @Test
    @DisplayName("菜单树构建时按钮权限不能作为导航节点返回")
    void buildTree_shouldExcludeButtonNodesFromNavigationTree() throws Exception {
        List<SysMenu> menus = List.of(
                menu(3L, 0L, "工艺管理", "D"),
                menu(301L, 3L, "执行指示", "M"),
                menu(30101L, 301L, "新增", "B")
        );

        List<SysMenuVO> tree = invokeBuildTree(menus);

        assertEquals(1, tree.size());
        assertEquals("工艺管理", tree.get(0).getMenuName());
        assertEquals(1, tree.get(0).getChildren().size());
        SysMenuVO processMenu = tree.get(0).getChildren().get(0);
        assertEquals("执行指示", processMenu.getMenuName());
        assertTrue(processMenu.getChildren().isEmpty(),
                "按钮权限只应参与鉴权，不能出现在导航树 children 中");
    }

    private void verifySelectListWrapper(ArgumentCaptor<LambdaQueryWrapper<SysMenu>> captor) {
        org.mockito.Mockito.verify(menuMapper).selectList(captor.capture());
    }

    @SuppressWarnings("unchecked")
    private List<SysMenuVO> invokeBuildTree(List<SysMenu> menus) throws Exception {
        Method method = SysMenuServiceImpl.class.getDeclaredMethod("buildTree", List.class);
        method.setAccessible(true);
        return (List<SysMenuVO>) method.invoke(sysMenuService, menus);
    }

    private SysMenu menu(Long id, Long parentId, String name, String type) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setMenuName(name);
        menu.setMenuType(type);
        menu.setVisible(true);
        menu.setSortOrder(1);
        return menu;
    }
}
