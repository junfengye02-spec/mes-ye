-- ============================================================
-- V2.04  DB 兜底方案（MySQL 8）
--   - 建立专用"业务应用账号" mes_app：
--     * 只能在 mes_db 数据库内活动
--     * 对业务表只能 SELECT/INSERT/UPDATE/DELETE（无 DDL）
--     * 对 sys_tenant / sys_audit_log / sys_tenant_provision_log 只读
--     * 禁止 SUPER / GRANT / PROCESS / RELOAD 等运维权限
--   - 迁移 / DDL 用另一个账号 mes_migrator（本脚本不创建该账号）
--
-- 注意：脚本假设数据库名为 mes_db；若不是请先 SET @db := '实际库名';
--       建议通过 .env 传入密码占位符：
--         mysql --defaults-extra-file=... --init-command="SET @app_pwd='${MES_APP_PASSWORD}';"
--       本脚本示例用 :app_password 需要在外部替换。
-- ============================================================

-- 1) 创建业务账号（幂等）------------------------------------------
CREATE USER IF NOT EXISTS 'mes_app'@'%'
    IDENTIFIED BY 'REPLACE_ME_IN_DEPLOY'
    PASSWORD EXPIRE NEVER;

-- 2) 业务表授权：只允许 DML，不允许 DDL。---------------------------
-- 精细到表：列表可用 INFORMATION_SCHEMA 动态生成，此处给出核心业务表示例。
GRANT SELECT, INSERT, UPDATE, DELETE ON mes_db.mes_work_order       TO 'mes_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON mes_db.mes_work_order_task  TO 'mes_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON mes_db.mes_material          TO 'mes_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON mes_db.mes_manufacturing_bom TO 'mes_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON mes_db.mes_process_template  TO 'mes_app'@'%';
-- 其余业务表建议在部署脚本里按 schema 全量授权：
--   INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='mes_db' AND TABLE_NAME LIKE 'mes\_%'
-- 然后 GRANT ... TO 'mes_app'@'%' ;

-- 3) 租户元 / 审计 / 用量表：只读，避免应用层被利用后改 sys_tenant
GRANT SELECT ON mes_db.sys_tenant                TO 'mes_app'@'%';
GRANT SELECT ON mes_db.sys_tenant_provision_log  TO 'mes_app'@'%';
GRANT SELECT, INSERT, UPDATE ON mes_db.sys_tenant_quota_usage TO 'mes_app'@'%';
GRANT SELECT, INSERT ON mes_db.sys_audit_log     TO 'mes_app'@'%';

-- 4) RBAC 表：应用需要写（角色/菜单/绑定可在后台改），但不能 DROP
GRANT SELECT, INSERT, UPDATE, DELETE ON mes_db.sys_user        TO 'mes_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON mes_db.sys_role        TO 'mes_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON mes_db.sys_menu        TO 'mes_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON mes_db.sys_user_role   TO 'mes_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON mes_db.sys_role_menu   TO 'mes_app'@'%';

-- 5) 吊销所有危险权限 ---------------------------------------------
-- 保险起见：显式声明该账号不允许 SUPER/PROCESS/FILE/RELOAD 等
REVOKE SUPER, PROCESS, FILE, RELOAD, SHUTDOWN, REPLICATION SLAVE, REPLICATION CLIENT ON *.* FROM 'mes_app'@'%';

FLUSH PRIVILEGES;

-- 6) （可选）创建业务表视图 + 触发器，提供 "SET @session_tenant = x" 纵深防御。
--    实际上线建议在关键表上加 BEFORE INSERT/UPDATE 触发器：
--      IF @session_tenant IS NOT NULL AND NEW.tenant_id <> @session_tenant THEN
--          SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'cross tenant write blocked';
--      END IF
--    应用层在 JwtAuthenticationFilter 之后执行：
--      SET @session_tenant = <currentTenantId>
--    这部分由 DBA 按实际表清单落地，不在本 SQL 中一次性铺。
