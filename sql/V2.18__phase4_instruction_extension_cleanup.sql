ALTER TABLE mes_instruction
  ADD COLUMN IF NOT EXISTS extension_data JSON COMMENT '扩展属性JSON' AFTER main_type;

UPDATE mes_instruction
SET extension_data = JSON_SET(
  COALESCE(extension_data, JSON_OBJECT()),
  '$.gtType',
  gt_type
)
WHERE gt_type IS NOT NULL AND gt_type <> '';

UPDATE mes_instruction
SET extension_data = JSON_SET(
  COALESCE(extension_data, JSON_OBJECT()),
  '$.repairGuideDrawing',
  repair_guide_drawing
)
WHERE repair_guide_drawing IS NOT NULL AND repair_guide_drawing <> '';
