-- ============================================================
-- V2.05  RBAC 权限码初始化（配合后端 Controller @PreAuthorize）
--
-- 背景：
--   V1.11 创建的 sys_menu 叶子菜单 permission 字段全部为 NULL，
--   当后端 Controller 使用 @PreAuthorize("hasAuthority('xxx:yyy')")
--   时，任何用户（含 admin）都会因授权码集合为空而 403。
--
-- 做法：
--   1) 给现存每个叶子菜单的 permission 字段写入该模块的"列表"权限码
--      （如 basic:material:list），用于前端路由级守卫。
--   2) 为每个叶子菜单新增一批 menu_type='B' 的"按钮"子菜单，对应
--      Controller 的每一个端点权限码（list/detail/create/update/
--      delete/publish/… 等），插入到 sys_menu 的平台模板
--      (tenant_id=0, is_template=1)。
--   3) 为租户 1 的 ADMIN 角色重新绑定"全部平台模板菜单"，保证
--      升级后 admin 立刻拥有全部新增权限码。
--
-- 依赖：V1.11__auth_rbac.sql、V2.02__tenantize_rbac.sql 已执行。
-- ============================================================

-- =============================================================
-- 1. 回填已有叶子菜单的 permission（主要用于菜单层面隐藏/显示）
-- =============================================================
UPDATE sys_menu SET permission = 'basic:material:list'        WHERE id = 101;
UPDATE sys_menu SET permission = 'basic:materialPrice:list'   WHERE id = 102;
UPDATE sys_menu SET permission = 'basic:workCenter:list'      WHERE id = 103;

UPDATE sys_menu SET permission = 'team:production:list'       WHERE id = 201;

UPDATE sys_menu SET menu_name = '执行指示',     permission = 'process:instruction:list',      sort_order = 5 WHERE id = 301;
UPDATE sys_menu SET menu_name = '工序模板',     permission = 'process:processTemplate:list',  sort_order = 3 WHERE id = 302;
UPDATE sys_menu SET menu_name = '工序库',       permission = 'process:processInfo:list',      sort_order = 2 WHERE id = 303;
UPDATE sys_menu SET menu_name = '作业指导书',   permission = 'process:workInstruction:list',  sort_order = 4 WHERE id = 304;
UPDATE sys_menu SET menu_name = '喷涂参数',     permission = 'process:sprayCondition:list',   sort_order = 7 WHERE id = 305;
UPDATE sys_menu SET menu_name = '机加程序参数', permission = 'process:machiningProgram:list', sort_order = 8 WHERE id = 306;
UPDATE sys_menu SET menu_name = '制造BOM',      permission = 'process:bom:list',              sort_order = 6 WHERE id = 307;
UPDATE sys_menu SET permission = 'process:route:list'           WHERE id = 308;

INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, path, component, menu_type, permission, sort_order, visible, is_template) VALUES
(308, 0, 3, '工艺路线', '/process/route', 'views/process/RouteList', 'M', 'process:route:list', 1, 1, 1)
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  path = VALUES(path),
  component = VALUES(component),
  menu_type = VALUES(menu_type),
  permission = VALUES(permission),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  is_template = VALUES(is_template);

UPDATE sys_menu SET permission = 'plan:order:list'       WHERE id = 401;
UPDATE sys_menu SET permission = 'plan:production:list'  WHERE id = 402;

UPDATE sys_menu SET permission = 'workorder:workorder:list' WHERE id = 501;

UPDATE sys_menu SET permission = 'dispatch:task:list'    WHERE id = 601;

UPDATE sys_menu SET permission = 'abnormal:contact:list' WHERE id = 701;

UPDATE sys_menu SET permission = 'quality:recheck:list'         WHERE id = 801;
UPDATE sys_menu SET permission = 'quality:workStartCheck:list'  WHERE id = 802;
UPDATE sys_menu SET permission = 'quality:orderStartCheck:list' WHERE id = 803;
UPDATE sys_menu SET permission = 'quality:shiftHandover:list'   WHERE id = 804;

