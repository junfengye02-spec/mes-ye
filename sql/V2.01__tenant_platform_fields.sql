-- ============================================================
-- V2.01  多租户生产级字段扩展（配合 MES多租户生产级设计方案.md）
-- 依赖：V1.14 已创建 sys_tenant 基础字段
--
-- 兼容性（P0 修复 R1，由 mcp30 接盘自 mcp26）：
--   原版本用一条 ALTER TABLE 批量 `ADD COLUMN IF NOT EXISTS`（13 列），
--   此语法是 MariaDB 方言，在纯 MySQL 8.0.x 上 ERROR 1064（mcp9-v2 复验）。
--   本次改为 INFORMATION_SCHEMA + PREPARE/EXECUTE 动态 SQL 逐列处理，
--   兼容 MySQL 8.0.12+ 所有发行版；连续执行两次幂等。
-- ============================================================

-- =========== 1) schema_mode ============
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_tenant'
     AND column_name  = 'schema_mode'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN schema_mode VARCHAR(16) NOT NULL DEFAULT ''POOL'' COMMENT ''隔离模式：POOL=共享库共享Schema；SCHEMA=独立Schema；DB=独立实例'' AFTER status',
  'SELECT ''sys_tenant.schema_mode already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =========== 2) data_region ============
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_tenant'
     AND column_name  = 'data_region'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN data_region VARCHAR(32) NOT NULL DEFAULT ''cn-east-1'' COMMENT ''数据归属区域，用于标识部署域'' AFTER schema_mode',
  'SELECT ''sys_tenant.data_region already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =========== 3) plan_id ============
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_tenant'
     AND column_name  = 'plan_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN plan_id BIGINT DEFAULT NULL COMMENT ''订阅计划ID（关联未来 sys_plan）'' AFTER data_region',
  'SELECT ''sys_tenant.plan_id already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =========== 4) quota_users ============
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_tenant'
     AND column_name  = 'quota_users'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN quota_users INT NOT NULL DEFAULT 50 COMMENT ''用户数上限'' AFTER plan_id',
  'SELECT ''sys_tenant.quota_users already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =========== 5) quota_storage_mb ============
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_tenant'
     AND column_name  = 'quota_storage_mb'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN quota_storage_mb BIGINT NOT NULL DEFAULT 10240 COMMENT ''文件存储上限（MB）'' AFTER quota_users',
  'SELECT ''sys_tenant.quota_storage_mb already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =========== 6) quota_qps ============
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_tenant'
     AND column_name  = 'quota_qps'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN quota_qps INT NOT NULL DEFAULT 50 COMMENT ''接口 QPS 上限（网关限流参考值）'' AFTER quota_storage_mb',
  'SELECT ''sys_tenant.quota_qps already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =========== 7) expire_at ============
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_tenant'
     AND column_name  = 'expire_at'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN expire_at DATETIME DEFAULT NULL COMMENT ''订阅到期时间（NULL=永久）'' AFTER quota_qps',
  'SELECT ''sys_tenant.expire_at already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =========== 8) contact_name ============
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_tenant'
     AND column_name  = 'contact_name'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN contact_name VARCHAR(100) DEFAULT NULL COMMENT ''租户主要联系人'' AFTER expire_at',
  'SELECT ''sys_tenant.contact_name already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =========== 9) contact_phone ============
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_tenant'
     AND column_name  = 'contact_phone'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN contact_phone VARCHAR(32) DEFAULT NULL COMMENT ''租户主要联系人手机'' AFTER contact_name',
  'SELECT ''sys_tenant.contact_phone already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =========== 10) contact_email ============
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_tenant'
     AND column_name  = 'contact_email'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN contact_email VARCHAR(128) DEFAULT NULL COMMENT ''租户主要联系人邮箱'' AFTER contact_phone',
  'SELECT ''sys_tenant.contact_email already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =========== 11) primary_admin_user_id ============
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_tenant'
     AND column_name  = 'primary_admin_user_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN primary_admin_user_id BIGINT DEFAULT NULL COMMENT ''租户内首个管理员用户 ID'' AFTER contact_email',
  'SELECT ''sys_tenant.primary_admin_user_id already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =========== 12) security_policy_json ============
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_tenant'
     AND column_name  = 'security_policy_json'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN security_policy_json JSON DEFAULT NULL COMMENT ''租户级安全策略：MFA、密码复杂度、IP 白名单等'' AFTER primary_admin_user_id',
  'SELECT ''sys_tenant.security_policy_json already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =========== 13) updated_time ============
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_tenant'
     AND column_name  = 'updated_time'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''最后更新时间'' AFTER created_time',
  'SELECT ''sys_tenant.updated_time already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =========== 14) deleted ============
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_tenant'
     AND column_name  = 'deleted'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN deleted INT NOT NULL DEFAULT 0 COMMENT ''软删标志（0=正常 1=已删除）'' AFTER updated_time',
  'SELECT ''sys_tenant.deleted already exists, skip'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- status 含义扩展：
--   0=PENDING（待审批）
--   1=ACTIVE（正常）
--   2=PROVISIONING（初始化中）
--   3=SUSPENDED（暂停）
--   4=ARCHIVED（归档只读）
-- 原始存量数据 status=1 默认视为 ACTIVE；WHERE 本身幂等（重复执行 0 行命中）。
UPDATE sys_tenant SET status = 1 WHERE status IS NULL;

-- 平台超管专属租户（id = 0），参与 RBAC 但不参与业务表隔离：
-- 业务侧 tenant_id = 0 的数据被视为平台级，MybatisPlusConfig 自动将该值作为
-- 默认过滤条件传入，不会影响普通租户的查询。
INSERT INTO sys_tenant (id, tenant_code, tenant_name, status, schema_mode, data_region, quota_users, quota_storage_mb, quota_qps, created_time)
VALUES (0, 'PLATFORM', '平台（超管）', 1, 'POOL', 'cn-east-1', 9999, 0, 999999, NOW())
ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name);

-- 默认租户 id=1 显式补齐配额（重复执行幂等：UPDATE 同值）
UPDATE sys_tenant SET quota_users = 200, quota_storage_mb = 51200, quota_qps = 200 WHERE id = 1;
