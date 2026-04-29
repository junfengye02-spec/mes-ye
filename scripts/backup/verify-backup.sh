#!/bin/bash
# ==============================================================================
# MES 备份自检脚本
# ------------------------------------------------------------------------------
# 功能：
#   1. 从本地（或 MinIO）随机抽取一个全量备份
#   2. 启动一个临时 MySQL（Docker 或本地端口）实例
#   3. 使用 mysql-restore.sh 恢复到该实例
#   4. 校对关键指标：
#      - flyway_schema_history 里应包含 V2.04 迁移（或等价版本）
#      - sys_tenant 行数 >= 期望最小值（默认 1，可通过 EXPECT_SYS_TENANT_MIN 覆盖）
#   5. 校验成功：记录 log 并把该备份标记通过（不删，留作审计）
#      校验失败：保留实例便于排查，通过 webhook 告警
#
# 依赖：bash / docker / mysql 客户端 / jq(可选)
#
# 使用示例：
#   bash verify-backup.sh                         # 随机抽本地 daily 一份
#   bash verify-backup.sh --bucket-prefix=daily   # 从 MinIO daily/ 中随机抽
#   bash verify-backup.sh --dump=/path/to/file    # 校验指定文件
#
# 退出码：
#   0 校验通过 / 10 参数/环境错误 / 20 找不到可用备份 / 30 恢复失败 / 40 校对失败
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -f "${SCRIPT_DIR}/.env" ]]; then
  # shellcheck disable=SC1091
  set -a; . "${SCRIPT_DIR}/.env"; set +a
fi

BACKUP_ROOT="${BACKUP_ROOT:-/backup/mysql}"
MINIO_BUCKET="${MINIO_BUCKET:-mes-backups}"
MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://minio:9000}"
MINIO_AK="${MINIO_AK:-${MINIO_ROOT_USER:-}}"
MINIO_SK="${MINIO_SK:-${MINIO_ROOT_PASSWORD:-}}"
BACKUP_NOTIFY_WEBHOOK="${BACKUP_NOTIFY_WEBHOOK:-}"
GPG_PASS="${BACKUP_GPG_PASSPHRASE:-}"

VERIFY_MYSQL_IMAGE="${VERIFY_MYSQL_IMAGE:-mysql:8.0}"
VERIFY_CONTAINER="mes-backup-verify-$$"
VERIFY_PORT="${VERIFY_PORT:-3399}"
VERIFY_ROOT_PASS="${VERIFY_ROOT_PASS:-verify_$(date +%s)}"
VERIFY_DB="${VERIFY_DB:-mes_verify}"

EXPECT_FLYWAY_VERSION="${EXPECT_FLYWAY_VERSION:-2.04}"
EXPECT_SYS_TENANT_MIN="${EXPECT_SYS_TENANT_MIN:-1}"

DUMP_FILE=""
BUCKET_PREFIX="daily"
KEEP_CONTAINER="false"

usage() { sed -n '2,30p' "$0"; exit 0; }

for arg in "$@"; do
  case "$arg" in
    --dump=*)           DUMP_FILE="${arg#*=}" ;;
    --bucket-prefix=*)  BUCKET_PREFIX="${arg#*=}" ;;
    --keep-container)   KEEP_CONTAINER="true" ;;
    --help|-h)          usage ;;
  esac
done

