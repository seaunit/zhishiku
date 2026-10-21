#!/usr/bin/env bash
set -Eeuo pipefail

REPO_URL="${KB_REPO_URL:-https://github.com/seaunit/zhishiku.git}"
REF="${KB_REF:-main}"
INSTALL_ROOT="${KB_INSTALL_ROOT:-/opt/zhishiku}"
PORTAL_ROOT="${KB_PORTAL_ROOT:-/opt/seaunit-portal}"
ENV_FILE="${KB_ENV_FILE:-/etc/zhishiku/kb.env}"
SERVICE_NAME="${KB_SERVICE_NAME:-zhishiku-api}"
SERVICE_USER="${KB_SERVICE_USER:-zhishiku}"
PORT="${KB_PORT:-8082}"
NGINX_CONFIG="${KB_NGINX_CONFIG:-/etc/nginx/conf.d/shuati.conf}"
NGINX_SNIPPET="/etc/nginx/snippets/zhishiku-kb.conf"
SKIP_TESTS="${KB_SKIP_TESTS:-0}"
PROMPT_SECRETS="${KB_PROMPT_SECRETS:-1}"
PROMPT_TIMEOUT="${KB_PROMPT_TIMEOUT:-60}"
SECRETS_ALREADY_PROMPTED="${KB_SECRETS_ALREADY_PROMPTED:-0}"

read_env_value() {
  local key="$1"
  awk -v key="$key" -v sq="'" '
    index($0, key "=") == 1 {
      value = substr($0, length(key) + 2)
      if ((substr(value, 1, 1) == sq && substr(value, length(value), 1) == sq) ||
          (substr(value, 1, 1) == "\"" && substr(value, length(value), 1) == "\"")) {
        value = substr(value, 2, length(value) - 2)
      }
      print value
      exit
    }
  ' "$ENV_FILE"
}

prompt_secret() {
  local label="$1"
  local current="$2"
  local value=""

  if ! { : > /dev/tty; } 2>/dev/null; then
    printf '[密钥] 未检测到可交互终端，%s 保持现有值或未设置状态。\n' "$label" >&2
    printf '%s' "$current"
    return 0
  fi
  if [[ "$PROMPT_SECRETS" != "1" ]]; then
    printf '%s' "$current"
    return 0
  fi
  if [[ -n "$current" ]]; then
    printf '\n[密钥] %s 已配置，当前长度 %s。\n' "$label" "${#current}" > /dev/tty
  else
    printf '\n[密钥] %s 尚未配置。\n' "$label" > /dev/tty
  fi
  printf '[密钥] 输入不会回显；直接回车 = 保留现有值或保持未设置。\n' > /dev/tty
  printf '[密钥] 最多等待 %s 秒，超时按回车处理：' "$PROMPT_TIMEOUT" > /dev/tty
  local rc=0
  IFS= read -r -s -t "$PROMPT_TIMEOUT" value < /dev/tty || rc=$?
  if ((rc > 128)); then
    value=""
    printf '\n[密钥] 等待超时，按直接回车处理。\n' > /dev/tty
  else
    printf '\n' > /dev/tty
  fi
  value="$(printf '%s' "$value" | tr -d '[:space:]')"
  value="${value//\'/}"
  if [[ -z "$value" && -n "$current" ]]; then
    value="$current"
  fi
  printf '%s' "$value"
}

set_env_value() {
  local key="$1"
  local value="$2"
  local env_tmp
  env_tmp="$(mktemp "${ENV_FILE}.XXXXXX")"
  awk -v key="$key" 'index($0, key "=") != 1' "$ENV_FILE" > "$env_tmp"
  printf "%s='%s'\n" "$key" "$value" >> "$env_tmp"
  chmod 0600 "$env_tmp"
  chown root:root "$env_tmp"
  mv "$env_tmp" "$ENV_FILE"
}

