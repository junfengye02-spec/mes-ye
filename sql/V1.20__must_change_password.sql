-- ============================================================
-- V1.20  sys_user 增加 must_change_password 字段
--
--   背景（P0-06 安全整改）：
--     种子测试账号 zhangsan / lisi / wangwu / zhaoliu 与 admin 共用
--     BCrypt(admin123) 密文；若客户初始化后裸奔将形成高危弱口令入口。
--     应用启动时由 WeakPasswordAuditor（prod profile）把命中弱口令
--     hash 的账号置 must_change_password=1，登录响应回带 mustChangePwd=true，
--     前端引导至"强制修改密码"对话框。
--
--   字段语义：
--     0 = 正常；1 = 下次登录必须先改密码才能继续使用
--
--   兼容性（P0 修复 R1，由 mcp30 接盘自 mcp26）：
--     - 原版本使用 `ADD COLUMN IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS`
--       —— 这是 MariaDB 方言，在纯 MySQL 8.0.x 上 ERROR 1064（mcp9-v2 复验）；
--     - 本次改为 INFORMATION_SCHEMA + PREPARE/EXECUTE 动态 SQL，
--       兼容 MySQL 8.0.12+ 所有发行版；连续执行两次幂等。
--     - 该字段由 SysUser 实体映射，变更时由 /system/user/change-my-password
--       接口在成功改密后复位为 0。
-- ============================================================

-- 1) sys_user.must_change_password ----------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_user'
     AND column_name  = 'must_change_password'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_user ADD COLUMN must_change_password TINYINT NOT NULL DEFAULT 0 COMMENT ''是否需要首次登录强制修改密码：0=正常，1=必须改密''',
  'SELECT ''sys_user.must_change_password already exists, skip ADD COLUMN'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 2) idx_sys_user_must_change_pwd：为巡检 + 后台看板查询 "有多少账号仍未改密" 提供加速
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'sys_user'
     AND index_name   = 'idx_sys_user_must_change_pwd'
);
SET @ddl := IF(@idx_exists = 0,
  'CREATE INDEX idx_sys_user_must_change_pwd ON sys_user (must_change_password)',
  'SELECT ''idx_sys_user_must_change_pwd already exists, skip CREATE INDEX'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
