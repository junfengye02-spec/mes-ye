# MES Phase 4 Process Parameter Genericization and Field Cleanup Plan

> Scope for this execution slice: implement the highest-leverage Phase 4 path that can land safely now: `4.1` generic process parameters, `10.1` handling for `MachiningProgram` as a thin compatibility adapter over the generic model, and `9.2` signature vendor decoupling for abnormal attachments.
>
> Scope extension completed during execution: `10.1` `WorkInstruction` empty-shell enrichment, `9.3` `pcclFlow` genericization to `flowCode` across plan/material models, the `9.1` furnace-field cleanup landing as generic resource context naming, and `9.4` cleanup by moving `Instruction` repair-only fields behind generic extension attributes and renaming core chain `workType` to generic `businessType`.

**Goal:** Replace hardcoded spray/machining top-level persistence with a reusable process-parameter schema/value model, keep existing spray and machining APIs compatible via adapter services, and remove the `fadadaFlag` vendor name from the abnormal attachment read/write model.

**Remaining non-goals after this slice:** any full `WorkInstruction`/`Instruction` merge and any broader product-positioning rewrite beyond the code/data-contract cleanup above.

## Task 1: Introduce generic process-parameter persistence

- [x] Add `mes_process_parameter_schema` and `mes_process_parameter_value` DDL in a new migration.
- [x] Add `ProcessParameterSchema` / `ProcessParameterValue` entities and mappers in `mes-process`.
- [x] Add a focused service/helper that:
  - auto-registers schemas such as `SPRAY_CONDITION` and `MACHINING_PROGRAM`
  - stores JSON field definitions and JSON parameter values
  - supports page/detail/create/update/delete by schema code

## Task 2: Move spray and machining flows behind the generic model

- [x] Write RED tests for the new generic parameter service behavior.
- [x] Write RED service tests proving:
  - spray condition create/update/page/detail map to generic parameter records
  - machining program create/update/page/detail map to generic parameter records
- [x] Refactor `SprayConditionServiceImpl` and `MachiningProgramServiceImpl` into compatibility adapters over the generic model.
- [x] Preserve current controller contracts and DTO/VO shapes.

## Task 3: Decouple vendor-specific abnormal attachment fields

- [x] Write RED tests for abnormal attachment VO/service behavior using generic signature fields.
- [x] Add `signatureProvider` and `signatureStatus` to the attachment model and migration.
- [x] Keep current signed workflow behavior, but stop exposing `fadadaFlag` as the primary model field.

## Task 4: Extend the empty-shell and field-leak follow-up

- [x] Enrich `WorkInstruction` with name/process/version/content/remark so it is no longer a 3-field shell.
- [x] Add service tests proving `WorkInstruction` page/detail/create map the enriched fields and process-name backfill correctly.
- [x] Replace `furnaceResourceType` / `furnaceNo` with generic `resourceSubtype` / `resourceGroupCode` naming across work-center/query/APS/frontend paths.
- [x] Rename `pcclFlow` to generic `flowCode` in plan/material models, update frontend types, and keep request-side compatibility via JSON aliasing.
- [x] Move `Instruction.repairGuideDrawing` / `Instruction.gtType` behind generic `extensionData`, while keeping DTO/VO compatibility fields mapped from the extension payload.
- [x] Rename cross-module `workType` fields to generic `businessType` in plan/workorder/material models, keep request/query/response compatibility bridges for `workType`, and align frontend bindings and SQL migrations.
- [x] Add RED/GREEN tests proving `OrderPlan` and `MaterialReturn` persist and return `flowCode`.
- [x] Add RED/GREEN tests proving work-center APS sync and work-status query return generic resource-context fields.
- [x] Add RED/GREEN tests proving `Instruction` create/detail serialize repair-specific data through generic extension attributes.
- [x] Add RED/GREEN tests proving `businessType` persists through order-plan/workorder/material flows and production-plan release passes it into auto-created work orders.

## Task 5: Verify

- [x] Run targeted module tests for `mes-process` and `mes-abnormal`.
- [x] Re-run any touched existing tests to confirm no regression in earlier phases.
- [x] Report which Phase 4 items are completed in code and which remain intentionally deferred.

Verification run during this slice:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-process,mes-abnormal -am -Dtest=ProcessParameterStoreServiceTest,SprayConditionServiceTest,MachiningProgramServiceTest,WorkInstructionServiceTest,AbnormalContactServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-plan,mes-material -am -Dtest=OrderPlanServiceTest,MaterialReturnServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-aps,mes-query -am -Dtest=ApsIntegrationFullTest,WorkStatusViewServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-process -am -Dtest=ProcessParameterStoreServiceTest,SprayConditionServiceTest,MachiningProgramServiceTest,WorkInstructionServiceTest,InstructionServiceTest,RouteServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-plan,mes-material,mes-workorder -am -Dtest=OrderPlanServiceTest,ProductionPlanServiceTest,MaterialReturnServiceTest,WorkOrderServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-common,mes-framework,mes-basic,mes-process,mes-workorder,mes-plan,mes-material -am test-compile`
- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-common,mes-framework,mes-basic,mes-team,mes-process,mes-workorder,mes-dispatch,mes-plan,mes-quality,mes-query,mes-material,mes-aps -am test-compile`
- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run build`

Follow-up verification on 2026-05-28:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-abnormal -am -Dtest=AbnormalContactServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run build`