UPDATE sys_menu SET permission = 'query:workStatus:list'      WHERE id = 901;
UPDATE sys_menu SET permission = 'query:productionWork:list'  WHERE id = 902;
UPDATE sys_menu SET permission = 'query:inspectionWork:list'  WHERE id = 903;
UPDATE sys_menu SET permission = 'quality:workStartCheck:list'  WHERE id = 904;
UPDATE sys_menu SET permission = 'quality:orderStartCheck:list' WHERE id = 905;
UPDATE sys_menu SET permission = 'quality:shiftHandover:list'   WHERE id = 906;
UPDATE sys_menu SET permission = 'workorder:workorder:list'     WHERE id = 907;
UPDATE sys_menu SET permission = 'dispatch:task:list'           WHERE id = 908;

UPDATE sys_menu SET permission = 'material:inventory:list'        WHERE id = 1001;
UPDATE sys_menu SET permission = 'material:requisition:list'      WHERE id = 1002;
UPDATE sys_menu SET permission = 'material:requisitionOrder:list' WHERE id = 1003;
UPDATE sys_menu SET permission = 'material:receiptRequest:list'   WHERE id = 1004;
UPDATE sys_menu SET permission = 'material:receipt:list'          WHERE id = 1005;
UPDATE sys_menu SET permission = 'material:return:list'           WHERE id = 1006;
UPDATE sys_menu SET permission = 'material:deliverySign:list'     WHERE id = 1007;

UPDATE sys_menu SET permission = 'aps:syncConfig:list'  WHERE id = 1101;
UPDATE sys_menu SET permission = 'aps:syncLog:list'     WHERE id = 1102;
UPDATE sys_menu SET permission = 'aps:dataMapping:list' WHERE id = 1103;

UPDATE sys_menu SET permission = 'system:user:list' WHERE id = 1201;
UPDATE sys_menu SET permission = 'system:role:list' WHERE id = 1202;
UPDATE sys_menu SET permission = 'system:menu:list' WHERE id = 1203;

-- =============================================================
-- 2. 为每个叶子菜单补齐"按钮级"子菜单（menu_type='B'）
--
-- 命名规则（与 Controller 中的 @PreAuthorize 值严格一致）：
--   {模块}:{资源}:{动作}
-- 使用 parent_id 指向父叶子菜单，id 规范：父 id * 10 + 动作序号。
-- 未分配到具体 id 的新权限放入 30000+ 段（如 file/platform）。
-- =============================================================

-- 基础数据-物料档案 (101)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(10101, 0, 101, '查看详情', 'B', 'basic:material:detail', 1, 0, 1),
(10102, 0, 101, '新增',     'B', 'basic:material:create', 2, 0, 1),
(10103, 0, 101, '修改',     'B', 'basic:material:update', 3, 0, 1),
(10104, 0, 101, '删除',     'B', 'basic:material:delete', 4, 0, 1);

-- 基础数据-物料价格 (102)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(10201, 0, 102, '查看详情', 'B', 'basic:materialPrice:detail', 1, 0, 1),
(10202, 0, 102, '新增',     'B', 'basic:materialPrice:create', 2, 0, 1),
(10203, 0, 102, '修改',     'B', 'basic:materialPrice:update', 3, 0, 1),
(10204, 0, 102, '删除',     'B', 'basic:materialPrice:delete', 4, 0, 1);

-- 基础数据-工作中心 (103)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(10301, 0, 103, '查看详情', 'B', 'basic:workCenter:detail', 1, 0, 1),
(10302, 0, 103, '新增',     'B', 'basic:workCenter:create', 2, 0, 1),
(10303, 0, 103, '修改',     'B', 'basic:workCenter:update', 3, 0, 1),
(10304, 0, 103, '删除',     'B', 'basic:workCenter:delete', 4, 0, 1);

-- 班组 (201)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(20101, 0, 201, '查看详情',   'B', 'team:production:detail', 1, 0, 1),
(20102, 0, 201, '新增',       'B', 'team:production:create', 2, 0, 1),
(20103, 0, 201, '修改',       'B', 'team:production:update', 3, 0, 1),
(20104, 0, 201, '删除',       'B', 'team:production:delete', 4, 0, 1),
(20105, 0, 201, '启停',       'B', 'team:production:toggle', 5, 0, 1);

