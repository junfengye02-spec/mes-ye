-- ============================================================
-- 模块：认证与权限管理 (RBAC)
-- 表数量：5
-- 说明：用户/角色/菜单/关联表 + 初始数据
-- ============================================================

-- 1. 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL COMMENT '用户名',
  password VARCHAR(200) NOT NULL COMMENT '密码（BCrypt）',
  real_name VARCHAR(50) COMMENT '真实姓名',
  phone VARCHAR(20) COMMENT '手机号',
  email VARCHAR(100) COMMENT '邮箱',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（1=启用, 0=禁用）',
  factory_code VARCHAR(50) COMMENT '所属工厂编码',
  created_by VARCHAR(50),
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(50),
  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0 COMMENT '逻辑删除（0=正常, 1=已删除）',
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 2. 系统角色表
CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
  role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
  description VARCHAR(200) COMMENT '角色描述',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_by VARCHAR(50),
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(50),
  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 3. 系统菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID（0=顶级）',
  menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
  path VARCHAR(200) COMMENT '路由路径',
  component VARCHAR(200) COMMENT '组件路径',
  menu_type CHAR(1) NOT NULL COMMENT '菜单类型（D=目录, M=菜单, B=按钮）',
  permission VARCHAR(100) COMMENT '权限标识',
  icon VARCHAR(50) COMMENT '菜单图标',
  sort_order INT DEFAULT 0 COMMENT '排序',
  visible TINYINT(1) DEFAULT 1 COMMENT '是否可见',
  created_by VARCHAR(50),
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(50),
  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';

-- 4. 用户-角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 5. 角色-菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
  role_id BIGINT NOT NULL COMMENT '角色ID',
  menu_id BIGINT NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 管理员账号：admin / admin123（BCrypt 加密）
INSERT INTO sys_user (username, password, real_name, enabled, created_by)
VALUES ('admin', '$2a$10$e2nbvCXt4JOvHpdJAqvIweP8fRNID1OUSVBmbxg4PLiVGdKonzRXy', '系统管理员', 1, 'system');

-- 角色
INSERT INTO sys_role (role_name, role_code, description, enabled, created_by) VALUES
('超级管理员', 'ADMIN', '拥有全部权限', 1, 'system'),
('生产主管', 'PRODUCTION_MANAGER', '管理生产工单、派工、物料', 1, 'system'),
('质量管理员', 'QUALITY_MANAGER', '管理质量检验、异常联络', 1, 'system'),
('普通操作员', 'OPERATOR', '基本数据查看与录入', 1, 'system');

-- 管理员 → 超级管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 菜单数据（目录 + 菜单）
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, icon, sort_order, visible) VALUES
-- 顶级目录
(1,  0, '基础数据',   '/basic',        NULL, 'D', 'Files',          1, 1),
(2,  0, '班组管理',   '/team',         NULL, 'D', 'User',           2, 1),
(3,  0, '工艺管理',   '/process',      NULL, 'D', 'Setting',        3, 1),
(4,  0, '计划管理',   '/plan',         NULL, 'D', 'Calendar',       4, 1),
(5,  0, '生产工单',   '/workorder',    NULL, 'D', 'Document',       5, 1),
(6,  0, '生产派工',   '/dispatch',     NULL, 'D', 'Coordinate',     6, 1),
(7,  0, '异常管理',   '/abnormal',     NULL, 'D', 'WarningFilled',  7, 1),
(8,  0, '成品质量',   '/quality',      NULL, 'D', 'CircleCheck',    8, 1),
(9,  0, '工作查询',   '/query',        NULL, 'D', 'Search',         9, 1),
(10, 0, '物料管理',   '/material-mgmt', NULL, 'D', 'Box',          10, 1),
(11, 0, 'APS 集成',  '/aps',          NULL, 'D', 'Connection',    11, 1),
(12, 0, '系统管理',   '/system',       NULL, 'D', 'Tools',         99, 1),

-- 基础数据子菜单
(101, 1, '物料档案',   '/basic/material',       'views/basic/MaterialList',       'M', NULL, 1, 1),
(102, 1, '物料价格',   '/basic/material-price',  'views/basic/MaterialPriceList',  'M', NULL, 2, 1),
(103, 1, '工作中心',   '/basic/work-center',     'views/basic/WorkCenterList',     'M', NULL, 3, 1),

-- 班组管理子菜单
(201, 2, '生产班组',   '/team/production-team',  'views/team/ProductionTeamList',  'M', NULL, 1, 1),

-- 工艺管理子菜单
(301, 3, '指示书管理',   '/process/instruction',        'views/process/InstructionList',        'M', NULL, 1, 1),
(302, 3, '工序模板',     '/process/template',           'views/process/ProcessTemplateList',     'M', NULL, 2, 1),
(303, 3, '工序信息',     '/process/info',               'views/process/ProcessInfoList',         'M', NULL, 3, 1),
(304, 3, '指导书管理',   '/process/work-instruction',    'views/process/WorkInstructionList',     'M', NULL, 4, 1),
(305, 3, '喷涂条件表',   '/process/spray-condition',     'views/process/SprayConditionList',      'M', NULL, 5, 1),
(306, 3, '机械加工程序表', '/process/machining-program',  'views/process/MachiningProgramList',    'M', NULL, 6, 1),
(307, 3, '制造BOM',      '/process/bom',                'views/process/ManufacturingBomList',    'M', NULL, 7, 1),

