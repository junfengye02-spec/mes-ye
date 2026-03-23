# 变更日志

## [1.1.0] - 2026-03-17

### 新增
- 高可用编排 `docker-compose.ha.yml`（MySQL 主从、Redis Sentinel、RabbitMQ、Nacos、多实例后端、Prometheus/Grafana）
- `mes-gateway` 微服务网关（JWT 全局过滤器、路由）
- Feign 客户端与降级、MQ 事件发布、分布式 ID 等基础能力
- 监控与脚本：`monitoring/`、`scripts/backup.sh`、`scripts/failover.sh`，MySQL/Redis 配置与初始化

### 变更
- 单体增强：`@EnableAsync`、线程池、限流过滤器、生产配置 `application-prod.yml`、Nginx 多后端 upstream
- 依赖：Spring Cloud / Alibaba BOM，OpenFeign 等

### 说明
- 后端与前端统一版本号 **1.1.0**（Maven `mes.version` / npm `version`）
