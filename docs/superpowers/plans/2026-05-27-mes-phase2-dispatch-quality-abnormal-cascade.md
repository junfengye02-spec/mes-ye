# MES Phase 2 Dispatch, Quality, and Abnormal Cascade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete Phase 2 from `docs/ISSUES-AND-PLAN.md`: make dispatch completion cascade into work-order and production-plan completion, create quality follow-up records from dispatch and abnormal events, and connect abnormal submissions back into dispatch and quality.

**Architecture:** Use Spring application events in `mes-common` as the contract boundary between dispatch, workorder, plan, quality, and abnormal modules. Keep business state transitions inside the owning service or listener, and use focused unit tests to verify every event hop and terminal state.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus 3.5, JUnit 5, Mockito, Maven, SQL migrations.

---

## File Structure

Create or finish in `mes-common`:

- `mes-backend/mes-common/src/main/java/com/mes/common/event/DispatchAllTasksCompletedEvent.java`: work-order-level completion signal from dispatch.
- `mes-backend/mes-common/src/main/java/com/mes/common/event/DispatchTaskStartedEvent.java`: dispatch start signal used by workorder and quality.
- `mes-backend/mes-common/src/main/java/com/mes/common/event/DispatchTaskQualityFailedEvent.java`: dispatch FAIL signal used by quality.
- `mes-backend/mes-common/src/main/java/com/mes/common/event/AbnormalSubmittedEvent.java`: abnormal submission signal used by dispatch and quality.
- `mes-backend/mes-common/src/main/java/com/mes/common/event/WorkOrderCompletedEvent.java`: work-order completion signal used by plan.

Modify in `mes-dispatch`:

- `mes-backend/mes-dispatch/src/main/java/com/mes/dispatch/enums/DispatchStatus.java`
- `mes-backend/mes-dispatch/src/main/java/com/mes/dispatch/service/impl/DispatchTaskServiceImpl.java`
- `mes-backend/mes-dispatch/src/main/java/com/mes/dispatch/listener/AbnormalEventListener.java`
- `mes-backend/mes-dispatch/src/test/java/com/mes/dispatch/service/DispatchServiceTest.java`
- `mes-backend/mes-dispatch/src/test/java/com/mes/dispatch/listener/AbnormalEventListenerTest.java`

Modify in `mes-workorder`:

- `mes-backend/mes-workorder/src/main/java/com/mes/workorder/service/impl/WorkOrderServiceImpl.java`
- `mes-backend/mes-workorder/src/main/java/com/mes/workorder/listener/DispatchLifecycleEventListener.java`
- `mes-backend/mes-workorder/src/test/java/com/mes/workorder/service/WorkOrderServiceTest.java`
- `mes-backend/mes-workorder/src/test/java/com/mes/workorder/listener/DispatchLifecycleEventListenerTest.java`

Modify in `mes-plan`:

- `mes-backend/mes-plan/src/main/java/com/mes/plan/enums/ProductionPlanStatus.java`
- `mes-backend/mes-plan/src/main/java/com/mes/plan/listener/ProductionPlanEventListener.java`
- `mes-backend/mes-plan/src/test/java/com/mes/plan/listener/ProductionPlanEventListenerTest.java`

Modify in `mes-quality`:

- `mes-backend/mes-quality/src/main/java/com/mes/quality/domain/entity/RecheckRequest.java`
- `mes-backend/mes-quality/src/main/java/com/mes/quality/domain/dto/RecheckRequestDTO.java`
- `mes-backend/mes-quality/src/main/java/com/mes/quality/domain/vo/RecheckRequestVO.java`
- `mes-backend/mes-quality/src/main/java/com/mes/quality/listener/QualityEventListener.java`
- `mes-backend/mes-quality/src/test/java/com/mes/quality/listener/QualityEventListenerTest.java`

Modify in `mes-abnormal`:

