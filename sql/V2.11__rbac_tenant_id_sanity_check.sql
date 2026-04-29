-- ============================================================
-- V2.11  RBAC 关联表 tenant_id 复合主键 sanity check（只读断言）
--
-- 背景（M9-P3-02 / P3-03，由 mcp9 接盘）：
--   V2.02 已经给 sys_user_role / sys_role_menu 加了 tenant_id 列
--   并重建了 PRIMARY KEY (tenant_id, user_id, role_id) /
--   (tenant_id, role_id, menu_id)。
--   本脚本 **不再重复加列**，而是做 P3-03 冷启动 "Flyway-validate 等价" 断言：
--     若列 / 主键 / 数据契约不满足租户化要求，直接让 SQL 语句失败
--     阻断后续迁移，起 fail-closed 防御深度作用。
--
-- 为什么放在 V2.11：
--   - V2.06__add_is_template_to_menu_role.sql 已被占用（MariaDB 方言修复）；
--   - V2.07 / V2.09 为 tenant_id 回填脚本；
--   - V2.10__audit_log_payload.sql 已被 P3-12 任务占用；
--   - V2.11 是下一可用且语义相邻的版本号。
--
-- 实现手法：
--   采用 PREPARE + EXECUTE 动态 SQL。好处是 SQL 在准备阶段不会提前
--   解析"失败分支"，避免 MySQL 在 CASE 表达式里提前解析子查询表名。
--   断言失败时构造 `SELECT 1 FROM <故意不存在的表>`，EXECUTE 阶段
--   抛 ERROR 1146 `Table '...' doesn't exist`，错误信息含断言名。
--   断言成功时构造 `SELECT 'PASS' AS note`。
--
-- 幂等性：
--   - 只读断言。成功输出 'PASS'；失败直接 ERROR 1146。
--   - 多次执行完全等价，不改数据、不改结构。
--
-- 依赖：V1.11（建表）、V2.02（tenantize_rbac）、V2.06（is_template）
-- ============================================================

-- ------------------------------------------------------------
-- 1) sys_user_role：tenant_id 列存在
-- ------------------------------------------------------------
SET @col_ok := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_user_role'
     AND column_name  = 'tenant_id'
);
SET @chk := IF(@col_ok = 0,
  'SELECT 1 FROM __SANITY_FAIL__sys_user_role_tenant_id_column_missing',
  'SELECT ''PASS: sys_user_role.tenant_id column exists'' AS sanity_1'
);
PREPARE s FROM @chk; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 2) sys_user_role：PRIMARY KEY 必须含 tenant_id
-- ------------------------------------------------------------
SET @pk_ok := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_user_role'
     AND index_name   = 'PRIMARY'
     AND column_name  = 'tenant_id'
);
SET @chk := IF(@pk_ok = 0,
  'SELECT 1 FROM __SANITY_FAIL__sys_user_role_pk_missing_tenant_id',
  'SELECT ''PASS: sys_user_role PRIMARY KEY includes tenant_id'' AS sanity_2'
);
PREPARE s FROM @chk; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 3) sys_role_menu：tenant_id 列存在
-- ------------------------------------------------------------
SET @col_ok := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_role_menu'
     AND column_name  = 'tenant_id'
);
SET @chk := IF(@col_ok = 0,
  'SELECT 1 FROM __SANITY_FAIL__sys_role_menu_tenant_id_column_missing',
  'SELECT ''PASS: sys_role_menu.tenant_id column exists'' AS sanity_3'
);
PREPARE s FROM @chk; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 4) sys_role_menu：PRIMARY KEY 必须含 tenant_id
-- ------------------------------------------------------------
SET @pk_ok := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_role_menu'
     AND index_name   = 'PRIMARY'
     AND column_name  = 'tenant_id'
);
SET @chk := IF(@pk_ok = 0,
  'SELECT 1 FROM __SANITY_FAIL__sys_role_menu_pk_missing_tenant_id',
  'SELECT ''PASS: sys_role_menu PRIMARY KEY includes tenant_id'' AS sanity_4'
);
PREPARE s FROM @chk; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 5) sys_user_role：不应存在 tenant_id IS NULL 的脏行
-- ------------------------------------------------------------
SET @dirty := (SELECT COUNT(*) FROM sys_user_role WHERE tenant_id IS NULL);
SET @chk := IF(@dirty > 0,
  'SELECT 1 FROM __SANITY_FAIL__sys_user_role_has_null_tenant_id',
  'SELECT ''PASS: sys_user_role has no NULL tenant_id rows'' AS sanity_5'
);
PREPARE s FROM @chk; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 6) sys_role_menu：不应存在 tenant_id IS NULL 的脏行
-- ------------------------------------------------------------
SET @dirty := (SELECT COUNT(*) FROM sys_role_menu WHERE tenant_id IS NULL);
SET @chk := IF(@dirty > 0,
  'SELECT 1 FROM __SANITY_FAIL__sys_role_menu_has_null_tenant_id',
  'SELECT ''PASS: sys_role_menu has no NULL tenant_id rows'' AS sanity_6'
);
PREPARE s FROM @chk; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 最终横幅
-- ------------------------------------------------------------
SELECT 'V2.11 sanity check PASSED: sys_user_role / sys_role_menu tenant_id + composite PK OK' AS result;
