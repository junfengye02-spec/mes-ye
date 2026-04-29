#!/bin/sh
# m8_run_v3_continue.sh - Resume from a given start script, run through end
# usage: sh m8_run_v3_continue.sh START_SCRIPT END_PATTERN
# env:   START="V1.12" END="V1.20"  (regex fragment for mid-range runs)

set -u
DB=mes_m8_v3_test
SQL_DIR=/tmp/mes_sql_v3
LOG=/tmp/mes_sql_v3_run.log
CSV=/tmp/mes_sql_v3_summary.csv
START_PATTERN=${START:-V1.00}
END_PATTERN=${END:-V99_99}

cd "$SQL_DIR" || { echo "cannot cd $SQL_DIR"; exit 2; }

SCRIPTS=$(ls V*.sql | LC_ALL=C sort)
STARTED=0
for s in $SCRIPTS; do
  case "$s" in
    ${START_PATTERN}*) STARTED=1 ;;
  esac
  [ $STARTED -eq 0 ] && continue

  echo "" >> "$LOG"
  echo "===== SCRIPT: $s =====" >> "$LOG"
  START_MS=$(date +%s%3N)
  OUT=$(mysql --default-character-set=utf8mb4 -uroot -p12345678 --connect-timeout=60 \
       --protocol=TCP -h127.0.0.1 -P3306 "$DB" < "$s" 2>&1)
  RC=$?
  END_MS=$(date +%s%3N)
  ELAPSED=$((END_MS-START_MS))
  LINES_OUT=$(printf "%s" "$OUT" | grep -cv "password on the command line")
  LINES_ERR=$(printf "%s" "$OUT" | grep -ci "error")
  STATUS="SUCCESS"
  if [ $RC -ne 0 ]; then STATUS="FAILED"; fi
  printf '"%s","%s","%d","%d","%d","%d"\n' "$s" "$STATUS" "$RC" "$ELAPSED" "$LINES_OUT" "$LINES_ERR" >> "$CSV"
  echo "$OUT" >> "$LOG"
  echo "[$s] rc=$RC elapsed=${ELAPSED}ms status=$STATUS"

  case "$s" in
    ${END_PATTERN}*) break ;;
  esac
done
echo "BATCH DONE"
