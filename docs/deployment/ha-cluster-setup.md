# MES HA 集群部署指南（Nacos / RabbitMQ / mes-gateway 去单点）

> 版本：P1-16  
> 适用文件：`docker-compose.ha.yml` + `nginx/gateway-lb.conf` + `rabbitmq/definitions.json`  
> 目标读者：运维、SRE、平台工程师

---

## 1. 背景与目标

原 `docker-compose.ha.yml` 虽然已经做了 MySQL 主从、Redis Sentinel，但 **Nacos、RabbitMQ、mes-gateway** 仍然是单点，一旦宕机会导致整个系统不可用：

| 组件 | 原状态 | 单点风险 |
|---|---|---|
| Nacos | 1 节点 standalone | 注册/配置全挂 |
| RabbitMQ | 1 节点 | 消息丢失、异步任务挂 |
| mes-gateway | 0 实例（HA 模式直接走 backend × 2） | 切微服务后全挂 |

P1-16 的目标是把这 3 个关键组件改成 **至少 2N+1 的奇数副本集群**，达到「任挂 1 台系统仍可用」的 HA 基线。

---

## 2. 整体拓扑

```
                    ┌─────────────────────────────────────────────┐
                    │             客户端 / 浏览器                  │
                    └──────────────────┬──────────────────────────┘
                                       │
                                  :80  │  HTTPS (生产)
                                       ▼
                    ┌─────────────────────────────────────────────┐
                    │   mes-frontend (nginx，静态站 + /api 反代)  │
                    └──────────────────┬──────────────────────────┘
                                       │ /api/**
                                  :8080│
                                       ▼
                    ┌─────────────────────────────────────────────┐
                    │   nginx-gateway-lb  (least_conn + passive   │
                    │                      health check)          │
                    └────────────┬──────────────┬────────────────┘
                                 │              │
                                 ▼              ▼
                       ┌─────────────┐ ┌─────────────┐
                       │ mes-gateway │ │ mes-gateway │    <-- 无状态，JWT
                       │     -1      │ │     -2      │        不需要 sticky
                       └──────┬──────┘ └──────┬──────┘
                              │               │
                              ▼               ▼
          ┌────────────────────────────────────────────────┐
          │           Nacos 注册中心 / 配置中心              │
          │ ┌─────────┐  ┌─────────┐  ┌─────────┐           │
          │ │ nacos-1 │─│ nacos-2 │─│ nacos-3 │  (raft)     │
          │ └────┬────┘  └────┬────┘  └────┬────┘           │
          │      └───────┬────┴────┬───────┘                 │
          │              ▼         ▼                         │
          │       MySQL(nacos_config) 表，3 节点共享         │
          └────────────────────────────────────────────────┘

          ┌────────────────────────────────────────────────┐
          │                RabbitMQ 3 节点集群               │
          │ ┌──────────┐  ┌──────────┐  ┌──────────┐        │
          │ │rabbitmq-1│─│rabbitmq-2│─│rabbitmq-3│  (mirror)│
          │ │  (disc)  │  │  (disc)  │  │  (disc)  │        │
          │ └────┬─────┘  └────┬─────┘  └────┬─────┘        │
          │      └─── policy: ha-mode=all ───┘              │
          └────────────────────────────────────────────────┘

          ┌────────────────────────────────────────────────┐
          │               MySQL 主从 / Redis Sentinel       │
          │  mysql-primary ─┬─→ mysql-replica               │
          │                 ▼                                │
          │  redis-master ─┬─ redis-slave-1 / slave-2       │
          │                ▼                                 │
          │        redis-sentinel-1/2/3 (majority quorum)    │
          └────────────────────────────────────────────────┘
```

---

## 3. Nacos 3 节点集群

### 3.1 拓扑

```
        ┌─────────┐       ┌─────────┐       ┌─────────┐
        │ nacos-1 │◀──────▶ nacos-2 │◀──────▶ nacos-3 │
        │ :8848   │       │ :8848   │       │ :8848   │
        └────┬────┘       └────┬────┘       └────┬────┘
             │                 │                 │
             └──────┬──────────┴──────┬──────────┘
                    ▼                 ▼
             mysql-primary (nacos_config 库)
```

