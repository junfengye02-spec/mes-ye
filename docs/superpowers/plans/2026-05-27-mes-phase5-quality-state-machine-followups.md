# MES Phase 5 Quality Workflow, Inspection Projection, Document Boundary, Dispatch Failure, and APS Callback Completion Plan

> Scope for this execution slice: land the most localized and unblocker-friendly Phase 5 items first, `5.2 InspectionWork` write-path completion, `5.3 RecheckRequest` state-machine completion, `5.4 WorkOrderEventListener` failure propagation, a concrete `4.2 Instruction / WorkInstruction` boundary cut, and then close `12.5 APS callback` end-to-end across `mrp-result`, `gantt-data`, `capacity-load`, and `schedule-change(CANCELLED)`.

**Goal:** Complete the missing `RecheckRequest` lifecycle so quality-created recheck records can move beyond `CREATED`, add the missing event-driven `InspectionWork` projection path, make `Instruction` vs `WorkInstruction` responsibilities explicit in the data model, stop dispatch-generation failures from being silently swallowed after work-order release, and close the outstanding APS callback gaps that blocked scheduling views and downstream material/dispatch reaction.

**Deferred after this slice:** move on from `12.5 APS callback` to the next cross-phase gaps such as `12.4` route sync completeness and `12.6` sync-type simplification.

## Task 1: Add the missing inspection-work write path

- [x] Keep `InspectionWork` as a query/projection model instead of exposing a new manual CRUD flow.
- [x] Add common event `RecheckCompletedEvent` so recheck completion can project into query data without introducing module cycles.
- [x] Extend `IInspectionWorkService` with internal projection methods:
  - `projectDispatchCompletion(DispatchTaskCompletedEvent event)`
  - `projectRecheckCompletion(RecheckCompletedEvent event)`
- [x] Implement deterministic projection keys:
  - `IW-DISPATCH-{dispatchTaskId}`
  - `IW-RECHECK-{recheckId}`
- [x] Project dispatch completion into completed inspection-work records with:
  - quantities from `actualQty`
  - `PASS/FAIL` mapped to `合格/不合格`
  - category `完工检验`
- [x] Project recheck completion into completed inspection-work records with category `复检`.
- [x] Add `InspectionWorkProjectionListener` in `mes-query` using `@TransactionalEventListener(AFTER_COMMIT)` so query writes happen only after source transactions commit.

## Task 2: Clarify Instruction vs WorkInstruction responsibility

- [x] Choose explicit separation instead of forced merge:
  - `Instruction` = 随工单执行指示 / 流转卡
  - `WorkInstruction` = 可复用作业指导书（SOP）模板
- [x] Add `Instruction.workInstructionId` so execution documents can explicitly reference a standard work-instruction template.
- [x] Surface `workInstructionId` through:
  - `Instruction` entity
  - `InstructionDTO`
  - `InstructionVO`
- [x] Update controller / schema descriptions so API semantics no longer leave the two concepts ambiguous.
- [x] Add migration `V2.20__phase5_instruction_work_instruction_boundary.sql`.
- [x] Align the instruction frontend type and form with the new link field so the boundary is not backend-only.
- [x] Add/extend `InstructionServiceTest` to verify `workInstructionId` persists through create/detail mapping.

## Task 3: Complete the recheck status model

- [x] Extend `RecheckStatus` from a single `CREATED` value to:
  - `CREATED`
  - `SUBMITTED`
  - `IN_REVIEW`
  - `APPROVED`
  - `REJECTED`
  - `COMPLETED`
- [x] Add request DTOs for review/approval actions:
  - `RecheckReviewDTO`
  - `RecheckApproveDTO`
- [x] Extend `IRecheckRequestService` with:
  - `submit(Long id)`
  - `review(Long id, RecheckReviewDTO dto)`
  - `approve(Long id, RecheckApproveDTO dto)`
  - `complete(Long id)`

## Task 4: Implement the lifecycle transitions

- [x] Implement `submit()` with `CREATED -> SUBMITTED`.
- [x] Implement `review()` with `SUBMITTED -> IN_REVIEW`.
- [x] Persist optional review metadata:
  - `reviewer`
  - `reviewDate`
  - `isReasonable`
- [x] Implement `approve()` with `IN_REVIEW -> APPROVED/REJECTED`.
- [x] Implement `complete()` with `APPROVED -> COMPLETED`.
- [x] Reject invalid source states with business assertions.
- [x] Reuse APS sync publication for create and status transitions.
- [x] Publish `RecheckCompletedEvent` on `complete()` for downstream query projection.

