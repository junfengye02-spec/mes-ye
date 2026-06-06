-- ============================================================
-- V2.23  AI助手权限
--
-- 目标：
--   1) 新增隐藏按钮权限 ai:assistant:chat，用于控制顶部 AI 助手入口和后端问答接口。
--   2) 不新增可见菜单页，避免把 AI 助手变成独立路由。
--   3) 给租户 ADMIN 角色绑定该权限；新租户仍会通过平台模板菜单克隆获得权限模板。
--
-- 依赖：V2.05__menu_permissions.sql 已执行。
-- ============================================================

INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, permission, sort_order, visible, is_template)
VALUES (30020, 0, 12, 'AI助手问答', 'B', 'ai:assistant:chat', 96, 0, 1)
ON DUPLICATE KEY UPDATE
  permission = VALUES(permission),
  menu_name = VALUES(menu_name),
  visible = VALUES(visible),
  is_template = VALUES(is_template);

INSERT INTO sys_role_menu (tenant_id, role_id, menu_id)
SELECT 1 AS tenant_id, r.id AS role_id, m.id AS menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.tenant_id = 1
  AND r.role_code = 'ADMIN'
  AND m.tenant_id = 0
  AND m.id = 30020
  AND m.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_role_menu (tenant_id, role_id, menu_id)
SELECT 0 AS tenant_id, r.id AS role_id, m.id AS menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.tenant_id = 0
  AND r.role_code = 'ADMIN'
  AND m.tenant_id = 0
  AND m.id = 30020
  AND m.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
