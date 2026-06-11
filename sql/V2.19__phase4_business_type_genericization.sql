SET @rename_order_plan_business_type_sql := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_order_plan'
        AND column_name = 'work_type'
    )
    AND NOT EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_order_plan'
        AND column_name = 'business_type'
    ),
    'ALTER TABLE mes_order_plan CHANGE COLUMN work_type business_type VARCHAR(50) COMMENT ''业务类型''',
    'SELECT 1'
  )
);
PREPARE stmt FROM @rename_order_plan_business_type_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @rename_production_plan_business_type_sql := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_production_plan'
        AND column_name = 'work_type'
    )
    AND NOT EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_production_plan'
        AND column_name = 'business_type'
    ),
    'ALTER TABLE mes_production_plan CHANGE COLUMN work_type business_type VARCHAR(50) COMMENT ''业务类型''',
    'SELECT 1'
  )
);
PREPARE stmt FROM @rename_production_plan_business_type_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @rename_work_order_business_type_sql := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_work_order'
        AND column_name = 'work_type'
    )
    AND NOT EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_work_order'
        AND column_name = 'business_type'
    ),
    'ALTER TABLE mes_work_order CHANGE COLUMN work_type business_type VARCHAR(50) COMMENT ''业务类型''',
    'SELECT 1'
  )
);
PREPARE stmt FROM @rename_work_order_business_type_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @rename_material_return_business_type_sql := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_material_return'
        AND column_name = 'work_type'
    )
    AND NOT EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_material_return'
        AND column_name = 'business_type'
    ),
    'ALTER TABLE mes_material_return CHANGE COLUMN work_type business_type VARCHAR(50) COMMENT ''业务类型''',
    'SELECT 1'
  )
);
PREPARE stmt FROM @rename_material_return_business_type_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
