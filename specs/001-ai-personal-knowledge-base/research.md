# Phase 0 Research: AI 个人知识库

**Feature**: `001-ai-personal-knowledge-base`

**Date**: 2026-10-21

**Status**: Complete; no unresolved clarification markers

## Decision 1: Modular monolith with isolated runtime roles

**Decision**: Use one TypeScript codebase with three runtime roles:

- `web`: Next.js App Router UI and short-lived HTTP handlers.
- `worker`: extraction, AI classification, indexing, cleanup, and periodic jobs.
- `browser-worker`: one-page Playwright rendering with no direct public network access.

**Rationale**: The product serves one owner and does not justify microservices. A modular
monolith keeps transactions and domain rules shared, while process isolation prevents
untrusted page rendering, OCR, and AI calls from occupying the web request process.

**Alternatives considered**:

- Next.js-only process with in-request work: rejected because long extraction, OCR, and AI
  tasks would be fragile and difficult to retry.
- Microservices or Kubernetes: rejected because the operational burden is disproportionate
  for one user.

## Decision 2: PostgreSQL is authoritative; Meilisearch is derived

**Decision**: Store all authoritative knowledge, sources, attachments, jobs, AI analyses,
authorizations, vocabulary, assignments, sessions, and lifecycle metadata in PostgreSQL.
Meilisearch stores a searchable projection only.

**Rationale**: PostgreSQL provides transactions, constraints, optimistic concurrency, durable
task state, and a reliable recovery source. Meilisearch provides fast typo-tolerant,
highlightable search for Chinese, English, and mixed queries without adding Redis.

**Alternatives considered**:

- PostgreSQL full-text search only: viable fallback, but Chinese segmentation, typo tolerance,
  ranking controls, and result highlighting need extra extensions and validation.
- Typesense: retained as the primary alternative if Meilisearch fails the Chinese benchmark.
- OpenSearch: rejected for V1 because it is operationally heavy for a single-owner product.

## Decision 3: Guarantee immediate searchability with a fallback path

**Decision**: On acceptance, create the item as `active` and write its base searchable fields
in the same PostgreSQL transaction. Submit a revisioned Meilisearch upsert best-effort. If
Meilisearch is delayed or unavailable, the search API uses a bounded PostgreSQL fallback and
hydrates all hits from PostgreSQL before returning them.

**Rationale**: Search availability must not depend on AI completion, authorization, or a
background index. The derived index can lag without making a newly accepted item invisible.

**Consistency rules**:

- A stale Meilisearch hit is removed during PostgreSQL hydration if the item is not `active`.
- Delete and recycle operations set the database lifecycle first and remove the Meili document
  immediately or through an idempotent job.
- Search fallback is intentionally limited and reports a degraded-search state. It is not the
  normal 100,000-item path.

**Alternatives considered**:

- Returning before indexing without fallback: rejected because it violates immediate
  searchability.
- Blocking all saves until Meilisearch acknowledges: rejected because search downtime would
  prevent saving.

## Decision 4: Meilisearch index design and benchmark gate

**Decision**: Use one index alias per schema version. Indexed fields and priorities:

1. `title`
2. `category_names`
3. `tag_names`
4. `summary`
5. `note`
6. `body`

Also index stable identifiers, source type, lifecycle marker, language hints, and an index
revision. Attachments, raw secrets, and authorization records are never indexed.

**Rationale**: Field weighting matches user expectations while preserving the “one search
box” rule. Search always queries all supported fields together.

**Release gate**:

- 100,000 synthetic items.
- 500 benchmark queries with expected results.
- 95% of queries render at least 20 personal results within 2 seconds.
- Chinese, English, mixed-language, short-query, tag, title, body, note, and zero-result cases.
- The implementation must prove this with a real browser, not only an API microbenchmark.

**Alternatives considered**:

- Index only title and tags: rejected because the specification requires content retrieval.
- Semantic embedding search in V1: rejected as out of scope until keyword benchmark and data
  model are stable.

## Decision 5: Durable jobs in PostgreSQL

**Decision**: Use pg-boss for classification, extraction, index synchronization, browser
capture, retry, recycle purge, reconciliation, and backup checks.

**Rationale**: pg-boss uses PostgreSQL locking and removes Redis from the operational surface.
It supports durable retries, scheduled work, and dead-letter handling.

**Retry policy defaults**:

