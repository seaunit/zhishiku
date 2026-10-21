#!/usr/bin/env bash
set -Eeuo pipefail

REPO_URL="${KB_REPO_URL:-https://github.com/seaunit/zhishiku.git}"
RAW_BASE="${KB_RAW_BASE:-https://raw.githubusercontent.com/seaunit/zhishiku}"
REF="${KB_REF:-main}"
ENV_FILE="${KB_ENV_FILE:-/etc/zhishiku/kb.env}"
SWAP_SIZE_MB="${KB_SWAP_SIZE_MB:-2048}"
PROMPT_SECRETS="${KB_PROMPT_SECRETS:-1}"
ROTATE_SECRETS="${KB_ROTATE_SECRETS:-0}"

read_env_value() {
  local key="$1"
  if [[ ! -f "$ENV_FILE" ]]; then
    return 0
  fi
  awk -v key="$key" '
    index($0, key "=") == 1 {
      sub(/^[^=]*=/, "")
      print
      exit
    }
  ' "$ENV_FILE"
}

prompt_secret() {
  local label="$1"
  local current="$2"
  local value=""

  if [[ "$PROMPT_SECRETS" != "1" || ! -r /dev/tty ]]; then
    printf '%s' "$current"
    return 0
  fi
  if [[ -n "$current" && "$ROTATE_SECRETS" != "1" ]]; then
    printf '%s' "$current"
    return 0
  fi

  if [[ -n "$current" ]]; then
    printf '%s [configured, blank keeps current]: ' "$label" > /dev/tty
  else
    printf '%s [input hidden, blank leaves unset]: ' "$label" > /dev/tty
  fi
  IFS= read -r -s value < /dev/tty || value=""
  printf '\n' > /dev/tty
  value="$(printf '%s' "$value" | tr -d '[:space:]')"
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
apt-get update
apt-get install -y \
  ca-certificates \
  curl \
  git \
  gnupg \
  jq \
  nginx \
  openssl \
  python3 \
  tar

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

if ! id zhishiku >/dev/null 2>&1; then
  useradd --system --home /opt/zhishiku --shell /usr/sbin/nologin zhishiku
fi

install -d -o zhishiku -g zhishiku /opt/zhishiku/releases
install -d -o zhishiku -g zhishiku /var/lib/zhishiku/uploads
install -d -m 0750 /etc/zhishiku

DB_PORT="$(pg_lsclusters --no-header | awk '$1 == "18" && $4 == "online" {print $3; exit}')"
DB_PORT="${DB_PORT:-5432}"

DB_PASSWORD=""
OWNER_PASSWORD=""
if [[ -f "$ENV_FILE" ]]; then
  DB_PASSWORD="${DATABASE_PASSWORD:-$(read_env_value DATABASE_PASSWORD)}"
  OWNER_PASSWORD="${KB_OWNER_PASSWORD:-$(read_env_value KB_OWNER_PASSWORD)}"
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
if [[ -f "$ENV_FILE" ]] && grep -q '^KB_AUTO_BOOTSTRAP=false$' "$ENV_FILE"; then
  auto_bootstrap="false"
fi
if [[ "$auto_bootstrap" == "true" && -z "$OWNER_PASSWORD" ]]; then
  OWNER_PASSWORD="$(openssl rand -hex 16)"
fi

OWNER_EMAIL="${KB_OWNER_EMAIL:-$(read_env_value KB_OWNER_EMAIL)}"
OWNER_EMAIL="${OWNER_EMAIL:-owner@seaunit.site}"
BOOTSTRAP_TOKEN="${KB_BOOTSTRAP_TOKEN:-$(read_env_value KB_BOOTSTRAP_TOKEN)}"
BOOTSTRAP_TOKEN="${BOOTSTRAP_TOKEN:-$(openssl rand -hex 32)}"

DEEPSEEK_API_KEY="${DEEPSEEK_API_KEY:-$(read_env_value DEEPSEEK_API_KEY)}"
DEEPSEEK_SEARCH_API_KEY="${DEEPSEEK_SEARCH_API_KEY:-$(read_env_value DEEPSEEK_SEARCH_API_KEY)}"
DEEPSEEK_FETCH_API_KEY="${DEEPSEEK_FETCH_API_KEY:-$(read_env_value DEEPSEEK_FETCH_API_KEY)}"
SMTP_PASSWORD="${SMTP_PASSWORD:-$(read_env_value SMTP_PASSWORD)}"

DEEPSEEK_API_KEY="$(prompt_secret "DeepSeek AI API key" "$DEEPSEEK_API_KEY")"
DEEPSEEK_SEARCH_API_KEY="$(prompt_secret "DeepSeek Search API key (blank reuses AI key)" "$DEEPSEEK_SEARCH_API_KEY")"
DEEPSEEK_FETCH_API_KEY="$(prompt_secret "DeepSeek Fetch API key (blank reuses AI key)" "$DEEPSEEK_FETCH_API_KEY")"
SMTP_PASSWORD="$(prompt_secret "SMTP password/app key" "$SMTP_PASSWORD")"

if [[ -z "$DEEPSEEK_SEARCH_API_KEY" ]]; then
  DEEPSEEK_SEARCH_API_KEY="$DEEPSEEK_API_KEY"
fi
if [[ -z "$DEEPSEEK_FETCH_API_KEY" ]]; then
  DEEPSEEK_FETCH_API_KEY="$DEEPSEEK_API_KEY"
fi

env_tmp="$(mktemp)"
cat > "$env_tmp" <<EOF
APP_ENV=production
SERVER_PORT=8082
DATABASE_URL=jdbc:postgresql://127.0.0.1:${DB_PORT}/zhishiku
DATABASE_USERNAME=zhishiku
DATABASE_PASSWORD=${DB_PASSWORD}
SESSION_COOKIE_SECURE=true
KB_STORAGE_ROOT=/var/lib/zhishiku/uploads
KB_AUTO_BOOTSTRAP=${auto_bootstrap}
KB_OWNER_EMAIL=${OWNER_EMAIL}
KB_BOOTSTRAP_TOKEN=${BOOTSTRAP_TOKEN}
DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY}
DEEPSEEK_SEARCH_API_KEY=${DEEPSEEK_SEARCH_API_KEY}
DEEPSEEK_FETCH_API_KEY=${DEEPSEEK_FETCH_API_KEY}
SMTP_PASSWORD=${SMTP_PASSWORD}
EOF
if [[ "$auto_bootstrap" == "true" ]]; then
  printf 'KB_OWNER_PASSWORD=%s\n' "$OWNER_PASSWORD" >> "$env_tmp"
fi
chmod 0600 "$env_tmp"
chown root:root "$env_tmp"
mv "$env_tmp" "$ENV_FILE"

preflight_tmp="$(mktemp)"
curl -fsSL "$RAW_BASE/$REF/deploy/scripts/preflight-existing-app.sh" -o "$preflight_tmp"
bash "$preflight_tmp"
rm -f "$preflight_tmp"

deploy_tmp="$(mktemp)"
curl -fsSL "$RAW_BASE/$REF/deploy/scripts/deploy.sh" -o "$deploy_tmp"
chmod 0700 "$deploy_tmp"
bash "$deploy_tmp" --ref "$REF"
rm -f "$deploy_tmp"

printf '\nZhishiku provisioning completed.\n'
printf 'DeepSeek AI: %s\n' "$([[ -n "$DEEPSEEK_API_KEY" ]] && printf configured || printf unset)"
printf 'SMTP credential: %s\n' "$([[ -n "$SMTP_PASSWORD" ]] && printf configured || printf unset)"
if [[ -f /root/zhishiku-initial-password ]]; then
  printf 'Initial login details: /root/zhishiku-initial-password\n'
fi
printf 'Public entry: https://seaunit.site/kb/\n'
