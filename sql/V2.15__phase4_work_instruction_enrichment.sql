ALTER TABLE mes_work_instruction
    ADD COLUMN IF NOT EXISTS instruction_name VARCHAR(200) NULL COMMENT '指导书名称' AFTER instruction_code,
    ADD COLUMN IF NOT EXISTS process_id BIGINT NULL COMMENT '关联工序ID' AFTER instruction_name,
    ADD COLUMN IF NOT EXISTS version VARCHAR(50) NULL COMMENT '版本' AFTER process_id,
    ADD COLUMN IF NOT EXISTS content TEXT NULL COMMENT '作业内容' AFTER version,
    ADD COLUMN IF NOT EXISTS remark VARCHAR(500) NULL COMMENT '备注' AFTER content;

UPDATE mes_work_instruction
SET instruction_name = COALESCE(instruction_name, instruction_code)
WHERE instruction_name IS NULL OR instruction_name = '';