## Task 5: Expose workflow endpoints

- [x] Add controller actions:
  - `POST /quality/recheck/{id}/submit`
  - `POST /quality/recheck/{id}/review`
  - `POST /quality/recheck/{id}/approve`
  - `POST /quality/recheck/{id}/complete`
- [x] Keep existing CRUD endpoints unchanged.

## Task 6: Verify with focused tests

- [x] Add `InspectionWorkServiceTest` coverage for:
  - dispatch-completion projection
  - recheck-completion projection
- [x] Add/extend `InstructionServiceTest` coverage for:
  - `workInstructionId` persistence on create
  - `workInstructionId` round-trip on detail
- [x] Add/finish `RecheckRequestServiceTest` coverage for:
  - submit transition
  - review transition and metadata persistence
  - approve-to-approved transition
  - approve-to-rejected transition
  - complete transition
  - invalid submit transition rejection
- [x] Assert `complete()` publishes `RecheckCompletedEvent`.
- [x] Re-run `QualityEventListenerTest` to confirm quality event creation flow remains green.

## Task 7: Stop swallowing dispatch generation failures

- [x] Add `WorkOrderEventListenerTest` coverage for:
  - successful auto-generation delegation
  - exception propagation when `generateFromWorkOrder()` fails
- [x] Change `mes-dispatch` `WorkOrderEventListener` to log and rethrow runtime failures instead of swallowing them.
- [x] Verify existing work-order/dispatch happy-path tests still pass after the failure-propagation change.

## Task 8: Persist APS gantt and capacity callback data

- [x] Add APS callback persistence entities and mappers for:
  - `mes_aps_gantt_cache`
  - `mes_aps_capacity_load`
- [x] Change `ApsExtendedCallbackServiceImpl.handleGanttData()` from log-only handling to cache replacement by `scheduleBatchId`.
- [x] Persist gantt callback rows with:
  - task identity / work-order / process / resource fields
  - task start/end, duration, status, priority
  - serialized `predecessors`
  - callback range metadata
- [x] Change `ApsExtendedCallbackServiceImpl.handleCapacityLoad()` from log-only handling to batch replacement by `scheduleBatchId`.
- [x] Persist capacity callback rows with:
  - work center identity
  - load date and calculated timestamp
  - available / scheduled capacity
  - load rate and overloaded flag
- [x] Add focused assertions in `ApsIntegrationFullTest` proving both callbacks delete stale rows for the batch and insert the new cache records.
- [x] Emit a warning log when overloaded capacity rows are received so this slice at least surfaces pressure immediately even before a dedicated alerting workflow exists.

## Task 9: Complete APS MRP and cancellation callbacks

- [x] Add `IMaterialRequisitionService.createFromMrp()` so APS MRP callback handling can generate request documents without reusing the stock-deducting interactive create flow.
- [x] Implement `MaterialRequisitionServiceImpl.createFromMrp()` with:
  - header generation in `CREATED` status
  - auto-generated requisition number
  - APS audit attribution
  - item rows with `pendingQty = demandQty`
  - `issueQty = 0`
  - no inventory deduction
  - no work-order issued-qty mutation
- [x] Extend `MaterialRequisitionServiceTest` to prove the APS/MRP generation path creates a request-only document instead of executing material issue side effects.
- [x] Change `ApsExtendedCallbackServiceImpl.handleMrpResult()` from log-only handling to:
  - group callback items by work order
  - resolve work order and material master references
  - build requisition DTOs
  - delegate to `createFromMrp()`
  - record per-group success / failure counts in the sync log
- [x] Add `ApsIntegrationFullTest` assertions proving MRP callback handling now delegates to requisition creation with mapped work-order/material context.
- [x] Change `schedule-change(CANCELLED)` handling to:
  - cancel non-completed dispatch tasks through `IDispatchTaskService.cancel()`
  - clear work-order plan start / end times
  - append an APS cancellation remark onto the work order for manual traceability
- [x] Add `ApsIntegrationFullTest` coverage proving APS cancellation now revokes unfinished dispatch tasks while leaving completed tasks untouched.