- **集群发现**：环境变量 `NACOS_SERVERS=nacos-1:8848 nacos-2:8848 nacos-3:8848`（空格分隔）。
- **数据持久化**：`SPRING_DATASOURCE_PLATFORM=mysql`，3 节点共享 `nacos_config` 库。
- **对外端口映射**：nacos-1→8848，nacos-2→8849，nacos-3→8850。

### 3.2 关键环境变量（必须显式设置）

```bash
# .env
MYSQL_ROOT_PASSWORD=<生产强密码>
MYSQL_REPLICATION_PASSWORD=<另一份强密码>

NACOS_AUTH_TOKEN=<Base64(≥32 字节随机)>
# 生成：openssl rand -base64 48
NACOS_AUTH_IDENTITY_KEY=serverIdentity
NACOS_AUTH_IDENTITY_VALUE=mes-nacos-security
NACOS_USERNAME=nacos
NACOS_PASSWORD=<生产强密码，≥16 字符>
```

### 3.3 客户端连接（业务服务）

业务 Pod / 容器通过 **逗号分隔的多地址** 连接：

```properties
NACOS_SERVER_ADDR=nacos-1:8848,nacos-2:8848,nacos-3:8848
SPRING_CLOUD_NACOS_DISCOVERY_USERNAME=nacos
SPRING_CLOUD_NACOS_DISCOVERY_PASSWORD=<NACOS_PASSWORD>
```

Nacos Java 客户端会：
- 启动时从列表取一个节点建立长连接；
- 节点失联时内部 `UpdateTask` 会切到下一个 healthy 节点；
- 服务列表通过 gRPC 推拉结合维护，RTO ≤ 15 秒。

### 3.4 自动故障剔除

- Nacos 节点间通过 **Jraft** 维护 leader/选举；Leader 挂后 10 秒内选出新 leader。
- 客户端按 `Nacos ServerListManager` 的定时任务（默认 30 秒）探测 `/nacos/health` 剔除失效节点。
- MySQL 的 `nacos_config.instance` 表作为兜底，保证任何节点读到的是同一份视图。

### 3.5 生产注意

- **3 节点是最少副本数**：Raft 要求 `f + 1` 副本才能容忍 `f` 故障。5 节点可容忍 2 节点故障。
- **避免跨 AZ 裂脑**：把 3 节点分布在至少 2 个 AZ（如 3-AZ 部署 1/1/1）。
- **不要随便删除 nacos_config 表**：即使全部容器被重建，只要 MySQL 还在，数据仍可恢复。

---

## 4. RabbitMQ 3 节点集群 + 镜像队列

### 4.1 拓扑

```
        ┌────────────┐     ┌────────────┐     ┌────────────┐
        │rabbitmq-1  │◀───▶│rabbitmq-2  │◀───▶│rabbitmq-3  │
        │ disc node  │     │ disc node  │     │ disc node  │
        │ seed       │     │ joiner     │     │ joiner     │
        └──────┬─────┘     └──────┬─────┘     └──────┬─────┘
               │                  │                  │
               └──────policy: ha-all (ha-mode=all)───┘
```

- **镜像（Classic Mirrored Queue）**：`policy` 匹配所有 vhost=`mes` 下的 queue，`ha-mode: all` 表示每个 queue 在所有节点都有副本。
- **Erlang Cookie**：所有节点共享 `RABBITMQ_ERLANG_COOKIE` 环境变量；**绝不能**把 cookie 写入仓库。
- **Peer Discovery**：`bitnami/rabbitmq` 镜像支持 `classic_config`，通过 `RABBITMQ_CLUSTER_NODE_NAME=rabbit@rabbitmq-1` 让 2/3 自动 `join_cluster`。

### 4.2 关键环境变量

```bash
# .env
RABBITMQ_ERLANG_COOKIE=<≥32 字符随机字符串>
# 生成：openssl rand -hex 32
RABBITMQ_PASSWORD=<生产强密码>
```

### 4.3 客户端连接（业务服务）

```yaml
spring:
  rabbitmq:
    addresses: rabbitmq-1:5672,rabbitmq-2:5672,rabbitmq-3:5672
    username: mes
    password: ${RABBITMQ_PASSWORD}
    virtual-host: mes
    cache:
      connection:
        mode: CHANNEL      # 单连接多 channel，不要切 CONNECTION
    template:
      retry:
        enabled: true      # 生产侧重试
        max-attempts: 3
    listener:
      simple:
        retry:
          enabled: true    # 消费侧重试
```

