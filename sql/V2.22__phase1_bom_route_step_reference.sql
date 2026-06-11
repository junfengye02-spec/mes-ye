ALTER TABLE mes_manufacturing_bom_item
    ADD COLUMN IF NOT EXISTS route_step_id BIGINT NULL COMMENT '关联工艺路线步骤ID' AFTER process_id;

UPDATE mes_manufacturing_bom_item
SET route_step_id = COALESCE(route_step_id, process_id),
    process_id = COALESCE(process_id, route_step_id)
WHERE route_step_id IS NULL
   OR process_id IS NULL;
