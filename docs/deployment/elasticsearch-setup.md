# Elasticsearch 部署与索引规划（P2-28）

> 适用版本：Elasticsearch 8.11.x + spring-data-elasticsearch 5.2.x + Spring Boot 3.2.12
> 目标场景：工单 / 派工任务 / 审计日志 达到百万级后 MySQL 模糊查询性能不足时的查询加速
> 关键原则：ES 是**加速副本**，不是事实源头，MySQL 仍是唯一真源

---

## 1. 定位与取舍

| 诉求 | MySQL | ElasticSearch |
|---|---|---|
| 精确查询 / 强一致 | 强 | 一般 |
| 模糊检索 / 中文分词 | 弱 | 强 |
| 聚合分析 / 大范围扫描 | 慢 | 快 |
| 全量存储 / 事务 | 必须 | 不适合 |
| 运维成本 | 低 | 中（集群 + 监控） |

**结论**：中等数据量（单表 < 500 万）MyBatis 足矣，不必强制所有查询走 ES。只有以下三张表到达**百万级**或出现**明显慢查询**时，再开启 ES：

1. `mes_work_order`（工单主表）
2. `mes_dispatch_task`（派工任务表）
3. `sys_audit_log`（审计日志，原任务描述误写成 `mes_audit_log`，实际表名是 `sys_audit_log`）

---

## 2. 部署架构

```
┌──────────────────────┐
│  mes-backend (Spring) │
│  ┌────────────────┐   │
│  │ SearchService  │   │
│  │  ┌─────┐       │   │
│  │  │ES主 │──失败─┼───▶MyBatis 降级
│  │  └─────┘       │   │
│  └────────────────┘   │
└──────────┬───────────┘
           │ HTTP(9200)
           ▼
┌─────────────────────────────────┐
│     ES 集群（3 节点）           │
│  ┌──────┐ ┌──────┐ ┌──────┐    │
│  │ es01 │ │ es02 │ │ es03 │    │
│  └──────┘ └──────┘ └──────┘    │
└─────────────────────────────────┘
           ▲
           │ Kibana（运维）
```

### 2.1 Docker Compose 启动

已提供 `docker-compose.es.yml`，叠加到主 compose 文件：

```bash
# 环境变量（建议写 .env）
export ELASTIC_PASSWORD=<随机强密码>
export KIBANA_SYSTEM_PASSWORD=<随机强密码>

# 启动
docker compose -f docker-compose.yml -f docker-compose.es.yml up -d

# 初始化 kibana_system 密码（首次）
docker exec -it mes-es01 \
  elasticsearch-reset-password -u kibana_system -i
```

### 2.2 资源规划

| 节点角色 | CPU | 内存 | 磁盘 | JVM Heap |
|---|---|---|---|---|
| 开发测试 3 节点 | 1c | 1 GB | 10 GB | 512 MB |
| 生产最小 3 节点 | 4c | 16 GB | SSD 500 GB | 8 GB |
| 生产推荐 3 master + 3 data | 8c | 32 GB | SSD 1 TB | 16 GB |

> 生产建议 master 与 data 分离，避免 master 选举受 data 节点 GC 影响

---

## 3. 索引分片策略

### 3.1 命名约定

```
{prefix}{logical_index}-{tenantId}
```

- `prefix` 由 `mes.es.index-prefix` 配置，多环境隔离（dev-/test-/prod-）
- 每个**租户一个独立索引**，实现物理隔离（ES 级别）
- 索引数过多时（> 1000 个）可改为**时间分片**：`{logical}-{yyyyMM}`

### 3.2 分片数

| 数据量 | 主分片 | 副本分片 | 单分片目标大小 |
|---|---|---|---|
| < 50 万条 | 1 | 1 | < 10 GB |
| 50 - 500 万 | 3 | 1 | 10-30 GB |
| > 500 万 | 5-7 | 1 | 30-50 GB |

> 分片数一旦建好不能修改；过多会浪费集群资源，过少则热点节点。

### 3.3 Mapping 模板（工单）

```json
PUT _index_template/mes_work_order_template
{
  "index_patterns": ["mes_work_order-*"],
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "refresh_interval": "5s"
    },
    "mappings": {
      "properties": {
        "id":            { "type": "long" },
        "workOrderNo":   { "type": "keyword" },
        "orderNo":       { "type": "keyword" },
        "mainProduct":   { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
        "productCode":   { "type": "keyword" },
        "productName":   { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
        "projectName":   { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
        "status":        { "type": "keyword" },
        "machineModel":  { "type": "keyword" },
        "planQty":       { "type": "double" },
        "tenantId":      { "type": "long" },
        "createdTime":   { "type": "date", "format": "yyyy-MM-dd HH:mm:ss" },
        "planStartTime": { "type": "date", "format": "yyyy-MM-dd HH:mm:ss" },
        "planEndTime":   { "type": "date", "format": "yyyy-MM-dd HH:mm:ss" }
      }
    }
  },
  "priority": 200
}
```

