# MES 系统逻辑问题与修复计划

> 基于对全部 12 个后端业务模块、14 个前端视图模块、33 个数据库迁移脚本的全面审查。

---

## 一、业务链断裂 (P0)

### 1.1 计划→工单→派工 自动链缺工作清单

**位置**: `ProductionPlanServiceImpl.release()` → `WorkOrderServiceImpl.release()`

**问题**: 生产计划下达时自动创建工单，但未填充 `tasks`（工作清单）。工单下达时强制校验「至少包含一个工作清单」，导致自动生成的工单无法直接下达，需人工补充。

**修复**:
- 生产计划下达时，从关联的制造 BOM 工序路线自动展开生成工单工作清单
- 或：基于 `ProcessInfo` 中的工序定义自动填充默认任务列表

### 1.2 状态机缺少自底向上的自动级联

**位置**: `DispatchTaskServiceImpl.complete()` → `WorkOrderServiceImpl.complete()` → `ProductionPlanServiceImpl`

**问题**:
- 派工任务全部完成后，工单不会自动完工
- 工单完工后，生产计划的 `completedQty` 不会更新
- 所有生产计划完成后，订单计划不会自动完成

**修复**:
- `DispatchTaskServiceImpl.complete()` 中增加：检查该工单下所有派工是否都已完成，若是则发布 `DispatchAllTasksCompletedEvent`
- `WorkOrderServiceImpl` 监听该事件，自动调用 `complete()`
- `WorkOrderServiceImpl.complete()` 发布 `WorkOrderCompletedEvent`
- `ProductionPlanServiceImpl` 监听该事件，更新 `completedQty`

### 1.3 派工完工 FAIL 不触发质量复检

**位置**: `DispatchTaskServiceImpl.complete()`

**问题**: `qualityResult = FAIL` 时无任何自动化动作。不创建复检申请，不通知质量模块。

**修复**:
- `complete()` 检测到 FAIL 时，自动创建 `RecheckRequest` 并关联派工任务
- 或发布 `DispatchTaskQualityFailedEvent`，由质量模块监听处理
- `RecheckRequest` 实体增加 `dispatchTaskId` 字段用于追溯

---

## 二、物料领料模块校验缺失 (P0)

### 2.1 领料不校验工单状态

**位置**: `MaterialRequisitionServiceImpl.create()` 行 92

**问题**: 接受 `workOrderId` 但从不校验工单是否存在、状态是否允许领料（应对 RELEASED/IN_PROGRESS）。

**修复**:
```java
WorkOrder wo = workOrderMapper.selectById(dto.getWorkOrderId());
AssertUtil.notNull(wo, "关联工单不存在");
AssertUtil.isTrue(
    WorkOrderStatus.RELEASED.getCode().equals(wo.getStatus()) ||
    WorkOrderStatus.IN_PROGRESS.getCode().equals(wo.getStatus()),
    "仅已下达/执行中的工单可以领料");
```

### 2.2 领料数量不校验工单物料需求

**位置**: `MaterialRequisitionServiceImpl.create()`

**问题**: 不查询 `WorkOrderInputMaterial.requiredQty`，可超需求领料。`issuedQty` 永不更新。

**修复**:
- 创建领料明细时，查询对应 `WorkOrderInputMaterial`
- 校验 `demandQty <= (requiredQty - issuedQty)`
- 创建成功后更新 `issuedQty`

### 2.3 update() 破坏库存数据一致性

**位置**: `MaterialRequisitionServiceImpl.update()` 行 143-170

**问题**: 删除旧明细时不冲销库存扣减，新增明细时不重新扣减。每次编辑都会导致库存数据偏差。

**修复**:
- `update()` 中先对旧明细逐一调用 `addStock()` 冲销
- 再对新明细逐一调用 `deductStock()` 扣减
- 整个操作包裹在 `@Transactional` 中

### 2.4 空明细/空物料静默通过

**位置**: `MaterialRequisitionServiceImpl.create()` 行 107-133

**问题**: `items` 为空或 `materialId` 为空时静默跳过，生成无意义的领料单。

**修复**:
- 创建时校验 `items` 非空
- 循环中校验 `materialId` 非空，否则抛出异常

---

## 三、工序路线缺失 (P0)

### 3.1 缺少独立的工艺路线 (Routing) 表

