# MES Phase 3 Material Validation and APS Master-Data Endpoint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete Phase 3 from `docs/ISSUES-AND-PLAN.md`: fix material requisition validation and consistency gaps in MES, and add the missing MES master-data receiving endpoints in APS so full master-data sync no longer returns 404.

**Architecture:** Keep MES-side requisition rules inside `mes-material`, backed directly by `mes-workorder` requirement rows and existing inventory services. On the APS side, extend `MesIntegrationController` and `MesIntegrationService` with additive master-data receiver endpoints, then persist into the existing APS `item`, `product_routing`, `resource`, and `process_definition` models through focused upsert helpers instead of introducing a second import path.

**Tech Stack:** Java 17, Spring Boot 3.x, MyBatis-Plus 3.5, Spring Data JPA, OpenFeign, JUnit 5, Mockito, Maven.

---

## File Structure

Modify in `mesYe`:

- `mes-backend/mes-material/pom.xml`: add `mes-workorder` dependency for work-order validation.
- `mes-backend/mes-material/src/main/java/com/mes/material/service/impl/MaterialRequisitionServiceImpl.java`: validate work-order status, reject empty/invalid items, cap issue quantities by remaining requirement, maintain stock rollback/reapply on update, and update `mes_work_order_input_material.issued_qty`.
- `mes-backend/mes-material/src/test/java/com/mes/material/service/MaterialRequisitionServiceTest.java`: service-level red/green tests for Phase 3 requisition rules.

Modify in APS sibling repo:

- `/Users/jf/Desktop/apsYe/titan-aps-cloud/aps-schedule-service/src/main/java/com/titan/aps/schedule/controller/MesIntegrationController.java`: add the 5 missing `/api/mes/master-data/*` endpoints.
- `/Users/jf/Desktop/apsYe/titan-aps-cloud/aps-schedule-service/src/main/java/com/titan/aps/schedule/service/MesIntegrationService.java`: declare master-data receiver methods.
- `/Users/jf/Desktop/apsYe/titan-aps-cloud/aps-schedule-service/src/main/java/com/titan/aps/schedule/service/impl/MesIntegrationServiceImpl.java`: implement request handling and delegate to APS order/resource upsert helpers.
- `/Users/jf/Desktop/apsYe/titan-aps-cloud/aps-common/aps-common-api/src/main/java/com/titan/aps/common/api/dto/`: add DTOs for MES work centers, process routes, BOMs, materials, and teams if current DTOs are insufficient.
- `/Users/jf/Desktop/apsYe/titan-aps-cloud/aps-schedule-service/src/test/java/com/titan/aps/schedule/service/MesIntegrationServiceMasterDataTest.java`: verify endpoint/service behavior and persistence/update semantics.

## Task 1: Fix MES material requisition validation and consistency

**Files:**
- Modify: `mes-backend/mes-material/pom.xml`
- Modify: `mes-backend/mes-material/src/main/java/com/mes/material/service/impl/MaterialRequisitionServiceImpl.java`
- Create: `mes-backend/mes-material/src/test/java/com/mes/material/service/MaterialRequisitionServiceTest.java`

- [ ] **Step 1: Write failing requisition validation tests**

Create `MaterialRequisitionServiceTest` with Mockito coverage for:

```java
@Test
@DisplayName("创建领料单 - 非 RELEASED/IN_PROGRESS 工单拒绝")
void create_rejectsWorkOrderWithInvalidStatus() { }

@Test
@DisplayName("创建领料单 - 明细为空时拒绝")
void create_rejectsEmptyItems() { }

@Test
@DisplayName("创建领料单 - 物料为空时拒绝")
void create_rejectsNullMaterialId() { }

@Test
@DisplayName("创建领料单 - 超出工单剩余需求时拒绝")
void create_rejectsIssueQtyBeyondRemainingRequirement() { }

@Test
@DisplayName("创建领料单 - 成功时更新工单物料已发数量并扣减库存")
void create_updatesIssuedQtyAndDeductsInventory() { }

@Test
@DisplayName("修改领料单 - 先冲销旧库存和旧已发数量，再应用新明细")
void update_revertsOldStateBeforeApplyingNewItems() { }
```

- [ ] **Step 2: Run the requisition tests and verify RED**

Run:

```bash
mvn -pl mes-backend/mes-material -Dtest=MaterialRequisitionServiceTest test
```

Expected: compilation failures because the service does not yet depend on `mes-workorder`, or assertion failures because the validation and rollback rules are missing.

- [ ] **Step 3: Implement the minimal requisition fixes**

Key rules:

