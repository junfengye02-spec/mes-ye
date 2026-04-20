-- ============================================================
-- 模块：基础数据
-- 表数量：3
-- 说明：此文件为 Flyway V1.0 迁移脚本，替代原 init-basic-data.sql
-- ============================================================

-- 1. 物料档案表
CREATE TABLE IF NOT EXISTS mes_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  material_code VARCHAR(50) NOT NULL COMMENT '物料编码',
  material_name VARCHAR(200) NOT NULL COMMENT '物料名称',
  material_type VARCHAR(50) NOT NULL COMMENT '物料类型',
  category_level1 VARCHAR(100) COMMENT '一级分类',
  category_level2 VARCHAR(100) COMMENT '二级分类',
  g_code VARCHAR(50) COMMENT 'G编码',
  product_type VARCHAR(50) COMMENT '产品类型',
  product_category VARCHAR(50) COMMENT '产品类别',
  machine_model VARCHAR(100) COMMENT '机型',
  part_name VARCHAR(200) COMMENT '部件名称',
  factory VARCHAR(100) COMMENT '工厂',
  base_unit VARCHAR(20) NOT NULL COMMENT '基本计量单位',
  trace_mode VARCHAR(20) COMMENT '物料追溯方式（SERIAL/BATCH/QUANTITY）',
  serial_generator VARCHAR(100) COMMENT '序列号生成器',
  batch_generator VARCHAR(100) COMMENT '批号生成器',
  barcode_type VARCHAR(50) COMMENT '条码类型',
  need_inspection TINYINT(1) DEFAULT 0 COMMENT '是否需要检验',
  drawing_no VARCHAR(100) COMMENT '图号',
  material_brand VARCHAR(100) COMMENT '材料牌号',
  product_image VARCHAR(500) COMMENT '产品外观图片',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_material_code (material_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料档案表';

-- 2. 物料价格表
CREATE TABLE IF NOT EXISTS mes_material_price (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  material_id BIGINT NOT NULL COMMENT '物料ID',
  unit_price DECIMAL(18,4) NOT NULL COMMENT '物料单价',
  unit VARCHAR(20) COMMENT '单位',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  KEY idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料价格表';

-- 3. 工作中心表
CREATE TABLE IF NOT EXISTS mes_work_center (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_center_code VARCHAR(50) NOT NULL COMMENT '工作中心编码',
  work_center_name VARCHAR(200) NOT NULL COMMENT '工作中心名称',
  work_center_category VARCHAR(50) COMMENT '工作中心分类',
  business_unit VARCHAR(100) COMMENT '业务单元',
  work_calendar VARCHAR(100) COMMENT '工作日历',
  resource_order INT COMMENT '资源排序',
  usage_qty DECIMAL(10,2) COMMENT '使用量',
  usage_unit VARCHAR(20) COMMENT '使用量单位',
  batch_qty DECIMAL(10,2) COMMENT '处理批量',
  efficiency DECIMAL(5,2) COMMENT '效率',
  resource_type VARCHAR(50) COMMENT '资源种类',
  furnace_resource_type VARCHAR(50) COMMENT '炉资源类型',
  resource_capacity DECIMAL(10,2) COMMENT '资源能力',
  process_no_interrupt TINYINT(1) DEFAULT 0 COMMENT '工序不中断',
  process_no_cross_day TINYINT(1) DEFAULT 0 COMMENT '工序不跨天',
  fixed_takt_production TINYINT(1) DEFAULT 0 COMMENT '固定节拍点生产',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_work_center_code (work_center_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作中心表';
