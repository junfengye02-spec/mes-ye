CREATE TABLE IF NOT EXISTS mes_process_parameter_schema (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  schema_code VARCHAR(50) NOT NULL COMMENT '模板编码',
  schema_name VARCHAR(100) NOT NULL COMMENT '模板名称',
  process_type VARCHAR(50) COMMENT '工艺类型',
  field_definitions JSON COMMENT '字段定义JSON',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_schema_code_tenant (tenant_id, schema_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用工艺参数模板表';

CREATE TABLE IF NOT EXISTS mes_process_parameter_value (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  schema_id BIGINT NOT NULL COMMENT '模板ID',
  parameter_code VARCHAR(100) NOT NULL COMMENT '参数编码',
  parameter_name VARCHAR(200) COMMENT '参数名称',
  process_info_id BIGINT COMMENT '关联工序ID',
  process_type VARCHAR(50) COMMENT '工艺类型',
  status VARCHAR(20) COMMENT '状态',
  search_text VARCHAR(500) COMMENT '搜索辅助文本',
  param_values JSON NOT NULL COMMENT '参数值JSON',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_param_code_tenant (tenant_id, schema_id, parameter_code),
  KEY idx_param_schema_status (tenant_id, schema_id, status),
  KEY idx_param_schema_name (tenant_id, schema_id, parameter_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用工艺参数值表';

ALTER TABLE mes_abnormal_contact_attachment
  ADD COLUMN IF NOT EXISTS signature_provider VARCHAR(50) COMMENT '签章供应商' AFTER submit_time,
  ADD COLUMN IF NOT EXISTS signature_status VARCHAR(20) COMMENT '签章状态' AFTER signature_provider;

INSERT INTO mes_process_parameter_schema (
  schema_code, schema_name, process_type, field_definitions,
  created_by, created_time, updated_by, updated_time, tenant_id, deleted
)
SELECT 'SPRAY_CONDITION', '喷涂条件', 'SPRAY',
       JSON_ARRAY(
         JSON_OBJECT('name', 'conditionNo', 'type', 'string', 'required', TRUE),
         JSON_OBJECT('name', 'sprayGunModel', 'type', 'string', 'required', FALSE),
         JSON_OBJECT('name', 'equipment', 'type', 'string', 'required', FALSE),
         JSON_OBJECT('name', 'powderFeedRate', 'type', 'decimal', 'required', FALSE),
         JSON_OBJECT('name', 'sprayDistance', 'type', 'decimal', 'required', FALSE)
       ),
       'system', NOW(), 'system', NOW(), 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM mes_process_parameter_schema
  WHERE tenant_id = 1 AND schema_code = 'SPRAY_CONDITION' AND deleted = 0
);

INSERT INTO mes_process_parameter_schema (
  schema_code, schema_name, process_type, field_definitions,
  created_by, created_time, updated_by, updated_time, tenant_id, deleted
)
SELECT 'MACHINING_PROGRAM', '机械加工程序', 'MACHINING',
       JSON_ARRAY(
         JSON_OBJECT('name', 'gCode', 'type', 'string', 'required', TRUE),
         JSON_OBJECT('name', 'programTable', 'type', 'string', 'required', FALSE),
         JSON_OBJECT('name', 'productName', 'type', 'string', 'required', FALSE)
       ),
       'system', NOW(), 'system', NOW(), 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM mes_process_parameter_schema
  WHERE tenant_id = 1 AND schema_code = 'MACHINING_PROGRAM' AND deleted = 0
);

INSERT INTO mes_process_parameter_value (
  schema_id, parameter_code, parameter_name, process_type, status, search_text, param_values,
  created_by, created_time, updated_by, updated_time, tenant_id, deleted
)
SELECT schema.id,
       spray.condition_no,
       spray.condition_no,
       'SPRAY',
       NULL,
       CONCAT_WS(' ', spray.spray_gun_model, spray.equipment, spray.powder_type),
       JSON_OBJECT(
         'conditionNo', spray.condition_no,
         'ministerApprover', spray.minister_approver,
         'ministerApproveTime', spray.minister_approve_time,
         'sectionApprover', spray.section_approver,
         'sectionApproveTime', spray.section_approve_time,
         'leaderApprover', spray.leader_approver,
         'leaderApproveTime', spray.leader_approve_time,
         'powderFeedRate', spray.powder_feed_rate,
         'sprayDistance', spray.spray_distance,
         'sprayGunModel', spray.spray_gun_model,
         'faiReport', spray.fai_report,
         'faiGuide', spray.fai_guide,
         'powderFeeder', spray.powder_feeder,
         'powderFeederSpeed', spray.powder_feeder_speed,
         'oxygenScfh', spray.oxygen_scfh,
         'keroseneGph', spray.kerosene_gph,
         'combustionPressure', spray.combustion_pressure,
         'carrierGas', spray.carrier_gas,
         'equipment', spray.equipment,
         'powderType', spray.powder_type
       ),
       spray.created_by, spray.created_time, spray.updated_by, spray.updated_time,
       COALESCE(spray.tenant_id, 1), COALESCE(spray.deleted, 0)
FROM mes_spray_condition spray
JOIN mes_process_parameter_schema schema
  ON schema.schema_code = 'SPRAY_CONDITION'
 AND schema.tenant_id = COALESCE(spray.tenant_id, 1)
 AND schema.deleted = 0
LEFT JOIN mes_process_parameter_value value_tbl
  ON value_tbl.schema_id = schema.id
 AND value_tbl.parameter_code = spray.condition_no
 AND value_tbl.tenant_id = COALESCE(spray.tenant_id, 1)
 AND value_tbl.deleted = 0
WHERE value_tbl.id IS NULL;

INSERT INTO mes_process_parameter_value (
  schema_id, parameter_code, parameter_name, process_type, status, search_text, param_values,
  created_by, created_time, updated_by, updated_time, tenant_id, deleted
)
SELECT schema.id,
       program.g_code,
       program.product_name,
       'MACHINING',
       NULL,
       CONCAT_WS(' ', program.g_code, program.product_name),
       JSON_OBJECT(
         'gCode', program.g_code,
         'programTable', program.program_table,
         'productName', program.product_name
       ),
       program.created_by, program.created_time, program.updated_by, program.updated_time,
       COALESCE(program.tenant_id, 1), COALESCE(program.deleted, 0)
FROM mes_machining_program program
JOIN mes_process_parameter_schema schema
  ON schema.schema_code = 'MACHINING_PROGRAM'
 AND schema.tenant_id = COALESCE(program.tenant_id, 1)
 AND schema.deleted = 0
LEFT JOIN mes_process_parameter_value value_tbl
  ON value_tbl.schema_id = schema.id
 AND value_tbl.parameter_code = program.g_code
 AND value_tbl.tenant_id = COALESCE(program.tenant_id, 1)
 AND value_tbl.deleted = 0
WHERE value_tbl.id IS NULL;

UPDATE mes_abnormal_contact_attachment
SET signature_provider = COALESCE(signature_provider, fadada_flag),
    signature_status = COALESCE(signature_status,
      CASE WHEN signed = 1 THEN 'SIGNED' ELSE 'UNSIGNED' END)
WHERE signature_provider IS NULL OR signature_status IS NULL;
