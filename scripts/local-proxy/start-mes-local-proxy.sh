#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RUNTIME_DIR="$PROJECT_ROOT/runtime"
NGINX_DIR="$RUNTIME_DIR/nginx"
NGINX_PREFIX="$NGINX_DIR/prefix"
LOG_DIR="$RUNTIME_DIR/logs"
BIN_DIR="$RUNTIME_DIR/bin"
LAUNCHD_DIR="$RUNTIME_DIR/launchd"
PID_FILE="$NGINX_DIR/mes-nginx.pid"
NGINX_CONF="$NGINX_DIR/mes-local-proxy.conf"

DOMAIN="${MES_DOMAIN:-mesmac.0000238.xyz}"
LISTEN_HOST="${MES_PROXY_LISTEN_HOST:-127.0.0.1}"
LISTEN_PORT="${MES_PROXY_PORT:-8088}"
FRONTEND_PORT="${MES_FRONTEND_PORT:-3000}"
BACKEND_PORT="${MES_BACKEND_PORT:-9091}"
BACKEND_HEALTH_PATH="${MES_BACKEND_HEALTH_PATH:-/api/actuator/health}"
BACKEND_JAR="${MES_BACKEND_JAR:-$PROJECT_ROOT/mes-backend/mes-admin/target/mes-admin-1.1.0.jar}"
BACKEND_JAVA_OPTS="${MES_BACKEND_JAVA_OPTS:-}"
BACKEND_RABBIT_HEALTH_ENABLED="${MES_MANAGEMENT_HEALTH_RABBIT_ENABLED:-false}"
BACKEND_HEALTH_SHOW_DETAILS="${MES_MANAGEMENT_HEALTH_SHOW_DETAILS:-always}"
BACKEND_LABEL="${MES_BACKEND_LAUNCHD_LABEL:-com.mesye.local.backend}"
FRONTEND_LABEL="${MES_FRONTEND_LAUNCHD_LABEL:-com.mesye.local.frontend}"
BACKEND_SCREEN_SESSION="${MES_BACKEND_SCREEN_SESSION:-mesye-backend}"
FRONTEND_SCREEN_SESSION="${MES_FRONTEND_SCREEN_SESSION:-mesye-frontend}"
START_BACKEND="${MES_START_BACKEND:-1}"
START_FRONTEND="${MES_START_FRONTEND:-1}"
START_DOCKER_DEPS="${MES_START_DOCKER_DEPS:-0}"
WAIT_TIMEOUT="${MES_WAIT_TIMEOUT:-120}"

GREEN=$'\033[0;32m'
YELLOW=$'\033[1;33m'
RED=$'\033[0;31m'
NC=$'\033[0m'

log() {
  printf '%s\n' "${GREEN}[mes-proxy]${NC} $*"
}

warn() {
  printf '%s\n' "${YELLOW}[mes-proxy]${NC} $*"
}

fail() {
  printf '%s\n' "${RED}[mes-proxy]${NC} $*" >&2
  exit 1
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing command: $1"
}

port_is_open() {
  local host="$1"
  local port="$2"
  nc -z "$host" "$port" >/dev/null 2>&1
}

launchctl_available() {
  [[ "$(uname -s)" == "Darwin" ]] && command -v launchctl >/dev/null 2>&1
}

screen_available() {
  command -v screen >/dev/null 2>&1
}

shell_quote() {
  printf '%q' "$1"
}

start_screen_job() {
  local session="$1"
  local launcher="$2"
  local log_file="$3"
  local q_launcher q_log_file

  q_launcher="$(shell_quote "$launcher")"
  q_log_file="$(shell_quote "$log_file")"

  screen -S "$session" -X quit >/dev/null 2>&1 || true
  screen -dmS "$session" /bin/bash -lc "exec $q_launcher >> $q_log_file 2>&1"
}

launchctl_domain() {
  printf 'gui/%s' "$(id -u)"
}

restart_launchctl_job() {
  local label="$1"
  local plist="$2"
  local domain
  domain="$(launchctl_domain)"

  launchctl bootout "$domain/$label" >/dev/null 2>&1 || true
  launchctl bootstrap "$domain" "$plist"
  launchctl kickstart -k "$domain/$label" >/dev/null 2>&1 || true
}

http_ready() {
  local url="$1"
  curl -fsS --max-time 3 "$url" >/dev/null 2>&1
}

wait_for_url() {
  local name="$1"
  local url="$2"
  local timeout="${3:-90}"
  local start
  start="$(date +%s)"

  while true; do
    if http_ready "$url"; then
      log "$name is reachable: $url"
      return 0
    fi

    if (( "$(date +%s)" - start >= timeout )); then
      warn "$name is not reachable after ${timeout}s: $url"
      return 1
    fi

    sleep 2
  done
}

