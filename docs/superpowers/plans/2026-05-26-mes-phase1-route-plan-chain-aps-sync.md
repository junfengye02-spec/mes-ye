# MES Phase 1 Route, Plan Chain, and APS Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add MES process routes, generate work-order task lists from routes when production plans are released, and stop unsupported APS upstream sync types from retrying against missing APS endpoints.

**Architecture:** `mes-process` owns the route model and route lookup service. `mes-plan` depends on `mes-process` and maps active route steps into `WorkOrderTaskDTO` during `ProductionPlanServiceImpl.release()`. `mes-aps` keeps queue processing local but marks unsupported upstream sync types as terminal `FAILED` without HTTP calls or retry increments.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus 3.5, JUnit 5, Mockito, Maven, SQL migrations.

---

## File Structure

Create in `mes-process`:

- `mes-backend/mes-process/src/main/java/com/mes/process/domain/entity/Route.java`: route header entity mapped to `mes_route`.
- `mes-backend/mes-process/src/main/java/com/mes/process/domain/entity/RouteStep.java`: ordered route step entity mapped to `mes_route_step`.
- `mes-backend/mes-process/src/main/java/com/mes/process/domain/dto/RouteDTO.java`: create/update payload with nested steps.
- `mes-backend/mes-process/src/main/java/com/mes/process/domain/dto/RouteStepDTO.java`: route step payload.
- `mes-backend/mes-process/src/main/java/com/mes/process/domain/query/RouteQuery.java`: route page filters.
- `mes-backend/mes-process/src/main/java/com/mes/process/domain/vo/RouteVO.java`: route response with nested steps.
- `mes-backend/mes-process/src/main/java/com/mes/process/domain/vo/RouteStepVO.java`: route step response.
- `mes-backend/mes-process/src/main/java/com/mes/process/enums/RouteStatus.java`: `DRAFT`, `ACTIVE`, `DISABLED`.
- `mes-backend/mes-process/src/main/java/com/mes/process/mapper/RouteMapper.java`: MyBatis-Plus mapper.
- `mes-backend/mes-process/src/main/java/com/mes/process/mapper/RouteStepMapper.java`: MyBatis-Plus mapper.
- `mes-backend/mes-process/src/main/java/com/mes/process/service/IRouteService.java`: route CRUD plus active route lookup.
- `mes-backend/mes-process/src/main/java/com/mes/process/service/impl/RouteServiceImpl.java`: route CRUD, step persistence, and active route matching.
- `mes-backend/mes-process/src/main/java/com/mes/process/controller/RouteController.java`: route API matching existing process controllers.
- `mes-backend/mes-process/src/test/java/com/mes/process/service/RouteServiceTest.java`: route lookup tests.
- `sql/V2.12__process_route.sql`: additive route tables and indexes.

Modify in `mes-plan`:

- `mes-backend/mes-plan/pom.xml`: add dependency on `mes-process`.
- `mes-backend/mes-plan/src/main/java/com/mes/plan/service/impl/ProductionPlanServiceImpl.java`: inject route service and set work-order tasks before creating the work order.
- `mes-backend/mes-plan/src/test/java/com/mes/plan/service/ProductionPlanServiceTest.java`: release behavior tests.

Modify in `mes-aps`:

- `mes-backend/mes-aps/src/main/java/com/mes/aps/service/impl/ApsUpstreamSyncServiceImpl.java`: add supported sync type guard.
- `mes-backend/mes-aps/src/test/java/com/mes/aps/service/ApsUpstreamSyncServiceTest.java`: unsupported type queue test.

## Task 1: Add Route Model and Active Lookup

