# Tasks: AI 个人知识库

**Input**: Design documents from `specs/001-ai-personal-knowledge-base/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/openapi.yaml](contracts/openapi.yaml),
[quickstart.md](quickstart.md)

**Tests**: Included because the project constitution requires independently verified acceptance
criteria and release gates.

**Organization**: Tasks are grouped by user story so each priority can be implemented and
validated independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with other marked tasks once its prerequisites are complete.
- **[Story]**: Maps a task to `US1`, `US2`, `US3`, `US4`, or `US5`.
- Every task includes at least one exact repository-relative file path.

## Phase 0: Feasibility and Governance Gates (self-contained)

**Purpose**: Resolve the technical risks that would otherwise invalidate later implementation
choices. PoC scripts use Node 24 built-ins and throwaway containers, so they do not depend on the
project workspace created later.

- [ ] T001 Prepare the independent root-portal governance amendment with rationale, impact, migration, version/date, and explicit product approval in `specs/001-ai-personal-knowledge-base/root-portal-exception.md`, then apply the approved amendment to `.specify/memory/constitution.md`, `plan.md`, and `deployment.md`; T124 and T127 are blocked until this task is approved
- [ ] T002 [P] Add DeepSeek Search/Fetch contract fixtures for timeout, 429, malformed response, single-URL behavior, and disabled-provider fallback in `tests/poc/deepseek-contract.test.ts`
- [ ] T003 [P] Create the fixed-seed 100,000-item corpus, 500-query performance set, 100-query correctness set, and manifest hash for Meilisearch Chinese/English/mixed relevance in `scripts/poc/meili-benchmark.ts` and `tests/poc/meili-benchmark.test.ts`
- [ ] T004 [P] Validate single-page screenshot success, OCR completeness, and redaction failure behavior in `tests/poc/page-capture.test.ts`
- [ ] T005 [P] Validate high-confidence secret detection, redaction, and unscannable fail-closed behavior in `tests/poc/secret-safety.test.ts`
- [ ] T006 Record provider/search PoC results, chosen fallbacks, and unresolved operational risks in `specs/001-ai-personal-knowledge-base/research.md` and `specs/001-ai-personal-knowledge-base/plan.md`

**Checkpoint**: No production implementation proceeds with an unverified search, screenshot, OCR,
or provider path.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize the pnpm workspace, runtime shells, local infrastructure, and delivery
baseline.

- [ ] T007 Create the pnpm workspace with Node engine and package manager pins, app manifests, tsconfigs, root scripts, and common metadata in `package.json`, `pnpm-workspace.yaml`, `turbo.json`, `apps/web/package.json`, `apps/worker/package.json`, and `apps/web/tsconfig.json`
- [ ] T008 [P] Create shared TypeScript base configuration in `tsconfig.base.json`, `packages/config/tsconfig.json`, and `packages/domain/tsconfig.json`
- [ ] T009 [P] Scaffold the Next.js web app with `basePath=/kb` in `apps/web/next.config.ts`, `apps/web/src/app/layout.tsx`, and `apps/web/src/app/page.tsx`
- [ ] T010 [P] Scaffold the background and single-page browser runtimes in `apps/worker/src/index.ts`, `apps/browser-worker/src/index.ts`, and `apps/browser-worker/package.json`
- [ ] T011 [P] Create shared package boundaries for config, database, domain, providers, contracts, and UI under `packages/*/package.json`
- [ ] T012 [P] Configure lint, formatting, unit, contract, E2E, accessibility, performance, and security runners in `eslint.config.mjs`, `.prettierrc.json`, `vitest.config.ts`, `playwright.config.ts`, `schemathesis.toml`, and `k6/search.js`
- [ ] T013 [P] Create the local PostgreSQL, Meilisearch, MinIO, OCR, and browser-worker stack with bucket initialization and health checks in `infra/compose.yml`, `infra/compose.env.example`, and `infra/minio/init.sh`
- [ ] T014 [P] Create baseline GitHub Actions quality gates in `.github/workflows/ci.yml`
- [ ] T015 [P] Create knowledge-base Nginx, systemd, and egress-proxy templates in `infra/nginx/zhishiku.conf`, `infra/systemd/zhishiku-web.service`, `infra/systemd/zhishiku-worker.service`, `infra/systemd/zhishiku-browser-worker.service`, and `infra/proxy/egress-proxy.ts`
- [ ] T016 [P] Create the complete non-secret environment template and startup documentation in `.env.example` and `README.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build shared persistence, security, queue, search, observability, and authentication
infrastructure required by every user story.

**CRITICAL**: No user story implementation starts until this phase passes its integration tests.

- [ ] T017 Implement Zod environment validation that reports variable names only in `packages/config/src/env.ts` and `packages/config/src/env.test.ts`
- [ ] T018 Configure Drizzle, PostgreSQL pooling, and migration execution in `packages/db/src/client.ts`, `packages/db/src/migrate.ts`, and `packages/db/drizzle.config.ts`
- [ ] T019 Implement Better Auth single-owner bootstrap, database sessions, closed registration, and `instance_key=1` uniqueness in `packages/db/src/schema/auth.ts`, `apps/web/src/server/auth/auth.ts`, and `packages/domain/src/auth/owner.ts`
- [ ] T020 Create `knowledge_item`, `source`, and `attachment` schema with required `title` length `1–300`, unique `UNIQUE(item_id)` source, `UNIQUE(storage_key)` attachment, lifecycle/state enums, timestamps, and `version` in `packages/db/src/schema/knowledge.ts`
- [ ] T021 Create `capture_job`, `processing_job`, and `ai_analysis` schema with non-negative attempt counts, safe errors, nullable completion timestamps, and `UNIQUE(owner_id, idempotency_key)` in `packages/db/src/schema/jobs.ts`
- [ ] T022 Create `category`, `tag`, `category_assignment`, `tag_assignment`, and `tag_merge_suggestion` schema with `UNIQUE(item_id) WHERE superseded_at IS NULL`, `UNIQUE(item_id, tag_id) WHERE superseded_at IS NULL`, and locked/provisional states in `packages/db/src/schema/classification.ts`
- [ ] T023 Create `ai_processing_authorization`, `external_search_candidate`, `request_idempotency`, and `audit_event` schema with active-scope uniqueness, candidate expiry, idempotency uniqueness, and metadata-only audit fields in `packages/db/src/schema/governance.ts`
- [ ] T024 Generate the migration from all Drizzle schemas and add the indexes from `data-model.md`, including current assignments, job leasing, purge scheduling, attachment lookup, and audit lookup in `packages/db/migrations/0001_foundation.sql`
- [ ] T025 Implement shared domain errors, result types, and lifecycle/state transition guards in `packages/domain/src/shared/errors.ts` and `packages/domain/src/shared/state-machines.ts`
- [ ] T026 Configure pg-boss queues for extraction, classification, indexing, browser capture, purge, and reconciliation with retry/backoff and worker registration in `packages/providers/src/queue/boss.ts` and `apps/worker/src/queues.ts`
- [ ] T027 Implement the private S3 object-store adapter with staging, promotion, deletion, and signed access in `packages/providers/src/storage/s3.ts`
- [ ] T028 Implement high-confidence secret detection, safe redaction notices, and text redaction in `packages/domain/src/security/secret-scanner.ts` and `packages/domain/src/security/secret-scanner.test.ts`
- [ ] T029 Implement the Meilisearch adapter, index schema, revisioned upsert/delete, and index alias switching in `packages/providers/src/search/meilisearch.ts` and `packages/domain/src/search/index-document.ts`
- [ ] T030 Implement structured Pino logging and OpenTelemetry request/job correlation without content logging in `packages/config/src/logger.ts` and `packages/config/src/telemetry.ts`
- [ ] T031 Implement authenticated API middleware, request fingerprinting, idempotency response replay, JSON validation, CSRF protection, rate limits, and safe error envelopes in `apps/web/src/server/api/middleware.ts` and `apps/web/src/server/api/idempotency.ts`
- [ ] T032 Generate Zod contract types from the OpenAPI document and add drift detection in `packages/contracts/src/generated.ts` and `scripts/check-openapi.ts`
- [ ] T033 Configure Testcontainers helpers for PostgreSQL, pg-boss, Meilisearch, and MinIO in `tests/integration/containers.ts`
- [ ] T034 Create the bilingual Next.js application shell and language preference provider in `apps/web/src/app/layout.tsx`, `apps/web/src/messages/zh-CN.json`, and `apps/web/src/messages/en.json`
- [ ] T035 Create a fail-closed deployment preflight script that checks existing app health, systemd/MySQL/Redis state, port conflicts, disk, memory thresholds, and Nginx routing in `deploy/scripts/preflight-existing-app.sh`
- [ ] T036 Create the local test fixture factory for owners, items, assignments, jobs, and candidates in `tests/fixtures/factories.ts`
- [ ] T037 Implement Better Auth route handlers, one-time setup API, logout/session endpoints, and auth integration tests in `apps/web/src/app/api/auth/[...all]/route.ts`, `apps/web/src/app/api/setup/route.ts`, and `tests/integration/auth.test.ts`
- [ ] T038 Implement persisted idempotency conflict handling, response replay, and expiry cleanup with tests in `packages/domain/src/shared/idempotency.ts` and `tests/integration/idempotency.test.ts`
- [ ] T039 Verify empty-database migration, forward migration, and schema drift checks in `packages/db/migrations/0001_foundation.sql` and `tests/integration/migrations.test.ts`
- [ ] T040 Implement index producer, index worker, delete/restore indexing, full rebuild, and revision reconciliation in `apps/worker/src/jobs/index-knowledge.ts` and `packages/domain/src/search/index-reconciler.ts`
- [ ] T041 Implement periodic candidate expiry, recycle purge, failed-classification retry, index reconciliation, and staging cleanup jobs in `apps/worker/src/jobs/periodic.ts`
- [ ] T042 Implement the DNS-revalidating egress proxy with destination IP, port, redirect, and DNS-rebinding checks in `infra/proxy/egress-proxy.ts`
- [ ] T043 Add the health endpoint and execute the foundational integration smoke suite for migrations, auth, pg-boss, S3, Meilisearch, and bilingual startup in `apps/web/src/app/health/route.ts` and `tests/integration/foundation-smoke.test.ts`

**Checkpoint**: Database, auth, queue, storage, search, security, i18n, and deployment preflight are
ready.

---

## Phase 3: User Story 1 - 手动收藏并找回知识 (Priority: P1)

**Goal**: A private owner can create, open, edit, soft-delete, and immediately retrieve manual
knowledge through the single home search box.

**Independent Test**: With AI providers disabled, create a manual item and retrieve it by values
from its title, body, note, category, and tags.

### Tests for User Story 1

- [ ] T044 [P] [US1] Add OpenAPI contract tests for knowledge CRUD and personal search in `tests/contract/knowledge.test.ts`
- [ ] T045 [P] [US1] Add integration tests for manual save, immediate searchability, source uniqueness, and optimistic versions in `tests/integration/manual-knowledge.test.ts`
- [ ] T046 [P] [US1] Add unit tests for title constraints `1–300`, lifecycle transitions, and version conflicts in `packages/domain/src/knowledge/knowledge-item.test.ts`

### Implementation for User Story 1

- [ ] T047 [US1] Implement knowledge item and source repositories with transactional create/update and lifecycle filtering in `packages/domain/src/knowledge/repository.ts`
- [ ] T048 [US1] Implement manual create, list, read, update, recycle, and initial classification-job enqueue use cases in `packages/domain/src/knowledge/service.ts`
- [ ] T049 [US1] Implement the PostgreSQL search fallback across title, body, note, category, and tag in `packages/domain/src/search/postgres-fallback.ts`
- [ ] T050 [US1] Implement the knowledge collection, item, and personal search route handlers in `apps/web/src/app/api/v1/knowledge-items/route.ts`, `apps/web/src/app/api/v1/knowledge-items/[itemId]/route.ts`, and `apps/web/src/app/api/v1/search/route.ts`
- [ ] T051 [P] [US1] Build the manual entry form with pre-save secret scanning and the safe redaction/rejection notice in `apps/web/src/components/knowledge/manual-entry-form.tsx` and `apps/web/src/components/security/secret-notice.tsx`
- [ ] T052 [P] [US1] Build and compose the single home search input, result list, match indicators, and internet-search entry point in `apps/web/src/components/search/personal-search.tsx` and `apps/web/src/app/page.tsx`
- [ ] T053 [US1] Build the knowledge library and item detail/edit routes in `apps/web/src/app/library/page.tsx` and `apps/web/src/app/library/[id]/page.tsx`
- [ ] T054 [US1] Apply `design-taste-frontend` with screenshot evidence, manual critique, fixes, and 1440/768/390 checks for P1 screens in `tests/e2e/taste-us1.spec.ts`
- [ ] T055 [US1] Add P1 end-to-end tests for authentication, manual save, source type display, all five searchable fields, pending processing visibility, and exactly one search input in `tests/e2e/manual-knowledge.spec.ts`
- [ ] T056 [US1] Build minimal setup/login routes and the protected private workspace shell over the Better Auth endpoints in `apps/web/src/app/setup/page.tsx`, `apps/web/src/app/login/page.tsx`, and `apps/web/src/components/app-shell.tsx`
- [ ] T057 [US1] Implement minimal soft-delete and recycle-bin restore behavior for the P1 deletion flow in `apps/web/src/app/api/v1/trash/route.ts` and `apps/web/src/app/trash/page.tsx`
- [ ] T058 [US1] Implement and validate the `/kb` route only through a loopback staging proxy without modifying production Nginx in `deploy/nginx/kb-staging.conf` and `tests/integration/nginx-staging.test.ts`

**Checkpoint**: US1 is a standalone MVP and remains fully usable when AI and internet services are
unavailable.

---

## Phase 4: User Story 2 - AI 自动归类并生成标签 (Priority: P2)

**Goal**: Every successful item receives an automatic category and tag set; low-confidence values
are applied provisionally, and user corrections lock item fields.

**Independent Test**: Save unclassified text without manual taxonomy, verify automatic
classification/tagging, then lock an edited value and prove re-analysis cannot overwrite it.

### Tests for User Story 2

- [ ] T059 [P] [US2] Add unit tests for provisional/applied/locked assignment precedence in `packages/domain/src/classification/assignment.test.ts`
- [ ] T060 [P] [US2] Add provider contract tests for valid, malformed, timed-out, and rate-limited AI output in `tests/contract/ai-provider.test.ts`
- [ ] T061 [P] [US2] Add integration tests for classification jobs, retries, stale revisions, and locked field preservation in `tests/integration/classification.test.ts`

### Implementation for User Story 2

- [ ] T062 [US2] Implement scoped AI processing authorization checks by `service_code` and `content_type`, including data-scope/provider/purpose disclosure and revoke behavior in `packages/domain/src/ai/authorization-service.ts`
- [ ] T063 [US2] Implement the DeepSeek classification adapter with strict structured output validation and related-item suggestions in `packages/providers/src/ai/deepseek-classification.ts`
- [ ] T064 [US2] Implement category/tag assignment repositories and item-field lock updates in `packages/domain/src/classification/repository.ts`
- [ ] T065 [US2] Implement the classification worker with provisional fallback, revision checks, and bounded retries in `apps/worker/src/jobs/classify-item.ts`
- [ ] T066 [US2] Implement automatic tag normalization and merge suggestions for unlocked values in `packages/domain/src/classification/tag-normalization.ts`
- [ ] T067 [US2] Implement grant/list/revoke authorization, re-analysis, and classification-correction endpoints in `apps/web/src/app/api/v1/ai-authorizations/route.ts`, `apps/web/src/app/api/v1/ai-authorizations/[authorizationId]/route.ts`, `apps/web/src/app/api/v1/knowledge-items/[id]/classifications/route.ts`, and `apps/web/src/app/api/v1/knowledge-items/[id]/reanalyze/route.ts`
- [ ] T068 [P] [US2] Build the AI authorization matrix with data scope, provider, content type, purpose, revoke, and re-consent behavior in `apps/web/src/components/settings/ai-authorization-matrix.tsx` and `apps/web/src/app/settings/authorizations/page.tsx`
- [ ] T069 [P] [US2] Build provisional, locked, corrected, related-item, source-type, and merge-suggestion classification controls in `apps/web/src/components/knowledge/classification-editor.tsx`
- [ ] T070 [US2] Build pending, running, awaiting-authorization, failed, and provisional item states in `apps/web/src/components/knowledge/processing-status.tsx`
- [ ] T071 [US2] Add US2 end-to-end tests for automatic classification, auth-scope refusal, revoke-without-future-transmission, lock preservation, cross-item lock isolation, and retry in `tests/e2e/classification.spec.ts`
- [ ] T072 [US2] Add prompt-injection tests for classification, related-item output, and malicious imported content in `tests/security/prompt-injection.test.ts`
- [ ] T073 [US2] Apply `design-taste-frontend` with screenshot evidence, manual critique, fixes, and 1440/768/390 checks for authorization and classification screens in `tests/e2e/taste-us2.spec.ts`
- [ ] T074 [US2] Add the 100-item labeled classification quality and automatic category/tag coverage benchmark for SC-003 and SC-010 in `tests/performance/classification-quality.test.ts`

**Checkpoint**: US2 works on top of US1 and never blocks saving or search.

---

## Phase 5: User Story 3 - 通过网页、文档和截图采集知识 (Priority: P3)

**Goal**: Accepted text, document, screenshot, and single-page URL imports create searchable
items immediately and complete extraction safely in the background.

**Independent Test**: Import one document, one screenshot, and one local/public webpage; verify
immediate searchability, source retention, secret scanning, and no link traversal.

### Tests for User Story 3

- [ ] T075 [P] [US3] Add OpenAPI contract tests for text, webpage, and upload import endpoints in `tests/contract/imports.test.ts`
- [ ] T076 [P] [US3] Add integration tests for staging rejection, document extraction, OCR, redacted images, and retry idempotency in `tests/integration/import-safety.test.ts`
- [ ] T077 [P] [US3] Add browser-worker E2E security tests for loopback/private/CGNAT/IPv6 ULA/metadata/reserved IPs, DNS rebinding, credential URLs, 80/443 enforcement, at most three revalidated redirects, sitemap/prefetch/infinite-scroll/multi-page prohibition, single top-level request graph, and no persistent cookies/Service Workers/popups/downloads in `tests/security/page-capture.test.ts`

### Implementation for User Story 3

- [ ] T078 [US3] Implement secure upload staging, MIME detection, size limits, and cleanup in `packages/domain/src/import/staging-service.ts`
- [ ] T079 [P] [US3] Implement supported MIME validation, document extraction, and bounded OCR with language packs and completeness status in `packages/providers/src/extraction/document-ocr.ts` and `packages/config/src/upload.ts`
- [ ] T080 [P] [US3] Implement image secret-region redaction and redacted-derivative persistence in `packages/domain/src/security/image-redactor.ts`
- [ ] T081 [P] [US3] Implement URL validation, connection-time DNS/IP rejection, redirect limits, and egress-proxy integration in `packages/domain/src/import/url-safety.ts` and `infra/proxy/egress-proxy.ts`
- [ ] T082 [US3] Implement the isolated Playwright single-page capture worker and its dedicated pg-boss capture consumer with no link discovery or persistent session in `apps/browser-worker/src/capture-page.ts` and `apps/browser-worker/src/queues.ts`
- [ ] T083 [US3] Implement text, webpage, and upload import creation plus job status, retry, and cancel route handlers in `apps/web/src/app/api/v1/imports/text/route.ts`, `apps/web/src/app/api/v1/imports/webpage/route.ts`, `apps/web/src/app/api/v1/imports/upload/route.ts`, `apps/web/src/app/api/v1/jobs/[jobId]/route.ts`, `apps/web/src/app/api/v1/jobs/[jobId]/retry/route.ts`, and `apps/web/src/app/api/v1/jobs/[jobId]/cancel/route.ts`
- [ ] T084 [US3] Implement extraction, OCR, redaction, processing jobs, and classification-job enqueue for accepted imports in `apps/worker/src/jobs/process-import.ts`
- [ ] T085 [P] [US3] Build text, URL, document, and screenshot import forms in `apps/web/src/components/import/import-panel.tsx` and `apps/web/src/app/capture/page.tsx`
- [ ] T086 [P] [US3] Build import scan, rejection, pending, retry, screenshot-fallback, and processing-queue states in `apps/web/src/components/import/import-status.tsx` and `apps/web/src/app/processing/page.tsx`
- [ ] T087 [US3] Add US3 end-to-end tests for all import modes and immediate searchability in `tests/e2e/imports.spec.ts`
- [ ] T088 [US3] Apply `design-taste-frontend` with screenshot evidence, manual critique, fixes, and 1440/768/390 checks for import and processing screens in `tests/e2e/taste-us3.spec.ts`
- [ ] T089 [US3] Add prompt-injection tests for document text, OCR text, webpage captures, and search summaries in `tests/security/import-injection.test.ts`
- [ ] T090 [US3] Add a text/document/screenshot import benchmark for SC-004, SC-005, and immediate-search criteria in `tests/performance/import-quality.test.ts`

**Checkpoint**: US3 extends capture without weakening US1 or US2.

---

## Phase 6: User Story 4 - 单框本地优先混合检索 (Priority: P4)

**Goal**: The single search box always returns personal results first, and internet search occurs
only after explicit consent; confirmed internet candidates become searchable items.

**Independent Test**: Search a term with local and internet matches, verify personal-first order,
confirm only the query is transmitted, and save one transient candidate into the library.

### Tests for User Story 4

- [ ] T091 [P] [US4] Add OpenAPI contract tests for external search and candidate save in `tests/contract/external-search.test.ts`
- [ ] T092 [P] [US4] Add integration tests for explicit consent, transient candidate expiry, and no item before confirmation in `tests/integration/external-search.test.ts`
- [ ] T093 [P] [US4] Add browser E2E tests for personal-first ordering and external failure isolation in `tests/e2e/hybrid-search.spec.ts`

### Implementation for User Story 4

- [ ] T094 [P] [US4] Implement the DeepSeek Search adapter with timeouts, retries, and provider error mapping in `packages/providers/src/search/deepseek-search.ts`
- [ ] T095 [P] [US4] Implement the 15-minute external candidate cache and expiry purge in `packages/domain/src/search/external-candidate-service.ts`
- [ ] T096 [US4] Implement explicit-consent external search and idempotent candidate-save API handlers in `apps/web/src/app/api/v1/search/external/route.ts` and `apps/web/src/app/api/v1/search/external/[candidateId]/save/route.ts`
- [ ] T097 [US4] Implement candidate confirmation as a PostgreSQL transaction that atomically claims `saved_item_id`, creates item/source, and enqueues classification and best-effort index jobs in `packages/domain/src/search/save-candidate.ts`
- [ ] T098 [P] [US4] Build external-search consent, provider disclosure, result grouping, and save controls in `apps/web/src/components/search/external-search.tsx`
- [ ] T099 [US4] Implement local-results preservation during external loading, timeout, no-result, partial-failure, and retry in `apps/web/src/components/search/search-page.tsx`
- [ ] T100 [US4] Add US4 acceptance automation for local-first ordering and candidate save in `tests/e2e/external-search-flow.spec.ts`
- [ ] T101 [US4] Apply `design-taste-frontend` with screenshot evidence, manual critique, fixes, and 1440/768/390 checks for hybrid search in `tests/e2e/taste-us4.spec.ts`
- [ ] T102 [US4] Add network-level tests proving only the query is sent, personal body/notes/attachments are not sent, and revoked scope produces no future external request in `tests/security/external-transmission.test.ts`
- [ ] T103 [US4] Add the 100-query single-input retrieval benchmark for SC-011 and local-first result ordering in `tests/performance/single-box-search.test.ts`

**Checkpoint**: US4 preserves personal search while adding opt-in internet retrieval.

---

## Phase 7: User Story 5 - 双语且安全的个人使用 (Priority: P5)

**Goal**: The owner can use complete Chinese and English experiences, manage policy states, and
recover deleted items without exposing credentials or changing user content.

**Independent Test**: Switch languages across all primary flows, verify English coverage, inspect
secret canaries, and restore/delete items through the recycle bin.

### Tests for User Story 5

- [ ] T104 [P] [US5] Add ICU/message completeness tests for Chinese and English keys in `tests/unit/i18n-messages.test.ts`
- [ ] T105 [P] [US5] Add unique canary tests across source-controlled files, configuration, documents, tests, logs, screenshots, build artifacts, version-control fixtures, diagnostics, DOM, API, database, Meilisearch, S3, and telemetry in `tests/security/secret-canaries.test.ts`
- [ ] T106 [P] [US5] Add recycle-bin lifecycle and expiry tests with a fake clock in `tests/integration/recycle-bin.test.ts`

### Implementation for User Story 5

- [ ] T107 [US5] Complete Chinese and English messages for every primary route, error, security notice, destructive action, and processing state in `apps/web/src/messages/zh-CN.json` and `apps/web/src/messages/en.json`
- [ ] T108 [US5] Implement persisted language selection, AI output-language preference, and non-translation of user content in `apps/web/src/server/preferences/language.ts`
- [ ] T109 [US5] Complete setup/login/logout/session integration with `/kb`-scoped cookie behavior and session recovery in `apps/web/src/app/login/page.tsx` and `apps/web/src/app/setup/page.tsx`
- [ ] T110 [P] [US5] Build safe secret-redaction and rejection notices without displaying matched values in `apps/web/src/components/security/secret-notice.tsx`
- [ ] T111 [US5] Implement recycle-bin list, restore, permanent purge, and disclosed expiry route handlers in `apps/web/src/app/api/v1/trash/route.ts`, `apps/web/src/app/api/v1/trash/[itemId]/restore/route.ts`, and `apps/web/src/app/api/v1/trash/[itemId]/route.ts`
- [ ] T112 [P] [US5] Build recycle-bin and settings/privacy screens in `apps/web/src/app/trash/page.tsx` and `apps/web/src/app/settings/page.tsx`
- [ ] T113 [US5] Add bilingual and recycle-bin E2E coverage for default Chinese, every route/state, unchanged user content, AI output language, and expiry messaging in `tests/e2e/bilingual-security.spec.ts`
- [ ] T114 [US5] Apply `design-taste-frontend` with screenshot evidence, manual critique, fixes, and 1440/768/390 checks for login, settings, security, and trash screens in `tests/e2e/taste-us5.spec.ts`
- [ ] T115 [US5] Add manual WCAG 2.2 AA checks for focus order, dialogs, labels, status announcements, contrast, reflow, target size, and screen-reader semantics in `tests/accessibility/manual-checklist.md`
- [ ] T116 [US5] Execute facilitated usability studies for SC-001 and SC-009 with 10 participants and record observations, pass counts, and results in `tests/usability/protocol.md` and `artifacts/usability/results.md`
- [ ] T117 [US5] Add full recycle-bin cascade assertions for immediate search removal, source/attachment/analysis/index cleanup, restore, and permanent purge in `tests/integration/recycle-cascade.test.ts`

**Checkpoint**: All five user stories are independently testable and coexist safely.

---

## Phase 8: Polish and Cross-Cutting Concerns

**Purpose**: Release gates, performance, deployment, recovery, and documentation.

- [ ] T118 Run the full OpenAPI contract suite and record failures in `tests/contract/all-endpoints.test.ts`
- [ ] T119 [P] Add and pass WCAG 2.2 AA automated and keyboard checks in `tests/e2e/accessibility.spec.ts`
- [ ] T120 [P] Execute the frozen 100,000-item, 500-query benchmark with cold/warm cache, C2 concurrency, 20 visible results, and a 95%-under-2-seconds report in `tests/performance/run-search-benchmark.ts` and `artifacts/performance/search-report.json`
- [ ] T121 [P] Add Semgrep, Gitleaks, Trivy, OWASP ZAP, and canary leak checks for all prohibited surfaces in `.github/workflows/security.yml` and `tests/security/leak-surfaces.test.ts`
- [ ] T122 Implement the pgBackRest/restic backup script without destructive restore actions in `deploy/scripts/backup.sh`
- [ ] T123 Implement the official-GitHub one-click provisioning entrypoint with fixed-ref selection, separate user/database/bucket/env/cookie/log paths, pre-deploy backup, dependency install, build, migration, startup, health checks, and fail-closed rollback in `deploy/scripts/provision-ubuntu.sh` and `deploy/scripts/lib/deploy-common.sh`
- [ ] T124 Configure the approved exact `/` portal, preserve the former landing at `/shuati`, and add `/kb` routing in `deploy/scripts/configure-nginx.sh`, `deploy/nginx/shuati.conf`, and `deploy/nginx/shuati-prod.conf` with backup and `nginx -t` rollback
- [ ] T125 Verify zero impact by comparing pre-change `/` with post-change `/shuati`, asserting the approved `/` portal exception, and preserving systemd, MySQL, Redis, cookies, data, `/app`, `/api`, `/bank`, `/pay`, `/apk`, and certificate baselines in `deploy/scripts/verify-existing-app.sh` and `tests/e2e/zero-impact.test.ts`
- [ ] T126 Wire GitHub Actions release deployment gates and tag/commit selection in `.github/workflows/deploy.yml`
- [ ] T127 Add a bilingual root two-application portal with 拾题 `/app` and 知识库 `/kb` entries in `deploy/portal/index.html`, `deploy/portal/portal.css`, and `deploy/portal/messages.json`
- [ ] T128 Run the complete [quickstart.md](quickstart.md) scenario in a clean environment and save command/output evidence in `artifacts/quickstart/report.md`
- [ ] T129 Update [README.md](../../README.md), [deployment.md](deployment.md), and `AGENTS.md` with the official GitHub repository URL, default branch, fixed-ref deployment usage, final paths, and verified commands
- [ ] T130 Run backup restore in an isolated environment, rebuild Meilisearch from PostgreSQL, verify recycle-window recovery, and prove expired backups cannot revive data in `tests/integration/restore-drill.test.ts`
- [ ] T131 Add a fail-closed host capacity and deployment-profile decision gate with thresholds and approval evidence in `deploy/scripts/capacity-gate.sh` and `artifacts/deployment/capacity-report.md`
- [ ] T132 Run the official HTTPS GitHub one-click deployment from a fixed tag/commit twice, inject a health-check failure, verify rollback, and scan logs/command history for secrets in `tests/deploy/one-click-deploy.test.sh` and `artifacts/deployment/one-click-report.md`
- [ ] T133 Run final acceptance evidence for SC-001 through SC-017, verify the target tag/commit passed lint, typecheck, tests, build, and security gates, confirm failed gates block deployment, and map each result to its task and artifact in `artifacts/acceptance/success-criteria.md`

---

## Success Criteria Coverage

| Criterion | Primary verification tasks |
|-----------|----------------------------|
| SC-001 | T116 |
| SC-002 | T120 |
| SC-003 | T074 |
| SC-004 | T090 |
| SC-005 | T077, T090 |
| SC-006 | T093, T100 |
| SC-007 | T105, T121 |
| SC-008 | T107, T113 |
| SC-009 | T116 |
| SC-010 | T074 |
| SC-011 | T103 |
| SC-012 | T071, T102 |
| SC-013 | T117, T130 |
| SC-014 | T076, T105 |
| SC-015 | T071, T102 |
| SC-016 | T045, T087 |
| SC-017 | T059, T061, T071 |

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 0 Feasibility (T001–T006)**: Starts immediately; provider and search PoCs gate the
  corresponding implementation decisions.
- **Phase 1 Setup (T007–T016)**: Can start alongside non-dependent PoC work.
- **Phase 2 Foundational (T017–T043)**: Depends on setup and blocks all user stories.
- **US1 (T044–T058)**: Starts after Phase 2 and establishes the MVP.
- **US2 (T059–T074)**: Requires US1 persistence and classification entry points.
- **US3 (T075–T090)**: Requires US1 item creation and US2 classification worker interfaces.
- **US4 (T091–T103)**: Requires US1 search and US2 automatic classification.
- **US5 (T104–T117)**: Cross-cuts the product; its language and lifecycle work completes after
  the story routes stabilize.
- **Polish (T118–T133)**: Depends on all selected stories and release artifacts.

### Critical Path

```text
T001-T006
  -> T007-T016
  -> T017-T043
  -> T044-T058
  -> T059-T074
  -> T075-T090
  -> T091-T103
  -> T104-T117
  -> T118-T133
```

### Story Dependencies

- **US1**: Independent after Phase 2.
- **US2**: Requires US1 item persistence but must preserve US1 behavior.
- **US3**: Requires US1 item creation and US2 classification worker interfaces.
- **US4**: Requires US1 search and US2 automatic classification.
- **US5**: Cross-cuts all stories but can be implemented incrementally after Phase 2.

---

## Parallel Opportunities

### Setup

```text
T008-T016 use different files and can run in parallel after T007.
```

### User Story 1

```text
T044-T046 can be written in parallel.
T051 and T052 can be built in parallel after T050 defines the API behavior.
```

### User Story 2

```text
T059-T061 can run in parallel.
T068 and T069 can run in parallel after T062 and T064 define the contracts.
```

### User Story 3

```text
T075-T077 can run in parallel.
T078-T081 use separate modules and can run in parallel.
T085 and T086 can run in parallel after T083.
```

### User Story 4

```text
T091-T093 can run in parallel.
T094, T095, and T098 can run in parallel after the external candidate contract is stable.
```

### User Story 5

```text
T104-T106 can run in parallel.
T110 and T112 can run in parallel after T107-T109 stabilize route and message keys.
```

### Polish

```text
T119-T121 can run in parallel.
T122, T123, and T130 are sequential deployment and recovery tasks, not parallel edits.
```

---

## Implementation Strategy

### MVP First

1. Complete T001–T043, including explicit approval of the root-portal governance amendment.
2. Complete US1 T044–T058.
3. Stop and validate manual save, immediate search, and the single-box home experience in local/staging only.
4. Keep public production routing unchanged until T107/T113 and the final Polish deployment gates pass.

### Incremental Delivery

1. US1: Manual knowledge MVP.
2. US2: Automatic classification and locked corrections.
3. US3: Document, screenshot, and single-page URL capture.
4. US4: Local-first hybrid search with opt-in internet results.
5. US5: Full bilingual, security, and recycle-bin completion.
6. Polish: Performance, deployment, backup, and final release gates.

### Parallel Team Strategy

- Development agent A: US1 and shared knowledge/search paths.
- Development agent B: US2 classification and AI authorization.
- Development agent C: US3 browser worker and import pipeline.
- Development agent D: US4 external search.
- Development agent E: US5 i18n, security UX, and recycle bin.
- Test agent: independent story acceptance, canary, SSRF, performance, and zero-impact checks.

---

## Notes

- Tests are required by the project constitution and must fail before the corresponding
  implementation tasks.
- [P] tasks must not edit the same file as another active task.
- All five story checkpoints must be independently demonstrated.
- Deployment work must pass the zero-impact checks before exposing `/kb`.
- The root portal change is an explicitly approved product decision; the existing application
  must remain reachable at `/app`, and its former landing must remain available at `/shuati`.
- Commit after each task or coherent task group.