Verification run during this slice:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-query,mes-quality -am -Dtest=InspectionWorkServiceTest,RecheckRequestServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-query,mes-quality -am -Dtest=InspectionWorkServiceTest,WorkStatusViewServiceTest,RecheckRequestServiceTest,QualityEventListenerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-process -am -Dtest=InstructionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-quality -am -Dtest=RecheckRequestServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-quality -am -Dtest=RecheckRequestServiceTest,QualityEventListenerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-common,mes-framework,mes-basic,mes-quality -am test-compile`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-dispatch -am -Dtest=WorkOrderEventListenerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-dispatch,mes-workorder -am -Dtest=WorkOrderEventListenerTest,DispatchServiceTest,WorkOrderServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-common,mes-framework,mes-workorder,mes-dispatch -am test-compile`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-common,mes-framework,mes-basic,mes-process -am test-compile`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-common,mes-framework,mes-quality,mes-query -am test-compile`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-aps -am -Dtest=ApsIntegrationFullTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-material,mes-aps -am -Dtest=MaterialRequisitionServiceTest,ApsIntegrationFullTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-common,mes-framework,mes-aps -am test-compile`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-common,mes-framework,mes-material,mes-aps -am test-compile`
- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run build`

## Remaining Phase 5 queue

- [x] `12.5` Finish APS callback TODOs and close the current integration loop.
- [x] `12.4` Complete MES -> APS process-route sync so APS receives ordered route-step payloads instead of flat `ProcessInfo` rows.
  - `ApsMasterDataSyncServiceImpl.syncProcessRoutes()` now queries active/effective `Route` headers plus ordered `RouteStep` rows.
  - Each route step is flattened into one APS payload row carrying route context, `sequenceNo`, `processSequence`, work-center/resource codes, `cycleTime`, and predecessor-derived dependency metadata.
  - Missing step display fields fall back to `ProcessInfo`, and work-center codes are resolved from `WorkCenter`.
- [x] `12.6` Simplify / normalize APS sync-type semantics across upstream queueing and callback handling.
  - [x] Localized first cut: unsupported MES→APS queue types are now rejected at `enqueue()` time instead of being inserted and failing later in the retry loop.
  - [x] Unsupported execution-feedback types now stop at `ApsExecutionFeedbackServiceImpl` and write an explicit local `ApsSyncLog` failure audit instead of silently disappearing behind queue-level skips.
  - [x] Keep the enum values as contract placeholders for now, but make unsupported semantics explicit until APS actually lands the missing endpoints.

## 2026-05-28 Follow-up: 12.4 Route Sync Completion

- [x] Add/extend `ApsIntegrationFullTest` so route sync is verified against a real `Route` + unordered `RouteStep` fixture instead of flat process rows.
- [x] Ensure sync output is step-ordered by `sequenceNo` and preserves predecessor linkage through:
  - `predecessorSequenceNo`
  - `dependencySequenceNos`
- [x] Keep route sync resilient when route-step rows omit duplicated process/work-center display data by resolving those fields lazily from master tables.

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-aps -am -Dtest=ApsIntegrationFullTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-common,mes-framework,mes-aps -am test-compile`

## 2026-05-28 Follow-up: 12.6 Queue-Semantics Guardrail

- [x] Add `ApsUpstreamSyncServiceTest` coverage proving unsupported sync types such as `DISPATCH` are skipped before queue insertion.
- [x] Move the "APS supports this as an upstream queue push" rule into `SyncType` so enqueue and queue-processing logic share one source of truth.
- [x] Keep `processQueue()`'s existing unsupported-type fail-fast behavior for legacy rows that may already exist in the queue table.

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-aps -am -Dtest=ApsUpstreamSyncServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-aps -am -Dtest=ApsUpstreamSyncServiceTest,ApsIntegrationFullTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-aps -am -Dtest=ApsExecutionFeedbackServiceTest,ApsUpstreamSyncServiceTest,ApsIntegrationFullTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 2026-05-28 Follow-up: Phase 7 Completion Evidence and 12.6 Real Enum Shrink

- [x] `8.1` 工单号碰撞风险已收口:
  - `ProductionPlanServiceImpl.generateWorkOrderNo()` 改为使用 `DistributedIdGenerator`，不再依赖 `Math.random()`.
- [x] `8.2` 库存选取已改为确定性排序:
  - `MaterialRequisitionServiceImpl` / `DeliverySignServiceImpl` 不再依赖歧义 `LIMIT 1`，而是按仓库/库位偏好、库存量、主键顺序稳定选取库存记录。
- [x] `8.3` `MaterialRequisition` 僵尸字段前后端契约已对齐:
  - 后端 DTO/VO 已贯通；
  - 前端类型与页面在 `material-mgmt.ts`、`material-mgmt.contract.ts`、`RequisitionList.vue` 中完成同步。
