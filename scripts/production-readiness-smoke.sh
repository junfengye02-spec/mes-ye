#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_NAME="${MES_SMOKE_PROJECT:-mesye-prod-smoke}"
KEEP_STACK=0
DOWN_ONLY=0

for arg in "$@"; do
  case "$arg" in
    --keep) KEEP_STACK=1 ;;
    --down-only) DOWN_ONLY=1 ;;
    *)
      echo "Unknown argument: $arg" >&2
      exit 2
      ;;
  esac
done

rand_hex() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex "${1:-24}"
  else
    date +%s%N | sha256sum | awk '{print $1}'
  fi
}

export MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-$(rand_hex 18)}"
export REDIS_PASSWORD="${REDIS_PASSWORD:-$(rand_hex 18)}"
export MINIO_ROOT_USER="${MINIO_ROOT_USER:-mesminio}"
export MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:-$(rand_hex 18)}"
export MES_JWT_SECRET="${MES_JWT_SECRET:-$(rand_hex 32)}"
export MES_APS_API_KEY="${MES_APS_API_KEY:-$(rand_hex 16)}"
export MES_APS_HMAC_KEY="${MES_APS_HMAC_KEY:-$(rand_hex 32)}"
export MES_FILE_STORAGE_TYPE="${MES_FILE_STORAGE_TYPE:-minio}"

export MES_MYSQL_HOST_PORT="${MES_MYSQL_HOST_PORT:-13307}"
export MES_REDIS_HOST_PORT="${MES_REDIS_HOST_PORT:-16379}"
export MES_MINIO_API_HOST_PORT="${MES_MINIO_API_HOST_PORT:-19000}"
export MES_MINIO_CONSOLE_HOST_PORT="${MES_MINIO_CONSOLE_HOST_PORT:-19001}"
export MES_BACKEND_HOST_PORT="${MES_BACKEND_HOST_PORT:-19090}"
export MES_FRONTEND_HOST_PORT="${MES_FRONTEND_HOST_PORT:-18080}"

BACKEND_BASE="http://127.0.0.1:${MES_BACKEND_HOST_PORT}/api"
FRONTEND_BASE="http://127.0.0.1:${MES_FRONTEND_HOST_PORT}"
COMPOSE=(docker compose -p "$PROJECT_NAME" -f "$ROOT_DIR/docker-compose.yml")

cleanup() {
  if [[ "$KEEP_STACK" != "1" && "$DOWN_ONLY" != "1" ]]; then
    "${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

wait_http() {
  local url="$1"
  local timeout="${2:-240}"
  local start
  start="$(date +%s)"
  until curl -fsS "$url" >/dev/null 2>&1; do
    if (( "$(date +%s)" - start > timeout )); then
      echo "Timed out waiting for $url" >&2
      "${COMPOSE[@]}" ps >&2 || true
      return 1
    fi
    sleep 5
  done
}

if [[ "$DOWN_ONLY" == "1" ]]; then
  "${COMPOSE[@]}" down -v --remove-orphans
  exit 0
fi

command -v docker >/dev/null 2>&1 || { echo "docker is required" >&2; exit 1; }
command -v python3 >/dev/null 2>&1 || { echo "python3 is required" >&2; exit 1; }

echo "[1/5] Reset isolated compose project: $PROJECT_NAME"
"${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1 || true

echo "[2/5] Build and start clean production stack"
"${COMPOSE[@]}" up -d --build

echo "[3/5] Wait for service health"
wait_http "$BACKEND_BASE/actuator/health" 360
wait_http "$FRONTEND_BASE/health" 180

echo "[4/5] Run API and frontend smoke checks"
BACKEND_BASE="$BACKEND_BASE" FRONTEND_BASE="$FRONTEND_BASE" python3 - <<'PY'
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

backend = os.environ["BACKEND_BASE"].rstrip("/")
frontend = os.environ["FRONTEND_BASE"].rstrip("/")

def request(method, url, token=None, body=None, timeout=20):
    data = None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if body is not None:
        data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8")
            return resp.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8")
        try:
            parsed = json.loads(raw) if raw else {}
        except Exception:
            parsed = {"raw": raw[:500]}
        return exc.code, parsed

def assert_ok(name, condition, detail=""):
    status = "PASS" if condition else "FAIL"
    print(f"[{status}] {name}{' - ' + detail if detail else ''}")
    if not condition:
        raise SystemExit(1)

with urllib.request.urlopen(frontend + "/", timeout=20) as resp:
    html = resp.read(5000).decode("utf-8", errors="ignore").lower()
    assert_ok("frontend index", resp.status == 200 and ("html" in html or "app" in html), f"status={resp.status}")

status, login = request("POST", backend + "/auth/login", body={
    "username": os.environ.get("E2E_USER", "admin"),
    "password": os.environ.get("E2E_PASS", "admin123"),
    "loginClient": "ADMIN",
})
token = ((login.get("data") or {}).get("accessToken") if isinstance(login, dict) else None)
assert_ok("admin login", status == 200 and login.get("code") == 200 and token, f"http={status}")

checks = [
    ("user info", "/auth/user-info"),
    ("menu tree", "/system/menu/user-tree"),
    ("material page", "/basic/material/page?pageNum=1&pageSize=10"),
    ("work center page", "/basic/work-center/page?pageNum=1&pageSize=10"),
    ("process template page", "/process/process-template/page?pageNum=1&pageSize=10"),
    ("work order page", "/workorder/work-order/page?pageNum=1&pageSize=10"),
    ("dispatch page", "/dispatch/task/page?pageNum=1&pageSize=10"),
    ("quality handover page", "/quality/shift-handover/page?pageNum=1&pageSize=10"),
    ("abnormal page", "/abnormal/contact/page?pageNum=1&pageSize=10"),
    ("inventory page", "/material/inventory/page?pageNum=1&pageSize=10"),
]
for name, path in checks:
    status, payload = request("GET", backend + path, token=token)
    assert_ok(name, status == 200 and payload.get("code") == 200, f"http={status}, code={payload.get('code')}")

print("[PASS] clean production stack smoke complete")
PY

echo "[5/5] Smoke complete"
if [[ "$KEEP_STACK" == "1" ]]; then
  echo "Stack kept: docker compose -p $PROJECT_NAME -f docker-compose.yml ps"
else
  echo "Stack will be removed; rerun with --keep to inspect it."
fi