**Files:**
- Create: `mes-backend/mes-process/src/test/java/com/mes/process/service/RouteServiceTest.java`
- Create: `mes-backend/mes-process/src/main/java/com/mes/process/domain/entity/Route.java`
- Create: `mes-backend/mes-process/src/main/java/com/mes/process/domain/entity/RouteStep.java`
- Create: `mes-backend/mes-process/src/main/java/com/mes/process/domain/dto/RouteDTO.java`
- Create: `mes-backend/mes-process/src/main/java/com/mes/process/domain/dto/RouteStepDTO.java`
- Create: `mes-backend/mes-process/src/main/java/com/mes/process/domain/query/RouteQuery.java`
- Create: `mes-backend/mes-process/src/main/java/com/mes/process/domain/vo/RouteVO.java`
- Create: `mes-backend/mes-process/src/main/java/com/mes/process/domain/vo/RouteStepVO.java`
- Create: `mes-backend/mes-process/src/main/java/com/mes/process/enums/RouteStatus.java`
- Create: `mes-backend/mes-process/src/main/java/com/mes/process/mapper/RouteMapper.java`
- Create: `mes-backend/mes-process/src/main/java/com/mes/process/mapper/RouteStepMapper.java`
- Create: `mes-backend/mes-process/src/main/java/com/mes/process/service/IRouteService.java`
- Create: `mes-backend/mes-process/src/main/java/com/mes/process/service/impl/RouteServiceImpl.java`
- Create: `mes-backend/mes-process/src/main/java/com/mes/process/controller/RouteController.java`
- Create: `sql/V2.12__process_route.sql`

- [ ] **Step 1: Write failing route lookup tests**

