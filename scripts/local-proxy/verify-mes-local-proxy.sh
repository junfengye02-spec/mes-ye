#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

DOMAIN="${MES_DOMAIN:-mesmac.0000238.xyz}"
LISTEN_PORT="${MES_PROXY_PORT:-8088}"
FRONTEND_PORT="${MES_FRONTEND_PORT:-3000}"
BACKEND_PORT="${MES_BACKEND_PORT:-9091}"
BACKEND_HEALTH_PATH="${MES_BACKEND_HEALTH_PATH:-/api/actuator/health}"
RESOLVE_IP="${MES_RESOLVE_IP:-127.0.0.1}"
PUBLIC_IP_CHECK="${MES_PUBLIC_IP_CHECK:-0}"
REAL_DOMAIN_CHECK="${MES_REAL_DOMAIN_CHECK:-0}"

GREEN=$'\033[0;32m'
YELLOW=$'\033[1;33m'
RED=$'\033[0;31m'
NC=$'\033[0m'

PASS_COUNT=0
FAIL_COUNT=0

pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  printf '%s\n' "${GREEN}[PASS]${NC} $*"
}

warn() {
  printf '%s\n' "${YELLOW}[WARN]${NC} $*"
}

fail_check() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  printf '%s\n' "${RED}[FAIL]${NC} $*"
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    fail_check "Missing command: $1"
    return 1
  }
}

curl_status() {
  local url="$1"
  curl -sS -o /tmp/mes-proxy-verify-body.$$ -w '%{http_code}' --max-time 8 "$url"
}

curl_resolved_status() {
  local url="$1"
  curl -sS -o /tmp/mes-proxy-verify-body.$$ -w '%{http_code}' \
    --max-time 8 \
    --resolve "$DOMAIN:$LISTEN_PORT:$RESOLVE_IP" \
    "$url"
}

check_http_status() {
  local name="$1"
  local url="$2"
  local expected_pattern="$3"
  local status

  status="$(curl_status "$url" || true)"
  if [[ "$status" =~ $expected_pattern ]]; then
    pass "$name returned HTTP $status"
  else
    fail_check "$name returned HTTP ${status:-curl-error}; url=$url"
  fi
}

check_resolved_status() {
  local name="$1"
  local path="$2"
  local expected_pattern="$3"
  local url="http://$DOMAIN:$LISTEN_PORT$path"
  local status

  status="$(curl_resolved_status "$url" || true)"
  if [[ "$status" =~ $expected_pattern ]]; then
    pass "$name returned HTTP $status via Host $DOMAIN"
  else
    fail_check "$name returned HTTP ${status:-curl-error}; url=$url"
  fi
}

check_dns() {
  if command -v dig >/dev/null 2>&1; then
    local answer
    answer="$(dig +short "$DOMAIN" A | tr '\n' ' ')"
    [[ -n "$answer" ]] && pass "$DOMAIN DNS A record: $answer" || fail_check "$DOMAIN has no A record"
    return
  fi

  if command -v nslookup >/dev/null 2>&1; then
    nslookup "$DOMAIN" >/tmp/mes-proxy-nslookup.$$ 2>&1 && pass "$DOMAIN resolves by nslookup" || fail_check "$DOMAIN does not resolve by nslookup"
    return
  fi

  warn "Skipping DNS check because dig/nslookup is unavailable"
}

check_public_ip() {
  [[ "$PUBLIC_IP_CHECK" == "1" ]] || {
    warn "Skipping public IP match check. Set MES_PUBLIC_IP_CHECK=1 to enable it."
    return 0
  }

  if ! command -v dig >/dev/null 2>&1; then
    warn "Skipping public IP check because dig is unavailable"
    return 0
  fi

  local dns_ips public_ip
  dns_ips="$(dig +short "$DOMAIN" A | sort -u | tr '\n' ' ')"
  public_ip="$(curl -fsS --max-time 5 https://api.ipify.org || true)"

  if [[ -z "$public_ip" ]]; then
    fail_check "Could not determine public IP from api.ipify.org"
    return 0
  fi

  if grep -qw "$public_ip" <<<"$dns_ips"; then
    pass "$DOMAIN resolves to this machine public IP: $public_ip"
  else
    fail_check "$DOMAIN DNS ($dns_ips) does not include this machine public IP ($public_ip)"
  fi
}

check_real_domain_status() {
  [[ "$REAL_DOMAIN_CHECK" == "1" ]] || {
    warn "Skipping real DNS-to-proxy HTTP check. Set MES_REAL_DOMAIN_CHECK=1 to enable it."
    return 0
  }

  check_http_status "MES real domain proxy health" "http://$DOMAIN:$LISTEN_PORT/__proxy_health" '^200$'
  check_http_status "MES real domain frontend reverse proxy" "http://$DOMAIN:$LISTEN_PORT/" '^(200|301|302|304)$'
  check_http_status "MES real domain API health reverse proxy" "http://$DOMAIN:$LISTEN_PORT$BACKEND_HEALTH_PATH" '^200$'
}

main() {
  need_cmd curl || true
  check_dns
  check_public_ip

  check_http_status "MES frontend direct" "http://127.0.0.1:$FRONTEND_PORT" '^(200|301|302|304)$'
  check_http_status "MES backend health direct" "http://127.0.0.1:$BACKEND_PORT$BACKEND_HEALTH_PATH" '^200$'
  check_resolved_status "MES nginx proxy health" "/__proxy_health" '^200$'
  check_resolved_status "MES domain frontend reverse proxy" "/" '^(200|301|302|304)$'
  check_resolved_status "MES domain API health reverse proxy" "$BACKEND_HEALTH_PATH" '^200$'
  check_real_domain_status

  rm -f /tmp/mes-proxy-verify-body.$$ /tmp/mes-proxy-nslookup.$$ 2>/dev/null || true

  if (( FAIL_COUNT > 0 )); then
    cat <<EOF

${RED}MES proxy verification failed.${NC}
Tips:
  - Start proxy: $PROJECT_ROOT/scripts/local-proxy/start-mes-local-proxy.sh
  - Check nginx log: $PROJECT_ROOT/runtime/logs/mes-nginx-error.log
  - If you need real port 80, rerun with MES_PROXY_PORT=80 and appropriate permissions.

EOF
    exit 1
  fi

  cat <<EOF

${GREEN}MES proxy verification passed.${NC}
  Domain URL: http://$DOMAIN:$LISTEN_PORT
  Verified with curl --resolve $DOMAIN:$LISTEN_PORT:$RESOLVE_IP

EOF
}

main "$@"