```java
AssertUtil.isFalse(CollectionUtils.isEmpty(dto.getItems()), "领料明细不能为空");

WorkOrder workOrder = workOrderMapper.selectById(dto.getWorkOrderId());
AssertUtil.notNull(workOrder, "关联工单不存在");
AssertUtil.isTrue(
        WorkOrderStatus.RELEASED.getCode().equals(workOrder.getStatus())
                || WorkOrderStatus.IN_PROGRESS.getCode().equals(workOrder.getStatus()),
        "仅已下达/执行中的工单可以领料");
```

For each detail:

```java
AssertUtil.notNull(itemDTO.getMaterialId(), "领料明细物料不能为空");
BigDecimal requestedQty = firstNonNull(itemDTO.getIssueQty(), itemDTO.getDemandQty());
WorkOrderInputMaterial requirement = findRequirement(dto.getWorkOrderId(), itemDTO.getMaterialId());
BigDecimal issuedQty = firstNonNull(requirement.getIssuedQty(), BigDecimal.ZERO);
BigDecimal remainingQty = requirement.getRequiredQty().subtract(issuedQty);
AssertUtil.isTrue(requestedQty.compareTo(remainingQty) <= 0, "领料数量超过工单剩余需求");
requirement.setIssuedQty(issuedQty.add(requestedQty));
inputMaterialMapper.updateById(requirement);
```

Update flow must reverse old rows before inserting new rows:

```java
for (MaterialRequisitionItem oldItem : oldItems) {
    restoreInventory(oldItem);
    rollbackIssuedQty(oldItem);
}
for (MaterialRequisitionItemDTO itemDTO : dto.getItems()) {
    saveItemAndApplyIssuedQty(id, dto.getWorkOrderId(), itemDTO);
}
```

- [ ] **Step 4: Re-run the requisition tests and verify GREEN**

Run:

```bash
mvn -pl mes-backend/mes-material -Dtest=MaterialRequisitionServiceTest test
```

Expected: PASS.

## Task 2: Add missing APS MES master-data receiving endpoints

**Files:**
- Modify: `/Users/jf/Desktop/apsYe/titan-aps-cloud/aps-schedule-service/src/main/java/com/titan/aps/schedule/controller/MesIntegrationController.java`
- Modify: `/Users/jf/Desktop/apsYe/titan-aps-cloud/aps-schedule-service/src/main/java/com/titan/aps/schedule/service/MesIntegrationService.java`
- Modify: `/Users/jf/Desktop/apsYe/titan-aps-cloud/aps-schedule-service/src/main/java/com/titan/aps/schedule/service/impl/MesIntegrationServiceImpl.java`
- Create or modify: `/Users/jf/Desktop/apsYe/titan-aps-cloud/aps-common/aps-common-api/src/main/java/com/titan/aps/common/api/dto/*`
- Create: `/Users/jf/Desktop/apsYe/titan-aps-cloud/aps-schedule-service/src/test/java/com/titan/aps/schedule/service/MesIntegrationServiceMasterDataTest.java`

- [ ] **Step 1: Write failing APS master-data tests**

Create tests for:

```java
@Test
@DisplayName("接收 MES 工作中心主数据 - 新资源 upsert 成功")
void receiveWorkCenters_upsertsResources() { }

@Test
@DisplayName("接收 MES 工艺路线主数据 - 物料与路线同时 upsert")
void receiveProcessRoutes_upsertsItemsProcessesAndRoutings() { }

@Test
@DisplayName("接收 MES BOM 主数据 - 已有物料不重复创建")
void receiveBoms_reusesExistingItems() { }

@Test
@DisplayName("接收 MES 物料主数据 - 已有码值执行更新")
void receiveMaterials_updatesExistingItems() { }
```

- [ ] **Step 2: Run the APS tests and verify RED**

Run:

```bash
mvn -f /Users/jf/Desktop/apsYe/titan-aps-cloud/pom.xml -pl aps-schedule-service -Dtest=MesIntegrationServiceMasterDataTest test
```

Expected: compilation failures because the master-data receiver methods and DTOs do not exist.

- [ ] **Step 3: Implement additive APS receiver endpoints**

Controller contract:

```java
@PostMapping("/master-data/work-centers")
public Result<Void> syncWorkCenters(@Valid @RequestBody MesWorkCenterSyncBatchDTO dto) { ... }

@PostMapping("/master-data/process-routes")
public Result<Void> syncProcessRoutes(@Valid @RequestBody MesProcessRouteSyncBatchDTO dto) { ... }

@PostMapping("/master-data/boms")
public Result<Void> syncBoms(@Valid @RequestBody MesBomSyncBatchDTO dto) { ... }

@PostMapping("/master-data/materials")
public Result<Void> syncMaterials(@Valid @RequestBody MesMaterialSyncBatchDTO dto) { ... }

@PostMapping("/master-data/teams")
public Result<Void> syncTeams(@Valid @RequestBody MesTeamSyncBatchDTO dto) { ... }
```

