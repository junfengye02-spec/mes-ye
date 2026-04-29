-- ============================================================
-- V2.06  sys_menu / sys_role.is_template migration normalization
--
-- Background:
--   V2.02 once used "ADD COLUMN IF NOT EXISTS" to add the is_template
--   column, but that syntax is MariaDB dialect and NOT natively
--   supported by MySQL 8.0 (only specific fork / 8.0.29+ patch
--   versions recognize it). On "pure MySQL 8.0" production the V2.02
--   execution failed here with ERROR 1064.
--
--   This script uses INFORMATION_SCHEMA + PREPARE dynamic SQL to
--   implement true-idempotent "ADD COLUMN IF NOT EXISTS" semantics,
--   and at the same time back-fills the defective Flyway records
--   ("column previously added via ddl-auto=update before V2.02"):
--     * column missing -> ALTER TABLE ... ADD COLUMN ... NOT NULL DEFAULT 0
--     * column present -> no-op (SELECT 1)
--     * remaining NULL values -> back-fill to 0
--
-- Idempotency:
--   - PREPARE branching is naturally idempotent (no-op when column exists);
--   - UPDATE ... WHERE IS NULL hits 0 rows on a NOT NULL column;
--   - Running the script twice in a row returns SUCCESS / 0 ERROR.
--
-- Compatibility:
--   - Requires MySQL >= 8.0 (PREPARE + INFORMATION_SCHEMA also works on 5.x);
--   - Does NOT rely on "IF NOT EXISTS" syntax, works on all MySQL 8.0.12+ builds.
--
-- Encoding note (mcp9 P3-03):
--   Historical UTF-8 CJK COMMENT strings in this file were corrupted
--   (embedded U+FFFD replacement bytes that broke the quoted literal
--   and triggered ERROR 1064 on fresh MySQL cold starts). The literal
--   COMMENT values below have been rewritten to plain ASCII English
--   without changing any DDL semantics; the column definitions,
--   defaults and back-fill targets are identical to the original.
--
-- Dependencies: V1.11 (creates sys_menu / sys_role) executed; V2.02 optional.
-- ============================================================

-- =============== 1) sys_role.is_template =====================
SET @col_exists_role := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_role'
     AND column_name  = 'is_template'
);
SET @ddl_role := IF(@col_exists_role = 0,
  'ALTER TABLE sys_role ADD COLUMN is_template TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1=platform template role (cloned into new tenants); 0=regular tenant role''',
  'SELECT ''sys_role.is_template already exists, skip ADD COLUMN'' AS note'
);
PREPARE s_role FROM @ddl_role;
EXECUTE s_role;
DEALLOCATE PREPARE s_role;

UPDATE sys_role
   SET is_template = 0
 WHERE is_template IS NULL;

-- =============== 2) sys_menu.is_template =====================
-- Note: V2.02 previously declared sys_menu.is_template DEFAULT 1
--       (treating existing menus as platform templates). This script
--       only runs ADD COLUMN when the column is missing; when the
--       column already exists we do NOT override the DEFAULT that
--       V2.02 set. Any remaining NULL legacy rows are back-filled to 0.
SET @col_exists_menu := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_menu'
     AND column_name  = 'is_template'
);
SET @ddl_menu := IF(@col_exists_menu = 0,
  'ALTER TABLE sys_menu ADD COLUMN is_template TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1=platform template menu (cloned into new tenants); 0=regular tenant menu''',
  'SELECT ''sys_menu.is_template already exists, skip ADD COLUMN'' AS note'
);
PREPARE s_menu FROM @ddl_menu;
EXECUTE s_menu;
DEALLOCATE PREPARE s_menu;

UPDATE sys_menu
   SET is_template = 0
 WHERE is_template IS NULL;