- AI classification: up to 5 attempts with exponential backoff and jitter.
- Search/Fetch and browser capture: up to 2 retries for timeout, 429, and 5xx only.
- Permanent security or policy failures: no retry.
- One idempotency key per `(item revision, job type)`; a late result for an old revision is
  discarded.

**Alternatives considered**:

- Redis/BullMQ: rejected because it adds a stateful service without solving a V1 problem.
- Kafka, Temporal: rejected as excessive for one owner.

## Decision 6: Single-owner authentication and closed registration

**Decision**: Use Better Auth with database sessions and a one-time bootstrap token. After the
owner account exists, public registration is disabled.

**Rationale**: The product is private by default and should not expose a second account path.
Database sessions allow explicit revocation and fit the server-side authority model.

**Rules**:

- Session tokens are stored only as hashes.
- All protected routes verify `owner_id`.
- Password reset and session lifetime are configuration values, not user-authored secrets.
- No keys or auth secrets are ever returned to the browser.

## Decision 7: DeepSeek through narrow provider adapters

**Decision**: Define separate adapters for:

- AI summarization/classification.
- DeepSeek Search.
- DeepSeek Fetch.
- Optional vision analysis where required by the selected model.

Each adapter has its own service code, timeout, retry policy, input schema, output schema, and
authorization scope.

**Rationale**: Search, Fetch, and classification have different privacy and failure semantics.
A single broad “AI service” permission would violate the accepted per-service authorization
decision.

**Provider gate**:

- Validate model IDs, structured output support, data handling terms, rate limits, and error
  formats in Phase 0 of implementation.
- Fetch may be disabled if it cannot be limited to one explicit public URL.
- A disabled provider must not disable manual entry, local search, recycle bin, or settings.

## Decision 8: Single-page browser capture is not crawling

**Decision**: A user explicitly submits one URL. A dedicated browser worker may render that
one page and capture a bounded screenshot. It may load subresources required for rendering,
but it must not:

- discover or follow links;
- enumerate URLs or sitemaps;
- prefetch pages;
- perform automatic infinite scrolling;
- create multi-page capture jobs;
- retain cookies, login state, downloads, popups, WebRTC, Service Workers, or persistent
  browser profiles.

**Capture limits**:

- Initial page only.
- At most three redirects, each revalidated.
- Only HTTP/HTTPS on ports 80 and 443.
- Reject loopback, private, link-local, CGNAT, IPv6 ULA, cloud metadata, and reserved address
  ranges.
- Validate DNS at connection time in the egress proxy to prevent rebinding.
- On login, CAPTCHA, policy denial, or capture failure, ask the user to upload a screenshot.

**Rationale**: The user explicitly asked for AI screenshot analysis, not web crawling. The
constitution prohibits crawling and unauthorised automatic traversal.

## Decision 9: Secret scanning and redaction are fail-closed

**Decision**: Run scanning before persistence and before any external request.

**Text and extracted documents**:

- Apply high-confidence provider patterns, credential formats, and entropy checks.
- Remove matched values from persisted text and replace them with a safe marker.
- Preserve the rest of the content and show a generic redaction notice.

**Screenshots and images**:

- OCR must produce text and bounding boxes.
- Detect secrets and redact corresponding pixels into a derived image.
- If OCR, region mapping, or redaction completeness is uncertain, reject the entire
  submission and remove temporary artifacts.

**Documents and PDFs**:

- Extract every supported content surface before persistence.
- If any required surface cannot be scanned, reject rather than storing an unscanned original.

**Alternatives considered**:

- Scan after storage: rejected because it creates a secret exposure window.
- Block only external transmission: rejected by the constitution and spec.

## Decision 10: Data lifecycle and retention defaults

**Decision**:

| Data | Default |
|------|---------|
| Active knowledge | Kept until user deletes |
| Recycle bin | 30 days, then permanent purge |
| Daily backups | 30 days |
| WAL/PITR | 7 days |
| Internet candidate TTL | 15 minutes |
| Audit events | 365 days, metadata only |
| AI authorization history | Account lifetime |

All values are configuration-backed and disclosed in the UI. The record deletion flow removes
object storage data before the final relational purge and never restores expired knowledge
from an old backup.

## Decision 11: UI and localization

**Decision**: Build the application shell as a quiet, dense working interface, not a marketing
page. Use one search input on the home page only; no search inputs in library, processing, or
trash views.

**Routes**:

- `/`
- `/library`
- `/library/[id]`
- `/capture`
- `/processing`
- `/trash`
- `/settings`
- `/settings/authorizations`
- `/setup` and `/login`