- `mes-backend/mes-abnormal/src/main/java/com/mes/abnormal/domain/entity/AbnormalContact.java`
- `mes-backend/mes-abnormal/src/main/java/com/mes/abnormal/domain/dto/AbnormalContactDTO.java`
- `mes-backend/mes-abnormal/src/main/java/com/mes/abnormal/domain/vo/AbnormalContactVO.java`
- `mes-backend/mes-abnormal/src/main/java/com/mes/abnormal/service/impl/AbnormalContactServiceImpl.java`
- `mes-backend/mes-abnormal/src/test/java/com/mes/abnormal/service/AbnormalContactServiceTest.java`
- `mes-backend/mes-abnormal/src/test/java/com/mes/abnormal/AbnormalModuleTest.java`

Migration:

- `sql/V2.13__phase2_dispatch_quality_abnormal_cascade.sql`

## Task 1: Lock the event contracts and persistence links

**Files:**
- Create or verify: `mes-backend/mes-common/src/main/java/com/mes/common/event/DispatchAllTasksCompletedEvent.java`
- Create or verify: `mes-backend/mes-common/src/main/java/com/mes/common/event/DispatchTaskStartedEvent.java`
- Create or verify: `mes-backend/mes-common/src/main/java/com/mes/common/event/DispatchTaskQualityFailedEvent.java`
- Create or verify: `mes-backend/mes-common/src/main/java/com/mes/common/event/AbnormalSubmittedEvent.java`
- Modify: `mes-backend/mes-abnormal/src/main/java/com/mes/abnormal/domain/entity/AbnormalContact.java`
- Modify: `mes-backend/mes-abnormal/src/main/java/com/mes/abnormal/domain/dto/AbnormalContactDTO.java`
- Modify: `mes-backend/mes-abnormal/src/main/java/com/mes/abnormal/domain/vo/AbnormalContactVO.java`
- Modify: `mes-backend/mes-quality/src/main/java/com/mes/quality/domain/entity/RecheckRequest.java`
- Modify: `mes-backend/mes-quality/src/main/java/com/mes/quality/domain/dto/RecheckRequestDTO.java`
- Modify: `mes-backend/mes-quality/src/main/java/com/mes/quality/domain/vo/RecheckRequestVO.java`
- Create or verify: `sql/V2.13__phase2_dispatch_quality_abnormal_cascade.sql`

- [x] **Step 1: Run the abnormal and quality tests that describe the link fields and events**

Run:

```bash
mvn -pl mes-backend/mes-abnormal,mes-backend/mes-quality -Dtest=AbnormalContactServiceTest,AbnormalModuleTest,QualityEventListenerTest test
```

Expected: failing compile or test assertions if `workOrderId`, `dispatchTaskId`, or the event payload fields are still missing or mismatched.

- [x] **Step 2: Implement the shared event payloads and link fields**

Make sure the event constructors expose the ids and labels consumed downstream:

```java
public class AbnormalSubmittedEvent extends ApplicationEvent {
    private final Long contactId;
    private final String contactNo;
    private final Long workOrderId;
    private final Long dispatchTaskId;
    private final String orderNo;
    private final String eventCategory;
    private final String abnormalDesc;
}
```

Persist cross-module link fields in both domains:

```java
public class AbnormalContact extends BaseEntity {
    private String contactNo;
    private Long workOrderId;
    private Long dispatchTaskId;
    // ...
}

public class RecheckRequest extends BaseEntity {
    private Long workOrderId;
    private Long dispatchTaskId;
    // ...
}
```

Migration must stay additive:

```sql
ALTER TABLE mes_abnormal_contact
    ADD COLUMN IF NOT EXISTS work_order_id BIGINT NULL COMMENT '关联工单ID' AFTER contact_no,
    ADD COLUMN IF NOT EXISTS dispatch_task_id BIGINT NULL COMMENT '关联派工任务ID' AFTER work_order_id;

ALTER TABLE mes_recheck_request
    ADD COLUMN IF NOT EXISTS work_order_id BIGINT NULL COMMENT '关联工单ID' AFTER id,
    ADD COLUMN IF NOT EXISTS dispatch_task_id BIGINT NULL COMMENT '关联派工任务ID' AFTER work_order_id;
```