mime_types_path() {
  for path in \
    /opt/homebrew/etc/nginx/mime.types \
    /usr/local/etc/nginx/mime.types \
    /usr/local/nginx/conf/mime.types \
    /etc/nginx/mime.types; do
    if [[ -f "$path" ]]; then
      printf '%s\n' "$path"
      return 0
    fi
  done
  return 1
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

start_docker_deps() {
  [[ "$START_DOCKER_DEPS" == "1" ]] || return 0

  need_cmd docker
  local dc
  dc="$(compose_cmd)" || fail "Docker Compose is required when MES_START_DOCKER_DEPS=1"

  log "Starting MES Docker dependencies (mysql, redis, minio)"
  (
    cd "$PROJECT_ROOT"
    # shellcheck disable=SC2086
    $dc up -d mysql redis minio
  )
}

start_backend() {
  [[ "$START_BACKEND" == "1" ]] || {
    warn "Skipping backend start because MES_START_BACKEND=$START_BACKEND"
    return 0
  }

  if port_is_open 127.0.0.1 "$BACKEND_PORT"; then
    log "MES backend port $BACKEND_PORT is already open; reusing it"
    return 0
  fi

  need_cmd java
  mkdir -p "$LOG_DIR"

  if [[ ! -f "$BACKEND_JAR" ]]; then
    need_cmd mvn
    log "MES backend jar is missing; building mes-admin from parent reactor"
    (
      cd "$PROJECT_ROOT/mes-backend"
      mvn -pl mes-admin -am -DskipTests package
    )
  fi

  log "Starting MES backend (mes-admin jar) on port $BACKEND_PORT"
  mkdir -p "$BIN_DIR" "$LAUNCHD_DIR"

  local launcher="$BIN_DIR/start-mes-backend.sh"
  local plist="$LAUNCHD_DIR/$BACKEND_LABEL.plist"

  cat > "$launcher" <<EOF
#!/usr/bin/env bash
set -Eeuo pipefail
echo \$\$ > "$LOG_DIR/mes-admin.pid"
cd "$PROJECT_ROOT/mes-backend/mes-admin"
exec java $BACKEND_JAVA_OPTS -jar "$BACKEND_JAR" --spring.profiles.active=dev --server.port="$BACKEND_PORT" --management.health.rabbit.enabled="$BACKEND_RABBIT_HEALTH_ENABLED" --management.endpoint.health.show-details="$BACKEND_HEALTH_SHOW_DETAILS"
EOF
  chmod +x "$launcher"

  if screen_available; then
    start_screen_job "$BACKEND_SCREEN_SESSION" "$launcher" "$LOG_DIR/mes-admin.log"
    return 0
  fi

  if launchctl_available; then
    cat > "$plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>$BACKEND_LABEL</string>
  <key>ProgramArguments</key>
  <array>
    <string>/bin/bash</string>
    <string>$launcher</string>
  </array>
  <key>WorkingDirectory</key>
  <string>$PROJECT_ROOT/mes-backend/mes-admin</string>
  <key>RunAtLoad</key>
  <true/>
  <key>StandardOutPath</key>
  <string>$LOG_DIR/mes-admin.log</string>
  <key>StandardErrorPath</key>
  <string>$LOG_DIR/mes-admin.log</string>
</dict>
</plist>
EOF
    restart_launchctl_job "$BACKEND_LABEL" "$plist"
    return 0
  fi

  (
    cd "$PROJECT_ROOT/mes-backend/mes-admin"
    nohup "$launcher" > "$LOG_DIR/mes-admin.log" 2>&1 &
    echo $! > "$LOG_DIR/mes-admin.pid"
  )
}

start_frontend() {
  [[ "$START_FRONTEND" == "1" ]] || {
    warn "Skipping frontend start because MES_START_FRONTEND=$START_FRONTEND"
    return 0
  }

  if port_is_open 127.0.0.1 "$FRONTEND_PORT"; then
    log "MES frontend port $FRONTEND_PORT is already open; reusing it"
    return 0
  fi

  need_cmd npm
  mkdir -p "$LOG_DIR"

  if [[ ! -d "$PROJECT_ROOT/mes-frontend/node_modules" ]]; then
    log "Installing MES frontend dependencies"
    (cd "$PROJECT_ROOT/mes-frontend" && npm install)
  fi

  log "Starting MES frontend on port $FRONTEND_PORT"
  mkdir -p "$BIN_DIR" "$LAUNCHD_DIR"

  local launcher="$BIN_DIR/start-mes-frontend.sh"
  local plist="$LAUNCHD_DIR/$FRONTEND_LABEL.plist"

  cat > "$launcher" <<EOF
#!/usr/bin/env bash
set -Eeuo pipefail
echo \$\$ > "$LOG_DIR/mes-frontend.pid"
cd "$PROJECT_ROOT/mes-frontend"
exec npm run dev -- --host 127.0.0.1 --port "$FRONTEND_PORT"
EOF
  chmod +x "$launcher"

  if screen_available; then
    start_screen_job "$FRONTEND_SCREEN_SESSION" "$launcher" "$LOG_DIR/mes-frontend.log"
    return 0
  fi

  if launchctl_available; then
    cat > "$plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>$FRONTEND_LABEL</string>
  <key>ProgramArguments</key>
  <array>
    <string>/bin/bash</string>
    <string>$launcher</string>
  </array>
  <key>WorkingDirectory</key>
  <string>$PROJECT_ROOT/mes-frontend</string>
  <key>RunAtLoad</key>
  <true/>
  <key>StandardOutPath</key>
  <string>$LOG_DIR/mes-frontend.log</string>
  <key>StandardErrorPath</key>
  <string>$LOG_DIR/mes-frontend.log</string>
</dict>
</plist>
EOF
    restart_launchctl_job "$FRONTEND_LABEL" "$plist"
    return 0
  fi

  (
    cd "$PROJECT_ROOT/mes-frontend"
    nohup "$launcher" > "$LOG_DIR/mes-frontend.log" 2>&1 &
    echo $! > "$LOG_DIR/mes-frontend.pid"
  )
}

write_nginx_conf() {
  mkdir -p "$NGINX_DIR" "$LOG_DIR" "$NGINX_PREFIX/logs"

  local mime_types
  mime_types="$(mime_types_path)" || fail "Could not find nginx mime.types. Install nginx or add a standard mime.types file."

  cat > "$NGINX_CONF" <<EOF
worker_processes 1;
pid $PID_FILE;
error_log $LOG_DIR/mes-nginx-error.log warn;

events {
    worker_connections 1024;
}

http {
    include $mime_types;
    default_type application/octet-stream;
    access_log $LOG_DIR/mes-nginx-access.log;
    sendfile on;
    keepalive_timeout 65;
    client_max_body_size 100m;

    map \$http_upgrade \$connection_upgrade {
        default upgrade;
        '' close;
    }

    upstream mes_frontend {
        server 127.0.0.1:$FRONTEND_PORT;
    }

    upstream mes_backend {
        server 127.0.0.1:$BACKEND_PORT;
    }

    server {
        listen $LISTEN_HOST:$LISTEN_PORT;
        server_name $DOMAIN;

        location = /__proxy_health {
            access_log off;
            add_header Content-Type text/plain;
            return 200 "mes local nginx proxy ok\\n";
        }

        location /api/ {
            proxy_pass http://mes_backend/api/;
            proxy_http_version 1.1;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto \$scheme;
            proxy_set_header Upgrade \$http_upgrade;
            proxy_set_header Connection \$connection_upgrade;
            proxy_connect_timeout 10s;
            proxy_send_timeout 60s;
            proxy_read_timeout 120s;
        }

        location / {
            proxy_pass http://mes_frontend;
            proxy_http_version 1.1;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto \$scheme;
            proxy_set_header Upgrade \$http_upgrade;
            proxy_set_header Connection \$connection_upgrade;
            proxy_connect_timeout 10s;
            proxy_send_timeout 60s;
            proxy_read_timeout 120s;
        }
    }
}
EOF
}

start_nginx() {
  need_cmd nginx
  write_nginx_conf

  nginx -p "$NGINX_PREFIX" -t -c "$NGINX_CONF"

  if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" >/dev/null 2>&1; then
    log "Reloading MES local nginx proxy"
    nginx -p "$NGINX_PREFIX" -s reload -c "$NGINX_CONF"
  else
    log "Starting MES local nginx proxy on $LISTEN_HOST:$LISTEN_PORT for $DOMAIN"
    nginx -p "$NGINX_PREFIX" -c "$NGINX_CONF"
  fi
}

print_summary() {
  cat <<EOF

${GREEN}MES local proxy is configured.${NC}
  Domain:        http://$DOMAIN:$LISTEN_PORT
  Frontend:      http://127.0.0.1:$FRONTEND_PORT
  Backend API:   http://127.0.0.1:$BACKEND_PORT/api
  Backend health:http://127.0.0.1:$BACKEND_PORT$BACKEND_HEALTH_PATH
  Nginx config:  $NGINX_CONF
  Logs:          $LOG_DIR

Verify:
  $PROJECT_ROOT/scripts/local-proxy/verify-mes-local-proxy.sh

Stop proxy:
  $PROJECT_ROOT/scripts/local-proxy/stop-mes-local-proxy.sh

EOF
}

main() {
  need_cmd curl
  need_cmd nc
  start_docker_deps
  start_backend
  start_frontend
  wait_for_url "MES frontend" "http://127.0.0.1:$FRONTEND_PORT" "$WAIT_TIMEOUT" || true
  wait_for_url "MES backend health" "http://127.0.0.1:$BACKEND_PORT$BACKEND_HEALTH_PATH" "$WAIT_TIMEOUT" || true
  start_nginx
  print_summary
}

main "$@"