Create `RouteServiceTest` with Mockito tests for exact product match, fallback, and no steps:

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RouteServiceTest {
    @Mock private RouteMapper routeMapper;
    @Mock private RouteStepMapper routeStepMapper;
    @InjectMocks private RouteServiceImpl routeService;

    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(routeService, "baseMapper", routeMapper);
    }

    @Test
    @DisplayName("匹配活动路线 - 优先产品编码精确匹配")
    void findActiveRouteWithSteps_prefersExactProductCode() {
        Route exact = route(1L, "R-P1", "P1", "CAT-A", "M1");
        when(routeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(exact));
        when(routeStepMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                step(11L, 1L, 20, "OP-020"),
                step(10L, 1L, 10, "OP-010")));

        RouteVO result = routeService.findActiveRouteWithSteps("P1", "CAT-A", "M1", "TYPE-A");

        assertEquals(1L, result.getId());
        assertEquals("R-P1", result.getRouteCode());
        assertEquals(List.of("OP-010", "OP-020"),
                result.getSteps().stream().map(RouteStepVO::getProcessNo).toList());
    }

    @Test
    @DisplayName("匹配活动路线 - 无产品编码时按产品类别和机型回退")
    void findActiveRouteWithSteps_fallsBackToCategoryAndMachineModel() {
        when(routeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(route(2L, "R-CAT-M", null, "CAT-A", "M1")));
        when(routeStepMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(step(20L, 2L, 10, "OP-010")));

        RouteVO result = routeService.findActiveRouteWithSteps("P-MISSING", "CAT-A", "M1", "TYPE-A");

        assertEquals(2L, result.getId());
        assertEquals("R-CAT-M", result.getRouteCode());
    }

    @Test
    @DisplayName("匹配活动路线 - 活动路线无步骤时拒绝")
    void findActiveRouteWithSteps_rejectsRouteWithoutSteps() {
        when(routeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(route(3L, "R-EMPTY", "P1", "CAT-A", "M1")));
        when(routeStepMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> routeService.findActiveRouteWithSteps("P1", "CAT-A", "M1", "TYPE-A"));
        assertTrue(ex.getMessage().contains("未配置工序步骤"));
    }

    private static Route route(Long id, String code, String productCode,
                               String productCategory, String machineModel) {
        Route route = new Route();
        route.setId(id);
        route.setRouteCode(code);
        route.setRouteName(code);
        route.setProductCode(productCode);
        route.setProductCategory(productCategory);
        route.setMachineModel(machineModel);
        route.setStatus(RouteStatus.ACTIVE.getCode());
        return route;
    }

    private static RouteStep step(Long id, Long routeId, Integer sequenceNo, String processNo) {
        RouteStep step = new RouteStep();
        step.setId(id);
        step.setRouteId(routeId);
        step.setSequenceNo(sequenceNo);
        step.setProcessId(id + 1000);
        step.setProcessNo(processNo);
        step.setProcessName("工序" + sequenceNo);
        step.setWorkCenterId(300L + sequenceNo);
        return step;
    }
}
```

- [ ] **Step 2: Run route tests and verify RED**

Run: `mvn -pl mes-process -Dtest=RouteServiceTest test`

Expected: compilation fails because `RouteServiceImpl`, `Route`, `RouteStep`, `RouteVO`, and `RouteStepVO` do not exist.

- [ ] **Step 3: Implement route entities, DTOs, VOs, mapper, service, controller, and migration**

Key service contract:

```java
public interface IRouteService extends IService<Route> {
    PageResult<RouteVO> page(RouteQuery query);
    RouteVO getDetail(Long id);
    Long create(RouteDTO dto);
    void update(Long id, RouteDTO dto);
    void delete(Long id);
    void activate(Long id);
    void disable(Long id);
    RouteVO findActiveRouteWithSteps(String productCode, String productCategory,
                                     String machineModel, String productType);
}
```

Key lookup implementation:

```java
@Override
public RouteVO findActiveRouteWithSteps(String productCode, String productCategory,
                                        String machineModel, String productType) {
    Route route = findExactProductRoute(productCode)
            .or(() -> findCategoryAndMachineRoute(productCategory, machineModel))
            .or(() -> findCategoryRoute(productCategory))
            .orElseThrow(() -> new BusinessException("未找到匹配的有效工艺路线"));

    List<RouteStep> steps = routeStepMapper.selectList(new LambdaQueryWrapper<RouteStep>()
            .eq(RouteStep::getRouteId, route.getId())
            .orderByAsc(RouteStep::getSequenceNo));
    AssertUtil.isFalse(steps.isEmpty(), "工艺路线未配置工序步骤");

    RouteVO vo = toVO(route);
    vo.setSteps(steps.stream().map(this::toStepVO).toList());
    return vo;
}
```

Migration must create `mes_route` and `mes_route_step` with `tenant_id`, `created_by`, `created_time`, `updated_by`, `updated_time`, and `deleted` columns, plus indexes on route code, product fields, status, and `(route_id, sequence_no)`.

- [ ] **Step 4: Run route tests and verify GREEN**

Run: `mvn -pl mes-process -Dtest=RouteServiceTest test`

Expected: PASS.

- [ ] **Step 5: Commit route model**

```bash
git add mes-backend/mes-process/src/main/java/com/mes/process sql/V2.12__process_route.sql mes-backend/mes-process/src/test/java/com/mes/process/service/RouteServiceTest.java
git commit -m "feat: add MES process routes"
```

## Task 2: Generate Work-Order Tasks During Production Plan Release

**Files:**
- Create: `mes-backend/mes-plan/src/test/java/com/mes/plan/service/ProductionPlanServiceTest.java`
- Modify: `mes-backend/mes-plan/pom.xml`
- Modify: `mes-backend/mes-plan/src/main/java/com/mes/plan/service/impl/ProductionPlanServiceImpl.java`

- [ ] **Step 1: Write failing production-plan release tests**

Create `ProductionPlanServiceTest` with tests that capture the `WorkOrderDTO` passed to `workOrderService.create()`:

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductionPlanServiceTest {
    @Mock private ProductionPlanMapper productionPlanMapper;
    @Mock private IOrderPlanService orderPlanService;
    @Mock private IPlanStatusLogService planStatusLogService;
    @Mock private IWorkOrderService workOrderService;
    @Mock private IRouteService routeService;
    @InjectMocks private ProductionPlanServiceImpl productionPlanService;

    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(productionPlanService, "baseMapper", productionPlanMapper);
    }

    @Test
    @DisplayName("下达生产计划 - 根据工艺路线生成工单工作清单")
    void release_generatesWorkOrderTasksFromRoute() {
        ProductionPlan plan = productionPlan(100L, "P1", "CAT-A", "M1");
        when(productionPlanMapper.selectById(100L)).thenReturn(plan);
        when(orderPlanService.getById(10L)).thenReturn(orderPlan("ORD-1"));
        when(routeService.findActiveRouteWithSteps("P1", "CAT-A", "M1", "TYPE-A"))
                .thenReturn(routeWithSteps());
        when(productionPlanMapper.updateById(any(ProductionPlan.class))).thenReturn(1);
        when(workOrderService.create(any(WorkOrderDTO.class))).thenReturn(200L);

        productionPlanService.release(100L);

        ArgumentCaptor<WorkOrderDTO> captor = ArgumentCaptor.forClass(WorkOrderDTO.class);
        verify(workOrderService).create(captor.capture());
        WorkOrderDTO dto = captor.getValue();
        assertEquals(2, dto.getTasks().size());
        assertEquals("OP-010", dto.getTasks().get(0).getTaskNo());
        assertEquals("首道工序", dto.getTasks().get(0).getTaskName());
        assertEquals(new BigDecimal("5"), dto.getTasks().get(0).getPlanQty());
        assertEquals("PCS", dto.getTasks().get(0).getQtyUnit());
        assertEquals(10, dto.getTasks().get(0).getSequenceNo());
        assertEquals(300L, dto.getTasks().get(0).getPlanWorkCenterId());
    }

    @Test
    @DisplayName("下达生产计划 - 无匹配路线时不更新计划状态也不创建工单")
    void release_rejectsWhenNoActiveRoute() {
        ProductionPlan plan = productionPlan(100L, "P1", "CAT-A", "M1");
        when(productionPlanMapper.selectById(100L)).thenReturn(plan);
        when(routeService.findActiveRouteWithSteps("P1", "CAT-A", "M1", "TYPE-A"))
                .thenThrow(new BusinessException("未找到匹配的有效工艺路线"));

        assertThrows(BusinessException.class, () -> productionPlanService.release(100L));

        verify(productionPlanMapper, never()).updateById(any());
        verify(workOrderService, never()).create(any());
    }

    private static ProductionPlan productionPlan(Long id, String productCode,
                                                 String productCategory, String machineModel) {
        ProductionPlan plan = new ProductionPlan();
        plan.setId(id);
        plan.setOrderPlanId(10L);
        plan.setOrderNo("ORD-1");
        plan.setProductCode(productCode);
        plan.setProductName("产品1");
        plan.setProductCategory(productCategory);
        plan.setMachineModel(machineModel);
        plan.setProductType("TYPE-A");
        plan.setStatus(ProductionPlanStatus.CREATED.getCode());
        plan.setPlanQty(new BigDecimal("5"));
        plan.setQtyUnit("PCS");
        return plan;
    }

    private static OrderPlan orderPlan(String orderNo) {
        OrderPlan orderPlan = new OrderPlan();
        orderPlan.setId(10L);
        orderPlan.setOrderNo(orderNo);
        return orderPlan;
    }

    private static RouteVO routeWithSteps() {
        RouteVO route = new RouteVO();
        route.setId(1L);
        route.setRouteCode("R-P1");
        route.setSteps(List.of(
                routeStep(10, "OP-010", "首道工序", 300L),
                routeStep(20, "OP-020", "二道工序", 301L)));
        return route;
    }

    private static RouteStepVO routeStep(Integer sequenceNo, String processNo,
                                         String processName, Long workCenterId) {
        RouteStepVO step = new RouteStepVO();
        step.setSequenceNo(sequenceNo);
        step.setProcessNo(processNo);
        step.setProcessName(processName);
        step.setWorkCenterId(workCenterId);
        return step;
    }
}
```