**问题**: 系统没有 `mes_route` / `mes_route_step` 表。当前的工序路线隐式存在于：
- `mes_manufacturing_bom_item` 的树形结构（物料视角，不是工序视角）
- `mes_process_info` 扁平列表（无顺序、无结构）
- `mes_work_order_task` 手工录入（不可复用）

APS 收到的是扁平工序列表，需要自己推断路线结构。

**修复**:
- 新建 `mes_route`（工艺路线头）和 `mes_route_step`（路线工序步骤）表
- `mes_route` 绑定产品 (product_code, product_category 等)，定义该产品的工序顺序
- `mes_route_step` 包含：`sequence_no`、`process_id`、`work_center_id`、`handle_time`、前后工序依赖
- BOM 明细的 `process_id` 改为引用 `route_step_id`
- 工单创建时从 Route 自动展开 `WorkOrderTask`
- APS 同步发送完整 Route 结构

```sql
-- 工艺路线头
CREATE TABLE mes_route (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    route_code VARCHAR(100) NOT NULL,
    route_name VARCHAR(200),
    product_code VARCHAR(50),
    product_category VARCHAR(100),
    machine_model VARCHAR(100),
    status VARCHAR(20) DEFAULT 'DRAFT',
    effective_date DATE,
    expiry_date DATE,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    deleted TINYINT(1) DEFAULT 0,
    ...
);

-- 路线工序步骤
CREATE TABLE mes_route_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    route_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    process_id BIGINT NOT NULL,        -- FK to mes_process_info
    work_center_id BIGINT,             -- 可覆盖工序默认工作中心
    handle_time DECIMAL(10,2),         -- 标准工时
    predecessor_step_id BIGINT,        -- 前置步骤
    is_parallel TINYINT(1) DEFAULT 0,  -- 是否并行
    is_optional TINYINT(1) DEFAULT 0,  -- 是否可选
    tenant_id BIGINT NOT NULL DEFAULT 1,
    ...
);
```

---

## 四、工艺管理模块抽象混乱 (P1)

### 4.1 行业特定工艺参数硬编码为顶级模块

**位置**: `mes-process` 模块中的 `SprayCondition`、`MachiningProgram`

**问题**: 喷涂条件（热喷涂 HVOF 参数）、CNC 加工程序等极其行业特定的实体，被硬编码为工艺管理的顶级子模块。如果要加焊接、热处理、3D 打印等工艺，需要不断新建表。

**修复**:
- 新建通用的 `mes_process_parameter` 表，使用 JSON 字段存储不同工艺类型的参数
- 或将工艺参数改为 key-value 扩展属性表
- 喷涂条件、加工程序改为基于通用参数模型的配置实例

```sql
-- 通用工艺参数定义
CREATE TABLE mes_process_parameter_schema (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    schema_code VARCHAR(50) UNIQUE,    -- SPRAY / MACHINING / WELDING / ...
    schema_name VARCHAR(100),
    process_type VARCHAR(50),          -- 关联 ProcessInfo.processType
    field_definitions JSON,            -- 字段定义：[{name,type,unit,required},...]
    ...
);

-- 工艺参数值实例
CREATE TABLE mes_process_parameter_value (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    schema_id BIGINT NOT NULL,
    process_info_id BIGINT,            -- 关联具体工序
    param_values JSON,                 -- 实际参数值
    ...
);
```

### 4.2 指示书/工作指示 概念重叠

**问题**: `mes_instruction`（指示书）有 20+ 字段，`mes_work_instruction`（工作指导书）只有 3 字段。两者关系不清。

**修复**:
- 明确两者职责：指示书 = 生产随工单/流转卡，工作指示 = 作业指导书
- 或者合并为一个通用的「生产文档」模型，用文档类型区分

---

## 五、模块间集成断裂 (P1)

### 5.1 派工/质量/异常 三模块完全孤立

**问题**:
- 派工完工 FAIL → 无复检创建
- 异常联络单提交 → 不标记关联工单/派工
- 异常联络单提交 → 不触发质量动作
- 派工开工 → 不创建开工检查
- `AbnormalContact.orderNo` 是自由文本，无外键约束

