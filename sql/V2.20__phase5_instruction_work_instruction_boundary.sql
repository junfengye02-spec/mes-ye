ALTER TABLE mes_instruction
    ADD COLUMN IF NOT EXISTS work_instruction_id BIGINT NULL COMMENT '关联作业指导书ID（引用可复用SOP模板）' AFTER work_order_no;