- [ ] **Step 2: Run production-plan tests and verify RED**

Run: `mvn -pl mes-process,mes-plan -Dtest=ProductionPlanServiceTest test`

Expected: compilation fails because `mes-plan` does not depend on `mes-process` and `ProductionPlanServiceImpl` does not inject `IRouteService`.

- [ ] **Step 3: Add `mes-process` dependency to `mes-plan`**

Add to `mes-backend/mes-plan/pom.xml`:

```xml
<dependency>
    <groupId>com.mes</groupId>
    <artifactId>mes-process</artifactId>
</dependency>
```

- [ ] **Step 4: Modify production-plan release flow**

In `ProductionPlanServiceImpl`, inject `IRouteService`, lookup route before status update, and set generated tasks on `workOrderDTO`:

```java
private final IRouteService routeService;

private List<WorkOrderTaskDTO> buildWorkOrderTasks(ProductionPlan plan) {
    RouteVO route = routeService.findActiveRouteWithSteps(
            plan.getProductCode(), plan.getProductCategory(),
            plan.getMachineModel(), plan.getProductType());
    return route.getSteps().stream()
            .map(step -> {
                WorkOrderTaskDTO dto = new WorkOrderTaskDTO();
                dto.setTaskNo(step.getProcessNo());
                dto.setTaskName(step.getProcessName());
                dto.setPlanWorkCenterId(step.getWorkCenterId());
                dto.setPlanQty(plan.getPlanQty());
                dto.setQtyUnit(plan.getQtyUnit());
                dto.setSequenceNo(step.getSequenceNo());
                return dto;
            })
            .toList();
}
```

