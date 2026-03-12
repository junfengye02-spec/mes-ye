-- ============================================================
-- 补丁：为缺少 deleted 列的表补充逻辑删除字段
-- 影响模块：基础数据、物料管理、质量管理、工作查询
-- ============================================================

ALTER TABLE mes_material_price ADD COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE mes_storage_inventory ADD COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE mes_material_requisition ADD COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE mes_finished_goods_receipt ADD COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE mes_requisition_order ADD COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE mes_finished_goods_receipt_request ADD COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE mes_material_return ADD COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE mes_work_start_check ADD COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE mes_order_start_check ADD COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE mes_production_work ADD COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE mes_inspection_work ADD COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE mes_shift_handover ADD COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';

-- 统一 sys_* 表 deleted 字段类型为 TINYINT(1) 以保持一致性
ALTER TABLE sys_user MODIFY COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE sys_role MODIFY COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
ALTER TABLE sys_menu MODIFY COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)';