ensure_secrets() {
  local ai_key search_key fetch_key smtp_password
  ai_key="${DEEPSEEK_API_KEY:-$(read_env_value DEEPSEEK_API_KEY)}"
  search_key="${DEEPSEEK_SEARCH_API_KEY:-$(read_env_value DEEPSEEK_SEARCH_API_KEY)}"
  fetch_key="${DEEPSEEK_FETCH_API_KEY:-$(read_env_value DEEPSEEK_FETCH_API_KEY)}"
  smtp_password="${SMTP_PASSWORD:-$(read_env_value SMTP_PASSWORD)}"

  if [[ "$SECRETS_ALREADY_PROMPTED" != "1" ]]; then
    ai_key="$(prompt_secret "DeepSeek AI API Key" "$ai_key")"
    search_key="$(prompt_secret "DeepSeek Search API Key（留空复用主 AI Key）" "$search_key")"
    fetch_key="$(prompt_secret "DeepSeek Fetch API Key（留空复用主 AI Key）" "$fetch_key")"
    smtp_password="$(prompt_secret "SMTP 密码/应用专用密钥" "$smtp_password")"
  fi

  ai_key="$(printf '%s' "$ai_key" | tr -d '[:space:]')"
  search_key="$(printf '%s' "$search_key" | tr -d '[:space:]')"
  fetch_key="$(printf '%s' "$fetch_key" | tr -d '[:space:]')"
  smtp_password="$(printf '%s' "$smtp_password" | tr -d '[:space:]')"
  ai_key="${ai_key//\'/}"
  search_key="${search_key//\'/}"
  fetch_key="${fetch_key//\'/}"
  smtp_password="${smtp_password//\'/}"

  if [[ -z "$search_key" ]]; then
    search_key="$ai_key"
  fi
  if [[ -z "$fetch_key" ]]; then
    fetch_key="$ai_key"
  fi

  set_env_value DEEPSEEK_API_KEY "$ai_key"
  set_env_value DEEPSEEK_SEARCH_API_KEY "$search_key"
  set_env_value DEEPSEEK_FETCH_API_KEY "$fetch_key"
  set_env_value SMTP_PASSWORD "$smtp_password"

  printf '[部署] DeepSeek AI 密钥：%s\n' \
    "$([[ -n "$ai_key" ]] && printf 已配置 || printf 未配置)"
  printf '[部署] SMTP 密钥：%s\n' \
    "$([[ -n "$smtp_password" ]] && printf 已配置 || printf 未配置)"
}

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

ensure_secrets

WORK_DIR="$(mktemp -d)"
SOURCE_DIR="$WORK_DIR/source"
STAGE_DIR=""
PORTAL_STAGE_DIR=""
PREVIOUS_RELEASE=""
PORTAL_PREVIOUS_RELEASE=""
ROLLBACK_ACTIVE=0
PORTAL_SWITCHED=0
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
    if ((PORTAL_SWITCHED == 1)); then
      if [[ -n "$PORTAL_PREVIOUS_RELEASE" && -d "$PORTAL_PREVIOUS_RELEASE" ]]; then
        ln -sfn "$PORTAL_PREVIOUS_RELEASE" "$PORTAL_ROOT/current.next"
        mv -Tf "$PORTAL_ROOT/current.next" "$PORTAL_ROOT/current"
      else
        rm -f "$PORTAL_ROOT/current"
      fi
    fi
    if [[ -n "$NGINX_BACKUP" && -f "$NGINX_BACKUP" ]]; then
      cp -a "$NGINX_BACKUP" "$NGINX_CONFIG"
      nginx -t >/dev/null 2>&1 && systemctl reload nginx || true
    fi
  fi
  if [[ -n "$STAGE_DIR" && -d "$STAGE_DIR" ]]; then
    rm -rf "$STAGE_DIR"
  fi
  if [[ -n "$PORTAL_STAGE_DIR" && -d "$PORTAL_STAGE_DIR" ]]; then
    rm -rf "$PORTAL_STAGE_DIR"
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

