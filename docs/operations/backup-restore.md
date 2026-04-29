# MES MySQL 备份与恢复 SOP

> 适用于生产环境 MES 主库。目标：**RTO ≤ 4 小时、RPO ≤ 15 分钟**。
> 任何修改请同步到 `scripts/backup/` 下的脚本与 `monitoring/prometheus/rules/mes-backup-alerts.yml`。

---

## 一、总体策略

### 1.1 备份分层

| 层级 | 频率 | 保留 | 介质 | 脚本模式 |
|------|------|------|------|----------|
| 日全量 | 每日 03:00 | 30 份 | 本地 PVC + MinIO `daily/` | `--mode=full` |
| binlog 增量 | 每 15 分钟 | 7 天 | 本地 PVC + MinIO `binlog/` | `--mode=binlog` |
| 周全量 | 每周日 04:00 | 12 份 | 本地 PVC + MinIO `weekly/` | `--mode=weekly` |
| 月全量 | 每月 1 日 05:00 | 永久 | 仅 MinIO `monthly/`（含异地复制） | `--mode=monthly` |
| 演练 | 每日 06:00 | 抽查 1 份 | 临时 MySQL | `verify-backup.sh` |

### 1.2 RTO / RPO 推导

- 日全量 + 15 分钟 binlog ⇒ 最大数据丢失窗口 = 15 分钟（**RPO ≤ 15 min ✓**）
- 典型恢复时长（80GB 库）：
  - 拉取备份 ≈ 15 min
  - 导入 dump ≈ 90 min
  - binlog 回放 ≈ 30 min
  - 合计 ≈ 2h15min（**RTO ≤ 4h ✓**）

---

## 二、数据流向图

```
┌─────────────┐  mysqldump --single-transaction       ┌────────────────┐
│  MySQL 主库  │ ───────────────────────────────────▶ │  本地 PVC       │
│ mysql-primary│  mysqlbinlog --read-from-remote       │ /backup/mysql  │
└──────┬──────┘                                       └───────┬────────┘
       │                                                      │
       │                                             gpg 对称加密
       │                                                      │
       │                                             mc mirror │
       ▼                                                      ▼
┌─────────────┐         异地同步（跨机房/跨云）        ┌─────────────────┐
│ 监控/告警   │ ◀─── Pushgateway / kube-state-metrics │  MinIO/OSS      │
│ Prometheus  │                                       │  mes-backups    │
└─────────────┘                                       └─────────────────┘
```

---

## 三、一键实战命令

### 3.1 Linux/K8s（推荐生产使用）

```bash
# 手工触发一次全量（仅运维排障时使用）
kubectl -n mes create job --from=cronjob/mes-mysql-backup-daily manual-$(date +%s)

# 查看最近 3 次备份结果
kubectl -n mes get jobs -l app=mes-backup --sort-by=.status.startTime | tail -n 5

# 查看备份日志
kubectl -n mes logs -l app=mes-backup --tail=200

# 从 MinIO 列出某一天的全量
mc ls mes-minio/mes-backups/daily/ | grep 20260421
```

### 3.2 Windows（备用/开发机）

```powershell
# 全量备份
.\scripts\backup\mysql-backup.ps1 -Mode full

# binlog 增量
.\scripts\backup\mysql-backup.ps1 -Mode binlog

# 演练（需要 Docker Desktop）
bash scripts/backup/verify-backup.sh
```

### 3.3 紧急恢复（到指定时间点）

```bash
# 1. 确定目标时间点（比如误操作发生在 2026-04-21 10:45）
export TARGET="2026-04-21 10:44:50"

# 2. 找到当天凌晨 03:00 的全量备份
mc cp mes-minio/mes-backups/daily/mes_daily_20260421_030000.sql.gz.gpg /tmp/

# 3. 拉取当天所有 binlog（03:00 之后的）
mkdir -p /tmp/mes-binlogs
mc mirror mes-minio/mes-backups/binlog/ /tmp/mes-binlogs/ \
   --newer-than 24h

# 4. 执行恢复
bash scripts/backup/mysql-restore.sh \
    --dump=/tmp/mes_daily_20260421_030000.sql.gz.gpg \
    --binlog-dir=/tmp/mes-binlogs \
    --target-time="${TARGET}" \
    --target-host=mysql-dr  # 建议先恢复到备库再切换
```

---

## 四、环境要求

### 4.1 MySQL 侧配置

```ini
# my.cnf 必须启用
[mysqld]
server-id = 1
log_bin = mysql-bin
binlog_format = ROW
binlog_expire_logs_seconds = 604800   # 7 天
gtid_mode = ON
enforce_gtid_consistency = ON
```

### 4.2 专用备份账号

```sql
-- 只赋最小权限
CREATE USER 'backup'@'%' IDENTIFIED BY 'Strong-Random-32chars+';
GRANT SELECT, SHOW VIEW, TRIGGER, LOCK TABLES, RELOAD, REPLICATION CLIENT,
      REPLICATION SLAVE, PROCESS, EVENT, SHOW DATABASES
   ON *.* TO 'backup'@'%';
FLUSH PRIVILEGES;
```

### 4.3 客户端工具

