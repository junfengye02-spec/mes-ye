-- ============================================================
-- 模块：生产工单
-- 表数量：9
-- ============================================================

-- 1. 生产工单主表
CREATE TABLE IF NOT EXISTS mes_work_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_no VARCHAR(100) NOT NULL COMMENT '工单号',
  work_order_type VARCHAR(50) COMMENT '工单类型',
  production_plan_no VARCHAR(100) COMMENT '生产计划',
  order_plan_no VARCHAR(100) COMMENT '订单计划',
  order_no VARCHAR(100) COMMENT '订单编号',
  product_code VARCHAR(100) COMMENT '产品编码',
  product_name VARCHAR(200) COMMENT '产品名称',
  main_product VARCHAR(200) COMMENT '主产品',
  machine_model VARCHAR(100) COMMENT '机型',
  product_category VARCHAR(50) COMMENT '产品类别',
  product_type VARCHAR(50) COMMENT '产品类型',
  bom_code VARCHAR(100) COMMENT '制造BOM',
  project_name VARCHAR(200) COMMENT '项目',
  wbs_element VARCHAR(100) COMMENT 'WBS元素',
  new_or_repair_type VARCHAR(50) COMMENT '新制维修类型',
  business_type VARCHAR(50) COMMENT '业务类型',
  plan_qty DECIMAL(18,4) COMMENT '计划数量',
  qty_unit VARCHAR(20) COMMENT '数量单位',
  factory_org VARCHAR(100) COMMENT '工厂组织',
  plan_org VARCHAR(100) COMMENT '计划组织',
  main_org VARCHAR(100) COMMENT '主制组织',
  plan_work_center_id BIGINT COMMENT '计划工作中心',
  specified_work_center_id BIGINT COMMENT '指定工作中心',
  status VARCHAR(20) DEFAULT 'CREATED' COMMENT '状态（CREATED/RELEASED/IN_PROGRESS/COMPLETED）',
  plan_start_time DATETIME COMMENT '计划开始时间',
  plan_end_time DATETIME COMMENT '计划结束时间',
  actual_start_time DATETIME COMMENT '实际开始时间',
  actual_end_time DATETIME COMMENT '实际结束时间',
  serial_no VARCHAR(100) COMMENT '序列号',
  special_stock_flag VARCHAR(50) COMMENT '特殊库存标识',
  delivery_location VARCHAR(200) COMMENT '交货地点',
  remark VARCHAR(500) COMMENT '说明',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_work_order_no (work_order_no),
  KEY idx_order_plan_no (order_plan_no),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产工单主表';

-- 2. 工作清单表
CREATE TABLE IF NOT EXISTS mes_work_order_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL COMMENT '工单ID',
  task_no VARCHAR(50) COMMENT '工序号/工作编号',
  task_name VARCHAR(200) COMMENT '工作名称',
  plan_work_center_id BIGINT COMMENT '计划工作中心',
  plan_qty DECIMAL(18,4) COMMENT '计划数量',
  qty_unit VARCHAR(20) COMMENT '数量单位',
  status VARCHAR(20) DEFAULT 'CREATED' COMMENT '状态',
  sequence_no INT COMMENT '顺序号',
  serial_no VARCHAR(100) COMMENT '序列号',
  project_name VARCHAR(200) COMMENT '项目',
  created_time DATETIME,
  updated_time DATETIME,
  KEY idx_work_order_id (work_order_id),
  KEY idx_sequence_no (sequence_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作清单表';

-- 3. 输入物料表
CREATE TABLE IF NOT EXISTS mes_work_order_input_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL COMMENT '工单ID',
  material_id BIGINT COMMENT '物料ID',
  material_code VARCHAR(100) COMMENT '物料编码',
  material_name VARCHAR(200) COMMENT '物料名称',
  required_qty DECIMAL(18,4) COMMENT '需求数量',
  issued_qty DECIMAL(18,4) DEFAULT 0 COMMENT '已发数量',
  qty_unit VARCHAR(20) COMMENT '数量单位',
  batch_no VARCHAR(100) COMMENT '批号',
  serial_no VARCHAR(100) COMMENT '序列号',
  KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='输入物料表';

-- 4. 输出物料表
CREATE TABLE IF NOT EXISTS mes_work_order_output_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL COMMENT '工单ID',
  material_id BIGINT COMMENT '物料ID',
  material_code VARCHAR(100) COMMENT '物料编码',
  material_name VARCHAR(200) COMMENT '物料名称',
  output_qty DECIMAL(18,4) COMMENT '产出数量',
  qty_unit VARCHAR(20) COMMENT '数量单位',
  KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='输出物料表';

-- 5. 检验项目清单表
CREATE TABLE IF NOT EXISTS mes_work_order_quality_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL COMMENT '工单ID',
  quality_item_code VARCHAR(100) COMMENT '检验项目编号',
  quality_item_name VARCHAR(200) COMMENT '检验项目名称',
  requirement VARCHAR(500) COMMENT '检验要求',
  status VARCHAR(20) COMMENT '状态',
  KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验项目清单表';

-- 6. 约束关系表
CREATE TABLE IF NOT EXISTS mes_work_order_constraint (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL COMMENT '工单ID',
  constraint_type VARCHAR(50) COMMENT '约束类型',
  related_work_order_id BIGINT COMMENT '关联工单ID',
  related_task_id BIGINT COMMENT '关联工作清单ID',
  remark VARCHAR(500) COMMENT '说明',
  KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='约束关系表';

-- 7. 供应计划表
CREATE TABLE IF NOT EXISTS mes_work_order_supply_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL COMMENT '工单ID',
  demand_plan_no VARCHAR(100) COMMENT '需求计划',
  supply_plan_no VARCHAR(100) COMMENT '供应计划',
  supply_qty DECIMAL(18,4) COMMENT '供应数量',
  qty_unit VARCHAR(20) COMMENT '计量单位',
  plan_org VARCHAR(100) COMMENT '计划组织',
  completed_qty DECIMAL(18,4) DEFAULT 0 COMMENT '完工数量',
  code VARCHAR(100) COMMENT '编号',
  KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应计划表';

-- 8. 文档附件表
CREATE TABLE IF NOT EXISTS mes_work_order_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL COMMENT '工单ID',
  file_name VARCHAR(200) COMMENT '文件名',
  file_type VARCHAR(50) COMMENT '文件类型',
  file_size_kb INT COMMENT '大小(K)',
  file_url VARCHAR(500) COMMENT '文件路径',
  file_modified_time DATETIME COMMENT '文件修改时间',
  modified_by VARCHAR(50) COMMENT '修改人',
  modified_time DATETIME COMMENT '修改时间',
  created_by VARCHAR(50),
  created_time DATETIME,
  KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档附件表';

-- 9. 工单状态日志表
CREATE TABLE IF NOT EXISTS mes_work_order_status_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL COMMENT '工单ID',
  from_status VARCHAR(20) COMMENT '原状态',
  to_status VARCHAR(20) COMMENT '新状态',
  action VARCHAR(50) COMMENT '动作',
  operator VARCHAR(50) COMMENT '操作人',
  operated_time DATETIME COMMENT '操作时间',
  remark VARCHAR(500) COMMENT '说明',
  KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单状态日志表';
