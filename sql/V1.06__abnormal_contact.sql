-- ============================================================
-- 模块：异常联络单管理
-- 表数量：3
-- ============================================================

-- 1. 异常联络单主表
CREATE TABLE IF NOT EXISTS mes_abnormal_contact (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contact_no VARCHAR(100) NOT NULL COMMENT '异常联络单号',
  subject VARCHAR(500) COMMENT '主题',
  occur_stage VARCHAR(50) COMMENT '发生阶段',
  event_category VARCHAR(50) COMMENT '事件分类',
  product_division VARCHAR(50) COMMENT '产品区分',
  order_no VARCHAR(100) COMMENT '订单号',
  customer_project VARCHAR(200) COMMENT '客户/项目',
  initiate_dept VARCHAR(100) COMMENT '发起部门',
  product_model VARCHAR(100) COMMENT '产品型号',
  product_type VARCHAR(50) COMMENT '产品类型',
  product_name VARCHAR(200) COMMENT '产品名称',
  initiate_process VARCHAR(100) COMMENT '发起工序',
  qty DECIMAL(18,4) COMMENT '数量',
  storage_location VARCHAR(200) COMMENT '实物存放点',
  discovery_date DATE COMMENT '发现日期',
  abnormal_desc TEXT COMMENT '异常描述',
  status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态（DRAFT/SUBMITTED/PROCESSING/CLOSED）',
  affect_schedule TINYINT(1) DEFAULT 0 COMMENT '是否影响排程',
  publish_time DATETIME COMMENT '发布时间',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_contact_no (contact_no),
  KEY idx_event_category (event_category),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常联络单主表';

-- 2. 异常联络单附件表
CREATE TABLE IF NOT EXISTS mes_abnormal_contact_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contact_id BIGINT NOT NULL COMMENT '异常联络单ID',
  file_no VARCHAR(100) COMMENT '文件编号',
  file_name VARCHAR(200) COMMENT '文件名',
  file_url VARCHAR(500) COMMENT '文件路径',
  file_type VARCHAR(50) COMMENT '文件类型',
  responsible_person VARCHAR(50) COMMENT '负责人',
  team VARCHAR(100) COMMENT '团队',
  publish_time DATETIME COMMENT '发布时间',
  submit_time DATETIME COMMENT '提交时间',
  fadada_flag VARCHAR(50) COMMENT '法大大标识',
  signed TINYINT(1) DEFAULT 0 COMMENT '已签',
  created_time DATETIME,
  updated_time DATETIME,
  KEY idx_contact_id (contact_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常联络单附件表';

-- 3. 异常联络单状态日志表
CREATE TABLE IF NOT EXISTS mes_abnormal_contact_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contact_id BIGINT NOT NULL COMMENT '异常联络单ID',
  from_status VARCHAR(20) COMMENT '原状态',
  to_status VARCHAR(20) COMMENT '新状态',
  action VARCHAR(50) COMMENT '动作',
  operator VARCHAR(50) COMMENT '操作人',
  operated_time DATETIME COMMENT '操作时间',
  remark VARCHAR(500) COMMENT '说明',
  KEY idx_contact_id (contact_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常联络单状态日志表';
