-- ============================================================
-- V2.02  RBAC 表租户化 + 用户名按租户唯一
--   - sys_role / sys_menu / sys_user_role / sys_role_menu 加 tenant_id
--   - 把 tenant_id = 0 的 sys_menu / sys_role 记录视为"平台模板"，供新租户克隆
--   - sys_user 的 uk_username 改为 (tenant_id, username)
-- 依赖：V1.11、V1.14、V2.01 已执行
--
-- 兼容性（P0 修复 R1，由 mcp30 接盘自 mcp26）：
--   原版本混用 `ADD COLUMN IF NOT EXISTS` / 无条件 `DROP INDEX` / 无条件 `ALTER ... ADD`，
--   其中 IF NOT EXISTS 是 MariaDB 方言 MySQL 8.0.x 报 ERROR 1064；
--   无条件 DROP INDEX / DROP PRIMARY KEY 在第二次运行时也会报错。
--   本次全部改为 INFORMATION_SCHEMA + PREPARE/EXECUTE 动态 SQL，
--   幂等判定以 "tenant_id 列是否存在" 作为"是否需要重建 PK/UK" 的触发器：
--     - 第一次执行：tenant_id 列不存在 → 加列 → DROP 原 PK/UK → ADD 新 PK/UK/INDEX
--     - 第二次执行：tenant_id 列已存在 → 跳过加列 → 跳过 DROP/ADD PK → 索引 IF NOT EXISTS
--   连续执行两次均 SUCCESS、0 ERROR。兼容 MySQL 8.0.12+ 所有发行版。
-- ============================================================

-- =======================================================================
-- 1) sys_role ------------------------------------------------------------
-- =======================================================================

-- 1.1 sys_role.tenant_id
SET @role_tenant_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_role'
     AND column_name  = 'tenant_id'
);
SET @ddl := IF(@role_tenant_exists = 0,
  'ALTER TABLE sys_role ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT ''租户ID；0=平台模板'' AFTER id',
  'SELECT ''sys_role.tenant_id already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.2 sys_role.is_template
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_role'
     AND column_name  = 'is_template'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_role ADD COLUMN is_template TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1=平台模板，新租户克隆入库'' AFTER enabled',
  'SELECT ''sys_role.is_template already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.3 把全局 uk_role_code 改为 (tenant_id, role_code)
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_role'
     AND index_name   = 'uk_role_code'
);
SET @ddl := IF(@idx_exists > 0,
  'ALTER TABLE sys_role DROP INDEX uk_role_code',
  'SELECT ''sys_role.uk_role_code already dropped, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_role'
     AND index_name   = 'uk_role_tenant_code'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE sys_role ADD UNIQUE KEY uk_role_tenant_code (tenant_id, role_code)',
  'SELECT ''sys_role.uk_role_tenant_code already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_role'
     AND index_name   = 'idx_role_tenant'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE sys_role ADD INDEX idx_role_tenant (tenant_id)',
  'SELECT ''sys_role.idx_role_tenant already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.4 把现有系统角色标记为平台模板，保留在 tenant_id=0 下（UPDATE 幂等）
UPDATE sys_role SET tenant_id = 0, is_template = 1
WHERE role_code IN ('ADMIN', 'PRODUCTION_MANAGER', 'QUALITY_MANAGER', 'OPERATOR');

-- =======================================================================
-- 2) sys_menu ------------------------------------------------------------
-- =======================================================================

-- 2.1 sys_menu.tenant_id
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_menu'
     AND column_name  = 'tenant_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_menu ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT ''租户ID；0=平台模板'' AFTER id',
  'SELECT ''sys_menu.tenant_id already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 2.2 sys_menu.is_template（注意 V2.02 用 DEFAULT 1；V2.06 在列已存在时是 no-op，不覆盖）
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_menu'
     AND column_name  = 'is_template'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_menu ADD COLUMN is_template TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''1=平台模板'' AFTER visible',
  'SELECT ''sys_menu.is_template already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 2.3 idx_menu_tenant
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_menu'
     AND index_name   = 'idx_menu_tenant'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE sys_menu ADD INDEX idx_menu_tenant (tenant_id)',
  'SELECT ''sys_menu.idx_menu_tenant already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 2.4 现存菜单默认全部为平台模板（tenant_id=0, is_template=1）
--     WHERE 条件在二次执行时命中 0 行，幂等
UPDATE sys_menu SET tenant_id = 0, is_template = 1 WHERE tenant_id IS NULL OR tenant_id = 1;

-- =======================================================================
-- 3) sys_user_role -------------------------------------------------------
--    重建主键为 (tenant_id, user_id, role_id)
-- =======================================================================

