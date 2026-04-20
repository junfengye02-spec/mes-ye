package com.mes.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.admin.domain.entity.SysTenant;
import com.mes.admin.domain.entity.SysUser;
import com.mes.admin.mapper.SysTenantMapper;
import com.mes.admin.mapper.SysUserMapper;
import com.mes.framework.security.LoginTenantContext;
import com.mes.framework.security.LoginUser;
import com.mes.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Spring Security 用户加载实现（多租户版）。
 *
 * <p>查询顺序：</p>
 * <ol>
 *   <li>若 {@link LoginTenantContext} 带 tenantCode：按 (tenant, username) 精确匹配；</li>
 *   <li>否则按 username 全局宽松查：
 *     <ul>
 *       <li>命中 0 个：用户不存在；</li>
 *       <li>命中 1 个：通过；</li>
 *       <li>命中 ≥ 2 个：要求前端显式传 tenantCode，否则拒绝。</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p><strong>为什么不自动遍历租户？</strong> 密码 BCrypt 校验发生在 Spring 框架里，
 * UserDetailsService 必须返回一个明确的用户行；让系统在多租户间"瞎猜"会产生定时攻击面，
 * 也会给审计日志带来歧义。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysTenantMapper tenantMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String tenantCode = LoginTenantContext.getTenantCode();
        SysUser user;

        if (StringUtils.hasText(tenantCode)) {
            Long tenantId = resolveTenantId(tenantCode);
            user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getTenantId, tenantId)
                    .eq(SysUser::getUsername, username));
            if (user == null) {
                throw new UsernameNotFoundException("用户不存在: " + username + "（租户 " + tenantCode + "）");
            }
        } else {
            List<SysUser> matched = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, username));
            if (matched == null || matched.isEmpty()) {
                throw new UsernameNotFoundException("用户不存在: " + username);
            }
            if (matched.size() > 1) {
                throw new UsernameNotFoundException(
                        "用户名 " + username + " 在多个租户下同名，请在登录时显式指定 tenantCode 或通过子域名登录");
            }
            user = matched.get(0);
        }

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        Long tenantId = user.getTenantId() != null ? user.getTenantId() : 1L;
        // 在认证阶段提前置入租户上下文，保证 RBAC 级联 SQL 能命中正确租户数据
        TenantContextHolder.setTenantId(tenantId);

        List<String> permissions = userMapper.selectPermissionsByUserIdScoped(user.getId(), tenantId);
        String accountType = StringUtils.hasText(user.getAccountType())
                ? user.getAccountType().toUpperCase(Locale.ROOT)
                : "ADMIN";

        return new LoginUser(
                user.getId(), user.getUsername(), user.getPassword(),
                user.getRealName(), Boolean.TRUE.equals(user.getEnabled()),
                new HashSet<>(permissions),
                tenantId,
                accountType);
    }

    private Long resolveTenantId(String tenantCode) {
        SysTenant tenant = tenantMapper.selectByTenantCode(tenantCode);
        if (tenant == null) {
            throw new UsernameNotFoundException("租户不存在: " + tenantCode);
        }
        // status: 0=PENDING 1=ACTIVE 2=PROVISIONING 3=SUSPENDED 4=ARCHIVED
        if (tenant.getStatus() == null || tenant.getStatus() != 1) {
            throw new UsernameNotFoundException("租户不可用（状态=" + tenant.getStatus() + "）: " + tenantCode);
        }
        return tenant.getId();
    }
}
