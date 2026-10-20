# Implementation Plan: AI 个人知识库

**Branch**: `001-ai-personal-knowledge-base` | **Date**: 2026-10-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-ai-personal-knowledge-base/spec.md`

## Summary

构建一个单用户、私有部署的 AI 个人知识库网站。用户可以手动录入，或通过文字、文档、
截图和单页网页截图采集知识；系统自动生成摘要、分类和标签，用户纠正后锁定。
首页只提供一个搜索框，先检索个人知识，再由用户明确触发互联网搜索。

技术路线采用 TypeScript 模块化单体：Next.js Web/API、独立后台 worker、强隔离的
Playwright 单页浏览器进程。PostgreSQL 是唯一权威数据源，Meilisearch 是可重建派生索引，
pg-boss 使用 PostgreSQL 执行异步任务，S3 兼容对象存储保存附件。AI、互联网搜索和单页读取
通过 DeepSeek 适配器接入；Web 请求进程不执行 OCR、截图或长时间 AI 任务。

## Technical Context

**Language/Version**: Node.js 24 LTS；TypeScript 使用与 Next.js 构建链兼容的最新稳定 5.x

**Primary Dependencies**: Next.js 16 App Router、React 19、Better Auth、Drizzle ORM、
pg-boss、Meilisearch、S3 SDK、Playwright、Sharp、Tesseract OCR、Secretlint、
next-intl、Zod、Pino、OpenTelemetry、Radix UI primitives、Lucide icons

**Storage**: PostgreSQL 18 为权威存储；Meilisearch 为派生全文索引；S3 兼容私有对象存储
保存附件和脱敏派生图；不引入 Redis

**Testing**: Vitest、fast-check、Testcontainers、Playwright、axe-core、Schemathesis、
k6、Semgrep、Gitleaks、Trivy

**Target Platform**: Ubuntu 24.04 x86_64、原生 Nginx 与 systemd；新知识库可在容量允许
时使用 Docker Compose，也可使用独立主机；现代桌面与移动浏览器

**Project Type**: Web application with web, background worker, and isolated browser worker

**Performance Goals**: 100,000 条知识基准下，95% 查询在 2 秒内显示至少 20 条个人结果；
目标并发为 2 个用户会话/标签页加 2 个后台任务

**Constraints**: 单用户；默认中文并支持英文；禁止爬虫、链接遍历和批量抓取；网页只处理
用户明确提交的单页；所有密钥仅来自环境变量；无法完整完成密钥扫描的附件整次拒绝；
AI 外发按服务和内容类型授权；实际前端必须通过 Taste 视觉检查；必须与现有
`https://seaunit.site/app` 应用共存，不修改其 `/api`、`/bank`、`/pay`、`/apk` 和认证边界

**Scale/Scope**: 1 个所有者账户、最多 3 至 5 个设备/会话、100,000 条知识、
约 10 个核心页面、文件上限在实施时冻结

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Pre-Research | Post-Design | Evidence |
|------|--------------|-------------|----------|
| SDD and approved clarifications | PASS | PASS | Plan derives from the 62-requirement specification and five accepted clarifications |
| Product/development/test separation | PASS | PASS | Product review completed; implementation and independent test gates are defined |
| Chinese default and complete English coverage | PASS | PASS | `next-intl` with `zh-CN` default; bilingual E2E and string validation required |
| Accuracy and traceability | PASS | PASS | Source, capture time, provider, analysis revision, and safe audit metadata are modeled |
| Taste visual verification | PASS | PASS | UI implementation must run `design-taste-frontend`; 1440/768/390 checks are release gates |
| No crawling and external-data safety | PASS | PASS | Single-page browser sandbox, SSRF egress proxy, redirect limits, no link discovery |
| Environment-only secrets and non-disclosure | PASS | PASS | Zod startup validation, secret scanner/redactor, canary tests, no secret columns or logs |
| Search scale and measurable acceptance | PASS | PASS | Meilisearch with PostgreSQL fallback and a frozen 100,000-item benchmark |

