# RabbitMQ Classic Mirrored → Quorum Queue 迁移手册

> 文档编号：M9-P3-11  
> 发布时间：2026-04-24  
> 作者：mcp11 / MES 架构组  
> 适用版本：RabbitMQ 3.13.x 三节点集群 + Spring AMQP 2.7.x

---

## 0. 背景与动机

| 维度 | Classic Mirrored Queue (M8) | Quorum Queue (M9) |
| --- | --- | --- |
| 一致性协议 | 主从异步/半同步复制 | Raft，强一致法定多数 |
| 故障恢复 | 依赖 `ha-sync-mode`；主节点宕机可能丢 in-flight | Leader 选举 ≤ 5s，提交日志永不回滚 |
| 性能 | 纯内存快，但流控脆弱 | 基于 WAL 落盘，吞吐略低但更稳定 |
| 已在 3.13 标记 | **deprecated**（4.0 将移除） | GA，官方推荐 |
| 限制 | 支持 x-max-priority / x-queue-mode | 不支持 x-max-priority / 不支持 exclusive |

MES 当前业务队列（order / dispatch / material / audit / aps-callback 等）对**消息不丢失**的要求高于 latency，符合 Quorum 适用场景。故本次在 M8 已部署的 3 节点 RabbitMQ 集群上进行 **原地切换**。

---

## 1. 改动清单

### 1.1 代码层（mes-backend）

文件：`mes-backend/mes-framework/src/main/java/com/mes/framework/config/RabbitMQConfig.java`

| 队列 | x-queue-type | 备注 |
| --- | --- | --- |
| `mes.aps.sync` | quorum | 业务主路径 |
| `mes.workorder.events` | quorum | 工单事件 |
| `mes.inventory.events` | quorum | 库存事件 |
| `mes.quality.events` | quorum | 质量事件 |
| `mes.aps.sync.dlq` | quorum | DLQ 本身也 quorum |
| `mes.workorder.events.dlq` | quorum | 同上 |

> 如将来新增**一次性通知 / 广播类**队列（可以容忍丢消息），应显式声明  
> `.withArgument("x-queue-type", "classic")`，避免被 default_queue_type 自动提升为 quorum。

### 1.2 服务器端

- `rabbitmq/definitions.json`  
  - `ha-all` 改为 `ha-classic-only`，`apply-to: classic_queues`，防止对 quorum 队列生效  
  - 新增 `quorum-defaults`：`delivery-limit=20`、`max-in-memory-length=10000`
- `rabbitmq/rabbitmq.conf`（新增）  
  - `default_queue_type = quorum`（客户端未指定时兜底）  
  - `cluster_partition_handling = autoheal`  
  - `quorum_commands_soft_limit = 32`  
  - 水位与 Prometheus 端口设置

### 1.3 编排

- `docker-compose.ha.yml`  
  - rabbitmq-1/2/3 挂载 `./rabbitmq/rabbitmq.conf → /bitnami/rabbitmq/conf/custom.conf`  
  - bitnami 镜像会将 `custom.conf` merge 到最终 `rabbitmq.conf`

---

## 2. 零停机迁移步骤（推荐）

> 适用于生产环境已有长期运行的 classic mirrored queue。  
> 以 `mes.aps.sync` 为例，其它队列按相同模式顺序执行。

### 2.1 预检（T-30min）

```bash
# 1. 集群 3 节点全绿
docker exec mes-rabbitmq-1 rabbitmq-diagnostics cluster_status

# 2. 当前队列 backlog（用于评估切换窗口）
docker exec mes-rabbitmq-1 rabbitmqctl list_queues name messages state policy \
  -p mes | tee /tmp/queues-before.txt

# 3. Consumer 列表
docker exec mes-rabbitmq-1 rabbitmqctl list_consumers -p mes
```

### 2.2 发布 `rabbitmq.conf` + definitions（T0）

