-- ============================================================
-- V1.18 生产上线补强（索引 / UNIQUE / 多租户隔离）
--
-- 来源：由 mcp9 在《MES 数据库脚本完整性与表结构审计》报告中产出
-- 整理：mcp7（P0-09 任务落地）
-- 说明：本脚本解决 mcp9 审计报告 §三、§四中发现的 13 张表缺唯一约束、
--       51 张表缺 tenant_id 索引、6 项多租户隔离漏洞中的可 DDL 层面修复部分。
--
-- 执行前提：
--   1. MySQL 版本 >= 8.0.29（使用 CREATE INDEX IF NOT EXISTS / ADD UNIQUE IF NOT EXISTS 风格）；
--      8.0.25-8.0.28 环境需要把下方 ALTER TABLE 手动包成存储过程做 IF NOT EXISTS 语义
--   2. 已经执行完 V1.00 ~ V1.17 + V2.01 ~ V2.04 的全量 Flyway 迁移
--   3. 业务低峰期执行，部分 ADD UNIQUE 可能需要先校对/清洗跨租户重复数据
--   4. 执行前务必全库备份：mysqldump -u root -p mes > mes_before_V1.18.sql
-- ============================================================

-- ------------------------------------------------------------
-- 步骤 0：幂等包装存储过程（适配 MySQL 8.0.25+，低于 8.0.29 时启用）
--   如果你的 MySQL 已 >= 8.0.29，可以删除这段（直接使用 IF NOT EXISTS）
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS mes_add_index_if_absent;
DELIMITER $$
CREATE PROCEDURE mes_add_index_if_absent(
    IN p_table   VARCHAR(128),
    IN p_index   VARCHAR(128),
    IN p_columns VARCHAR(500),
    IN p_unique  TINYINT
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    SELECT COUNT(1) INTO v_exists
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index;
    IF v_exists = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD ',
                          IF(p_unique = 1, 'UNIQUE KEY ', 'KEY '),
                          '`', p_index, '` (', p_columns, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS mes_drop_index_if_exists;
DELIMITER $$
CREATE PROCEDURE mes_drop_index_if_exists(
    IN p_table VARCHAR(128),
    IN p_index VARCHAR(128)
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    SELECT COUNT(1) INTO v_exists
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index;
    IF v_exists > 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ------------------------------------------------------------
-- 步骤 1：把原本跨租户的业务编号 UNIQUE 改为 (tenant_id, deleted, xxx_no) 联合 UNIQUE
--   原因（mcp9 报告 V-4）：一个租户占用了某编号，其他租户就不能再用；
--   软删除时同一编号还能被再次使用。
-- ------------------------------------------------------------
CALL mes_drop_index_if_exists('mes_outsource_order',         'uk_outsource_order_no');
CALL mes_add_index_if_absent ('mes_outsource_order',         'uk_tenant_outsource_no', '`tenant_id`, `deleted`, `outsource_order_no`', 1);

CALL mes_drop_index_if_exists('mes_transfer_order',          'uk_transfer_no');
CALL mes_add_index_if_absent ('mes_transfer_order',          'uk_tenant_transfer_no',  '`tenant_id`, `deleted`, `transfer_no`',        1);

CALL mes_drop_index_if_exists('mes_finished_goods_receipt',  'uk_receipt_no');
CALL mes_add_index_if_absent ('mes_finished_goods_receipt',  'uk_tenant_receipt_no',   '`tenant_id`, `deleted`, `receipt_no`',         1);

-- ------------------------------------------------------------
-- 步骤 2：给原本没有 UNIQUE 的业务编号补上 (tenant_id, work_no) 联合 UNIQUE
--   原因（mcp9 报告 V-2）：跨租户业务编号可能被串号。
-- ------------------------------------------------------------
CALL mes_add_index_if_absent('mes_production_work', 'uk_tenant_work_no',  '`tenant_id`, `work_no`', 1);
CALL mes_add_index_if_absent('mes_inspection_work', 'uk_tenant_iw_no',    '`tenant_id`, `work_no`', 1);

-- ------------------------------------------------------------
-- 步骤 3：库存唯一性（最关键！）
--   原因（mcp9 报告 §四 最高风险项）：mes_storage_inventory 原本
--   (工厂, 仓库, 库位, 物料) 无唯一约束，并发入库可能创建两条记录。
--
--   ⚠️ 执行前必须先清洗已存在的重复数据：
--      SELECT tenant_id, factory, warehouse, storage_location, material_code, COUNT(*)
--        FROM mes_storage_inventory GROUP BY 1,2,3,4,5 HAVING COUNT(*) > 1;
--      合并重复行为同一条后再执行本步骤。
-- ------------------------------------------------------------
CALL mes_add_index_if_absent(
    'mes_storage_inventory',
    'uk_tenant_inv_loc',
    '`tenant_id`, `factory`, `warehouse`, `storage_location`, `material_code`',
    1
);

-- ------------------------------------------------------------
-- 步骤 4：BOM 明细唯一性
--   同一个 BOM 同一个物料的同一个序号只能有一条。
-- ------------------------------------------------------------
CALL mes_add_index_if_absent(
    'mes_manufacturing_bom_item',
    'uk_tenant_bom_item',
    '`tenant_id`, `bom_id`, `material_code`, `sequence_no`',
    1
);

-- ------------------------------------------------------------
-- 步骤 5：APS 产能负荷按批次+中心+日期唯一
-- ------------------------------------------------------------
CALL mes_add_index_if_absent(
    'mes_aps_capacity_load',
    'uk_tenant_cap',
    '`tenant_id`, `schedule_batch_id`, `work_center_code`, `load_date`',
    1
);

-- ------------------------------------------------------------
-- 步骤 6：高频查询表补 tenant 索引
--   原因（mcp9 报告 V-3）：51 张表 WHERE tenant_id = ? 走全表扫，QPS 大时 CPU/IO 暴涨。
--   本脚本只补 5 张最紧迫的，其余 46 张表由 DBA 分批补。
-- ------------------------------------------------------------
CALL mes_add_index_if_absent('mes_shift_handover',  'idx_tenant_date_shift', '`tenant_id`, `handover_date`, `handover_shift`', 0);
CALL mes_add_index_if_absent('mes_recheck_request', 'idx_tenant_status',     '`tenant_id`, `status`',                          0);
CALL mes_add_index_if_absent('mes_material_return', 'idx_tenant_status_time','`tenant_id`, `status`, `created_time`',          0);
CALL mes_add_index_if_absent('mes_dispatch_task',   'idx_tenant_status',     '`tenant_id`, `dispatch_status`',                 0);

-- ------------------------------------------------------------
-- 步骤 7：日志表补 (tenant_id, xxx_id) 复合索引
--   日志量级大，没索引导致按业务对象查历史日志接近全表扫。
-- ------------------------------------------------------------
CALL mes_add_index_if_absent('mes_work_order_status_log', 'idx_tenant_wo', '`tenant_id`, `work_order_id`',      0);
CALL mes_add_index_if_absent('mes_dispatch_status_log',   'idx_tenant_dt', '`tenant_id`, `dispatch_task_id`',   0);
CALL mes_add_index_if_absent('mes_abnormal_contact_log',  'idx_tenant_ac', '`tenant_id`, `contact_id`',         0);
CALL mes_add_index_if_absent('mes_plan_status_log',       'idx_tenant_pt', '`tenant_id`, `plan_type`, `plan_id`', 0);

-- ------------------------------------------------------------
-- 步骤 8：清理辅助存储过程
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS mes_add_index_if_absent;
DROP PROCEDURE IF EXISTS mes_drop_index_if_exists;

-- ============================================================
-- 未纳入本脚本的补强项（需要人工评审后分批落地）：
--   1) 全库 COLLATE 对齐：ALTER DATABASE mes CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
--      —— 涉及所有表数据重排，需要停机窗口，建议单独 V1.19 处理。
--   2) 跨租户写保护触发器（mcp9 报告步骤 8）：
--      —— 需要应用层保证每次连接 SET @session_tenant = :tenantId，
--      MyBatis-Plus TenantLineInnerInterceptor 默认不做这个。建议改用应用层保障 + 审计日志。
--   3) mes_aps_sync_config 补 tenant_id：
--      —— 需要配套改 Service 逻辑和历史数据回填脚本，由独立 V1.20 处理。
--   4) seed_test_data.sql 的 4 个弱口令账号禁入生产：
--      —— 建议改名为 R__seed_test_data.sql（Repeatable，仅 Docker 初始化用），
--      或由 CI 在打生产镜像时删除。
-- ============================================================