Current-state notes after the follow-up audit:

- `9.2` is now closed at the API-contract level as well as the persistence level:
  - `AbnormalContactAttachmentVO` exposes only `signatureProvider` / `signatureStatus` plus `signed`.
  - `fadadaFlag` remains only as an internal compatibility/persistence bridge on the entity so historical rows can still backfill generic signature data.
  - `AbnormalContactServiceTest` now proves both conditions: vendor field absent from the VO contract and legacy rows still backfill generic signature fields.
- `9.3` / `9.4` compatibility aliases were re-audited and are currently compatibility-only rather than primary fields:
  - frontend forms/tables bind `businessType` / `flowCode` as the primary fields;
  - backend DTOs accept `workType` / `pcclFlow` through `@JsonAlias`;
  - backend VOs mirror `workType` only as a compatibility echo populated from `businessType`.
- `10.1` `MaterialPrice` was re-audited and the current acceptable end-state is now explicit:
  - backend still models it as the intentionally simple `materialId + unitPrice + unit` price table;
  - a stray frontend drift had expanded the page/type layer to unsupported fields such as `priceType`, `currency`, `supplier`, and effective-date ranges;
  - that drift has now been removed so the frontend once again matches the backend's simple-price contract rather than pretending a richer pricing model already exists.

Verification run for this follow-up:

- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run build`

Additional implementation note:

- Added static contract guard `mes-frontend/src/types/basic.contract.ts` so future frontend edits cannot silently drift `MaterialPrice` away from the backend-supported `unitPrice/unit` shape without breaking `vue-tsc`.

## 2026-05-28 Follow-up: Fresh Proof for 4.1 and 10.1 Process Parameter Adapters

- [x] `4.1` 通用工艺参数模型在当前代码上复验通过：
  - `ProcessParameterStoreServiceTest` 继续证明通用 schema/value 持久化与读取语义可用；
  - `SprayConditionServiceTest` / `MachiningProgramServiceTest` 继续证明喷涂条件与加工程序已经作为兼容适配层运行在通用参数模型之上，而不是继续依赖独立顶级持久化路径。
- [x] `10.1` 本轮范围内的空壳实体补强在当前代码上复验通过：
  - `WorkInstructionServiceTest` 继续证明 `WorkInstruction` 已具备名称、工序、版本、内容等核心字段的读写映射，不再是只有 3 个业务字段的空壳。
- [x] 相关前端当前态构建通过：
  - `mes-frontend` build 继续通过，保留既有动态导入与 chunk size warning，但无构建失败。

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-process -am -Dtest=ProcessParameterStoreServiceTest,SprayConditionServiceTest,MachiningProgramServiceTest,WorkInstructionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run build`

## 2026-05-28 Follow-up: Fresh Proof for 9.1 / 9.3 / 9.4 and MaterialPrice Current State

- [x] `9.1` 通用资源上下文字段在当前代码上复验通过：
  - `WorkStatusViewServiceTest` 继续证明查询视图返回的是 `resourceGroupCode`；
  - `ApsIntegrationFullTest` 继续证明 APS 主数据同步输出使用 `resourceSubtype`，而不是旧的 furnace 专属命名。
- [x] `9.3` `pcclFlow -> flowCode` 在当前代码上复验通过：
  - `MaterialReturnServiceTest` 继续证明退料服务保存与回传的都是 `flowCode`；
  - 前端 build 继续通过，说明 `plan.ts` / `material-mgmt.ts` 的泛化字段契约仍保持一致。
- [x] `9.4` 维修专属字段迁移到扩展属性在当前代码上复验通过：
  - `InstructionServiceTest` 继续证明 `gtType` / `repairGuideDrawing` 已作为兼容字段桥接到 `extensionData` 持久化与详情回传；
  - `OrderPlanServiceTest` / `ProductionPlanServiceTest` / `WorkOrderServiceTest` 继续证明核心链路的主字段已使用 `businessType`，不再依赖旧的 `workType` 作为主语义。
- [x] `10.1` `MaterialPrice` 的当前可接受终态已再次复验：
  - 前端 `basic.contract.ts` 与 fresh build 继续约束它只暴露后端真实支持的 `unitPrice / unit` 简单价格模型，没有重新漂移出未实现的复杂定价字段。

Verification run for this follow-up:

- `mvn -f /Users/jf/Desktop/mesYe/mes-backend/pom.xml -pl mes-aps,mes-query,mes-process,mes-material,mes-plan,mes-workorder -am -Dtest=ApsIntegrationFullTest,WorkStatusViewServiceTest,MaterialReturnServiceTest,InstructionServiceTest,OrderPlanServiceTest,ProductionPlanServiceTest,WorkOrderServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `npm -C /Users/jf/Desktop/mesYe/mes-frontend run build`