-- 3.1 tenant_id 列；同时记录"是否是首次加列"用于后续 PK 重建判定
SET @user_role_needs_pk_rebuild := (
  SELECT IF(COUNT(*) = 0, 1, 0) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_user_role'
     AND column_name  = 'tenant_id'
);
SET @ddl := IF(@user_role_needs_pk_rebuild = 1,
  'ALTER TABLE sys_user_role ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID（冗余，防挂错租户）'' FIRST',
  'SELECT ''sys_user_role.tenant_id already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 3.2 DROP 原 PK（仅在首次加列时才执行；二次执行时新 PK 已含 tenant_id）
SET @ddl := IF(@user_role_needs_pk_rebuild = 1,
  'ALTER TABLE sys_user_role DROP PRIMARY KEY',
  'SELECT ''sys_user_role primary key already rebuilt, skip DROP'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 3.3 ADD 新 PK
SET @ddl := IF(@user_role_needs_pk_rebuild = 1,
  'ALTER TABLE sys_user_role ADD PRIMARY KEY (tenant_id, user_id, role_id)',
  'SELECT ''sys_user_role primary key already rebuilt, skip ADD'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 3.4 idx_ur_user
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_user_role'
     AND index_name   = 'idx_ur_user'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE sys_user_role ADD INDEX idx_ur_user (tenant_id, user_id)',
  'SELECT ''sys_user_role.idx_ur_user already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 3.5 idx_ur_role
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_user_role'
     AND index_name   = 'idx_ur_role'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE sys_user_role ADD INDEX idx_ur_role (tenant_id, role_id)',
  'SELECT ''sys_user_role.idx_ur_role already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =======================================================================
-- 4) sys_role_menu -------------------------------------------------------
--    重建主键为 (tenant_id, role_id, menu_id)
-- =======================================================================

-- 4.1 tenant_id 列
SET @role_menu_needs_pk_rebuild := (
  SELECT IF(COUNT(*) = 0, 1, 0) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_role_menu'
     AND column_name  = 'tenant_id'
);
SET @ddl := IF(@role_menu_needs_pk_rebuild = 1,
  'ALTER TABLE sys_role_menu ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT ''租户ID（0 表示平台模板角色的菜单绑定）'' FIRST',
  'SELECT ''sys_role_menu.tenant_id already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 4.2 DROP 原 PK
SET @ddl := IF(@role_menu_needs_pk_rebuild = 1,
  'ALTER TABLE sys_role_menu DROP PRIMARY KEY',
  'SELECT ''sys_role_menu primary key already rebuilt, skip DROP'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 4.3 ADD 新 PK
SET @ddl := IF(@role_menu_needs_pk_rebuild = 1,
  'ALTER TABLE sys_role_menu ADD PRIMARY KEY (tenant_id, role_id, menu_id)',
  'SELECT ''sys_role_menu primary key already rebuilt, skip ADD'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 4.4 idx_rm_role
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_role_menu'
     AND index_name   = 'idx_rm_role'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE sys_role_menu ADD INDEX idx_rm_role (tenant_id, role_id)',
  'SELECT ''sys_role_menu.idx_rm_role already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 4.5 idx_rm_menu
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_role_menu'
     AND index_name   = 'idx_rm_menu'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE sys_role_menu ADD INDEX idx_rm_menu (tenant_id, menu_id)',
  'SELECT ''sys_role_menu.idx_rm_menu already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =======================================================================
-- 5) sys_user：username 按租户唯一 --------------------------------------
-- =======================================================================
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_user'
     AND index_name   = 'uk_username'
);
SET @ddl := IF(@idx_exists > 0,
  'ALTER TABLE sys_user DROP INDEX uk_username',
  'SELECT ''sys_user.uk_username already dropped, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_user'
     AND index_name   = 'uk_user_tenant_username'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE sys_user ADD UNIQUE KEY uk_user_tenant_username (tenant_id, username)',
  'SELECT ''sys_user.uk_user_tenant_username already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_user'
     AND index_name   = 'idx_user_tenant'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE sys_user ADD INDEX idx_user_tenant (tenant_id)',
  'SELECT ''sys_user.idx_user_tenant already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =======================================================================
-- 6) 初始化"默认租户"的 RBAC（克隆平台模板到 tenant_id=1）--------------
-- =======================================================================
-- 角色模板克隆（ON DUPLICATE KEY UPDATE 幂等）
INSERT INTO sys_role (tenant_id, role_name, role_code, description, enabled, is_template, created_by)
SELECT 1 AS tenant_id, role_name, role_code, description, enabled, 0 AS is_template, 'system'
FROM sys_role WHERE tenant_id = 0 AND is_template = 1
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 原 user_role 数据补 tenant_id=1（重复执行命中 0 行）
UPDATE sys_user_role SET tenant_id = 1 WHERE tenant_id = 0 OR tenant_id IS NULL;

-- 把默认租户管理员 (admin) 的 user_role 重指向"租户 1 内的 ADMIN 角色"
-- 先查 tenant=1 下的 ADMIN role id，回填到 sys_user_role。
-- 重复执行时 role_id 已经是 tenant=1 下的 ADMIN id，赋值同值幂等。
UPDATE sys_user_role ur
JOIN sys_user u ON u.id = ur.user_id
JOIN sys_role r ON r.tenant_id = u.tenant_id AND r.role_code = 'ADMIN'
SET ur.role_id = r.id, ur.tenant_id = u.tenant_id
WHERE u.username = 'admin';

-- 7) 默认租户下的 sys_role_menu：让租户 1 的 ADMIN 角色拥有全部平台模板菜单
--    ON DUPLICATE KEY UPDATE 幂等
INSERT INTO sys_role_menu (tenant_id, role_id, menu_id)
SELECT 1 AS tenant_id, r.id AS role_id, m.id AS menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.tenant_id = 1 AND r.role_code = 'ADMIN'
  AND m.tenant_id = 0 AND m.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
