#!/bin/sh
# m8_run_v3.sh - Run all V*.sql migrations against mes_m8_v3_test on titan-mysql
set -u
DB=mes_m8_v3_test
SQL_DIR=/tmp/mes_sql_v3
LOG=/tmp/mes_sql_v3_run.log
CSV=/tmp/mes_sql_v3_summary.csv

: > "$LOG"
printf 'script,status,exitCode,elapsedMs,linesOut,linesErr\n' > "$CSV"

cd "$SQL_DIR" || { echo "cannot cd $SQL_DIR"; exit 2; }

SCRIPTS=$(ls V*.sql | LC_ALL=C sort)

ABORT=0
for s in $SCRIPTS; do
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
  [ $RC -ne 0 ] && STATUS="FAILED"
  printf '%s,%s,%d,%d,%d,%d\n' "$s" "$STATUS" "$RC" "$ELAPSED" "$LINES_OUT" "$LINES_ERR" >> "$CSV"
  echo "$OUT" >> "$LOG"
  echo "[$s] rc=$RC elapsed=${ELAPSED}ms status=$STATUS" >> "$LOG"
  echo "[$s] rc=$RC elapsed=${ELAPSED}ms status=$STATUS"
  if [ $RC -ne 0 ]; then
    echo "[!] FAIL on $s — continuing to surface all errors" >> "$LOG"
    ABORT=1
    # do not break; continue to capture all
  fi
done
echo "===== END $(date -u +%Y-%m-%dT%H:%M:%SZ) =====" >> "$LOG"
exit $ABORT