- [x] **Step 3: Re-run the abnormal and quality tests**

Run:

```bash
mvn -pl mes-backend/mes-abnormal,mes-backend/mes-quality -Dtest=AbnormalContactServiceTest,AbnormalModuleTest,QualityEventListenerTest test
```

Expected: PASS.

## Task 2: Dispatch publishes lifecycle and failure events

**Files:**
- Modify: `mes-backend/mes-dispatch/src/main/java/com/mes/dispatch/enums/DispatchStatus.java`
- Modify: `mes-backend/mes-dispatch/src/main/java/com/mes/dispatch/service/impl/DispatchTaskServiceImpl.java`
- Modify: `mes-backend/mes-dispatch/src/main/java/com/mes/dispatch/listener/AbnormalEventListener.java`
- Modify: `mes-backend/mes-dispatch/src/test/java/com/mes/dispatch/service/DispatchServiceTest.java`
- Modify: `mes-backend/mes-dispatch/src/test/java/com/mes/dispatch/listener/AbnormalEventListenerTest.java`

- [x] **Step 1: Run dispatch tests and verify RED**

Run:

```bash
mvn -pl mes-backend/mes-dispatch -Dtest=DispatchServiceTest,AbnormalEventListenerTest test
```

Expected: failing assertions if `start()` does not publish `DispatchTaskStartedEvent`, `complete()` does not publish `DispatchTaskQualityFailedEvent` / `DispatchAllTasksCompletedEvent`, or abnormal submissions do not mark dispatch tasks `ABNORMAL`.

- [x] **Step 2: Implement the dispatch-side event flow**

`DispatchTaskServiceImpl.start()` must publish a start event after the status update:

```java
eventPublisher.publishEvent(new DispatchTaskStartedEvent(
        this,
        task.getId(),
        task.getWorkOrderId(),
        task.getWorkOrderTaskId(),
        workOrder != null ? workOrder.getWorkOrderNo() : null,
        task.getProcessNo(),
        task.getWorkName()
));
```

`DispatchTaskServiceImpl.complete()` must always publish `DispatchTaskCompletedEvent`, publish `DispatchTaskQualityFailedEvent` only when `qualityResult` is `FAIL`, and publish `DispatchAllTasksCompletedEvent` when the same work order has no unfinished dispatch tasks left:

```java
if ("FAIL".equalsIgnoreCase(task.getQualityResult())) {
    eventPublisher.publishEvent(new DispatchTaskQualityFailedEvent(/* ... */));
}

long unfinishedCount = count(new LambdaQueryWrapper<DispatchTask>()
        .eq(DispatchTask::getWorkOrderId, task.getWorkOrderId())
        .ne(DispatchTask::getDispatchStatus, DispatchStatus.COMPLETED.getCode()));
if (unfinishedCount == 0) {
    eventPublisher.publishEvent(new DispatchAllTasksCompletedEvent(
            this, task.getWorkOrderId(), workOrderNo));
}
```

`AbnormalEventListener` must move active dispatch tasks into `ABNORMAL` and log the transition:

```java
if (!COMPLETED.getCode().equals(currentStatus)
        && !CANCELLED.getCode().equals(currentStatus)
        && !ABNORMAL.getCode().equals(currentStatus)) {
    task.setDispatchStatus(ABNORMAL.getCode());
    dispatchTaskMapper.updateById(task);
    statusLogService.log(task.getId(), currentStatus, ABNORMAL.getCode(), "异常提报",
            "异常联络单 " + event.getContactNo() + " 已提交");
}
```

- [x] **Step 3: Re-run dispatch tests**

Run:

```bash
mvn -pl mes-backend/mes-dispatch -Dtest=DispatchServiceTest,AbnormalEventListenerTest test
```

