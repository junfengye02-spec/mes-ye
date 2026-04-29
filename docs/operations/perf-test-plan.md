# MES 性能压测计划（X-31）

> 文档版本：v1.0
> 起草人：频道 27（mcp27）
> 起草日期：2026-04-21
> 关联任务：`mes_p2_26_x_31_sentinel_stress`
> 关联脚本目录：`scripts/perf/`

---

## 1. 目标与范围

### 1.1 目标

- **验证 P2-26 Sentinel 限流规则**：核心端点在达到阈值时正确返回 HTTP 429 + `R.fail(429, "请求太快，稍后再试")`
- **建立 MES 后端核心链路性能基线**：为后续容量规划、扩容决策提供依据
- **给出 CI/CD 可回归的 SLA 阈值**：通过 k6 `thresholds` 让性能回退自动失败

### 1.2 范围

本轮压测覆盖 4 条**最常被点**的核心 URL：

| 编号 | 接口 | 场景 | 脚本 |
|---|---|---|---|
| S1 | `POST /auth/login` | 用户登录 | `auth-login.js` |
| S2 | `POST /workorder/work-order` | 工单创建 | `workorder-create.js` |
| S3 | `GET  /workorder/work-order/page` | 工单分页（看板最高频） | `workorder-page.js` |
| S4 | `POST /dispatch/task/assign` | 派工指派 | `dispatch-assign.js` |

**不在本轮范围**：
- 文件上传、导出（S5/S6）—— 预留给下一轮
- 跨租户混合压测 —— 需要先完成 P2-27 租户隔离联调
- APS 对外调用压测 —— 受 APS 环境限制

---

## 2. 环境与前置

### 2.1 推荐测试环境规格

| 组件 | 规格 | 备注 |
|---|---|---|
| 压测机（k6 runner） | 4C8G Linux，1Gbps | 避免本机自压导致 CPU 争用 |
| MES 后端 | 8C16G × 1 实例 | 与生产最小规模一致 |
| MySQL | 8.0.33，8C16G，SSD | `max_connections=300` |
| Redis | 7.x，单机 2C4G | Sentinel 内部滑动窗口也用 |
| 网络 | 压测机 ↔ 后端 < 1ms 延迟 | 不跨公网 |

### 2.2 前置数据

- 系统至少有 **10,000 条工单** + **3,000 条派工单**（便于分页查询扫页）
- 至少 5 个租户、每个租户 20 个账号
- 准备 200 个可用的 `task_id` 供派工压测复用

### 2.3 监控准备

- Grafana 打开 `MES Sentinel Rate Limit & Circuit Breaker` 面板（`monitoring/grafana/dashboards/mes-sentinel.json`）
- Actuator `/actuator/prometheus` 端点开放
- MySQL 开启 `slow_query_log`，阈值 100ms
- Redis 开启 `slowlog get 10` 定时抓取

---

## 3. 压测场景详细说明

### 3.1 S1 登录接口（`auth-login.js`）

| 项 | 值 |
|---|---|
| 并发模型 | ramp-up：`0 → 50 VU / 60s`，保持 3 分钟，ramp-down `→ 0 / 60s` |
| 总请求量预估 | 约 12,000 次（限流前） |
| Sentinel 规则 | `auth:login` 每 IP 10 QPS（ParamFlowRule paramIdx=0） |
| 预期行为 | 50 VU × 1 QPS = 50 QPS 超过 10 QPS，约 80% 被 429 拦截 |
| 验证点 | ① P95 < 500ms（200 请求）② 429 数量 > 0 ③ `sentinel_blocked` 计数器 > 0 |
| 失败判据 | 5xx 比率 ≥ 1% 或 P95 ≥ 500ms |

### 3.2 S2 工单创建（`workorder-create.js`）

| 项 | 值 |
|---|---|
| 并发模型 | ramp-up：`0 → 10 VU / 30s`，保持 3 分钟 |
| 总请求量预估 | 约 6,000 次 |
| Sentinel 规则 | 未设（写入链路由 DB 自然限速） |
| 预期行为 | 工单编号唯一 → 100% 成功 |
| 验证点 | ① P95 < 500ms ② 失败率 < 1% ③ MySQL `mes_work_order` 连接池不打满 |
| 失败判据 | 失败率 ≥ 1% 或 P95 ≥ 500ms |