-- 工艺-执行指示 (301)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(30101, 0, 301, '查看详情', 'B', 'process:instruction:detail',  1, 0, 1),
(30102, 0, 301, '新增',     'B', 'process:instruction:create',  2, 0, 1),
(30103, 0, 301, '修改',     'B', 'process:instruction:update',  3, 0, 1),
(30104, 0, 301, '删除',     'B', 'process:instruction:delete',  4, 0, 1),
(30105, 0, 301, '版本升级', 'B', 'process:instruction:upgrade', 5, 0, 1);

-- 工艺-工序模板 (302)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(30201, 0, 302, '查看详情', 'B', 'process:processTemplate:detail', 1, 0, 1),
(30202, 0, 302, '新增',     'B', 'process:processTemplate:create', 2, 0, 1),
(30203, 0, 302, '修改',     'B', 'process:processTemplate:update', 3, 0, 1),
(30204, 0, 302, '删除',     'B', 'process:processTemplate:delete', 4, 0, 1);

-- 工艺-工序库 (303)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(30301, 0, 303, '查看详情', 'B', 'process:processInfo:detail', 1, 0, 1),
(30302, 0, 303, '新增',     'B', 'process:processInfo:create', 2, 0, 1),
(30303, 0, 303, '修改',     'B', 'process:processInfo:update', 3, 0, 1),
(30304, 0, 303, '删除',     'B', 'process:processInfo:delete', 4, 0, 1);

-- 工艺-作业指导书 (304)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(30401, 0, 304, '查看详情', 'B', 'process:workInstruction:detail', 1, 0, 1),
(30402, 0, 304, '新增',     'B', 'process:workInstruction:create', 2, 0, 1),
(30403, 0, 304, '修改',     'B', 'process:workInstruction:update', 3, 0, 1),
(30404, 0, 304, '删除',     'B', 'process:workInstruction:delete', 4, 0, 1);

-- 工艺-喷涂参数 (305)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(30501, 0, 305, '查看详情', 'B', 'process:sprayCondition:detail', 1, 0, 1),
(30502, 0, 305, '新增',     'B', 'process:sprayCondition:create', 2, 0, 1),
(30503, 0, 305, '修改',     'B', 'process:sprayCondition:update', 3, 0, 1),
(30504, 0, 305, '删除',     'B', 'process:sprayCondition:delete', 4, 0, 1);

-- 工艺-机加程序参数 (306)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(30601, 0, 306, '查看详情', 'B', 'process:machiningProgram:detail', 1, 0, 1),
(30602, 0, 306, '新增',     'B', 'process:machiningProgram:create', 2, 0, 1),
(30603, 0, 306, '修改',     'B', 'process:machiningProgram:update', 3, 0, 1),
(30604, 0, 306, '删除',     'B', 'process:machiningProgram:delete', 4, 0, 1);

-- 工艺-制造BOM (307)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(30701, 0, 307, '查看详情', 'B', 'process:bom:detail',  1, 0, 1),
(30702, 0, 307, '新增',     'B', 'process:bom:create',  2, 0, 1),
(30703, 0, 307, '修改',     'B', 'process:bom:update',  3, 0, 1),
(30704, 0, 307, '删除',     'B', 'process:bom:delete',  4, 0, 1),
(30705, 0, 307, '版本升级', 'B', 'process:bom:upgrade', 5, 0, 1),
(30706, 0, 307, '发布',     'B', 'process:bom:publish', 6, 0, 1),
(30707, 0, 307, '停用',     'B', 'process:bom:disable', 7, 0, 1);

