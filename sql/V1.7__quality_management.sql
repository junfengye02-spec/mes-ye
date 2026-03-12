-- ============================================================
-- 模块：成品质量管理
-- 表数量：6
-- ============================================================

-- 1. 复检申请主表
CREATE TABLE IF NOT EXISTS mes_recheck_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_code VARCHAR(100) COMMENT '项目编码',
  project_name VARCHAR(200) COMMENT '项目名称',
  material_code VARCHAR(100) COMMENT '物料编码',
  material_name VARCHAR(200) COMMENT '物料名称',
  production_order_no VARCHAR(100) COMMENT '生产订单',
  recheck_requirement VARCHAR(500) COMMENT '复检需求',
  recheck_reason VARCHAR(500) COMMENT '复检原因',
  recheck_proposer VARCHAR(50) COMMENT '复检提出人',
  recheck_propose_time DATETIME COMMENT '复检提出时间',
  required_delivery_time DATETIME COMMENT '需求交货时间',
  is_reasonable TINYINT(1) COMMENT '是否合理',
  reviewer VARCHAR(50) COMMENT '审核人员',
  review_date DATE COMMENT '审核日期',
  status VARCHAR(20) DEFAULT 'CREATED' COMMENT '状态',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复检申请主表';

-- 2. 复检申请订单计划关联表
CREATE TABLE IF NOT EXISTS mes_recheck_order_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  recheck_id BIGINT NOT NULL COMMENT '复检申请ID',
  order_plan_id BIGINT COMMENT '订单计划ID',
  related_object VARCHAR(200) COMMENT '被关联对象',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  KEY idx_recheck_id (recheck_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复检申请订单计划关联表';

-- 3. 复检申请产品序列号表
CREATE TABLE IF NOT EXISTS mes_recheck_serial (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  recheck_id BIGINT NOT NULL COMMENT '复检申请ID',
  serial_no VARCHAR(100) COMMENT '序列号',
  manufacturer VARCHAR(200) COMMENT '生产厂商',
  name VARCHAR(200) COMMENT '名称',
  status_category VARCHAR(50) COMMENT '状态分类',
  qty DECIMAL(18,4) COMMENT '数量',
  frozen TINYINT(1) DEFAULT 0 COMMENT '冻结',
  split_completed TINYINT(1) DEFAULT 0 COMMENT '拆分完成',
  unit VARCHAR(20) COMMENT '计量单位',
  barcode VARCHAR(100) COMMENT '条码号',
  KEY idx_recheck_id (recheck_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复检申请产品序列号表';

-- 4. 生产工作开工检查表
CREATE TABLE IF NOT EXISTS mes_work_start_check (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_no VARCHAR(100) COMMENT '工作编号',
  work_order_task_id BIGINT COMMENT '工作清单ID',
  work_order_id BIGINT COMMENT '工单ID',
  work_order_no VARCHAR(100) COMMENT '工单号',
  check_item VARCHAR(200) COMMENT '检查项目',
  check_result VARCHAR(50) COMMENT '检查结果',
  check_status VARCHAR(20) COMMENT '开工检查状态（PASSED/FAILED）',
  check_remark VARCHAR(500) COMMENT '开工检查备注',
  checker VARCHAR(50) COMMENT '检查人',
  check_time DATETIME COMMENT '检查时间',
  remark VARCHAR(500) COMMENT '备注',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)',
  KEY idx_work_order_id (work_order_id),
  KEY idx_work_order_task_id (work_order_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产工作开工检查表';

-- 5. 生产工单开工检查表
CREATE TABLE IF NOT EXISTS mes_order_start_check (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT COMMENT '工单ID',
  work_order_no VARCHAR(100) COMMENT '工单号',
  check_item VARCHAR(200) COMMENT '检查项目',
  check_result VARCHAR(50) COMMENT '检查结果',
  check_status VARCHAR(20) COMMENT '开工检查状态（PASSED/FAILED）',
  check_remark VARCHAR(500) COMMENT '开工检查备注',
  checker VARCHAR(50) COMMENT '检查人',
  check_time DATETIME COMMENT '检查时间',
  remark VARCHAR(500) COMMENT '备注',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)',
  KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产工单开工检查表';

-- 6. 交班记录表
CREATE TABLE IF NOT EXISTS mes_shift_handover (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_name VARCHAR(200) COMMENT '项目名称',
  product_serial_no VARCHAR(100) COMMENT '产品序列号',
  process_content VARCHAR(500) COMMENT '工序内容',
  handover_date DATE COMMENT '交班日期',
  handover_weekday INT COMMENT '发起星期',
  handover_time TIME COMMENT '发起时间',
  handover_team_id BIGINT COMMENT '发起班组ID',
  handover_team_name VARCHAR(200) COMMENT '发起班组',
  handover_shift VARCHAR(50) COMMENT '发起班次',
  takeover_shift VARCHAR(50) COMMENT '接收班次',
  takeover_team_id BIGINT COMMENT '接班班组ID',
  takeover_team_name VARCHAR(200) COMMENT '接班班组',
  handover_person VARCHAR(50) COMMENT '交接人员',
  takeover_person VARCHAR(50) COMMENT '接班人员',
  team_leader VARCHAR(50) COMMENT '班组长',
  plan_qty DECIMAL(18,4) COMMENT '计划数量',
  actual_qty DECIMAL(18,4) COMMENT '实际完成',
  gap_analysis VARCHAR(500) COMMENT '未达标分析',
  handover_content TEXT COMMENT '交班内容',
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态（PENDING/RECEIVED）',
  other_matters TEXT COMMENT '其它需要交付事宜',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交班记录表';