### 4.4 Policy 自动注入

`rabbitmq/definitions.json` 在启动时由 `RABBITMQ_LOAD_DEFINITIONS=yes` 自动加载，定义了：

```json
{
  "vhost": "mes",
  "name": "ha-all",
  "pattern": ".*",
  "apply-to": "all",
  "definition": {
    "ha-mode": "all",
    "ha-sync-mode": "automatic",
    "ha-sync-batch-size": 256
  }
}
```

> **注意**：3.13 开始 RabbitMQ 官方推荐把新队列切到 **Quorum Queue**（基于 Raft），比 Classic Mirrored Queue 更可靠。但 Classic Mirrored 在 3.13 仍可用，业务 DTO 完全兼容。生产切换 Quorum Queue 需要应用侧声明时显式指定 `x-queue-type: quorum`（建议 P2 迭代）。

### 4.5 自动故障剔除

- 客户端（Spring AMQP）通过 `RabbitTemplate.retry` 和 `SimpleMessageListenerContainer.retry` 在 Connection Loss 时自动重连到 `addresses` 里的下一个节点。
- `connection-timeout=5s` + `requested-heartbeat=30s` 保证故障感知在 30~60 秒内。
- 服务端镜像队列的自动同步（automatic sync）确保新 master 接管时不丢消息（已 ack 的可能有重复消费，消费端需幂等）。

### 4.6 生产注意

- **镜像队列会翻倍带宽消耗**：每条 publish 都会复制到所有节点。队列多、体量大时建议 `ha-mode: exactly`，副本数 2 即可（平衡带宽）。
- **磁盘节点 vs 内存节点**：3 个磁盘节点最稳；若为了性能加纯内存节点，必须保证任何时候至少有 1 个磁盘节点在线。
- **优雅升级**：滚动重启必须等 policy 同步完成（用 `rabbitmqctl sync_queue <q>` 手动触发）。

---

## 5. mes-gateway 2 实例 + nginx-gateway-lb

### 5.1 拓扑

```
               客户端
                 │
                 ▼
          nginx-gateway-lb (:8080 对外)
           │              │
           │  least_conn  │
           ▼              ▼
     mes-gateway-1   mes-gateway-2
           │              │
           └──Nacos lb://──┘
                   │
                   ▼
         下游微服务（system / master-data / production / ...）
```

- **2 实例**：`mes-gateway-1` 与 `mes-gateway-2`，对 Nacos 同名注册为 `mes-gateway`。
- **无状态**：JWT 全链路无状态鉴权，**无需 session affinity / sticky session**。
- **前端 LB**：`nginx-gateway-lb` 使用 `least_conn` + `max_fails=3 fail_timeout=30s` 被动健康检查。

### 5.2 nginx 配置关键参数（见 `nginx/gateway-lb.conf`）

```nginx
upstream mes_gateway_cluster {
    least_conn;
    server mes-gateway-1:8080 max_fails=3 fail_timeout=30s;
    server mes-gateway-2:8080 max_fails=3 fail_timeout=30s;
    keepalive 64;
    keepalive_requests 10000;
}

location / {
    proxy_pass http://mes_gateway_cluster;
    proxy_http_version 1.1;
    proxy_set_header Connection "";
    proxy_next_upstream error timeout http_502 http_503 http_504;
    proxy_next_upstream_tries 2;
}
```

### 5.3 为什么不开 sticky session？

- MES 后端全部使用 **JWT**（`Authorization: Bearer xxx`）而非 server-side Session；
- 每个 gateway 副本都能独立解析 JWT，并通过 Nacos `lb://` 路由到下游；
- Sticky 反而会在单副本故障时造成这一批用户需要重新登录；**不开 sticky 是正确选择**。

### 5.4 扩展：替换为 Traefik（示意）

```yaml
services:
  traefik:
    image: traefik:v2.11
    command:
      - "--providers.docker=true"
      - "--entrypoints.web.address=:8080"
      - "--providers.docker.exposedbydefault=false"
    ports:
      - "8080:8080"
    volumes:
      - "/var/run/docker.sock:/var/run/docker.sock:ro"

  mes-gateway-1:
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.gateway.rule=PathPrefix(`/`)"
      - "traefik.http.services.gateway.loadbalancer.server.port=8080"
```