log()  { printf '[%s] [verify] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }
fail() { log "FAIL: $*"; exit "${2:-40}"; }

notify() {
  local reason="$1"
  log "告警：${reason}"
  [[ -n "${BACKUP_NOTIFY_WEBHOOK}" ]] || return 0
  curl -s -m 10 -X POST -H 'Content-Type: application/json' \
    -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"[MES备份演练告警] ${reason}\"}}" \
    "${BACKUP_NOTIFY_WEBHOOK}" || true
}

# ---------- 1. 选备份文件 ----------
if [[ -z "${DUMP_FILE}" ]]; then
  log "未指定 --dump，随机抽样一份备份"

  # 优先从本地 full 目录选
  CANDIDATES=()
  if [[ -d "${BACKUP_ROOT}/full" ]]; then
    while IFS= read -r -d '' f; do CANDIDATES+=("${f}"); done < <(find "${BACKUP_ROOT}/full" -maxdepth 1 -type f \( -name '*.gz' -o -name '*.gpg' \) -print0 2>/dev/null)
  fi

  # 本地没有就到 MinIO 抽
  if [[ ${#CANDIDATES[@]} -eq 0 && -n "${MINIO_AK}" && -n "${MINIO_SK}" ]]; then
    command -v mc >/dev/null 2>&1 || fail "本地无备份且 mc 不可用" 10
    local_alias="mes-verify-$$"
    mc alias set "${local_alias}" "${MINIO_ENDPOINT}" "${MINIO_AK}" "${MINIO_SK}" --quiet >/dev/null
    remote_list=$(mc ls --json "${local_alias}/${MINIO_BUCKET}/${BUCKET_PREFIX}/" 2>/dev/null | awk -F'"key":"' '{print $2}' | awk -F'"' '{print $1}' | grep -v '^$' || true)
    if [[ -n "${remote_list}" ]]; then
      pick=$(echo "${remote_list}" | shuf -n1 2>/dev/null || echo "${remote_list}" | head -n1)
      TMP_DL="/tmp/mes-verify-dl-$$"
      mkdir -p "${TMP_DL}"
      mc cp --quiet "${local_alias}/${MINIO_BUCKET}/${BUCKET_PREFIX}/${pick}" "${TMP_DL}/${pick}"
      CANDIDATES=("${TMP_DL}/${pick}")
    fi
    mc alias remove "${local_alias}" >/dev/null 2>&1 || true
  fi

  if [[ ${#CANDIDATES[@]} -eq 0 ]]; then
    notify "找不到可用备份，演练未执行"
    fail "找不到可用备份" 20
  fi

  # 从候选里随机挑一个；shuf 不可用时退化为首个
  if command -v shuf >/dev/null 2>&1; then
    DUMP_FILE=$(printf '%s\n' "${CANDIDATES[@]}" | shuf -n1)
  else
    DUMP_FILE="${CANDIDATES[0]}"
  fi
fi

log "将校验备份：${DUMP_FILE}"

# ---------- 2. 启动临时 MySQL 实例 ----------
command -v docker >/dev/null 2>&1 || fail "需要 docker 环境" 10

cleanup() {
  if [[ "${KEEP_CONTAINER}" == "true" ]]; then
    log "按需保留容器 ${VERIFY_CONTAINER}（用于人工排查）"
    return
  fi
  log "清理临时容器 ${VERIFY_CONTAINER}"
  docker rm -f "${VERIFY_CONTAINER}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

log "启动临时 MySQL 容器 ${VERIFY_CONTAINER} (port=${VERIFY_PORT})"
docker run -d --rm \
  --name "${VERIFY_CONTAINER}" \
  -e MYSQL_ROOT_PASSWORD="${VERIFY_ROOT_PASS}" \
  -e MYSQL_DATABASE="${VERIFY_DB}" \
  -p "${VERIFY_PORT}:3306" \
  --tmpfs /var/lib/mysql:rw,size=4G \
  "${VERIFY_MYSQL_IMAGE}" \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci \
  --default-authentication-plugin=mysql_native_password \
  >/dev/null

# 等待 MySQL 健康
log "等待 MySQL 就绪（最多 120 秒）"
for i in $(seq 1 60); do
  if docker exec "${VERIFY_CONTAINER}" mysqladmin ping -uroot -p"${VERIFY_ROOT_PASS}" --silent >/dev/null 2>&1; then
    log "MySQL 就绪 (第 ${i} 次探活)"
    break
  fi
  sleep 2
  if [[ "${i}" -eq 60 ]]; then
    fail "MySQL 启动超时" 30
  fi
done

# ---------- 3. 执行恢复 ----------
log "调用 mysql-restore.sh 执行恢复"
if ! bash "${SCRIPT_DIR}/mysql-restore.sh" \
        --dump="${DUMP_FILE}" \
        --target-host=127.0.0.1 \
        --target-port="${VERIFY_PORT}" \
        --target-user=root \
        --target-pass="${VERIFY_ROOT_PASS}" \
        --target-db="${VERIFY_DB}" \
        --gpg-pass="${GPG_PASS}"; then
  notify "备份恢复失败 ${DUMP_FILE}"
  fail "恢复失败" 30
fi

# ---------- 4. 数据校对 ----------
log "开始关键指标校对"
MYSQL_CLI=(docker exec "${VERIFY_CONTAINER}" mysql -uroot -p"${VERIFY_ROOT_PASS}" -Nse)

# 4.1 flyway 版本：V2.04 必须存在
flyway_check=$("${MYSQL_CLI[@]}" \
  "SELECT COUNT(*) FROM \`${VERIFY_DB}\`.flyway_schema_history WHERE version='${EXPECT_FLYWAY_VERSION}' AND success=1" \
  2>/dev/null || echo "0")
log "flyway V${EXPECT_FLYWAY_VERSION} 成功迁移条数=${flyway_check}"

# 4.2 sys_tenant 行数下限
tenant_rows=$("${MYSQL_CLI[@]}" \
  "SELECT COUNT(*) FROM \`${VERIFY_DB}\`.sys_tenant" \
  2>/dev/null || echo "0")
log "sys_tenant 行数=${tenant_rows}"

# 4.3 额外抽样：确保至少 10 张业务表存在（防止 dump 被截断）
table_count=$("${MYSQL_CLI[@]}" \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${VERIFY_DB}'" \
  2>/dev/null || echo "0")
log "业务表总数=${table_count}"

fail_reasons=()
if [[ "${flyway_check}" -lt 1 ]]; then
  fail_reasons+=("flyway V${EXPECT_FLYWAY_VERSION} 缺失")
fi
if [[ "${tenant_rows}" -lt "${EXPECT_SYS_TENANT_MIN}" ]]; then
  fail_reasons+=("sys_tenant 行数=${tenant_rows} 低于期望 ${EXPECT_SYS_TENANT_MIN}")
fi
if [[ "${table_count}" -lt 10 ]]; then
  fail_reasons+=("业务表总数=${table_count} 过少，疑似 dump 截断")
fi

if [[ ${#fail_reasons[@]} -gt 0 ]]; then
  notify "备份演练失败 ${DUMP_FILE} 原因: $(IFS=';'; echo "${fail_reasons[*]}")"
  # 保留容器便于排查
  KEEP_CONTAINER="true"
  fail "校对失败: $(IFS=';'; echo "${fail_reasons[*]}")" 40
fi

# ---------- 5. 通过：记录审计 ----------
STAMP_DIR="${BACKUP_ROOT}/verify-logs"
mkdir -p "${STAMP_DIR}"
STAMP_FILE="${STAMP_DIR}/verify_$(date +%Y%m%d_%H%M%S).log"
{
  echo "backup_file=${DUMP_FILE}"
  echo "verified_at=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  echo "flyway_v${EXPECT_FLYWAY_VERSION}_count=${flyway_check}"
  echo "sys_tenant_rows=${tenant_rows}"
  echo "business_tables=${table_count}"
  echo "verdict=PASS"
} > "${STAMP_FILE}"

log "演练通过，审计记录：${STAMP_FILE}"
exit 0
