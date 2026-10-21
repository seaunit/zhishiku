#!/usr/bin/env bash
set -Eeuo pipefail

KB_PORT="${KB_PORT:-8082}"
failures=0

log() {
  printf '[preflight] %s\n' "$*"
}

warn() {
  printf '[preflight] WARNING: %s\n' "$*" >&2
}

fail() {
  printf '[preflight] ERROR: %s\n' "$*" >&2
  failures=$((failures + 1))
}

log "Memory"
free -h

log "Disk"
df -h /opt /var

if command -v systemctl >/dev/null 2>&1; then
  for unit in nginx shuati.service shuati-payments.service; do
    if systemctl is-active --quiet "$unit"; then
      log "$unit is active"
    else
      warn "$unit is not active; verify whether this is expected"
    fi
  done
fi

if command -v ss >/dev/null 2>&1; then
  if ss -ltnH "sport = :$KB_PORT" | grep -q .; then
    service_pid=""
    if command -v systemctl >/dev/null 2>&1; then
      service_pid="$(systemctl show -p MainPID --value zhishiku-api 2>/dev/null || true)"
    fi
    if [[ -n "$service_pid" && "$service_pid" != "0" ]] \
      && ss -ltnpH "sport = :$KB_PORT" | grep -q "pid=$service_pid,"; then
      log "Port $KB_PORT is serving the existing zhishiku-api release"
    else
      fail "Port $KB_PORT is already in use by another process"
    fi
  else
    log "Port $KB_PORT is available"
  fi

  if ss -ltnH "sport = :5432" | grep -q .; then
    warn "Port 5432 is already in use; confirm it is the intended PostgreSQL cluster"
  fi
fi

if command -v nginx >/dev/null 2>&1; then
  if nginx -t; then
    log "Current Nginx configuration is valid"
  else
    fail "Current Nginx configuration is invalid"
  fi
else
  warn "Nginx is not installed yet"
fi

if ((failures > 0)); then
  printf '[preflight] %d blocking issue(s) found\n' "$failures" >&2
  exit 1
fi

log "Preflight completed"
