# MES 运维故障处理手册（Runbook）

> 适用范围：MES 项目（docker-compose 单体 / HA / 微服务三种部署形态）
> 维护方：运维值班团队
> 更新规则：每次真实事故后必须回填"复盘 / 改进项"小节

---

## 总体原则

1. **先稳住，再根治**：先让业务恢复可用（切流量 / 重启 / 回滚），再慢慢定位根因。
2. **变更留痕**：所有手工操作在 [事故处理记录](#附录a事故处理记录表) 中登记，包含时间、操作人、命令、结果。
3. **升级规则**：P0 事故 10 分钟内仍未恢复，必须升级到技术负责人；30 分钟内必须通知公司高层。
4. **禁止盲操作**：涉及 `DROP` / `TRUNCATE` / `rm -rf` 的命令必须双人复核。

---

## 事故分级与 SLA

| 等级 | 定义 | 响应时间 | 恢复目标（RTO） | 数据丢失目标（RPO） |
|---|---|---|---|---|
| **P0** | 核心业务完全不可用（登录挂、工单无法下发、数据丢失） | 5 分钟 | ≤ 30 分钟 | ≤ 15 分钟 |
| **P1** | 非核心功能不可用 / 性能严重下降 | 15 分钟 | ≤ 2 小时 | ≤ 1 小时 |
| **P2** | 单用户/单车间问题，不影响整体 | 1 小时 | ≤ 1 工作日 | N/A |
| **P3** | 可观测告警（磁盘 70%、慢查询 > 1s 等） | 24 小时 | 排期修复 | N/A |

---

## 场景 1 · mes-backend 服务起不来

### 1.1 症状

- `docker ps` 中 `mes-backend` 状态 `Restarting` 或 `Exited (1)` 循环重启。
- 浏览器访问 `http://<host>/api/actuator/health` 返回 502/连接拒绝。
- 网关（HA/微服务模式）日志出现大量 `Connection refused to lb://mes-system`。

### 1.2 排查流程

```
docker logs --tail 200 mes-backend
   │
   ├── 日志包含 "Communications link failure"            ──▶ 跳 场景 2（DB 连不上）
   ├── 日志包含 "RedisConnectionFailureException"        ──▶ 跳 场景 3（Redis 挂）
   ├── 日志包含 "Failed to bind to /0.0.0.0:9090"        ──▶ 端口被占
   ├── 日志包含 "NacosException: Client not connected"   ──▶ Nacos 连不上
   ├── 日志包含 "生产环境配置校验失败（P0-05）"              ──▶ JWT/密码校验失败
   └── 其他                                              ──▶ 看完整堆栈
```

### 1.3 常见修复命令

**端口被占（Windows 开发环境）**：

```powershell
netstat -ano | findstr :9090
taskkill /PID <pid> /F
```

**端口被占（Linux 生产）**：

```bash
ss -ltnp | grep :9090
# 若是旧容器残留：
docker rm -f $(docker ps -aq --filter name=mes-backend)
docker compose up -d mes-backend
```

**JWT/密码校验失败**（来自 `ProdEnvValidator.java`）：

```bash
# 错误信息形如：
#   生产环境配置校验失败（P0-05 安全策略）：
#     - 环境变量 SPRING_DATASOURCE_PASSWORD 必须注入且长度 >= 12 位
#     - 配置 mes.jwt.secret 长度必须 >= 32 位

# 修复：在 .env 或 secrets 里配齐
export MES_JWT_SECRET=$(head -c 32 /dev/urandom | base64)
export SPRING_DATASOURCE_PASSWORD=<运维交接的 12 位以上强密码>
docker compose up -d mes-backend
```

**Nacos 连不上（微服务模式）**：

```bash
docker exec mes-nacos curl -s http://localhost:8848/nacos/actuator/health
# 若 Nacos 自己不健康 → 先修 Nacos（场景 10 参考）
# Nacos 正常但 backend 连不上 → 检查 NACOS_SERVER_ADDR 环境变量是否写对
```

### 1.4 时间线示例（真实事故）

| 时间 | 事件 |
|---|---|
| 10:02 | 告警：`MesServiceDown` / mes-backend |
| 10:03 | 值班登录，`docker logs` 发现 `Communications link failure: jdbc:mysql://mysql:3306` |
| 10:05 | 确认 MySQL 容器 `Exited`，执行 `docker compose up -d mysql` 重启 |
| 10:08 | MySQL 健康检查通过，backend 自动恢复（restart: unless-stopped） |
| 10:10 | 业务自测 OK，清零告警 |
| 当日 | 复盘：宿主机磁盘满导致 MySQL 被 OOMKiller 杀，补磁盘扩容 + 加告警阈值 |

### 1.5 复盘要点

- 是否可以通过 HEALTHCHECK + `restart` 策略自愈？
- 启动失败的根因（配置 / 依赖 / 代码）？
- 是否要补新的告警规则？

---

## 场景 2 · 数据库连不上

### 2.1 症状

- backend 日志：`com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure`
- 访问 `/actuator/health` 返回 `"db":{"status":"DOWN"}`
- 所有写接口 500，有些读接口走缓存可能还能返回

### 2.2 排查流程

```bash
# 1) MySQL 容器存在吗？
docker ps -a | grep mysql

# 2) MySQL 进程在跑吗？
docker exec mes-mysql mysqladmin ping -h localhost

# 3) 网络通吗？（容器间）
docker exec mes-backend ping -c 3 mysql

# 4) 用户密码对吗？
docker exec mes-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT 1"

# 5) 连接数满了吗？
docker exec mes-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
  -e "SHOW STATUS LIKE 'Threads_connected'; SHOW VARIABLES LIKE 'max_connections';"

# 6) 慢查询堵住了吗？
docker exec mes-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
  -e "SHOW PROCESSLIST" | head -40
```

### 2.3 修复动作

- 容器挂了 → `docker compose up -d mysql`。
- 连接数满 → 临时 `SET GLOBAL max_connections = 800`，并排查连接池泄漏（Druid `druid-spring-boot-3-starter` 的 `min-idle=20` 正常）。
- 慢查询堵 → `KILL <id>` 杀掉长事务，同时记录 SQL 去优化。
- 磁盘满 → `df -h /var/lib/docker` 检查宿主机；清理历史 binlog（`PURGE BINARY LOGS BEFORE now() - INTERVAL 3 DAY`）。
- 密码不对 → 说明 `.env` 改了但容器没 recreate，执行 `docker compose up -d --force-recreate mysql mes-backend`。

### 2.4 时间线示例

| 时间 | 事件 |
|---|---|
| 02:17 | 告警 `MysqlDown` + `MesHttp5xxSpike` |
| 02:19 | SHOW PROCESSLIST 发现 800+ 连接全是 `Sleep` 状态，来自单台 backend IP |
| 02:20 | 检查 backend 是否泄漏连接，`jstack` pid 看到 500+ 线程等待 Druid 连接 |
| 02:22 | 临时 KILL 所有 Sleep 连接，业务恢复 |
| 02:40 | 定位到代码中 `try-with-resources` 漏了 → 紧急发补丁 → 发版 |

### 2.5 复盘要点

- Druid 配置 `removeAbandoned: true, removeAbandonedTimeout: 180` 有没有开？
- 慢查询日志（`long_query_time=2`）有没有接 Grafana 看板？

---

## 场景 3 · Redis 挂或性能骤降

### 3.1 症状

- backend 日志：`RedisConnectionFailureException: Unable to connect to Redis`
- 接口响应慢（缓存降级到 DB），QPS 下降到 1/5
- Sentinel 模式下告警：`sentinel.xxx down-after-milliseconds` 触发

### 3.2 排查流程

```bash
# 1) Redis 活着吗？
docker exec mes-redis redis-cli -a "$REDIS_PASSWORD" ping
# 期望：PONG

# 2) 内存是否爆满？
docker exec mes-redis redis-cli -a "$REDIS_PASSWORD" info memory | grep used_memory_human
# 对比 info memory 中的 maxmemory

# 3) 慢查询定位
docker exec mes-redis redis-cli -a "$REDIS_PASSWORD" slowlog get 50

# 4) 连接数
docker exec mes-redis redis-cli -a "$REDIS_PASSWORD" info clients

# 5) 持久化状态
docker exec mes-redis redis-cli -a "$REDIS_PASSWORD" info persistence | grep aof
```

### 3.3 修复动作

- Redis 容器挂 → `docker compose up -d redis`；AOF 开启（`--appendonly yes`）理论上重启后数据不丢（最多丢最后 1s，`appendfsync everysec`）。
- 内存爆满 → 已配置 `--maxmemory-policy allkeys-lru` 会自动淘汰，但要排查：是否有 big key（`redis-cli --bigkeys`），是否 TTL 漏配。
- 慢查询 → `SLOWLOG GET` 看到 `KEYS *`、`SMEMBERS` 大集合这类命令，立即找业务代码定位改写为 `SCAN`、`SMEMBERS` 分页。
- Sentinel 场景主从切换未完成 → 检查 `SENTINEL sentinels mes-redis-master` 看哨兵数量是否达到法定人数（2/3）。

### 3.4 时间线示例

| 时间 | 事件 |
|---|---|
| 14:30 | 告警 5xx 飙升，JVM 堆未满 |
| 14:32 | SLOWLOG 发现 `KEYS order:*` 单条 3.5 秒，阻塞 Redis 单线程 |
| 14:33 | 临时：重启 backend 把连接打回去（缓存穿透到 DB 但能撑） |
| 14:40 | 定位到新上线的"订单列表"代码用了 `KEYS`，紧急 hotfix 改 `SCAN` |

### 3.5 复盘要点

- 是否有 `KEYS` 命令的代码扫描规则（CI 里加 grep）？
- big key 监控（`--bigkeys` 每日跑一次）？

---

## 场景 4 · RabbitMQ 消息堆积

### 4.1 症状

- 告警 `RabbitMqQueueBacklog` 触发（ready > 10000）
- 业务现象：工单下发后状态长时间停留在 `PENDING_DISPATCH`
- `http://rabbitmq:15672` 控制台看到某 queue 红色警告

### 4.2 判断是"扩容消费者"还是"清队列"

```
Ready 消息数
     ↑
     │  持续增长曲线（消费速度 < 生产）    ──▶ 扩容消费者 / 查 consumer 异常
     │  阶梯式一次性暴涨（批处理任务）      ──▶ 等自然消费 / 临时调高 prefetch
     │  飙升后稳定（消费全挂）              ──▶ 查 consumer 报错，必要时手动 ack + 重放
     │
     └──────────────────────────────────▶ 时间
```

### 4.3 常用命令

```bash
# 看具体队列状态
docker exec mes-rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers

# 看 consumer 情况
docker exec mes-rabbitmq rabbitmqctl list_consumers

# 清空指定队列（慎用！确认消息可以丢）
docker exec mes-rabbitmq rabbitmqctl purge_queue <queue_name> -p mes

# 扩容：加多个 backend 实例或调高 prefetch
# application-prod.yml:
#   spring.rabbitmq.listener.simple.prefetch: 10 → 50
#   concurrency: 5 → 10
#   max-concurrency: 20 → 50
# 改完 force-recreate backend
```

### 4.4 时间线示例

| 时间 | 事件 |
|---|---|
| 09:15 | 告警 RabbitMqQueueBacklog |
| 09:17 | list_consumers 发现 consumer 为 0（backend 某实例挂导致订阅丢） |
| 09:18 | 重启 backend → consumer 恢复 → 3 分钟内消费完堆积 |

### 4.5 复盘要点

- `publisher-confirm-type: correlated` 已配，生产者侧失败重试有没有落地？
- 死信队列（DLX）是否配齐？超过 N 次重试的消息去哪儿？

---

## 场景 5 · MinIO 挂了 uploads 访问异常

### 5.1 症状

- 前端上传文件报 500 / 超时
- 详情页图片不显示（403 / 预签名 URL 失效）
- backend 日志：`ErrorResponseException: The specified bucket does not exist` 或 `Connection refused to http://minio:9000`

### 5.2 排查与修复

```bash
# 1) MinIO 进程存在？
docker ps | grep minio
docker logs --tail 100 mes-minio

# 2) 健康检查
curl http://localhost:9000/minio/health/live

# 3) 连得通？
docker exec mes-backend curl -sS http://minio:9000/minio/health/live

# 4) AccessKey 权限对？
docker run --rm --network mes-net \
  -e MC_HOST_local=http://$MINIO_ROOT_USER:$MINIO_ROOT_PASSWORD@minio:9000 \
  minio/mc:latest ls local/mes | head

# 5) 磁盘满了？
docker exec mes-minio df -h /data
```

### 5.3 紧急降级方案

- 若 MinIO 需要长时间修复，**临时切回本地存储**：
  ```bash
  export MES_FILE_STORAGE_TYPE=local
  docker compose up -d mes-backend
  ```
  （仅对新上传生效；旧的 `minio://` URL 仍无法访问。）
- MinIO 数据卷损坏 → 从备份恢复：`mc mirror backup/mes local/mes`。

### 5.4 时间线示例

| 时间 | 事件 |
|---|---|
| 16:20 | 告警 `MesServiceDown` MinIO |
| 16:22 | df -h 发现 /data 100%，被单个 8GB 设计图纸撑爆 |
| 16:25 | 临时切 storage-type=local 让业务继续 |
| 16:30 | 挂载更大磁盘，`docker compose up -d minio` 恢复 |

### 5.5 复盘要点

- 单文件大小限制（`mes.file.max-size-bytes`）是否太宽松？
- MinIO 磁盘使用率告警阈值（85%）是否加了？

---

## 场景 6 · 容器重启后数据没了

### 6.1 症状

- 用户反映"昨天下单的工单都不见了"
- 或 Grafana 上历史曲线断掉一段

### 6.2 根本原因：**数据卷未挂载 / 挂错路径**

### 6.3 三步确认

```bash
# 1) 检查容器挂了哪些 volume
docker inspect mes-mysql -f '{{ range .Mounts }}{{ .Type }} {{ .Source }} -> {{ .Destination }}{{ println }}{{ end }}'
# 期望：
#   volume /var/lib/docker/volumes/mes_mysql_data/_data -> /var/lib/mysql

# 2) 如果是 bind 挂载本地目录，检查目录权限
ls -la /path/to/mysql_data  # 应归 999:999 (mysql 用户)

# 3) 历史 volume 是否意外被 docker system prune 清理了
docker volume ls
docker volume inspect mes_mysql_data
```

### 6.4 预防措施（必须上线前核对）

- `docker-compose.yml` 的 `volumes:` 段落必须显式声明命名卷。
- 不要用 `docker compose down -v`（会删 volume），用 `docker compose down` 保留数据。
- `docker system prune --volumes` 严禁在生产跑。
- 运维手册写死检查清单：上线前 `docker compose config | grep volumes` 核对一次。

### 6.5 恢复尝试

- 走 MySQL 备份恢复（场景 9）。
- 走 MinIO 桶镜像恢复（`mc mirror` 异地备份回写）。

---

## 场景 7 · 磁盘满了

### 7.1 症状

- 告警 `HostHighDisk` 或 `df -h` 使用率 > 85%
- backend 无法写日志，反复 WARN `Failed to write log`
- MySQL 拒绝新写入（`InnoDB: Error: unable to create temporary file`）

### 7.2 清理清单（按优先级）

```bash
# 1) 日志滚动未清理（最常见）
find /var/lib/docker/volumes/mes_uploads/_data -name "*.log.gz" -mtime +30 -delete
docker logs --details mes-backend 2>/dev/null | wc -l   # 不限行数的 logs 导致超大

# 2) MySQL binlog 过期
docker exec mes-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
  -e "PURGE BINARY LOGS BEFORE now() - INTERVAL 3 DAY"

# 3) Docker 历史镜像 / 构建缓存
docker system df                        # 先看哪块占最多
docker image prune -af --filter "until=168h"   # 清 7 天前的镜像
docker builder prune -af --keep-storage 2GB

# 4) uploads 大文件审计
du -sh /var/lib/docker/volumes/mes_uploads/_data/* | sort -h | tail -20

# 5) 紧急扩容
# 云主机：阿里云/腾讯云控制台一键扩容（在线扩）
# 自建：lvm 扩 / 加磁盘 fstab 挂
```

### 7.3 预防

- logback 已配 `maxHistory=30, totalSizeCap=3GB`，确认生产不被运维手工关掉。
- MySQL binlog：`binlog-expire-logs-seconds=604800`（7 天）保留够用。
- uploads 切 MinIO（P0-08 已落地）后就不占本机磁盘。

---

## 场景 8 · 紧急灰度回滚

### 8.1 触发条件

- 新版发完 5xx 率 > 5%
- 关键业务链路 smoke-test 失败
- 新代码数据污染（写错状态等，还来得及回到上一个 tag）

### 8.2 Docker tag 回滚（最快，分钟级）

```bash
# 假设当前 ghcr.io/mes/mes-backend:20260421-abc12345 出问题
# 上个已知稳定版本：20260420-def67890

# 1) 改 .env 或 compose 里的 image tag
export MES_BACKEND_TAG=20260420-def67890
export MES_FRONTEND_TAG=20260420-def67890

# 2) 拉镜像并滚动替换
docker compose pull mes-backend mes-frontend
docker compose up -d --no-deps --remove-orphans mes-backend mes-frontend

# 3) 验证
curl -fsS http://localhost:9090/api/actuator/health
curl -fsS http://localhost/ | head
```

### 8.3 HA 模式下的蓝绿回滚

```bash
# 方式 A：先回 backend-2，观察 5 分钟，没问题再回 backend-1
MES_BACKEND_TAG=<stable> docker compose up -d mes-backend-2
# 通过 nginx 上线/下线配合：
docker exec mes-frontend nginx -s reload
```

### 8.4 数据库 schema 变更回滚

- **原则**：schema 变更尽量做"向前兼容"（加列、加索引），避免 drop。
- 如果真的执行了 DROP/ALTER，从 binlog 反向生成 undo SQL：
  ```bash
  mysqlbinlog --start-datetime="2026-04-21 10:00:00" \
              --stop-datetime="2026-04-21 10:05:00" \
              /var/lib/mysql/mysql-bin.000123 > undo.sql
  # 手工审查 undo.sql，确认反向语句后再 source
  ```

### 8.5 时间线示例

| 时间 | 事件 |
|---|---|
| 11:00 | 发版 20260421-abc12345 |
| 11:08 | 5xx 率从 0.2% 飙到 8%，告警 MesHttp5xxSpike |
| 11:10 | 决策回滚。执行 tag 切换 |
| 11:13 | 新容器健康，5xx 回落 |
| 11:15 | 业务验证通过，事故 15 分钟内解决 |

---

## 场景 9 · 生产 MySQL 从备份恢复（RTO ≤ 4h / RPO ≤ 15min）

### 9.1 前置条件

- 每日 02:00 跑 `scripts/backup.sh` → 全量 `*.sql.gz` 到本地 `/backup/mysql/` + 异地 OSS
- binlog 开启（`log-bin=mysql-bin`、`sync-binlog=1`、保留 7 天）
- RPO 15 分钟 = 每 15 分钟 flush 一次 binlog 到异地

### 9.2 恢复 SOP（含时间预估）

```bash
# ========== 阶段 1：准备（10 分钟） ==========
# 1.1 挂到新的 MySQL 实例（单机实验建议用独立容器）
docker run -d --name mes-mysql-restore \
  -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
  -v mysql_restore_data:/var/lib/mysql \
  -p 3310:3306 mysql:8.0

# 1.2 从 OSS/本地拉取最近一次全量备份
mc cp aliyun/mes-backup/mysql/mes_20260421_020000.sql.gz ./
gunzip mes_20260421_020000.sql.gz

# ========== 阶段 2：全量恢复（1-2 小时，视数据量） ==========
# 2.1 导入全量
cat mes_20260421_020000.sql | \
  docker exec -i mes-mysql-restore mysql -uroot -p"$MYSQL_ROOT_PASSWORD"

# 2.2 检查行数与主库对账
docker exec mes-mysql-restore mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
  -e "SELECT COUNT(*) FROM mes.mes_work_order"

# ========== 阶段 3：增量追平（30-60 分钟） ==========
# 3.1 找到全量备份结束时的 GTID（备份文件头部有 SET @@GLOBAL.GTID_PURGED）
grep -A1 "GTID_PURGED" mes_20260421_020000.sql | head -3

# 3.2 从异地备份拉 binlog（02:00 之后的所有 binlog）
mc cp --recursive aliyun/mes-backup/binlog/ ./binlog-restore/

# 3.3 应用 binlog（注意 --start-position 或 --start-datetime）
mysqlbinlog --start-datetime="2026-04-21 02:00:00" \
            --stop-datetime="2026-04-21 14:45:00" \
            ./binlog-restore/mysql-bin.* | \
  docker exec -i mes-mysql-restore mysql -uroot -p"$MYSQL_ROOT_PASSWORD"

# ========== 阶段 4：切换（15 分钟） ==========
# 4.1 应用层切换连接串
#    docker-compose.yml -> SPRING_DATASOURCE_URL=jdbc:mysql://mysql-restore:3306/mes
# 4.2 停旧 MySQL（如果还在)
# 4.3 docker compose up -d 应用
# 4.4 冒烟测试：登录、下单、查询
```

### 9.3 演练建议

- **每季度必须做一次完整演练**，否则 SOP 一定过时。
- 演练目标：按上面 4 阶段实测耗时，如果 > 4h 要优化（更快的备份解压、并行 binlog）。

---

## 场景 10 · HA 版 MySQL 主从切换

### 10.1 触发条件

- `MysqlDown` 告警命中 `mysql-primary`
- 主库磁盘/内存/网络不可恢复

### 10.2 手动切换步骤（依赖 `scripts/failover.sh`）

```bash
# 1) 先验证从库延迟
docker exec mes-mysql-replica mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
  -e "SHOW SLAVE STATUS\G" | grep Seconds_Behind_Master
# 延迟 < 10s 可切，> 60s 需评估数据损失

# 2) 执行 failover 脚本（内部会 STOP SLAVE、关 read_only）
./scripts/failover.sh

# 3) 改应用连接串指到从库
#    临时方案：直接改 docker-compose.ha.yml 的 SPRING_DATASOURCE_URL
#    生产应用：如果已有 ProxySQL 则修改路由规则而不动应用
sed -i 's|mysql-primary:3306|mysql-replica:3306|g' docker-compose.ha.yml
docker compose -f docker-compose.ha.yml up -d --no-deps mes-backend-1 mes-backend-2

# 4) 旧主恢复后作为新从库
# 执行 CHANGE MASTER TO 指向新主，START SLAVE
```

### 10.3 自动化改进路径

- 当前 `failover.sh` 是"人工触发"，生产建议上 **Orchestrator** 或 **MHA**：自动选主、应用层走 ProxySQL 路由，RTO 可压到 30 秒内。
- 目标：P1 任务单独立项。

---

## 场景 11 · 事故分级与应急升级矩阵

### 11.1 升级阈值

```
P0 事故
  ├── 发生 + 5 分钟：值班工程师接手
  ├── 发生 + 10 分钟：主管 + 技术负责人介入
  ├── 发生 + 30 分钟：业务方同步 + 开始对外公告
  └── 发生 + 1 小时：公司高层通报

P1 事故
  ├── 发生 + 15 分钟：值班工程师接手
  ├── 发生 + 1 小时：主管介入
  └── 发生 + 4 小时：技术负责人知情

P2/P3
  └── 按工单流程处理，不强制即时响应
```

### 11.2 值班联系表（填写模板）

| 角色 | 联系人 | 电话 | 钉钉/微信 | 备份 |
|---|---|---|---|---|
| 值班 L1（轮班） | （每周轮班） | | | L2 |
| 值班 L2 | | | | 主管 |
| 运维主管 | | | | 技术负责人 |
| DBA | | | | 技术负责人 |
| 技术负责人 | | | | CTO |
| 业务接口人 | | | | 业务负责人 |

### 11.3 沟通模板（钉钉/群公告）

**事故通报**：

```
【MES-P0】2026-04-21 14:30 起工单下发接口不可用
影响范围：所有车间工单无法创建
根因推测：RabbitMQ 消息堆积 → 待确认
当前状态：已开始排查
预计恢复时间：15 分钟
责任人：@张三
```

**恢复通报**：

```
【MES-P0】已于 14:45 恢复
持续时间：15 分钟
根因：RabbitMQ 消费者线程异常退出
已做处理：重启消费者，消息已全部 ack
后续改进：补 consumer 存活告警、加自动 failover
复盘会议：2026-04-21 17:00 线上会议室 X
```

---

## 附录 A · 事故处理记录表

每次事故必须在下表新增一行（保留至少 1 年）：

| 时间 | 等级 | 影响 | 根因 | 恢复方式 | 责任人 | 链接 |
|---|---|---|---|---|---|---|
| 2026-04-21 14:30 | P0 | 工单下发 | RabbitMQ consumer 异常退出 | 重启 consumer | 张三 | [工单 #12345](#) |

---

## 附录 B · 常用诊断一键脚本参考

```bash
# mes-health-check.sh（建议加入 scripts/）
#!/bin/bash
set -euo pipefail

echo "=== MES 快速体检 ==="
echo "[1/5] 容器状态"
docker compose ps

echo "[2/5] Actuator 健康"
curl -fsS http://localhost:9090/api/actuator/health | jq .

echo "[3/5] MySQL ping"
docker exec mes-mysql mysqladmin ping -h localhost

echo "[4/5] Redis ping"
docker exec mes-redis redis-cli -a "$REDIS_PASSWORD" ping

echo "[5/5] 磁盘使用率"
df -h | grep -E "^/dev|Filesystem"

echo "=== 体检完成 ==="
```

---

**最后更新**：2026-04-21 · 频道 mcp11
**维护人**：运维团队 + 值班工程师
**评审周期**：每 3 个月 / 每次 P0 事故后
