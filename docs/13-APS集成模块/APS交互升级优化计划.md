# APS 交互升级优化计划

> 本文档为 APS 系统开发方提供 MES-APS 交互接口规范，MES 侧代码已完成开发，APS 侧需按此文档实现对应接口。

---

## 一、架构概述

### 1.1 交互模式

两个系统完全独立部署，通过 **REST API + JSON** 进行数据交互：

```
┌──────────────────┐                    ┌──────────────────┐
│       MES        │  ── HTTP/JSON ──>  │       APS        │
│  (生产执行系统)   │  <── HTTP/JSON ──  │  (高级排程系统)   │
└──────────────────┘                    └──────────────────┘
```

- **MES → APS**：MES 主动推送主数据和执行反馈（REST POST）
- **APS → MES**：APS 主动回调下发排程结果（REST POST 到 MES 回调端点）
- **认证方式**：HTTP Header `X-API-Key` 携带 API 密钥
- **数据格式**：JSON，UTF-8 编码
- **超时设置**：30 秒
- **重试策略**：最多 3 次，指数退避（5s → 15s → 30s）
- **熔断保护**：连续 5 次失败触发熔断，30 秒后半开

### 1.2 同步方式

| 方式 | 说明 | 适用场景 |
|------|------|---------|
| 事件驱动 | MES 业务操作触发即时推送 | 状态变更、异常事件、物料短缺 |
| 定时轮询 | 每 5 分钟消费队列批量推送 | 非紧急的执行反馈 |
| 手动触发 | 管理员在 MES 界面触发全量同步 | 主数据初始化、数据修复 |
| APS 回调 | APS 排程完成后主动推送到 MES | 排程结果、MRP、产能负荷 |

---

## 二、MES → APS 接口清单（APS 需实现的接收端点）

### 2.1 主数据同步接口

APS 需实现以下 **接收端点**，MES 会按需推送主数据：

#### 2.1.1 工作中心主数据

- **端点**：`POST /api/mes/master-data/work-centers`
- **触发**：手动全量同步 / 工作中心数据变更
- **请求体**：

```json
{
  "data": [
    {
      "workCenterCode": "WC-001",
      "workCenterName": "CNC加工中心1号",
      "workCenterCategory": "MACHINING",
      "businessUnit": "BU01",
      "workCalendar": "CAL-STD",
      "resourceOrder": 1,
      "efficiency": 0.95,
      "resourceCapacity": 480.00,
      "batchQty": 100.00,
      "resourceType": "EQUIPMENT",
      "furnaceResourceType": null,
      "processNoInterrupt": 1,
      "processNoCrossDay": 0,
      "fixedTaktProduction": 0
    }
  ]
}
```

**关键字段说明**：

| 字段 | 类型 | 说明 | APS 用途 |
|------|------|------|---------|
| efficiency | decimal | 效率系数（0~1） | 排程时计算实际产能 |
| resourceCapacity | decimal | 资源能力（分钟/天） | 产能约束 |
| batchQty | decimal | 处理批量 | 批量排程 |
| processNoInterrupt | int | 工序不中断（1=是） | 排程约束 |
| processNoCrossDay | int | 工序不跨天（1=是） | 排程约束 |
| fixedTaktProduction | int | 固定节拍点生产 | 节拍排程 |

#### 2.1.2 工艺路线/工序时间

- **端点**：`POST /api/mes/master-data/process-routes`
- **请求体**：

```json
{
  "data": [
    {
      "processNo": "OP-010",
      "processName": "粗车加工",
      "processCode": "PC-001",
      "processType": "MACHINING",
      "product": "PROD-001",
      "productCategory": "ENGINE",
      "machineModel": "CFM56",
      "workCenterId": 1,
      "workshopArea": "AREA-A",
      "teamId": 5,
      "handleTime": 120.00,
      "disassembleTime": 15.00,
      "installTime": 20.00,
      "needStrip": 0
    }
  ]
}
```

**关键字段说明**：

| 字段 | 类型 | 说明 | APS 用途 |
|------|------|------|---------|
| handleTime | decimal | 处理时间（分钟） | 排程工时计算 |
| disassembleTime | decimal | 拆卸时间（分钟） | 换型时间 |
| installTime | decimal | 安装时间（分钟） | 换型时间 |
| workCenterId | long | 默认工作中心 | 资源约束 |
| teamId | long | 默认班组 | 人力约束 |

#### 2.1.3 制造BOM

- **端点**：`POST /api/mes/master-data/boms`
- **请求体**：

