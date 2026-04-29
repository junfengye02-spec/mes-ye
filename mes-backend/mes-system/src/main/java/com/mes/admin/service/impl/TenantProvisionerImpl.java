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
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        // Step: CLONE_MENUS —— 由于 sys_menu 在 ignoreTable 名单中，跨租户 INSERT 被允许；
        // 注意：模板中既有目录/叶子菜单，又有 V2.05 引入的大量按钮级菜单（menu_type='B'），
        //      这些菜单通过 parent_id 形成树结构。直接 INSERT...SELECT 会把模板的原始 id
        //      作为新租户的 parent_id，导致新租户菜单树指向错误 —— 这里采用"按 parent 拓扑
        //      顺序逐条插入 + 旧新 id 映射回填"的方式，保证 parent_id 指向新租户自己的菜单。
        long t1 = System.currentTimeMillis();
        int menusCloned = cloneTemplateMenus(tenantId);
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

    /**
     * 按"父先于子"的拓扑顺序把模板菜单（tenant_id=0 且 is_template=1 或 deleted=0）
     * 克隆到目标租户，并重建 parent_id 指向新租户自己的菜单。
     *
     * <p>核心流程：</p>
     * <ol>
     *   <li>查询全部模板菜单（按 parent_id 升序 + sort_order 升序，保证父先于子被处理）；</li>
     *   <li>维护 {@code oldIdToNewId} 映射，逐条 INSERT 并通过 {@link KeyHolder} 抓取自增主键；</li>
     *   <li>INSERT 时：新菜单 parent_id = map.get(模板 parent_id) ?: 0，保证指向新租户自己的父菜单；</li>
     *   <li>V2.05 新增的按钮级菜单 (menu_type='B') 通过同一流程克隆，permission 字段被原样继承，
     *       供前端 v-auth 指令 / 后端 @PreAuthorize 使用。</li>
     * </ol>
     *
     * @param tenantId 目标租户 id
     * @return 被克隆的菜单行数
     */
    private int cloneTemplateMenus(Long tenantId) {
        // 取全部模板菜单，按 parent_id 升序保证父先于子被处理
        List<Map<String, Object>> templateRows = jdbcTemplate.queryForList(
                "SELECT id, parent_id, menu_name, path, component, menu_type, permission, icon, sort_order, visible "
                        + "FROM sys_menu WHERE tenant_id = 0 AND deleted = 0 "
                        + "ORDER BY parent_id ASC, sort_order ASC, id ASC");
        if (templateRows.isEmpty()) {
            return 0;
        }

        // 旧菜单 id -> 新菜单 id 的映射；用 LinkedHashMap 便于观察
        Map<Long, Long> oldIdToNewId = new LinkedHashMap<>(templateRows.size() * 2);
        // 兜底：pending 行（父菜单尚未在本次克隆批次里完成）缓存，避免排序异常时丢数据
        Map<Long, Map<String, Object>> pendingRows = new HashMap<>();

        String insertSql = "INSERT INTO sys_menu "
                + "(tenant_id, parent_id, menu_name, path, component, menu_type, permission, icon, sort_order, visible, is_template, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 'system', NOW())";

        int cloned = 0;
        for (Map<String, Object> row : templateRows) {
            Long oldId = toLong(row.get("id"));
            Long oldParentId = toLong(row.get("parent_id"));
            Long newParentId = resolveNewParentId(oldParentId, oldIdToNewId);

            Long newId = insertMenuRow(insertSql, tenantId, newParentId, row);
            oldIdToNewId.put(oldId, newId);
            cloned++;

            // 如果有"等待当前父"的子行，理论上按排序逻辑不会触发；仍做兜底扫描
            retryPending(insertSql, tenantId, pendingRows, oldIdToNewId);
        }
        return cloned;
    }

    /**
     * 把模板 parent_id 翻译为新租户 parent_id：
     * <ul>
     *   <li>模板顶级 (parent_id = 0 或 null) → 新租户也保持 0</li>
     *   <li>模板已克隆 → 映射到新 id</li>
     *   <li>未找到（说明父尚未处理）→ 兜底 0，实际极端情况下会被 retryPending 修正</li>
     * </ul>
     *
     * @param oldParentId 模板里的 parent_id
     * @param oldIdToNewId 当前已建立的映射
     * @return 新租户 parent_id
     */
    private Long resolveNewParentId(Long oldParentId, Map<Long, Long> oldIdToNewId) {
        if (oldParentId == null || oldParentId == 0L) {
            return 0L;
        }
        Long mapped = oldIdToNewId.get(oldParentId);
        return mapped != null ? mapped : 0L;
    }

    /**
     * 对 pendingRows 做一次扫描：若其 oldParentId 已经出现在 oldIdToNewId 中，则补插入并登记映射。
     *
     * @param insertSql 新菜单 INSERT SQL
     * @param tenantId 目标租户
     * @param pendingRows 待处理行
     * @param oldIdToNewId 旧新 id 映射
     */
    private void retryPending(String insertSql, Long tenantId,
                              Map<Long, Map<String, Object>> pendingRows,
                              Map<Long, Long> oldIdToNewId) {
        if (pendingRows.isEmpty()) {
            return;
        }
        pendingRows.entrySet().removeIf(entry -> {
            Map<String, Object> row = entry.getValue();
            Long oldParentId = toLong(row.get("parent_id"));
            if (oldParentId != null && oldIdToNewId.containsKey(oldParentId)) {
                Long newParentId = oldIdToNewId.get(oldParentId);
                Long newId = insertMenuRow(insertSql, tenantId, newParentId, row);
                oldIdToNewId.put(entry.getKey(), newId);
                return true;
            }
            return false;
        });
    }

    /**
     * 插入单行菜单并返回自增主键。
     *
     * @param insertSql 新菜单 INSERT SQL
     * @param tenantId 目标租户
     * @param newParentId 新租户内的父菜单 id（0 表示顶级）
     * @param row 模板行
     * @return 新菜单自增 id
     */
    private Long insertMenuRow(String insertSql, Long tenantId, Long newParentId, Map<String, Object> row) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, tenantId);
            ps.setLong(2, newParentId == null ? 0L : newParentId);
            ps.setString(3, (String) row.get("menu_name"));
            ps.setString(4, (String) row.get("path"));
            ps.setString(5, (String) row.get("component"));
            ps.setString(6, (String) row.get("menu_type"));
            ps.setString(7, (String) row.get("permission"));
            ps.setString(8, (String) row.get("icon"));
            Integer sortOrder = toInt(row.get("sort_order"));
            ps.setInt(9, sortOrder == null ? 0 : sortOrder);
            Integer visible = toInt(row.get("visible"));
            ps.setInt(10, visible == null ? 1 : visible);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("插入模板菜单后未能获取自增主键, tenantId=" + tenantId);
        }
        return key.longValue();
    }

    /**
     * 安全转换为 Long，兼容 JDBC 驱动可能返回的 Integer/BigInteger。
     *
     * @param val 原始值
     * @return Long；null 直接返回 null
     */
    private static Long toLong(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return Long.valueOf(val.toString());
    }

    /**
     * 安全转换为 Integer。
     *
     * @param val 原始值
     * @return Integer；null 直接返回 null
     */
    private static Integer toInt(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return Integer.valueOf(val.toString());
    }
}
