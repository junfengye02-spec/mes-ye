-- ============================================================
-- Phase 2: 状态级联、质量复检、异常联动
-- ============================================================

ALTER TABLE mes_abnormal_contact
    ADD COLUMN IF NOT EXISTS work_order_id BIGINT NULL COMMENT '关联工单ID' AFTER contact_no,
    ADD COLUMN IF NOT EXISTS dispatch_task_id BIGINT NULL COMMENT '关联派工任务ID' AFTER work_order_id;

ALTER TABLE mes_recheck_request
    ADD COLUMN IF NOT EXISTS work_order_id BIGINT NULL COMMENT '关联工单ID' AFTER id,
    ADD COLUMN IF NOT EXISTS dispatch_task_id BIGINT NULL COMMENT '关联派工任务ID' AFTER work_order_id;
