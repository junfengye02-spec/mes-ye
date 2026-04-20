#!/bin/bash
# ------------------------------------------------------------
# HA 主从复制用户（与 sql/00_replication_user.sh 等价，保留此目录用于
# 独立挂载 MySQL 初始化目录的部署场景）。
#
# 环境变量：
#   - MYSQL_ROOT_PASSWORD
#   - MYSQL_REPLICATION_PASSWORD
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
