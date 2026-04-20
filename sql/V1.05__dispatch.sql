-- ============================================================
-- 模块：生产派工
-- 表数量：3
-- ============================================================

-- 1. 派工任务表
CREATE TABLE IF NOT EXISTS mes_dispatch_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL COMMENT '工单ID',
  work_order_task_id BIGINT COMMENT '工作清单ID',
  order_no VARCHAR(100) COMMENT '订单编号',
  process_no VARCHAR(50) COMMENT '工序号',
  work_name VARCHAR(200) COMMENT '工作名称',
  plan_work_center_id BIGINT COMMENT '计划工作中心',
  serial_no VARCHAR(100) COMMENT '序列号',
  project_name VARCHAR(200) COMMENT '项目',
  plan_qty DECIMAL(18,4) COMMENT '计划数量',
  qty_unit VARCHAR(20) COMMENT '数量单位',
  dispatch_status VARCHAR(20) DEFAULT 'UNASSIGNED' COMMENT '分派状态（UNASSIGNED/ASSIGNED/REVOKED）',
  plan_start_time DATETIME COMMENT '计划开始时间',
  plan_end_time DATETIME COMMENT '计划结束时间',
  created_time DATETIME,
  updated_time DATETIME,
  KEY idx_work_order_id (work_order_id),
  KEY idx_dispatch_status (dispatch_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='派工任务表';

-- 2. 派工分配表
CREATE TABLE IF NOT EXISTS mes_dispatch_assignment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dispatch_task_id BIGINT NOT NULL COMMENT '派工任务ID',
  assign_type VARCHAR(20) NOT NULL COMMENT '分派类型（PERSON/DEVICE/TEAM）',
  assignee_id BIGINT COMMENT '分派对象ID',
  assignee_code VARCHAR(50) COMMENT '分派对象编码',
  assignee_name VARCHAR(200) COMMENT '分派对象名称',
  assigned_qty DECIMAL(18,4) COMMENT '分派数量',
  qty_unit VARCHAR(20) COMMENT '数量单位',
  status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '分派状态（ACTIVE/REVOKED）',
  assigned_by VARCHAR(50) COMMENT '派工人',
  assigned_time DATETIME COMMENT '派工时间',
  revoked_by VARCHAR(50) COMMENT '撤销人',
  revoked_time DATETIME COMMENT '撤销时间',
  KEY idx_dispatch_task_id (dispatch_task_id),
  KEY idx_assign_type (assign_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='派工分配表';

-- 3. 派工状态日志表
CREATE TABLE IF NOT EXISTS mes_dispatch_status_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dispatch_task_id BIGINT NOT NULL COMMENT '派工任务ID',
  from_status VARCHAR(20) COMMENT '原状态',
  to_status VARCHAR(20) COMMENT '新状态',
  action VARCHAR(50) COMMENT '动作',
  operator VARCHAR(50) COMMENT '操作人',
  operated_time DATETIME COMMENT '操作时间',
  remark VARCHAR(500) COMMENT '说明',
  KEY idx_dispatch_task_id (dispatch_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='派工状态日志表';
