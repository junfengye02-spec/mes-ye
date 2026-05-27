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