**IK 分词器**：需额外安装 `elasticsearch-analysis-ik`，版本必须对齐 ES 8.11.4：

```bash
docker exec -it mes-es01 \
  ./bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.11.4/elasticsearch-analysis-ik-8.11.4.zip
# 三个节点都要装，然后滚动重启
```

若不使用 IK，把 Mapping 中的 `analyzer` 改为 `standard` 或安装 ICU 分词器即可。

---

## 4. 数据同步方案对比

### 方案 A（本项目采用）：Spring ApplicationEvent → 异步 Listener → ES

```
业务 Service ──publish──▶ WorkOrderReleasedEvent
                              │
         @TransactionalEventListener(AFTER_COMMIT)
                              │
                              ▼
                  WorkOrderEsSyncListener
                   （@Async 异步线程池）
                              │
                              ▼
                    Elasticsearch.save()
```

| 维度 | 说明 |
|---|---|
| 侵入性 | 低，复用项目已有事件机制 |
| 一致性 | 最终一致；事务提交后才同步，无脏数据 |
| 可靠性 | ES 同步失败不影响业务；失败通过定时回填补偿 |
| 成本 | 零外部组件 |
| 缺陷 | 需业务侧在所有写路径 publish 事件；当前只有 `WorkOrderReleasedEvent` 一处，其他路径需补齐 |

### 方案 B：Logstash JDBC input / Canal binlog → ES

```
MySQL binlog ──► Canal Server ──► Kafka ──► Canal Adapter ──► ES
                     或
MySQL ──► Logstash (jdbc input) ──► ES（定时拉）
```

| 维度 | 说明 |
|---|---|
| 侵入性 | 零，业务代码不动 |
| 一致性 | 近实时（binlog 秒级） |
| 可靠性 | Canal / Kafka 本身可靠 |
| 成本 | 多一套组件，运维 + DBA binlog 权限审批 |
| 缺陷 | 字段映射维护在外部配置，和业务脱节；对软删除（`deleted`）需特殊处理 |

### 选型建议

- **MVP 或小团队**：方案 A，先跑起来
- **成熟运维体系 / 多系统共享 binlog**：方案 B
- **混合**：方案 A 做增量，Canal 做补偿，两种不冲突

本项目 P2-28 落地的是**方案 A**（代码见 `WorkOrderEsSyncListener`）。

---

## 5. 首次全量回填

业务跑了一段时间后才上线 ES，存量数据需要回填。两种方式任选：

### 5.1 基于 scroll + bulk 的批处理脚本（推荐）

```java
// 伪代码，放到一次性 CommandLineRunner 或运维接口里
int page = 0, pageSize = 500;
while (true) {
    List<WorkOrder> list = workOrderMapper.selectPage(new Page<>(page++, pageSize), null).getRecords();
    if (list.isEmpty()) break;
    // 按租户分组
    Map<Long, List<WorkOrderDoc>> grouped = list.stream()
        .map(this::toDoc)
        .collect(Collectors.groupingBy(WorkOrderDoc::getTenantId));
    grouped.forEach((tid, docs) -> {
        String idx = indexResolver.resolve("mes_work_order", tid);
        esOps.save(docs, IndexCoordinates.of(idx));
    });
}
```

### 5.2 Logstash 一次性同步

```conf
input {
  jdbc {
    jdbc_connection_string => "jdbc:mysql://mysql:3306/mes"
    jdbc_user => "${MYSQL_USER}"
    jdbc_password => "${MYSQL_PASSWORD}"
    statement => "SELECT id, work_order_no AS workOrderNo, order_no AS orderNo, main_product AS mainProduct,
                         product_code AS productCode, product_name AS productName, project_name AS projectName,
                         status, machine_model AS machineModel, plan_qty AS planQty, tenant_id AS tenantId,
                         created_time AS createdTime, plan_start_time AS planStartTime, plan_end_time AS planEndTime
                  FROM mes_work_order WHERE deleted = 0"
  }
}
output {
  elasticsearch {
    hosts => ["http://es01:9200"]
    index => "mes_work_order-%{tenantId}"
    document_id => "%{id}"
    user => "elastic"
    password => "${ELASTIC_PASSWORD}"
  }
}
```

---

## 6. 备份与恢复演练

### 6.1 快照仓库

