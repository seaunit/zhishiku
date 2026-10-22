#!/usr/bin/env bash
set -Eeuo pipefail

REPO_URL="${SHIHAI_REPO_URL:-${KB_REPO_URL:-https://github.com/seaunit/zhishiku.git}}"
RAW_BASE="${SHIHAI_RAW_BASE:-${KB_RAW_BASE:-https://raw.githubusercontent.com/seaunit/zhishiku}}"
REF="${SHIHAI_REF:-${KB_REF:-main}}"
ENV_FILE="${SHIHAI_ENV_FILE:-${KB_ENV_FILE:-/etc/zhishiku/kb.env}}"
WAFFO_ENV_FILE="${SHIHAI_WAFFO_ENV_FILE:-${WAFFO_ENV_FILE:-/etc/zhishiku/waffo.env}}"
SWAP_SIZE_MB="${SHIHAI_SWAP_SIZE_MB:-${KB_SWAP_SIZE_MB:-2048}}"
PROMPT_SECRETS="${SHIHAI_PROMPT_SECRETS:-${KB_PROMPT_SECRETS:-1}}"
PROMPT_TIMEOUT="${SHIHAI_PROMPT_TIMEOUT:-${KB_PROMPT_TIMEOUT:-60}}"

read_env_value() {
  local key="$1"
  local legacy_key="${2:-}"
  if [[ ! -f "$ENV_FILE" ]]; then
    return 0
  fi
  awk -v key="$key" -v legacy_key="$legacy_key" -v sq="'" '
    index($0, key "=") == 1 {
      value = substr($0, length(key) + 2)
      if ((substr(value, 1, 1) == sq && substr(value, length(value), 1) == sq) ||
          (substr(value, 1, 1) == "\"" && substr(value, length(value), 1) == "\"")) {
        value = substr(value, 2, length(value) - 2)
      }
      primary = value
    }
    legacy_key != "" && index($0, legacy_key "=") == 1 {
      value = substr($0, length(legacy_key) + 2)
      if ((substr(value, 1, 1) == sq && substr(value, length(value), 1) == sq) ||
          (substr(value, 1, 1) == "\"" && substr(value, length(value), 1) == "\"")) {
        value = substr(value, 2, length(value) - 2)
      }
      legacy = value
    }
    END {
      value = primary != "" ? primary : legacy
      if (value != "") print value
    }
  ' "$ENV_FILE"
}

read_waffo_env_value() {
  local key="$1"
  local legacy_key="${2:-}"
  if [[ ! -f "$WAFFO_ENV_FILE" ]]; then
    return 0
  fi
  awk -v key="$key" -v legacy_key="$legacy_key" -v sq="'" '
    index($0, key "=") == 1 {
      value = substr($0, length(key) + 2)
      if ((substr(value, 1, 1) == sq && substr(value, length(value), 1) == sq) ||
          (substr(value, 1, 1) == "\"" && substr(value, length(value), 1) == "\"")) {
        value = substr(value, 2, length(value) - 2)
      }
      primary = value
    }
    legacy_key != "" && index($0, legacy_key "=") == 1 {
      value = substr($0, length(legacy_key) + 2)
      if ((substr(value, 1, 1) == sq && substr(value, length(value), 1) == sq) ||
          (substr(value, 1, 1) == "\"" && substr(value, length(value), 1) == "\"")) {
        value = substr(value, 2, length(value) - 2)
      }
      legacy = value
    }
    END {
      value = primary != "" ? primary : legacy
      if (value != "") print value
    }
  ' "$WAFFO_ENV_FILE"
}

read_raw_file_value() {
  local target_file="$1"
  local key="$2"
  [[ -f "$target_file" ]] || return 0
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
  ' "$target_file"
}

