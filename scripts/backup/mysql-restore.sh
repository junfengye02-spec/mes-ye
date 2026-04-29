#!/bin/bash
# ==============================================================================
# MES MySQL 恢复脚本（支持 PITR - Point In Time Recovery）
# ------------------------------------------------------------------------------
# 使用场景：
#   1. 灾难恢复：全量备份 + binlog 回放到指定时间点
#   2. 演练：随机抽取一个备份恢复到临时实例（见 verify-backup.sh）
#
# 使用示例：
#   # 恢复最新日备到现有 MySQL
#   bash mysql-restore.sh --dump=/backup/mysql/full/mes_daily_20260421_030000.sql.gz.gpg
#
#   # 恢复到指定时间点（PITR）
#   bash mysql-restore.sh \
#     --dump=/backup/mysql/full/mes_daily_20260421_030000.sql.gz.gpg \
#     --binlog-dir=/backup/mysql/binlog \
#     --target-time="2026-04-21 10:45:00"
#
#   # 恢复到临时 MySQL 实例（不覆盖生产）
#   bash mysql-restore.sh --dump=... --target-host=127.0.0.1 --target-port=3399 --target-db=mes_verify
#
# 退出码：
#   0 成功 / 10 参数校验失败 / 20 解密失败 / 30 还原 dump 失败 / 40 binlog 回放失败
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -f "${SCRIPT_DIR}/.env" ]]; then
  # shellcheck disable=SC1091
  set -a; . "${SCRIPT_DIR}/.env"; set +a
fi

# ---------- 默认值 ----------
DUMP_FILE=""
BINLOG_DIR=""
TARGET_TIME=""
TARGET_HOST="${MYSQL_HOST:-mysql-primary}"
TARGET_PORT="${MYSQL_PORT:-3306}"
TARGET_USER="${MYSQL_USER:-root}"
TARGET_PASS="${MYSQL_PASS:-${MYSQL_PASSWORD:-}}"
TARGET_DB="${MYSQL_DB:-${MYSQL_DATABASE:-mes}}"
GPG_PASS="${BACKUP_GPG_PASSPHRASE:-}"
DRY_RUN="false"
FROM_REMOTE=""

usage() {
  sed -n '2,30p' "$0"
  exit 0
}

# ---------- 参数解析 ----------
for arg in "$@"; do
  case "$arg" in
    --dump=*)        DUMP_FILE="${arg#*=}" ;;
    --binlog-dir=*)  BINLOG_DIR="${arg#*=}" ;;
    --target-time=*) TARGET_TIME="${arg#*=}" ;;
    --target-host=*) TARGET_HOST="${arg#*=}" ;;
    --target-port=*) TARGET_PORT="${arg#*=}" ;;
    --target-user=*) TARGET_USER="${arg#*=}" ;;
    --target-pass=*) TARGET_PASS="${arg#*=}" ;;
    --target-db=*)   TARGET_DB="${arg#*=}" ;;
    --gpg-pass=*)    GPG_PASS="${arg#*=}" ;;
    --from-remote=*) FROM_REMOTE="${arg#*=}" ;;
    --dry-run)       DRY_RUN="true" ;;
    --help|-h)       usage ;;
    *) echo "未知参数: $arg" >&2; exit 10 ;;
  esac
done

