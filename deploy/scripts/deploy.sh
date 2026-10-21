#!/usr/bin/env bash
set -Eeuo pipefail

REPO_URL="${KB_REPO_URL:-https://github.com/seaunit/zhishiku.git}"
REF="${KB_REF:-main}"
INSTALL_ROOT="${KB_INSTALL_ROOT:-/opt/zhishiku}"
ENV_FILE="${KB_ENV_FILE:-/etc/zhishiku/kb.env}"
SERVICE_NAME="${KB_SERVICE_NAME:-zhishiku-api}"
SERVICE_USER="${KB_SERVICE_USER:-zhishiku}"
PORT="${KB_PORT:-8082}"
NGINX_CONFIG="${KB_NGINX_CONFIG:-/etc/nginx/conf.d/shuati.conf}"
NGINX_SNIPPET="/etc/nginx/snippets/zhishiku-kb.conf"
SKIP_TESTS="${KB_SKIP_TESTS:-0}"

usage() {
  cat <<'EOF'
Usage: deploy.sh [--ref <branch|tag|commit>]

Fetches the requested GitHub ref, builds it, installs an immutable release,
starts the systemd service, and enables the /kb Nginx routes.
EOF
}

while (($# > 0)); do
  case "$1" in
    --ref)
      REF="${2:?--ref requires a value}"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      printf 'Unknown argument: %s\n' "$1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if ((EUID != 0)); then
  printf 'Run this script as root.\n' >&2
  exit 1
fi

for command in curl git java nginx node pnpm psql python3 systemctl; do
  if ! command -v "$command" >/dev/null 2>&1; then
    printf 'Missing required command: %s\n' "$command" >&2
    exit 1
  fi
done
if ! java -version 2>&1 | grep -q 'version "17\.'; then
  printf 'Java 17 is required.\n' >&2
  exit 1
fi
if [[ ! -f "$ENV_FILE" ]]; then
  printf 'Environment file not found: %s\n' "$ENV_FILE" >&2
  printf 'Run provision-ubuntu.sh first.\n' >&2
  exit 1
fi

WORK_DIR="$(mktemp -d)"
SOURCE_DIR="$WORK_DIR/source"
STAGE_DIR=""
PREVIOUS_RELEASE=""
ROLLBACK_ACTIVE=0
NGINX_BACKUP=""

cleanup() {
  status=$?
  if ((status != 0 && ROLLBACK_ACTIVE == 1)); then
    printf '[deploy] Deployment failed; restoring the previous release.\n' >&2
    if [[ -n "$PREVIOUS_RELEASE" && -d "$PREVIOUS_RELEASE" ]]; then
      ln -sfn "$PREVIOUS_RELEASE" "$INSTALL_ROOT/current.next"
      mv -Tf "$INSTALL_ROOT/current.next" "$INSTALL_ROOT/current"
      systemctl restart "$SERVICE_NAME" || true
    fi
    if [[ -n "$NGINX_BACKUP" && -f "$NGINX_BACKUP" ]]; then
      cp -a "$NGINX_BACKUP" "$NGINX_CONFIG"
      nginx -t >/dev/null 2>&1 && systemctl reload nginx || true
    fi
  fi
  if [[ -n "$STAGE_DIR" && -d "$STAGE_DIR" ]]; then
    rm -rf "$STAGE_DIR"
  fi
  rm -rf "$WORK_DIR"
  exit "$status"
}
trap cleanup EXIT

wait_for_health() {
  local attempt
  for attempt in $(seq 1 40); do
    if curl -fsS --max-time 3 "http://127.0.0.1:$PORT/kb/api/actuator/health" \
      | grep -q '"status":"UP"'; then
      return 0
    fi
    sleep 2
  done
  return 1
}

git init -q "$SOURCE_DIR"
git -C "$SOURCE_DIR" remote add origin "$REPO_URL"
git -C "$SOURCE_DIR" fetch --depth=1 origin "$REF"
git -C "$SOURCE_DIR" checkout -q --detach FETCH_HEAD
SOURCE_SHA="$(git -C "$SOURCE_DIR" rev-parse HEAD)"
printf '[deploy] Building %s (%s)\n' "$REF" "$SOURCE_SHA"

(
  cd "$SOURCE_DIR/backend"
  if [[ "$SKIP_TESTS" != "1" ]]; then
    MAVEN_OPTS="${MAVEN_OPTS:--Xmx512m}" bash ./mvnw -q -pl kb-app -am test
  fi
  MAVEN_OPTS="${MAVEN_OPTS:--Xmx512m}" bash ./mvnw -q -pl kb-app -am package -DskipTests
)

(
  cd "$SOURCE_DIR/frontend/kb-web"
  pnpm install --frozen-lockfile
  if [[ "$SKIP_TESTS" != "1" ]]; then
    pnpm lint
    pnpm test
  fi
  NODE_OPTIONS="${NODE_OPTIONS:---max-old-space-size=768}" pnpm build
)

RELEASE_ID="${SOURCE_SHA:0:12}-$(date -u +%Y%m%d%H%M%S)"
RELEASE_DIR="$INSTALL_ROOT/releases/$RELEASE_ID"
STAGE_DIR="$INSTALL_ROOT/.deploy-$RELEASE_ID"
install -d "$STAGE_DIR/frontend/kb"
install -m 0644 \
  "$SOURCE_DIR/backend/kb-app/target/kb-app-0.1.0-SNAPSHOT.jar" \
  "$STAGE_DIR/kb-app.jar"
cp -a "$SOURCE_DIR/frontend/kb-web/dist/." "$STAGE_DIR/frontend/kb/"
printf '%s\n' "$SOURCE_SHA" > "$STAGE_DIR/VERSION"
chown -R "$SERVICE_USER:$SERVICE_USER" "$STAGE_DIR"
mv "$STAGE_DIR" "$RELEASE_DIR"
STAGE_DIR=""

if [[ -L "$INSTALL_ROOT/current" ]]; then
  PREVIOUS_RELEASE="$(readlink -f "$INSTALL_ROOT/current")"
fi

install -m 0644 \
  "$SOURCE_DIR/deploy/systemd/zhishiku-api.service" \
  "/etc/systemd/system/$SERVICE_NAME.service"
install -d -m 0755 /etc/nginx/snippets
install -m 0644 \
  "$SOURCE_DIR/deploy/nginx/zhishiku-kb.conf" \
  "$NGINX_SNIPPET"

ln -sfn "$RELEASE_DIR" "$INSTALL_ROOT/current.next"
mv -Tf "$INSTALL_ROOT/current.next" "$INSTALL_ROOT/current"
ROLLBACK_ACTIVE=1

systemctl daemon-reload
systemctl enable "$SERVICE_NAME"
systemctl restart "$SERVICE_NAME"
if ! wait_for_health; then
  journalctl -u "$SERVICE_NAME" -n 100 --no-pager >&2 || true
  printf '[deploy] API health check failed.\n' >&2
  exit 1
fi

if [[ -f "$NGINX_CONFIG" ]] && ! grep -q 'zhishiku-kb.conf' "$NGINX_CONFIG"; then
  NGINX_BACKUP="/root/nginx-before-zhishiku-$(date -u +%Y%m%d%H%M%S).conf"
  cp -a "$NGINX_CONFIG" "$NGINX_BACKUP"
  python3 - "$NGINX_CONFIG" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")
if "zhishiku-kb.conf" in text:
    raise SystemExit(0)
if "listen 443" not in text or "seaunit.site" not in text:
    raise SystemExit("Target Nginx file does not contain the expected HTTPS server.")

lines = text.splitlines(keepends=True)
closing = [index for index, line in enumerate(lines) if line.strip() == "}"]
if not closing:
    raise SystemExit("Could not find the target server closing brace.")

marker = "    include /etc/nginx/snippets/zhishiku-kb.conf; # managed by zhishiku\n"
lines.insert(closing[-1], marker)
path.write_text("".join(lines), encoding="utf-8")
PY
  if ! nginx -t; then
    cp -a "$NGINX_BACKUP" "$NGINX_CONFIG"
    printf '[deploy] Nginx validation failed; the original configuration was restored.\n' >&2
    exit 1
  fi
fi

nginx -t
systemctl reload nginx

# Finalize the one-time owner bootstrap without leaving the password in systemd config.
if grep -q '^KB_AUTO_BOOTSTRAP=true$' "$ENV_FILE"; then
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
  owner_count="$(
    PGPASSWORD="$DATABASE_PASSWORD" psql \
      -h 127.0.0.1 \
      -p "$(printf '%s' "$DATABASE_URL" | sed -E 's#.*127\.0\.0\.1:([0-9]+)/.*#\1#')" \
      -U "$DATABASE_USERNAME" \
      -d zhishiku \
      -tAc "SELECT count(*) FROM owner_account" 2>/dev/null || printf '0'
  )"
  if [[ "$owner_count" == "1" ]]; then
    umask 077
    cat > /root/zhishiku-initial-password <<EOF