- [x] `11.1` 首批 10 个未继承 `BaseEntity` 的实体已统一，并补充数据库迁移 `V2.21__phase7_base_entity_unification.sql`.
- [x] `12.6` 从“语义护栏”推进到“真正精简”:
  - `SyncType` 收缩为 20 个真实 MES↔APS 合同类型，不再把 9 个 APS 尚未支持的执行反馈码混入对外合同枚举；
  - 新增 `ApsExecutionFeedbackType` 作为 `mes-aps` 内部反馈类型，保留原有事件分发能力；
  - `ApsExecutionFeedbackListener` / `ApsSyncEventListener` 改为以内部反馈枚举判定反馈事件；
  - `ApsExecutionFeedbackServiceImpl` 保持现有“本地失败审计、不入上行队列”的兼容行为；
  - `ApsIntegrationFullTest` / `SyncTypeSemanticsTest` / `ApsExecutionFeedbackServiceTest` / `ApsUpstreamSyncServiceTest` 已改为验证新的边界。
- [x] APS 同步日志前端字典已补全:
  - 即使 `DISPATCH` / `START_CHECK` 等反馈码已从 `SyncType` 收缩出去，`SyncLogList.vue` 仍能通过 `dict.ts` 正常显示和筛选这些本地失败审计记录。

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-abnormal,mes-query,mes-quality,mes-material -am -Dtest=AbnormalBaseEntityUnificationTest,QueryBaseEntityUnificationTest,QualityBaseEntityUnificationTest,MaterialBaseEntityUnificationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-abnormal,mes-quality,mes-query,mes-material -am -Dtest=AbnormalContactServiceTest,RecheckRequestServiceTest,WorkStatusViewServiceTest,DeliverySignServiceTest,MaterialRequisitionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-material -am -Dtest=MaterialManagementServiceTest,MaterialMgmtModuleTest,MaterialRequisitionServiceTest,DeliverySignServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-aps -am -Dtest=SyncTypeSemanticsTest,ApsExecutionFeedbackServiceTest,ApsUpstreamSyncServiceTest,ApsIntegrationFullTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run build`
- `mvn -f /Users/jf/Desktop/apsYe/titan-aps-cloud/pom.xml -pl aps-common/aps-common-api,aps-schedule-service -am -Dtest=MesIntegrationServiceMasterDataTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/apsYe/titan-aps-cloud/pom.xml -pl aps-common,aps-order-service,aps-resource-service,aps-schedule-service -am test-compile`

## 2026-05-28 Follow-up: Recheck Frontend Workflow Closure

- [x] Frontend `recheckStatus` 字典补齐为完整状态机:
  - `CREATED`
  - `SUBMITTED`
  - `IN_REVIEW`
  - `APPROVED`
  - `REJECTED`
  - `COMPLETED`
- [x] `mes-frontend/src/api/quality/recheckRequest.ts` 已补全复检工作流接口:
  - `submit`
  - `review`
  - `approve`
  - `complete`
- [x] `mes-frontend/src/views/quality/RecheckRequestList.vue` 已从纯 CRUD 页面升级为状态流页面:
  - `CREATED` 行展示 `编辑 / 删除 / 提交`
  - `SUBMITTED` 行展示 `审核`
  - `IN_REVIEW` 行展示 `批准 / 驳回`
  - `APPROVED` 行展示 `完结`
  - 审核动作新增前端弹窗，提交 `reviewer / reviewDate / isReasonable`
- [x] 新增 Playwright 页面级回归 `mes-frontend/tests/e2e/recheck.spec.ts`，验证:
  - 状态筛选包含完整复检状态机选项
  - 复检列表在不同状态下展示对应工作流动作
  - 页面会调用 `submit / review / approve / complete` 对应前端请求链路

Verification run for this follow-up:

- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run test:e2e -- tests/e2e/recheck.spec.ts --project=chromium`
- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run build`

## 2026-05-28 Follow-up: Fresh Proof for 11.1 / 4.2 / 5.4

- [x] `11.1` BaseEntity 统一在当前代码上复验通过:
  - `AbnormalBaseEntityUnificationTest`
  - `QualityBaseEntityUnificationTest`
  - `QueryBaseEntityUnificationTest`
  - `MaterialBaseEntityUnificationTest`
- [x] `4.2` `Instruction / WorkInstruction` 边界在当前代码上复验通过:
  - `InstructionServiceTest` 继续验证 `workInstructionId` 持久化与详情回传。
- [x] `5.4` 派工生成失败不再静默吞错在当前代码上复验通过:
  - `WorkOrderEventListenerTest` 继续验证异常会记录日志并向上抛出。

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-abnormal,mes-query,mes-quality,mes-material -am -Dtest=AbnormalBaseEntityUnificationTest,QueryBaseEntityUnificationTest,QualityBaseEntityUnificationTest,MaterialBaseEntityUnificationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-process -am -Dtest=InstructionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-dispatch -am -Dtest=WorkOrderEventListenerTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 2026-05-28 Follow-up: Fresh Proof for 5.2 InspectionWork Projection

- [x] `5.2` 在当前代码上复验通过，已经不是“只有查没有写”的空壳查询服务：
  - `InspectionWorkServiceImpl` 提供了 `projectDispatchCompletion()` / `projectRecheckCompletion()` 两条内部投影写路径；
  - `InspectionWorkProjectionListener` 使用 `@TransactionalEventListener(AFTER_COMMIT)` 监听 `DispatchTaskCompletedEvent` 与 `RecheckCompletedEvent`，在源事务提交后写入查询投影；
  - `InspectionWorkServiceTest` fresh 证明两条事件路径都会落成 `InspectionWork` 记录，且主键语义固定为 `IW-DISPATCH-{dispatchTaskId}` / `IW-RECHECK-{recheckId}`。

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-query -am -Dtest=InspectionWorkServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 2026-05-28 Follow-up: Fresh Proof for 1.3 / 5.3 Recheck Flow

- [x] `1.3` “派工完工 FAIL 自动触发复检”在当前代码上复验通过：
  - `QualityEventListenerTest` 继续证明 `DispatchTaskQualityFailedEvent` 会自动创建 `RecheckRequestDTO`；
  - 复检申请会携带 `workOrderId` / `dispatchTaskId`，并把 `productionOrderNo` 指向当前工单号，满足派工失败追溯要求。
- [x] `5.3` `RecheckRequest` 状态机在当前代码上复验通过：
  - `RecheckRequestServiceTest` 继续覆盖 `submit -> review -> approve/reject -> complete` 的核心状态迁移与非法源状态拦截；
  - 前端 `recheck.spec.ts` fresh 证明列表页仍能展示完整状态机选项，并按状态展示对应工作流动作。

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-quality -am -Dtest=QualityEventListenerTest,RecheckRequestServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run test:e2e -- tests/e2e/recheck.spec.ts --project=chromium`

