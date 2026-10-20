# Deployment Coexistence Plan: seaunit.site

**Feature**: `001-ai-personal-knowledge-base`

**Inspected**: 2026-10-21

**Status**: Inspection and design only; no production changes were made

## Current Server Baseline

| Item | Observed |
|------|----------|
| Host | `8.210.73.129` |
| Domain | `https://seaunit.site/` and `https://www.seaunit.site/` |
| OS | Ubuntu 24.04.2 LTS |
| Resources | 2 vCPU, 3.4 GiB RAM, no swap, 49 GiB disk, about 1.8 GiB currently available |
| Docker | Not installed |
| Web entry | Native Nginx 1.24.0 on ports 80 and 443 |
| Runtime | Native systemd services |
| DNS/CDN | Cloudflare edge |

The current host serves an existing application named “拾题”. The application is not a
Docker deployment.

## Existing Process Topology

| Component | Runtime | Binding |
|-----------|---------|---------|
| Spring Boot backend | `systemd: shuati.service` | `*:8080` |
| Payment sidecar | `systemd: shuati-payments.service` | `127.0.0.1:8090` |
| MySQL | system package | `127.0.0.1:3306` |
| Redis | system package | `127.0.0.1:6379` |
| Nginx | system package | `0.0.0.0:80`, `0.0.0.0:443` |
| SSH | system service | port 22 |
| Cloudflare Tunnel helper | manually running `cloudflared` process | local `127.0.0.1:20241` |

`cloudflared.service` is currently inactive; the running tunnel process is not managed by the
active systemd unit. This should be reviewed separately before relying on it for production
availability.

## Existing Files and Services

| Path or service | Purpose |
|-----------------|---------|
| `/opt/shuati/` | Application root |
| `/opt/shuati/shuati-backend.jar` | Spring Boot backend |
| `/opt/shuati/frontend/` | Existing SPA and static files |
| `/opt/shuati/payments/` | Payment sidecar |
| `/opt/shuati/deploy/` | Nginx, systemd, and deployment scripts |
| `/etc/shuati/env` | Existing backend environment file |
| `/etc/shuati/payments.env` | Existing payment environment file |
| `/etc/nginx/conf.d/shuati.conf` | Active Nginx site configuration |
| `/etc/systemd/system/shuati.service` | Existing backend unit |
| `/etc/systemd/system/shuati-payments.service` | Existing payment unit |

Deployment scripts in `/opt/shuati/deploy/scripts/deploy.sh` upload the jar, frontend,
payment sidecar, and `deploy/` directory, restart the two systemd units, validate Nginx, and
reload it. Nginx source templates must therefore be updated in both:

- `/opt/shuati/deploy/nginx/shuati.conf`
- `/opt/shuati/deploy/nginx/shuati-prod.conf`

Updating only `/etc/nginx/conf.d/shuati.conf` is fragile because a future deployment or
configuration refresh may regenerate it.

## Current HTTP Routing

| Path | Upstream or file |
|------|------------------|
| `/` | Spring Boot landing page at `127.0.0.1:8080` |
| `/app/**` | Existing SPA from `/opt/shuati/frontend` |
| `/api/**` | Spring Boot at `127.0.0.1:8080` |
| `/api/auth/**` | Spring Boot auth routes with stricter rate limiting |
| `/bank/**` | Spring Boot public bank pages |
| `/sitemap.xml` | Spring Boot |
| `/actuator/health` | Spring Boot |
| `/pay/webhooks/**` | Payment sidecar at `127.0.0.1:8090` |
| `/apk/**` | `/opt/shuati/apk` |
| `/assets/**` | Existing SPA asset cache |

Both `/` and `/app` currently return `200`. `/` is a backend-rendered landing page and `/app`
is the SPA.

## Target URL Layout

```text
https://seaunit.site/
  -> new portal with two entries:
       - 拾题 -> /app
       - 知识库 -> /kb

https://seaunit.site/app
  -> existing 拾题 SPA, unchanged

https://seaunit.site/kb
  -> new AI personal knowledge base, path-prefixed deployment

https://seaunit.site/bank/...
  -> existing public question-bank pages, unchanged

https://seaunit.site/api/...
  -> existing Spring Boot API, unchanged

https://seaunit.site/pay/webhooks/...
  -> existing payment sidecar, unchanged
```

The knowledge base must use `basePath=/kb`. Its routes and API are therefore:

```text
/kb
/kb/library
/kb/library/:id
/kb/capture
/kb/processing
/kb/trash
/kb/settings
/kb/api/...
/kb/_next/...
```

The new application must not reuse the existing `/api`, `/auth`, `/assets`, or `/app` route
prefixes.

## Nginx Coexistence Rules

1. Preserve all existing `/api`, `/api/auth`, `/bank`, `/sitemap.xml`, `/actuator`, `/pay`,
   `/apk`, and `/app` behavior.
2. Add an exact `location = /` for the new two-product portal.
3. Add `/kb` before the SPA catch-all and preserve the `/kb` prefix when proxying.
4. Bind the knowledge base application to `127.0.0.1:3000`; do not expose port 3000 publicly.
5. Cache hashed `_next/static` assets aggressively, but do not cache HTML or search responses.
6. Use a separate session cookie name and path scoped to `/kb`; the existing application's
   cookies must not be replaced.
7. Set `client_max_body_size` only for `/kb` to the approved upload limit.
8. Keep Nginx validation and rollback in the deployment process:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

Illustrative route shape:

```nginx
location = / {
    root /opt/seaunit-portal;
    try_files /index.html =404;
    add_header Cache-Control "no-store";
}

location = /kb {
    return 308 /kb/;
}

location /kb/_next/static/ {
    proxy_pass http://127.0.0.1:3000;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto https;
    add_header Cache-Control "public, max-age=31536000, immutable";
}

location /kb/ {
    proxy_pass http://127.0.0.1:3000;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $shuati_real_ip;
    proxy_set_header X-Forwarded-For $shuati_real_ip;
    proxy_set_header X-Forwarded-Proto https;
    proxy_read_timeout 300s;
    client_max_body_size 25m;
}
```

The final configuration must be tested against the actual existing `shuati.conf`; this
snippet is a design guide, not a configuration to apply blindly.

## Portal Home Page

Create a separate static portal at:

```text
/opt/seaunit-portal/index.html
```

The portal should:

- Identify the site and present two clear application entries.
- Link “拾题” to `/app` and “知识库” to `/kb`.
- Keep the existing public question-bank and SEO routes reachable.
- Avoid JavaScript and external dependencies so it remains stable during application deploys.
- Include Chinese as the default language with an English option if the portal itself is
  localized.

The current backend-rendered `/` landing page remains available at a separate path during
migration, such as `/shuati`, if its SEO content must be retained temporarily.

## Resource Constraint and Deployment Profile

The existing server currently has only about 1.8 GiB RAM available and no swap. Running the
full knowledge-base stack on this host would add:

- PostgreSQL
- Meilisearch
- S3-compatible object storage or a local equivalent
- Web/API process
- Background worker
- Browser capture worker

This is a high memory-pressure profile on a 3.4 GiB host alongside the existing Spring Boot,
MySQL, Redis, and payment sidecar. It is not recommended to deploy the full stack without a
capacity review.

### Recommended production profile

- Upgrade the host to at least 4 vCPU / 8 GiB RAM, or place the knowledge base on a separate
  host.
- Keep Nginx on the existing host and route `/kb` to the knowledge-base service.
- Use Docker Compose only for the knowledge-base stack if Docker is installed and resource
  limits are enforced.
- Keep the existing 拾题 services native and unchanged.
- Bind all new services to loopback and expose only Nginx.

### Current-host constrained profile

If the same host must be used immediately:

- Add swap and explicit memory limits before starting the new stack.
- Do not deploy a full Meilisearch + object-storage + browser stack without measuring idle and
  screenshot-burst memory.
- Prefer the smallest possible deployment profile and disable optional AI integrations until
  the provider and resource PoCs pass.
- Keep the browser worker isolated and cap concurrent capture jobs to one.
- Define a maintenance window and rollback path for the portal and Nginx changes.

The current host, as inspected, does not have enough headroom to promise the 100,000-item
search target while the existing application remains under load.

## Knowledge-Base Runtime Layout on the Current Host

If native systemd deployment is selected instead of Docker:

```text
/opt/zhishiku/                 application release
/etc/zhishiku/kb.env           environment file, mode 600
/var/lib/zhishiku/             durable data if local storage is used
/var/log/zhishiku/             optional log directory; journald preferred
```

Suggested units:

```text
zhishiku-web.service
zhishiku-worker.service
zhishiku-browser-worker.service
```

The web service should bind only to `127.0.0.1:3000`. Worker services should not expose public
ports. The existing Node.js 20 installation is not sufficient for the planned Node.js 24
runtime; install a separate Node 24 runtime or use a container image.

## Environment and Secret Rules

- Do not append knowledge-base secrets to `/etc/shuati/env`.
- Create a separate environment file at `/etc/zhishiku/kb.env` with mode `600`.
- Use separate credentials and, where possible, separate DeepSeek keys from the existing app.
- Never place real keys in the repository, Nginx config, systemd unit files, logs, or this
  document.
- Startup validation may print missing variable names but never values.
- The knowledge-base session cookie must use a distinct name and path from the existing app.

Expected new variable names include:

```text
APP_BASE_URL
SESSION_SECRET
DATABASE_URL
MEILI_URL
MEILI_MASTER_KEY
S3_ENDPOINT
S3_BUCKET
S3_ACCESS_KEY
S3_SECRET_KEY
DEEPSEEK_API_KEY
DEEPSEEK_SEARCH_API_KEY
DEEPSEEK_FETCH_API_KEY
TRASH_RETENTION_DAYS
BACKUP_RETENTION_DAYS
MAX_UPLOAD_BYTES
LOG_LEVEL
```

## Deployment Order

1. Record a release snapshot of the existing Nginx config and services.
2. Verify current health endpoints.
3. Deploy the knowledge-base runtime on `127.0.0.1:3000` without public routing.
4. Verify `/kb` locally through Nginx.
5. Add the portal and root exact location.
6. Run `nginx -t`; on failure, restore the previous config.
7. Verify `/`, `/app`, `/kb`, `/api/*`, `/bank/*`, and `/pay/*`.
8. Check cookies, login/logout, search, uploads, AI authorization, and recycle bin.
9. Monitor memory, disk, Nginx errors, and systemd restarts for at least one day.

## Rollback

- Keep timestamped Nginx backups.
- Revert the root portal location and `/kb` route together.
- Stop and disable the knowledge-base units or containers.
- Do not delete knowledge-base data during an application rollback.
- Re-check the existing app's `/`, `/app`, `/api`, `/bank`, and payment routes.

## Explicitly Out of Scope Until a Separate Change

- Modifying the existing 拾题 application code.
- Changing existing MySQL or Redis data.
- Changing the existing payment webhook path.
- Replacing existing TLS certificates.
- Installing Docker or changing server packages.