```bash
# 注册 NFS / S3 仓库（示例：本地共享）
curl -u elastic:$ELASTIC_PASSWORD -XPUT http://localhost:9200/_snapshot/mes_backup \
  -H 'Content-Type: application/json' -d '{
    "type": "fs",
    "settings": { "location": "/mnt/es_backup" }
  }'

# 定时快照（SLM）
curl -u elastic:$ELASTIC_PASSWORD -XPUT http://localhost:9200/_slm/policy/daily-snapshot \
  -H 'Content-Type: application/json' -d '{
    "schedule": "0 30 1 * * ?",
    "name": "<daily-{now/d}>",
    "repository": "mes_backup",
    "config": { "indices": ["mes_*"], "include_global_state": false },
    "retention": { "expire_after": "30d", "min_count": 5, "max_count": 30 }
  }'
```

### 6.2 恢复演练

至少**每季度**做一次恢复演练：

1. 新开一个 dev 环境 ES 集群
2. 注册同名仓库并指向共享存储
3. `POST /_snapshot/mes_backup/<snapshot_id>/_restore`
4. 对业务侧 count / 抽样数据验证
5. 记录 RTO（恢复耗时）与 RPO（数据丢失时间）

---

## 7. 索引生命周期管理（ILM）

审计日志、访问日志类时序数据建议上 ILM：

```
HOT   （0-7 天，写入 + 查询活跃） → WARM（7-30 天，只读可压缩） → COLD（30+ 天，冻结） → DELETE（90 天删除）
```

示例：

```bash
curl -u elastic:$ELASTIC_PASSWORD -XPUT http://localhost:9200/_ilm/policy/mes_audit_policy \
  -H 'Content-Type: application/json' -d '{
    "policy": {
      "phases": {
        "hot":    { "actions": { "rollover": { "max_size": "20gb", "max_age": "7d" } } },
        "warm":   { "min_age": "7d",  "actions": { "forcemerge": { "max_num_segments": 1 }, "shrink": { "number_of_shards": 1 } } },
        "cold":   { "min_age": "30d", "actions": { "freeze": {} } },
        "delete": { "min_age": "90d", "actions": { "delete": {} } }
      }
    }
  }'
```

---

## 8. 应用侧配置

在 `application-dev.yml` / `application-prod.yml` 增加：

```yaml
mes:
  es:
    enabled: false            # 默认关闭；确认 ES 集群可用再打开
    uris:
      - http://localhost:9200
    username: elastic
    password: ${ELASTIC_PASSWORD:}
    connect-timeout: 5s
    socket-timeout: 10s
    max-conn-total: 50
    max-conn-per-route: 10
    index-prefix: "dev-"       # 多环境隔离；prod 可留空或改成 prod-
```

启用方式：

1. `mes.es.enabled=true`
2. 通过环境变量注入 `ELASTIC_PASSWORD`
3. 应用启动后观察日志 `Elasticsearch` 相关 Bean 是否注册成功
4. 调 `/api/workorder/work-order/query-rich` 验证；若 ES 宕机会看到 WARN 日志并自动降级

---

## 9. 后续扩展 TODO

本次 P2-28 只交付 `WorkOrderDoc` 作为完整示例，以下两个按同样模式补齐：

### 9.1 DispatchTaskDoc（mes-dispatch 模块）

- 字段：`id / orderNo / processNo / workName / dispatchStatus / tenantId / createdTime`
- 索引：`mes_dispatch_task-{tenantId}`
- 同步事件：派工单分派 / 状态流转时 publish；现无事件，需新增

### 9.2 AuditLogDoc（mes-framework 模块）

- 表名修正：实际是 `sys_audit_log`（任务描述里的 `mes_audit_log` 是笔误）
- 字段：`id / action / targetType / targetId / operatorUsername / tenantId / traceId / createdTime`
- 建议走 ILM（时序数据量大）
- 同步切入点：`AuditLogService.log()` 内已是 `@Async`，在 JDBC 写完后追加一次 ES save 即可

---

## 10. 监控

| 指标 | 来源 | 告警阈值 |
|---|---|---|
| 集群健康 | `GET /_cluster/health` | 非 `green`/`yellow` 超 5 分钟 |
| JVM 堆使用率 | Kibana Stack Monitoring | > 85% 持续 10 分钟 |
| 索引写入速率 | 同上 | 日均差异 > 50% |
| 慢查询 | `index.search.slowlog` | >1s 的查询数量 |
| ES 同步队列堆积 | 应用 metric（自埋点） | > 1000 条 |

与 Prometheus 集成可使用 `elasticsearch_exporter`，在 mes-backend 侧可用 Micrometer 自定义指标记录同步耗时。

---

## 版本变更

| 日期 | 变更 | 作者 |
|---|---|---|
| 2026-04-21 | 首版，P2-28 | mcp30 |
