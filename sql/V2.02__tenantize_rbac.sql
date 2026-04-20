-- ============================================================
-- V2.02  RBAC 表租户化 + 用户名按租户唯一
--   - sys_role / sys_menu / sys_user_role / sys_role_menu 加 tenant_id
--   - 把 tenant_id = 0 的 sys_menu / sys_role 记录视为"平台模板"，供新租户克隆
--   - sys_user 的 uk_username 改为 (tenant_id, username)
-- 依赖：V1.11、V1.14、V2.01 已执行
-- ============================================================

-- 1) sys_role ----------------------------------------------------
ALTER TABLE sys_role
  ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID；0=平台模板' AFTER id,
  ADD COLUMN IF NOT EXISTS is_template TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=平台模板，新租户克隆入库' AFTER enabled;

-- 原来是全局 (role_code) 唯一；现在改为 (tenant_id, role_code)
ALTER TABLE sys_role DROP INDEX uk_role_code;
ALTER TABLE sys_role ADD UNIQUE KEY uk_role_tenant_code (tenant_id, role_code);
ALTER TABLE sys_role ADD INDEX idx_role_tenant (tenant_id);

-- 把现有系统角色标记为平台模板，保留在 tenant_id=0 下
UPDATE sys_role SET tenant_id = 0, is_template = 1
WHERE role_code IN ('ADMIN', 'PRODUCTION_MANAGER', 'QUALITY_MANAGER', 'OPERATOR');

-- 2) sys_menu ----------------------------------------------------
ALTER TABLE sys_menu
  ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID；0=平台模板' AFTER id,
  ADD COLUMN IF NOT EXISTS is_template TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=平台模板' AFTER visible;

ALTER TABLE sys_menu ADD INDEX idx_menu_tenant (tenant_id);

-- 现存菜单默认全部为平台模板（tenant_id=0, is_template=1）
UPDATE sys_menu SET tenant_id = 0, is_template = 1 WHERE tenant_id IS NULL OR tenant_id = 1;

-- 3) sys_user_role ----------------------------------------------
ALTER TABLE sys_user_role
  ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID（冗余，防挂错租户）' FIRST;

ALTER TABLE sys_user_role DROP PRIMARY KEY;
ALTER TABLE sys_user_role
  ADD PRIMARY KEY (tenant_id, user_id, role_id);
ALTER TABLE sys_user_role ADD INDEX idx_ur_user (tenant_id, user_id);
ALTER TABLE sys_user_role ADD INDEX idx_ur_role (tenant_id, role_id);

-- 4) sys_role_menu ----------------------------------------------
ALTER TABLE sys_role_menu
  ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID（0 表示平台模板角色的菜单绑定）' FIRST;

ALTER TABLE sys_role_menu DROP PRIMARY KEY;
ALTER TABLE sys_role_menu
  ADD PRIMARY KEY (tenant_id, role_id, menu_id);
ALTER TABLE sys_role_menu ADD INDEX idx_rm_role (tenant_id, role_id);
ALTER TABLE sys_role_menu ADD INDEX idx_rm_menu (tenant_id, menu_id);

-- 5) sys_user：username 按租户唯一 ------------------------------
-- 为避免在切换唯一键期间冲突，先 drop 再 add。
ALTER TABLE sys_user DROP INDEX uk_username;
ALTER TABLE sys_user ADD UNIQUE KEY uk_user_tenant_username (tenant_id, username);
ALTER TABLE sys_user ADD INDEX idx_user_tenant (tenant_id);

-- 6) 初始化"默认租户"的 RBAC（克隆平台模板到 tenant_id=1）-------
-- 角色模板克隆
INSERT INTO sys_role (tenant_id, role_name, role_code, description, enabled, is_template, created_by)
SELECT 1 AS tenant_id, role_name, role_code, description, enabled, 0 AS is_template, 'system'
FROM sys_role WHERE tenant_id = 0 AND is_template = 1
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 原 user_role 数据补 tenant_id=1
UPDATE sys_user_role SET tenant_id = 1 WHERE tenant_id = 0 OR tenant_id IS NULL;

-- 把默认租户管理员 (admin) 的 user_role 重指向"租户 1 内的 ADMIN 角色"
-- 先查 tenant=1 下的 ADMIN role id，回填到 sys_user_role。
UPDATE sys_user_role ur
JOIN sys_user u ON u.id = ur.user_id
JOIN sys_role r ON r.tenant_id = u.tenant_id AND r.role_code = 'ADMIN'
SET ur.role_id = r.id, ur.tenant_id = u.tenant_id
WHERE u.username = 'admin';

-- 7) 默认租户下的 sys_role_menu：让租户 1 的 ADMIN 角色拥有全部平台模板菜单
INSERT INTO sys_role_menu (tenant_id, role_id, menu_id)
SELECT 1 AS tenant_id, r.id AS role_id, m.id AS menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.tenant_id = 1 AND r.role_code = 'ADMIN'
  AND m.tenant_id = 0 AND m.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