**修复**:
- 建立事件驱动集成：`DispatchTaskCompletedEvent`、`QualityFailedEvent`、`AbnormalSubmittedEvent`
- `AbnormalContact` 增加 `workOrderId`、`dispatchTaskId` 外键字段
- 异常提交时自动标记关联的派工任务状态
- 派工开工时自动创建 `WorkStartCheck` 记录

### 5.2 InspectionWork 无写入路径

**问题**: `InspectionWorkServiceImpl` 只有 `page` / `getDetail`，没有 create/update。检验记录来源不明。

**修复**:
- 增加检验工作记录的创建逻辑（派工完工时、复检完成时自动生成）
- 或如果数据来自外部系统，在文档中明确说明

### 5.3 RecheckRequest 状态机未实现

**问题**: `RecheckStatus` 枚举只有 `CREATED` 一个值。

**修复**:
- 增加状态: `CREATED → SUBMITTED → IN_REVIEW → APPROVED/REJECTED → COMPLETED`
- 实现对应的 `submit()`、`review()`、`approve()`、`complete()` 方法

### 5.4 WorkOrderEventListener 静默吞错

**位置**: `WorkOrderEventListener.onWorkOrderReleased()` 行 21-31

**问题**: 事件监听器 catch 所有异常只记日志。工单已下达但派工生成失败时，系统无感知。

**修复**:
- 派工生成失败时，将工单状态回滚或标记为异常
- 引入重试机制或异步补偿任务
- 至少发送告警通知

---

## 六、订单计划逻辑 (P1)

### 6.1 expand() 方法是空操作

**位置**: `OrderPlanServiceImpl.expand()` 行 211-229

**问题**: 方法名暗示「展开为生产计划」，但实际只改了 `expandStatus` 字段。生产计划仍需人工创建。

**修复**:
- `expand()` 中自动创建 `ProductionPlan`（基于关联的 BOM/路线生成）
- 或重命名为 `markExpanded()` 以反映实际行为

---

## 七、前端 API 缺口 (P2)

### 7.1 派工写操作前端无入口

**缺失的前端 API**:
- `POST /dispatch/task/create` — 手动创建派工
- `PUT /dispatch/task/update` — 编辑派工
- `POST /dispatch/task/cancel/{id}` — 撤销派工
- `POST /dispatch/task/assign` — 统一派工（批量）
- `POST /dispatch/task/unassign/{id}` — 取消指派
- `POST /dispatch/task/start/{id}` — 开工
- `POST /dispatch/task/complete/{id}` — 完工

**修复**: 在 `src/api/dispatch/dispatchTask.ts` 中补充以上 API 调用，在对应的 Vue 页面中增加操作按钮。

### 7.2 Query 模块路由与 API 不匹配

**问题**: 前端路由定义 8 个查询视图，`workQuery.ts` 只提供 3 个 API。其余 5 个跨模块调用其他 domain API。

**修复**: 统一查询模块的 API 层，或在前端路由中明确标注数据来源。

---

## 八、技术债务 (P2)

### 8.1 工单号生成随机数碰撞风险

**位置**: `ProductionPlanServiceImpl.generateWorkOrderNo()` 行 197-201

**问题**: 使用 `Math.random()` 生成序号，多实例并发有碰撞风险。

**修复**: 改用数据库序列、Redis 自增或雪花 ID。

### 8.2 库存查询 LIMIT 1 不确定性

**位置**: `MaterialRequisitionServiceImpl` 行 125-128、`DeliverySignServiceImpl` 行 80-83

**问题**: 同一物料多库位时，`getOne(...LIMIT 1)` 随机命中，可能跳过有库存的库位。

**修复**: 增加仓库/库位筛选条件，或汇总所有库位的可用库存再判断。

### 8.3 MaterialRequisition 僵尸字段

**问题**: `salesOrderLine`、`actualQty`、`qualifiedQty`、`actualStartTime`、`actualEndTime` 在实体中存在但 DTO 中缺失，永远无法通过正常流程写入。

**修复**: 要么补全 DTO 和业务逻辑，要么从实体中移除。

---

## 九、行业特定字段泄漏到通用实体 (P1)

### 9.1 炉窑/热处理字段泄漏

**位置**: `WorkCenter.furnaceResourceType`、`WorkStatusView.furnaceNo`

**问题**: 通用的工作中心和状态视图实体中硬编码了 `furnaceResourceType`（炉窑资源类型）、`furnaceNo`（炉号），将热处理/炉窑车间的特定概念固化到了所有场景。

