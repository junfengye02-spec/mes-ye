-- ============================================================
-- 多租户 + 管理端/现场端账号类型
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_tenant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_code VARCHAR(50) NOT NULL COMMENT '租户编码',
  tenant_name VARCHAR(100) NOT NULL COMMENT '租户名称',
  status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=启用',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_code (tenant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户';

INSERT INTO sys_tenant (id, tenant_code, tenant_name, status) VALUES
(1, 'default', '默认租户', 1),
(2, 'east', '华东二厂', 1)
ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name);

ALTER TABLE sys_user
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER factory_code,
  ADD COLUMN account_type VARCHAR(20) NOT NULL DEFAULT 'ADMIN' COMMENT 'ADMIN=可登录管理端, STAFF=仅现场端' AFTER tenant_id;

CREATE INDEX idx_sys_user_tenant ON sys_user(tenant_id);

UPDATE sys_user SET account_type = 'ADMIN', tenant_id = 1 WHERE username = 'admin';
UPDATE sys_user SET account_type = 'STAFF', tenant_id = 1
  WHERE username IN ('zhangsan', 'lisi', 'wangwu', 'zhaoliu');
