package com.mes.admin.service.impl;

import com.mes.admin.domain.entity.SysTenant;
import com.mes.admin.mapper.SysTenantMapper;
import com.mes.admin.service.ITenantProvisioner;
import com.mes.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 租户初始化（Provisioning）流水：
 * <ol>
 *   <li>校验租户编码可用；</li>
 *   <li>状态 PROVISIONING；</li>
 *   <li>克隆平台模板菜单到租户（复制 sys_menu tenant_id=0 的行到该 tenant_id）；</li>
 *   <li>克隆平台模板角色；</li>
 *   <li>给 ADMIN 角色绑定租户自身全部菜单；</li>
 *   <li>创建首个管理员账号（默认密码 + 必须首登修改）；</li>
 *   <li>初始化配额用量为 0；</li>
 *   <li>状态 ACTIVE。</li>
 * </ol>
 *
 * <p>每一步写 {@code sys_tenant_provision_log}。中途失败状态保持 PROVISIONING，
 * 平台运营人员在后台"重试"可继续从失败点继续；全部成功后置 ACTIVE。</p>
 *
 * <p>注意：本类内部涉及 sys_menu / sys_role 等表，这些表都在 MyBatis-Plus
 * 租户拦截器的忽略名单中（参见 {@code MybatisPlusConfig}），因此跨租户 INSERT 是被允许的。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisionerImpl implements ITenantProvisioner {

    private final SysTenantMapper tenantMapper;
    private final JdbcTemplate jdbcTemplate;

    /** 循环依赖兜底：SysUserServiceImpl 可能用到 tenantMapper，延迟注入。 */
    @Lazy
    @Autowired
    private SysUserServiceImpl sysUserService;

    @Override
    @Async("tenantProvisionExecutor")
    public void provisionAsync(Long tenantId, String initialAdminUsername, String initialAdminPassword) {
        try {
            provisionInternal(tenantId, initialAdminUsername, initialAdminPassword);
        } catch (Exception e) {
            log.error("[Provision] 租户初始化失败 tenantId={}", tenantId, e);
            logStep(tenantId, "PROVISION_FAIL", "FAILED", e.getMessage(), 0);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void provisionInternal(Long tenantId, String initialAdminUsername, String initialAdminPassword) {
        long t0 = System.currentTimeMillis();
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) throw new IllegalStateException("租户不存在: " + tenantId);

        // Step: VALIDATE
        if (!StringUtils.hasText(tenant.getTenantCode())) {
            throw new IllegalStateException("租户编码为空");
        }
        SysTenant existing = tenantMapper.selectByTenantCode(tenant.getTenantCode());
        if (existing != null && !existing.getId().equals(tenantId)) {
            throw new IllegalStateException("租户编码重复: " + tenant.getTenantCode());
        }
        logStep(tenantId, "VALIDATE_CODE", "SUCCESS", null, System.currentTimeMillis() - t0);

        // Step: CLONE_MENUS —— 由于 sys_menu 在 ignoreTable 名单中，这里直接按原值复制 tenant_id=0 的行
        long t1 = System.currentTimeMillis();
        int menusCloned = jdbcTemplate.update(
                "INSERT INTO sys_menu (tenant_id, parent_id, menu_name, path, component, menu_type, permission, icon, sort_order, visible, is_template, created_by, created_time) "
                + "SELECT ?, parent_id, menu_name, path, component, menu_type, permission, icon, sort_order, visible, 0, 'system', NOW() "
                + "FROM sys_menu WHERE tenant_id = 0 AND deleted = 0",
                tenantId);
        logStep(tenantId, "CLONE_MENUS", "SUCCESS", "cloned=" + menusCloned, System.currentTimeMillis() - t1);

        // Step: CLONE_ROLES
        long t2 = System.currentTimeMillis();
        int rolesCloned = jdbcTemplate.update(
                "INSERT INTO sys_role (tenant_id, role_name, role_code, description, enabled, is_template, created_by, created_time) "
                + "SELECT ?, role_name, role_code, description, enabled, 0, 'system', NOW() "
                + "FROM sys_role WHERE tenant_id = 0 AND is_template = 1 AND deleted = 0",
                tenantId);
        logStep(tenantId, "CLONE_ROLES", "SUCCESS", "cloned=" + rolesCloned, System.currentTimeMillis() - t2);

        // Step: BIND_ADMIN_MENUS —— 租户内的 ADMIN 角色拥有租户自身的全部菜单
        long t3 = System.currentTimeMillis();
        int bound = jdbcTemplate.update(
                "INSERT IGNORE INTO sys_role_menu (tenant_id, role_id, menu_id) "
                + "SELECT ?, r.id, m.id FROM sys_role r "
                + "JOIN sys_menu m ON m.tenant_id = r.tenant_id AND m.deleted = 0 "
                + "WHERE r.tenant_id = ? AND r.role_code = 'ADMIN' AND r.deleted = 0",
                tenantId, tenantId);
        logStep(tenantId, "BIND_ADMIN_MENUS", "SUCCESS", "bound=" + bound, System.currentTimeMillis() - t3);

        // Step: CREATE_ADMIN —— 租户首个管理员
        long t4 = System.currentTimeMillis();
        Long adminUserId = TenantContextHolder.callAsOrThrow(tenantId, () ->
                sysUserService.bootstrapTenantAdmin(tenantId, initialAdminUsername, initialAdminPassword));
        logStep(tenantId, "CREATE_ADMIN", "SUCCESS", "userId=" + adminUserId, System.currentTimeMillis() - t4);

        // Step: INIT_QUOTA
        long t5 = System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT IGNORE INTO sys_tenant_quota_usage (tenant_id, metric, value_current, value_peak) VALUES "
                + "(?, 'USERS', 0, 0), (?, 'STORAGE_MB', 0, 0), (?, 'API_CALLS_DAILY', 0, 0)",
                tenantId, tenantId, tenantId);
        logStep(tenantId, "INIT_QUOTA", "SUCCESS", null, System.currentTimeMillis() - t5);

        // Step: ACTIVATE
        long t6 = System.currentTimeMillis();
        tenant.setStatus(1);
        tenant.setPrimaryAdminUserId(adminUserId);
        tenant.setUpdatedTime(LocalDateTime.now());
        tenantMapper.updateById(tenant);
        logStep(tenantId, "ACTIVATE", "SUCCESS", null, System.currentTimeMillis() - t6);

        log.info("[Provision] 租户 {} 初始化完成，adminUserId={}", tenant.getTenantCode(), adminUserId);
    }

    @Override
    public void register(String tenantCode, String tenantName, String contactName,
                         String contactEmail, String initialAdminUsername, String initialAdminPassword) {
        if (tenantMapper.selectByTenantCode(tenantCode) != null) {
            throw new DataIntegrityViolationException("租户编码已存在: " + tenantCode);
        }
        SysTenant tenant = new SysTenant();
        tenant.setTenantCode(tenantCode);
        tenant.setTenantName(tenantName);
        tenant.setStatus(2); // PROVISIONING
        tenant.setSchemaMode("POOL");
        tenant.setDataRegion("cn-east-1");
        tenant.setQuotaUsers(50);
        tenant.setQuotaStorageMb(10240L);
        tenant.setQuotaQps(50);
        tenant.setContactName(contactName);
        tenant.setContactEmail(contactEmail);
        tenant.setCreatedTime(LocalDateTime.now());
        tenant.setUpdatedTime(LocalDateTime.now());
        tenantMapper.insert(tenant);

        provisionAsync(tenant.getId(), initialAdminUsername, initialAdminPassword);
    }

    private void logStep(Long tenantId, String step, String status, String message, long elapsed) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO sys_tenant_provision_log (tenant_id, step, status, message, elapsed_ms, operator, created_time) VALUES (?, ?, ?, ?, ?, ?, NOW())",
                    tenantId, step, status, message, (int) Math.min(elapsed, Integer.MAX_VALUE), "system");
        } catch (Exception e) {
            log.warn("[Provision] 记录审计失败: tenantId={}, step={}, err={}", tenantId, step, e.getMessage());
        }
    }
}
