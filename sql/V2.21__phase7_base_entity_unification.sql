ALTER TABLE mes_abnormal_contact_attachment
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(50) NULL COMMENT '创建人' AFTER signed,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50) NULL COMMENT '修改人' AFTER created_time,
    ADD COLUMN IF NOT EXISTS deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)' AFTER tenant_id;

ALTER TABLE mes_abnormal_contact_log
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(50) NULL COMMENT '创建人' AFTER remark,
    ADD COLUMN IF NOT EXISTS created_time DATETIME NULL COMMENT '创建时间' AFTER created_by,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50) NULL COMMENT '修改人' AFTER created_time,
    ADD COLUMN IF NOT EXISTS updated_time DATETIME NULL COMMENT '修改时间' AFTER updated_by,
    ADD COLUMN IF NOT EXISTS deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)' AFTER tenant_id;

UPDATE mes_abnormal_contact_log
SET created_by = COALESCE(created_by, operator),
    created_time = COALESCE(created_time, operated_time),
    updated_by = COALESCE(updated_by, operator),
    updated_time = COALESCE(updated_time, operated_time)
WHERE created_by IS NULL
   OR created_time IS NULL
   OR updated_by IS NULL
   OR updated_time IS NULL;

ALTER TABLE mes_shift_handover_attachment
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50) NULL COMMENT '修改人' AFTER created_time,
    ADD COLUMN IF NOT EXISTS updated_time DATETIME NULL COMMENT '修改时间' AFTER updated_by,
    ADD COLUMN IF NOT EXISTS deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)' AFTER tenant_id;

UPDATE mes_shift_handover_attachment
SET created_by = COALESCE(created_by, uploader)
WHERE created_by IS NULL;

ALTER TABLE mes_work_status_view
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(50) NULL COMMENT '创建人' AFTER issued,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50) NULL COMMENT '修改人' AFTER tenant_id,
    ADD COLUMN IF NOT EXISTS updated_time DATETIME NULL COMMENT '修改时间' AFTER updated_by,
    ADD COLUMN IF NOT EXISTS deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)' AFTER updated_time;

ALTER TABLE mes_recheck_order_plan
    ADD COLUMN IF NOT EXISTS deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)' AFTER tenant_id;

ALTER TABLE mes_recheck_serial
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(50) NULL COMMENT '创建人' AFTER barcode,
    ADD COLUMN IF NOT EXISTS created_time DATETIME NULL COMMENT '创建时间' AFTER created_by,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50) NULL COMMENT '修改人' AFTER created_time,
    ADD COLUMN IF NOT EXISTS updated_time DATETIME NULL COMMENT '修改时间' AFTER updated_by,
    ADD COLUMN IF NOT EXISTS deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)' AFTER tenant_id;

ALTER TABLE mes_delivery_sign
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(50) NULL COMMENT '创建人' AFTER delivery_time,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50) NULL COMMENT '修改人' AFTER updated_time,
    ADD COLUMN IF NOT EXISTS deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)' AFTER tenant_id;

ALTER TABLE mes_finished_goods_receipt_item
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(50) NULL COMMENT '创建人' AFTER variance_reason,
    ADD COLUMN IF NOT EXISTS created_time DATETIME NULL COMMENT '创建时间' AFTER created_by,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50) NULL COMMENT '修改人' AFTER created_time,
    ADD COLUMN IF NOT EXISTS updated_time DATETIME NULL COMMENT '修改时间' AFTER updated_by,
    ADD COLUMN IF NOT EXISTS deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)' AFTER tenant_id;

ALTER TABLE mes_material_requisition_item
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(50) NULL COMMENT '创建人' AFTER is_final,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50) NULL COMMENT '修改人' AFTER updated_time,
    ADD COLUMN IF NOT EXISTS deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)' AFTER tenant_id;
