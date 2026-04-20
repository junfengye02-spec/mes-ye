-- ============================================================
-- V1.17 修复：
--   1) 业务单号 UNIQUE 改为 (tenant_id, code, deleted)，解决软删后不能重用编码的问题 (B-8)
--   2) 为之前遗漏 UNIQUE 的单号类字段补上 UNIQUE (H-12)
--   3) mes_work_order_task 增加 (tenant_id, work_order_id, task_no) 唯一约束 (H-13)
--   4) APS 扩展三表补 tenant_id 列与索引 (H-14)
--
-- 执行方式：与其它迁移一致，Flyway/手工 mysql 客户端依次执行。
-- 注意：若历史数据中已存在重复编码或同一工单下重复 task_no，需先清理再执行本脚本。
-- ============================================================

-- ---------------------------------------------------------------
-- 1) 业务单号 UNIQUE 改为 (tenant_id, code, deleted)
--    策略：先 DROP 原唯一键（若存在），再按 (tenant_id, deleted, code) 重建
-- ---------------------------------------------------------------

-- mes_material.material_code
ALTER TABLE mes_material DROP INDEX uk_material_code;
ALTER TABLE mes_material
    ADD UNIQUE KEY uk_tenant_material_code (tenant_id, deleted, material_code);

-- mes_work_order.work_order_no
ALTER TABLE mes_work_order DROP INDEX uk_work_order_no;
ALTER TABLE mes_work_order
    ADD UNIQUE KEY uk_tenant_work_order_no (tenant_id, deleted, work_order_no);

-- mes_order_plan.order_no
ALTER TABLE mes_order_plan DROP INDEX uk_order_no;
ALTER TABLE mes_order_plan
    ADD UNIQUE KEY uk_tenant_order_no (tenant_id, deleted, order_no);

-- mes_abnormal_contact.contact_no
ALTER TABLE mes_abnormal_contact DROP INDEX uk_contact_no;
ALTER TABLE mes_abnormal_contact
    ADD UNIQUE KEY uk_tenant_contact_no (tenant_id, deleted, contact_no);

-- mes_aps_data_mapping.(type, mes_code) / (type, aps_code)
ALTER TABLE mes_aps_data_mapping DROP INDEX uk_type_mes_code;
ALTER TABLE mes_aps_data_mapping DROP INDEX uk_type_aps_code;
ALTER TABLE mes_aps_data_mapping
    ADD UNIQUE KEY uk_tenant_type_mes_code (tenant_id, deleted, mapping_type, mes_code),
    ADD UNIQUE KEY uk_tenant_type_aps_code (tenant_id, deleted, mapping_type, aps_code);

-- ---------------------------------------------------------------
-- 2) 之前遗漏 UNIQUE 的业务单号（领料/入库/退料/配送签收等）
-- ---------------------------------------------------------------

-- mes_material_requisition.requisition_no
ALTER TABLE mes_material_requisition
    ADD UNIQUE KEY uk_tenant_req_no (tenant_id, deleted, requisition_no);

-- mes_finished_goods_receipt_request.request_no
ALTER TABLE mes_finished_goods_receipt_request
    ADD UNIQUE KEY uk_tenant_fgr_req_no (tenant_id, deleted, request_no);

-- mes_material_return.return_no
ALTER TABLE mes_material_return
    ADD UNIQUE KEY uk_tenant_return_no (tenant_id, deleted, return_no);

-- mes_requisition_order.(delivery_request_no, line_no)
ALTER TABLE mes_requisition_order
    ADD UNIQUE KEY uk_tenant_dr_line (tenant_id, deleted, delivery_request_no, line_no);

-- ---------------------------------------------------------------
-- 3) 工单任务：同一工单下工序号不可重复
-- ---------------------------------------------------------------

ALTER TABLE mes_work_order_task
    ADD UNIQUE KEY uk_tenant_wo_task_no (tenant_id, work_order_id, task_no);

-- ---------------------------------------------------------------
-- 4) APS 扩展三表补 tenant_id（V1.16 曾遗漏）
-- ---------------------------------------------------------------

ALTER TABLE mes_aps_gantt_cache
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    ADD KEY idx_tenant_batch_id (tenant_id, schedule_batch_id),
    ADD KEY idx_tenant_work_order_no (tenant_id, work_order_no);

ALTER TABLE mes_aps_capacity_load
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    ADD KEY idx_tenant_batch_id (tenant_id, schedule_batch_id),
    ADD KEY idx_tenant_wc_date (tenant_id, work_center_code, load_date);

ALTER TABLE mes_aps_schedule_change
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    ADD KEY idx_tenant_batch_id (tenant_id, schedule_batch_id),
    ADD KEY idx_tenant_work_order_no (tenant_id, work_order_no);