```json
{
  "data": [
    {
      "bomCode": "BOM-2024-001",
      "bomName": "CFM56叶片BOM",
      "productCode": "PROD-001",
      "productName": "CFM56叶片",
      "bomVersion": "V2.0",
      "status": "ACTIVE",
      "effectiveDate": "2024-01-01",
      "expiryDate": null,
      "factoryOrg": "FAC-01",
      "items": [
        {
          "materialCode": "MAT-001",
          "materialName": "钛合金棒料",
          "materialSpec": "Φ50×200mm",
          "quantity": 1.00,
          "lossRate": 5.00,
          "unit": "PCS",
          "supplyType": "PURCHASE",
          "processNo": "OP-010",
          "isKeyPart": 1
        }
      ]
    }
  ]
}
```

#### 2.1.4 物料主数据

- **端点**：`POST /api/mes/master-data/materials`
- **请求体**：

```json
{
  "data": [
    {
      "materialCode": "MAT-001",
      "materialName": "钛合金棒料",
      "materialType": "RAW",
      "baseUnit": "PCS",
      "categoryLevel1": "金属材料",
      "categoryLevel2": "钛合金",
      "productCategory": "ENGINE",
      "machineModel": "CFM56",
      "traceMode": "BATCH"
    }
  ]
}
```

#### 2.1.5 班组信息

- **端点**：`POST /api/mes/master-data/teams`
- **请求体**：

```json
{
  "data": [
    {
      "teamCode": "TEAM-A01",
      "teamName": "甲班一组",
      "orgCode": "ORG-01",
      "orgName": "机加车间",
      "enabled": 1,
      "description": "数控加工甲班"
    }
  ]
}
```

---

### 2.2 执行反馈接口

APS 需实现以下 **接收端点**，MES 在业务事件触发时自动推送：

#### 2.2.1 派工分配结果

- **端点**：`POST /api/mes/feedback/dispatch`
- **触发时机**：MES 完成派工操作
- **请求体**：

```json
{
  "workOrderId": 1001,
  "workOrderTaskId": 2001,
  "orderNo": "ORD-2024-001",
  "processNo": "OP-010",
  "planWorkCenterId": 5,
  "assignType": "DEVICE",
  "assigneeCode": "CNC-001",
  "assigneeName": "CNC加工中心1号",
  "assignedQty": 50.00,
  "status": "ACTIVE",
  "assignedTime": "2024-06-15T08:30:00"
}
```

#### 2.2.2 开工检查结果

- **端点**：`POST /api/mes/feedback/start-check`
- **触发时机**：开工检查完成（特别是 FAILED 时优先级高）
- **请求体**：

```json
{
  "workOrderTaskId": 2001,
  "checkStatus": "FAILED",
  "checkTime": 1718420400000
}
```

> APS 处理建议：当 checkStatus=FAILED 时，表示该工序暂时无法开工，APS 应考虑推迟该工序排程或分配备用资源。

#### 2.2.3 工单约束关系

- **端点**：`POST /api/mes/feedback/constraint`
- **触发时机**：工单创建或约束关系修改
- **请求体**：

```json
{
  "workOrderId": 1001,
  "workOrderNo": "WO-2024-001",
  "constraints": [
    {
      "constraintType": "FINISH_TO_START",
      "relatedWorkOrderId": 1002,
      "relatedTaskId": null,
      "remark": "叶片加工完成后才能进行装配"
    }
  ]
}
```

#### 2.2.4 交班实际产出

- **端点**：`POST /api/mes/feedback/shift-output`
- **触发时机**：交班确认完成
- **请求体**：

```json
{
  "projectName": "CFM56叶片加工",
  "productSerialNo": "SN-001",
  "processContent": "粗车加工",
  "handoverDate": "2024-06-15",
  "handoverShift": "白班",
  "handoverTeamName": "甲班一组",
  "planQty": 50.00,
  "actualQty": 45.00,
  "gapAnalysis": "设备临时维修导致停工30分钟"
}
```

> APS 处理建议：比较 planQty 和 actualQty 的差异，滚动调整后续排程。

#### 2.2.5 物料齐套/短缺

- **端点**：`POST /api/mes/feedback/material-shortage`
- **触发时机**：领料时发现缺料
- **优先级**：高（priority=2）
- **请求体**：

