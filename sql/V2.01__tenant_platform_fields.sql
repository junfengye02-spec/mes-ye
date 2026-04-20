-- ============================================================
-- V2.01  多租户生产级字段扩展（配合 MES多租户生产级设计方案.md）
-- 依赖：V1.14 已创建 sys_tenant 基础字段
-- ============================================================

ALTER TABLE sys_tenant
  ADD COLUMN IF NOT EXISTS schema_mode VARCHAR(16) NOT NULL DEFAULT 'POOL'
             COMMENT '隔离模式：POOL=共享库共享Schema；SCHEMA=独立Schema；DB=独立实例' AFTER status,
  ADD COLUMN IF NOT EXISTS data_region VARCHAR(32) NOT NULL DEFAULT 'cn-east-1'
             COMMENT '数据归属区域，用于标识部署域' AFTER schema_mode,
  ADD COLUMN IF NOT EXISTS plan_id BIGINT DEFAULT NULL
             COMMENT '订阅计划ID（关联未来 sys_plan）' AFTER data_region,
  ADD COLUMN IF NOT EXISTS quota_users INT NOT NULL DEFAULT 50
             COMMENT '用户数上限' AFTER plan_id,
  ADD COLUMN IF NOT EXISTS quota_storage_mb BIGINT NOT NULL DEFAULT 10240
             COMMENT '文件存储上限（MB）' AFTER quota_users,
  ADD COLUMN IF NOT EXISTS quota_qps INT NOT NULL DEFAULT 50
             COMMENT '接口 QPS 上限（网关限流参考值）' AFTER quota_storage_mb,
  ADD COLUMN IF NOT EXISTS expire_at DATETIME DEFAULT NULL
             COMMENT '订阅到期时间（NULL=永久）' AFTER quota_qps,
  ADD COLUMN IF NOT EXISTS contact_name VARCHAR(100) DEFAULT NULL
             COMMENT '租户主要联系人' AFTER expire_at,
  ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(32) DEFAULT NULL
             COMMENT '租户主要联系人手机' AFTER contact_name,
  ADD COLUMN IF NOT EXISTS contact_email VARCHAR(128) DEFAULT NULL
             COMMENT '租户主要联系人邮箱' AFTER contact_phone,
  ADD COLUMN IF NOT EXISTS primary_admin_user_id BIGINT DEFAULT NULL
             COMMENT '租户内首个管理员用户 ID' AFTER contact_email,
  ADD COLUMN IF NOT EXISTS security_policy_json JSON DEFAULT NULL
             COMMENT '租户级安全策略：MFA、密码复杂度、IP 白名单等' AFTER primary_admin_user_id,
  ADD COLUMN IF NOT EXISTS updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
             COMMENT '最后更新时间' AFTER created_time,
  ADD COLUMN IF NOT EXISTS deleted INT NOT NULL DEFAULT 0
             COMMENT '软删标志（0=正常 1=已删除）' AFTER updated_time;

-- status 含义扩展：
--   0=PENDING（待审批）
--   1=ACTIVE（正常）
--   2=PROVISIONING（初始化中）
--   3=SUSPENDED（暂停）
--   4=ARCHIVED（归档只读）
-- 原始存量数据 status=1 默认视为 ACTIVE。
UPDATE sys_tenant SET status = 1 WHERE status IS NULL;

-- 平台超管专属租户（id = 0），参与 RBAC 但不参与业务表隔离：
-- 业务侧 tenant_id = 0 的数据被视为平台级，MybatisPlusConfig 自动将该值作为
-- 默认过滤条件传入，不会影响普通租户的查询。
INSERT INTO sys_tenant (id, tenant_code, tenant_name, status, schema_mode, data_region, quota_users, quota_storage_mb, quota_qps, created_time)
VALUES (0, 'PLATFORM', '平台（超管）', 1, 'POOL', 'cn-east-1', 9999, 0, 999999, NOW())
ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name);

-- 默认租户 id=1 显式补齐配额
UPDATE sys_tenant SET quota_users = 200, quota_storage_mb = 51200, quota_qps = 200 WHERE id = 1;