-- 工艺-工艺路线 (308)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(30801, 0, 308, '查看详情', 'B', 'process:route:detail', 1, 0, 1),
(30802, 0, 308, '新增',     'B', 'process:route:create', 2, 0, 1),
(30803, 0, 308, '修改',     'B', 'process:route:update', 3, 0, 1),
(30804, 0, 308, '删除',     'B', 'process:route:delete', 4, 0, 1),
(30805, 0, 308, '启用',     'B', 'process:route:update', 5, 0, 1),
(30806, 0, 308, '停用',     'B', 'process:route:update', 6, 0, 1)
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  permission = VALUES(permission),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  is_template = VALUES(is_template);

-- 计划-订单计划 (401)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(40101, 0, 401, '查看详情', 'B', 'plan:order:detail',    1, 0, 1),
(40102, 0, 401, '新增',     'B', 'plan:order:create',    2, 0, 1),
(40103, 0, 401, '修改',     'B', 'plan:order:update',    3, 0, 1),
(40104, 0, 401, '删除',     'B', 'plan:order:delete',    4, 0, 1),
(40105, 0, 401, '下达',     'B', 'plan:order:release',   5, 0, 1),
(40106, 0, 401, '完成',     'B', 'plan:order:complete',  6, 0, 1),
(40107, 0, 401, '终止',     'B', 'plan:order:terminate', 7, 0, 1),
(40108, 0, 401, '展开',     'B', 'plan:order:expand',    8, 0, 1),
(40109, 0, 401, '状态日志', 'B', 'plan:order:log',       9, 0, 1);

-- 计划-生产计划 (402)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(40201, 0, 402, '查看详情', 'B', 'plan:production:detail',  1, 0, 1),
(40202, 0, 402, '新增',     'B', 'plan:production:create',  2, 0, 1),
(40203, 0, 402, '修改',     'B', 'plan:production:update',  3, 0, 1),
(40204, 0, 402, '删除',     'B', 'plan:production:delete',  4, 0, 1),
(40205, 0, 402, '下达',     'B', 'plan:production:release', 5, 0, 1),
(40206, 0, 402, '状态日志', 'B', 'plan:production:log',     6, 0, 1);

-- 工单管理 (501)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(50101, 0, 501, '查看详情',   'B', 'workorder:workorder:detail',         1, 0, 1),
(50102, 0, 501, '新增',       'B', 'workorder:workorder:create',         2, 0, 1),
(50103, 0, 501, '修改',       'B', 'workorder:workorder:update',         3, 0, 1),
(50104, 0, 501, '删除',       'B', 'workorder:workorder:delete',         4, 0, 1),
(50105, 0, 501, '下发',       'B', 'workorder:workorder:release',        5, 0, 1),
(50106, 0, 501, '开工',       'B', 'workorder:workorder:start',          6, 0, 1),
(50107, 0, 501, '完工',       'B', 'workorder:workorder:complete',       7, 0, 1),
(50108, 0, 501, '强制完工',   'B', 'workorder:workorder:forceComplete',  8, 0, 1),
(50109, 0, 501, '状态日志',   'B', 'workorder:workorder:log',            9, 0, 1),
(50110, 0, 501, '附件-查看', 'B', 'workorder:attachment:list',          10, 0, 1),
(50111, 0, 501, '附件-新增', 'B', 'workorder:attachment:create',        11, 0, 1),
(50112, 0, 501, '附件-删除', 'B', 'workorder:attachment:delete',        12, 0, 1);

-- 派工管理 (601)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(60101, 0, 601, '查看详情',     'B', 'dispatch:task:detail',           1, 0, 1),
(60102, 0, 601, '从工单生成',   'B', 'dispatch:task:generate',         2, 0, 1),
(60103, 0, 601, '手动创建',     'B', 'dispatch:task:create',           3, 0, 1),
(60104, 0, 601, '修改',         'B', 'dispatch:task:update',           4, 0, 1),
(60105, 0, 601, '撤销',         'B', 'dispatch:task:cancel',           5, 0, 1),
(60106, 0, 601, '指派',         'B', 'dispatch:task:assign',           6, 0, 1),
(60107, 0, 601, '取消指派',     'B', 'dispatch:task:unassign',         7, 0, 1),
(60108, 0, 601, '开工',         'B', 'dispatch:task:start',            8, 0, 1),
(60109, 0, 601, '完工',         'B', 'dispatch:task:complete',         9, 0, 1),
(60110, 0, 601, '人员分派',     'B', 'dispatch:assignment:assignPerson', 10, 0, 1),
(60111, 0, 601, '设备分派',     'B', 'dispatch:assignment:assignDevice', 11, 0, 1),
(60112, 0, 601, '班组分派',     'B', 'dispatch:assignment:assignTeam',   12, 0, 1),
(60113, 0, 601, '撤销分派',     'B', 'dispatch:assignment:revoke',       13, 0, 1),
(60114, 0, 601, '查询分派记录', 'B', 'dispatch:assignment:list',         14, 0, 1);

