# MES 性能压测预期基准线报告（X-31 纸上分析）

> 文档版本：v1.0-baseline（预期值）
> 起草人：频道 27（mcp27）
> 起草日期：2026-04-21
> 关联任务：`mes_p2_26_x_31_sentinel_stress`
>
> **注意**：本文档是**纸上分析**。由于当前环境无法启动真实 MES 集群，
> 下列数值是基于代码走查、SQL 执行计划、历史同类系统经验给出的**预期基准线**，
> 并非真实压测产出。后续真跑完成后，请用真实值替换本文档相应章节。

---

## 0. TL;DR（执行摘要）

| 场景 | 预期 P95 | 预期 QPS | 预期触发限流 | 预期失败率 |
|---|---|---|---|---|
| 登录（/auth/login） | 60~120ms | 10 QPS/IP（上限） | ≥ 80% 超阈值被 429 | < 0.5% |
| 工单创建（POST /workorder/work-order） | 150~250ms | 30~50 QPS/实例 | 不触发（未设规则） | < 0.5% |
| 工单分页（GET /workorder/work-order/page） | 80~150ms | 200 QPS/实例（上限） | ≥ 20% 超阈值被 429 | < 0.3% |
| 派工指派（POST /dispatch/task/assign） | 200~350ms | 15~20 QPS/实例 | 不触发 | < 1%（冲突允许） |

**结论**：
- 若真跑结果与此基线偏差 < 20%，视为系统符合预期
- 若偏差 ≥ 50%，优先排查 MySQL 慢查询、连接池、GC 频率

---

## 1. 场景 S1：登录（/auth/login）

### 1.1 预期性能

| 指标 | 预期值 | 依据 |
|---|---|---|
| P50 响应时间 | 40~80ms | `BCrypt.matches` 单次 ~25ms + DB 单表查询 ~5ms + Redis 写 captcha fail counter ~1ms |
| P95 响应时间 | 60~120ms | 预留 GC 与网络抖动 |
| P99 响应时间 | 150~250ms | Druid 连接获取等待最坏 100ms |
| 单 IP 最大通过 QPS | 10（Sentinel ParamFlowRule） | `auth:login` 阈值 |
| 429 比例（50 VU） | ≈ 80%（即 40/50 VU 同时冲击 10 QPS 阈值） | `(50 - 10) / 50 = 80%` |
| 真实失败率 | < 0.5% | 只有 DB/Redis 抖动导致 5xx |

### 1.2 Sentinel 触发临界点

- **恰好命中阈值**：同一 IP 每秒请求数 = 10
- **稳定触发限流**：同一 IP 每秒请求数 ≥ 11
- **完全打满阈值+50% 拦截**：同一 IP 每秒请求数 ≥ 20

### 1.3 兜底建议

- 前端在登录失败后引导用户看验证码（P1-14 验证码），自然降 QPS
- 后端日志告警：`rate(sentinel_blocked{resource="auth:login"}[5m]) > 5 / minute` 说明有爆破迹象，触发阈值告警 → 拉黑 IP 15 分钟

---

## 2. 场景 S2：工单创建（POST /workorder/work-order）

### 2.1 预期性能

| 指标 | 预期值 | 依据 |
|---|---|---|
| P50 响应时间 | 80~120ms | 事务：INSERT mes_work_order + INSERT mes_work_order_status_log + 发 RabbitMQ 消息 ~3ms |
| P95 响应时间 | 150~250ms | 考虑主键索引冲突重试、MQ 发送 confirm 同步等待 |
| P99 响应时间 | 400~500ms | 最坏情况：Druid 池内等待 + MySQL binlog 刷盘 |
| 最大吞吐（10 VU） | 30~50 TPS | 10 VU × (1 / 0.2s sleep) ≈ 50，单机瓶颈在 MySQL 主键/唯一索引写 |
| 真实失败率 | < 0.5% | 唯一键冲突属预期业务异常，不计入 |

### 2.2 瓶颈分析

- 主要瓶颈：MySQL 主表写 + binlog（`sync_binlog=1`）
- 次要瓶颈：RabbitMQ publisher confirm（`publisher-confirm-type=correlated`）
- 如吞吐不达标：
  1. 把 `mes_work_order` 的 insert 改批量（batchInsert）
  2. MQ 发送改异步 `CompletableFuture`
  3. 主键从 `AUTO_INCREMENT` 换 Snowflake 减少 gap 锁

### 2.3 Sentinel 触发临界点

- 当前未设 FlowRule，理论上不会被限流
- 若线上洪水（300+ TPS），应临时加一条 `workorder:create` 单机 100 QPS 规则

---

## 3. 场景 S3：工单分页（GET /workorder/work-order/page）

### 3.1 预期性能

| 指标 | 预期值 | 依据 |
|---|---|---|
| P50 响应时间 | 30~60ms | 覆盖索引 `idx_workorder_tenant_status_time` 下 pageSize=20 的 LIMIT 查询 |
| P95 响应时间 | 80~150ms | 考虑 JOIN 产品表 & 租户过滤 + MyBatis 结果映射 |
| P99 响应时间 | 300~400ms | LIMIT offset 深分页最坏 |
| 最大吞吐（100 VU） | 200 QPS（触发 Sentinel 阈值） | FlowRule `workorder:list` count=200 |
| 429 比例（100 VU, 0.5s sleep） | 20%~30% | 总请求 QPS ≈ 200，超出部分被拦截 |
| 真实失败率 | < 0.3% | 纯读、无写锁 |

