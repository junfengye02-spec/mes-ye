SET @rename_order_plan_flow_code_sql := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_order_plan'
        AND column_name = 'pccl_flow'
    )
    AND NOT EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_order_plan'
        AND column_name = 'flow_code'
    ),
    'ALTER TABLE mes_order_plan CHANGE COLUMN pccl_flow flow_code VARCHAR(100) COMMENT ''流程编码''',
    'SELECT 1'
  )
);
PREPARE stmt FROM @rename_order_plan_flow_code_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @rename_material_return_flow_code_sql := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_material_return'
        AND column_name = 'pccl_flow'
    )
    AND NOT EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_material_return'
        AND column_name = 'flow_code'
    ),
    'ALTER TABLE mes_material_return CHANGE COLUMN pccl_flow flow_code VARCHAR(100) COMMENT ''流程编码''',
    'SELECT 1'
  )
);
PREPARE stmt FROM @rename_material_return_flow_code_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