-- 异常联络单 (701)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(70101, 0, 701, '查看详情',   'B', 'abnormal:contact:detail',            1, 0, 1),
(70102, 0, 701, '新增',       'B', 'abnormal:contact:create',            2, 0, 1),
(70103, 0, 701, '修改',       'B', 'abnormal:contact:update',            3, 0, 1),
(70104, 0, 701, '删除',       'B', 'abnormal:contact:delete',            4, 0, 1),
(70105, 0, 701, '提交',       'B', 'abnormal:contact:submit',            5, 0, 1),
(70106, 0, 701, '开始处理',   'B', 'abnormal:contact:process',           6, 0, 1),
(70107, 0, 701, '关闭',       'B', 'abnormal:contact:close',             7, 0, 1),
(70108, 0, 701, '上传附件',   'B', 'abnormal:contact:uploadAttachment',  8, 0, 1),
(70109, 0, 701, '删除附件',   'B', 'abnormal:contact:deleteAttachment',  9, 0, 1),
(70110, 0, 701, '签署附件',   'B', 'abnormal:contact:signAttachment',   10, 0, 1);

-- 质量-复检申请 (801)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(80101, 0, 801, '查看详情', 'B', 'quality:recheck:detail', 1, 0, 1),
(80102, 0, 801, '新增',     'B', 'quality:recheck:create', 2, 0, 1),
(80103, 0, 801, '修改',     'B', 'quality:recheck:update', 3, 0, 1),
(80104, 0, 801, '删除',     'B', 'quality:recheck:delete', 4, 0, 1);

-- 质量-工作开工检查 (802)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(80201, 0, 802, '查看详情', 'B', 'quality:workStartCheck:detail', 1, 0, 1),
(80202, 0, 802, '新增',     'B', 'quality:workStartCheck:create', 2, 0, 1),
(80203, 0, 802, '修改',     'B', 'quality:workStartCheck:update', 3, 0, 1);

-- 质量-工单开工检查 (803)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(80301, 0, 803, '查看详情', 'B', 'quality:orderStartCheck:detail', 1, 0, 1),
(80302, 0, 803, '新增',     'B', 'quality:orderStartCheck:create', 2, 0, 1),
(80303, 0, 803, '修改',     'B', 'quality:orderStartCheck:update', 3, 0, 1);

-- 质量-交班记录 (804)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(80401, 0, 804, '查看详情',     'B', 'quality:shiftHandover:detail',   1, 0, 1),
(80402, 0, 804, '新增',         'B', 'quality:shiftHandover:create',   2, 0, 1),
(80403, 0, 804, '修改',         'B', 'quality:shiftHandover:update',   3, 0, 1),
(80404, 0, 804, '接收',         'B', 'quality:shiftHandover:receive',  4, 0, 1),
(80405, 0, 804, '附件-查看',    'B', 'query:shiftHandoverAttachment:list',   5, 0, 1),
(80406, 0, 804, '附件-新增',    'B', 'query:shiftHandoverAttachment:create', 6, 0, 1),
(80407, 0, 804, '附件-删除',    'B', 'query:shiftHandoverAttachment:delete', 7, 0, 1);

-- 查询类 按钮
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(90201, 0, 902, '查看详情', 'B', 'query:productionWork:detail', 1, 0, 1),
(90301, 0, 903, '查看详情', 'B', 'query:inspectionWork:detail', 1, 0, 1);

