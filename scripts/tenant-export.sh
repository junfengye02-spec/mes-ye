#!/usr/bin/env bash
# =====================================================================
# 租户导出工具（M6 Silo 迁移脚手架）
#   用法：./tenant-export.sh <tenantId> <outputDir>
#   输出：<outputDir>/tenant-<id>-<yyyyMMddHHmm>/
#           - schema.sql        建表 DDL（只包含业务表）
#           - data/<table>.csv  按 tenant_id 过滤导出的数据
#           - files.tar.gz      上传文件目录 uploads/tenant-<id>/ 打包
#
# 后续 import 到目标 Schema / 实例即可完成 POOL → SCHEMA / DB 的迁移。
# 本脚本假设本地可用 mysql / mysqldump，实际生产请放到 CI/CD 中并发。
# =====================================================================

set -euo pipefail

TENANT_ID="${1:-}"
OUT_DIR="${2:-./tenant-export}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-mes_migrator}"
DB_PASS="${DB_PASS:?需要通过环境变量 DB_PASS 提供迁移账号密码}"
DB_NAME="${DB_NAME:-mes_db}"
UPLOAD_DIR="${MES_UPLOAD_DIR:-./uploads}"

if [[ -z "$TENANT_ID" ]]; then
  echo "Usage: $0 <tenantId> [outputDir]" >&2
  exit 1
fi

STAMP="$(date +%Y%m%d%H%M)"
WORK_DIR="${OUT_DIR}/tenant-${TENANT_ID}-${STAMP}"
mkdir -p "${WORK_DIR}/data"

echo "[1/4] 导出 DDL -> ${WORK_DIR}/schema.sql"
mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" \
  --no-data --skip-triggers --skip-comments "$DB_NAME" \
  $(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -N -B -e \
      "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='${DB_NAME}' AND TABLE_NAME LIKE 'mes\\_%';") \
  > "${WORK_DIR}/schema.sql"

echo "[2/4] 导出业务表数据（按 tenant_id=${TENANT_ID} 过滤） -> ${WORK_DIR}/data/*.csv"
while IFS= read -r table; do
  if [[ -z "$table" ]]; then continue; fi
  mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -D "$DB_NAME" -B -e \
    "SELECT * FROM ${table} WHERE tenant_id = ${TENANT_ID}" \
    > "${WORK_DIR}/data/${table}.csv"
done < <(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -N -B -D "$DB_NAME" -e \
    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='${DB_NAME}' AND COLUMN_NAME='tenant_id' AND TABLE_NAME LIKE 'mes\\_%';")

echo "[3/4] 打包上传文件目录 uploads/tenant-${TENANT_ID}/"
if [[ -d "${UPLOAD_DIR}/tenant-${TENANT_ID}" ]]; then
  tar -C "${UPLOAD_DIR}" -czf "${WORK_DIR}/files.tar.gz" "tenant-${TENANT_ID}"
else
  echo "  （未找到 ${UPLOAD_DIR}/tenant-${TENANT_ID}，跳过）"
fi

echo "[4/4] 完成：${WORK_DIR}"
echo "       下一步：将 ${WORK_DIR} 拷贝到目标环境，执行 tenant-import.sh"
