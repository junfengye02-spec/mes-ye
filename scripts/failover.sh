#!/bin/bash
set -euo pipefail

echo "=== MES System Failover Script ==="
echo "[$(date)] Starting failover check..."

check_mysql_primary() {
    docker exec mes-mysql-primary mysqladmin ping -h localhost --silent 2>/dev/null
    return $?
}

check_redis_sentinel() {
    docker exec mes-redis-sentinel-1 redis-cli -p 26379 ping 2>/dev/null | grep -q PONG
    return $?
}

promote_mysql_replica() {
    echo "[$(date)] Promoting MySQL replica to primary..."
    docker exec mes-mysql-replica mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-12345678}" \
        -e "STOP SLAVE; RESET SLAVE ALL; SET GLOBAL read_only=OFF; SET GLOBAL super_read_only=OFF;"
    echo "[$(date)] MySQL replica promoted. Update application connection strings."
}

if ! check_mysql_primary; then
    echo "[$(date)] WARNING: MySQL primary is DOWN!"
    promote_mysql_replica
else
    echo "[$(date)] MySQL primary is healthy."
fi

if ! check_redis_sentinel; then
    echo "[$(date)] WARNING: Redis Sentinel is DOWN!"
else
    REDIS_MASTER=$(docker exec mes-redis-sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mes-redis-master 2>/dev/null)
    echo "[$(date)] Redis Sentinel healthy. Current master: ${REDIS_MASTER}"
fi

echo "[$(date)] Failover check completed."
