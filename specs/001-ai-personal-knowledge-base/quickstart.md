# Phase 1 Quickstart and Validation: AI 个人知识库

**Feature**: `001-ai-personal-knowledge-base`

**Purpose**: Validate the planned feature end to end after implementation. This is not a
current implementation report; the repository does not yet contain the application.

## Prerequisites

The current production host does not have Docker installed and already runs a native systemd
application. Docker Compose is a local-development or capacity-approved deployment option, not
the current production baseline. See [deployment.md](deployment.md) before changing
`seaunit.site`.

- Docker Engine with Compose v2
- Node.js 24 LTS
- pnpm
- A Chromium-compatible browser for Playwright
- Optional real DeepSeek credentials for live-provider tests

## Planned Environment Variables

Create a local `.env` from the committed `.env.example`. The example file contains names and
safe placeholders only, never real credentials.

```dotenv
APP_ENV=development
APP_BASE_URL=http://localhost:3000
SESSION_SECRET=replace-me
DATABASE_URL=postgresql://app:replace-me@localhost:5432/zhishiku
MEILI_URL=http://localhost:7700
MEILI_MASTER_KEY=replace-me
S3_ENDPOINT=http://localhost:9000
S3_BUCKET=zhishiku
S3_ACCESS_KEY=replace-me
S3_SECRET_KEY=replace-me
DEEPSEEK_API_KEY=replace-me
DEEPSEEK_SEARCH_API_KEY=replace-me
DEEPSEEK_FETCH_API_KEY=replace-me
TRASH_RETENTION_DAYS=30
BACKUP_RETENTION_DAYS=30
MAX_UPLOAD_BYTES=26214400
LOG_LEVEL=info
```

The application must fail feature-specific startup checks with variable names only when a
value is missing. It must never print the values.

## Start the Planned Stack

```powershell
pnpm install
docker compose -f infra/compose.yml up -d
pnpm db:migrate
pnpm dev
```

Expected services:

- Web/API: `http://localhost:3000`
- PostgreSQL: internal network only
- Meilisearch: internal network or local diagnostic port
- S3/MinIO: internal network or local diagnostic port
- Worker and browser worker: no public ports

## Production Coexistence

The current production routing plan is:

```text
https://seaunit.site/       -> portal with 拾题 and 知识库 entries
https://seaunit.site/app    -> existing 拾题 SPA
https://seaunit.site/kb     -> new knowledge base
```

The existing `/api`, `/bank`, `/pay`, and `/apk` routes must remain on the existing services.
The knowledge-base application must use `/kb` and `/kb/api`, bind to `127.0.0.1:3000`, and be
proxied through the existing Nginx. The current host has limited memory; see
[deployment.md](deployment.md) before installing or starting the application stack.

## Validation Scenarios

### 1. Bootstrap and access control

1. Open `/setup` with a one-time bootstrap token.
2. Create the sole owner account.
3. Confirm `/setup` closes after bootstrap.
4. Sign out and confirm protected routes reject anonymous access.
5. Sign in and confirm Chinese is the default interface language.

### 2. Manual save and immediate search

1. Create a note with title, body, note, and source link.
2. Confirm the item is visible in `/library` immediately.
3. Search with the only search box using values found in title, body, note, category, and tag.
4. Confirm each query returns the item and shows which field matched.
5. Confirm there is no second search box in the library, processing, or trash pages.

### 3. AI authorization and classification

1. Save a new text item without an authorization.
2. Confirm the item remains active and searchable while awaiting classification.
3. Grant classification permission for the selected provider and `text` content type.
4. Confirm the item receives a summary, category, and at least one tag.
5. Simulate low confidence and confirm a provisional result plus review marker appears without
   blocking search.
6. Correct the category or tag and confirm the field becomes locked.
7. Run re-analysis and confirm the locked field does not change.

### 4. Secret scanning and redaction

1. Submit text containing a synthetic canary credential.
2. Confirm the credential is removed, the remaining text is saved, and the message never shows
   the matched value.
3. Confirm the canary does not appear in PostgreSQL, Meilisearch, object storage, logs,
   screenshots, or telemetry.
4. Upload an image with a detectable synthetic secret and confirm only the redacted derived
   image remains.
5. Simulate an image or document that cannot be fully scanned and confirm the entire
   submission is rejected with no durable rows or objects.