No constitutional violations are required for this design.

## Architecture Decisions

1. **Modular monolith, separate processes.** `web`, `worker`, and `browser-worker` share the
   same TypeScript domain code and schema. This keeps deployment operationally small while
   isolating untrusted browser rendering.
2. **PostgreSQL is authoritative.** Meilisearch and derived files can be rebuilt. Search
   hydration always checks current PostgreSQL lifecycle state so recycled items cannot leak
   through a stale index.
3. **Immediate searchability.** Every accepted manual or AI import creates an active item in
   the same transaction as its base search fields. Meilisearch is updated best-effort; a
   PostgreSQL fallback covers pending or temporarily unavailable indexing.
4. **No Redis or external queue.** pg-boss stores durable tasks in PostgreSQL and supports
   locking, retries, dead letters, and periodic jobs.
5. **One-page browser sandbox.** A user-submitted URL is opened once in an isolated Playwright
   process. It may load required subresources, but it must not discover or follow links,
   enumerate routes, scroll infinitely, or batch process URLs. All redirects and DNS results
   are revalidated through an egress proxy.
6. **DeepSeek behind ports.** Classification, Search, and Fetch are separate provider
   adapters with distinct authorization scopes and timeout/retry behavior. Fetch is optional
   until its single-URL behavior and data policy pass a provider PoC.
7. **Secret safety is fail-closed.** Text, extracted document text, OCR output, and screenshot
   pixels are scanned before persistence or external transmission. Unscannable content is
   rejected; high-confidence secrets are removed from text or redacted in derived images.
8. **Single owner with closed registration.** Better Auth uses database sessions. A one-time
   bootstrap token creates the sole owner; subsequent public registration is disabled.

## Project Structure

### Documentation (this feature)

```text
specs/001-ai-personal-knowledge-base/
├── plan.md
├── research.md
├── data-model.md
├── deployment.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
└── tasks.md               # Created later by $speckit-tasks
```

### Source Code (repository root)

```text
apps/
├── web/
│   ├── src/app/
│   ├── src/components/
│   ├── src/messages/
│   └── src/server/
├── worker/
│   └── src/jobs/
└── browser-worker/
    └── src/

packages/
├── config/
│   └── src/
├── contracts/
│   └── src/
├── db/
│   ├── migrations/
│   └── src/schema/
├── domain/
│   └── src/
├── providers/
│   └── src/
└── ui/
    └── src/

tests/
├── contract/
├── e2e/
├── integration/
├── performance/
├── security/
└── unit/

infra/
├── compose.yml
├── nginx/
├── proxy/
├── systemd/
└── backup/
```

**Structure Decision**: Use a pnpm workspace. The web app owns UI and short-lived HTTP
requests; workers own extraction, classification, indexing, cleanup, and browser rendering.
Shared packages prevent duplicated domain rules while the browser worker remains isolated.

## Data and API Design

- The complete relational model, constraints, indexes, deletion behavior, and state machines
  are defined in [data-model.md](data-model.md).
- External HTTP behavior is defined in [contracts/openapi.yaml](contracts/openapi.yaml).
- Internet search candidates use a signed, user-scoped, short-lived server cache for 15
  minutes and never create a knowledge item until the owner confirms saving.
- Category assignment is at most one current category per item. Tag assignment is many-to-many.
  User edits create `locked` item-level assignments; shared vocabulary rows never carry a
  per-item lock.
- Search query fields are title, body, note, category names, and tag names. There is no second
  search input or user-selected search mode.

## Deployment Coexistence

- The existing “拾题” application is a native systemd deployment, not Docker:
  Spring Boot on `127.0.0.1:8080`, payment sidecar on `127.0.0.1:8090`, MySQL, Redis, and
  Nginx on the same host.
- Keep the existing application and its `/app`, `/api`, `/bank`, `/pay`, `/apk`, and auth
  routes unchanged.