-- 物料-库存 (1001)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(100101, 0, 1001, '查看详情', 'B', 'material:inventory:detail', 1, 0, 1),
(100102, 0, 1001, '新增',     'B', 'material:inventory:create', 2, 0, 1),
(100103, 0, 1001, '修改',     'B', 'material:inventory:update', 3, 0, 1);

-- 物料-生产领料 (1002)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(100201, 0, 1002, '查看详情', 'B', 'material:requisition:detail', 1, 0, 1),
(100202, 0, 1002, '新增',     'B', 'material:requisition:create', 2, 0, 1),
(100203, 0, 1002, '修改',     'B', 'material:requisition:update', 3, 0, 1),
(100204, 0, 1002, '删除',     'B', 'material:requisition:delete', 4, 0, 1);

-- 物料-按物料领料 (1003)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(100301, 0, 1003, '查看详情', 'B', 'material:requisitionOrder:detail', 1, 0, 1),
(100302, 0, 1003, '新增',     'B', 'material:requisitionOrder:create', 2, 0, 1),
(100303, 0, 1003, '修改',     'B', 'material:requisitionOrder:update', 3, 0, 1),
(100304, 0, 1003, '删除',     'B', 'material:requisitionOrder:delete', 4, 0, 1);

-- 物料-完工入库申请 (1004)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(100401, 0, 1004, '查看详情', 'B', 'material:receiptRequest:detail', 1, 0, 1),
(100402, 0, 1004, '新增',     'B', 'material:receiptRequest:create', 2, 0, 1),
(100403, 0, 1004, '修改',     'B', 'material:receiptRequest:update', 3, 0, 1),
(100404, 0, 1004, '删除',     'B', 'material:receiptRequest:delete', 4, 0, 1);

-- 物料-完工入库 (1005)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(100501, 0, 1005, '查看详情', 'B', 'material:receipt:detail', 1, 0, 1),
(100502, 0, 1005, '新增',     'B', 'material:receipt:create', 2, 0, 1),
(100503, 0, 1005, '修改',     'B', 'material:receipt:update', 3, 0, 1),
(100504, 0, 1005, '删除',     'B', 'material:receipt:delete', 4, 0, 1);

-- 物料-生产退料 (1006)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(100601, 0, 1006, '查看详情', 'B', 'material:return:detail', 1, 0, 1),
(100602, 0, 1006, '新增',     'B', 'material:return:create', 2, 0, 1),
(100603, 0, 1006, '修改',     'B', 'material:return:update', 3, 0, 1),
(100604, 0, 1006, '删除',     'B', 'material:return:delete', 4, 0, 1);

-- 物料-发货签收 (1007)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(100701, 0, 1007, '新增',   'B', 'material:deliverySign:create',  1, 0, 1),
(100702, 0, 1007, '确认',   'B', 'material:deliverySign:confirm', 2, 0, 1);

-- APS-同步配置 (1101)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(110101, 0, 1101, '查看详情', 'B', 'aps:syncConfig:detail', 1, 0, 1),
(110102, 0, 1101, '新增',     'B', 'aps:syncConfig:create', 2, 0, 1),
(110103, 0, 1101, '修改',     'B', 'aps:syncConfig:update', 3, 0, 1),
(110104, 0, 1101, '删除',     'B', 'aps:syncConfig:delete', 4, 0, 1);

-- APS-同步日志 (1102)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(110201, 0, 1102, '查看详情', 'B', 'aps:syncLog:detail', 1, 0, 1);

-- APS-数据映射 (1103)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(110301, 0, 1103, '查看详情', 'B', 'aps:dataMapping:detail', 1, 0, 1),
(110302, 0, 1103, '新增',     'B', 'aps:dataMapping:create', 2, 0, 1),
(110303, 0, 1103, '修改',     'B', 'aps:dataMapping:update', 3, 0, 1),
(110304, 0, 1103, '删除',     'B', 'aps:dataMapping:delete', 4, 0, 1);

