#!/bin/bash
# ------------------------------------------------------------
# HA 主从复制用户（幂等）；MySQL 官方镜像会把 /docker-entrypoint-initdb.d
# 下的 .sh 文件通过 bash 执行，因此可以访问环境变量。
#
# 需在 docker-compose 中为 mysql 服务注入：
#   - MYSQL_ROOT_PASSWORD          （本脚本用于以 root 连接执行授权）
#   - MYSQL_REPLICATION_PASSWORD   （必填；若未设置将直接退出，避免使用弱口令）
# ------------------------------------------------------------
set -euo pipefail

if [[ -z "${MYSQL_REPLICATION_PASSWORD:-}" ]]; then
    echo "[init] MYSQL_REPLICATION_PASSWORD 未设置，跳过创建复制用户（生产必须配置）" >&2
    exit 0
fi

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<EOSQL
CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED BY '${MYSQL_REPLICATION_PASSWORD}';
ALTER USER 'repl'@'%' IDENTIFIED BY '${MYSQL_REPLICATION_PASSWORD}';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;
EOSQL

echo "[init] repl 用户创建/更新完成"
