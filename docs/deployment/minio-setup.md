# MinIO / 对象存储 部署指南

本文档介绍如何将 MES 系统的用户上传文件从"本地磁盘"迁移到"MinIO 对象存储"，以解决 HA 多实例共享文件、数据备份、跨节点访问等问题。

---

## 一、为什么要用对象存储

| 场景 | 本地磁盘 | MinIO / OSS |
|---|---|---|
| 单机部署 | ✅ 足够 | ❌ 过度设计 |
| HA 多后端实例 | ❌ 文件只在一台机器上 | ✅ 所有实例共享 |
| 容器重启 | ⚠️ 依赖 docker volume | ✅ 数据独立于容器生命周期 |
| 异地备份 | ❌ 需额外 rsync/cron | ✅ 桶复制/快照原生支持 |
| 跨地域灾备 | ❌ 几乎不可行 | ✅ 多区域复制 |
| 存储扩容 | ❌ 依赖单机磁盘 | ✅ 水平扩展 |

**结论**：生产环境必须使用对象存储；开发环境 `storage-type=local` 继续用本地磁盘即可。

---

## 二、快速开始（Docker Compose 单机版）

### 2.1 启动 MinIO 服务

项目根目录 [docker-compose.yml](../../docker-compose.yml) 已内置 `minio` 服务。首次启动前在 `.env` 文件（或 shell 环境变量）中准备：

```bash
# .env 示例（不要提交到 git）
MINIO_ROOT_USER=mes_admin
MINIO_ROOT_PASSWORD=Strong-Random-Secret-Here-32-chars+
MES_FILE_STORAGE_TYPE=minio
MES_FILE_MINIO_ENDPOINT=http://minio:9000
MES_FILE_MINIO_BUCKET=mes
```

启动：

```bash
docker compose up -d minio mes-backend mes-frontend
```

### 2.2 访问控制台

- API 地址：`http://localhost:9000`（后端使用）
- Web 控制台：`http://localhost:9001`（运维管理）
- 登录用户名：`MINIO_ROOT_USER`
- 登录密码：`MINIO_ROOT_PASSWORD`

### 2.3 bucket 初始化

应用启动时会**自动创建** `mes` bucket（`mes.file.minio.auto-create-bucket=true`）。如需手动预建：

```bash
# 使用 mc 客户端
docker run --rm -it --network mes-net \
  -e MC_HOST_local=http://mes_admin:YOUR_PASSWORD@minio:9000 \
  minio/mc:latest mb local/mes
```

---

## 三、应用配置切换

### 3.1 application.yml 新增配置项

```yaml
mes:
  file:
    # local：本地磁盘（默认）；minio：对象存储
    storage-type: ${MES_FILE_STORAGE_TYPE:local}
    minio:
      endpoint: ${MES_FILE_MINIO_ENDPOINT:http://minio:9000}
      access-key: ${MES_FILE_MINIO_ACCESS_KEY:}
      secret-key: ${MES_FILE_MINIO_SECRET_KEY:}
      bucket: ${MES_FILE_MINIO_BUCKET:mes}
      region: ${MES_FILE_MINIO_REGION:}
      presigned-expiry-seconds: 3600
      auto-create-bucket: true
```

### 3.2 切换规则

- `storage-type=local`（缺省）→ 生效 `LocalFileServiceImpl`，上传到 `./uploads`
- `storage-type=minio` → 生效 `MinioFileServiceImpl`，上传到 MinIO，数据库中存储 `minio://mes/tenant-1/.../abc.png`

两个实现通过 Spring `@ConditionalOnProperty` 互斥装配，**不会同时加载**。

### 3.3 数据迁移（从本地 → MinIO）

```bash
# 使用 mc 的 mirror 功能一次性迁移
docker run --rm \
  -v /path/to/uploads:/src:ro \
  -e MC_HOST_local=http://USER:PASS@minio:9000 \
  minio/mc:latest mirror /src local/mes
```

迁移完成后：
1. 在数据库 `mes_file` 表中执行批量 UPDATE，把 `/files/xxx` 前缀替换为 `minio://mes/xxx`
2. 切换 `MES_FILE_STORAGE_TYPE=minio` 并重启后端

---

## 四、生产部署建议

### 4.1 MinIO 集群（推荐 4 节点分布式部署）

