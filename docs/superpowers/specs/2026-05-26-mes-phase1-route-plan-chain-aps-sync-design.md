# MES Phase 1 Route, Plan Chain, and APS Sync Design

## Goal

Implement the first MES-side repair phase from `docs/ISSUES-AND-PLAN.md`: add an explicit process route model, use it to generate work-order task lists when production plans are released, and stop MES from repeatedly pushing APS sync types that the current APS service does not support.

## Scope

This phase stays inside the `mesYe` repository. It does not change the APS repository at `/Users/jf/Desktop/apsYe/titan-aps-cloud`.

Included:

- Add `mes_route` and `mes_route_step` tables and matching Java domain/service/controller layers in `mes-process`.
- Match a production plan to an active route by product fields, then expand route steps into `WorkOrderTaskDTO` during `ProductionPlanServiceImpl.release()`.
- Preserve the existing BOM item `processId` field for compatibility. Do not migrate BOM items to `routeStepId` in this phase.
- Add APS upstream sync type support checks so unsupported master-data and feedback types do not enter a 404 retry loop.
- Add focused unit tests for route matching, work-order task expansion, production-plan release behavior, and APS unsupported-type handling.

Excluded:

- APS-side REST endpoint implementation.
- Frontend route management screens.
- BOM schema migration from `processId` to `routeStepId`.
- Status cascade, quality recheck, material requisition validation, and other later phases.

## Current State

`ProductionPlanServiceImpl.release()` creates a `WorkOrderDTO` without `tasks`. `WorkOrderServiceImpl.release()` requires at least one `mes_work_order_task`, so automatically generated work orders cannot be released without manual task entry.

The process module has `ProcessInfo` and `ManufacturingBomItem.processId`, but no reusable, ordered route model. `ProcessInfo` is a flat catalog. BOM items are material-oriented and cannot reliably represent route sequence or dependencies.

`ApsUpstreamSyncServiceImpl.pushToAps()` contains endpoints for more MES sync types than APS currently implements. Unsupported feedback and master-data types can produce repeated 404 retries.

## Design Decisions

### Route Model

Create a reusable route header and ordered route steps in `mes-process`.

`Route` stores product matching and lifecycle fields:

- `routeCode`, `routeName`
- `productCode`, `productCategory`, `machineModel`, `productType`
- `status`
- `effectiveDate`, `expiryDate`
- standard tenant, audit, and soft-delete fields through `BaseEntity`

`RouteStep` stores the executable ordered steps:

- `routeId`
- `sequenceNo`
- `processId`
- denormalized `processNo` and `processName` for stable display and work-order task creation
- `workCenterId`
- `handleTime`
- optional dependency fields: `predecessorStepId`, `parallelFlag`, `optionalFlag`
- standard tenant, audit, and soft-delete fields through `BaseEntity`

The first implementation supports ordered linear routes. Dependency and parallel flags are persisted for compatibility with later APS work, but task generation uses `sequenceNo`.

### Route Matching

Add a route query service method that finds one active route for a production plan:

1. `status = ACTIVE`
2. effective date is empty or not after today
3. expiry date is empty or not before today
4. product fields match from most specific to least specific

The first matching pass will prefer exact `productCode`, then fall back to `productCategory + machineModel`, then `productCategory`. This keeps route lookup useful for existing data that may not have perfect product codes.

If no route is found, production-plan release fails with a clear business error instead of creating a broken work order. This is stricter than the current behavior, but it prevents hidden downstream failures.

### Work-Order Task Expansion

Add a private route-to-work-order task assembler in `ProductionPlanServiceImpl`. `mes-plan` will depend on `mes-process`, inject the route service, and convert active route steps into `WorkOrderTaskDTO`:

- `taskNo = processNo`
- `taskName = processName`
- `planWorkCenterId = routeStep.workCenterId`
- `planQty = productionPlan.planQty`
- `qtyUnit = productionPlan.qtyUnit`
- `sequenceNo = routeStep.sequenceNo`
- `serialNo = null` because `ProductionPlan` does not expose a serial number in the current model
- `projectName = null` because `ProductionPlan` does not expose a project name in the current model

