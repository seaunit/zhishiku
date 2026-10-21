#!/usr/bin/env bash
set -Eeuo pipefail

REPO_URL="${KB_REPO_URL:-https://github.com/seaunit/zhishiku.git}"
RAW_BASE="${KB_RAW_BASE:-https://raw.githubusercontent.com/seaunit/zhishiku}"
REF="${KB_REF:-main}"
ENV_FILE="${KB_ENV_FILE:-/etc/zhishiku/kb.env}"

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
  # shellcheck disable=SC1090
  set -a
  . "$ENV_FILE"
  set +a
  DB_PASSWORD="${DATABASE_PASSWORD:-}"
  OWNER_PASSWORD="${KB_OWNER_PASSWORD:-}"
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
KB_OWNER_EMAIL=${KB_OWNER_EMAIL:-owner@seaunit.site}
KB_BOOTSTRAP_TOKEN=$(openssl rand -hex 32)
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
if [[ -f /root/zhishiku-initial-password ]]; then
  printf 'Initial login details: /root/zhishiku-initial-password\n'
fi
printf 'Public entry: https://seaunit.site/kb/\n'
