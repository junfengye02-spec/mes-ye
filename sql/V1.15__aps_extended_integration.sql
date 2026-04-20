-- ============================================================
-- 模块：APS 扩展集成（MES-APS 交互升级）
-- 表数量：3（甘特图缓存 + 产能负荷 + 排程变更记录）
-- ============================================================

-- ==================== 新增表 ====================

-- 1. APS 甘特图数据缓存表
CREATE TABLE IF NOT EXISTS mes_aps_gantt_cache (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  schedule_batch_id VARCHAR(64) NOT NULL COMMENT '排程批次号',
  task_id VARCHAR(100) COMMENT 'APS任务ID',
  work_order_no VARCHAR(100) COMMENT '工单号',
  order_no VARCHAR(100) COMMENT '订单号',
  product_code VARCHAR(100) COMMENT '产品编码',
  product_name VARCHAR(200) COMMENT '产品名称',
  process_no VARCHAR(50) COMMENT '工序号',
  process_name VARCHAR(200) COMMENT '工序名称',
  resource_code VARCHAR(100) COMMENT '资源编码（工作中心）',
  resource_name VARCHAR(200) COMMENT '资源名称',
  start_time DATETIME COMMENT '计划开始时间',
  end_time DATETIME COMMENT '计划结束时间',
  duration INT COMMENT '时长（分钟）',
  status VARCHAR(20) COMMENT '状态',
  priority INT COMMENT '优先级',
  predecessors JSON COMMENT '前置任务ID列表',
  range_start DATETIME COMMENT '排程范围-开始',
  range_end DATETIME COMMENT '排程范围-结束',
  created_time DATETIME,
  KEY idx_batch_id (schedule_batch_id),
  KEY idx_work_order_no (work_order_no),
  KEY idx_resource_code (resource_code),
  KEY idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APS甘特图数据缓存表';

-- 2. APS 产能负荷数据表
CREATE TABLE IF NOT EXISTS mes_aps_capacity_load (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  schedule_batch_id VARCHAR(64) NOT NULL COMMENT '排程批次号',
  work_center_code VARCHAR(100) NOT NULL COMMENT '工作中心编码',
  work_center_name VARCHAR(200) COMMENT '工作中心名称',
  load_date DATE NOT NULL COMMENT '日期',
  available_capacity DECIMAL(18,2) COMMENT '可用产能（分钟）',
  scheduled_capacity DECIMAL(18,2) COMMENT '已排产能（分钟）',
  load_rate DECIMAL(8,2) COMMENT '负荷率（%）',
  overloaded TINYINT(1) DEFAULT 0 COMMENT '是否超负荷',
  calculated_at DATETIME COMMENT '计算时间',
  created_time DATETIME,
  KEY idx_batch_id (schedule_batch_id),
  KEY idx_work_center_date (work_center_code, load_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APS产能负荷数据表';

-- 3. APS 排程变更记录表
CREATE TABLE IF NOT EXISTS mes_aps_schedule_change (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  schedule_batch_id VARCHAR(64) COMMENT '排程批次号',
  change_reason VARCHAR(500) COMMENT '变更原因',
  change_time DATETIME COMMENT '变更时间',
  work_order_no VARCHAR(100) COMMENT '工单号',
  order_no VARCHAR(100) COMMENT '订单号',
  change_type VARCHAR(50) COMMENT '变更类型（TIME_CHANGED/RESOURCE_CHANGED/CANCELLED/NEW）',
  old_start_time DATETIME COMMENT '原计划开始时间',
  new_start_time DATETIME COMMENT '新计划开始时间',
  old_end_time DATETIME COMMENT '原计划结束时间',
  new_end_time DATETIME COMMENT '新计划结束时间',
  remark VARCHAR(500) COMMENT '变更说明',
  applied TINYINT(1) DEFAULT 0 COMMENT '是否已应用到MES工单',
  created_time DATETIME,
  KEY idx_batch_id (schedule_batch_id),
  KEY idx_work_order_no (work_order_no),
  KEY idx_change_time (change_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APS排程变更记录表';

-- ==================== 扩展同步配置 ====================

INSERT INTO mes_aps_sync_config (config_key, config_value, config_desc, enabled, created_time) VALUES
('aps.sync.master-data.enabled', 'true', 'APS主数据同步开关', 1, NOW()),
('aps.sync.master-data.interval.hours', '24', '主数据同步间隔（小时）', 1, NOW()),
('aps.sync.feedback.enabled', 'true', 'APS执行反馈同步开关', 1, NOW()),
('aps.sync.gantt.enabled', 'true', 'APS甘特图数据接收开关', 1, NOW()),
('aps.sync.capacity.enabled', 'true', 'APS产能负荷数据接收开关', 1, NOW()),
('aps.sync.schedule-change.enabled', 'true', 'APS排程变更通知接收开关', 1, NOW()),
('aps.sync.mrp.enabled', 'true', 'APS物料需求计划接收开关', 1, NOW()),
('aps.sync.resource-allocation.enabled', 'true', 'APS资源分配计划接收开关', 1, NOW());