Expected: PASS.

## Task 3: Cascade dispatch completion into workorder and plan completion

**Files:**
- Modify: `mes-backend/mes-workorder/src/main/java/com/mes/workorder/service/impl/WorkOrderServiceImpl.java`
- Create or verify: `mes-backend/mes-workorder/src/main/java/com/mes/workorder/listener/DispatchLifecycleEventListener.java`
- Modify: `mes-backend/mes-workorder/src/test/java/com/mes/workorder/service/WorkOrderServiceTest.java`
- Create or verify: `mes-backend/mes-workorder/src/test/java/com/mes/workorder/listener/DispatchLifecycleEventListenerTest.java`
- Modify: `mes-backend/mes-plan/src/main/java/com/mes/plan/enums/ProductionPlanStatus.java`
- Create or verify: `mes-backend/mes-plan/src/main/java/com/mes/plan/listener/ProductionPlanEventListener.java`
- Create or verify: `mes-backend/mes-plan/src/test/java/com/mes/plan/listener/ProductionPlanEventListenerTest.java`

- [x] **Step 1: Run workorder and plan cascade tests and verify RED**

Run:

```bash
mvn -pl mes-backend/mes-workorder,mes-backend/mes-plan -Dtest=WorkOrderServiceTest,DispatchLifecycleEventListenerTest,ProductionPlanEventListenerTest test
```

Expected: failing assertions if work-order completion does not emit `WorkOrderCompletedEvent`, dispatch lifecycle listeners do not auto-start/complete work orders, or production plans do not advance to `COMPLETED`.

- [x] **Step 2: Implement the cascade listeners and work-order completion event**

`WorkOrderServiceImpl.complete()` must publish a terminal work-order event after the status update:

```java
eventPublisher.publishEvent(new WorkOrderCompletedEvent(
        this,
        entity.getId(),
        entity.getWorkOrderNo(),
        entity.getProductionPlanNo(),
        entity.getOrderPlanNo(),
        entity.getPlanQty(),
        entity.getActualEndTime()
));
```

`DispatchLifecycleEventListener` must auto-start a released work order on the first dispatch start, and auto-complete an in-progress work order when all dispatch tasks are completed:

```java
if (WorkOrderStatus.RELEASED.getCode().equals(workOrder.getStatus())) {
    workOrderService.start(workOrder.getId());
}
if (workOrder != null && WorkOrderStatus.IN_PROGRESS.getCode().equals(workOrder.getStatus())) {
    workOrderService.complete(workOrder.getId());
}
```

`ProductionPlanEventListener` must use `WorkOrderCompletedEvent` to add to `completedQty`, cap it at `planQty`, flip the production plan to `COMPLETED` when full, and complete the parent order plan when no sibling production plan remains unfinished:

```java
BigDecimal targetCompletedQty = completedQty.add(increment);
if (planQty.compareTo(BigDecimal.ZERO) > 0 && targetCompletedQty.compareTo(planQty) > 0) {
    targetCompletedQty = planQty;
}
if (completed) {
    plan.setStatus(ProductionPlanStatus.COMPLETED.getCode());
}
```

- [x] **Step 3: Re-run workorder and plan cascade tests**

Run:

```bash
mvn -pl mes-backend/mes-workorder,mes-backend/mes-plan -Dtest=WorkOrderServiceTest,DispatchLifecycleEventListenerTest,ProductionPlanEventListenerTest test
```

Expected: PASS.

## Task 4: Quality reacts to dispatch FAIL and abnormal submission

**Files:**
- Modify: `mes-backend/mes-quality/src/main/java/com/mes/quality/listener/QualityEventListener.java`
- Modify: `mes-backend/mes-quality/src/test/java/com/mes/quality/listener/QualityEventListenerTest.java`
- Modify: `mes-backend/mes-abnormal/src/main/java/com/mes/abnormal/service/impl/AbnormalContactServiceImpl.java`