**修复**:
- 从通用实体中移除 `furnace*` 字段
- 改为在 `ProcessInfo` 或 `RouteStep` 中使用扩展属性 (JSON) 存储工序特定的资源需求
- 或移至 `mes_process_parameter_value`（见 4.1）

### 9.2 电子签章供应商耦合

**位置**: `AbnormalContactAttachment.fadadaFlag`

**问题**: `fadadaFlag` 直接引用了「法大大」——一家特定的中国电子签章供应商。供应商名称硬编码在数据模型中。

**修复**:
- 改为通用的 `signatureProvider VARCHAR(50)` + `signatureStatus VARCHAR(20)`
- 供应商特定逻辑移到配置或适配器层

### 9.3 `pcclFlow` 跨模块泄漏

**位置**: `OrderPlan.pcclFlow`、`MaterialReturn.pcclFlow`

**问题**: 不明缩写 `pcclFlow` 出现在计划模块和物料模块两个不相关的领域，含义不明，边界不清。

**修复**:
- 明确 `pcclFlow` 的业务含义并文档化
- 如果是通用概念，统一为一个可复用的字段定义（如 `flowCode`）
- 如果是特定业务场景的字段，移到扩展属性中

### 9.4 MRO/维修场景假设

**位置**: `OrderPlan`、`ProductionPlan`、`WorkOrder`、`MaterialReturn`、`Instruction`

**问题**: `newOrRepairType`（新制/维修类型）和 `workType` 出现在几乎所有核心实体中，说明系统底层假设了 MRO（维修/翻新）场景，而非通用制造 MES。`Instruction` 实体还有 `repairGuideDrawing`、`gtType` 等维修专属字段。

**修复**:
- 将 `newOrRepairType` 保留为合法的制造分类字段（制造 vs 维修确实有差异）
- 但 `repairGuideDrawing`、`gtType` 等应移至扩展属性
- 明确系统的行业定位，如果确实是通用 MES，需要对这些字段做分类处理

---

## 十、空壳实体——功能未完成 (P2)

### 10.1 仅 3 个业务字段的实体

| 实体 | 模块 | 字段 |
|------|------|------|
| `WorkInstruction` | mes-process | `instructionCode`, `level`, `status` |
| `MachiningProgram` | mes-process | `gCode`, `programTable`, `productName` |
| `MaterialPrice` | mes-basic | `materialId`, `unitPrice`, `unit` |

**问题**: 这些实体只有 3 个业务字段（不含 id/时间戳），基本等同于一个 key-value 对。`WorkInstruction` 和 `MachiningProgram` 同时兼有「行业特定」和「空壳」两重问题。

**修复**:
- `MachiningProgram`: 合并到通用工艺参数模型（见 4.1）
- `WorkInstruction`: 要么扩展为完整的作业指导书模型（内容、步骤、版本、附件），要么合并到 `Instruction` 中
- `MaterialPrice`: 如果是简单的单价表，可以保持；如果需要多币种/有效期/阶梯价格，需要扩展

---

## 十一、架构不一致 (P2)

### 11.1 10 个实体不继承 BaseEntity

**问题实体**: `AbnormalContactAttachment`、`AbnormalContactLog`、`ShiftHandover`、`RecheckOrderPlan`、`RecheckSerial`、`DeliverySign`、`FinishedGoodsReceiptItem`、`MaterialRequisitionItem`、`ShiftHandoverAttachment`、`WorkStatusView`

**问题**: 这些实体各自手动管理 `id`、`createdTime`、`updatedTime`、`tenantId`、`deleted` 字段，导致：
- 租户隔离可能不完整（没有统一的 `TenantLineInnerInterceptor` 自动过滤）
- 软删除逻辑不一致
- 审计字段填充不统一

**修复**:
- 统一改造这些实体继承 `BaseEntity`
- 移除手动的 id/时间/租户/删除字段
- 验证 MyBatis-Plus 的 `MetaObjectHandler` 和 `TenantLineInnerInterceptor` 对所有实体生效

---

## 十二、MES ↔ APS 集成断裂 (P0)

> 基于对 MES (`mes-aps` 模块) 和 APS (`titan-aps-cloud` 微服务) 两端完整代码的交叉审查。