Call `List<WorkOrderTaskDTO> tasks = buildWorkOrderTasks(entity);` before setting the production plan status to `RELEASED`, then call `workOrderDTO.setTasks(tasks);` before `workOrderService.create(workOrderDTO)`.

- [ ] **Step 5: Run production-plan tests and verify GREEN**

Run: `mvn -pl mes-process,mes-plan -Dtest=ProductionPlanServiceTest test`

Expected: PASS.

- [ ] **Step 6: Run work-order release regression**

Run: `mvn -pl mes-workorder -Dtest=WorkOrderServiceTest test`

Expected: PASS. The existing test that rejects work orders without tasks remains valid.

- [ ] **Step 7: Commit production-plan route task generation**

```bash
git add mes-backend/mes-plan/pom.xml mes-backend/mes-plan/src/main/java/com/mes/plan/service/impl/ProductionPlanServiceImpl.java mes-backend/mes-plan/src/test/java/com/mes/plan/service/ProductionPlanServiceTest.java
git commit -m "feat: generate work order tasks from routes"
```

## Task 3: Guard APS Upstream Queue Against Unsupported Types

**Files:**
- Create: `mes-backend/mes-aps/src/test/java/com/mes/aps/service/ApsUpstreamSyncServiceTest.java`
- Modify: `mes-backend/mes-aps/src/main/java/com/mes/aps/service/impl/ApsUpstreamSyncServiceImpl.java`

- [ ] **Step 1: Write failing APS unsupported-type test**

