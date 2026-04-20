-- ============================================================
-- V2.03  租户生命周期基础表
--   - sys_tenant_provision_log：租户注册/初始化审计
--   - sys_tenant_quota_usage  ：用量快照（配合 @TenantQuota 切面）
--   - sys_audit_log           ：跨租户操作审计（平台超管、权限变更、导出、删除）
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_tenant_provision_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '关联 sys_tenant.id',
  step VARCHAR(64) NOT NULL COMMENT 'VALIDATE_CODE/INSERT_TENANT/CLONE_MENUS/CLONE_ROLES/CREATE_ADMIN/ACTIVATE/...',
  status VARCHAR(16) NOT NULL COMMENT 'SUCCESS / FAILED / RETRY',
  message VARCHAR(2000) COMMENT '日志 / 异常信息',
  elapsed_ms INT DEFAULT NULL COMMENT '本步骤耗时',
  operator VARCHAR(100) COMMENT '操作人（平台超管 / system）',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ptl_tenant (tenant_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户初始化审计日志';

CREATE TABLE IF NOT EXISTS sys_tenant_quota_usage (
  tenant_id BIGINT NOT NULL,
  metric VARCHAR(32) NOT NULL COMMENT 'USERS/STORAGE_MB/API_CALLS_DAILY/... ',
  value_current BIGINT NOT NULL DEFAULT 0,
  value_peak BIGINT NOT NULL DEFAULT 0 COMMENT '最近窗口峰值',
  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (tenant_id, metric)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户配额用量';

CREATE TABLE IF NOT EXISTS sys_audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT '发生事件的租户（跨租户操作时为目标租户；平台级为0）',
  operator_tenant_id BIGINT COMMENT '发起者所在租户（超管通常 0）',
  operator_user_id BIGINT COMMENT '操作人 ID',
  operator_username VARCHAR(100) COMMENT '操作人用户名（冗余，方便检索）',
  action VARCHAR(64) NOT NULL COMMENT 'LOGIN / LOGIN_FAIL / ROLE_CHANGE / DELETE / EXPORT / TENANT_SUSPEND ...',
  target_type VARCHAR(64) COMMENT '操作对象类型：USER / ROLE / WORK_ORDER ...',
  target_id VARCHAR(100) COMMENT '操作对象 ID（字符串，兼容复合主键）',
  trace_id VARCHAR(64) COMMENT '串链的请求追踪 ID',
  ip VARCHAR(64) COMMENT '来源 IP',
  user_agent VARCHAR(512) COMMENT '请求 UA',
  payload_json JSON COMMENT '关键上下文（变更前/后）',
  result VARCHAR(16) NOT NULL DEFAULT 'OK' COMMENT 'OK / FAIL',
  error_message VARCHAR(2000),
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_tenant_time (tenant_id, created_time),
  INDEX idx_audit_operator (operator_user_id),
  INDEX idx_audit_action (action, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨租户/运维审计日志';