- Publish the knowledge base at `/kb` with `basePath=/kb`; bind its web process to
  `127.0.0.1:3000` and proxy it through the existing Nginx.
- Replace only the exact root landing route with a portal containing entries to `/app` and
  `/kb`; preserve public bank and sitemap routes.
- The inspected host has about 1.8 GiB RAM available. The full knowledge-base stack is not
  recommended on that host without additional memory/swap and resource validation.
- The two Nginx source templates under `/opt/shuati/deploy/nginx/` must be updated together
  with the active server config.

The complete deployment baseline, routing design, resource limits, rollback, and secret rules
are documented in [deployment.md](deployment.md).

## Implementation Sequence

1. **Provider and search PoCs:** verify DeepSeek Search/Fetch constraints, Meilisearch Chinese
   relevance at 100,000 items, screenshot success rate, and OCR/redaction completeness.
2. **Foundation:** pnpm workspace, Docker Compose, environment schema, PostgreSQL migrations,
   Better Auth bootstrap, i18n shell, structured logging.
3. **Core capture:** manual CRUD, secret scanning/redaction, object staging, import jobs,
   processing states, recycle bin, optimistic concurrency.
4. **AI classification:** provider adapters, authorization matrix, structured output schema,
   provisional/applied/locked assignments, retry and idempotency.
5. **Search:** Meilisearch indexing, PostgreSQL fallback, stale-index hydration guard,
   single-box query, external-search handoff, candidate confirmation transaction.
6. **Browser worker:** Playwright single-page capture, egress proxy, private-network rejection,
   screenshot-only fallback, capture metadata.
7. **UI:** implement with the Taste skill, then complete real-browser checks at 1440, 768,
   and 390 pixels.
8. **Hardening:** contract/security/performance/accessibility/bilingual suites, backup and
   restore rehearsal, deletion expiry verification.

## Validation Strategy

- Unit: state machines, lock precedence, idempotency, secret patterns, version conflicts,
  retention calculations, and provider response validation.
- Integration: PostgreSQL, pg-boss, Meilisearch, and object storage using Testcontainers.
- Contract: OpenAPI and provider adapter schemas, errors, timeout, 429, and malformed output.
- E2E: manual save, all import modes, candidate confirmation, recycle bin, language switch,
  authorization, and immediate searchability.
- Security: canary secrets across DOM, network, DB, index, logs, screenshots, and artifacts;
  SSRF/private-IP/credential URL tests; authentication and access-control tests.
- Performance: frozen 100,000-item corpus and 500-query benchmark using the parameters in
  [research.md](research.md), measured through visible Playwright result rendering.
- Visual: the Taste inspector and screenshot review at 1440, 768, and 390 pixels.

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Meilisearch Chinese relevance misses acceptance | Run the fixed benchmark before implementing the full UI; Typesense and PostgreSQL fallback are evaluated alternatives |
| DeepSeek Fetch cannot guarantee single-URL behavior | Keep Fetch behind an adapter and feature flag; disable it if the provider PoC fails; use Search plus user-uploaded screenshots |
| Image secret redaction is unreliable | Redact only when OCR/sensitive-region confidence is sufficient; otherwise reject and delete temporary artifacts |
| Index and database divergence | PostgreSQL lifecycle guard during hydration, revisioned idempotent index jobs, and full rebuild by alias switch |
| Browser capture causes SSRF or accidental crawling | Isolated browser egress proxy, DNS/IP validation at connection time, redirect cap, no link discovery, no persistent session |
| AI output prompt injection | Treat all imported content as data, disable tools, use strict JSON schema, validate output, and never execute model-suggested actions |
| Retention window is misunderstood | Set configurable defaults, display the exact purge and backup exposure dates, and test expiry with a fake clock and a short-retention sandbox |

## Complexity Tracking

No constitutional violations are required. The separate browser worker and derived search
index are justified by the no-crawler security boundary and the 100,000-item search target;
they will be removed only if their provider and search PoCs fail to show value.
