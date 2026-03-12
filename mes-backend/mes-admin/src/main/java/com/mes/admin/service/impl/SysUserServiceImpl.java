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

        Page<SysUser> page = userMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        List<SysUserVO> voList = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public SysUserVO getDetail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysUserDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) throw new BusinessException("用户名已存在");

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(
                StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : "123456"));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        user.setFactoryCode(dto.getFactoryCode());
        userMapper.insert(user);

        saveUserRoles(user.getId(), dto.getRoleIds());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SysUserDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");

        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setEnabled(dto.getEnabled());
        user.setFactoryCode(dto.getFactoryCode());
        userMapper.updateById(user);

        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        saveUserRoles(id, dto.getRoleIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                userRoleMapper.insert(new SysUserRole(userId, roleId));
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