```yaml
# docker-compose.minio-cluster.yml 示例（自行按物理机规划）
services:
  minio1:
    image: minio/minio:RELEASE.2025-01-20T14-49-07Z
    command: server http://minio{1...4}/data{1...2} --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    volumes:
      - minio1_data1:/data1
      - minio1_data2:/data2
  # minio2, minio3, minio4 类似
```

- **存储冗余**：4 节点 2 磁盘 → 可容忍 1 节点 + 2 磁盘同时挂掉
- **读写性能**：分布式纠删码（Erasure Code），高并发读写无瓶颈

### 4.2 替代方案：直接用云 OSS

MinIO 协议兼容 S3，所以以下服务可以无缝替代：

| 云厂商 | endpoint 示例 | 备注 |
|---|---|---|
| 阿里云 OSS | `https://oss-cn-hangzhou.aliyuncs.com` | 需要开启"S3 兼容 API" |
| 腾讯云 COS | `https://cos.ap-guangzhou.myqcloud.com` | 同上 |
| 华为云 OBS | `https://obs.cn-north-4.myhuaweicloud.com` | 同上 |
| AWS S3 | `https://s3.cn-north-1.amazonaws.com.cn` | 国内用 AWS 中国区 |

配置示例（以阿里云 OSS 为例）：

```yaml
mes:
  file:
    storage-type: minio
    minio:
      endpoint: https://oss-cn-hangzhou.aliyuncs.com
      access-key: ${OSS_ACCESS_KEY_ID}
      secret-key: ${OSS_ACCESS_KEY_SECRET}
      bucket: mes-prod
      region: cn-hangzhou
```

### 4.3 安全加固

1. **IAM 最小权限**：给 MES 应用单独创建 AccessKey，只授予 `s3:PutObject`、`s3:GetObject`、`s3:DeleteObject`、`s3:ListBucket`，**不要**用 root 账号。
2. **桶策略**：关闭"默认公开读写"，所有访问走应用侧的预签名 URL（`getUrl`）。
3. **HTTPS**：生产必须 `https://` 端点，不可明文。
4. **日志审计**：开启 MinIO 审计日志，对接 ELK / Loki。
5. **加密**：启用 SSE-S3 服务端加密。

---

## 五、备份策略

### 5.1 MinIO 自身备份

```bash
# 用 mc mirror 做跨集群备份（例如本集群 → 异地集群）
mc mirror --watch local/mes backup/mes
```

### 5.2 MinIO → 云 OSS 备份

```bash
# 定时把自建 MinIO 同步到阿里云 OSS（一备份三保险）
mc alias set aliyun https://oss-cn-hangzhou.aliyuncs.com AK SK
mc mirror --overwrite local/mes aliyun/mes-backup
```

cron 示例（每日凌晨 3 点）：

```cron
0 3 * * * /usr/local/bin/mc mirror --overwrite local/mes aliyun/mes-backup >> /var/log/minio-backup.log 2>&1
```

---

## 六、常见问题

### Q1：切到 minio 后旧的 `/files/xxx` URL 还能访问吗？

不能。需要完成数据迁移 + 数据库 URL 批量改写。未迁移前建议保持 `storage-type=local`。

### Q2：MinIO 启动时报 `bucket exists` 怎么办？

应用会自动跳过，不影响启动。手动预建 bucket 时把 `auto-create-bucket` 设为 `false` 更干净。

### Q3：预签名 URL 过期时间怎么调？

`mes.file.minio.presigned-expiry-seconds`（默认 3600 秒 = 1 小时）。或调用 `fileService.getUrl(url, 7200)` 按调用点指定。

### Q4：为什么 `upload` 返回的是 `minio://mes/xxx` 而不是真实 URL？

设计意图：数据库里存**不可过期的逻辑路径**；前端展示时用 `getUrl(logicalUrl, expiry)` 换成临时签名 URL。这样避免 URL 过期导致历史数据失效。

---

## 附录：与本地存储的差异对照

| 能力 | LocalFileServiceImpl | MinioFileServiceImpl |
|---|---|---|
| upload | 写入 `./uploads/` | 写入 bucket |
| delete | 删本地文件 | 删对象 |
| download | 返回 FileInputStream | 返回 MinIO 流 |
| getUrl | 返回原始 accessPrefix 路径 | 返回**预签名临时 URL** |
| 租户隔离 | tenant-{id}/ 目录 | tenant-{id}/ 对象前缀 |
| 扩展名白名单 | ✅ 一致 | ✅ 一致 |
| 大小限制 | mes.file.max-size-bytes | 同左 |
| 路径穿越防护 | ✅ | ✅ |
