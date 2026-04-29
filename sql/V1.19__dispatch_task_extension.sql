-- ============================================================
-- V1.19 生产派工模块扩展（P0-03）
-- ------------------------------------------------------------
-- 目的：配合 DispatchTaskController 新增的 7 个写接口：
--   create / update / cancel / assign / unassign / start / complete
-- 新增字段：actual_start_time / actual_end_time / actual_qty
--           quality_result / cancel_reason / created_by / updated_by
--           deleted（逻辑删除列，配合 DispatchTask 实体 extends BaseEntity 后的 @TableLogic）
-- 枚举扩展：dispatch_status 可取值
--   UNASSIGNED / ASSIGNED / IN_PROGRESS / COMPLETED / CANCELLED / REVOKED
--
-- 兼容性（P0 修复 R1，由 mcp30 接盘自 mcp26）：
--   原版本使用 `ADD COLUMN IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS`
--   —— 这是 MariaDB 方言，MySQL 8.0.x 原生报 ERROR 1064（mcp9-v2 复验），
--   本次改为 INFORMATION_SCHEMA + PREPARE/EXECUTE/DEALLOCATE 动态 SQL 实现
--   真正幂等，兼容 MySQL 8.0.12+ 所有发行版。
--
-- 幂等性：
--   - PREPARE 分支天然幂等（列 / 索引已存在则 no-op）；
--   - MODIFY COLUMN 设置相同定义是 no-op；
--   - 连续执行两次均应 SUCCESS、0 ERROR。
-- ============================================================

-- 1. mes_dispatch_task 表扩展 ---------------------------------

-- 1.1 actual_start_time
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'mes_dispatch_task'
     AND column_name  = 'actual_start_time'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE mes_dispatch_task ADD COLUMN actual_start_time DATETIME NULL COMMENT ''实际开工时间'' AFTER plan_end_time',
  'SELECT ''mes_dispatch_task.actual_start_time already exists, skip ADD COLUMN'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.2 actual_end_time
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'mes_dispatch_task'
     AND column_name  = 'actual_end_time'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE mes_dispatch_task ADD COLUMN actual_end_time DATETIME NULL COMMENT ''实际完工时间'' AFTER actual_start_time',
  'SELECT ''mes_dispatch_task.actual_end_time already exists, skip ADD COLUMN'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.3 actual_qty
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'mes_dispatch_task'
     AND column_name  = 'actual_qty'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE mes_dispatch_task ADD COLUMN actual_qty DECIMAL(18,4) NULL COMMENT ''实际完成数量'' AFTER actual_end_time',
  'SELECT ''mes_dispatch_task.actual_qty already exists, skip ADD COLUMN'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.4 quality_result
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'mes_dispatch_task'
     AND column_name  = 'quality_result'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE mes_dispatch_task ADD COLUMN quality_result VARCHAR(10) NULL COMMENT ''质量结果：PASS/FAIL/NA'' AFTER actual_qty',
  'SELECT ''mes_dispatch_task.quality_result already exists, skip ADD COLUMN'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.5 cancel_reason
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'mes_dispatch_task'
     AND column_name  = 'cancel_reason'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE mes_dispatch_task ADD COLUMN cancel_reason VARCHAR(500) NULL COMMENT ''撤销原因（CANCELLED 状态时必填）'' AFTER quality_result',
  'SELECT ''mes_dispatch_task.cancel_reason already exists, skip ADD COLUMN'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.6 created_by
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'mes_dispatch_task'
     AND column_name  = 'created_by'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE mes_dispatch_task ADD COLUMN created_by VARCHAR(50) NULL COMMENT ''创建人'' AFTER cancel_reason',
  'SELECT ''mes_dispatch_task.created_by already exists, skip ADD COLUMN'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.7 updated_by
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'mes_dispatch_task'
     AND column_name  = 'updated_by'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE mes_dispatch_task ADD COLUMN updated_by VARCHAR(50) NULL COMMENT ''更新人'' AFTER created_by',
  'SELECT ''mes_dispatch_task.updated_by already exists, skip ADD COLUMN'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.8 deleted（逻辑删除；配合 DispatchTask extends BaseEntity 的 @TableLogic 使用）
--     由 mcp30 引入：R3 修复把 mes_dispatch_task 纳入 MyBatis-Plus 逻辑删除与租户自动填充，
--     表 V1.05 建表时遗漏 deleted，V1.12 仅覆盖 sys_* 表，本步骤在 V1.19 里一并补齐。
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name   = 'mes_dispatch_task'
     AND column_name  = 'deleted'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE mes_dispatch_task ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''逻辑删除(0=正常,1=已删除)'' AFTER updated_time',
  'SELECT ''mes_dispatch_task.deleted already exists, skip ADD COLUMN'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. 为资源占用冲突校验提供查询加速 -----------------------------
--    常用条件：(assignee_id, assign_type, status='ACTIVE') 反查 dispatch_task_id 再比计划时间
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'mes_dispatch_assignment'
     AND index_name   = 'idx_assignee_active'
);
SET @ddl := IF(@idx_exists = 0,
  'CREATE INDEX idx_assignee_active ON mes_dispatch_assignment (assignee_id, assign_type, status)',
  'SELECT ''idx_assignee_active already exists, skip CREATE INDEX'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 3. 计划时间区间查询 ----------------------------------------
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name   = 'mes_dispatch_task'
     AND index_name   = 'idx_plan_time'
);
SET @ddl := IF(@idx_exists = 0,
  'CREATE INDEX idx_plan_time ON mes_dispatch_task (plan_start_time, plan_end_time)',
  'SELECT ''idx_plan_time already exists, skip CREATE INDEX'' AS note'
);
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 4. dispatch_status 注释更新（MODIFY COLUMN 天然幂等：重复执行等价于把相同定义再设一次） ---
ALTER TABLE mes_dispatch_task
    MODIFY COLUMN dispatch_status VARCHAR(20) DEFAULT 'UNASSIGNED'
        COMMENT '分派状态：UNASSIGNED/ASSIGNED/IN_PROGRESS/COMPLETED/CANCELLED/REVOKED';

-- 5. 配套权限点（RBAC）：为 dispatch:task:* 新权限写入菜单/权限表
--    若项目采用 sys_permission 统一管理，这里做幂等插入；不存在该表则忽略本段
--    （以 INSERT IGNORE 方式容错，表缺失时 SQL 文件不阻塞启动）
-- INSERT IGNORE INTO sys_permission (code, name, module) VALUES
--   ('dispatch:task:list',     '派工任务-查询',       '生产派工'),
--   ('dispatch:task:detail',   '派工任务-详情',       '生产派工'),
--   ('dispatch:task:generate', '派工任务-工单生成',   '生产派工'),
--   ('dispatch:task:create',   '派工任务-手动创建',   '生产派工'),
--   ('dispatch:task:update',   '派工任务-更新',       '生产派工'),
--   ('dispatch:task:cancel',   '派工任务-撤销',       '生产派工'),
--   ('dispatch:task:assign',   '派工任务-指派',       '生产派工'),
--   ('dispatch:task:unassign', '派工任务-取消指派',   '生产派工'),
--   ('dispatch:task:start',    '派工任务-开工',       '生产派工'),
--   ('dispatch:task:complete', '派工任务-完工',       '生产派工');
-- NOTE: 项目权限表结构以 V1.11__auth_rbac.sql 为准，正式上线前按实际表结构放开上面的 INSERT。