**Localization**:

- `next-intl` with `zh-CN` default and `en`.
- User-entered content is never auto-translated.
- AI summary/classification output follows the interface language unless the user selects the
  source language.
- UI implementation must use the Taste skill and real-browser checks at 1440, 768, and 390 px.

## Decision 12: Test and operations baseline

**Decision**:

- Vitest and fast-check for unit/property tests.
- Testcontainers for PostgreSQL, pg-boss, Meilisearch, and object storage.
- Playwright and axe-core for E2E/accessibility.
- Schemathesis for OpenAPI contracts.
- k6 for diagnostic load; Playwright is the final visible-result timing authority.
- Semgrep, Gitleaks, Trivy, and OWASP ZAP for security gates.
- Pino JSON logs and OpenTelemetry metrics/traces.
- pgBackRest for PostgreSQL/WAL and restic for object storage; run restore drills.

**Logging rule**: Do not log user content, filenames, query strings, cookies, provider request
bodies, provider responses, or secret-bearing errors. Use request/trace IDs and safe error
codes.

## Decision 13: Release benchmark contract

**Corpus**:

- 100,000 synthetic items with fixed seed and manifest hash.
- 70% Chinese, 20% English, 10% mixed.
- About 100 categories and 5,000 tags with Zipf-like distribution.
- Body median 2 KiB, P95 20 KiB.

**Query set**:

- 500 performance queries with verified expected results.
- Field mix: title 20%, body 30%, note 15%, category 10%, tag 15%, mixed 10%.
- Result-count mix: 20–49 (35%), 50–199 (45%), 200+ (20%).
- Separate 100 correctness queries for zero results, single results, misspellings, and synonyms.

**Reference environment**:

- Linux x86_64, 8 vCPU, 32 GiB RAM, NVMe.
- App containers limited to 4 vCPU/8 GiB.
- Fixed image digests and browser version.

**Measurement**:

- From search submission until at least 20 result cards are visible and loading ends.
- Includes HTTP, search, ranking, serialization, and browser rendering.
- C2 formal concurrency: 2 browser sessions plus 2 background jobs.
- C3 diagnostic load: 4 concurrent searches for 60 seconds.
- Cold-query and warm-cache runs both must meet 95% within 2 seconds; cold process start is
  reported separately.

## Decision 14: Coexist with the existing non-Docker production application

**Decision**: Treat the existing `seaunit.site` application as an immutable native systemd
deployment during this feature. Publish the knowledge base at `/kb`, proxy it through the
existing Nginx, and replace only the exact root landing route with a two-application portal.

**Observed baseline**:

- No Docker installation on the production host.
- Spring Boot and payment sidecar run as systemd units.
- MySQL, Redis, Nginx, and Cloudflare tooling run on the same host.
- The host has 2 vCPU, 3.4 GiB RAM, and about 1.8 GiB currently available.

**Rationale**: The existing application already owns `/api`, `/app`, `/bank`, `/pay`, and
authentication cookies. Path-prefixed routing is the lowest-risk integration. The current
memory headroom is insufficient to assume the complete new stack can run alongside the
existing application without limits and load testing.

**Deployment consequences**:

- The web process binds to `127.0.0.1:3000`.
- Next.js uses `basePath=/kb` and a cookie name/path separate from the existing app.
- Nginx source templates and the active configuration must change together.
- The portal is a static exact-`/` route; `/app` and existing business routes remain intact.
- Full-stack deployment requires a memory upgrade, a separate host, or a measured constrained
  profile with swap and concurrency limits.

## Resolved Unknowns

- Application stack: TypeScript and Node.js 24 LTS.
- Authoritative data store: PostgreSQL.
- Search: Meilisearch with a PostgreSQL fallback; exact Chinese relevance is a release gate.
- Task queue: pg-boss.
- Attachment storage: S3-compatible private object storage.
- Authentication: Better Auth, one owner, closed registration.
- Browser capture: isolated Playwright worker, single user-requested page only.
- AI/search integration: separate DeepSeek adapters and authorization scopes.
- Recycle-bin/backup defaults: 30-day recycle bin, 30-day daily backup, 7-day WAL.
- Localization: Chinese default, complete English coverage.
- Performance target: 100,000 items, 95% of 500 queries, 2 seconds, 20 visible results.
- Production coexistence: native Nginx/systemd host with `/kb` path and a two-entry root portal.
