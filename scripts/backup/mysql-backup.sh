#!/bin/bash
# ==============================================================================
# MES MySQL 备份脚本（Linux/Bash 版）
# ------------------------------------------------------------------------------
# 功能概述：
#   1. 全量备份（mysqldump --single-transaction --routines --triggers --events）
#   2. 可选 binlog 增量同步（--mode=binlog）
#   3. gzip 压缩 + gpg 对称加密
#   4. 通过 mc (MinIO client) 把本地备份异地同步到 MinIO / OSS
#   5. 本地保留策略：日备 30 份、小时 binlog 7 天、周备 12 份、月备永久
#
# 调用模式：
#   bash mysql-backup.sh --mode=full      # 每日 03:00 执行
#   bash mysql-backup.sh --mode=binlog    # 每 15 分钟执行
#   bash mysql-backup.sh --mode=weekly    # 每周日 04:00 执行
#   bash mysql-backup.sh --mode=monthly   # 每月 1 日 05:00 执行
#
# 关键环境变量（从 .env 或系统环境读入，缺省值仅供本地调试）：
#   MYSQL_HOST / MYSQL_PORT / MYSQL_USER / MYSQL_PASS / MYSQL_DB
#   BACKUP_ROOT            （本地备份根目录，默认 /backup/mysql）
#   BACKUP_GPG_PASSPHRASE  （加密口令，必填，否则跳过加密）
#   MINIO_ENDPOINT / MINIO_AK / MINIO_SK / MINIO_BUCKET
#   BACKUP_NOTIFY_WEBHOOK  （可选，失败时钉钉 / 飞书 webhook）
#
# 退出码：
#   0 成功
#   10 环境变量校验失败
#   20 mysqldump 失败
#   30 加密失败
#   40 异地同步失败
# ==============================================================================

set -euo pipefail

# ---------- 1. 默认值与环境变量装载 ----------
# 允许通过 .env 文件注入敏感信息，脚本同目录优先
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -f "${SCRIPT_DIR}/.env" ]]; then
  # shellcheck disable=SC1091
  set -a; . "${SCRIPT_DIR}/.env"; set +a
fi

MYSQL_HOST="${MYSQL_HOST:-mysql-primary}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASS="${MYSQL_PASS:-${MYSQL_PASSWORD:-}}"
MYSQL_DB="${MYSQL_DB:-${MYSQL_DATABASE:-mes}}"

BACKUP_ROOT="${BACKUP_ROOT:-/backup/mysql}"
BACKUP_GPG_PASSPHRASE="${BACKUP_GPG_PASSPHRASE:-}"

MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://minio:9000}"
MINIO_AK="${MINIO_AK:-${MINIO_ROOT_USER:-}}"
MINIO_SK="${MINIO_SK:-${MINIO_ROOT_PASSWORD:-}}"
MINIO_BUCKET="${MINIO_BUCKET:-mes-backups}"

BACKUP_NOTIFY_WEBHOOK="${BACKUP_NOTIFY_WEBHOOK:-}"
BACKUP_MODE="full"

# 本地保留策略（天 / 份）
RETAIN_DAILY=30
RETAIN_BINLOG_DAYS=7
RETAIN_WEEKLY=12
# 月备保留永久（0 = 不清理）
RETAIN_MONTHLY=0

# ---------- 2. 解析参数 ----------
for arg in "$@"; do
  case "$arg" in
    --mode=*) BACKUP_MODE="${arg#*=}" ;;
    --help|-h)
      sed -n '2,30p' "$0"
      exit 0
      ;;
    *) echo "未知参数: $arg" >&2; exit 1 ;;
  esac
done

# ---------- 3. 日志与工具函数 ----------
LOG_FILE="${BACKUP_ROOT}/logs/backup_$(date +%Y%m%d).log"
mkdir -p "$(dirname "${LOG_FILE}")"

log() {
  # 同时写 stdout 和日志文件，便于 docker logs + 本地排查
  printf '[%s] [%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "${BACKUP_MODE}" "$*" | tee -a "${LOG_FILE}"
}

notify_failure() {
  local reason="$1"
  log "告警：${reason}"
  if [[ -n "${BACKUP_NOTIFY_WEBHOOK}" ]]; then
    # 发送到钉钉/飞书 webhook，失败不阻塞主流程
    curl -s -m 10 -X POST -H "Content-Type: application/json" \
      -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"[MES备份告警] mode=${BACKUP_MODE} ${reason}\"}}" \
      "${BACKUP_NOTIFY_WEBHOOK}" || true
  fi
}