-- 计划管理子菜单
(401, 4, '订单计划',   '/plan/order',       'views/plan/OrderPlanList',       'M', NULL, 1, 1),
(402, 4, '生产计划',   '/plan/production',  'views/plan/ProductionPlanList',  'M', NULL, 2, 1),

-- 生产工单子菜单
(501, 5, '工单管理',   '/workorder/list',   'views/workorder/WorkOrderList',  'M', NULL, 1, 1),

-- 生产派工子菜单
(601, 6, '派工管理',   '/dispatch/task',    'views/dispatch/DispatchTask',    'M', NULL, 1, 1),

-- 异常管理子菜单
(701, 7, '异常联络单', '/abnormal/contact',  'views/abnormal/AbnormalContactList', 'M', NULL, 1, 1),

-- 成品质量子菜单
(801, 8, '复检申请',           '/quality/recheck',          'views/quality/RecheckRequestList',   'M', NULL, 1, 1),
(802, 8, '开工检查实绩(工作)', '/quality/work-start-check',  'views/quality/WorkStartCheckList',   'M', NULL, 2, 1),
(803, 8, '开工检查实绩(工单)', '/quality/order-start-check', 'views/quality/OrderStartCheckList',  'M', NULL, 3, 1),
(804, 8, '交班记录',           '/quality/shift-handover',    'views/quality/ShiftHandoverList',    'M', NULL, 4, 1),

-- 工作查询子菜单
(901, 9, '六状态查看',         '/query/work-status',       'views/query/WorkStatusView',         'M', NULL, 1, 1),
(902, 9, '生产工作查询',       '/query/production-work',   'views/query/ProductionWorkQuery',    'M', NULL, 2, 1),
(903, 9, '检验工作查询',       '/query/inspection-work',   'views/query/InspectionWorkQuery',    'M', NULL, 3, 1),
(904, 9, '生产工作开工检查实绩', '/query/work-start-check', 'views/query/WorkStartCheckQuery',    'M', NULL, 4, 1),
(905, 9, '生产工单开工检查实绩', '/query/order-start-check', 'views/query/OrderStartCheckQuery',  'M', NULL, 5, 1),
(906, 9, '交班记录',           '/query/shift-handover',    'views/query/ShiftHandoverQuery',     'M', NULL, 6, 1),
(907, 9, '生产工单',           '/query/work-order',        'views/query/WorkOrderQuery',         'M', NULL, 7, 1),
(908, 9, '派工工作查询',       '/query/dispatch-work',     'views/query/DispatchWorkQuery',       'M', NULL, 8, 1),

-- 物料管理子菜单
(1001, 10, '存储地点库存',  '/material-mgmt/inventory',        'views/material-mgmt/InventoryList',        'M', NULL, 1, 1),
(1002, 10, '生产领料',      '/material-mgmt/requisition',      'views/material-mgmt/RequisitionList',      'M', NULL, 2, 1),
(1003, 10, '按物料领料',    '/material-mgmt/requisition-order', 'views/material-mgmt/RequisitionOrderList', 'M', NULL, 3, 1),
(1004, 10, '完工入库申请',  '/material-mgmt/receipt-request',   'views/material-mgmt/ReceiptRequestList',   'M', NULL, 4, 1),
(1005, 10, '完工入库',      '/material-mgmt/receipt',          'views/material-mgmt/ReceiptList',          'M', NULL, 5, 1),
(1006, 10, '生产退料',      '/material-mgmt/return',           'views/material-mgmt/ReturnList',           'M', NULL, 6, 1),
(1007, 10, '发货签收',      '/material-mgmt/delivery-sign',    'views/material-mgmt/DeliverySignList',     'M', NULL, 7, 1),

-- APS 集成子菜单
(1101, 11, '同步配置',     '/aps/sync-config',   'views/aps/SyncConfigList',   'M', NULL, 1, 1),
(1102, 11, '同步日志',     '/aps/sync-log',      'views/aps/SyncLogList',      'M', NULL, 2, 1),
(1103, 11, '数据映射管理', '/aps/data-mapping',   'views/aps/DataMappingList',  'M', NULL, 3, 1),

-- 系统管理子菜单
(1201, 12, '用户管理',   '/system/user',  'views/system/UserList',  'M', NULL, 1, 1),
(1202, 12, '角色管理',   '/system/role',  'views/system/RoleList',  'M', NULL, 2, 1),
(1203, 12, '菜单管理',   '/system/menu',  'views/system/MenuList',  'M', NULL, 3, 1);

-- 超级管理员角色拥有全部菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE deleted = 0;
