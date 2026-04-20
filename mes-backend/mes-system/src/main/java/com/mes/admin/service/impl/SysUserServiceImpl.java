package com.mes.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mes.admin.domain.dto.SysUserDTO;
import com.mes.admin.domain.entity.SysUser;
import com.mes.admin.domain.entity.SysUserRole;
import com.mes.admin.domain.query.SysUserQuery;
import com.mes.admin.domain.vo.SysRoleVO;
import com.mes.admin.domain.vo.SysUserVO;
import com.mes.admin.mapper.SysRoleMapper;
import com.mes.admin.mapper.SysUserMapper;
import com.mes.admin.mapper.SysUserRoleMapper;
import com.mes.admin.service.ISysUserService;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.framework.security.StaffPortalRestrictionFilter;
import com.mes.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements ISysUserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<SysUserVO> page(SysUserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getRealName()), SysUser::getRealName, query.getRealName())
                .eq(query.getEnabled() != null, SysUser::getEnabled, query.getEnabled())
                .orderByDesc(SysUser::getCreatedTime);
        Long tid = TenantContextHolder.getTenantId();
        if (tid != null) {
            wrapper.eq(SysUser::getTenantId, tid);
        }

        Page<SysUser> page = userMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        List<SysUserVO> voList = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public SysUserVO getDetail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        assertSameTenant(user);
        return toVO(user);
    }

    /**
     * 仅限 {@code TenantProvisionerImpl} 使用：新租户初始化时创建第一个管理员账号。
     *
     * <p>和普通 {@link #create} 相比：</p>
     * <ul>
     *   <li>跳过"账号上限"配额（租户刚建，配额表还没有记录）；</li>
     *   <li>跳过"当前上下文租户"的推断，直接用传入的 tenantId；</li>
     *   <li>自动绑定到该租户的 ADMIN 角色。</li>
     * </ul>
     */
    @Transactional(rollbackFor = Exception.class)
    public Long bootstrapTenantAdmin(Long tenantId, String username, String password) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(
                StringUtils.hasText(password) ? password : "Admin@" + tenantId + "!Change"));
        user.setRealName("租户管理员");
        user.setEnabled(true);
        user.setTenantId(tenantId);
        user.setAccountType("ADMIN");
        userMapper.insert(user);

        // 绑定租户 ADMIN 角色（TenantProvisionerImpl 已克隆）
        SysRoleMapper.class.getName(); // keep import
        List<com.mes.admin.domain.entity.SysRole> roles = roleMapper.selectList(
                new LambdaQueryWrapper<com.mes.admin.domain.entity.SysRole>()
                        .eq(com.mes.admin.domain.entity.SysRole::getTenantId, tenantId)
                        .eq(com.mes.admin.domain.entity.SysRole::getRoleCode, "ADMIN"));
        if (!roles.isEmpty()) {
            SysUserRole ur = new SysUserRole();
            ur.setTenantId(tenantId);
            ur.setUserId(user.getId());
            ur.setRoleId(roles.get(0).getId());
            userRoleMapper.insert(ur);
        }
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysUserDTO dto) {
        Long tid = TenantContextHolder.requireTenantId();
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tid)
                        .eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) throw new BusinessException("当前租户下用户名已存在");

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(
                StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : "123456"));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        user.setFactoryCode(dto.getFactoryCode());
        user.setTenantId(tid);
        String at = StringUtils.hasText(dto.getAccountType()) ? dto.getAccountType().trim().toUpperCase() : "STAFF";
        if (!"ADMIN".equals(at) && !StaffPortalRestrictionFilter.ACCOUNT_STAFF.equals(at)) {
            throw new BusinessException("账号类型须为 ADMIN 或 STAFF");
        }
        user.setAccountType(at);
        userMapper.insert(user);

        saveUserRoles(user.getId(), dto.getRoleIds());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SysUserDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        assertSameTenant(user);

        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setEnabled(dto.getEnabled());
        user.setFactoryCode(dto.getFactoryCode());
        if (StringUtils.hasText(dto.getAccountType())) {
            String at = dto.getAccountType().trim().toUpperCase();
            if (!"ADMIN".equals(at) && !StaffPortalRestrictionFilter.ACCOUNT_STAFF.equals(at)) {
                throw new BusinessException("账号类型须为 ADMIN 或 STAFF");
            }
            user.setAccountType(at);
        }
        userMapper.updateById(user);

        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        saveUserRoles(id, dto.getRoleIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        assertSameTenant(user);
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        assertSameTenant(user);
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    private void assertSameTenant(SysUser user) {
        Long tid = TenantContextHolder.getTenantId();
        if (tid == null || user.getTenantId() == null) {
            return;
        }
        if (!tid.equals(user.getTenantId())) {
            throw new BusinessException("无权操作其他租户的用户");
        }
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds != null && !roleIds.isEmpty()) {
            Long tid = TenantContextHolder.requireTenantId();
            for (Long roleId : roleIds) {
                userRoleMapper.insert(new SysUserRole(tid, userId, roleId));
            }
        }
    }

    private SysUserVO toVO(SysUser user) {
        SysUserVO vo = new SysUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setEnabled(user.getEnabled());
        vo.setFactoryCode(user.getFactoryCode());
        vo.setTenantId(user.getTenantId());
        vo.setAccountType(user.getAccountType());
        vo.setCreatedTime(user.getCreatedTime());
        vo.setUpdatedTime(user.getUpdatedTime());

        var roles = roleMapper.selectRolesByUserId(user.getId());
        vo.setRoles(roles.stream().map(r -> {
            SysRoleVO rv = new SysRoleVO();
            rv.setId(r.getId());
            rv.setRoleName(r.getRoleName());
            rv.setRoleCode(r.getRoleCode());
            return rv;
        }).collect(Collectors.toList()));
        return vo;
    }
}
