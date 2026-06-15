#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

FRONTEND_PORT="${MES_FRONTEND_PORT:-3000}"
BACKEND_PORT="${MES_BACKEND_PORT:-9091}"
PROXY_PORT="${MES_PROXY_PORT:-8088}"
STOP_DOCKER_DEPS="${MES_STOP_DOCKER_DEPS:-0}"

GREEN=$'\033[0;32m'
YELLOW=$'\033[1;33m'
NC=$'\033[0m'

log() {
  printf '%s\n' "${GREEN}[mes-stop]${NC} $*"
}

warn() {
  printf '%s\n' "${YELLOW}[mes-stop]${NC} $*"
}

port_status() {
  local name="$1"
  local port="$2"

  if command -v nc >/dev/null 2>&1 && nc -z 127.0.0.1 "$port" >/dev/null 2>&1; then
    warn "$name port $port is still open"
  else
    log "$name port $port is stopped"
  fi
}

compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    printf 'docker compose'
  elif command -v docker-compose >/dev/null 2>&1; then
    printf 'docker-compose'
  else
    return 1
  fi
}

log "Stopping MES local proxy stack"
"$PROJECT_ROOT/scripts/local-proxy/stop-mes-local-proxy.sh"

if [[ "$STOP_DOCKER_DEPS" == "1" ]]; then
  if command -v docker >/dev/null 2>&1; then
    if dc="$(compose_cmd)"; then
      log "Stopping MES Docker dependencies: mysql redis minio"
      (
        cd "$PROJECT_ROOT"
        # shellcheck disable=SC2086
        $dc stop mysql redis minio
      )
    else
      warn "Docker Compose not found; skipping Docker dependency stop"
    fi
  else
    warn "Docker not found; skipping Docker dependency stop"
  fi
fi

port_status "MES frontend" "$FRONTEND_PORT"
port_status "MES backend" "$BACKEND_PORT"
port_status "MES local nginx proxy" "$PROXY_PORT"

log "MES stop command finished"
