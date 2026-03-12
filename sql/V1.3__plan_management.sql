-- ============================================================
-- 模块：计划管理
-- 表数量：3
-- ============================================================

-- 1. 订单计划主表
CREATE TABLE IF NOT EXISTS mes_order_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(100) NOT NULL COMMENT '订单号',
  product_code VARCHAR(100) COMMENT '产品编码',
  product_name VARCHAR(200) COMMENT '产品名称',
  project_name VARCHAR(200) COMMENT '项目',
  wbs_element VARCHAR(100) COMMENT 'WBS元素',
  new_or_repair_type VARCHAR(50) COMMENT '新制维修类型',
  work_type VARCHAR(50) COMMENT '类型（维修/检查/主机）',
  machine_model VARCHAR(100) COMMENT '机型',
  product_category VARCHAR(50) COMMENT '产品类别',
  product_type VARCHAR(50) COMMENT '产品类型',
  plan_qty DECIMAL(18,4) COMMENT '计划数量',
  qty_unit VARCHAR(20) COMMENT '数量单位',
  factory_org VARCHAR(100) COMMENT '工厂组织',
  plan_org VARCHAR(100) COMMENT '计划组织',
  main_org VARCHAR(100) COMMENT '主制组织',
  plan_work_center_id BIGINT COMMENT '计划工作中心',
  status VARCHAR(20) DEFAULT 'CREATED' COMMENT '状态（CREATED/RELEASED/COMPLETED/TERMINATED）',
  flow_status VARCHAR(20) COMMENT '流程状态（RUNNING/COMPLETED/TERMINATED）',
  expand_status VARCHAR(20) DEFAULT 'UNEXPANDED' COMMENT '展开状态（UNEXPANDED/EXPANDED）',
  completion_status VARCHAR(20) DEFAULT 'NOT_STARTED' COMMENT '完工状态（NOT_STARTED/APPROVED）',
  is_order TINYINT(1) DEFAULT 0 COMMENT '是否订单',
  pccl_flow VARCHAR(100) COMMENT 'PCCL流程',
  plan_start_time DATETIME COMMENT '计划开始时间',
  plan_end_time DATETIME COMMENT '计划结束时间',
  actual_start_time DATETIME COMMENT '实际开始时间',
  actual_end_time DATETIME COMMENT '实际结束时间',
  data_source VARCHAR(20) DEFAULT 'MANUAL' COMMENT '数据来源（MANUAL/APS）',
  aps_order_id BIGINT COMMENT 'APS订单ID',
  aps_sync_batch_id VARCHAR(64) COMMENT 'APS同步批次号',
  aps_sync_status VARCHAR(20) COMMENT 'APS同步状态（SYNCED/PENDING/FAILED）',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单计划主表';

-- 2. 生产计划主表
CREATE TABLE IF NOT EXISTS mes_production_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_plan_id BIGINT COMMENT '订单计划ID',
  order_no VARCHAR(100) COMMENT '订单编号',
  product_code VARCHAR(100) COMMENT '产品编码',
  product_name VARCHAR(200) COMMENT '产品名称',
  new_or_repair_type VARCHAR(50) COMMENT '新制维修类型',
  work_type VARCHAR(50) COMMENT '类型',
  machine_model VARCHAR(100) COMMENT '机型',
  product_category VARCHAR(50) COMMENT '产品类别',
  product_type VARCHAR(50) COMMENT '产品类型',
  wbs_element VARCHAR(100) COMMENT 'WBS元素',
  work_order_type VARCHAR(50) COMMENT '计划工单类型',
  plan_org VARCHAR(100) COMMENT '计划组织',
  plan_qty DECIMAL(18,4) COMMENT '计划数量',
  qty_unit VARCHAR(20) COMMENT '数量单位',
  completed_qty DECIMAL(18,4) DEFAULT 0 COMMENT '完工数量',
  status VARCHAR(20) DEFAULT 'CREATED' COMMENT '状态（CREATED/RELEASED）',
  plan_start_time DATETIME COMMENT '计划开始时间',
  plan_end_time DATETIME COMMENT '计划完成时间',
  actual_start_time DATETIME COMMENT '实际开始时间',
  actual_end_time DATETIME COMMENT '实际完成时间',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  KEY idx_order_plan_id (order_plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产计划主表';

-- 3. 计划状态日志表
CREATE TABLE IF NOT EXISTS mes_plan_status_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_type VARCHAR(20) NOT NULL COMMENT '计划类型（ORDER/PRODUCTION）',
  plan_id BIGINT NOT NULL COMMENT '计划ID',
  from_status VARCHAR(20) COMMENT '原状态',
  to_status VARCHAR(20) COMMENT '新状态',
  action VARCHAR(50) COMMENT '动作',
  operator VARCHAR(50) COMMENT '操作人',
  operated_time DATETIME COMMENT '操作时间',
  remark VARCHAR(500) COMMENT '说明',
  KEY idx_plan_type_id (plan_type, plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划状态日志表';