- [x] **Step 1: Run the quality and abnormal event tests and verify RED**

Run:

```bash
mvn -pl mes-backend/mes-quality,mes-backend/mes-abnormal -Dtest=QualityEventListenerTest,AbnormalContactServiceTest,AbnormalModuleTest test
```

Expected: failing assertions if dispatch start does not create a work-start check, dispatch FAIL does not create a recheck request, or abnormal submission does not publish `AbnormalSubmittedEvent`.

- [x] **Step 2: Implement the quality listeners and abnormal submission event**

`QualityEventListener` must create a `WorkStartCheckDTO` from `DispatchTaskStartedEvent`, a `RecheckRequestDTO` from `DispatchTaskQualityFailedEvent`, and a second `RecheckRequestDTO` from `AbnormalSubmittedEvent` when at least one link field is present:

```java
if (event.getWorkOrderId() == null && event.getDispatchTaskId() == null
        && !StringUtils.hasText(event.getOrderNo())) {
    return;
}

dto.setWorkOrderId(event.getWorkOrderId());
dto.setDispatchTaskId(event.getDispatchTaskId());
dto.setProductionOrderNo(event.getOrderNo());
dto.setRecheckRequirement("异常联络单提交后触发质量复检");
```

`AbnormalContactServiceImpl.submit()` must publish `AbnormalSubmittedEvent` after persisting the status change and status log, before optional APS reschedule debounce logic:

```java
eventPublisher.publishEvent(new AbnormalSubmittedEvent(
        this,
        entity.getId(),
        entity.getContactNo(),
        entity.getWorkOrderId(),
        entity.getDispatchTaskId(),
        entity.getOrderNo(),
        entity.getEventCategory(),
        entity.getAbnormalDesc()
));
```

- [x] **Step 3: Re-run the quality and abnormal event tests**

Run:

```bash
mvn -pl mes-backend/mes-quality,mes-backend/mes-abnormal -Dtest=QualityEventListenerTest,AbnormalContactServiceTest,AbnormalModuleTest test
```

Expected: PASS.

## Task 5: Final verification across all affected modules

**Files:**
- Verify only: `mes-backend/mes-common`
- Verify only: `mes-backend/mes-dispatch`
- Verify only: `mes-backend/mes-workorder`
- Verify only: `mes-backend/mes-plan`
- Verify only: `mes-backend/mes-quality`
- Verify only: `mes-backend/mes-abnormal`
- Verify only: `sql/V2.13__phase2_dispatch_quality_abnormal_cascade.sql`

- [x] **Step 1: Run the full affected test suite**

Run:

```bash
mvn -pl mes-backend/mes-dispatch,mes-backend/mes-workorder,mes-backend/mes-plan,mes-backend/mes-quality,mes-backend/mes-abnormal \
  -Dtest=DispatchServiceTest,AbnormalEventListenerTest,WorkOrderServiceTest,DispatchLifecycleEventListenerTest,ProductionPlanEventListenerTest,QualityEventListenerTest,AbnormalContactServiceTest,AbnormalModuleTest \
  test
```

Expected: PASS.

- [x] **Step 2: Run affected-module compilation**

Run:

```bash
mvn -pl mes-backend/mes-dispatch,mes-backend/mes-workorder,mes-backend/mes-plan,mes-backend/mes-quality,mes-backend/mes-abnormal -am test-compile
```

Expected: BUILD SUCCESS.

- [x] **Step 3: Inspect the final diff**

Run:

```bash
git diff -- mes-backend/mes-common mes-backend/mes-dispatch mes-backend/mes-workorder mes-backend/mes-plan mes-backend/mes-quality mes-backend/mes-abnormal sql/V2.13__phase2_dispatch_quality_abnormal_cascade.sql docs/superpowers/plans/2026-05-27-mes-phase2-dispatch-quality-abnormal-cascade.md
```

Expected: only Phase 2 event, listener, status, DTO/entity/VO, test, migration, and plan-file changes are present.