```json
{
  "workOrderId": 1001,
  "workOrderNo": "WO-2024-001",
  "shortageItems": [
    {
      "materialCode": "MAT-001",
      "materialName": "钛合金棒料",
      "requiredQty": 100.00,
      "issuedQty": 60.00,
      "shortageQty": 40.00
    }
  ]
}
```

> APS 处理建议：收到物料短缺信号后，应推迟该工单或调整排程优先级，避免排了但无法执行。

#### 2.2.6 领料进度

- **端点**：`POST /api/mes/feedback/requisition`
- **触发时机**：领料申请状态变更
- **请求体**：

```json
{
  "requisitionNo": "REQ-2024-001",
  "workOrderNo": "WO-2024-001",
  "productCode": "PROD-001",
  "planQty": 100.00,
  "actualQty": 80.00,
  "qualifiedQty": 78.00,
  "status": "PARTIAL_ISSUED"
}
```

#### 2.2.7 供应计划完成度

- **端点**：`POST /api/mes/feedback/supply-progress`
- **触发时机**：供应到货或完工
- **请求体**：

```json
{
  "workOrderId": 1001,
  "workOrderNo": "WO-2024-001",
  "demandPlanNo": "DEM-001",
  "supplyPlanNo": "SUP-001",
  "supplyQty": 100.00,
  "completedQty": 75.00
}
```

#### 2.2.8 工单状态变更（实时）

- **端点**：`POST /api/mes/feedback/status-change`
- **触发时机**：工单状态发生任何变更（即时推送）
- **优先级**：高（priority=2）
- **请求体**：

```json
{
  "workOrderId": 1001,
  "workOrderNo": "WO-2024-001",
  "orderPlanNo": "ORD-2024-001",
  "productCode": "PROD-001",
  "oldStatus": "IN_PROGRESS",
  "newStatus": "COMPLETED",
  "planStartTime": "2024-06-15T08:00:00",
  "planEndTime": "2024-06-15T17:00:00",
  "actualStartTime": "2024-06-15T08:15:00",
  "actualEndTime": "2024-06-15T16:30:00",
  "changeTime": 1718420400000
}
```

#### 2.2.9 工艺变更通知

- **端点**：`POST /api/mes/feedback/process-change`
- **触发时机**：BOM 版本升级、工序时间调整
- **请求体**：

```json
{
  "changeType": "BOM_UPGRADE",
  "entityId": 101,
  "entityCode": "BOM-2024-001",
  "changeTime": 1718420400000
}
```

> changeType 取值：`BOM_UPGRADE`（BOM升级）、`PROCESS_TIME_CHANGED`（工序时间调整）、`PROCESS_ROUTE_CHANGED`（工艺路线变更）

---

## 三、APS → MES 接口清单（APS 需调用的 MES 回调端点）

MES 已实现以下回调端点，APS 排程完成后需调用：

### 3.1 已有回调（维持不变）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/aps/callback/schedule-result` | POST | 排程结果回调 |
| `/aps/callback/request-rejected` | POST | 请求拒绝回调 |

### 3.2 新增回调端点

#### 3.2.1 物料需求计划(MRP)

- **端点**：`POST /aps/callback/mrp-result`
- **调用时机**：APS 完成排程后，生成物料需求计划
- **请求体**：

```json
{
  "requestId": "uuid-xxx",
  "scheduleBatchId": "BATCH-2024-001",
  "items": [
    {
      "workOrderNo": "WO-2024-001",
      "processNo": "OP-010",
      "materialCode": "MAT-001",
      "materialName": "钛合金棒料",
      "requiredQty": 100.00,
      "unit": "PCS",
      "requiredDate": "2024-06-15T06:00:00",
      "priority": 1
    }
  ]
}
```

#### 3.2.2 资源分配计划

- **端点**：`POST /aps/callback/resource-allocation`
- **调用时机**：排程完成后，下发每个工序的资源分配
- **请求体**：

```json
{
  "requestId": "uuid-xxx",
  "scheduleBatchId": "BATCH-2024-001",
  "items": [
    {
      "workOrderNo": "WO-2024-001",
      "processNo": "OP-010",
      "workCenterCode": "WC-001",
      "assignType": "DEVICE",
      "assigneeCode": "CNC-001",
      "assigneeName": "CNC加工中心1号",
      "assignedQty": 50.00,
      "planStartTime": "2024-06-15T08:00:00",
      "planEndTime": "2024-06-15T12:00:00"
    }
  ]
}
```

> MES 处理：自动更新派工任务的计划时间，并创建预分配记录。

#### 3.2.3 排程甘特图数据

