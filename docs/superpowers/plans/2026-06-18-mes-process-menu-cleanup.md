# MES Process Menu Cleanup Plan

## Goal

Clean up redundant entries under `工艺管理` by fixing the real RBAC menu tree issue and aligning the visible process menu with the current process domain model.

## Root Cause

- `sys_menu`, `sys_role_menu`, and `sys_user_role` are excluded from the MyBatis tenant interceptor, but menu queries were not consistently scoped by tenant.
- Button permission rows (`menu_type = 'B'`) could be returned into the tree-building path, so permission nodes could appear as navigation children.
- The backend already had `/process/route` APIs, but the RBAC seed and frontend route/menu entry were missing.
- Existing labels mixed overlapping terms such as `指示书管理`, `指导书管理`, `工序信息`, and parameter table names, making the process menu look duplicated.

## Implementation

- Add `SysMenuServiceTenantIsolationTest` to cover tenant-context fail-fast behavior, tenant-scoped menu tree queries, and button-node filtering.
- Scope user menu and role-menu mapper queries with tenant id.
- Make `SysMenuServiceImpl` require tenant context, query menu trees by tenant, fall back to platform templates only when a tenant has no menu rows, and exclude `menu_type = 'B'` from navigation trees.
- Add the `工艺路线` menu (`308`) and `process:route:*` button permissions to `V2.05__menu_permissions.sql`.
- Rename and reorder the process menu to:
  `工艺路线`, `工序库`, `工序模板`, `作业指导书`, `执行指示`, `制造BOM`, `喷涂参数`, `机加程序参数`.
- Add frontend API/page/route for `工艺路线`.
- Align process page titles and local static/i18n menu labels with the cleaned menu structure.
- Add `308` to the production-manager seed role menu list for local demo/test data.

## Verification

- `mvn -pl mes-system -Dtest=SysMenuServiceTenantIsolationTest,SysRoleMenuTenantIsolationTest,SysUserRoleTenantIsolationTest test`
  - Result: 14 tests, 0 failures, 0 errors.
- `npm run build`
  - Result: `vue-tsc` and `vite build` completed with exit code 0.
- Static audit confirmed the new labels and `process:route:*` permissions are present, with the old menu titles removed from frontend process page titles and menu config.
- `git diff --check`
  - Result: no whitespace errors.