Create `ApsUpstreamSyncServiceTest`:

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApsUpstreamSyncServiceTest {
    @Mock private ApsSyncQueueMapper syncQueueMapper;
    @Mock private ApsClient apsClient;
    @Mock private IApsSyncConfigService configService;
    @Mock private IApsSyncLogService syncLogService;
    @Mock private ObjectMapper objectMapper;
    @Mock private OrderPlanMapper orderPlanMapper;
    @Mock private WorkOrderMapper workOrderMapper;

    private ApsUpstreamSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApsUpstreamSyncServiceImpl(syncQueueMapper, apsClient, configService,
                syncLogService, objectMapper, orderPlanMapper, workOrderMapper);
    }

    @Test
    @DisplayName("处理队列 - APS不支持的同步类型直接终态失败且不调用HTTP")
    void processQueue_marksUnsupportedTypeFailedWithoutRetryOrHttp() {
        ApsSyncQueue item = new ApsSyncQueue();
        item.setId(1L);
        item.setSyncType("DISPATCH");
        item.setDataType("DISPATCH");
        item.setDataId(10L);
        item.setDataNo("DT-1");
        item.setRetryCount(0);
        item.setMaxRetry(3);
        item.setSyncStatus(SyncStatus.PENDING.getCode());

        when(syncLogService.createLog(anyString(), eq(SyncDirection.UPSTREAM.getCode()), eq("QUEUE")))
                .thenReturn(syncLog(99L));
        when(configService.getBooleanConfig("aps.sync.upstream.enabled", true)).thenReturn(true);
        when(configService.getIntConfig("aps.sync.batch.size", 200)).thenReturn(200);
        when(syncQueueMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

        ApsSyncResultVO result = service.processQueue();

        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        verify(apsClient, never()).post(anyString(), any(), any());
        verify(apsClient, never()).postAsync(anyString(), any());
        verify(syncQueueMapper).updateById(argThat(q ->
                SyncStatus.FAILED.getCode().equals(q.getSyncStatus())
                        && q.getRetryCount() == 0
                        && q.getErrorMessage().contains("APS不支持")));
    }

    private static ApsSyncLog syncLog(Long id) {
        ApsSyncLog log = new ApsSyncLog();
        log.setId(id);
        log.setBatchId("batch-1");
        log.setSyncDirection(SyncDirection.UPSTREAM.getCode());
        log.setSyncType("QUEUE");
        return log;
    }
}
```

- [ ] **Step 2: Run APS test and verify RED**

Run: `mvn -pl mes-aps -Dtest=ApsUpstreamSyncServiceTest test`

Expected: test fails because unsupported `DISPATCH` currently reaches `ApsClient` or retry failure handling.

- [ ] **Step 3: Implement supported upstream type guard**

Add to `ApsUpstreamSyncServiceImpl`:

```java
private static final Set<String> SUPPORTED_UPSTREAM_SYNC_TYPES = Set.of(
        "WORKORDER", "INVENTORY", "QUALITY", "OUTSOURCE", "TRANSFER", "ABNORMAL");

private boolean isUnsupportedUpstreamType(ApsSyncQueue item) {
    return !SUPPORTED_UPSTREAM_SYNC_TYPES.contains(item.getSyncType());
}