- **端点**：`POST /aps/callback/gantt-data`
- **调用时机**：排程完成后
- **请求体**：

```json
{
  "requestId": "uuid-xxx",
  "scheduleBatchId": "BATCH-2024-001",
  "rangeStart": "2024-06-15T00:00:00",
  "rangeEnd": "2024-06-22T00:00:00",
  "tasks": [
    {
      "taskId": "TASK-001",
      "workOrderNo": "WO-2024-001",
      "orderNo": "ORD-2024-001",
      "productCode": "PROD-001",
      "productName": "CFM56叶片",
      "processNo": "OP-010",
      "processName": "粗车加工",
      "resourceCode": "WC-001",
      "resourceName": "CNC加工中心1号",
      "startTime": "2024-06-15T08:00:00",
      "endTime": "2024-06-15T12:00:00",
      "duration": 240,
      "status": "SCHEDULED",
      "priority": 1,
      "predecessors": []
    }
  ]
}
```

#### 3.2.4 产能负荷数据

- **端点**：`POST /aps/callback/capacity-load`
- **调用时机**：排程完成后 / 定时推送
- **请求体**：

```json
{
  "requestId": "uuid-xxx",
  "scheduleBatchId": "BATCH-2024-001",
  "calculatedAt": "2024-06-15T07:00:00",
  "items": [
    {
      "workCenterCode": "WC-001",
      "workCenterName": "CNC加工中心1号",
      "date": "2024-06-15",
      "availableCapacity": 480.00,
      "scheduledCapacity": 420.00,
      "loadRate": 87.50,
      "overloaded": false
    }
  ]
}
```

#### 3.2.5 排程变更通知

- **端点**：`POST /aps/callback/schedule-change`
- **调用时机**：重排或排程调整后
- **请求体**：

```json
{
  "requestId": "uuid-xxx",
  "scheduleBatchId": "BATCH-2024-001",
  "changeReason": "设备故障导致重排",
  "changeTime": "2024-06-15T10:30:00",
  "affectedOrders": [
    {
      "workOrderNo": "WO-2024-001",
      "orderNo": "ORD-2024-001",
      "changeType": "TIME_CHANGED",
      "oldStartTime": "2024-06-15T08:00:00",
      "newStartTime": "2024-06-15T13:00:00",
      "oldEndTime": "2024-06-15T12:00:00",
      "newEndTime": "2024-06-15T17:00:00",
      "remark": "因CNC-001设备故障，推迟到下午"
    }
  ]
}
```

> MES 处理：自动更新工单的 planStartTime 和 planEndTime。对于 changeType=CANCELLED 的工单，MES 仅记录日志不自动取消，需人工确认。

---

## 四、实施计划

### 4.1 阶段一：主数据对接（1~2 周）

| 任务 | APS 侧工作 | MES 状态 |
|------|-----------|---------|
| 工作中心接收 | 实现 `/api/mes/master-data/work-centers` | ✅ 已完成 |
| 工艺路线接收 | 实现 `/api/mes/master-data/process-routes` | ✅ 已完成 |
| 制造BOM接收 | 实现 `/api/mes/master-data/boms` | ✅ 已完成 |
| 物料主数据接收 | 实现 `/api/mes/master-data/materials` | ✅ 已完成 |
| 班组信息接收 | 实现 `/api/mes/master-data/teams` | ✅ 已完成 |
| 数据映射建立 | 建立 MES↔APS 编码映射关系 | ✅ 已有映射框架 |

### 4.2 阶段二：执行反馈对接（2~3 周）

| 任务 | APS 侧工作 | MES 状态 |
|------|-----------|---------|
| 派工结果接收 | 实现 `/api/mes/feedback/dispatch` | ✅ 已完成 |
| 开工检查接收 | 实现 `/api/mes/feedback/start-check` | ✅ 已完成 |
| 工单约束接收 | 实现 `/api/mes/feedback/constraint` | ✅ 已完成 |
| 交班产出接收 | 实现 `/api/mes/feedback/shift-output` | ✅ 已完成 |
| 物料短缺接收 | 实现 `/api/mes/feedback/material-shortage` | ✅ 已完成 |
| 领料进度接收 | 实现 `/api/mes/feedback/requisition` | ✅ 已完成 |
| 供应计划接收 | 实现 `/api/mes/feedback/supply-progress` | ✅ 已完成 |
| 工单状态变更 | 实现 `/api/mes/feedback/status-change` | ✅ 已完成 |
| 工艺变更通知 | 实现 `/api/mes/feedback/process-change` | ✅ 已完成 |

