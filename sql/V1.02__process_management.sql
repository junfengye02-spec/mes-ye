-- ============================================================
-- 模块：工艺管理
-- 表数量：14
-- ============================================================

-- 1. 指示书主表
CREATE TABLE IF NOT EXISTS mes_instruction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  instruction_no VARCHAR(100) NOT NULL COMMENT '指示书号',
  version VARCHAR(20) NOT NULL COMMENT '版本',
  status VARCHAR(20) COMMENT '状态',
  upgrade_from_id BIGINT COMMENT '来源版本ID',
  project_no VARCHAR(100) COMMENT '项目编号',
  wbs VARCHAR(100) COMMENT 'WBS',
  new_or_repair_type VARCHAR(50) COMMENT '新制维修类型',
  main_type VARCHAR(50) COMMENT '类型（如主机）',
  gt_type VARCHAR(50) COMMENT 'G/T类型',
  product_category VARCHAR(50) COMMENT '产品类别',
  product_type VARCHAR(50) COMMENT '产品类型',
  part_name VARCHAR(200) COMMENT '部件名称',
  work_order_no VARCHAR(100) COMMENT '生产订单编号',
  finish_date DATE COMMENT '生产完工日期',
  qty INT COMMENT '数量',
  issue_date DATE COMMENT '发行日期',
  final_delivery_date DATE COMMENT '产品最终交货期',
  check_submit_date DATE COMMENT '检查提交日期',
  drawing_no VARCHAR(200) COMMENT '项目·图纸号',
  repair_guide_drawing VARCHAR(200) COMMENT '维修指导图',
  assignee VARCHAR(100) COMMENT '担当',
  processing_status VARCHAR(50) COMMENT '加工状态',
  raw_material_arrival_date DATE COMMENT '原材料到货期',
  raw_material_purchase_name VARCHAR(200) COMMENT '原材料采购名义',
  purchase_request_no VARCHAR(100) COMMENT '采购申请单号',
  receive_time DATETIME COMMENT '接收时间',
  remark VARCHAR(500) COMMENT '备注',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_instruction_version (instruction_no, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指示书主表';

-- 2. 指示书阶段内容表
CREATE TABLE IF NOT EXISTS mes_instruction_stage (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  instruction_id BIGINT NOT NULL COMMENT '指示书ID',
  stage VARCHAR(50) COMMENT '阶段（收货前/收货检查时/收货检查报告后/维修）',
  role VARCHAR(50) COMMENT '角色（售后服务/制造&品管/制造/MHI/采购/品管等）',
  content TEXT COMMENT '内容',
  required_date DATE COMMENT '要求纳期',
  actual_date DATE COMMENT '实际纳期',
  created_time DATETIME,
  updated_time DATETIME,
  KEY idx_instruction_id (instruction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指示书阶段内容表';

-- 3. 指示书序列号表
CREATE TABLE IF NOT EXISTS mes_instruction_serial (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  instruction_id BIGINT NOT NULL COMMENT '指示书ID',
  product_type VARCHAR(50) COMMENT '产品类型',
  qty INT COMMENT '数量',
  receive_kg_code VARCHAR(100) COMMENT '接收K/G编码',
  send_g_code VARCHAR(100) COMMENT '发送时G编码',
  scheduled_check_time DATETIME COMMENT '定检时间',
  receive_time DATETIME COMMENT '接收时间',
  remark VARCHAR(500) COMMENT '备注',
  KEY idx_instruction_id (instruction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指示书序列号表';

-- 4. 指示书流程日志表
CREATE TABLE IF NOT EXISTS mes_instruction_flow_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  instruction_id BIGINT NOT NULL COMMENT '指示书ID',
  action VARCHAR(100) COMMENT '动作（创建/升级/删除/查看）',
  operator VARCHAR(50) COMMENT '操作人',
  operated_time DATETIME COMMENT '操作时间',
  detail VARCHAR(500) COMMENT '说明',
  KEY idx_instruction_id (instruction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指示书流程日志表';

-- 5. 工序模板表
CREATE TABLE IF NOT EXISTS mes_process_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  process_no VARCHAR(50) NOT NULL COMMENT '工序号',
  process_name VARCHAR(200) COMMENT '工序名',
  parent_process_no VARCHAR(50) COMMENT '父工序',
  product_category VARCHAR(50) COMMENT '产品类别',
  machine_model VARCHAR(100) COMMENT '机型',
  product_type VARCHAR(50) COMMENT '产品类型',
  process_type VARCHAR(50) COMMENT '工序类型（生产工序/检验工序）',
  process_form VARCHAR(100) COMMENT '工序过程表单',
  process_drawing VARCHAR(200) COMMENT '加工图纸',
  work_center_id BIGINT COMMENT '工作中心ID',
  handle_time DECIMAL(10,2) COMMENT '处理时间',
  remark VARCHAR(500) COMMENT '备注',
  description VARCHAR(500) COMMENT '说明',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_process_no (process_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序模板表';

-- 6. 工序信息表
CREATE TABLE IF NOT EXISTS mes_process_info (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  process_no VARCHAR(50) COMMENT '工序号',
  process_name VARCHAR(200) COMMENT '工序名',
  process_code VARCHAR(100) COMMENT '工艺编码',
  product VARCHAR(100) COMMENT '产品',
  g_code VARCHAR(100) COMMENT 'G编码',
  product_category VARCHAR(50) COMMENT '产品类别',
  machine_model VARCHAR(100) COMMENT '机型',
  product_type VARCHAR(50) COMMENT '产品类型',
  process_drawing VARCHAR(200) COMMENT '加工图纸',
  process_form VARCHAR(100) COMMENT '工序过程表单',
  process_template_id BIGINT COMMENT '工序模板ID',
  process_type VARCHAR(50) COMMENT '工序类型',
  factory VARCHAR(100) COMMENT '工厂',
  business_org VARCHAR(100) COMMENT '业务组织',
  work_center_id BIGINT COMMENT '工作中心ID',
  workshop_area VARCHAR(100) COMMENT '工段/区域',
  team_id BIGINT COMMENT '班组ID',
  need_strip TINYINT(1) DEFAULT 0 COMMENT '是否剥离',
  handle_time DECIMAL(10,2) COMMENT '处理时间',
  disassemble_time DECIMAL(10,2) COMMENT '拆卸时间',
  install_time DECIMAL(10,2) COMMENT '安装时间',
  remark VARCHAR(500) COMMENT '说明',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序信息表';

-- 7. 指导书表
CREATE TABLE IF NOT EXISTS mes_work_instruction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  instruction_code VARCHAR(100) NOT NULL COMMENT '指导书编号',
  level VARCHAR(20) COMMENT '等级',
  status VARCHAR(20) COMMENT '状态',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_instruction_code (instruction_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指导书表';

-- 8. 指导书人员表
CREATE TABLE IF NOT EXISTS mes_work_instruction_person (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  instruction_id BIGINT NOT NULL COMMENT '指导书ID',
  person_code VARCHAR(50) COMMENT '人员编号',
  person_name VARCHAR(100) COMMENT '姓名',
  person_category VARCHAR(50) COMMENT '人员分类',
  gender VARCHAR(10) COMMENT '性别',
  birth_date DATE COMMENT '出生日期',
  phone VARCHAR(50) COMMENT '手机号',
  email VARCHAR(100) COMMENT '邮箱',
  KEY idx_instruction_id (instruction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指导书人员表';

-- 9. 喷涂条件表
CREATE TABLE IF NOT EXISTS mes_spray_condition (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  condition_no VARCHAR(50) COMMENT '条件号',
  minister_approver VARCHAR(50) COMMENT '部长审批人',
  minister_approve_time DATETIME COMMENT '部长审批时间',
  section_approver VARCHAR(50) COMMENT '工段审批人',
  section_approve_time DATETIME COMMENT '工段审批时间',
  leader_approver VARCHAR(50) COMMENT '系长审批人',
  leader_approve_time DATETIME COMMENT '系长审批时间',
  powder_feed_rate DECIMAL(10,2) COMMENT '送粉量(g/min)',
  spray_distance DECIMAL(10,2) COMMENT '喷涂距离(mm)',
  spray_gun_model VARCHAR(100) COMMENT '喷枪型号',
  fai_report VARCHAR(200) COMMENT 'FAI报告书',
  fai_guide VARCHAR(200) COMMENT 'FAI要领书',
  powder_feeder VARCHAR(100) COMMENT '送粉器',
  powder_feeder_speed DECIMAL(10,2) COMMENT '送粉器转速(r/min)',
  oxygen_scfh DECIMAL(10,2) COMMENT '氧气(SCFH)',
  kerosene_gph DECIMAL(10,2) COMMENT '煤油(GPH)',
  combustion_pressure DECIMAL(10,2) COMMENT '燃烧压力(PSI)',
  carrier_gas VARCHAR(100) COMMENT '载气氮气',
  equipment VARCHAR(100) COMMENT '设备',
  powder_type VARCHAR(100) COMMENT '对应粉末',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_condition_no (condition_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='喷涂条件表';

-- 10. 机械加工程序表
CREATE TABLE IF NOT EXISTS mes_machining_program (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  g_code VARCHAR(100) NOT NULL COMMENT 'G-code',
  program_table VARCHAR(200) COMMENT '程序表',
  product_name VARCHAR(200) COMMENT '产品名称',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_g_code (g_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机械加工程序表';

-- 11. 制造BOM主表
CREATE TABLE IF NOT EXISTS mes_manufacturing_bom (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  bom_code VARCHAR(100) NOT NULL COMMENT 'BOM编码',
  bom_name VARCHAR(200) COMMENT 'BOM名称',
  product_id BIGINT COMMENT '产品ID',
  product_code VARCHAR(100) COMMENT '产品编码',
  product_name VARCHAR(200) COMMENT '产品名称',
  product_category VARCHAR(50) COMMENT '产品类别',
  machine_model VARCHAR(100) COMMENT '机型',
  product_type VARCHAR(50) COMMENT '产品类型',
  new_or_repair_type VARCHAR(50) COMMENT '新制维修类型',
  bom_version VARCHAR(20) NOT NULL COMMENT 'BOM版本',
  status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态（DRAFT/PUBLISHED/DISABLED）',
  effective_date DATE COMMENT '生效日期',
  expiry_date DATE COMMENT '失效日期',
  factory_org VARCHAR(100) COMMENT '工厂组织',
  upgrade_from_id BIGINT COMMENT '来源版本ID',
  remark VARCHAR(500) COMMENT '备注',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_bom_code_version (bom_code, bom_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='制造BOM主表';

-- 12. 制造BOM明细表
CREATE TABLE IF NOT EXISTS mes_manufacturing_bom_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  bom_id BIGINT NOT NULL COMMENT 'BOM主表ID',
  parent_item_id BIGINT COMMENT '父级明细ID（支持多层嵌套）',
  level INT COMMENT '层级',
  material_id BIGINT COMMENT '物料ID',
  material_code VARCHAR(100) COMMENT '物料编码',
  material_name VARCHAR(200) COMMENT '物料名称',
  material_spec VARCHAR(200) COMMENT '物料规格',
  material_type VARCHAR(50) COMMENT '物料类型（原材料/半成品/辅料/标准件）',
  quantity DECIMAL(18,6) COMMENT '用量（单位产品用量）',
  loss_rate DECIMAL(5,2) COMMENT '损耗率(%)',
  unit VARCHAR(20) COMMENT '计量单位',
  supply_type VARCHAR(20) COMMENT '供应类型（自制/采购/委外）',
  process_id BIGINT COMMENT '关联工序ID',
  process_no VARCHAR(50) COMMENT '关联工序号',
  is_substitute TINYINT(1) DEFAULT 0 COMMENT '替代料标识',
  substitute_group VARCHAR(50) COMMENT '替代料组',
  is_key_part TINYINT(1) DEFAULT 0 COMMENT '是否关键件',
  sequence_no INT COMMENT '排序号',
  remark VARCHAR(500) COMMENT '备注',
  created_time DATETIME,
  updated_time DATETIME,
  KEY idx_bom_id (bom_id),
  KEY idx_parent_item_id (parent_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='制造BOM明细表';

-- 13. BOM替代料表
CREATE TABLE IF NOT EXISTS mes_bom_substitute (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  bom_item_id BIGINT NOT NULL COMMENT 'BOM明细ID',
  main_material_id BIGINT COMMENT '主物料ID',
  main_material_code VARCHAR(100) COMMENT '主物料编码',
  substitute_material_id BIGINT COMMENT '替代物料ID',
  substitute_material_code VARCHAR(100) COMMENT '替代物料编码',
  substitute_material_name VARCHAR(200) COMMENT '替代物料名称',
  priority INT COMMENT '替代优先级',
  substitute_ratio DECIMAL(5,2) COMMENT '替代比例',
  effective_date DATE COMMENT '生效日期',
  expiry_date DATE COMMENT '失效日期',
  remark VARCHAR(500) COMMENT '备注',
  created_time DATETIME,
  updated_time DATETIME,
  KEY idx_bom_item_id (bom_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM替代料表';

-- 14. BOM版本日志表
CREATE TABLE IF NOT EXISTS mes_bom_version_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  bom_id BIGINT NOT NULL COMMENT 'BOM主表ID',
  from_version VARCHAR(20) COMMENT '原版本',
  to_version VARCHAR(20) COMMENT '新版本',
  action VARCHAR(50) COMMENT '动作（创建/升级/发布/停用）',
  operator VARCHAR(50) COMMENT '操作人',
  operated_time DATETIME COMMENT '操作时间',
  change_summary TEXT COMMENT '变更摘要',
  KEY idx_bom_id (bom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM版本日志表';