require() {
  # 必须的命令存在性检查
  local cmd="$1"
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    log "命令缺失: ${cmd}"
    notify_failure "依赖缺失 ${cmd}"
    exit 10
  fi
}

# ---------- 4. 前置校验 ----------
require mysqldump
require gzip
require find

if [[ -z "${MYSQL_PASS}" ]]; then
  log "MYSQL_PASS / MYSQL_PASSWORD 未设置，拒绝执行"
  exit 10
fi

mkdir -p "${BACKUP_ROOT}/full" "${BACKUP_ROOT}/binlog" "${BACKUP_ROOT}/weekly" "${BACKUP_ROOT}/monthly"

# ---------- 5. 全量备份 ----------
do_full_backup() {
  local target_dir="$1"
  local tag="$2"
  local ts; ts="$(date +%Y%m%d_%H%M%S)"
  local dump_file="${target_dir}/${MYSQL_DB}_${tag}_${ts}.sql"
  local gz_file="${dump_file}.gz"
  local final_file="${gz_file}"

  log "开始全量备份 -> ${gz_file}"

  # --single-transaction 保证一致性快照，对 InnoDB 无锁
  # --master-data=2 记录 binlog 位点，PITR 必备
  # --set-gtid-purged=ON 适配 GTID 模式（MySQL 8 默认）
  if ! mysqldump \
        --host="${MYSQL_HOST}" \
        --port="${MYSQL_PORT}" \
        --user="${MYSQL_USER}" \
        --password="${MYSQL_PASS}" \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        --master-data=2 \
        --set-gtid-purged=ON \
        --default-character-set=utf8mb4 \
        "${MYSQL_DB}" | gzip -c > "${gz_file}"; then
    log "mysqldump 失败"
    notify_failure "mysqldump 失败"
    rm -f "${gz_file}"
    exit 20
  fi

  # ---------- 5.1 对称加密 ----------
  if [[ -n "${BACKUP_GPG_PASSPHRASE}" ]]; then
    require gpg
    local enc_file="${gz_file}.gpg"
    # --batch + --yes 非交互；--cipher-algo AES256 强度 AES-256
    if ! gpg --batch --yes --pinentry-mode loopback \
             --cipher-algo AES256 \
             --passphrase "${BACKUP_GPG_PASSPHRASE}" \
             --symmetric --output "${enc_file}" "${gz_file}"; then
      log "gpg 加密失败"
      notify_failure "gpg 加密失败"
      exit 30
    fi
    rm -f "${gz_file}"
    final_file="${enc_file}"
    log "加密完成 -> ${final_file}"
  else
    log "未设置 BACKUP_GPG_PASSPHRASE，跳过加密（仅建议测试环境）"
  fi

  local size_human; size_human="$(du -h "${final_file}" | cut -f1)"
  log "本地备份完成 size=${size_human}"

  # 记录本次备份文件供上层同步流程使用
  echo "${final_file}"
}

# ---------- 6. binlog 增量同步 ----------
do_binlog_sync() {
  local target_dir="${BACKUP_ROOT}/binlog"
  local ts; ts="$(date +%Y%m%d_%H%M%S)"
  local tmp_dir="${target_dir}/tmp_${ts}"
  mkdir -p "${tmp_dir}"

  log "开始 binlog 增量同步"
  require mysql
  require mysqlbinlog

  # 从 MySQL 查出当前已切换的 binlog 文件列表
  local binlog_list
  if ! binlog_list=$(mysql \
        --host="${MYSQL_HOST}" \
        --port="${MYSQL_PORT}" \
        --user="${MYSQL_USER}" \
        --password="${MYSQL_PASS}" \
        -Nse 'SHOW BINARY LOGS' 2>/dev/null | awk '{print $1}'); then
    log "SHOW BINARY LOGS 失败，确认账号是否有 REPLICATION CLIENT 权限"
    notify_failure "binlog 查询失败"
    exit 20
  fi

  # 逐个 binlog 用 mysqlbinlog --read-from-remote-server 拉取
  local count=0
  while IFS= read -r bl; do
    [[ -z "${bl}" ]] && continue
    local out="${tmp_dir}/${bl}.sql"
    if mysqlbinlog \
          --read-from-remote-server \
          --host="${MYSQL_HOST}" \
          --port="${MYSQL_PORT}" \
          --user="${MYSQL_USER}" \
          --password="${MYSQL_PASS}" \
          --raw \
          --result-file="${tmp_dir}/" \
          "${bl}" >/dev/null 2>&1; then
      count=$((count+1))
    else
      log "binlog 拉取失败: ${bl}"
    fi
  done <<< "${binlog_list}"

  # 打包并上传
  local tarball="${target_dir}/binlog_${ts}.tar.gz"
  (cd "${tmp_dir}" && tar -czf "${tarball}" .) || {
    log "binlog 打包失败"
    exit 20
  }
  rm -rf "${tmp_dir}"
  log "binlog 同步完成 count=${count} -> ${tarball}"

  echo "${tarball}"
}