PORTAL_RELEASE_DIR="$PORTAL_ROOT/releases/$RELEASE_ID"
PORTAL_STAGE_DIR="$PORTAL_ROOT/.deploy-$RELEASE_ID"
install -d -m 0755 "$PORTAL_STAGE_DIR"
cp -a "$SOURCE_DIR/deploy/portal/." "$PORTAL_STAGE_DIR/"
printf '%s\n' "$SOURCE_SHA" > "$PORTAL_STAGE_DIR/VERSION"
chmod -R u=rwX,go=rX "$PORTAL_STAGE_DIR"
mv "$PORTAL_STAGE_DIR" "$PORTAL_RELEASE_DIR"
PORTAL_STAGE_DIR=""
if [[ -L "$PORTAL_ROOT/current" ]]; then
  PORTAL_PREVIOUS_RELEASE="$(readlink -f "$PORTAL_ROOT/current")"
fi
ln -sfn "$PORTAL_RELEASE_DIR" "$PORTAL_ROOT/current.next"
mv -Tf "$PORTAL_ROOT/current.next" "$PORTAL_ROOT/current"
PORTAL_SWITCHED=1

if [[ -f "$NGINX_CONFIG" ]]; then
  NGINX_BACKUP="/root/nginx-before-zhishiku-$(date -u +%Y%m%d%H%M%S).conf"
  cp -a "$NGINX_CONFIG" "$NGINX_BACKUP"
  python3 - "$NGINX_CONFIG" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")
if "listen 443" not in text or "seaunit.site" not in text:
    raise SystemExit("Target Nginx file does not contain the expected HTTPS server.")

lines = text.splitlines(keepends=True)
changed = False

if "root /opt/seaunit-portal/current;" not in text:
    start = next(
        (index for index, line in enumerate(lines) if line.strip() == "location = / {"),
        None,
    )
    if start is None:
        raise SystemExit("Could not find the exact root location block.")

    depth = 0
    end = None
    for index in range(start, len(lines)):
        depth += lines[index].count("{") - lines[index].count("}")
        if depth == 0:
            end = index
            break
    if end is None:
        raise SystemExit("Could not find the end of the root location block.")

    indent = lines[start][: len(lines[start]) - len(lines[start].lstrip())]
    lines[start : end + 1] = [
        f"{indent}location = / {{\n",
        f"{indent}    root /opt/seaunit-portal/current;\n",
        f"{indent}    try_files /index.html =404;\n",
        f"{indent}}}\n",
    ]
    changed = True

if "zhishiku-kb.conf" not in "".join(lines):
    closing = [index for index, line in enumerate(lines) if line.strip() == "}"]
    if not closing:
        raise SystemExit("Could not find the target server closing brace.")
    marker = "    include /etc/nginx/snippets/zhishiku-kb.conf; # managed by zhishiku\n"
    lines.insert(closing[-1], marker)
    changed = True

if changed:
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
  DATABASE_URL="$(read_env_value DATABASE_URL)"
  DATABASE_USERNAME="$(read_env_value DATABASE_USERNAME)"
  DATABASE_PASSWORD="$(read_env_value DATABASE_PASSWORD)"
  KB_OWNER_EMAIL="$(read_env_value KB_OWNER_EMAIL)"
  KB_OWNER_PASSWORD="$(read_env_value KB_OWNER_PASSWORD)"
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
curl -fsS --resolve seaunit.site:443:127.0.0.1 https://seaunit.site/bank/1 >/dev/null
curl -fsS --resolve seaunit.site:443:127.0.0.1 https://seaunit.site/login >/dev/null
curl -fsS --resolve seaunit.site:443:127.0.0.1 https://seaunit.site/ \
  | grep -q '拾题与识海'

ROLLBACK_ACTIVE=0
printf '[deploy] Release %s is live at https://seaunit.site/ and https://seaunit.site/kb/\n' "$RELEASE_ID"
if [[ -f /root/zhishiku-initial-password ]]; then
  printf '[deploy] Initial login details: /root/zhishiku-initial-password\n'
fi