## 2026-05-28 Follow-up: 5.1 Abnormal Work-Order Link Completion

- [x] 补齐了 `5.1` 的一个真实残口：
  - 旧实现仅在 `AbnormalSubmittedEvent.dispatchTaskId` 存在时标记单个派工任务异常；
  - 当异常联络单只关联 `workOrderId`、未指定 `dispatchTaskId` 时，该工单下活动派工不会被联动标记，和计划文档中“异常提交时自动标记关联派工任务状态”的要求仍有差距。
- [x] `AbnormalEventListener` 现已支持工单级回退联动：
  - 若事件带 `dispatchTaskId`，保持原有精确更新路径；
  - 若仅带 `workOrderId`，会查询该工单下派工任务，并将 `ASSIGNED / IN_PROGRESS` 的活动派工统一标记为 `ABNORMAL`；
  - `COMPLETED / CANCELLED / ABNORMAL` 仍保持跳过，避免污染终态记录。
- [x] 当前态验证已经 fresh 通过：
  - `AbnormalEventListenerTest` 新增 `workOrderId` 回退场景，证明活动派工会被批量标记，已完成派工不会被误改；
  - 同时复验 `AbnormalContactServiceTest` / `AbnormalModuleTest` / `QualityEventListenerTest`，确认异常联络单到派工、质量的既有联动链路未回归。

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-dispatch -am -Dtest=AbnormalEventListenerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-dispatch,mes-quality,mes-abnormal -am -Dtest=AbnormalEventListenerTest,QualityEventListenerTest,AbnormalContactServiceTest,AbnormalModuleTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 2026-05-28 Follow-up: Fresh Proof for 1.2 Cascade Edge Cases

- [x] `1.2` 派工完工级联到工单完工的边界语义已在当前代码上补充 fresh proof：
  - `DispatchLifecycleEventListenerTest` 新增 `RELEASED` 工单场景，证明 `DispatchAllTasksCompletedEvent` 到达时，即使工单尚未被显式开工，也会先自动 `start()`，再自动 `complete()`，不会因为状态仍停留在 `RELEASED` 而丢失完工级联。
- [x] `1.2` 订单计划自动完工的“不过早完成”语义已在当前代码上补充 fresh proof：
  - `ProductionPlanEventListenerTest` 新增同一 `orderPlanId` 仍有其他未完工生产计划的场景，证明监听器只会更新当前生产计划，不会提前调用 `orderPlanService.complete()`。
- [x] 这次 follow-up 没有引入新的生产代码改动：
  - 当前实现已经满足该两条链路语义；
  - 本次仅通过新增聚焦测试把它们锁成回归保护，便于后续继续清理剩余计划项时不再重复人工核对。

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-workorder -am -Dtest=DispatchLifecycleEventListenerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-plan -am -Dtest=ProductionPlanEventListenerTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 2026-05-28 Follow-up: 5.1 Frontend Link-Field Closure

- [x] `5.1` 还存在一个真实使用层残口：
  - 后端 `AbnormalContactDTO/VO` 与事件链已经支持 `workOrderId` / `dispatchTaskId`；
  - 但异常联络单前端类型和录入页面此前仍只暴露自由文本 `orderNo`，实际用户无法在 UI 中录入这两个关联字段，导致“异常提交 -> 派工/质量联动”在真实使用上仍是半闭环。
- [x] 前端异常联络页面现已补齐关联字段入口：
  - `mes-frontend/src/types/abnormal.ts` 增加 `workOrderId` / `dispatchTaskId`；
  - `mes-frontend/src/views/abnormal/AbnormalContactList.vue` 的新增/编辑表单增加 `关联工单ID`、`关联派工任务ID` 输入项；
  - 详情抽屉同步展示这两个字段，便于人工核对联动对象。
