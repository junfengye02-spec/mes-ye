SET @rename_work_center_resource_subtype_sql := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_work_center'
        AND column_name = 'furnace_resource_type'
    )
    AND NOT EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_work_center'
        AND column_name = 'resource_subtype'
    ),
    'ALTER TABLE mes_work_center CHANGE COLUMN furnace_resource_type resource_subtype VARCHAR(50) COMMENT ''资源子类型''',
    'SELECT 1'
  )
);
PREPARE stmt FROM @rename_work_center_resource_subtype_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @rename_work_status_resource_group_code_sql := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_work_status_view'
        AND column_name = 'furnace_no'
    )
    AND NOT EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_work_status_view'
        AND column_name = 'resource_group_code'
    ),
    'ALTER TABLE mes_work_status_view CHANGE COLUMN furnace_no resource_group_code VARCHAR(100) COMMENT ''资源组编码''',
    'SELECT 1'
  )
);
PREPARE stmt FROM @rename_work_status_resource_group_code_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