private void markUnsupportedTypeFailed(ApsSyncQueue item) {
    item.setSyncStatus(SyncStatus.FAILED.getCode());
    item.setErrorMessage("APS不支持当前同步类型: " + item.getSyncType());
    item.setUpdatedTime(LocalDateTime.now());
    syncQueueMapper.updateById(item);
}
```

In `processQueue()`, before marking an item as `PROCESSING`, add:

```java
if (isUnsupportedUpstreamType(item)) {
    failCount++;
    markUnsupportedTypeFailed(item);
    log.warn("跳过APS不支持的同步类型: id={}, type={}, dataNo={}",
            item.getId(), item.getSyncType(), item.getDataNo());
    continue;
}
```

- [ ] **Step 4: Run APS test and verify GREEN**

Run: `mvn -pl mes-aps -Dtest=ApsUpstreamSyncServiceTest test`

Expected: PASS.

- [ ] **Step 5: Commit APS sync guard**

```bash
git add mes-backend/mes-aps/src/main/java/com/mes/aps/service/impl/ApsUpstreamSyncServiceImpl.java mes-backend/mes-aps/src/test/java/com/mes/aps/service/ApsUpstreamSyncServiceTest.java
git commit -m "fix: stop unsupported APS sync retries"
```

## Task 4: Affected Module Verification

**Files:**
- Modify only files already touched by Tasks 1-3 if verification reveals compile or test issues.

- [ ] **Step 1: Run affected module tests**

Run:

```bash
mvn -pl mes-process,mes-plan,mes-workorder,mes-aps test
```

Expected: PASS.

- [ ] **Step 2: Run full backend compile if affected tests pass**

Run:

```bash
mvn -DskipTests compile
```

Expected: PASS.

- [ ] **Step 3: Inspect final diff**

Run:

```bash
git status --short
git diff --stat HEAD
```

Expected: only Phase 1 route, production-plan, APS guard, tests, and migration changes are present.

- [ ] **Step 4: Commit any verification-only fixes**

If Step 1 or Step 2 required small compile fixes, run `git status --short`, stage only files already listed in Tasks 1-3, and commit them with:

```bash
git commit -m "test: stabilize MES phase 1 verification"
```

If no fixes were needed, do not create an empty commit.

## Self-Review Checklist

- Spec coverage: Task 1 covers route model and active lookup. Task 2 covers plan release task generation. Task 3 covers APS sync guard. Task 4 covers verification.
- Placeholder scan: no placeholder tasks are allowed; each code step names exact files and concrete behavior.
- Type consistency: route service returns `RouteVO` with `List<RouteStepVO> steps`; production plan maps `RouteStepVO` to `WorkOrderTaskDTO`; APS guard uses existing `SyncStatus.FAILED`.

## 2026-05-28 Follow-up: 3.1 BOM Route-Step Reference Completion

- [x] `3.1` 的剩余兼容缺口已在当前代码上补齐：
  - `ManufacturingBomItem` / `ManufacturingBomItemDTO` / `ManufacturingBomItemVO` 现已显式增加 `routeStepId`。
  - `ManufacturingBomServiceImpl` 在保存时以 `routeStepId` 为主，若缺省则从兼容字段 `processId` 回填；持久化与 VO 回显都会把 `processId` 镜像为最终 `routeStepId`，避免新旧字段分叉。
  - 新增迁移 `sql/V2.22__phase1_bom_route_step_reference.sql`，为 `mes_manufacturing_bom_item` 增加 `route_step_id` 并从历史 `process_id` 回填。
- [x] 当前态验证已经 fresh 通过：
  - `ManufacturingBomRouteStepCompatibilityTest` 证明：
    - 仅提供旧字段 `processId` 时，实体会回填 `routeStepId`
    - 同时提供两个字段时，以 `routeStepId` 为主并同步回显 `processId`
    - 读取 BOM 明细树时，VO 会同时返回 `routeStepId` 与兼容字段 `processId`
  - 前端类型契约继续保留 `ManufacturingBomItemVO.routeStepId` 与 `ManufacturingBomItemVO.processId`，构建通过。

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-process -am -Dtest=ManufacturingBomRouteStepCompatibilityTest,RouteServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run build`

## 2026-05-28 Follow-up: Fresh Proof for 1.1 Plan -> WorkOrder -> Dispatch Chain

- [x] `1.1` 在当前代码上的真实闭环语义已补充 fresh evidence：
  - `ProductionPlanServiceTest` 证明 `ProductionPlanServiceImpl.release()` 会在下达生产计划时，先按 Route 有序展开 `WorkOrderTaskDTO`，再把非空 `tasks` 一起传入自动创建的工单。
  - `WorkOrderServiceTest` 证明工单下发仍然坚持“必须已有工作清单”这一闸门，同时成功下发时会发布 `WorkOrderReleasedEvent`，把后续自动派工链路显式接上。
  - `WorkOrderEventListenerTest` + `DispatchServiceTest` 共同证明派工侧会消费 `WorkOrderReleasedEvent`，并从工单工作清单生成 `DispatchTask`；若工单没有工作清单，则失败会抛出而不是被静默吞掉。
- [x] 本次复验后的结论是：
  - `1.1` 当前不再是“自动创建的工单缺工作清单”这一实现缺口；
  - 现状语义是“生产计划下达自动生成带工作清单的工单，工单下发再自动生成派工任务”，因此剩余关注点主要是证据清晰度，而不是该编号下的业务修复缺失。

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-plan -am -Dtest=ProductionPlanServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-workorder -am -Dtest=WorkOrderServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-dispatch -am -Dtest=WorkOrderEventListenerTest,DispatchServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
