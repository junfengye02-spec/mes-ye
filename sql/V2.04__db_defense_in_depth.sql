-- ============================================================
-- V2.04  DB 兜底方案（MySQL 8）
--   - 建立专用"业务应用账号" mes_app：
--     * 只能在当前业务 schema 内活动（由运维在 --database= 参数 / Flyway schema 决定）
--     * 对业务表只能 SELECT/INSERT/UPDATE/DELETE（无 DDL）
--     * 对 sys_tenant / sys_audit_log / sys_tenant_provision_log 只读
--     * 禁止 SUPER / GRANT / PROCESS / RELOAD 等运维权限
--   - 迁移 / DDL 用另一个账号 mes_migrator（本脚本不创建该账号）
--
-- 兼容性（P0 修复 R2，由 mcp30 接盘自 mcp26）：
--   原版本硬编码 `mes_db.xxx` 表名，任何 schema 不是 mes_db 的环境（多租户
--   物理分库、灰度、测试）都会 ERROR 1146（mcp9-v2 复验）。
--   本次改造原则：
--     1) GRANT 的 schema 从当前执行连接的 DATABASE() 动态解析（PREPARE/EXECUTE）；
--     2) 保持 "mes_app 只能在当前业务库内活动" 的安全语义；
--     3) 连续执行两次幂等（GRANT 重复执行 MySQL 8 会把权限设为并集，结果相同）。
--
-- 注意：脚本要求执行连接已经 USE 到业务库（Flyway 会自动切；手工执行时
--       执行 `USE mes_db;` / `USE mes_m8_v2_test;` 等目标 schema 后再跑）。
-- ============================================================

-- 0) 捕获当前业务 schema 名，后续所有 GRANT 动态拼接 ----------------
SET @db := DATABASE();

-- 0.1 防御：禁止在 information_schema / mysql / performance_schema / sys 这种系统库里执行
--          通过 INSERT INTO _v204_guard_only_in_business_schema 方式让 Flyway 显式失败。
--          逻辑：在"被拒绝的系统库"上，把 @db 强制置成不存在的保留名，
--          后续所有 CONCAT 动态 SQL 会 GRANT ON `__V204_ABORT__`.xxx，
--          MySQL 会以 ERROR 1146 / 1044 终止执行，Flyway 回滚事务。
SET @db := IF(@db IS NULL
             OR @db IN ('mysql', 'information_schema', 'performance_schema', 'sys'),
             '__V204_ABORT_MUST_RUN_IN_BUSINESS_SCHEMA__',
             @db);
SELECT CONCAT('V2.04 target schema = ', @db) AS note;

-- 1) 创建业务账号（幂等）------------------------------------------
CREATE USER IF NOT EXISTS 'mes_app'@'%'
    IDENTIFIED BY 'REPLACE_ME_IN_DEPLOY'
    PASSWORD EXPIRE NEVER;

-- ------------------------------------------------------------
-- 动态 GRANT 辅助：封装 "GRANT <priv> ON `<db>`.`<tbl>` TO 'mes_app'@'%'"
-- 为了保持脚本清晰，这里为每张表展开一段 SET @ddl + PREPARE + EXECUTE。
-- ------------------------------------------------------------

-- 2) 业务表授权：只允许 DML，不允许 DDL。---------------------------
--    精细到表，核心业务表示例；完整列表建议由部署脚本按 INFORMATION_SCHEMA 动态补齐。

SET @ddl := CONCAT('GRANT SELECT, INSERT, UPDATE, DELETE ON `', @db, '`.`mes_work_order` TO ''mes_app''@''%''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := CONCAT('GRANT SELECT, INSERT, UPDATE, DELETE ON `', @db, '`.`mes_work_order_task` TO ''mes_app''@''%''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := CONCAT('GRANT SELECT, INSERT, UPDATE, DELETE ON `', @db, '`.`mes_material` TO ''mes_app''@''%''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := CONCAT('GRANT SELECT, INSERT, UPDATE, DELETE ON `', @db, '`.`mes_manufacturing_bom` TO ''mes_app''@''%''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := CONCAT('GRANT SELECT, INSERT, UPDATE, DELETE ON `', @db, '`.`mes_process_template` TO ''mes_app''@''%''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 其余业务表建议在部署脚本里按 schema 全量授权：
--   INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME LIKE 'mes\_%'
-- 然后 GRANT ... TO 'mes_app'@'%' ;

-- 3) 租户元 / 审计 / 用量表：只读，避免应用层被利用后改 sys_tenant ----
SET @ddl := CONCAT('GRANT SELECT ON `', @db, '`.`sys_tenant` TO ''mes_app''@''%''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := CONCAT('GRANT SELECT ON `', @db, '`.`sys_tenant_provision_log` TO ''mes_app''@''%''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := CONCAT('GRANT SELECT, INSERT, UPDATE ON `', @db, '`.`sys_tenant_quota_usage` TO ''mes_app''@''%''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := CONCAT('GRANT SELECT, INSERT ON `', @db, '`.`sys_audit_log` TO ''mes_app''@''%''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 4) RBAC 表：应用需要写（角色/菜单/绑定可在后台改），但不能 DROP
SET @ddl := CONCAT('GRANT SELECT, INSERT, UPDATE, DELETE ON `', @db, '`.`sys_user` TO ''mes_app''@''%''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := CONCAT('GRANT SELECT, INSERT, UPDATE, DELETE ON `', @db, '`.`sys_role` TO ''mes_app''@''%''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := CONCAT('GRANT SELECT, INSERT, UPDATE, DELETE ON `', @db, '`.`sys_menu` TO ''mes_app''@''%''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := CONCAT('GRANT SELECT, INSERT, UPDATE, DELETE ON `', @db, '`.`sys_user_role` TO ''mes_app''@''%''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := CONCAT('GRANT SELECT, INSERT, UPDATE, DELETE ON `', @db, '`.`sys_role_menu` TO ''mes_app''@''%''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 5) 吊销所有危险权限 ---------------------------------------------
-- 保险起见：显式声明该账号不允许 SUPER/PROCESS/FILE/RELOAD 等
-- REVOKE 无条件执行：第二次执行时权限本就没授过，REVOKE 直接 no-op（MySQL 8 REVOKE 对不存在的权限是 0 warning）
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