Traefik 内置 **主动健康检查**（`traefik.http.services.gateway.loadBalancer.healthCheck.path=/actuator/health`），比开源 nginx 更强。

### 5.5 k8s 部署等价形式（示意）

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mes-gateway
spec:
  replicas: 2
  template:
    spec:
      containers:
        - name: mes-gateway
          image: mes-gateway:latest
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: mes-gateway
spec:
  selector:
    app: mes-gateway
  ports:
    - port: 8080
  sessionAffinity: None     # 不开 sticky
```

---

## 6. 验证步骤（任挂 1 个节点系统仍可用）

### 6.1 前置

```bash
# 启动 HA 集群
cp .env.example .env
# 填充所有必需的密码/token
docker compose -f docker-compose.ha.yml up -d

# 确认所有容器健康
docker compose -f docker-compose.ha.yml ps
```

### 6.2 Nacos 故障演练

```bash
# 1. 查看当前注册
curl -u nacos:$NACOS_PASSWORD http://localhost:8848/nacos/v1/ns/instance/list?serviceName=mes-gateway

# 2. 随机 kill 一个节点
docker stop mes-nacos-2

# 3. 业务侧应仍能工作（等待 15s 让客户端感知）
curl http://localhost:8080/api/actuator/health
#   => 期望 200

# 4. 观察剩余 Nacos 节点日志，应看到 raft 重新选举
docker logs mes-nacos-1 | tail -n 30

# 5. 恢复节点
docker start mes-nacos-2
# 节点会自动重新加入集群
```

### 6.3 RabbitMQ 故障演练

```bash
# 1. 查看集群状态
docker exec mes-rabbitmq-1 rabbitmqctl cluster_status

# 2. 查看镜像队列状态
docker exec mes-rabbitmq-1 rabbitmqctl list_queues name policy pid slave_pids

# 3. kill 掉当前 master 节点（假设是 rabbitmq-1）
docker stop mes-rabbitmq-1

# 4. 业务侧发送消息应仍然成功
curl -X POST -H "Authorization: Bearer <jwt>" \
     http://localhost:8080/api/aps/sync-test
#   => 期望 200

# 5. 在剩余节点查看新 master
docker exec mes-rabbitmq-2 rabbitmqctl list_queues name policy pid

# 6. 恢复
docker start mes-rabbitmq-1
# 自动 re-join + sync（可能需要几分钟同步旧消息）
docker exec mes-rabbitmq-1 rabbitmqctl sync_queue -p mes <queue-name>
```

### 6.4 mes-gateway 故障演练

```bash
# 1. 测试基线
for i in {1..100}; do
  curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/actuator/health
done | sort | uniq -c
# => 100 200

# 2. kill 一个 gateway 副本
docker stop mes-gateway-1

# 3. 再跑一轮，期望仍全部 200（nginx 自动把流量切到 gateway-2）
for i in {1..100}; do
  curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/actuator/health
done | sort | uniq -c
# => 100 200（可能极少数因 keepalive 连接正好被挂而 502，但 next_upstream 兜底）

