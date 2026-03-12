-- ============================================================
-- 模块：APS 集成
-- 表数量：10（5 核心 + 5 扩展）
-- ============================================================

-- ==================== 核心同步表 ====================

-- 1. APS 同步配置表
CREATE TABLE IF NOT EXISTS mes_aps_sync_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(100) NOT NULL COMMENT '配置键（如 aps.sync.enabled）',
  config_value VARCHAR(500) NOT NULL COMMENT '配置值',
  config_desc VARCHAR(200) COMMENT '配置说明',
  enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APS同步配置表';

-- 2. APS 同步日志表
CREATE TABLE IF NOT EXISTS mes_aps_sync_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_id VARCHAR(64) NOT NULL COMMENT '同步批次号（UUID）',
  sync_direction VARCHAR(20) NOT NULL COMMENT '同步方向（DOWNSTREAM/UPSTREAM）',
  sync_type VARCHAR(50) NOT NULL COMMENT '同步类型（ORDER/WORKORDER/INVENTORY/QUALITY/ABNORMAL）',
  total_count INT COMMENT '数据总量',
  success_count INT COMMENT '成功数量',
  fail_count INT COMMENT '失败数量',
  status VARCHAR(20) NOT NULL COMMENT '同步状态（SUCCESS/FAIL/PARTIAL）',
  start_time DATETIME COMMENT '开始时间',
  end_time DATETIME COMMENT '结束时间',
  duration_ms BIGINT COMMENT '耗时（毫秒）',
  error_message TEXT COMMENT '错误信息',
  created_time DATETIME,
  UNIQUE KEY uk_batch_id (batch_id),
  KEY idx_direction_type_time (sync_direction, sync_type, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APS同步日志表';

-- 3. APS 同步数据明细表
CREATE TABLE IF NOT EXISTS mes_aps_sync_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_id VARCHAR(64) NOT NULL COMMENT '同步批次号',
  data_type VARCHAR(50) NOT NULL COMMENT '数据类型（ORDER/WORKORDER/INVENTORY）',
  data_id BIGINT COMMENT '关联数据ID',
  data_no VARCHAR(100) COMMENT '关联数据编号（如订单号）',
  sync_action VARCHAR(20) NOT NULL COMMENT '同步动作（CREATE/UPDATE/DELETE）',
  sync_status VARCHAR(20) NOT NULL COMMENT '同步状态（SUCCESS/FAIL/PENDING）',
  aps_data JSON COMMENT 'APS侧数据快照',
  mes_data JSON COMMENT 'MES侧数据快照',
  error_message VARCHAR(500) COMMENT '错误信息',
  retry_count INT DEFAULT 0 COMMENT '重试次数',
  created_time DATETIME,
  updated_time DATETIME,
  KEY idx_batch_id (batch_id),
  KEY idx_data_type_no (data_type, data_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APS同步数据明细表';

-- 4. APS 数据映射表
CREATE TABLE IF NOT EXISTS mes_aps_data_mapping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  mapping_type VARCHAR(50) NOT NULL COMMENT '映射类型（MATERIAL/WORK_CENTER/STATUS/FACTORY/PROCESS/SUPPLIER）',
  mes_code VARCHAR(100) NOT NULL COMMENT 'MES编码',
  mes_name VARCHAR(200) COMMENT 'MES名称',
  aps_code VARCHAR(100) NOT NULL COMMENT 'APS编码',
  aps_name VARCHAR(200) COMMENT 'APS名称',
  enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_type_mes_code (mapping_type, mes_code),
  UNIQUE KEY uk_type_aps_code (mapping_type, aps_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APS数据映射表';

-- 5. APS 待同步队列表
CREATE TABLE IF NOT EXISTS mes_aps_sync_queue (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sync_direction VARCHAR(20) NOT NULL COMMENT '同步方向（DOWNSTREAM/UPSTREAM）',
  sync_type VARCHAR(50) NOT NULL COMMENT '同步类型',
  data_type VARCHAR(50) NOT NULL COMMENT '数据类型',
  data_id BIGINT COMMENT '关联数据ID',
  data_no VARCHAR(100) COMMENT '关联数据编号',
  priority INT DEFAULT 5 COMMENT '优先级（1最高，10最低）',
  sync_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态（PENDING/PROCESSING/SYNCED/FAILED）',
  retry_count INT DEFAULT 0 COMMENT '已重试次数',
  max_retry INT DEFAULT 3 COMMENT '最大重试次数',
  next_retry_time DATETIME COMMENT '下次重试时间',
  payload JSON COMMENT '同步数据载荷',
  error_message VARCHAR(500) COMMENT '错误信息',
  created_time DATETIME,
  updated_time DATETIME,
  KEY idx_status_priority_time (sync_status, priority, created_time),
  KEY idx_data_type_no (data_type, data_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APS待同步队列表';

-- ==================== 扩展表（APS 高级功能） ====================

-- 6. 工作清单任务分段表（APS TaskSegment 映射）
CREATE TABLE IF NOT EXISTS mes_work_order_task_segment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_task_id BIGINT NOT NULL COMMENT '工作清单项ID',
  segment_index INT NOT NULL COMMENT '分段序号（从1开始）',
  segment_start_time DATETIME COMMENT '分段开始时间',
  segment_end_time DATETIME COMMENT '分段结束时间',
  segment_duration INT COMMENT '分段时长（分钟）',
  shift_name VARCHAR(50) COMMENT '所属班次名称',
  assigned_team_id BIGINT COMMENT '负责班组ID',
  actual_start_time DATETIME COMMENT '实际开始时间',
  actual_end_time DATETIME COMMENT '实际结束时间',
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT '分段状态（PENDING/IN_PROGRESS/COMPLETED）',
  created_time DATETIME,
  updated_time DATETIME,
  KEY idx_work_order_task_id (work_order_task_id),
  UNIQUE KEY uk_task_segment (work_order_task_id, segment_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作清单任务分段表';

-- 7. 外协订单表（APS OutsourceOrder 映射）
CREATE TABLE IF NOT EXISTS mes_outsource_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  outsource_order_no VARCHAR(100) NOT NULL COMMENT '外协订单号',
  parent_order_no VARCHAR(100) COMMENT '主生产订单号',
  aps_order_id BIGINT COMMENT 'APS外协订单ID',
  material_id BIGINT COMMENT '物料ID',
  material_code VARCHAR(100) COMMENT '物料编码',
  material_name VARCHAR(200) COMMENT '物料名称',
  plan_qty DECIMAL(18,4) COMMENT '计划数量',
  supplier_id BIGINT COMMENT '供应商ID',
  supplier_name VARCHAR(200) COMMENT '供应商名称',
  process_sequence INT COMMENT '工序顺序号',
  process_name VARCHAR(200) COMMENT '工序名称',
  plan_start_time DATETIME COMMENT '计划开始时间',
  plan_end_time DATETIME COMMENT '计划结束时间',
  aps_status VARCHAR(20) COMMENT 'APS状态',
  mes_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'MES执行状态（PENDING/SHIPPED/RECEIVED/QC_PASSED/QC_FAILED/COMPLETED）',
  actual_ship_time DATETIME COMMENT '实际发出时间',
  actual_receive_time DATETIME COMMENT '实际收货时间',
  received_qty DECIMAL(18,4) COMMENT '实际收货数量',
  qualified_qty DECIMAL(18,4) COMMENT '合格数量',
  remark VARCHAR(500) COMMENT '备注',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_outsource_order_no (outsource_order_no),
  KEY idx_parent_order_no (parent_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外协订单表';

-- 8. 转厂订单表（APS TransferOrder 映射）
CREATE TABLE IF NOT EXISTS mes_transfer_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  transfer_no VARCHAR(100) NOT NULL COMMENT '转厂单号',
  parent_order_no VARCHAR(100) COMMENT '主生产订单号',
  aps_transfer_id BIGINT COMMENT 'APS转厂订单ID',
  material_id BIGINT COMMENT '物料ID',
  material_code VARCHAR(100) COMMENT '物料编码',
  material_name VARCHAR(200) COMMENT '物料名称',
  plan_qty DECIMAL(18,4) COMMENT '计划数量',
  from_factory VARCHAR(100) COMMENT '发出工厂',
  to_factory VARCHAR(100) COMMENT '接收工厂',
  plan_ship_time DATETIME COMMENT '计划发出时间',
  plan_arrive_time DATETIME COMMENT '计划到达时间',
  aps_status VARCHAR(20) COMMENT 'APS状态',
  mes_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'MES执行状态（PENDING/SHIPPED/IN_TRANSIT/ARRIVED/RECEIVED）',
  actual_ship_time DATETIME COMMENT '实际发出时间',
  actual_arrive_time DATETIME COMMENT '实际到达时间',
  received_qty DECIMAL(18,4) COMMENT '实际接收数量',
  remark VARCHAR(500) COMMENT '备注',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_transfer_no (transfer_no),
  KEY idx_parent_order_no (parent_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转厂订单表';

-- 9. 资源日历表（APS ResourceCalendar 映射）
CREATE TABLE IF NOT EXISTS mes_resource_calendar (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  aps_calendar_id BIGINT COMMENT 'APS日历ID',
  calendar_name VARCHAR(200) COMMENT '日历名称',
  resource_id BIGINT COMMENT '关联工作中心ID',
  resource_code VARCHAR(100) COMMENT '关联工作中心编码',
  created_time DATETIME,
  updated_time DATETIME,
  KEY idx_aps_calendar_id (aps_calendar_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源日历表';

-- 10. 资源日历班次表（APS CalendarShift 映射）
CREATE TABLE IF NOT EXISTS mes_resource_calendar_shift (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  calendar_id BIGINT NOT NULL COMMENT '资源日历ID',
  day_of_week VARCHAR(20) COMMENT '星期几（MONDAY~SUNDAY）',
  shift_start_time TIME COMMENT '班次开始时间',
  shift_end_time TIME COMMENT '班次结束时间',
  shift_name VARCHAR(50) COMMENT '班次名称',
  created_time DATETIME,
  updated_time DATETIME,
  KEY idx_calendar_id (calendar_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源日历班次表';

-- ==================== 初始同步配置数据 ====================

INSERT INTO mes_aps_sync_config (config_key, config_value, config_desc, enabled, created_time) VALUES
('aps.base.url', 'http://localhost:8081', 'APS服务基础地址', 1, NOW()),
('aps.api.key', 'mes-default-api-key', 'APS接口认证密钥', 1, NOW()),
('aps.sync.enabled', 'true', 'APS同步全局开关', 1, NOW()),
('aps.sync.downstream.enabled', 'true', 'APS下行同步开关（APS→MES）', 1, NOW()),
('aps.sync.upstream.enabled', 'true', 'APS上行同步开关（MES→APS）', 1, NOW()),
('aps.sync.reschedule.enabled', 'true', 'APS重排触发开关', 1, NOW()),
('aps.sync.interval.minutes', '5', '同步间隔（分钟）', 1, NOW()),
('aps.sync.batch.size', '200', '单次同步批量大小', 1, NOW()),
('aps.sync.timeout.seconds', '30', 'HTTP请求超时（秒）', 1, NOW()),
('aps.sync.retry.max', '3', '最大重试次数', 1, NOW()),
('aps.circuit.failure.threshold', '5', '熔断器失败阈值', 1, NOW()),
('aps.circuit.open.timeout.seconds', '30', '熔断器打开持续时间（秒）', 1, NOW());