```bash
cd /opt/mes
git pull   # 拉到含本次改动的 commit
docker compose -f docker-compose.ha.yml up -d rabbitmq-1 rabbitmq-2 rabbitmq-3
# custom.conf 生效需要 restart，上述 up -d 会触发有状态重启
```

**预期**：
- 原 `ha-all` policy 已被替换为 `ha-classic-only`（仅作用 classic）；
- 原 classic mirrored 队列继续以 classic 形式运行，消息不丢。

### 2.3 停老队列 consumer（T+2min）

```bash
# 把 mes-backend-1/2 临时摘掉 consumer（prefetch=0 停拉，不停连接）
# 通过 actuator 关闭监听器（若未暴露，可 docker compose stop mes-backend-1 mes-backend-2）
curl -X POST http://mes-backend-1:9090/api/actuator/rabbit/listeners/stop
curl -X POST http://mes-backend-2:9090/api/actuator/rabbit/listeners/stop
```

> 若 Spring Boot 未暴露自定义 endpoint，可直接停整个服务；前提是此时前端已切到维护页（或有旁路）。

### 2.4 声明新 Quorum 队列（T+5min）

使用 `_quorum` 后缀先**并行**创建新队列，发布/订阅两侧都改为它，对比稳定后再删老队列；也可以**原名替换**（需要临时改名）。推荐并行方案：

```bash
# 临时方式：通过 management API 直接声明
docker exec mes-rabbitmq-1 rabbitmqadmin -u mes -p "$RABBITMQ_PASSWORD" -V mes \
  declare queue name=mes.aps.sync.quorum \
  durable=true \
  arguments='{"x-queue-type":"quorum","x-dead-letter-exchange":"mes.dlx","x-dead-letter-routing-key":"dlq.aps.sync"}'

# 绑定
docker exec mes-rabbitmq-1 rabbitmqadmin -u mes -p "$RABBITMQ_PASSWORD" -V mes \
  declare binding source=mes.topic destination=mes.aps.sync.quorum routing_key='aps.sync.#'
```

或者直接重启 mes-backend（已修改 RabbitMQConfig）让 Spring AMQP `RabbitAdmin` 自动声明同名 quorum queue —— 但前提是**老队列先删除**，否则 Spring 会因 `x-queue-type` 不匹配报 `PRECONDITION_FAILED`。

推荐顺序：
1. 停 consumer（2.3）
2. 等老队列 `messages=0`（有积压时用 shovel 或 `rabbitmqctl export_definitions` + 手动处理）
3. 删老队列
4. 启 mes-backend，Spring 自动声明新 quorum 队列

### 2.5 迁移 in-flight 消息（若有积压）

方案 A：**shovel 插件**一次性搬运

```bash
docker exec mes-rabbitmq-1 rabbitmq-plugins enable rabbitmq_shovel rabbitmq_shovel_management

docker exec mes-rabbitmq-1 rabbitmqctl set_parameter -p mes shovel migrate-aps-sync \
  '{"src-protocol":"amqp091","src-uri":"amqp:///mes","src-queue":"mes.aps.sync",
    "dest-protocol":"amqp091","dest-uri":"amqp:///mes","dest-queue":"mes.aps.sync.quorum",
    "ack-mode":"on-confirm","src-delete-after":"queue-length"}'
```

方案 B：**暴力丢弃**（仅允许在明确业务可补偿时使用）

```bash
docker exec mes-rabbitmq-1 rabbitmqctl purge_queue -p mes mes.aps.sync
```

### 2.6 删除老队列（T+15min）

```bash
docker exec mes-rabbitmq-1 rabbitmqctl delete_queue -p mes mes.aps.sync
```

### 2.7 启动新 consumer（T+16min）

```bash
# 若采用「停服务」方案：
docker compose -f docker-compose.ha.yml up -d mes-backend-1 mes-backend-2
# 若用 actuator 停监听器：
curl -X POST http://mes-backend-1:9090/api/actuator/rabbit/listeners/start
curl -X POST http://mes-backend-2:9090/api/actuator/rabbit/listeners/start
```