### 12.1 架构概览

```
MES (mesYe)                                APS (apsYe/titan-aps-cloud)
┌────────────────────┐                    ┌──────────────────────────┐
│ ApsMasterDataSync  │── POST /api/mes/   │ MesIntegrationController │
│ (手动全量同步)     │   master-data/*    │   ⚠ 5个端点不存在        │
│                    │                    │                          │
│ ApsUpstreamSync    │── POST /api/mes/   │ 仅 6 个端点有实现:       │
│ (事件→队列→HTTP)   │   status/inventory │ reschedule, status,      │
│                    │   /quality/...     │ inventory, quality,      │
│                    │   /feedback/*      │ outsource, transfer      │
│                    │                    │                          │
│ ApsDownstreamSync  │←── GET /api/orders │ 排程结果拉取              │
│ (定时拉取APS数据)  │   /api/tasks ...   │                          │
│                    │                    │                          │
│ ApsCallbackCtrl    │←── POST /aps/      │ 排程结果/MRP/甘特/       │
│ (被动接收回调)     │   callback/*       │ 资源分配/产能负荷         │
└────────────────────┘                    └──────────────────────────┘
```

### 12.2 MES 发送 15 种同步，APS 只认 6 种

**位置**: `ApsUpstreamSyncServiceImpl.processQueue()` → 各种 APS HTTP 端点

| MES SyncType | 发送的端点 | APS 是否存在 |
|-------------|-----------|:---:|
| WORKORDER | `POST /api/mes/status/sync` | OK |
| INVENTORY | `POST /api/mes/inventory/sync` | OK |
| QUALITY | `POST /api/mes/quality/sync` | OK |
| OUTSOURCE | `POST /api/mes/outsource/status` | OK |
| TRANSFER | `POST /api/mes/transfer/status` | OK |
| ABNORMAL | `POST /api/mes/reschedule` | OK |
| **DISPATCH** | `POST /api/mes/feedback/dispatch` | ❌ 不存在 |
| **START_CHECK** | `POST /api/mes/feedback/start-check` | ❌ 不存在 |
| **CONSTRAINT** | `POST /api/mes/feedback/constraint` | ❌ 不存在 |
| **SHIFT_OUTPUT** | `POST /api/mes/feedback/shift-output` | ❌ 不存在 |
| **MATERIAL_SHORTAGE** | `POST /api/mes/feedback/material-shortage` | ❌ 不存在 |
| **REQUISITION** | `POST /api/mes/feedback/requisition` | ❌ 不存在 |
| **SUPPLY_PROGRESS** | `POST /api/mes/feedback/supply-progress` | ❌ 不存在 |
| **STATUS_CHANGE** | `POST /api/mes/feedback/status-change` | ❌ 不存在 |
| **PROCESS_CHANGE** | `POST /api/mes/feedback/process-change` | ❌ 不存在 |

**影响**: 9 个 feedback 类型的队列消息发到 APS 会返回 **404 Not Found**。队列中的这些消息会持续重试直到达到最大次数后标记为 FAILED。

**修复**:
- 方案 A: APS 侧补齐这 9 个端点（如果业务需要这些实时反馈）
- 方案 B: MES 侧只发送 APS 实际支持的 6 种同步类型，其余类型暂不入队

### 12.3 主数据同步端点 APS 侧不存在

**位置**: `ApsMasterDataSyncServiceImpl`

MES 推送端:
```java
POST /api/mes/master-data/work-centers     // 工作中心
POST /api/mes/master-data/process-routes   // 工序路线
POST /api/mes/master-data/boms             // 制造BOM
POST /api/mes/master-data/materials        // 物料主数据
POST /api/mes/master-data/teams            // 班组
```

**APS 侧**: `MesIntegrationController` 中**不存在这些端点**。APS 的数据导入走的是 `/api/import` 或 `/api/data`（Excel 文件上传导入），不是 MES 的 REST 推送。

**影响**: 主数据同步功能完全不可用。MES 的「同步主数据到 APS」按钮点下去，所有请求都 404。

**修复**:
- APS 侧新增 `POST /api/mes/master-data/*` 端点，接收 JSON 数据并写入 APS 的 Order/Item/Resource/Process/Routing 表
- 或者统一改为 MES 导出 Excel → APS 通过 `/api/import` 导入的方式
- 引入增量同步机制（当前是全量 `selectList(null)`）