## 2026-05-28 Follow-up: Fresh Proof for 12.1 APS Integration Architecture Overview

- [x] `12.1` 不是一个独立待修复的单点缺陷，而是对 MES ↔ APS 当前集成拓扑的描述性总览；此前真正的实现缺口已经分别落在 `12.2` ~ `12.6`，现已在代码和子项 follow-up 中逐项收口。
- [x] 当前代码中的拓扑已经能和 `12.1` 的架构图一一对上：
  - `ApsMasterDataSyncServiceImpl` 负责 MES -> APS 的 5 类主数据推送，目标端点为 `/api/mes/master-data/*`；
  - APS 侧 `MesIntegrationController` 现已补齐对应 5 个接收端点，`MesIntegrationServiceImpl` 会把请求落为待执行记录并进入主数据执行分支；
  - `ApsUpstreamSyncServiceImpl` 继续承担事件 -> 队列 -> HTTP 的上行链路，但现在只允许真实受支持的上行合同类型入队；
  - `ApsDownstreamSyncServiceImpl` 继续承担从 APS 拉取 `orders / tasks / resources / calendars / outsource / transfer` 的下行同步；
  - `ApsCallbackController` + `ApsExtendedCallbackController` 现已覆盖 `schedule-result`、`request-rejected`、`mrp-result`、`resource-allocation`、`gantt-data`、`capacity-load`、`schedule-change`，其中 `ApsExtendedCallbackServiceImpl` 已不再是空壳日志实现。
- [x] 本轮复验后的归档结论是：
  - `12.1` 应按“描述性概览项，现已由子项实现和显式架构证据覆盖”关闭；
  - 后续若再审计 APS 集成，不应把 `12.1` 当作新的待开发缺口，而应回到 `12.2` ~ `12.6` 的合同边界逐项核对。

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-aps -am -Dtest=ApsIntegrationFullTest,SyncTypeSemanticsTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/apsYe/titan-aps-cloud/pom.xml -pl aps-schedule-service -am -Dtest=MesIntegrationServiceMasterDataTest -Dsurefire.failIfNoSpecifiedTests=false test`
