#!/bin/sh
cd /tmp/mes_sql_v3 || exit 1
printf 'script|lines|sizeBytes|createTable|alterTable|insertInto|updateStmt|createIndex\n'
for f in V*.sql; do
  lines=$(wc -l < "$f")
  size=$(stat -c %s "$f")
  ct=$(grep -c '^CREATE TABLE' "$f")
  at=$(grep -cE '^ALTER TABLE' "$f")
  ii=$(grep -cE '^INSERT INTO' "$f")
  ui=$(grep -cE '^UPDATE ' "$f")
  ci=$(grep -cE '^(CREATE INDEX|CREATE UNIQUE INDEX)' "$f")
  printf '%s|%s|%s|%s|%s|%s|%s|%s\n' "$f" "$lines" "$size" "$ct" "$at" "$ii" "$ui" "$ci"
done | sort