Service upsert direction:

```java
Item item = itemRepository.findByCode(code).orElseGet(Item::new);
item.setCode(code);
item.setName(name);
item.setUnit(unit);
itemRepository.save(item);
```

```java
ProcessDefinition process = processRepository.findByCode(processNo).orElseGet(ProcessDefinition::new);
process.setCode(processNo);
process.setName(processName);
process.setDefaultCycleTime(handleTime);
processRepository.save(process);
```

```java
Resource resource = resourceRepository.findByCode(workCenterCode).orElseGet(Resource::new);
resource.setCode(workCenterCode);
resource.setName(workCenterName);
resource.setEfficiency(defaultIfNull(efficiency, 1.0));
resourceRepository.save(resource);
```

```java
ProductRouting routing = findRouting(itemId, sequenceNo).orElseGet(ProductRouting::new);
routing.setItem(item);
routing.setProcessId(process.getId());
routing.setProcessSequence(sequenceNo);
routing.setResourceId(resource != null ? resource.getId() : null);
routing.setCycleTime(defaultIfNull(handleTime, 0D));
routingRepository.save(routing);
```

- [ ] **Step 4: Re-run the APS tests and verify GREEN**

Run:

```bash
mvn -f /Users/jf/Desktop/apsYe/titan-aps-cloud/pom.xml -pl aps-schedule-service -Dtest=MesIntegrationServiceMasterDataTest test
```

Expected: PASS.

## Task 3: Cross-repo Phase 3 verification

**Files:**
- Verify: `mes-backend/mes-material`
- Verify: `/Users/jf/Desktop/apsYe/titan-aps-cloud/aps-schedule-service`
- Verify: `/Users/jf/Desktop/apsYe/titan-aps-cloud/aps-order-service`
- Verify: `/Users/jf/Desktop/apsYe/titan-aps-cloud/aps-resource-service`

- [ ] **Step 1: Run MES material tests**

```bash
mvn -pl mes-backend/mes-material -Dtest=MaterialManagementServiceTest,MaterialMgmtModuleTest,MaterialRequisitionServiceTest test
```

Expected: PASS.

- [ ] **Step 2: Run APS master-data receiver tests**

```bash
mvn -f /Users/jf/Desktop/apsYe/titan-aps-cloud/pom.xml -pl aps-schedule-service -Dtest=MesIntegrationServiceMasterDataTest test
```

Expected: PASS.

- [ ] **Step 3: Compile affected APS modules together**

```bash
mvn -f /Users/jf/Desktop/apsYe/titan-aps-cloud/pom.xml -pl aps-common,aps-order-service,aps-resource-service,aps-schedule-service -am test-compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Inspect final diffs**

```bash
git -C /Users/jf/Desktop/mesYe diff -- mes-backend/mes-material docs/superpowers/plans/2026-05-27-mes-phase3-material-validation-aps-master-data.md
git -C /Users/jf/Desktop/apsYe/titan-aps-cloud diff -- aps-common/aps-common-api aps-schedule-service
```

Expected: only Phase 3 requisition and master-data endpoint changes are present in the touched files.

## 2026-05-28 Follow-up: Phase 3 Fresh Verification Evidence

- [x] MES 侧物料校验与确定性库存路径在当前代码上复验通过:
  - `MaterialRequisitionServiceTest`
  - `DeliverySignServiceTest`
- [x] APS 侧 5 个主数据接收端点已在当前代码中存在并通过定向服务测试:
  - `/api/mes/master-data/work-centers`
  - `/api/mes/master-data/process-routes`
  - `/api/mes/master-data/boms`
  - `/api/mes/master-data/materials`
  - `/api/mes/master-data/teams`
- [x] APS 相关受影响模块当前编译通过:
  - `aps-common`
  - `aps-order-service`
  - `aps-resource-service`
  - `aps-schedule-service`

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-material -am -Dtest=MaterialRequisitionServiceTest,DeliverySignServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/apsYe/titan-aps-cloud/pom.xml -pl aps-schedule-service -am -Dtest=MesIntegrationServiceMasterDataTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/apsYe/titan-aps-cloud/pom.xml -pl aps-common,aps-order-service,aps-resource-service,aps-schedule-service -am test-compile`