# ---------- 7. 异地同步到 MinIO/OSS ----------
sync_to_remote() {
  local local_file="$1"
  local remote_prefix="$2"

  if [[ -z "${MINIO_AK}" || -z "${MINIO_SK}" ]]; then
    log "未配置 MINIO_AK/SK，跳过异地同步（仅建议测试环境）"
    return 0
  fi

  require mc
  # 动态 alias：避免把密钥写入 ~/.mc/config.json
  local alias="mes-backup-$$"
  mc alias set "${alias}" "${MINIO_ENDPOINT}" "${MINIO_AK}" "${MINIO_SK}" --quiet >/dev/null

  # bucket 自动创建（如果不存在）
  if ! mc ls "${alias}/${MINIO_BUCKET}" >/dev/null 2>&1; then
    log "bucket ${MINIO_BUCKET} 不存在，创建"
    mc mb "${alias}/${MINIO_BUCKET}" --ignore-existing >/dev/null || true
  fi

  local remote_path="${alias}/${MINIO_BUCKET}/${remote_prefix}/$(basename "${local_file}")"
  log "异地同步 -> ${remote_path}"

  if ! mc cp --quiet "${local_file}" "${remote_path}"; then
    log "mc cp 失败"
    notify_failure "异地同步失败 ${local_file}"
    mc alias remove "${alias}" >/dev/null 2>&1 || true
    exit 40
  fi
  mc alias remove "${alias}" >/dev/null 2>&1 || true
}

# ---------- 8. 本地保留策略清理 ----------
cleanup_local() {
  local dir="$1"
  local retention_days="$2"
  [[ "${retention_days}" -le 0 ]] && return 0

  log "清理 ${dir} 中 ${retention_days} 天前的文件"
  find "${dir}" -maxdepth 1 -type f \( -name "*.gz" -o -name "*.gpg" -o -name "*.tar.gz" \) \
    -mtime +"${retention_days}" -print -delete | sed 's/^/  removed: /' | tee -a "${LOG_FILE}" || true
}

# ---------- 9. 主流程分派 ----------
case "${BACKUP_MODE}" in
  full)
    FILE="$(do_full_backup "${BACKUP_ROOT}/full" "daily" | tail -n1)"
    sync_to_remote "${FILE}" "daily"
    cleanup_local "${BACKUP_ROOT}/full" "${RETAIN_DAILY}"
    ;;
  binlog)
    FILE="$(do_binlog_sync | tail -n1)"
    sync_to_remote "${FILE}" "binlog"
    cleanup_local "${BACKUP_ROOT}/binlog" "${RETAIN_BINLOG_DAYS}"
    ;;
  weekly)
    FILE="$(do_full_backup "${BACKUP_ROOT}/weekly" "weekly" | tail -n1)"
    sync_to_remote "${FILE}" "weekly"
    # 按份数保留：按文件 mtime 倒序，保留 RETAIN_WEEKLY 个
    ls -1t "${BACKUP_ROOT}/weekly"/*.{gz,gpg} 2>/dev/null \
      | tail -n +"$((RETAIN_WEEKLY + 1))" \
      | xargs -r rm -f || true
    ;;
  monthly)
    FILE="$(do_full_backup "${BACKUP_ROOT}/monthly" "monthly" | tail -n1)"
    sync_to_remote "${FILE}" "monthly"
    cleanup_local "${BACKUP_ROOT}/monthly" "${RETAIN_MONTHLY}"
    ;;
  *)
    log "未知 mode=${BACKUP_MODE}，可选: full|binlog|weekly|monthly"
    exit 1
    ;;
esac

log "备份流程完成"
exit 0