| 工具 | 用途 | 最低版本 |
|------|------|----------|
| `mysqldump` | 全量导出 | 8.0.x（与服务端一致） |
| `mysqlbinlog` | binlog 导出/回放 | 8.0.x |
| `gpg` | 对称加密 | 2.2+ |
| `mc` | MinIO client | RELEASE.2024-01+ |
| `docker` | verify-backup 用临时库 | 20.10+ |

---

## 五、告警与超时监控

监控规则：[`monitoring/prometheus/rules/mes-backup-alerts.yml`](../../monitoring/prometheus/rules/mes-backup-alerts.yml)

| 告警名 | 级别 | 触发 | 响应时限 |
|--------|------|------|----------|
| `MesBackupJobFailed` | P0 | 任一备份 Job 失败 ≥ 5 分钟 | 30 分钟内确认 |
| `MesBackupJobTimeout` | P1 | 单次 Job 运行 > 2 小时 | 1 小时内确认 |
| `MesBackupSizeAbnormal` | P1 | 体积偏离 7 天中位数 ±30% | 1 小时内确认 |
| `MesBackupFullStale` | P0 | 全量 > 24h 无成功 | 30 分钟内确认 |
| `MesBackupBinlogStale` | P0 | binlog > 30min 无成功 | 15 分钟内确认 |
| `MesBackupSizeDrift` | P1 | 单日体积偏离 ±30% | 1 小时内确认 |

超时监控：
- K8s CronJob `activeDeadlineSeconds=10800`（全量 3 小时强制结束）
- binlog `activeDeadlineSeconds=600`（必须 10 分钟内结束，给下一轮留时间）

---

## 六、演练流程

每日 06:00 自动执行 `verify-backup.sh`：

1. 随机抽取一份本地或 MinIO 上的日备
2. 启动临时 MySQL 容器（`mysql:8.0` + tmpfs）
3. 调用 `mysql-restore.sh` 完成恢复
4. 校对：
   - `flyway_schema_history` 中 `V2.04` 迁移必须存在且 `success=1`
   - `sys_tenant` 行数 ≥ `EXPECT_SYS_TENANT_MIN`（默认 1）
   - 业务表总数 ≥ 10（防止 dump 被截断）
5. 通过则追加 `verify-logs/verify_YYYYMMDD_HHMMSS.log`
6. 失败则保留容器 + webhook 告警

季度全流程演练：
- 第 1 个月：仅恢复 daily
- 第 2 个月：恢复 daily + binlog PITR 到随机时间点
- 第 3 个月：从 MinIO 异地（跨机房）拉取 weekly 并恢复到冷备机

---

## 七、故障诊断

### 7.1 `mysqldump: Error: Binlogging on server not active`

源库没开 binlog。`--master-data=2` 需要 binlog 支持。
修复：修改 `my.cnf` 启用 `log_bin`，重启数据库。

### 7.2 `ERROR 1227 (42000): Access denied; ... need SUPER, SYSTEM_VARIABLES_ADMIN`

账号权限不足。参考 §4.2 重新授权。

### 7.3 `gpg: decryption failed: Bad session key`

口令错误或 GPG 版本不兼容。
- 确认 `BACKUP_GPG_PASSPHRASE` 与加密时一致
- 客户端 GPG ≥ 2.2，使用 `--pinentry-mode loopback`

### 7.4 `mc: Access Denied`

检查：
- `MINIO_AK / MINIO_SK` 是否过期
- bucket 策略是否允许写入
- 时钟同步（S3 签名对时间误差敏感，NTP 必开）

### 7.5 PITR 回放到一半报错

- 若为单表误操作，使用 `--database=mes` 限制作用域
- 若某条 DDL 冲突，可以在 `--stop-datetime` 之前加一个 `--stop-position` 精确截断
- 再不行，回退到最近 weekly 全量

---

## 八、变更审计

| 日期 | 变更 | 变更人 |
|------|------|--------|
| 2026-04-21 | 初版发布（P1-17） | MCP26 |

修改本文档时请同步更新：
1. `scripts/backup/mysql-backup.sh` / `.ps1`
2. `scripts/backup/k8s/backup-cronjob.yaml`
3. `monitoring/prometheus/rules/mes-backup-alerts.yml`
4. 值班通讯录（运维钉钉群）

---

## 附录 A：最小化迁移到其它对象存储

MinIO 协议兼容 S3，可无缝切到：
- 阿里云 OSS：`MINIO_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com`
- 腾讯云 COS：`MINIO_ENDPOINT=https://cos.ap-guangzhou.myqcloud.com`
- AWS S3：`MINIO_ENDPOINT=https://s3.cn-north-1.amazonaws.com.cn`

只需替换 3 个环境变量：`MINIO_ENDPOINT / MINIO_AK / MINIO_SK`，其他脚本无需改动。

## 附录 B：恢复耗时估算表

| 库大小 | 拉取 | dump 导入 | binlog 回放 | 合计 | 是否满足 RTO=4h |
|--------|------|-----------|-------------|------|------------------|
| 20 GB  | 3 min  | 20 min | 10 min | 33 min | ✓ |
| 80 GB  | 15 min | 90 min | 30 min | 2h15min | ✓ |
| 200 GB | 40 min | 3h 30min | 45 min | 4h55min | ✗ → 需拆分库或用 xtrabackup 物理备份 |

> 200 GB 以上建议引入 Percona XtraBackup 做物理备份，可把恢复时间压缩到 1/4。