URL=https://seaunit.site/kb/login
EMAIL=${KB_OWNER_EMAIL}
PASSWORD=${KB_OWNER_PASSWORD}
EOF
    chmod 0600 /root/zhishiku-initial-password
    env_tmp="$(mktemp)"
    grep -v -E '^(KB_AUTO_BOOTSTRAP|KB_OWNER_PASSWORD|KB_BOOTSTRAP_TOKEN)=' "$ENV_FILE" \
      > "$env_tmp" || true
    printf 'KB_AUTO_BOOTSTRAP=false\n' >> "$env_tmp"
    chmod 0600 "$env_tmp"
    mv "$env_tmp" "$ENV_FILE"
    systemctl restart "$SERVICE_NAME"
    if ! wait_for_health; then
      printf '[deploy] API health check failed after owner initialization.\n' >&2
      exit 1
    fi
  fi
fi

if command -v systemctl >/dev/null 2>&1; then
  systemctl is-active --quiet shuati.service
  systemctl is-active --quiet shuati-payments.service
fi

curl -fsS --resolve seaunit.site:443:127.0.0.1 https://seaunit.site/kb/ >/dev/null
curl -fsS --resolve seaunit.site:443:127.0.0.1 https://seaunit.site/app >/dev/null

ROLLBACK_ACTIVE=0
printf '[deploy] Release %s is live at https://seaunit.site/kb/\n' "$RELEASE_ID"
if [[ -f /root/zhishiku-initial-password ]]; then
  printf '[deploy] Initial login details: /root/zhishiku-initial-password\n'
fi
