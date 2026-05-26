-- ============================================================
-- 模块：工艺路线
-- 说明：新增显式 Route / RouteStep 模型，用于工单任务展开和 APS 路线同步
-- ============================================================

CREATE TABLE IF NOT EXISTS mes_route (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  route_code VARCHAR(100) NOT NULL COMMENT '路线编码',
  route_name VARCHAR(200) COMMENT '路线名称',
  product_code VARCHAR(100) COMMENT '产品编码',
  product_category VARCHAR(100) COMMENT '产品类别',
  machine_model VARCHAR(100) COMMENT '机型',
  product_type VARCHAR(100) COMMENT '产品类型',
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态（DRAFT/ACTIVE/DISABLED）',
  effective_date DATE COMMENT '生效日期',
  expiry_date DATE COMMENT '失效日期',
  remark VARCHAR(500) COMMENT '备注',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_route_tenant_code (tenant_id, route_code),
  KEY idx_route_product_code (tenant_id, product_code),
  KEY idx_route_product_match (tenant_id, product_category, machine_model),
  KEY idx_route_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺路线主表';

CREATE TABLE IF NOT EXISTS mes_route_step (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  route_id BIGINT NOT NULL COMMENT '路线ID',
  sequence_no INT NOT NULL COMMENT '顺序号',
  process_id BIGINT NOT NULL COMMENT '工序ID',
  process_no VARCHAR(50) COMMENT '工序号',
  process_name VARCHAR(200) COMMENT '工序名称',
  work_center_id BIGINT COMMENT '工作中心ID',
  handle_time DECIMAL(10,2) COMMENT '标准处理时间',
  predecessor_step_id BIGINT COMMENT '前置步骤ID',
  parallel_flag TINYINT(1) DEFAULT 0 COMMENT '是否并行',
  optional_flag TINYINT(1) DEFAULT 0 COMMENT '是否可选',
  remark VARCHAR(500) COMMENT '备注',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_route_step_sequence (tenant_id, route_id, sequence_no),
  KEY idx_route_step_route (tenant_id, route_id),
  KEY idx_route_step_process (tenant_id, process_id),
  KEY idx_route_step_work_center (tenant_id, work_center_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺路线步骤表';