assert_alias_consistency() {
  local target_file="$1"
  local primary="$2"
  local legacy="$3"
  local primary_value legacy_value
  primary_value="$(read_raw_file_value "$target_file" "$primary")"
  legacy_value="$(read_raw_file_value "$target_file" "$legacy")"
  if [[ -n "$primary_value" && -n "$legacy_value" && "$primary_value" != "$legacy_value" ]]; then
    printf 'Environment aliases must match during migration: %s and %s in %s.\n' \
      "$primary" "$legacy" "$target_file" >&2
    exit 1
  fi
}

legacy_name_for() {
  local primary="$1"
  case "$primary" in
    SHIHAI_WAFFO_*)
      printf 'WAFFO_%s' "${primary#SHIHAI_WAFFO_}"
      ;;
    SHIHAI_OWNER_*|SHIHAI_BOOTSTRAP_TOKEN|SHIHAI_AUTO_BOOTSTRAP|SHIHAI_STORAGE_ROOT|SHIHAI_CAPTCHA_*|SHIHAI_BROWSER_*|SHIHAI_OCR_*|SHIHAI_WEB_IMPORT_*|SHIHAI_DOCUMENT_IMPORT_*|SHIHAI_SCREENSHOT_IMPORT_*|SHIHAI_TESSERACT_COMMAND)
      printf 'KB_%s' "${primary#SHIHAI_}"
      ;;
    SHIHAI_*)
      printf '%s' "${primary#SHIHAI_}"
      ;;
    *)
      return 1
      ;;
  esac
}

assert_all_aliases() {
  local target_file="$1"
  local primary legacy
  [[ -f "$target_file" ]] || return 0
  while IFS= read -r primary; do
    legacy="$(legacy_name_for "$primary")"
    assert_alias_consistency "$target_file" "$primary" "$legacy"
  done < <(sed -n 's/^\(SHIHAI_[A-Z0-9_]*\)=.*/\1/p' "$target_file" | sort -u)
}

set_file_value() {
  local target_file="$1"
  local key="$2"
  local value="$3"
  local escaped
  local env_tmp
  if [[ ! -f "$target_file" ]]; then
    install -m 0600 /dev/null "$target_file"
  fi
  env_tmp="$(mktemp "${target_file}.XXXXXX")"
  awk -v key="$key" 'index($0, key "=") != 1' "$target_file" > "$env_tmp"
  escaped="$(printf '%s' "$value" | sed 's/\\/\\\\/g; s/"/\\"/g')"
  printf '%s="%s"\n' "$key" "$escaped" >> "$env_tmp"
  chmod 0600 "$env_tmp"
  chown root:root "$env_tmp"
  mv "$env_tmp" "$target_file"
}

set_env_pair() {
  local primary="$1"
  local legacy="$2"
  local value="$3"
  set_file_value "$ENV_FILE" "$primary" "$value"
  set_file_value "$ENV_FILE" "$legacy" "$value"
}