`ProductionPlanServiceImpl.release()` sets `workOrderDTO.tasks` before calling `workOrderService.create(workOrderDTO)`.

`WorkOrderServiceImpl.create()` already saves child tasks, so no change is required there. Tests will verify task persistence through the existing path.

### APS Sync Type Guard

Introduce an explicit private supported upstream type set in `ApsUpstreamSyncServiceImpl`.

Supported by current APS:

- `WORKORDER`
- `INVENTORY`
- `QUALITY`
- `OUTSOURCE`
- `TRANSFER`
- `ABNORMAL`

Unsupported in this MES-only phase:

- feedback types such as `DISPATCH`, `START_CHECK`, `CONSTRAINT`, `SHIFT_OUTPUT`, `MATERIAL_SHORTAGE`, `REQUISITION`, `SUPPLY_PROGRESS`, `STATUS_CHANGE`, `PROCESS_CHANGE`
- queue-driven master-data types such as `WORK_CENTER`, `PROCESS_ROUTE`, `BOM`, `MATERIAL_MASTER`, `TEAM`

When a queue item has an unsupported type, do not call `ApsClient`. Mark the item as terminal `FAILED`, leave `retryCount` unchanged, and set `errorMessage` to explain that the type is not supported by the current APS contract. This uses the existing `SyncStatus` enum without introducing a new status in Phase 1.

The existing `ApsMasterDataSyncServiceImpl.sync*()` methods remain unchanged in this phase because they are manually invoked full sync paths. The Phase 1 guard targets upstream queue processing and retry storms.

## Data Flow

Production plan release:

1. User releases a `ProductionPlan`.
2. `ProductionPlanServiceImpl.release()` validates the plan is `CREATED`.
3. Route service finds the active route matching the plan product fields.
4. Route steps are converted into work-order task DTOs.
5. Work order is created with tasks.
6. Production plan status log is written as today.
7. The generated work order can pass `WorkOrderServiceImpl.release()` task validation.

APS queue processing:

1. `ApsUpstreamSyncServiceImpl.processQueue()` loads pending queue items.
2. Before HTTP dispatch, each item is checked against supported upstream sync types.
3. Supported types use the existing endpoint mapping.
4. Unsupported types are marked `FAILED` with an explicit non-retry message.
5. The sync batch result counts unsupported items as failures, but they do not consume retry attempts or loop back to `PENDING`.

## Error Handling

Route lookup errors are business errors:

- No active route found: release is rejected before the production plan status is changed.
- Active route has no steps: release is rejected.
- Route step references a missing process only when the implementation needs process enrichment. If route step stores denormalized process fields, task generation can still proceed from route-step data.

APS unsupported type errors are operationally clear:

- Logs include queue id, sync type, and data number.
- Queue item message explains that the type is not supported by the current APS contract.
- Unsupported types must not consume retry attempts in a loop.

## Testing

Add unit tests before implementation code:

- `RouteServiceTest`: active route lookup prefers exact product code and falls back by product category/machine model.
- `ProductionPlanServiceTest`: route steps produce deterministic `WorkOrderTaskDTO` values ordered by `sequenceNo`.
- `ProductionPlanServiceTest`: releasing a production plan creates a work order with generated tasks.
- `ProductionPlanServiceTest`: releasing without a matching active route fails before creating a work order.
- `ApsUpstreamSyncServiceTest`: unsupported sync type does not call `ApsClient` and is marked non-retry.
- Existing work-order tests continue to verify that work orders without tasks cannot be released.

Run targeted Maven tests for touched modules, then run broader affected-module tests:

- `mvn -pl mes-process test`
- `mvn -pl mes-plan test`
- `mvn -pl mes-aps test`

## Rollout Notes

The migration should be additive. Existing data remains valid. Deployments without route seed data will reject production-plan releases until routes are configured, which is intentional for this phase because creating broken work orders is worse than failing early.

Route management UI is not part of this phase. Routes can be inserted through SQL, API, or later frontend work.

## Open Decisions Resolved

- Use a real Route model instead of generating tasks from flat `ProcessInfo`.
- Keep this phase MES-only and avoid APS repository changes.
- Do not migrate existing BOM item `processId` references yet.
- Fail production-plan release early if no active route can be found.