### 3.2 Sentinel 触发临界点

- 阈值：单机 200 QPS
- 扩容决策：
  - 若 `rate(http_server_requests_seconds_count{uri="/workorder/work-order/page",status="429"}[5m]) > 10/s` 持续 5 分钟 → 说明阈值已不够
  - 处置：① 先把规则临时提到 300 QPS（通过 Nacos 在线下发）② 观察 MySQL 是否还能撑 → 不行则水平扩容
- 严禁：把 count 直接设到 1000+，会把数据库 QPS 压到爆

### 3.3 深分页风险

- 预期问题：`pageNum ≥ 100` 时 P95 会劣化到 500ms+（LIMIT offset 扫描量 × N）
- 缓解方案：
  1. 业务上提示用户"最多翻 100 页"
  2. 改用游标分页（`id > last_id ORDER BY id LIMIT 20`）

---

## 4. 场景 S4：派工指派（POST /dispatch/task/assign）

### 4.1 预期性能

| 指标 | 预期值 | 依据 |
|---|---|---|
| P50 响应时间 | 120~200ms | 事务：锁 dispatch_task + INSERT assignment + UPDATE task 状态 |
| P95 响应时间 | 200~350ms | 行级锁等待 + 状态日志写入 |
| P99 响应时间 | 500~700ms | 最坏：同 taskId 并发，需要排队 |
| 最大吞吐（5 VU） | 15~20 TPS | 业务不高并发，5 VU × 1 QPS ≈ 5，瓶颈在 row lock |
| 预期"冲突" | < 5% | 5 VU 随机挑选 3 个 taskId，1/3 概率冲突 |

### 4.2 幻读 / 丢失更新验证

- `mes_dispatch_assignment` 有 `task_id + target_id + deleted` 唯一键
- 事务隔离级别 RR + MySQL gap lock → 不会幻读
- 如观察到 `重复指派` 错误，说明唯一键建错或被绕过

---

## 5. Sentinel 规则效果汇总表

| 资源 | 规则类型 | 阈值 | 统计窗口 | 维度 | 场景 |
|---|---|---|---|---|---|
| `auth:login` | ParamFlowRule | 10 | 1s | IP | 登录防爆破 |
| `file:upload` | ParamFlowRule | 5 | 1s | 租户 | 小文件洪水防护 |
| `workorder:list` | FlowRule | 200 | 1s | 单机 | 看板查询保护 |
| `dispatch:task:page` | FlowRule | 200 | 1s | 单机 | 派工列表保护 |
| `any:export` | ParamFlowRule | 1 | 1s | 租户 | 报表爆推防护 |

### 5.1 规则调整路径

- **dev**：改代码 `SentinelRuleInitializer.loadDefaultRules()` 重启生效
- **prod**：Nacos 配置中心推送（`mes-sentinel-flow.json` / `mes-sentinel-param-flow.json`），无需重启
- **回滚**：`mes.sentinel.enabled=false` 全局关闭，请求落到老 `RateLimitFilter`（Redis 滑动窗口）兜底

---

## 6. JVM / 资源基准

| 指标 | 预期值 | 告警阈值 |
|---|---|---|
| CPU 使用率（压测期间） | 40~60% | ≥ 85% |
| 堆内存（-Xmx4g） | 1.5~2.5 GB | ≥ 3.5 GB |
| Young GC 频率 | 1 次 / 10s | > 1 次 / 2s |
| Full GC 频率 | 0 次 / 压测周期 | ≥ 1 次 |
| Druid `active` 连接数 | 10~30 | ≥ 80（配置 max-active=100） |
| Redis 连接数 | 5~15 | ≥ 40（配置 max-active=50） |

---

## 7. 下一轮建议

1. **S5 文件上传压测**：补齐 `file-upload.js`，验证每租户 5 QPS
2. **S6 报表导出压测**：验证每租户 1 QPS（`any:export`）
3. **混合场景压测**：50% 登录 + 40% 查询 + 10% 写，贴近真实业务
4. **长时压测**：连续 4 小时，观察内存泄漏、连接池耗尽
5. **故障注入**：Chaos Mesh 模拟 MySQL 主节点宕机，验证熔断切读库

---

## 8. 报告模板（真跑后填写）

真跑完成后，请按下表填写真实数据并更新本文档标题为 `perf-test-report-baseline.md → perf-test-report-2026xxxx.md`：

```
| 场景 | 实测 P95 | 实测最大 QPS | 实测 429 比例 | 实测失败率 | 偏差 vs 基准 |
|---|---|---|---|---|---|
| S1 登录 | ?? ms | ?? QPS | ??% | ??% | ±??% |
| S2 工单创建 | ?? ms | ?? TPS | - | ??% | ±??% |
| S3 工单分页 | ?? ms | ?? QPS | ??% | ??% | ±??% |
| S4 派工指派 | ?? ms | ?? TPS | - | ??% | ±??% |
```