### 12.4 MES 推给 APS 的「工序路线」是扁平无序列表

**位置**: `ApsMasterDataSyncServiceImpl.syncProcessRoutes()` 行 86

```java
List<ProcessInfo> all = processInfoMapper.selectList(null);
// → 发送给 POST /api/mes/master-data/process-routes
```

发送的是 `mes_process_info` 的全部行——**一个没有 sequence_no、没有依赖关系的扁平工序定义列表**。

而 APS 侧的 `product_routing` 表有完整的路线字段：
```java
processSequence: Integer    // 工序顺序
resourceId: Long            // 工作中心
cycleTime: Double           // 标准加工时间
setupTime: Double           // 换型时间
yieldRate: Double           // 良率
```

**影响**: 即使主数据同步端点修好了，APS 收到的也是缺失关键信息的工序列表。APS 无法从中构建出正确的工艺路线。

**修复**:
- 先将 MES 的工艺路线表建好（见 3.1）
- 路线同步时发送完整结构：工序顺序、依赖关系、标准工时、工作中心映射

### 12.5 APS 回调有空实现

**位置**: MES 侧 `ApsCallbackController` + `ApsExtendedCallbackController`

| APS 回调 | MES 处理 | 问题 |
|----------|----------|------|
| `schedule-result` | 更新工单计划时间 | OK |
| `request-rejected` | 记录日志 | OK |
| `mrp-result` | **TODO: 仅记日志** | MRP 物料需求计算结果已收到，但未自动生成领料单/采购申请 |
| `resource-allocation` | 更新派工时间+创建分配 | OK |
| `gantt-data` | **仅记日志** | 甘特图数据收到但不落库，前端无法展示 APS 排程甘特图 |
| `capacity-load` | **仅记日志** | 产能负荷数据收到但不触发预警 |
| `schedule-change` | 更新时间（取消只记日志） | APS 排程取消的工单在 MES 中不会自动处理 |

**修复**:
- `mrp-result`: 自动生成 `MaterialRequisition` 或 `PurchaseRequest`
- `gantt-data`: 写入 `mes_aps_gantt_cache` 表供前端展示
- `capacity-load`: 写入 `mes_aps_capacity_load` 表，超阈值时触发告警
- `schedule-change` (CANCELLED): 增加自动取消工单/派工的逻辑（或至少通知相关角色）

### 12.6 SyncType 枚举膨胀——29 种类型远超实际需要

**位置**: `SyncType` 枚举

MES 定义了 29 种 SyncType，但：
- APS 只处理 6 种核心类型
- 9 种 feedback 类型 APS 端不存在
- 5 种 master-data 类型 APS 端不存在
- 多种类型之间存在模糊边界（DISPATCH vs STATUS_CHANGE vs PROCESS_CHANGE）

**修复**:
- 将 SyncType 精简为实际可用的类型
- 与 APS 团队对齐：哪些数据需要 MES→APS 同步，哪些需要 APS→MES 同步
- 制定明确的集成协议文档（包含端点、数据格式、触发条件、错误处理）

---

## 实施顺序建议

| 阶段 | 内容 | 影响范围 |
|------|------|----------|
| **Phase 1** | 1.1 计划链补全 + 3.1 工艺路线表新建 + **12.2 对齐 MES/APS 同步类型** | 数据模型、计划/工单/派工、APS 集成 |
| **Phase 2** | 1.2 状态级联 + 1.3 质量 FAIL 处理 + 5.1 模块事件集成 | 派工/工单/质量/异常模块 |
| **Phase 3** | 2.1-2.4 领料校验修复 + **12.3 补齐 APS 主数据同步端点** | 物料模块、APS |
| **Phase 4** | 4.1 工艺参数通用化 + 9.1-9.4 行业字段清理 + 10.1 空壳实体处理 | 工艺/基础数据/异常/物料实体 |
| **Phase 5** | 5.2-5.4 补全状态机 + 监听器修复 + 4.2 指示书合并 + **12.5 APS 回调补全** | 质量/异常/工艺模块、APS 集成 |
| **Phase 6** | 6.1 expand 逻辑 + 7.1-7.2 前端补全 + **12.4 路线同步修复** | 计划模块 + 前端 + APS |
| **Phase 7** | 8.1-8.3 技术债务 + 11.1 BaseEntity 统一 + **12.6 SyncType 精简** | 各模块、APS 集成 |

