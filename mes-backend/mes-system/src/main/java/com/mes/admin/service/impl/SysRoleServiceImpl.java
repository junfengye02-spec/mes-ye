package com.mes.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mes.admin.domain.dto.SysRoleDTO;
import com.mes.admin.domain.entity.SysRole;
import com.mes.admin.domain.entity.SysRoleMenu;
import com.mes.admin.domain.query.SysRoleQuery;
import com.mes.admin.domain.vo.SysRoleVO;
import com.mes.admin.mapper.SysMenuMapper;
import com.mes.admin.mapper.SysRoleMapper;
import com.mes.admin.mapper.SysRoleMenuMapper;
import com.mes.admin.service.ISysRoleService;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements ISysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public PageResult<SysRoleVO> page(SysRoleQuery query) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(StringUtils.hasText(query.getRoleName()), SysRole::getRoleName, query.getRoleName())
                .like(StringUtils.hasText(query.getRoleCode()), SysRole::getRoleCode, query.getRoleCode())
                .eq(query.getEnabled() != null, SysRole::getEnabled, query.getEnabled())
                .orderByAsc(SysRole::getId);

        Page<SysRole> page = roleMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        List<SysRoleVO> voList = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public List<SysRoleVO> listAll() {
        List<SysRole> roles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getEnabled, true).orderByAsc(SysRole::getId));
        return roles.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public SysRoleVO getDetail(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException("角色不存在");
        return toVO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysRoleDTO dto) {
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, dto.getRoleCode()));
        if (count > 0) throw new BusinessException("角色编码已存在");

        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        role.setDescription(dto.getDescription());
        role.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        roleMapper.insert(role);

        if (dto.getMenuIds() != null) {
            assignMenus(role.getId(), dto.getMenuIds());
        }
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SysRoleDTO dto) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException("角色不存在");
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setEnabled(dto.getEnabled());
        roleMapper.updateById(role);
    }

    @Override
    public void delete(Long id) {
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        // P3 硬化：sys_role_menu 的 tenant_id 必须与所挂 role 一致，避免把
        // 租户 A 的角色挂上租户 B 的菜单。优先用 role 自身的 tenantId，兜底
        // 到当前请求上下文（TenantContextHolder），两者都缺才失败。
        Long tenantId = resolveRoleTenantId(roleId);
        for (Long menuId : menuIds) {
            roleMenuMapper.insert(new SysRoleMenu(tenantId, roleId, menuId));
        }
    }

    private Long resolveRoleTenantId(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role != null && role.getTenantId() != null) {
            return role.getTenantId();
        }
        Long ctx = TenantContextHolder.getTenantId();
        if (ctx != null) {
            return ctx;
        }
        throw new BusinessException("角色未绑定租户上下文，无法分配菜单");
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        Long tenantId = resolveRoleTenantId(roleId);
        return menuMapper.selectMenuIdsByRoleId(roleId, tenantId);
    }

    private SysRoleVO toVO(SysRole role) {
        SysRoleVO vo = new SysRoleVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleCode(role.getRoleCode());
        vo.setDescription(role.getDescription());
        vo.setEnabled(role.getEnabled());
        vo.setCreatedTime(role.getCreatedTime());
        return vo;
    }
}
