-- ============================================================
-- 补丁：统一 sys_* 表 deleted 字段类型（与 MyBatis-Plus 逻辑删除一致）
-- 说明：业务表 deleted 已在各 V1.0x 建表脚本中带出，此处不再重复 ADD，避免 Docker 首次初始化报错
-- ============================================================

ALTER TABLE sys_user MODIFY COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE sys_role MODIFY COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE sys_menu MODIFY COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