### 3.3 S3 工单分页（`workorder-page.js`）

| 项 | 值 |
|---|---|
| 并发模型 | ramp-up：`0 → 100 VU / 60s`，保持 5 分钟 |
| 总请求量预估 | 约 60,000 次 |
| Sentinel 规则 | `workorder:list` 单机 200 QPS（FlowRule） |
| 预期行为 | 100 VU × 2 QPS = 200 QPS，恰好命中阈值，触发 10%~20% 429 |
| 验证点 | ① P95 < 500ms（200 请求）② MySQL 命中覆盖索引（`idx_workorder_status_tenant`） |
| 失败判据 | 5xx 比率 ≥ 1% 或 P95 ≥ 500ms |

### 3.4 S4 派工指派（`dispatch-assign.js`）

| 项 | 值 |
|---|---|
| 并发模型 | ramp-up：`0 → 5 VU / 30s`，保持 3 分钟 |
| 总请求量预估 | 约 900 次 |
| Sentinel 规则 | 未设（低并发业务） |
| 预期行为 | 同 taskId 可能冲突，409 属合法 |
| 验证点 | ① P95 < 500ms ② `mes_dispatch_assignment` 无幻读/丢失更新 |
| 失败判据 | 5xx 比率 ≥ 1% |

---

## 4. 运行步骤

```bash
# 1. 准备 token（复用 admin）
TOKEN=$(curl -s -X POST http://localhost:9091/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')

# 2. 顺序跑（避免相互干扰）
k6 run -e BASE_URL=http://localhost:9091/api scripts/perf/auth-login.js
k6 run -e BASE_URL=http://localhost:9091/api -e TOKEN=$TOKEN scripts/perf/workorder-page.js
k6 run -e BASE_URL=http://localhost:9091/api -e TOKEN=$TOKEN scripts/perf/workorder-create.js
k6 run -e BASE_URL=http://localhost:9091/api -e TOKEN=$TOKEN \
       -e TASK_IDS=2001,2002,2003 scripts/perf/dispatch-assign.js

# 3. 导出 JSON 结果
k6 run --out json=logs/perf-$(date +%Y%m%d-%H%M%S).json scripts/perf/workorder-page.js
```

---

## 5. 验收标准

| 编号 | 指标 | 标准 | 实测方式 |
|---|---|---|---|
| A1 | P95 响应时间（200 请求） | < 500ms | k6 `http_req_duration{status:200}` |
| A2 | 真实失败率（5xx / 超时） | < 1% | k6 `http_req_failed{status:5xx}` |
| A3 | Sentinel 规则生效 | S1/S3 场景能看到 429 且 429 比例与阈值预期吻合 | k6 自定义 counter + Grafana 面板 |
| A4 | MySQL 连接池 | Druid `active ≤ max-active × 80%` | Druid 监控页 `/druid/` |
| A5 | Redis 慢查询 | `slowlog` ≤ 10ms | `redis-cli slowlog get 10` |
| A6 | JVM GC | Full GC ≤ 1 次 / 压测周期 | Grafana JVM 面板 |

任意一条未达标即视为本轮压测失败，需修复后重跑。

---

## 6. 无法真跑时的替代方案

本环境当前**无法接入真实 MES 集群**，因此本轮交付的是"纸上分析 + 可运行脚本"。
- 脚本代码已完成，可在任何具备 k6 + 后端实例的环境中直接跑
- 预期数值见 `perf-test-report-baseline.md`（预期基准线）
- 上预发后由运维同学按第 4 节步骤执行，产出真实报告后替换 `perf-test-report-baseline.md`

---

## 7. 附录：k6 thresholds 与 CI 联动

所有脚本都设置了 `thresholds`，当阈值未达标时 k6 退出码 = 1。
CI 示例（GitLab Runner）：

```yaml
perf-test:
  stage: test
  image: grafana/k6:latest
  script:
    - k6 run scripts/perf/auth-login.js
    - k6 run -e TOKEN=$PERF_TOKEN scripts/perf/workorder-page.js
  allow_failure: false
```
