-- ============================================================
-- 模块：物料管理
-- 表数量：9
-- ============================================================

-- 1. 存储地点库存表
CREATE TABLE IF NOT EXISTS mes_storage_inventory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  factory VARCHAR(100) COMMENT '工厂',
  inventory_org VARCHAR(100) COMMENT '存货组织',
  warehouse VARCHAR(100) COMMENT '仓库',
  storage_location VARCHAR(100) COMMENT '存储地点',
  material_id BIGINT COMMENT '物料ID',
  material_code VARCHAR(100) COMMENT '物料编码',
  material_name VARCHAR(200) COMMENT '物料名称',
  unrestricted_stock DECIMAL(18,4) DEFAULT 0 COMMENT '非限制库存',
  quality_stock DECIMAL(18,4) DEFAULT 0 COMMENT '质检库存',
  frozen_stock DECIMAL(18,4) DEFAULT 0 COMMENT '冻结库存',
  unit VARCHAR(20) COMMENT '计量单位',
  team_id BIGINT COMMENT '班组ID',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)',
  KEY idx_material_code (material_code),
  KEY idx_storage_location (storage_location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储地点库存表';

-- 2. 生产领料申请表
CREATE TABLE IF NOT EXISTS mes_material_requisition (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  requisition_no VARCHAR(100) COMMENT '领料单号',
  work_order_id BIGINT NOT NULL COMMENT '工单ID',
  work_order_no VARCHAR(100) COMMENT '工单编号',
  product_code VARCHAR(100) COMMENT '产品编码',
  product_name VARCHAR(200) COMMENT '产品名称',
  plan_qty DECIMAL(18,4) COMMENT '计划数量',
  actual_qty DECIMAL(18,4) COMMENT '实际生产数量',
  qualified_qty DECIMAL(18,4) COMMENT '合格数量',
  qty_unit VARCHAR(20) COMMENT '数量单位',
  main_org VARCHAR(100) COMMENT '主制组织',
  plan_start_time DATETIME COMMENT '计划开始时间',
  plan_end_time DATETIME COMMENT '计划结束时间',
  actual_start_time DATETIME COMMENT '实际开始时间',
  actual_end_time DATETIME COMMENT '实际结束时间',
  sales_order_line VARCHAR(100) COMMENT '销售订单行',
  project_name VARCHAR(200) COMMENT '项目',
  wbs_element VARCHAR(100) COMMENT 'WBS元素',
  status VARCHAR(20) DEFAULT 'CREATED' COMMENT '状态',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)',
  KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产领料申请表';

-- 3. 生产领料明细表
CREATE TABLE IF NOT EXISTS mes_material_requisition_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  requisition_id BIGINT NOT NULL COMMENT '领料申请ID',
  work_order_id BIGINT COMMENT '工单ID',
  work_id BIGINT COMMENT '工作ID',
  material_id BIGINT COMMENT '物料ID',
  material_code VARCHAR(100) COMMENT '物料编码',
  material_name VARCHAR(200) COMMENT '物料名称',
  demand_qty DECIMAL(18,4) COMMENT '需求数量',
  pending_qty DECIMAL(18,4) COMMENT '待领料数量',
  issue_qty DECIMAL(18,4) COMMENT '本次领料数量',
  unit VARCHAR(20) COMMENT '计量单位',
  issue_location VARCHAR(100) COMMENT '发货地点',
  demand_time DATETIME COMMENT '需求时间',
  description VARCHAR(500) COMMENT '说明',
  is_final TINYINT(1) DEFAULT 0 COMMENT '最终标识',
  created_time DATETIME,
  updated_time DATETIME,
  KEY idx_requisition_id (requisition_id),
  KEY idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产领料明细表';

-- 4. 完工入库单主表
CREATE TABLE IF NOT EXISTS mes_finished_goods_receipt (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  receipt_no VARCHAR(100) COMMENT '收货单号',
  receipt_type VARCHAR(50) COMMENT '收货类型（新制品/维修品/不可维修品）',
  warehouse VARCHAR(100) COMMENT '仓库',
  movement_type VARCHAR(50) COMMENT '移动类型',
  plan_receipt_time DATETIME COMMENT '计划收货时间',
  actual_receipt_time DATETIME COMMENT '实际收货时间',
  status VARCHAR(20) DEFAULT 'CREATED' COMMENT '状态',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)',
  UNIQUE KEY uk_receipt_no (receipt_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='完工入库单主表';

-- 5. 完工入库明细表
CREATE TABLE IF NOT EXISTS mes_finished_goods_receipt_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  receipt_id BIGINT NOT NULL COMMENT '入库单ID',
  item_code VARCHAR(100) COMMENT '编码',
  work_order_id BIGINT COMMENT '生产工单ID',
  work_order_no VARCHAR(100) COMMENT '生产工单号',
  material_id BIGINT COMMENT '物料ID',
  material_code VARCHAR(100) COMMENT '物料编码',
  material_name VARCHAR(200) COMMENT '物料名称',
  receipt_qty DECIMAL(18,4) COMMENT '收货数量',
  unit VARCHAR(20) COMMENT '计量单位',
  storage_location VARCHAR(100) COMMENT '存储地点',
  staging_area VARCHAR(100) COMMENT '收货暂存区',
  staging_bin VARCHAR(100) COMMENT '收货暂存货位',
  stock_status VARCHAR(50) COMMENT '库存状态',
  special_stock VARCHAR(50) COMMENT '特殊库存',
  customer VARCHAR(100) COMMENT '客户',
  wbs_element VARCHAR(100) COMMENT 'WBS元素',
  sales_order_line VARCHAR(100) COMMENT '销售订单行',
  variance_qty DECIMAL(18,4) COMMENT '差异数量',
  variance_reason VARCHAR(500) COMMENT '差异原因',
  KEY idx_receipt_id (receipt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='完工入库明细表';

-- 6. 生产领料单管理表
CREATE TABLE IF NOT EXISTS mes_requisition_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_node VARCHAR(100) COMMENT '上级节点',
  delivery_request_no VARCHAR(100) COMMENT '发货申请',
  line_no VARCHAR(50) COMMENT '行号',
  work_order_id BIGINT COMMENT '生产工单ID',
  work_order_no VARCHAR(100) COMMENT '生产工单号',
  delivery_order_created TINYINT(1) DEFAULT 0 COMMENT '已制发货单',
  material_id BIGINT COMMENT '物料ID',
  material_code VARCHAR(100) COMMENT '物料编码',
  material_name VARCHAR(200) COMMENT '物料名称',
  warehouse_delivered TINYINT(1) DEFAULT 0 COMMENT '仓库已发货',
  requisition_qty DECIMAL(18,4) COMMENT '领料数量',
  status VARCHAR(20) COMMENT '状态',
  delivery_warehouse VARCHAR(100) COMMENT '发货仓库',
  delivery_location VARCHAR(100) COMMENT '发货存储地点',
  work_id BIGINT COMMENT '工作ID',
  work_station VARCHAR(100) COMMENT '工位',
  material_demand_id BIGINT COMMENT '物料需求ID',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)',
  KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产领料单管理表';

-- 7. 完工入库申请表
CREATE TABLE IF NOT EXISTS mes_finished_goods_receipt_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_no VARCHAR(100) COMMENT '申请单号',
  receipt_type VARCHAR(50) COMMENT '入库类型（新制品/维修品/不可维修品）',
  work_order_id BIGINT COMMENT '生产工单ID',
  work_order_no VARCHAR(100) COMMENT '生产工单号',
  project_name VARCHAR(200) COMMENT '项目',
  wbs_element VARCHAR(100) COMMENT 'WBS元素',
  material_id BIGINT COMMENT '物料ID',
  material_code VARCHAR(100) COMMENT '物料编码',
  material_name VARCHAR(200) COMMENT '物料名称',
  serial_no VARCHAR(100) COMMENT '序列号',
  qty DECIMAL(18,4) COMMENT '数量',
  unqualified_qty DECIMAL(18,4) COMMENT '不合格数量',
  qualified_qty DECIMAL(18,4) COMMENT '合格数量',
  unit VARCHAR(20) COMMENT '计量单位',
  description VARCHAR(500) COMMENT '说明',
  plan_receipt_time DATETIME COMMENT '计划入库时间',
  actual_production_qty DECIMAL(18,4) COMMENT '实际生产数量',
  pending_receipt_qty DECIMAL(18,4) COMMENT '待收料数量',
  status VARCHAR(20) DEFAULT 'CREATED' COMMENT '状态',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)',
  KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='完工入库申请表';

-- 8. 生产退料申请表
CREATE TABLE IF NOT EXISTS mes_material_return (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  return_no VARCHAR(100) COMMENT '退料单号',
  work_order_id BIGINT COMMENT '工单ID',
  work_order_no VARCHAR(100) COMMENT '工单号',
  order_no VARCHAR(100) COMMENT '订单号',
  product_code VARCHAR(100) COMMENT '产品编码',
  product_name VARCHAR(200) COMMENT '产品名称',
  project_name VARCHAR(200) COMMENT '项目',
  wbs_element VARCHAR(100) COMMENT 'WBS元素',
  new_or_repair_type VARCHAR(50) COMMENT '新制维修类型',
  business_type VARCHAR(50) COMMENT '业务类型',
  machine_model VARCHAR(100) COMMENT '机型',
  product_category VARCHAR(50) COMMENT '产品类别',
  product_type VARCHAR(50) COMMENT '产品类型',
  plan_qty DECIMAL(18,4) COMMENT '计划数量',
  completed_qty DECIMAL(18,4) COMMENT '完工数量',
  factory_org VARCHAR(100) COMMENT '工厂组织',
  plan_org VARCHAR(100) COMMENT '计划组织',
  main_org VARCHAR(100) COMMENT '主制组织',
  plan_work_center_id BIGINT COMMENT '计划工作中心',
  status VARCHAR(20) COMMENT '状态',
  flow_status VARCHAR(20) COMMENT '流程状态',
  expand_status VARCHAR(20) COMMENT '展开状态',
  is_order TINYINT(1) DEFAULT 0 COMMENT '是否订单',
  flow_code VARCHAR(100) COMMENT '流程编码',
  plan_start_time DATETIME COMMENT '计划开始时间',
  plan_end_time DATETIME COMMENT '计划结束时间',
  actual_start_time DATETIME COMMENT '实际开始时间',
  actual_end_time DATETIME COMMENT '实际结束时间',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)',
  KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产退料申请表';

-- 9. 发货签收表
CREATE TABLE IF NOT EXISTS mes_delivery_sign (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  line_no VARCHAR(50) COMMENT '行号',
  work_order_id BIGINT COMMENT '工单ID',
  work_order_no VARCHAR(100) COMMENT '工单号',
  material_id BIGINT COMMENT '物料ID',
  material_code VARCHAR(100) COMMENT '物料编码',
  material_name VARCHAR(200) COMMENT '物料名称',
  plan_delivery_qty DECIMAL(18,4) COMMENT '计划发货数量',
  pending_sign_qty DECIMAL(18,4) COMMENT '待签收数量',
  unit VARCHAR(20) COMMENT '计量单位',
  delivery_warehouse VARCHAR(100) COMMENT '发货仓库',
  delivery_location VARCHAR(100) COMMENT '发货存储地点',
  order_creator VARCHAR(50) COMMENT '制单人',
  order_create_time DATETIME COMMENT '制单时间',
  deliverer VARCHAR(50) COMMENT '发货人',
  delivery_time DATETIME COMMENT '发货时间',
  created_time DATETIME,
  updated_time DATETIME,
  KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发货签收表';