### 2.8 验证（T+20min）

```bash
# 1. 队列类型=quorum，leader + 2 follower
docker exec mes-rabbitmq-1 rabbitmqctl list_queues \
  name type leader members -p mes

# 期望输出
# mes.aps.sync  quorum  rabbit@rabbitmq-1  [rabbit@rabbitmq-1,rabbit@rabbitmq-2,rabbit@rabbitmq-3]

# 2. 发一条测试消息并确认消费
curl -X POST http://mes-backend-1:9090/api/admin/rabbit/probe \
  -H 'Content-Type: application/json' \
  -d '{"queue":"mes.aps.sync","payload":{"probe":"m9-p3-11"}}'

# 3. Prometheus 指标
curl -s http://localhost:15692/metrics | grep rabbitmq_queue_messages | head
```

---

## 3. 回滚步骤

**前置条件**：发现问题且必须在 30min 内恢复。

1. 停 mes-backend：`docker compose stop mes-backend-1 mes-backend-2`
2. 删新 quorum 队列：
   ```bash
   for q in mes.aps.sync mes.workorder.events mes.inventory.events \
            mes.quality.events mes.aps.sync.dlq mes.workorder.events.dlq; do
     docker exec mes-rabbitmq-1 rabbitmqctl delete_queue -p mes $q
   done
   ```
3. 恢复代码：`git revert <commit-sha>` → 部署回 M8 版本 RabbitMQConfig
4. 恢复 definitions：
   ```bash
   git checkout HEAD~1 -- rabbitmq/definitions.json
   docker cp rabbitmq/definitions.json mes-rabbitmq-1:/opt/bitnami/rabbitmq/etc/rabbitmq/definitions.json
   docker exec mes-rabbitmq-1 rabbitmqctl import_definitions /opt/bitnami/rabbitmq/etc/rabbitmq/definitions.json
   ```
5. 重启 mes-backend：Spring 会以 classic 模式重新声明队列

> ⚠️ 注意：一旦 quorum 队列里已有**业务消息**被消费，回滚到 classic 时那部分消息将无法追溯——只能依赖应用层 `mq_outbox` 表（M9-P2-02 已引入）补偿重放。

---

## 4. 不兼容项与已知风险

| 项 | 说明 | 处理 |
| --- | --- | --- |
| `x-max-priority` | quorum 不支持优先级 | 本项目未使用；业务若需优先级改用独立高/低优先级队列 |
| `x-queue-mode=lazy` | quorum 内建类似策略（max-in-memory-length） | 通过 policy `quorum-defaults` 控制 |
| `exclusive`/`auto-delete` | quorum 不支持 exclusive | 回复队列仍是 classic auto-delete（Spring 默认行为） |
| 大量小队列（> 1w） | quorum 对 Raft 元数据开销敏感 | 合并队列，或继续使用 classic |
| `rabbitmqctl sync_queue` | 不适用于 quorum | 使用 `rabbitmqctl check_if_node_is_mirror_sync_critical`（已无意义）或 quorum 专有的 `queue group_info` |

---

## 5. Smoke 验证脚本片段

```bash
#!/usr/bin/env bash
set -euo pipefail
EXPECT_QUORUM=(mes.aps.sync mes.workorder.events mes.inventory.events \
               mes.quality.events mes.aps.sync.dlq mes.workorder.events.dlq)

OUT=$(docker exec mes-rabbitmq-1 rabbitmqctl list_queues -p mes name type --no-table-headers)
for q in "${EXPECT_QUORUM[@]}"; do
  TYPE=$(echo "$OUT" | awk -v q="$q" '$1==q{print $2}')
  [[ "$TYPE" == "quorum" ]] || { echo "FAIL: $q is $TYPE, expect quorum"; exit 1; }
done
echo "ALL QUORUM ✅"
```

---

**负责人**：mcp11 / MES 平台组  
**Runbook 归档**：`docs/operations/` 目录，随 M9 上线一同合并到 main 分支