set_waffo_pair() {
  local primary="$1"
  local legacy="$2"
  local value="$3"
  set_file_value "$WAFFO_ENV_FILE" "$primary" "$value"
  set_file_value "$WAFFO_ENV_FILE" "$legacy" "$value"
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
  value="$(printf '%s' "$value" | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//')"
  if [[ ${#value} -ge 2 && "${value:0:1}" == "'" && "${value: -1}" == "'" ]]; then
    value="${value:1:${#value}-2}"
  elif [[ ${#value} -ge 2 && "${value:0:1}" == '"' && "${value: -1}" == '"' ]]; then
    value="${value:1:${#value}-2}"
  fi
  if [[ -z "$value" && -n "$current" ]]; then
    value="$current"
  fi
  printf '%s' "$value"
}

usage() {
  cat <<'EOF'
Usage: provision-ubuntu.sh [--ref <branch|tag|commit>]

Installs the runtime dependencies, creates the independent PostgreSQL database and
application environment, then calls the GitHub-hosted deployment script.
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

BILLING_RUNTIME_ENV="${SHIHAI_RUNTIME_ENV:-${KB_RUNTIME_ENV:-production}}"
if [[ "$BILLING_RUNTIME_ENV" == "prod" ]]; then
  BILLING_RUNTIME_ENV="production"
fi
if [[ "$BILLING_RUNTIME_ENV" != "test" && "$BILLING_RUNTIME_ENV" != "production" ]]; then
  printf 'SHIHAI_RUNTIME_ENV must be test or production.\n' >&2
  exit 1
fi
WAFFO_ENVIRONMENT="${SHIHAI_WAFFO_ENV:-${WAFFO_ENV:-$BILLING_RUNTIME_ENV}}"
if [[ "$WAFFO_ENVIRONMENT" == "prod" ]]; then
  WAFFO_ENVIRONMENT="production"
fi
if [[ "$WAFFO_ENVIRONMENT" != "$BILLING_RUNTIME_ENV" ]]; then
  printf 'SHIHAI_WAFFO_ENV and SHIHAI_RUNTIME_ENV must identify the same environment.\n' >&2
  exit 1
fi
if [[ -f "$ENV_FILE" ]]; then
  assert_all_aliases "$ENV_FILE"
  existing_app_env="$(read_env_value SHIHAI_APP_ENV APP_ENV)"
  existing_billing_db_env="$(read_env_value SHIHAI_BILLING_DB_ENV BILLING_DB_ENV)"
  if [[ -n "$existing_app_env" && "$existing_app_env" != "$BILLING_RUNTIME_ENV" ]]; then
    printf 'Existing SHIHAI_APP_ENV=%s conflicts with requested SHIHAI_RUNTIME_ENV=%s.\n' \
      "$existing_app_env" "$BILLING_RUNTIME_ENV" >&2
    exit 1
  fi
  if [[ -n "$existing_billing_db_env" && "$existing_billing_db_env" != "$BILLING_RUNTIME_ENV" ]]; then
    printf 'Existing SHIHAI_BILLING_DB_ENV=%s conflicts with requested SHIHAI_RUNTIME_ENV=%s.\n' \
      "$existing_billing_db_env" "$BILLING_RUNTIME_ENV" >&2
    exit 1
  fi
fi
if [[ -f "$WAFFO_ENV_FILE" ]]; then
  assert_all_aliases "$WAFFO_ENV_FILE"
  existing_waffo_env="$(read_waffo_env_value SHIHAI_WAFFO_ENV WAFFO_ENV)"
  if [[ "$existing_waffo_env" == "prod" ]]; then
    existing_waffo_env="production"
  fi
  if [[ -n "$existing_waffo_env" && "$existing_waffo_env" != "$BILLING_RUNTIME_ENV" ]]; then
    printf 'Existing SHIHAI_WAFFO_ENV=%s conflicts with requested SHIHAI_RUNTIME_ENV=%s.\n' \
      "$existing_waffo_env" "$BILLING_RUNTIME_ENV" >&2
    exit 1
  fi
fi

if ((EUID != 0)); then
  printf 'Run this script as root.\n' >&2
  exit 1
fi

if [[ ! -r /etc/os-release ]]; then
  printf 'Cannot identify the operating system.\n' >&2
  exit 1
fi

# shellcheck disable=SC1091
. /etc/os-release
if [[ "${ID:-}" != "ubuntu" ]]; then
  printf 'This provisioner supports Ubuntu only; detected %s.\n' "${ID:-unknown}" >&2
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive
echo "==> 1/8 安装系统依赖"
apt-get update
apt-get install -y \
  ca-certificates \
  curl \
  git \
  gnupg \
  jq \
  libvips-tools \
  nginx \
  openssl \
  python3 \
  tesseract-ocr \
  tesseract-ocr-chi-sim \
  tar

echo "==> 2/8 检查 Java 17、Node.js 20+ 和 pnpm"
if ! command -v java >/dev/null 2>&1 || ! java -version 2>&1 | grep -q 'version "17\.'; then
  apt-get install -y openjdk-17-jdk-headless
  update-alternatives --set java /usr/lib/jvm/java-17-openjdk-amd64/bin/java
fi

node_major=0
if command -v node >/dev/null 2>&1; then
  node_major="$(node -p 'Number(process.versions.node.split(".")[0])')"
fi
if ((node_major < 20)); then
  curl -fsSL https://deb.nodesource.com/setup_22.x | bash -
  apt-get install -y nodejs
fi

if ! command -v pnpm >/dev/null 2>&1; then
  npm install --global pnpm@10.33.0
fi

echo "==> 3/8 安装 PostgreSQL 18 并配置 swap"
if ! dpkg -s postgresql-18 >/dev/null 2>&1; then
  apt-get install -y postgresql-common
  install -d -m 0755 /usr/share/postgresql-common/pgdg
  curl -fsSL \
    -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc \
    https://www.postgresql.org/media/keys/ACCC4CF8.asc
  printf 'deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt %s-pgdg main\n' \
    "${VERSION_CODENAME:?Missing Ubuntu codename}" \
    > /etc/apt/sources.list.d/pgdg.list
  apt-get update
  apt-get install -y postgresql-18
fi

systemctl enable --now postgresql

if ! swapon --show=NAME --noheadings | grep -q . && ((SWAP_SIZE_MB > 0)); then
  SWAP_FILE="/swapfile-zhishiku"
  if [[ ! -f "$SWAP_FILE" ]]; then
    fallocate -l "${SWAP_SIZE_MB}M" "$SWAP_FILE"
    chmod 0600 "$SWAP_FILE"
    mkswap "$SWAP_FILE"
  fi
  if ! swapon --show=NAME --noheadings | grep -qx "$SWAP_FILE"; then
    swapon "$SWAP_FILE"
  fi
  if ! grep -q "^$SWAP_FILE " /etc/fstab; then
    printf '%s none swap sw 0 0\n' "$SWAP_FILE" >> /etc/fstab
  fi
fi

echo "==> 4/8 创建运行用户和持久目录"
if ! id zhishiku >/dev/null 2>&1; then
  useradd --system --home /opt/zhishiku --shell /usr/sbin/nologin zhishiku
fi

install -d -o zhishiku -g zhishiku /opt/zhishiku/releases
install -d -o zhishiku -g zhishiku /var/lib/zhishiku/uploads
install -d -m 0750 /etc/zhishiku

DB_PORT="$(pg_lsclusters --no-header | awk '$1 == "18" && $4 == "online" {print $3; exit}')"
DB_PORT="${DB_PORT:-5432}"

echo "==> 5/8 初始化独立 PostgreSQL 数据库"
DB_PASSWORD=""
OWNER_PASSWORD=""
if [[ -f "$ENV_FILE" ]]; then
  DB_PASSWORD="${SHIHAI_DATABASE_PASSWORD:-$(read_env_value SHIHAI_DATABASE_PASSWORD DATABASE_PASSWORD)}"
  OWNER_PASSWORD="${SHIHAI_OWNER_PASSWORD:-$(read_env_value SHIHAI_OWNER_PASSWORD KB_OWNER_PASSWORD)}"
fi
DB_PASSWORD="${DB_PASSWORD:-$(openssl rand -hex 24)}"

if ! runuser -u postgres -- psql -p "$DB_PORT" -tAc \
  "SELECT 1 FROM pg_roles WHERE rolname='zhishiku'" | grep -q 1; then
  runuser -u postgres -- psql -p "$DB_PORT" -v ON_ERROR_STOP=1 \
    -c "CREATE ROLE zhishiku LOGIN PASSWORD '$DB_PASSWORD'"
else
  runuser -u postgres -- psql -p "$DB_PORT" -v ON_ERROR_STOP=1 \
    -c "ALTER ROLE zhishiku WITH LOGIN PASSWORD '$DB_PASSWORD'"
fi

if ! runuser -u postgres -- psql -p "$DB_PORT" -tAc \
  "SELECT 1 FROM pg_database WHERE datname='zhishiku'" | grep -q 1; then
  runuser -u postgres -- createdb -p "$DB_PORT" -O zhishiku zhishiku
fi
runuser -u postgres -- psql -p "$DB_PORT" -d zhishiku -v ON_ERROR_STOP=1 \
  -c 'CREATE EXTENSION IF NOT EXISTS pg_trgm'

auto_bootstrap="true"
if [[ -f "$ENV_FILE" ]] && grep -Eq '^(SHIHAI_AUTO_BOOTSTRAP|KB_AUTO_BOOTSTRAP)=false$' "$ENV_FILE"; then
  auto_bootstrap="false"
fi
if [[ "$auto_bootstrap" == "true" && -z "$OWNER_PASSWORD" ]]; then
  OWNER_PASSWORD="$(openssl rand -hex 16)"
fi

OWNER_EMAIL="${SHIHAI_OWNER_EMAIL:-$(read_env_value SHIHAI_OWNER_EMAIL KB_OWNER_EMAIL)}"
OWNER_EMAIL="${OWNER_EMAIL:-owner@seaunit.site}"
BOOTSTRAP_TOKEN="${SHIHAI_BOOTSTRAP_TOKEN:-$(read_env_value SHIHAI_BOOTSTRAP_TOKEN KB_BOOTSTRAP_TOKEN)}"
BOOTSTRAP_TOKEN="${BOOTSTRAP_TOKEN:-$(openssl rand -hex 32)}"

DEEPSEEK_API_KEY="${SHIHAI_DEEPSEEK_API_KEY:-$(read_env_value SHIHAI_DEEPSEEK_API_KEY DEEPSEEK_API_KEY)}"
DEEPSEEK_SEARCH_API_KEY="${SHIHAI_DEEPSEEK_SEARCH_API_KEY:-$(read_env_value SHIHAI_DEEPSEEK_SEARCH_API_KEY DEEPSEEK_SEARCH_API_KEY)}"
DEEPSEEK_FETCH_API_KEY="${SHIHAI_DEEPSEEK_FETCH_API_KEY:-$(read_env_value SHIHAI_DEEPSEEK_FETCH_API_KEY DEEPSEEK_FETCH_API_KEY)}"
SMTP_PASSWORD="${SHIHAI_SMTP_PASSWORD:-$(read_env_value SHIHAI_SMTP_PASSWORD SMTP_PASSWORD)}"

echo "==> 6/8 交互配置 AI/SMTP 密钥并写入受限环境文件"
DEEPSEEK_API_KEY="$(prompt_secret "DeepSeek AI API Key" "$DEEPSEEK_API_KEY")"
DEEPSEEK_SEARCH_API_KEY="$(prompt_secret "DeepSeek Search API Key（留空复用主 AI Key）" "$DEEPSEEK_SEARCH_API_KEY")"
DEEPSEEK_FETCH_API_KEY="$(prompt_secret "DeepSeek Fetch API Key（留空复用主 AI Key）" "$DEEPSEEK_FETCH_API_KEY")"
SMTP_PASSWORD="$(prompt_secret "SMTP 密码/应用专用密钥" "$SMTP_PASSWORD")"
WAFFO_MERCHANT_ID="${SHIHAI_WAFFO_MERCHANT_ID:-$(read_waffo_env_value SHIHAI_WAFFO_MERCHANT_ID WAFFO_MERCHANT_ID)}"
WAFFO_STORE_ID="${SHIHAI_WAFFO_STORE_ID:-$(read_waffo_env_value SHIHAI_WAFFO_STORE_ID WAFFO_STORE_ID)}"
WAFFO_PRIVATE_KEY_BASE64="${SHIHAI_WAFFO_PRIVATE_KEY_BASE64:-$(read_waffo_env_value SHIHAI_WAFFO_PRIVATE_KEY_BASE64 WAFFO_PRIVATE_KEY_BASE64)}"
WAFFO_PRODUCT_MAP="${SHIHAI_WAFFO_PRODUCT_MAP:-$(read_waffo_env_value SHIHAI_WAFFO_PRODUCT_MAP WAFFO_PRODUCT_MAP)}"
APP_TO_BRIDGE_SECRET="${SHIHAI_APP_TO_BRIDGE_SECRET:-$(read_env_value SHIHAI_APP_TO_BRIDGE_SECRET APP_TO_BRIDGE_SECRET)}"
BRIDGE_TO_APP_SECRET="${SHIHAI_BRIDGE_TO_APP_SECRET:-$(read_env_value SHIHAI_BRIDGE_TO_APP_SECRET BRIDGE_TO_APP_SECRET)}"

WAFFO_MERCHANT_ID="$(prompt_secret "Waffo 商户 ID" "$WAFFO_MERCHANT_ID")"
WAFFO_STORE_ID="$(prompt_secret "Waffo 店铺 ID（非 dry-run 必填）" "$WAFFO_STORE_ID")"
WAFFO_PRIVATE_KEY_BASE64="$(prompt_secret "Waffo 私钥 Base64" "$WAFFO_PRIVATE_KEY_BASE64")"
WAFFO_PRODUCT_MAP="$(prompt_secret "Waffo 商品映射（kb_pack_9=...，逗号分隔）" "$WAFFO_PRODUCT_MAP")"
APP_TO_BRIDGE_SECRET="$(prompt_secret "Spring 到 Waffo 桥接内部密钥（留空自动生成）" "$APP_TO_BRIDGE_SECRET")"
BRIDGE_TO_APP_SECRET="$(prompt_secret "Waffo 桥接到 Spring 内部密钥（留空自动生成）" "$BRIDGE_TO_APP_SECRET")"

if [[ -z "$APP_TO_BRIDGE_SECRET" ]]; then
  APP_TO_BRIDGE_SECRET="$(openssl rand -hex 32)"
fi
if [[ -z "$BRIDGE_TO_APP_SECRET" ]]; then
  BRIDGE_TO_APP_SECRET="$(openssl rand -hex 32)"
fi
if [[ "$APP_TO_BRIDGE_SECRET" == "$BRIDGE_TO_APP_SECRET" ]]; then
  printf 'SHIHAI_APP_TO_BRIDGE_SECRET and SHIHAI_BRIDGE_TO_APP_SECRET must be different; generating a new bridge-to-app secret.\n' >&2
  BRIDGE_TO_APP_SECRET="$(openssl rand -hex 32)"
fi

BILLING_ENABLED_VALUE="${SHIHAI_BILLING_ENABLED:-$(read_env_value SHIHAI_BILLING_ENABLED BILLING_ENABLED)}"
BILLING_ENABLED_VALUE="${BILLING_ENABLED_VALUE:-false}"
if [[ "$BILLING_ENABLED_VALUE" != "true" && "$BILLING_ENABLED_VALUE" != "false" ]]; then
  printf 'SHIHAI_BILLING_ENABLED must be true or false.\n' >&2
  exit 1
fi
if [[ "$BILLING_ENABLED_VALUE" == "true" ]] &&
  [[ -z "$WAFFO_MERCHANT_ID" || -z "$WAFFO_PRIVATE_KEY_BASE64" || -z "$WAFFO_PRODUCT_MAP" ]]; then
  printf 'SHIHAI_BILLING_ENABLED=true requires Waffo merchant, private key, and product map.\n' >&2
  exit 1
fi

if [[ -z "$DEEPSEEK_SEARCH_API_KEY" ]]; then
  DEEPSEEK_SEARCH_API_KEY="$DEEPSEEK_API_KEY"
fi
if [[ -z "$DEEPSEEK_FETCH_API_KEY" ]]; then
  DEEPSEEK_FETCH_API_KEY="$DEEPSEEK_API_KEY"
fi

set_env_pair SHIHAI_APP_ENV APP_ENV "$BILLING_RUNTIME_ENV"
set_env_pair SHIHAI_SERVER_PORT SERVER_PORT 8082
set_env_pair SHIHAI_DATABASE_URL DATABASE_URL "jdbc:postgresql://127.0.0.1:${DB_PORT}/zhishiku"
set_env_pair SHIHAI_DATABASE_USERNAME DATABASE_USERNAME zhishiku
set_env_pair SHIHAI_DATABASE_PASSWORD DATABASE_PASSWORD "$DB_PASSWORD"
set_env_pair SHIHAI_SESSION_COOKIE_SECURE SESSION_COOKIE_SECURE true
set_env_pair SHIHAI_STORAGE_ROOT KB_STORAGE_ROOT /var/lib/zhishiku/uploads
set_env_pair SHIHAI_WEB_IMPORT_ALLOW_UNBILLED_AI KB_WEB_IMPORT_ALLOW_UNBILLED_AI false
set_env_pair SHIHAI_BILLING_ENABLED BILLING_ENABLED "$BILLING_ENABLED_VALUE"
set_env_pair SHIHAI_BILLING_DB_ENV BILLING_DB_ENV "$BILLING_RUNTIME_ENV"
set_env_pair SHIHAI_APP_TO_BRIDGE_SECRET APP_TO_BRIDGE_SECRET "$APP_TO_BRIDGE_SECRET"
set_env_pair SHIHAI_BRIDGE_TO_APP_SECRET BRIDGE_TO_APP_SECRET "$BRIDGE_TO_APP_SECRET"
set_env_pair SHIHAI_BILLING_CATALOG_VERSION BILLING_CATALOG_VERSION 2026-10-mvp
set_env_pair SHIHAI_BILLING_ADMIN_EMAILS BILLING_ADMIN_EMAILS "$OWNER_EMAIL"
set_env_pair SHIHAI_CHECKOUT_SUCCESS_URL CHECKOUT_SUCCESS_URL "https://seaunit.site/kb/billing/return"
set_env_pair SHIHAI_BILLING_WEBHOOK_URL BILLING_WEBHOOK_URL "https://seaunit.site/kb/pay/webhooks/waffo"
set_env_pair \
  SHIHAI_REFUND_SUPPORT_URL \
  REFUND_SUPPORT_URL \
  "mailto:${OWNER_EMAIL}?subject=Zhishiku%20refund"
set_env_pair SHIHAI_REFUND_RESPONSE_TARGET REFUND_RESPONSE_TARGET "3 个工作日"
set_env_pair SHIHAI_AUTO_BOOTSTRAP KB_AUTO_BOOTSTRAP "$auto_bootstrap"
set_env_pair SHIHAI_OWNER_EMAIL KB_OWNER_EMAIL "$OWNER_EMAIL"
set_env_pair SHIHAI_BOOTSTRAP_TOKEN KB_BOOTSTRAP_TOKEN "$BOOTSTRAP_TOKEN"
set_env_pair SHIHAI_DEEPSEEK_API_KEY DEEPSEEK_API_KEY "$DEEPSEEK_API_KEY"
set_env_pair \
  SHIHAI_DEEPSEEK_SEARCH_API_KEY DEEPSEEK_SEARCH_API_KEY "$DEEPSEEK_SEARCH_API_KEY"
set_env_pair SHIHAI_DEEPSEEK_FETCH_API_KEY DEEPSEEK_FETCH_API_KEY "$DEEPSEEK_FETCH_API_KEY"
set_env_pair SHIHAI_SMTP_PASSWORD SMTP_PASSWORD "$SMTP_PASSWORD"
set_env_pair SHIHAI_REGISTRATION_ENABLED REGISTRATION_ENABLED "${REGISTRATION_ENABLED:-true}"
if [[ "$auto_bootstrap" == "true" ]]; then
  set_env_pair SHIHAI_OWNER_PASSWORD KB_OWNER_PASSWORD "$OWNER_PASSWORD"
fi

if [[ -n "$WAFFO_MERCHANT_ID" && -n "$WAFFO_PRIVATE_KEY_BASE64" && -n "$WAFFO_PRODUCT_MAP" ]]; then
  set_waffo_pair SHIHAI_WAFFO_ENV WAFFO_ENV "$WAFFO_ENVIRONMENT"
  set_waffo_pair SHIHAI_WAFFO_STORE_ID WAFFO_STORE_ID "$WAFFO_STORE_ID"
  set_waffo_pair SHIHAI_WAFFO_MERCHANT_ID WAFFO_MERCHANT_ID "$WAFFO_MERCHANT_ID"
  set_waffo_pair \
    SHIHAI_WAFFO_PRIVATE_KEY_BASE64 WAFFO_PRIVATE_KEY_BASE64 "$WAFFO_PRIVATE_KEY_BASE64"
  set_waffo_pair SHIHAI_WAFFO_PRODUCT_MAP WAFFO_PRODUCT_MAP "$WAFFO_PRODUCT_MAP"
  set_waffo_pair SHIHAI_WAFFO_CURRENCY WAFFO_CURRENCY CNY
  set_waffo_pair SHIHAI_WAFFO_DRY_RUN WAFFO_DRY_RUN false
  set_waffo_pair SHIHAI_WAFFO_BRIDGE_PORT WAFFO_BRIDGE_PORT 8091
  set_waffo_pair \
    SHIHAI_WAFFO_SUCCESS_URL_ALLOWLIST \
    WAFFO_SUCCESS_URL_ALLOWLIST \
    "https://seaunit.site/kb/billing/return"
  set_waffo_pair \
    SHIHAI_INTERNAL_EVENT_URL \
    INTERNAL_EVENT_URL \
    "http://127.0.0.1:8082/kb/api/internal/billing/waffo-events"
  set_waffo_pair \
    SHIHAI_APP_TO_BRIDGE_SECRET APP_TO_BRIDGE_SECRET "$APP_TO_BRIDGE_SECRET"
  set_waffo_pair \
    SHIHAI_BRIDGE_TO_APP_SECRET BRIDGE_TO_APP_SECRET "$BRIDGE_TO_APP_SECRET"
fi

echo "==> 7/8 执行现有应用和 Nginx 预检"
preflight_tmp="$(mktemp)"
curl -fsSL "$RAW_BASE/$REF/deploy/scripts/preflight-existing-app.sh" -o "$preflight_tmp"
bash "$preflight_tmp"
rm -f "$preflight_tmp"

echo "==> 8/8 从 GitHub 拉取 $REF 并部署"
deploy_tmp="$(mktemp)"
curl -fsSL "$RAW_BASE/$REF/deploy/scripts/deploy.sh" -o "$deploy_tmp"
chmod 0700 "$deploy_tmp"
SHIHAI_SECRETS_ALREADY_PROMPTED=1 bash "$deploy_tmp" --ref "$REF"
rm -f "$deploy_tmp"

printf '\n识海部署配置已完成。\n'
printf 'DeepSeek AI 密钥：%s\n' "$([[ -n "$DEEPSEEK_API_KEY" ]] && printf 已配置 || printf 未配置)"
printf 'SMTP 密钥：%s\n' "$([[ -n "$SMTP_PASSWORD" ]] && printf 已配置 || printf 未配置)"
if [[ -f /root/zhishiku-initial-password ]]; then
  printf 'Initial login details: /root/zhishiku-initial-password\n'
fi
printf 'Public entry: https://seaunit.site/kb/\n'