-- APS-同步操作（没有一级菜单展示，直接挂 APS 目录 11 下作为隐藏按钮）
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(11001, 0, 11, '下行同步',     'B', 'aps:sync:downstream', 91, 0, 1),
(11002, 0, 11, '上行同步',     'B', 'aps:sync:upstream',   92, 0, 1),
(11003, 0, 11, '补偿同步',     'B', 'aps:sync:compensate', 93, 0, 1),
(11004, 0, 11, '状态/健康',    'B', 'aps:sync:status',     94, 0, 1),
(11005, 0, 11, '主数据同步',   'B', 'aps:sync:masterData', 95, 0, 1);

-- 系统-用户管理 (1201)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(120101, 0, 1201, '查看详情', 'B', 'system:user:detail',    1, 0, 1),
(120102, 0, 1201, '新增',     'B', 'system:user:create',    2, 0, 1),
(120103, 0, 1201, '修改',     'B', 'system:user:update',    3, 0, 1),
(120104, 0, 1201, '删除',     'B', 'system:user:delete',    4, 0, 1),
(120105, 0, 1201, '重置密码', 'B', 'system:user:resetPwd',  5, 0, 1);

-- 系统-角色管理 (1202)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(120201, 0, 1202, '查看详情',     'B', 'system:role:detail',     1, 0, 1),
(120202, 0, 1202, '新增',         'B', 'system:role:create',     2, 0, 1),
(120203, 0, 1202, '修改',         'B', 'system:role:update',     3, 0, 1),
(120204, 0, 1202, '删除',         'B', 'system:role:delete',     4, 0, 1),
(120205, 0, 1202, '菜单授权',     'B', 'system:role:assignMenu', 5, 0, 1);

-- 系统-菜单管理 (1203)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(120301, 0, 1203, '查看详情', 'B', 'system:menu:detail', 1, 0, 1),
(120302, 0, 1203, '新增',     'B', 'system:menu:create', 2, 0, 1),
(120303, 0, 1203, '修改',     'B', 'system:menu:update', 3, 0, 1),
(120304, 0, 1203, '删除',     'B', 'system:menu:delete', 4, 0, 1);

-- =============================================================
-- 3. 基础设施类（没有明确父菜单）权限：挂到"系统管理"目录 12 下
-- =============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template) VALUES
(30001, 0, 12, '文件上传',   'B', 'system:file:upload',        81, 0, 1),
(30002, 0, 12, '文件删除',   'B', 'system:file:delete',        82, 0, 1),
-- 平台超管租户 (独立于单租户)
(30010, 0, 12, '平台-租户列表',   'B', 'platform:tenant:list',        91, 0, 1),
(30011, 0, 12, '平台-租户详情',   'B', 'platform:tenant:detail',      92, 0, 1),
(30012, 0, 12, '平台-租户暂停',   'B', 'platform:tenant:suspend',     93, 0, 1),
(30013, 0, 12, '平台-租户归档',   'B', 'platform:tenant:archive',     94, 0, 1),
(30014, 0, 12, '平台-重新初始化', 'B', 'platform:tenant:reprovision', 95, 0, 1);

-- =============================================================
-- 4. 把租户 1 的 ADMIN 角色重新绑定"全部平台模板菜单"
--    这样升级后 admin 立即拥有本次新增的全部 permission
-- =============================================================
INSERT INTO sys_role_menu (tenant_id, role_id, menu_id)
SELECT 1 AS tenant_id, r.id AS role_id, m.id AS menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.tenant_id = 1
  AND r.role_code = 'ADMIN'
  AND m.tenant_id = 0
  AND m.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- =============================================================
-- 5. 同步把平台超管租户 (tenant_id=0) 下的 ADMIN 也绑上（若存在）
--    允许平台侧测试环境直接查菜单。
-- =============================================================
INSERT INTO sys_role_menu (tenant_id, role_id, menu_id)
SELECT 0 AS tenant_id, r.id AS role_id, m.id AS menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.tenant_id = 0
  AND r.role_code = 'ADMIN'
  AND m.tenant_id = 0
  AND m.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