### 5. Document and screenshot import

1. Upload a supported text, PDF, or word-processing document.
2. Confirm the item is created and searchable before extraction finishes.
3. Confirm processing status changes through the documented state machine.
4. Upload a screenshot with clear text and confirm OCR, summary, category, and tags populate.
5. Retry a simulated extraction failure and confirm no duplicate item or attachment is created.

### 6. Single-page webpage import

1. Use a local public-HTML fixture or approved public page.
2. Submit exactly one URL.
3. Confirm the browser worker opens only that page and captures a bounded screenshot.
4. Confirm no link discovery, sitemap traversal, infinite scrolling, or multi-page jobs occur.
5. Confirm redirects are limited and private-IP, loopback, CGNAT, metadata, and credential URLs
   are rejected.
6. Confirm login/CAPTCHA/failure returns a screenshot-upload fallback rather than crawling.

### 7. Local-first internet search

1. Enter a query that has both personal and internet results.
2. Confirm personal results render first.
3. Confirm the browser does not send the query to the internet provider until the user chooses
   “continue internet search”.
4. Confirm only the query is transmitted, not knowledge bodies, notes, categories, attachments,
   or authorizations.
5. Confirm external candidates are visually separate and remain transient.
6. Confirm saving a candidate creates a searchable item with its source preserved.
7. Confirm refreshing before confirmation does not create a knowledge item.

### 8. Recycle bin and purge

1. Delete an active item.
2. Confirm it disappears from search immediately.
3. Confirm it appears in `/trash` with a visible purge date.
4. Restore it and confirm it becomes searchable again.
5. Delete again, run the purge job with a short test retention period, and confirm the item,
   source, attachments, analysis, and index document are permanently removed.
6. Confirm an old backup cannot restore an item after the disclosed backup expiry.

### 9. Bilingual behavior

1. Confirm default Chinese on first visit.
2. Switch to English and visit every primary route.
3. Confirm navigation, errors, security notices, destructive confirmations, and processing
   states are complete in English.
4. Confirm user-entered content is unchanged by the language switch.
5. Confirm AI output language follows the setting or explicit source-language choice.

### 10. Taste visual verification

Use the `design-taste-frontend` skill during implementation and inspect the real app at:

- Desktop: 1440 px
- Tablet: 768 px
- Mobile: 390 px

Required checks:

- Search is the dominant home-page action and the only text search input.
- Local results are visually before the internet-search action.
- Loading, empty, failed, pending, provisional, locked, redacted, and recycled states remain
  readable without layout shift.
- Text does not overflow controls at either language.
- Keyboard focus, labels, dialogs, and destructive confirmations are accessible.

## Automated Commands

These commands become the release gate after the implementation creates the workspace and
scripts:

```powershell
pnpm lint
pnpm typecheck
pnpm test
pnpm test:integration
pnpm test:contract
pnpm test:e2e
pnpm test:a11y
pnpm test:security
pnpm test:perf
pnpm build
```

## Performance Validation

1. Load the frozen 100,000-item benchmark corpus.
2. Confirm the dataset manifest and hash match the committed benchmark definition.
3. Run 500 performance queries and the separate 100 correctness queries.
4. Use two browser sessions and two background jobs as formal concurrency.
5. Measure from search submission until 20 result cards are visible.
6. Confirm 95% of formal queries complete within 2 seconds in both cold-query and warm-cache
   conditions.
7. Run four concurrent searches for 60 seconds as a diagnostic pressure test; no 5xx responses
   are allowed.

## Backup and Recovery Validation

1. Back up PostgreSQL, object storage, analysis metadata, authorizations, and deletion
   tombstones.
2. Rebuild Meilisearch from PostgreSQL.
3. Restore into an isolated Compose project.
4. Verify item, attachment, classification, language, and source consistency.
5. Verify a recycled item can be restored inside its window and cannot reappear after the
   disclosed backup expiry.
6. Confirm logs and metrics contain no user content, raw query, filename, token, or provider
   response.

## Related Design Artifacts

- [spec.md](spec.md)
- [plan.md](plan.md)
- [research.md](research.md)
- [data-model.md](data-model.md)
- [deployment.md](deployment.md)
- [contracts/openapi.yaml](contracts/openapi.yaml)
