# MES 性能压测脚本（X-31）

本目录包含基于 [k6](https://k6.io/) 的性能压测脚本，覆盖 MES 四条核心链路：

| 脚本 | 业务场景 | 并发 | 持续 | 阈值（SLA） |
|---|---|---|---|---|
| `auth-login.js` | 登录（+Sentinel IP 限流验证） | 1 → 50 VU | 5 分钟 | P95 < 500ms、失败率 < 1% |
| `workorder-create.js` | 工单创建（写入） | 10 VU | 3 分钟 | P95 < 500ms、失败率 < 1% |
| `workorder-page.js` | 工单分页（+Sentinel 单机 200 QPS 验证） | 100 VU | 5 分钟 | P95 < 500ms、失败率 < 1% |
| `dispatch-assign.js` | 派工指派（事务写入） | 5 VU | 3 分钟 | P95 < 500ms、失败率 < 1% |

## 前置条件

1. 安装 k6（无 GUI，无 JRE，绿色单可执行）：
    - Windows：`choco install k6` 或从 https://k6.io/docs/get-started/installation/ 下载
    - Linux：`sudo apt-get install k6`
    - Docker：`docker run --rm -i grafana/k6 run - <auth-login.js`

2. 运行 MES 后端至少 1 个实例（单机 / k8s 1 副本都可），确认 `/api/actuator/prometheus` 可访问。

3. 提前登录获取 JWT：
   ```bash
   curl -X POST http://localhost:9091/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
   ```
   从响应中取 `data.accessToken`，后续通过 `-e TOKEN=xxx` 传入。

## 运行

```bash
# 登录压测
k6 run scripts/perf/auth-login.js

# 工单创建（需 token）
k6 run -e TOKEN=eyJhbGciOi... scripts/perf/workorder-create.js

# 工单分页（需 token）
k6 run -e TOKEN=eyJhbGciOi... scripts/perf/workorder-page.js

# 派工指派（需 token + 合法 taskId）
k6 run -e TOKEN=eyJhbGciOi... -e TASK_IDS=2001,2002 scripts/perf/dispatch-assign.js
```

## 输出报告

每个脚本运行完毕会输出终端摘要；若需要导出 JSON/HTML：

```bash
k6 run --out json=result.json --summary-export=summary.json scripts/perf/auth-login.js
```

## 关键点

- **Sentinel 验证**：登录脚本与工单分页脚本会统计 `sentinel_blocked` 计数器，若为 0 则说明限流未生效。
- **基线报告**：详见 `docs/operations/perf-test-report-baseline.md`。
- **阈值失败**：k6 的 `thresholds` 在任一阈值未达标时会退出 1，CI 可据此 fail build。

## 不能在本地跑压测怎么办？

- 先依据 `docs/operations/perf-test-plan.md` 与 `docs/operations/perf-test-report-baseline.md` 做纸上分析
- 上预发时，将脚本随代码一起发布到 CI pipeline，由 Jenkins/GitLab Runner 在独立 agent 上运行