log() { printf '[%s] [restore] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }

if [[ -z "${DUMP_FILE}" && -z "${FROM_REMOTE}" ]]; then
  echo "必须指定 --dump=<文件> 或 --from-remote=<alias/bucket/path>" >&2
  usage
fi
if [[ -z "${TARGET_PASS}" ]]; then
  echo "未设置 target 密码，拒绝执行" >&2
  exit 10
fi

# ---------- 临时工作目录（脚本退出自动清理） ----------
WORK_DIR="$(mktemp -d -t mysql-restore-XXXXXX)"
cleanup() { rm -rf "${WORK_DIR}"; }
trap cleanup EXIT

# ---------- 1. 如指定 --from-remote，先从 MinIO 拉取 ----------
if [[ -n "${FROM_REMOTE}" ]]; then
  command -v mc >/dev/null 2>&1 || { log "mc 缺失"; exit 10; }
  log "从 MinIO 拉取 ${FROM_REMOTE}"
  DUMP_FILE="${WORK_DIR}/$(basename "${FROM_REMOTE}")"
  mc cp --quiet "${FROM_REMOTE}" "${DUMP_FILE}" || { log "mc cp 拉取失败"; exit 20; }
fi

if [[ ! -f "${DUMP_FILE}" ]]; then
  log "dump 文件不存在: ${DUMP_FILE}"
  exit 10
fi

# ---------- 2. 按需解密 ----------
INPUT_FILE="${DUMP_FILE}"
if [[ "${INPUT_FILE}" == *.gpg ]]; then
  if [[ -z "${GPG_PASS}" ]]; then
    log "备份是 .gpg 加密文件，但未指定 --gpg-pass 或 BACKUP_GPG_PASSPHRASE"
    exit 10
  fi
  command -v gpg >/dev/null 2>&1 || { log "gpg 缺失"; exit 10; }
  DEC_FILE="${WORK_DIR}/$(basename "${INPUT_FILE%.gpg}")"
  log "解密 -> ${DEC_FILE}"
  if ! gpg --batch --yes --pinentry-mode loopback \
           --passphrase "${GPG_PASS}" \
           --decrypt --output "${DEC_FILE}" "${INPUT_FILE}" >/dev/null 2>&1; then
    log "gpg 解密失败（口令错误？）"
    exit 20
  fi
  INPUT_FILE="${DEC_FILE}"
fi

# ---------- 3. 按需解压 ----------
if [[ "${INPUT_FILE}" == *.gz ]]; then
  SQL_FILE="${WORK_DIR}/$(basename "${INPUT_FILE%.gz}")"
  log "解压 -> ${SQL_FILE}"
  gunzip -c "${INPUT_FILE}" > "${SQL_FILE}"
else
  SQL_FILE="${INPUT_FILE}"
fi

log "准备恢复到 ${TARGET_USER}@${TARGET_HOST}:${TARGET_PORT}/${TARGET_DB}"
if [[ "${DRY_RUN}" == "true" ]]; then
  log "DRY-RUN 已开启，仅解析文件不执行实际 mysql import。SQL 文件: ${SQL_FILE}"
  # 粗略输出一下 head 用于排查
  head -n 20 "${SQL_FILE}" || true
  exit 0
fi

# ---------- 4. 目标库预处理（drop+create） ----------
log "目标库 ${TARGET_DB} 将被 drop 后重建（务必确认已做最终备份）"
mysql \
  --host="${TARGET_HOST}" \
  --port="${TARGET_PORT}" \
  --user="${TARGET_USER}" \
  --password="${TARGET_PASS}" \
  -e "DROP DATABASE IF EXISTS \`${TARGET_DB}\`; CREATE DATABASE \`${TARGET_DB}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# ---------- 5. 导入全量 dump ----------
log "开始导入 dump (大约需要数分钟到数十分钟，取决于库大小)"
if ! mysql \
      --host="${TARGET_HOST}" \
      --port="${TARGET_PORT}" \
      --user="${TARGET_USER}" \
      --password="${TARGET_PASS}" \
      --default-character-set=utf8mb4 \
      "${TARGET_DB}" < "${SQL_FILE}"; then
  log "dump 导入失败"
  exit 30
fi
log "全量导入完成"

# ---------- 6. 提取 dump 的 binlog 位点（用于 PITR 回放起点） ----------
# mysqldump --master-data=2 会在注释中写入：
#   -- CHANGE MASTER TO MASTER_LOG_FILE='mysql-bin.000123', MASTER_LOG_POS=456;
# 我们解析这行作为 binlog 回放起点
BINLOG_FILE=""
BINLOG_POS=""
if grep -E '^-- CHANGE MASTER TO MASTER_LOG_FILE' "${SQL_FILE}" >/dev/null 2>&1; then
  CHANGE_LINE=$(grep -E '^-- CHANGE MASTER TO MASTER_LOG_FILE' "${SQL_FILE}" | head -n1)
  BINLOG_FILE=$(echo "${CHANGE_LINE}" | sed -E "s/.*MASTER_LOG_FILE='([^']+)'.*/\1/")
  BINLOG_POS=$(echo "${CHANGE_LINE}"  | sed -E "s/.*MASTER_LOG_POS=([0-9]+).*/\1/")
  log "dump 起点 binlog=${BINLOG_FILE} pos=${BINLOG_POS}"
fi

# ---------- 7. PITR：按指定时间回放 binlog ----------
if [[ -n "${BINLOG_DIR}" && -n "${TARGET_TIME}" ]]; then
  if [[ ! -d "${BINLOG_DIR}" ]]; then
    log "binlog 目录不存在: ${BINLOG_DIR}"
    exit 10
  fi
  command -v mysqlbinlog >/dev/null 2>&1 || { log "mysqlbinlog 缺失"; exit 10; }

  # 如果 binlog 是 tar.gz，需要先解包到工作目录
  BINLOG_WORK="${WORK_DIR}/binlogs"
  mkdir -p "${BINLOG_WORK}"
  shopt -s nullglob
  for t in "${BINLOG_DIR}"/*.tar.gz; do
    log "解包 binlog 归档 ${t}"
    tar -xzf "${t}" -C "${BINLOG_WORK}"
  done
  # 已经是散文件时也复制一份
  for f in "${BINLOG_DIR}"/mysql-bin.[0-9]*; do
    [[ -f "${f}" ]] && cp "${f}" "${BINLOG_WORK}/"
  done
  shopt -u nullglob

  # 收集 binlog 列表：按文件名顺序，起点从 BINLOG_FILE 开始
  mapfile -t ALL_BINLOGS < <(ls -1 "${BINLOG_WORK}" | grep -E '^mysql-bin\.[0-9]+$' | sort)
  if [[ ${#ALL_BINLOGS[@]} -eq 0 ]]; then
    log "工作目录里没有可用的 binlog 文件"
    exit 40
  fi
  APPLY_LIST=()
  START_FOUND=0
  for b in "${ALL_BINLOGS[@]}"; do
    if [[ -n "${BINLOG_FILE}" && "${START_FOUND}" -eq 0 ]]; then
      if [[ "${b}" == "${BINLOG_FILE}" ]]; then START_FOUND=1; fi
    else
      # 如果没有起点信息，就全部回放（保守）
      START_FOUND=1
    fi
    if [[ "${START_FOUND}" -eq 1 ]]; then
      APPLY_LIST+=("${BINLOG_WORK}/${b}")
    fi
  done

  log "开始回放 binlog 到 ${TARGET_TIME}，共 ${#APPLY_LIST[@]} 个文件"
  # mysqlbinlog 管道给 mysql；--stop-datetime 精确到秒
  # 对第一个文件使用 --start-position，后续全量回放
  TMP_SQL="${WORK_DIR}/pitr.sql"
  if [[ -n "${BINLOG_POS}" && -n "${BINLOG_FILE}" ]]; then
    mysqlbinlog \
      --start-position="${BINLOG_POS}" \
      --stop-datetime="${TARGET_TIME}" \
      --database="${TARGET_DB}" \
      "${BINLOG_WORK}/${BINLOG_FILE}" > "${TMP_SQL}"
    # 起点之后的所有 binlog
    for f in "${APPLY_LIST[@]}"; do
      if [[ "${f}" == "${BINLOG_WORK}/${BINLOG_FILE}" ]]; then continue; fi
      mysqlbinlog \
        --stop-datetime="${TARGET_TIME}" \
        --database="${TARGET_DB}" \
        "${f}" >> "${TMP_SQL}"
    done
  else
    # 没位点信息：保守全量回放
    mysqlbinlog \
      --stop-datetime="${TARGET_TIME}" \
      --database="${TARGET_DB}" \
      "${APPLY_LIST[@]}" > "${TMP_SQL}"
  fi

  if ! mysql \
        --host="${TARGET_HOST}" \
        --port="${TARGET_PORT}" \
        --user="${TARGET_USER}" \
        --password="${TARGET_PASS}" \
        "${TARGET_DB}" < "${TMP_SQL}"; then
    log "binlog 回放失败"
    exit 40
  fi
  log "binlog 回放完成，数据库已恢复到时间点 ${TARGET_TIME}"
fi

# ---------- 8. 校验恢复结果：若存在 V2.04__ 迁移标记则抽样校对 ----------
log "恢复后自检：当前已执行的 flyway 迁移数量 / sys_tenant 行数"
mysql \
  --host="${TARGET_HOST}" \
  --port="${TARGET_PORT}" \
  --user="${TARGET_USER}" \
  --password="${TARGET_PASS}" \
  -Nse "SELECT CONCAT('flyway_total=', COUNT(*)) FROM flyway_schema_history; \
        SELECT CONCAT('sys_tenant_rows=', COUNT(*)) FROM sys_tenant;" \
  "${TARGET_DB}" 2>/dev/null || log "flyway/sys_tenant 表不存在或暂不可读，请人工确认"

log "恢复完成"
exit 0