## 2026-05-28 当前态审计

> 本节用于把上面的原始问题清单映射到 2026-05-28 当前代码状态。结论以当前仓库代码、分阶段 follow-up 文档、以及对应定向验证为准。

### 审计结论

- 原计划中的 32 个编号项当前都已有明确归类。
- 其中 `12.1` 属于“架构概览”型条目，不是独立编码缺口；它当前已由 `12.2` ~ `12.6` 的实现收口和显式架构证据共同覆盖。
- 其余 31 个编号项当前都已具备实现闭环或兼容闭环，并在分阶段文档中补充了 fresh evidence。

### 编号项状态总表

| 编号 | 当前归类 | 当前结论 | 证据归档 |
|------|----------|----------|----------|
| `1.1` | 已关闭 | 生产计划下达会按 Route 展开工单工作清单，工单下达再触发自动派工 | Phase 1 follow-up: `Fresh Proof for 1.1 Plan -> WorkOrder -> Dispatch Chain` |
| `1.2` | 已关闭 | 派工 -> 工单 -> 生产计划 -> 订单计划的自底向上级联已到位，并补齐边界语义证明 | Phase 2 follow-up: `Fresh Proof for 1.2 Cascade Edge Cases` |
| `1.3` | 已关闭 | 派工完工 `FAIL` 会触发质量复检申请，并保留派工/工单追溯 | Phase 5 follow-up: `Fresh Proof for 1.3 / 5.3 Recheck Flow` |
| `2.1` | 已关闭 | 领料已校验关联工单存在且状态允许领料 | Phase 3 follow-up: `Phase 3 Fresh Verification Evidence` |
| `2.2` | 已关闭 | 领料已按工单物料需求与剩余已发数量做上限校验 | Phase 3 follow-up: `Phase 3 Fresh Verification Evidence` |
| `2.3` | 已关闭 | 领料单更新会先冲销旧库存/旧已发数量，再应用新明细 | Phase 3 follow-up: `Phase 3 Fresh Verification Evidence` |
| `2.4` | 已关闭 | 空明细与空物料已改为显式拒绝，不再静默生成空单据 | Phase 3 follow-up: `Phase 3 Fresh Verification Evidence` |
| `3.1` | 已关闭 | 独立 Route / RouteStep 已落地，且 BOM 明细已补齐 `routeStepId` 兼容桥接 | Phase 1 follow-ups: `3.1 BOM Route-Step Reference Completion` |
| `4.1` | 已关闭 | 工艺参数已转入通用参数模型，喷涂/加工程序保留为兼容适配器 | Phase 4 follow-up: `Fresh Proof for 4.1 and 10.1 Process Parameter Adapters` |
| `4.2` | 已关闭 | `Instruction` 与 `WorkInstruction` 的边界已明确并经服务测试回归保护 | Phase 5 follow-up: `Fresh Proof for 11.1 / 4.2 / 5.4` |
| `5.1` | 已关闭 | 派工/质量/异常三模块已通过事件与关联字段打通，并补齐前端入口与语义修正 | Phase 2 follow-ups: `5.1 Abnormal Work-Order Link Completion`, `5.1 Frontend Link-Field Closure`, `5.1 Abnormal Event Semantics Correction`, `5.1 orderNo Consistency Closure` |
| `5.2` | 已关闭 | `InspectionWork` 已具备 dispatch/recheck 事件投影写路径，不再是只读壳层 | Phase 5 follow-up: `Fresh Proof for 5.2 InspectionWork Projection` |
| `5.3` | 已关闭 | `RecheckRequest` 完整状态机与前端工作流已落地 | Phase 5 follow-up: `Fresh Proof for 1.3 / 5.3 Recheck Flow` |
| `5.4` | 已关闭 | 派工生成失败不再被监听器静默吞错，而是记录后向上抛出 | Phase 5 follow-up: `Fresh Proof for 11.1 / 4.2 / 5.4` |
| `6.1` | 已关闭 | `expand()` 会真实创建 `ProductionPlan`，而不是只翻转状态位 | Phase 6 follow-up: `Fresh Proof for 6.1 / 7.1 / 7.2` |
| `7.1` | 已关闭 | 派工前端已补齐创建、编辑、撤销、指派、开工、完工等写操作入口 | Phase 6 follow-up: `Fresh Proof for 6.1 / 7.1 / 7.2` |
| `7.2` | 已关闭 | Query 页面已统一经 `workQueryApi` 访问，不再跨域混用其他模块 API | Phase 6 follow-up: `Fresh Proof for 6.1 / 7.1 / 7.2` |
| `8.1` | 已关闭 | 工单号生成已改为 `DistributedIdGenerator`，移除随机碰撞风险 | Phase 5 follow-up: `Phase 7 Completion Evidence and 12.6 Real Enum Shrink` |
| `8.2` | 已关闭 | 库存选取已改为确定性排序，不再依赖歧义 `LIMIT 1` | Phase 5 follow-up: `Phase 7 Completion Evidence and 12.6 Real Enum Shrink` |
| `8.3` | 已关闭 | `MaterialRequisition` 僵尸字段已贯通 DTO/VO 与前端契约 | Phase 5 follow-up: `Phase 7 Completion Evidence and 12.6 Real Enum Shrink` |
| `9.1` | 已关闭 | 炉窑语义已泛化为通用资源上下文字段，不再把热处理概念硬编码到通用实体 | Phase 4 follow-up: `Fresh Proof for 9.1 / 9.3 / 9.4 and MaterialPrice Current State` |
| `9.2` | 已关闭 | 电子签章字段已从供应商硬编码转为通用签章提供方/状态契约 | Phase 4 follow-up: `Follow-up verification on 2026-05-28` |
| `9.3` | 已关闭 | `pcclFlow` 已统一泛化为 `flowCode`，旧字段只保留兼容语义 | Phase 4 follow-up: `Fresh Proof for 9.1 / 9.3 / 9.4 and MaterialPrice Current State` |
| `9.4` | 已关闭 | 维修专属字段已迁入扩展属性，主链路字段已泛化为 `businessType` | Phase 4 follow-up: `Fresh Proof for 9.1 / 9.3 / 9.4 and MaterialPrice Current State` |
| `10.1` | 已关闭 | `MachiningProgram` 已转兼容适配，`WorkInstruction` 已补强，`MaterialPrice` 保持简单单价表并与前端契约一致 | Phase 4 follow-ups: `Fresh Proof for 4.1 and 10.1 Process Parameter Adapters`, `Fresh Proof for 9.1 / 9.3 / 9.4 and MaterialPrice Current State` |
| `11.1` | 已关闭 | 首批 10 个未继承 `BaseEntity` 的实体已统一并有专项回归 | Phase 5 follow-up: `Fresh Proof for 11.1 / 4.2 / 5.4` |
| `12.1` | 描述性概览项，已显式归档 | 当前 MES ↔ APS 架构拓扑已能与概览图一一对应；真正缺口落在 `12.2` ~ `12.6` 并已分别收口 | Phase 5 follow-up: `Fresh Proof for 12.1 APS Integration Architecture Overview` |
| `12.2` | 已关闭 | 上行合同已收缩到 APS 真正支持的类型，不再把不支持的 feedback 直接推入 APS | Phase 5 follow-ups: `12.6 Queue-Semantics Guardrail`, `Phase 7 Completion Evidence and 12.6 Real Enum Shrink` |
| `12.3` | 已关闭 | APS 侧 5 个主数据接收端点已补齐并通过定向服务测试 | Phase 3 follow-up: `Phase 3 Fresh Verification Evidence` |
| `12.4` | 已关闭 | MES -> APS 工艺路线同步已改为有序 `RouteStep` 结构，而非扁平 `ProcessInfo` 列表 | Phase 5 follow-up: `12.4 Route Sync Completion` |
| `12.5` | 已关闭 | MRP、甘特、产能负荷、排程取消等 APS 回调已从空实现改为真实落库或业务动作 | Phase 5 main slice: `12.5 Finish APS callback TODOs` |
| `12.6` | 已关闭 | `SyncType` 已按真实合同边界精简，并把本地执行反馈类型拆到内部枚举 | Phase 5 follow-ups: `12.6 Queue-Semantics Guardrail`, `Phase 7 Completion Evidence and 12.6 Real Enum Shrink` |
