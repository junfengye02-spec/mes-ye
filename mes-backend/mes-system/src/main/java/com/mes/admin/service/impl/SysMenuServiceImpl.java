package com.mes.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.admin.domain.dto.SysMenuDTO;
import com.mes.admin.domain.entity.SysMenu;
import com.mes.admin.domain.vo.SysMenuVO;
import com.mes.admin.mapper.SysMenuMapper;
import com.mes.admin.service.ISysMenuService;
import com.mes.common.exception.BusinessException;
import com.mes.framework.security.LoginUser;
import com.mes.framework.security.SecurityUtils;
import com.mes.framework.security.StaffPortalRestrictionFilter;
import com.mes.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements ISysMenuService {

    private final SysMenuMapper menuMapper;

    @Override
    public List<SysMenuVO> getTree() {
        Long tenantId = TenantContextHolder.requireTenantId();
        List<SysMenu> menus = selectTreeMenus(tenantId);
        if (menus.isEmpty() && !TenantContextHolder.PLATFORM_TENANT_ID.equals(tenantId)) {
            menus = selectTreeMenus(TenantContextHolder.PLATFORM_TENANT_ID);
        }
        return buildTree(menus);
    }

    @Override
    public List<SysMenuVO> getUserTree(Long userId) {
        Long tenantId = TenantContextHolder.requireTenantId();
        List<SysMenu> menus = menuMapper.selectMenusByUserId(userId, tenantId);
        LoginUser u = SecurityUtils.getCurrentUser();
        if (u != null && StaffPortalRestrictionFilter.ACCOUNT_STAFF.equals(u.getAccountType())) {
            menus = menus.stream()
                    .filter(m -> m.getPath() == null || !m.getPath().startsWith("/system"))
                    .collect(Collectors.toList());
        }
        return buildTree(menus);
    }

    private List<SysMenu> selectTreeMenus(Long tenantId) {
        return menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getTenantId, tenantId)
                        .orderByAsc(SysMenu::getParentId)
                        .orderByAsc(SysMenu::getSortOrder)
                        .orderByAsc(SysMenu::getId));
    }

    @Override
    public SysMenuVO getDetail(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) throw new BusinessException("菜单不存在");
        return toVO(menu);
    }

    @Override
    public Long create(SysMenuDTO dto) {
        SysMenu menu = new SysMenu();
        menu.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        menu.setMenuName(dto.getMenuName());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setMenuType(dto.getMenuType());
        menu.setPermission(dto.getPermission());
        menu.setIcon(dto.getIcon());
        menu.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        menu.setVisible(dto.getVisible() != null ? dto.getVisible() : true);
        menuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    public void update(Long id, SysMenuDTO dto) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) throw new BusinessException("菜单不存在");
        menu.setParentId(dto.getParentId());
        menu.setMenuName(dto.getMenuName());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setMenuType(dto.getMenuType());
        menu.setPermission(dto.getPermission());
        menu.setIcon(dto.getIcon());
        menu.setSortOrder(dto.getSortOrder());
        menu.setVisible(dto.getVisible());
        menuMapper.updateById(menu);
    }

    @Override
    public void delete(Long id) {
        Long childCount = menuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount > 0) throw new BusinessException("存在子菜单，无法删除");
        menuMapper.deleteById(id);
    }

    private List<SysMenuVO> buildTree(List<SysMenu> menus) {
        List<SysMenuVO> voList = menus.stream()
                .filter(menu -> !"B".equals(menu.getMenuType()))
                .map(this::toVO)
                .collect(Collectors.toList());
        Map<Long, List<SysMenuVO>> childMap = voList.stream()
                .filter(m -> m.getParentId() != null && m.getParentId() != 0)
                .collect(Collectors.groupingBy(SysMenuVO::getParentId));

        voList.forEach(m -> m.setChildren(childMap.getOrDefault(m.getId(), new ArrayList<>())));
        return voList.stream()
                .filter(m -> m.getParentId() == null || m.getParentId() == 0)
                .collect(Collectors.toList());
    }

    private SysMenuVO toVO(SysMenu menu) {
        SysMenuVO vo = new SysMenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setMenuType(menu.getMenuType());
        vo.setPermission(menu.getPermission());
        vo.setIcon(menu.getIcon());
        vo.setSortOrder(menu.getSortOrder());
        vo.setVisible(menu.getVisible());
        return vo;
    }
}