# 4. 恢复
docker start mes-gateway-1
```

### 6.5 验收标准

| 场景 | RTO | RPO | 验证通过条件 |
|---|---|---|---|
| 1 台 Nacos 宕机 | ≤ 15 s | 0 | 客户端重连成功，新请求正常 |
| 1 台 RabbitMQ 宕机 | ≤ 30 s | 0（镜像队列） | publish/consume 继续工作 |
| 1 台 gateway 宕机 | ≤ 3 s | 0 | 请求成功率 ≥ 99% |
| 同时宕 2 台 Nacos | 全挂 | — | 预期行为，Raft 少数派不可用 |

---

## 7. 已知限制与 P2 待办

### 7.1 `@Scheduled` 任务在多实例下的重复执行

`mes-aps` 模块有 4 个定时任务：

- `ApsUpstreamSyncJob` (每 5 分钟上行同步 MES→APS)
- `ApsDownstreamSyncJob` (每 5 分钟下行同步 APS→MES)
- `ApsCompensationSyncJob` (补偿重试)
- `ApsHealthCheckJob` (APS 服务健康探测)

**风险**：HA 场景下 `mes-backend-1` 和 `mes-backend-2`（或未来微服务拆分后的 `mes-integration-service` 副本）都会独立触发这些任务，导致：
- 上游同步重复调用 APS 接口（APS 侧 rate limit 可能触发）
- 补偿任务抢锁 → 数据库死锁风险
- 重复的健康探测请求浪费

**当前解决方案（临时）**：
- 暂时保持单副本部署 `mes-integration-service`（不横向扩容），避免多实例问题。
- 应用层用 Redis `SETNX` 分布式锁 + TTL 做幂等（`ApsUpstreamSyncService` 内部已有部分锁，需 Review）。

**建议 P2-27 改造方案（XXL-Job / ShedLock 二选一）**：

- **ShedLock**（轻量，推荐）：引入 `net.javacrumbs.shedlock:shedlock-spring` 和 `shedlock-provider-redis-spring`，在 `@Scheduled` 方法上加 `@SchedulerLock(name="ApsUpstreamSync", lockAtMostFor="PT4M")`，Redis 作为锁后端。
- **XXL-Job**（完备，推荐生产）：引入 `xxl-job-admin` 独立调度中心，把定时任务从 Spring `@Scheduled` 改为 `@XxlJob`，由调度器分片执行，支持失败重试、可视化监控。

任务预估：2~3 天，属于 P2-27 待办。

### 7.2 MySQL 自动故障切换

当前 `mysql-primary` 宕机需要人工切换 `mysql-replica` 为 primary。生产建议：
- **短期**：写好 runbook，配合监控报警（Prometheus mysql-exporter + Alertmanager）。
- **中长期**：切换到 **MHA / Orchestrator**，或直接用云厂商托管 RDS（自动高可用）。

### 7.3 nginx-gateway-lb 自己是单点

`nginx-gateway-lb` 本身只跑了 1 个容器，如果它挂了，两台 gateway 都白搭。

**生产建议**：
- 用云厂商的 SLB / ALB / NLB 替换（阿里云 SLB / AWS ALB / 腾讯云 CLB）—— 它们天然多副本 + 跨 AZ。
- 或用 `keepalived + nginx` 双机热备方案。
- k8s 环境下用 `Service` + `Ingress Controller`（ingress-nginx 本身 HA）即可。

### 7.4 Redis Sentinel 自动化切换

当前方案已满足 HA 要求，但注意：
- 应用端必须使用 **Sentinel 模式** 的连接（`spring.data.redis.sentinel.*`），而不是直接写 master 的 IP。
- 本 HA yml 里业务服务环境变量已配置 `SPRING_DATA_REDIS_SENTINEL_NODES`，请勿改回 `SPRING_DATA_REDIS_HOST`。

---

## 8. 故障排查速查表

| 现象 | 可能原因 | 命令 |
|---|---|---|
| Nacos 客户端 `Connection refused` | 3 个节点全挂 / 数据库库表缺失 | `docker logs mes-nacos-1` |
| RabbitMQ `connection_failed` | cookie 不一致 / 节点没 join | `docker exec mes-rabbitmq-1 rabbitmqctl cluster_status` |
| Gateway 502 Bad Gateway | 所有副本挂 / nginx 看不到副本 | `docker logs mes-gateway-lb` |
| 镜像队列不同步 | policy 没下发 / vhost 名字错 | `rabbitmqctl list_policies -p mes` |
| Nacos 启动后无 leader | MySQL 连不上 / 时钟漂移 | 看各节点 `logs/naming-server.log` |

---

## 9. 参考

- Nacos 集群部署官方文档：<https://nacos.io/zh-cn/docs/cluster-mode-quick-start.html>
- RabbitMQ Classic Mirrored Queue：<https://www.rabbitmq.com/ha.html>
- RabbitMQ Quorum Queue：<https://www.rabbitmq.com/quorum-queues.html>
- Spring AMQP multi-host support：<https://docs.spring.io/spring-amqp/reference/html/#connection-factories-with-multiple-hosts>
- ShedLock：<https://github.com/lukas-krecan/ShedLock>
- XXL-Job：<https://github.com/xuxueli/xxl-job>

---

**文档版本**：1.0 (P1-16)  
**最后更新**：2026-04-21
