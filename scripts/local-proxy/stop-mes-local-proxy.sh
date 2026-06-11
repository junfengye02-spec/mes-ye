#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
NGINX_CONF="$PROJECT_ROOT/runtime/nginx/mes-local-proxy.conf"
PID_FILE="$PROJECT_ROOT/runtime/nginx/mes-nginx.pid"
NGINX_PREFIX="$PROJECT_ROOT/runtime/nginx/prefix"
BACKEND_LABEL="${MES_BACKEND_LAUNCHD_LABEL:-com.mesye.local.backend}"
FRONTEND_LABEL="${MES_FRONTEND_LAUNCHD_LABEL:-com.mesye.local.frontend}"
BACKEND_SCREEN_SESSION="${MES_BACKEND_SCREEN_SESSION:-mesye-backend}"
FRONTEND_SCREEN_SESSION="${MES_FRONTEND_SCREEN_SESSION:-mesye-frontend}"

launchctl_available() {
  [[ "$(uname -s)" == "Darwin" ]] && command -v launchctl >/dev/null 2>&1
}

launchctl_domain() {
  printf 'gui/%s' "$(id -u)"
}

stop_launchctl_job() {
  local label="$1"
  local domain
  domain="$(launchctl_domain)"
  launchctl bootout "$domain/$label" >/dev/null 2>&1 || true
}

stop_screen_job() {
  local session="$1"
  if command -v screen >/dev/null 2>&1; then
    screen -S "$session" -X quit >/dev/null 2>&1 || true
  fi
}

stop_pid_file() {
  local name="$1"
  local pid_file="$2"
  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" >/dev/null 2>&1; then
    kill -TERM "$(cat "$pid_file")" >/dev/null 2>&1 || true
    echo "Stopped $name."
  fi
}

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" >/dev/null 2>&1; then
  mkdir -p "$NGINX_PREFIX/logs"
  nginx -p "$NGINX_PREFIX" -s stop -c "$NGINX_CONF"
  echo "Stopped MES local nginx proxy."
else
  echo "MES local nginx proxy is not running."
fi

if launchctl_available; then
  stop_launchctl_job "$BACKEND_LABEL"
  stop_launchctl_job "$FRONTEND_LABEL"
fi

stop_screen_job "$BACKEND_SCREEN_SESSION"
stop_screen_job "$FRONTEND_SCREEN_SESSION"

stop_pid_file "MES backend" "$PROJECT_ROOT/runtime/logs/mes-admin.pid"
stop_pid_file "MES frontend" "$PROJECT_ROOT/runtime/logs/mes-frontend.pid"
