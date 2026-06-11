# MES Phase 6 Expand and Frontend Follow-up

> Scope for this execution slice: continue the next-stage items from `docs/ISSUES-AND-PLAN.md` after the APS route-sync fix landed, starting with `6.1 expand()` so order-plan expansion creates a real production-plan record instead of only flipping a status flag.

**Goal:** Make `OrderPlanServiceImpl.expand()` behave like its name implies by generating a `ProductionPlan` header from the released order plan and only then marking the order as expanded, while preserving the remaining Phase 6 frontend gaps (`7.1`, `7.2`) for the next slice.

## Task 1: Make order-plan expansion create production plans

- [x] Add `OrderPlanServiceTest` coverage proving `expand()`:
  - calls `IProductionPlanService.create()` with order-derived fields
  - updates `expandStatus` only after production-plan creation succeeds
  - keeps the order untouched when production-plan creation fails
- [x] Change `OrderPlanServiceImpl.expand()` to build a `ProductionPlanDTO` from the released order plan and delegate creation through `IProductionPlanService`.
- [x] Use provider-based lookup for `IProductionPlanService` so `OrderPlanServiceImpl` can call into production-plan creation without turning the service dependency into a hard constructor cycle.
- [x] Include the created production-plan id in the order-plan status log / service log so the expand action is traceable.

## Remaining Phase 6 queue

- [x] `6.1` `expand()` now creates a production plan before marking the order expanded.
- [x] `7.1` Add missing dispatch write APIs to `mes-frontend/src/api/dispatch/dispatchTask.ts` and surface the actions in the relevant Vue pages.
  - `dispatchTaskApi` now exposes `create / update / cancel / assign / unassign / start / complete` in addition to the legacy assignment helpers.
  - `DispatchTask.vue` now surfaces task creation/editing plus status-aware lifecycle actions: `开工`, `完工`, `撤销任务`, and routes assignment revocation through the unified task unassign endpoint.
  - `dispatchStatus` dictionary now covers `IN_PROGRESS / ABNORMAL / COMPLETED / CANCELLED / REVOKED`, so the list view reflects the backend state machine correctly.
- [x] `7.2` Reconcile query routes with a consistent frontend API layer for the query module.
  - `workQueryApi` now exposes wrappers for `work-start-check`, `order-start-check`, `shift-handover`, `work-order`, and `dispatch-work`.
  - All 8 query views now import from `@/api/query/workQuery` instead of mixing direct calls to quality / workorder / dispatch API modules.

## Task 2: Unify query-module API boundaries

- [x] Extend `mes-frontend/src/api/query/workQuery.ts` so every query route has a corresponding query-layer wrapper, even when the backend endpoint still lives under another domain namespace.
- [x] Update these views to use `workQueryApi` only:
  - `WorkStartCheckQuery.vue`
  - `OrderStartCheckQuery.vue`
  - `ShiftHandoverQuery.vue`
  - `WorkOrderQuery.vue`
  - `DispatchWorkQuery.vue`
- [x] Verify there are no remaining `@/api/quality/*`, `@/api/workorder/*`, or `@/api/dispatch/*` imports inside `mes-frontend/src/views/query`.

Verification run during this slice:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-plan -am -Dtest=OrderPlanServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-plan -am -Dtest=OrderPlanServiceTest,ProductionPlanServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-common,mes-framework,mes-plan -am test-compile`
- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run build`
- `npx playwright test tests/e2e/dispatch.spec.ts --project=chromium --reporter=line`

Notes from this slice:

- The new dispatch lifecycle UI regression originally got skipped because its seed data used a fixed team id (`90001`) that conflicted with existing shared local data. The spec now seeds a unique person assignee instead, so the lifecycle-button regression reliably exercises the frontend instead of short-circuiting in `beforeAll`.

## 2026-05-28 Follow-up: Fresh Proof for 6.1 / 7.1 / 7.2

- [x] `6.1` `expand()` 在当前代码上复验通过：
  - `OrderPlanServiceTest` 继续证明 `expand()` 会先创建 `ProductionPlan`，再更新 `expandStatus`；
  - `ProductionPlanServiceTest` 同步复验生产计划下达链路，确认 Phase 6 没有破坏前序 Phase 1 的工单生成语义。
- [x] `7.1` 派工前端写操作入口在当前代码上复验通过：
  - `dispatch.spec.ts` fresh 证明派工列表可达、工具条可用、生命周期动作入口可见；
  - 同一设备资源冲突与撤销后重新占用的数据级回归也继续通过，说明新增写操作入口未破坏关键派工行为。
- [x] `7.2` Query API 边界统一在当前代码上复验通过：
  - `src/views/query` 下已无直接 `@/api/quality/*`、`@/api/workorder/*`、`@/api/dispatch/*` 引用；
  - `mes-frontend` build fresh 通过，保留既有动态导入与 chunk size warning，但无构建失败。

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-plan -am -Dtest=OrderPlanServiceTest,ProductionPlanServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run test:e2e -- tests/e2e/dispatch.spec.ts --project=chromium`
- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run build`