- [x] 当前态验证已经 fresh 通过：
  - Playwright 新增页面级回归，证明异常联络单新增对话框确实展示关联字段；
  - 新增请求链路回归，证明填写后的 `workOrderId` / `dispatchTaskId` 会进入 `POST /api/abnormal/contact` 请求体；
  - `vue-tsc + vite build` 继续通过，未引入新的前端编译失败。

Verification run for this follow-up:

- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run test:e2e -- tests/e2e/abnormal.spec.ts --project=chromium -g "新增异常对话框展示关联工单和派工字段"`
- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run test:e2e -- tests/e2e/abnormal.spec.ts --project=chromium -g "新增异常对话框会把关联工单和派工字段提交到请求体"`
- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run build`

## 2026-05-28 Follow-up: 5.1 Abnormal Event Semantics Correction

- [x] 继续收口了 `5.1` 的一个真实语义残口：
  - `AbnormalSubmittedEvent` 先前只携带 `orderNo`；
  - 但质量侧 `QualityEventListener` 会把它直接映射到 `RecheckRequest.productionOrderNo`，而现有码路与测试实际上把这个值当成工单/生产单号来消费，存在“订单号字段名、工单号语义”的错位。
- [x] 当前代码已改为显式区分两种编号：
  - `AbnormalSubmittedEvent` 新增 `workOrderNo`；
  - `AbnormalContactServiceImpl.submit()` 在存在 `workOrderId` 时会查询并回填真实 `workOrderNo`；
  - `QualityEventListener` 在异常联络触发复检时，优先使用 `event.workOrderNo` 作为 `productionOrderNo`，仅在旧数据缺少该字段时回退到 `orderNo`，保持兼容。
- [x] 这次修补保持了最小变更和兼容边界：
  - `orderNo` 仍保留在事件与实体中，避免打断既有调用方；
  - 新逻辑只是在异常->质量链路中优先使用语义正确的工单号，不改动派工侧异常状态联动行为。
- [x] 当前态验证已经 fresh 通过：
  - `AbnormalContactServiceTest` 现在证明异常提报事件会同时携带 `orderNo` 与 `workOrderNo`；
  - `QualityEventListenerTest` 证明质量监听器优先使用 `workOrderNo`，并对缺失 `workOrderNo` 的旧事件兼容回退到 `orderNo`；
  - `AbnormalEventListenerTest` 复验通过，证明事件合同调整未破坏异常到派工状态联动。

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-abnormal -am -Dtest=AbnormalContactServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-quality -am -Dtest=QualityEventListenerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-dispatch -am -Dtest=AbnormalEventListenerTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 2026-05-28 Follow-up: 5.1 orderNo Consistency Closure

- [x] `5.1` 中“`AbnormalContact.orderNo` 是自由文本”在当前代码上继续收口：
  - 之前异常联络单虽然已经新增 `workOrderId` / `dispatchTaskId`，但 create/update 仍允许 `orderNo` 保持任意文本，与关联工单的真实订单号分叉；
  - 这会导致异常数据本身和后续事件载荷在“关联工单存在时”仍可能不一致。
- [x] `AbnormalContactServiceImpl` 现已在 create/update 时规范化工单引用：
  - 当 `workOrderId` 存在时，先校验关联工单必须存在；
  - 若工单上存在标准 `orderNo`，则：
    - 用户未传 `orderNo` 时自动回填；
    - 用户传了不同的 `orderNo` 时直接拒绝，避免异常联络单与工单主数据分叉。
- [x] 这次修补仍保持最小边界：
  - 没有强推数据库外键或迁移；
  - 规则仅落在异常联络单服务层，优先把“有工单关联时的文本分叉”收住，不扩大到无工单关联的自由录入场景。
- [x] 当前态验证已经 fresh 通过：
  - `AbnormalContactServiceTest` 新增覆盖：
    - `workOrderId` 存在时自动回填标准订单号；
    - 手输 `orderNo` 与工单不一致时拒绝；
    - update 场景同样会规范化 `orderNo`。

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-abnormal -am -Dtest=AbnormalContactServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
