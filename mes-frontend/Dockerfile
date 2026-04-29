# syntax=docker/dockerfile:1.6

# ============================================================
#  Stage 1: 前端构建
# ============================================================
FROM node:20-alpine AS builder
WORKDIR /build

# 先拷贝 lock 文件，利用 Docker layer cache
COPY package.json package-lock.json* ./
RUN npm ci --no-audit --prefer-offline

COPY . .
RUN npm run build

# ============================================================
#  Stage 2: 运行时 Nginx（M9-P3-13 全链路降权）
#  - nginx:1.25-alpine-slim + 非 root master + HEALTHCHECK
#  - 主进程以 appuser(uid=10001) 启动，非 root 无法绑定 <1024 特权端口，
#    站点配置统一 listen 8080；对外由 docker-compose / K8s Service 做
#    80 → 8080 端口映射，保持客户端体验不变
#  - Alpine slim 镜像无 ngx_brotli 模块，尝试从 apk 安装
#    （alpine 社区仓库有 nginx-mod-http-brotli；失败则降级仅 gzip）
# ============================================================
FROM nginx:1.25-alpine-slim AS runtime

ARG NGINX_CONF=nginx.conf
ENV TZ=Asia/Shanghai

# 安装 healthcheck 所需的 wget + 可选 brotli 模块
RUN set -eux; \
    apk add --no-cache wget tzdata ca-certificates; \
    (apk add --no-cache nginx-mod-http-brotli 2>/dev/null \
        && echo "brotli_enabled=true" > /etc/nginx/.brotli.flag) \
        || echo "brotli_enabled=false (fallback to gzip only)" > /etc/nginx/.brotli.flag; \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime; \
    echo "Asia/Shanghai" > /etc/timezone

# 创建非 root 用户 appuser:10001
# 主进程改由 appuser 启动，worker 继承相同 uid；CAP_NET_BIND_SERVICE 被内核禁用
RUN addgroup -g 10001 -S appuser \
    && adduser -u 10001 -S -G appuser -h /app -s /sbin/nologin appuser

# 主 nginx.conf 的 `user nginx;` 指令在非 root master 下会打 warn 并被忽略，
# 显式注释掉以消除 warn，并把 pid 路径迁到 appuser 可写目录
RUN set -eux; \
    sed -i 's|^user  *nginx;|#user  nginx;  # disabled: master runs as appuser (M9-P3-13)|' /etc/nginx/nginx.conf; \
    sed -i 's|^pid  */var/run/nginx.pid;|pid /tmp/nginx.pid;|' /etc/nginx/nginx.conf

# 准备 nginx 工作目录权限（缓存/日志/pid 均需可写）
RUN mkdir -p /var/cache/nginx /var/log/nginx /var/run /tmp \
    && chown -R appuser:appuser /var/cache/nginx /var/log/nginx /var/run /tmp \
    && chown -R appuser:appuser /usr/share/nginx/html \
    && chown -R appuser:appuser /etc/nginx/conf.d \
    && touch /tmp/nginx.pid \
    && chown appuser:appuser /tmp/nginx.pid

# 拷贝产物与站点配置
COPY --from=builder --chown=appuser:appuser /build/dist /usr/share/nginx/html
COPY --chown=appuser:appuser ${NGINX_CONF} /etc/nginx/conf.d/default.conf
# M8 Runbook §2.1 / §2.13 的升级维护页：镜像内同时预置 ha.conf + maintenance.conf
# 供 `docker exec ... cp` + `nginx -s reload` 原地切换，无需重建镜像
COPY --chown=appuser:appuser nginx.ha.conf /etc/nginx/conf.d/nginx.ha.conf
COPY --chown=appuser:appuser nginx.maintenance.conf /etc/nginx/conf.d/nginx.maintenance.conf
# M9-P3-08: 所有 nginx.*.conf 共享的安全头/nonce CSP include 文件
# 任意配置切换（default → ha → maintenance）时都能 include 到同一个路径
COPY --chown=appuser:appuser csp-headers.inc /etc/nginx/conf.d/csp-headers.inc

# M9-P3-13: EXPOSE 非特权端口 8080（代替原来的 80）
EXPOSE 8080

# 健康检查：/health 端点由 nginx.conf 提供，返回 ok
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD wget -qO- --tries=1 --spider http://localhost:8080/health || exit 1

# M9-P3-13: 主进程降权为 appuser(10001)
USER 10001:10001

CMD ["nginx", "-g", "daemon off;"]
