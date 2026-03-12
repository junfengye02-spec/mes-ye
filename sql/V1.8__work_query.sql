-- ============================================================
-- 模块：工作查询
-- 表数量：4（新增表，部分表与质量/工单模块共用）
-- 共用表：mes_work_start_check, mes_order_start_check,
--         mes_shift_handover（已在 V1.7 创建）
-- ============================================================

-- 1. 交班记录附件表
CREATE TABLE IF NOT EXISTS mes_shift_handover_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  handover_id BIGINT NOT NULL COMMENT '交班记录ID',
  file_name VARCHAR(200) COMMENT '文件名',
  file_url VARCHAR(500) COMMENT '文件路径',
  file_type VARCHAR(50) COMMENT '文件类型',
  file_size VARCHAR(50) COMMENT '文件大小',
  uploader VARCHAR(50) COMMENT '上传人',
  download_count INT DEFAULT 0 COMMENT '下载次数',
  load_status VARCHAR(20) DEFAULT 'LOADED' COMMENT '状态（LOADED）',
  created_by VARCHAR(50),
  created_time DATETIME,
  KEY idx_handover_id (handover_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交班记录附件表';

-- 2. 生产工作表
CREATE TABLE IF NOT EXISTS mes_production_work (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_no VARCHAR(100) NOT NULL COMMENT '工作编号',
  work_name VARCHAR(200) NOT NULL COMMENT '工作名称',
  work_order_id BIGINT COMMENT '生产工单ID',
  work_order_no VARCHAR(100) COMMENT '生产工单号',
  product_material VARCHAR(200) COMMENT '产品物料',
  production_factory VARCHAR(100) COMMENT '生产工厂',
  production_org VARCHAR(100) COMMENT '生产组织',
  actual_start_time DATETIME COMMENT '实际开始时间',
  actual_end_time DATETIME COMMENT '实际结束时间',
  plan_start_time DATETIME COMMENT '计划开始时间',
  plan_end_time DATETIME COMMENT '计划结束时间',
  actual_process_time DECIMAL(10,2) COMMENT '实际处理时间',
  time_unit VARCHAR(20) DEFAULT '分钟' COMMENT '时间单位',
  is_report_point TINYINT(1) DEFAULT 0 COMMENT '报告点',
  is_check_point TINYINT(1) DEFAULT 0 COMMENT '检验点',
  is_handover_point TINYINT(1) DEFAULT 0 COMMENT '交接点',
  remark VARCHAR(500) COMMENT '备注',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)',
  KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产工作表';

-- 3. 检验工作表
CREATE TABLE IF NOT EXISTS mes_inspection_work (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_no VARCHAR(100) NOT NULL COMMENT '工作编号',
  work_name VARCHAR(200) NOT NULL COMMENT '工作名称',
  plan_inspect_qty DECIMAL(18,4) COMMENT '计划检验数量',
  inspected_qty DECIMAL(18,4) DEFAULT 0 COMMENT '已检数量',
  qualified_qty DECIMAL(18,4) DEFAULT 0 COMMENT '合格数量',
  unqualified_qty DECIMAL(18,4) DEFAULT 0 COMMENT '不合格数量',
  judgment VARCHAR(50) COMMENT '判定',
  is_check_point TINYINT(1) DEFAULT 0 COMMENT '检验点',
  dispatch_status VARCHAR(20) COMMENT '分派状态',
  sampling_in_inspect VARCHAR(100) COMMENT '检验中取样',
  work_status VARCHAR(20) DEFAULT 'CREATED' COMMENT '工作状态',
  inspect_type VARCHAR(50) COMMENT '检验类',
  inspect_category VARCHAR(50) COMMENT '检验类型',
  qc_org VARCHAR(100) COMMENT '质检组织',
  inspect_factory VARCHAR(100) COMMENT '检验工厂',
  plan_team_lab VARCHAR(100) COMMENT '计划班组/检测室',
  actual_start_time DATETIME COMMENT '实际开始时间',
  actual_end_time DATETIME COMMENT '实际完成时间',
  is_report_point TINYINT(1) DEFAULT 0 COMMENT '报告点',
  work_order_id BIGINT COMMENT '所属工单ID',
  work_order_no VARCHAR(100) COMMENT '所属工单号',
  order_status VARCHAR(20) COMMENT '工单状态',
  description VARCHAR(500) COMMENT '说明',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)',
  KEY idx_work_order_id (work_order_id),
  KEY idx_work_status (work_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验工作表';

-- 4. 工作状态查看视图（物理表，数据源自 mes_work_order_task 按状态分类展示）
-- 说明：此表为宽表/视图备用，实际查询可直接从 mes_work_order_task 联合查询
CREATE TABLE IF NOT EXISTS mes_work_status_view (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_no VARCHAR(100) COMMENT '编号',
  sequence_no INT COMMENT '顺序号',
  process_no VARCHAR(50) COMMENT '工序号',
  work_name VARCHAR(200) COMMENT '名称',
  is_output TINYINT(1) DEFAULT 0 COMMENT '是否产出',
  process_form VARCHAR(100) COMMENT '工序过程表单',
  process_drawing VARCHAR(200) COMMENT '加工图纸',
  status VARCHAR(20) COMMENT '状态（CREATED/RELEASED/ISSUED/IN_PROGRESS/COMPLETED/PAUSED）',
  description VARCHAR(500) COMMENT '说明',
  furnace_no VARCHAR(100) COMMENT '组炉号',
  belong_process VARCHAR(100) COMMENT '所属工序',
  factory VARCHAR(100) COMMENT '工厂',
  business_org VARCHAR(100) COMMENT '业务组织',
  plan_section VARCHAR(100) COMMENT '计划工段',
  plan_work_center_id BIGINT COMMENT '计划工作中心ID',
  plan_work_center_name VARCHAR(200) COMMENT '计划工作中心',
  specified_section VARCHAR(100) COMMENT '指定工段',
  specified_work_center_id BIGINT COMMENT '指定工作中心ID',
  specified_work_center_name VARCHAR(200) COMMENT '指定工作中心',
  plan_team_id BIGINT COMMENT '计划班组ID',
  plan_team_name VARCHAR(200) COMMENT '计划班组',
  plan_shift VARCHAR(50) COMMENT '计划班次',
  source_no VARCHAR(100) COMMENT '来源单号',
  time_unit VARCHAR(20) DEFAULT '分钟' COMMENT '时间单位',
  created_time DATETIME COMMENT '创建时间',
  plan_start_time DATETIME COMMENT '计划开始时间',
  plan_end_time DATETIME COMMENT '计划结束时间',
  actual_start_time DATETIME COMMENT '实际开始时间',
  actual_end_time DATETIME COMMENT '实际完成时间',
  approval_remark VARCHAR(500) COMMENT '审批备注',
  issued TINYINT(1) DEFAULT 0 COMMENT '下发',
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作状态查看表';