### 4.3 阶段三：APS 下发对接（2~3 周）

| 任务 | APS 侧工作 | MES 状态 |
|------|-----------|---------|
| MRP 结果下发 | 调用 `/aps/callback/mrp-result` | ✅ 已完成 |
| 资源分配下发 | 调用 `/aps/callback/resource-allocation` | ✅ 已完成 |
| 甘特图下发 | 调用 `/aps/callback/gantt-data` | ✅ 已完成 |
| 产能负荷下发 | 调用 `/aps/callback/capacity-load` | ✅ 已完成 |
| 排程变更通知 | 调用 `/aps/callback/schedule-change` | ✅ 已完成 |

### 4.4 阶段四：联调测试（1~2 周）

1. 主数据全量同步验证
2. 执行反馈事件触发验证
3. APS 排程 → 回调 → MES 更新 全链路验证
4. 异常场景测试（网络中断、数据不一致、熔断恢复）
5. 性能压测（批量数据同步场景）

---

## 五、统一响应格式

### 5.1 MES 回调端点响应

```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 5.2 APS 接收端点建议响应

```json
{
  "code": 200,
  "message": "accepted",
  "data": {
    "receivedCount": 15,
    "processedCount": 15
  }
}
```

### 5.3 错误响应

```json
{
  "code": 400,
  "message": "数据校验失败: materialCode 不能为空",
  "data": null
}
```

---

## 六、完整接口清单一览

### MES → APS（共 14 个端点，APS 需实现接收）

| # | 端点 | 方法 | 分类 | 优先级 |
|---|------|------|------|:-----:|
| 1 | `/api/mes/master-data/work-centers` | POST | 主数据 | P1 |
| 2 | `/api/mes/master-data/process-routes` | POST | 主数据 | P1 |
| 3 | `/api/mes/master-data/boms` | POST | 主数据 | P2 |
| 4 | `/api/mes/master-data/materials` | POST | 主数据 | P2 |
| 5 | `/api/mes/master-data/teams` | POST | 主数据 | P2 |
| 6 | `/api/mes/feedback/dispatch` | POST | 执行反馈 | P1 |
| 7 | `/api/mes/feedback/start-check` | POST | 执行反馈 | P1 |
| 8 | `/api/mes/feedback/constraint` | POST | 执行反馈 | P2 |
| 9 | `/api/mes/feedback/shift-output` | POST | 执行反馈 | P2 |
| 10 | `/api/mes/feedback/material-shortage` | POST | 执行反馈 | P1 |
| 11 | `/api/mes/feedback/requisition` | POST | 执行反馈 | P3 |
| 12 | `/api/mes/feedback/supply-progress` | POST | 执行反馈 | P3 |
| 13 | `/api/mes/feedback/status-change` | POST | 执行反馈 | P1 |
| 14 | `/api/mes/feedback/process-change` | POST | 执行反馈 | P3 |

### APS → MES（共 7 个端点，MES 已实现接收）

| # | 端点 | 方法 | 分类 | 状态 |
|---|------|------|------|:----:|
| 1 | `/aps/callback/schedule-result` | POST | 排程结果 | ✅ 已有 |
| 2 | `/aps/callback/request-rejected` | POST | 请求拒绝 | ✅ 已有 |
| 3 | `/aps/callback/mrp-result` | POST | MRP下发 | ✅ 新增 |
| 4 | `/aps/callback/resource-allocation` | POST | 资源分配 | ✅ 新增 |
| 5 | `/aps/callback/gantt-data` | POST | 甘特图 | ✅ 新增 |
| 6 | `/aps/callback/capacity-load` | POST | 产能负荷 | ✅ 新增 |
| 7 | `/aps/callback/schedule-change` | POST | 排程变更 | ✅ 新增 |

---

## 七、注意事项

1. **幂等性**：所有接口都应支持幂等调用（通过 requestId / batchId 去重）
2. **编码映射**：MES 和 APS 的物料编码、工作中心编码可能不同，需通过 `mes_aps_data_mapping` 表维护映射关系
3. **时区**：所有时间字段统一使用 `Asia/Shanghai` 时区，格式 `yyyy-MM-dd'T'HH:mm:ss`
4. **批量大小**：单次推送建议不超过 200 条，超过时分批推送
5. **数据库迁移**：MES 新增了 `V1.15__aps_extended_integration.sql`，包含甘特图缓存、产能负荷、排程变更记录 3 张表
