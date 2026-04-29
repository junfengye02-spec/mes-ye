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
import com.mes.common.utils.PasswordGenerator;
import com.mes.framework.security.StaffPortalRestrictionFilter;
import com.mes.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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
        // P0-07 安全整改：禁止使用硬编码弱密码 "123456"。
        // 管理员未指定初始密码时，由服务端生成随机强密码，并通过 result 返回给调用方（管理员需一次性告知用户）。
        String initPwd = StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : PasswordGenerator.generate();
        user.setPassword(passwordEncoder.encode(initPwd));
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
        resetPasswordAndReturn(id, newPassword);
    }

    /**
     * 重置密码并返回实际生效的密码明文。
     *
     * <p>P0-07 安全整改：</p>
     * <ul>
     *   <li>newPassword 非空：使用管理员指定的密码（需满足长度 ≥ 8 的校验，调用方保证）</li>
     *   <li>newPassword 空/空白：服务端生成随机强密码（12 位，含大小写数字特殊符号）</li>
     * </ul>
     *
     * @param id          用户 ID
     * @param newPassword 新密码明文，可为空；空时生成随机密码
     * @return 实际生效的密码明文，一次性返回给管理员当面告知用户
     */
    @Transactional(rollbackFor = Exception.class)
    public String resetPasswordAndReturn(Long id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        assertSameTenant(user);
        String effectivePwd = StringUtils.hasText(newPassword) ? newPassword : PasswordGenerator.generate();
        user.setPassword(passwordEncoder.encode(effectivePwd));
        userMapper.updateById(user);
        log.info("[P0-07] 重置密码成功 userId={} 密码由管理员指定={}", id, StringUtils.hasText(newPassword));
        return effectivePwd;
    }

    /**
     * 当前登录用户自助修改密码（P0-06）。
     *
     * <p>见 {@link ISysUserService#changeMyPassword(Long, String, String)} 的完整契约。
     * 这里只做服务端二次校验与写库，权限由 Controller 的 {@code @PreAuthorize("isAuthenticated()")} 保证。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeMyPassword(Long currentUserId, String oldPassword, String newPassword) {
        if (currentUserId == null) {
            throw new BusinessException("未识别到当前登录用户");
        }
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new BusinessException("旧密码和新密码均不能为空");
        }
        if (newPassword.length() < 8 || newPassword.length() > 64) {
            throw new BusinessException("新密码长度必须在 8-64 位之间");
        }
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException("新密码不能与旧密码相同");
        }
        if (!isPasswordStrongEnough(newPassword)) {
            throw new BusinessException("新密码必须包含大写字母/小写字母/数字/特殊字符中至少 3 类");
        }

        SysUser user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 这里不强制 assertSameTenant：因为 currentUserId 来自 SecurityContext，
        // 天然就是当前登录用户自己；不过 sys_user 本来就在租户拦截忽略名单，安全性靠 "只能改自己" 保障。
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("[P0-06] 自助改密时旧密码错误 userId={}", currentUserId);
            throw new BusinessException("旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        // 成功改密后清除弱口令强制改密标记
        user.setMustChangePassword(0);
        userMapper.updateById(user);
        log.info("[P0-06] 自助改密成功 userId={} username={} 已复位 mustChangePassword=0",
                currentUserId, user.getUsername());
    }

    /**
     * 密码强度规则：大写/小写/数字/特殊字符中至少出现 3 类。
     *
     * <p>与 {@code PasswordGenerator} 生成的随机强密码策略保持一致。</p>
     *
     * @param pwd 新密码明文
     * @return true=满足强度要求
     */
    private boolean isPasswordStrongEnough(String pwd) {
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (int i = 0; i < pwd.length(); i++) {
            char c = pwd.charAt(i);
            if (c >= 'A' && c <= 'Z') hasUpper = true;
            else if (c >= 'a' && c <= 'z') hasLower = true;
            else if (c >= '0' && c <= '9') hasDigit = true;
            else hasSpecial = true;
        }
        int categories = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0)
                + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
        return categories >= 3;
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
