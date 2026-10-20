# 任务：AI 个人知识库

**输入**：设计文档位于 `specs/001-ai-personal-knowledge-base/`

**前置条件**：[plan.md](plan.md)、[spec.md](spec.md)、[research.md](research.md)、
[data-model.md](data-model.md)、[contracts/openapi.yaml](contracts/openapi.yaml)、
[quickstart.md](quickstart.md)、[002 计费计划](../002-ai-credit-billing/plan.md)、
[002 计费任务](../002-ai-credit-billing/tasks.md)

**测试**：已纳入，因为项目章程要求对验收标准进行独立验证，
并设置发布门禁。

**组织方式**：任务按用户故事分组，使每个故事都能独立实现和验证。
每个任务还带有实施优先级。

## 优先级

- **P0 — MVP 核心（必须优先交付）**：最小可用的安全端到端产品：环境搭建、私有
  身份验证、手动知识采集、密钥扫描、自动生成分类/标签、
  单框本地检索、即时可搜索性、处理进度可见、核心测试以及
  最小 UI。
- **P1 — 核心扩展**：多模态导入、互联网搜索、完整双语覆盖、完整
  回收站流程、安全的生产路由以及 GitHub 一键部署。
- **P2 — 规模与发布加固**：大型知识库基准测试、分类/导入质量
  基准测试、人工可访问性与可用性研究、备份恢复演练以及最终的
  成功标准证据。

P0 是 MVP 范围。完整发布仍需要 P1 和 P2，但在 P0 检查点通过验证前
不得实施。

### P0 MVP 完成定义

- 私有单所有者设置、登录、会话和退出功能可用。
- 手动文本知识可以保存、打开、编辑、软删除并立即被找到。
- 自动生成摘要、分类和标签；低置信度结果作为暂定值且可编辑。
- 用户更正会锁定当前项目的字段，且不影响其他项目。
- 单一搜索输入覆盖标题、正文、备注、分类和标签。
- 密钥扫描/脱敏以及仅在环境中处理凭据的机制通过核心金丝雀测试。
- 中文是默认界面；必要控件在英文下仍可使用。
- 核心 CRUD、搜索、分类、安全和 Taste 检查通过。
- 所有付费 AI 调用都经过 002 的报价、预留和结算门禁；002 P0 未通过时不得开放真实 AI
  消费，只可使用受控测试计费或保持待整理。
- 完成 P0 检查点不要求实现 P1 多模态导入、互联网搜索、完整双语覆盖和生产部署。

## 格式：`[ID] [Priority] [P?] [Story] 描述`

- **[Priority]**：`[P0]`、`[P1]` 或 `[P2]`。
- **[P]**：先决条件完成后，可与其他已标记任务并行运行。
- **[Story]**：将任务映射到 `US1`、`US2`、`US3`、`US4` 或 `US5`。
- 每个任务至少包含一个精确的仓库相对文件路径。

## 阶段 0：可行性与治理门禁（自包含）

**目的**：解决那些否则会使后续实施选择失效的技术风险。PoC 脚本使用 Node 24 内置模块和
一次性容器，因此不依赖稍后创建的项目工作区。

- [ ] T001 [P1] 在 `specs/001-ai-personal-knowledge-base/root-portal-exception.md` 中准备独立的根门户治理修订，包含理由、影响、迁移、版本/日期和明确的产品批准，然后将获批修订应用到 `.specify/memory/constitution.md`、`plan.md` 和 `deployment.md`；在此任务获批前，T124 和 T127 受阻
- [ ] T002 [P1] [P] 在 `tests/poc/deepseek-contract.test.ts` 中为超时、429、格式错误响应、单 URL 行为以及禁用提供方时的回退添加 DeepSeek Search/Fetch 契约测试夹具
- [ ] T003 [P2] [P] 在 `scripts/poc/meili-benchmark.ts` 和 `tests/poc/meili-benchmark.test.ts` 中为 Meilisearch 中文/英文/混合相关性创建固定种子的 100,000 条项目语料库、500 条查询性能集、100 条查询正确性集以及清单哈希
- [ ] T004 [P1] [P] 在 `tests/poc/page-capture.test.ts` 中验证单页截图成功、OCR 完整性和脱敏失败行为
- [ ] T005 [P0] [P] 在 `tests/poc/secret-safety.test.ts` 中验证高置信度密钥检测、脱敏以及无法扫描时的失败关闭行为
- [ ] T006 [P1] 在 `specs/001-ai-personal-knowledge-base/research.md` 和 `specs/001-ai-personal-knowledge-base/plan.md` 中记录提供方/搜索 PoC 结果、选定的回退方案以及未解决的操作风险

**检查点**：搜索、截图、OCR 或提供方路径未经核验时，不得继续任何生产实施。

---

## 阶段 1：环境搭建（共享基础设施）

**目的**：初始化 pnpm 工作区、运行时外壳、本地基础设施和交付
基线。

- [ ] T007 [P0] 在 `package.json`、`pnpm-workspace.yaml`、`turbo.json`、`apps/web/package.json`、`apps/worker/package.json` 和 `apps/web/tsconfig.json` 中创建 pnpm 工作区，包含 Node 引擎和包管理器版本固定、应用清单、tsconfig、根脚本和通用元数据
- [ ] T008 [P0] [P] 在 `tsconfig.base.json`、`packages/config/tsconfig.json` 和 `packages/domain/tsconfig.json` 中创建共享 TypeScript 基础配置
- [ ] T009 [P0] [P] 在 `apps/web/next.config.ts`、`apps/web/src/app/layout.tsx` 和 `apps/web/src/app/page.tsx` 中搭建使用 `basePath=/kb` 的 Next.js Web 应用
- [ ] T010 [P0] [P] 在 `apps/worker/src/index.ts`、`apps/browser-worker/src/index.ts` 和 `apps/browser-worker/package.json` 中搭建后台运行时和单页浏览器运行时
- [ ] T011 [P0] [P] 在 `packages/*/package.json` 下为 config、database、domain、providers、contracts 和 UI 创建共享包边界
- [ ] T012 [P0] [P] 在 `eslint.config.mjs`、`.prettierrc.json`、`vitest.config.ts`、`playwright.config.ts`、`schemathesis.toml` 和 `k6/search.js` 中配置 lint、格式化、单元、契约、E2E、可访问性、性能和安全运行器
- [ ] T013 [P0] [P] 在 `infra/compose.yml`、`infra/compose.env.example` 和 `infra/minio/init.sh` 中创建包含存储桶初始化和健康检查的本地 PostgreSQL、Meilisearch、MinIO、OCR 和 browser-worker 技术栈
- [ ] T014 [P0] [P] 在 `.github/workflows/ci.yml` 中创建基线 GitHub Actions 质量门禁
- [ ] T015 [P0] [P] 在 `infra/nginx/zhishiku.conf`、`infra/systemd/zhishiku-web.service`、`infra/systemd/zhishiku-worker.service`、`infra/systemd/zhishiku-browser-worker.service` 和 `infra/proxy/egress-proxy.ts` 中创建知识库 Nginx、systemd 和出站代理模板
- [ ] T016 [P0] [P] 在 `.env.example` 和 `README.md` 中创建完整的不含真实密钥的环境模板和启动文档

---

## 阶段 2：基础能力（阻塞性先决条件）

**目的**：构建每个用户故事都需要的共享持久化、安全、队列、搜索、可观测性和身份验证
基础设施。

**关键**：在此阶段通过其集成测试前，不得开始任何用户故事的实施。

- [ ] T017 [P0] 在 `packages/config/src/env.ts` 和 `packages/config/src/env.test.ts` 中实现仅报告变量名称的 Zod 环境校验
- [ ] T018 [P0] 在 `packages/db/src/client.ts`、`packages/db/src/migrate.ts` 和 `packages/db/drizzle.config.ts` 中配置 Drizzle、PostgreSQL 连接池和迁移执行
- [ ] T019 [P0] 在 `packages/db/src/schema/auth.ts`、`apps/web/src/server/auth/auth.ts` 和 `packages/domain/src/auth/owner.ts` 中实现 Better Auth 单所有者引导、数据库会话、关闭注册以及 `instance_key=1` 唯一性
- [ ] T020 [P0] 在 `packages/db/src/schema/knowledge.ts` 中创建 `knowledge_item`、`source` 和 `attachment` 模式，包含必填 `title` 长度 `1–300`、`source.input_method`、唯一 `UNIQUE(item_id)` 来源约束、唯一 `UNIQUE(storage_key)` 附件约束、生命周期/状态枚举、时间戳和 `version`
- [ ] T021 [P0] 在 `packages/db/src/schema/jobs.ts` 中创建 `capture_job`、`processing_job` 和 `ai_analysis` 模式，包含非负尝试次数、安全错误、可空完成时间戳和 `UNIQUE(owner_id, idempotency_key)` 唯一约束
- [ ] T022 [P0] 在 `packages/db/src/schema/classification.ts` 中创建 `category`、`tag`、`category_assignment`、`tag_assignment` 和 `tag_merge_suggestion` 模式，包含 `UNIQUE(item_id) WHERE superseded_at IS NULL`、`UNIQUE(item_id, tag_id) WHERE superseded_at IS NULL` 以及锁定/暂定状态
- [ ] T023 [P0] 在 `packages/db/src/schema/governance.ts` 中创建 `ai_processing_authorization`、`external_search_candidate`、`request_idempotency` 和 `audit_event` 模式，包含活动范围唯一性、候选项过期、幂等唯一性和仅含元数据的审计字段
- [ ] T024 [P0] 根据所有 Drizzle 模式生成迁移，并在 `packages/db/migrations/0001_foundation.sql` 中添加 `data-model.md` 中的索引，包括当前分配、任务租用、清除调度、附件查找和审计查找
- [ ] T025 [P0] 在 `packages/domain/src/shared/errors.ts` 和 `packages/domain/src/shared/state-machines.ts` 中实现共享领域错误、结果类型以及生命周期/状态转换防护
- [ ] T026 [P0] 在 `packages/providers/src/queue/boss.ts` 和 `apps/worker/src/queues.ts` 中为提取、分类、索引、浏览器采集、清除和核对配置 pg-boss 队列，包含重试/退避和 worker 注册
- [ ] T027 [P1] 在 `packages/providers/src/storage/s3.ts` 中实现私有 S3 对象存储适配器，包含暂存、提升、删除和签名访问
- [ ] T028 [P0] 在 `packages/domain/src/security/secret-scanner.ts` 和 `packages/domain/src/security/secret-scanner.test.ts` 中实现高置信度密钥检测、安全脱敏提示和文本脱敏
- [ ] T029 [P0] 在 `packages/providers/src/search/meilisearch.ts` 和 `packages/domain/src/search/index-document.ts` 中实现 Meilisearch 适配器、索引模式、带修订版本的更新插入/删除以及索引别名切换
- [ ] T030 [P0] 在 `packages/config/src/logger.ts` 和 `packages/config/src/telemetry.ts` 中实现结构化 Pino 日志以及 OpenTelemetry 请求/任务关联，且不记录内容日志
- [ ] T031 [P0] 在 `apps/web/src/server/api/middleware.ts` 和 `apps/web/src/server/api/idempotency.ts` 中实现经过身份验证的 API 中间件、请求指纹、幂等响应重放、JSON 校验、CSRF 防护、速率限制和安全错误封装
- [ ] T032 [P0] 在 `packages/contracts/src/generated.ts` 和 `scripts/check-openapi.ts` 中根据 OpenAPI 文档生成 Zod 契约类型并添加漂移检测
- [ ] T033 [P0] 在 `tests/integration/containers.ts` 中为 PostgreSQL、pg-boss、Meilisearch 和 MinIO 配置 Testcontainers 辅助工具
- [ ] T034 [P0] 在 `apps/web/src/app/layout.tsx`、`apps/web/src/messages/zh-CN.json` 和 `apps/web/src/messages/en.json` 中创建双语 Next.js 应用外壳和语言偏好提供程序
- [ ] T035 [P0] 在 `deploy/scripts/preflight-existing-app.sh` 中创建一个失败关闭的部署预检脚本，用于检查现有应用健康状况、systemd/MySQL/Redis 状态、端口冲突、磁盘、内存阈值和 Nginx 路由
- [ ] T036 [P0] 在 `tests/fixtures/factories.ts` 中为所有者、项目、分配、任务和候选项创建本地测试夹具工厂
- [ ] T037 [P0] 在 `apps/web/src/app/api/auth/[...all]/route.ts`、`apps/web/src/app/api/setup/route.ts` 和 `tests/integration/auth.test.ts` 中实现 Better Auth 路由处理器、一次性设置 API、退出/会话端点以及身份验证集成测试
- [ ] T038 [P0] 在 `packages/domain/src/shared/idempotency.ts` 和 `tests/integration/idempotency.test.ts` 中实现持久化幂等冲突处理、响应重放和过期清理，并添加测试
- [ ] T039 [P0] 在 `packages/db/migrations/0001_foundation.sql` 和 `tests/integration/migrations.test.ts` 中验证空数据库迁移、向前迁移和模式漂移检查
- [ ] T040 [P0] 在 `apps/worker/src/jobs/index-knowledge.ts` 和 `packages/domain/src/search/index-reconciler.ts` 中实现索引生产者、索引 worker、删除/恢复索引、完整重建和修订核对
- [ ] T041 [P1] 在 `apps/worker/src/jobs/periodic.ts` 中实现定期候选项过期、回收站清除、失败分类重试、索引核对和暂存清理任务
- [ ] T042 [P1] 在 `infra/proxy/egress-proxy.ts` 中实现会重新校验 DNS 的出站代理，包含目标 IP、端口、重定向和 DNS 重绑定检查
- [ ] T043 [P0] 在 `apps/web/src/app/health/route.ts` 和 `tests/integration/foundation-smoke.test.ts` 中添加健康端点，并执行针对迁移、auth、pg-boss、S3、Meilisearch 和双语启动的基础集成冒烟测试套件

**检查点**：数据库、身份验证、队列、存储、搜索、安全、i18n 和部署预检均已
就绪。

---

## 阶段 3：用户故事 1 - 手动收藏并找回知识（优先级：P1）

**目标**：私有所有者可以通过单一首页搜索框创建、打开、编辑、软删除并立即找回手动
知识。

**独立测试**：在 AI 提供方禁用的情况下，创建一个手动项目，并通过其标题、正文、备注、
分类和标签中的值找回该项目。

### 用户故事 1 的测试

- [ ] T044 [P0] [P] [US1] 在 `tests/contract/knowledge.test.ts` 中为知识 CRUD 和个人搜索添加 OpenAPI 契约测试
- [ ] T045 [P0] [P] [US1] 在 `tests/integration/manual-knowledge.test.ts` 中为手动保存、即时可搜索性、来源唯一性和乐观版本添加集成测试
- [ ] T046 [P0] [P] [US1] 在 `packages/domain/src/knowledge/knowledge-item.test.ts` 中为标题约束 `1–300`、生命周期转换和版本冲突添加单元测试

### 用户故事 1 的实施

- [ ] T047 [P0] [US1] 在 `packages/domain/src/knowledge/repository.ts` 中实现知识项目和来源仓库，包含事务性创建/更新和生命周期过滤
- [ ] T048 [P0] [US1] 在 `packages/domain/src/knowledge/service.ts` 中实现手动创建、列出、读取、更新、回收以及初始分类任务入队用例
- [ ] T049 [P0] [US1] 在 `packages/domain/src/search/postgres-fallback.ts` 中实现跨标题、正文、备注、分类和标签的 PostgreSQL 搜索回退
- [ ] T050 [P0] [US1] 在 `apps/web/src/app/api/v1/knowledge-items/route.ts`、`apps/web/src/app/api/v1/knowledge-items/[itemId]/route.ts` 和 `apps/web/src/app/api/v1/search/route.ts` 中实现知识集合、项目和个人搜索路由处理器
- [ ] T051 [P0] [P] [US1] 在 `apps/web/src/components/knowledge/manual-entry-form.tsx` 和 `apps/web/src/components/security/secret-notice.tsx` 中构建包含保存前密钥扫描以及安全脱敏/拒绝提示的手动录入表单
- [ ] T052 [P0] [P] [US1] 在 `apps/web/src/components/search/personal-search.tsx` 和 `apps/web/src/app/page.tsx` 中构建并组合单一首页搜索输入、结果列表、匹配指示器和互联网搜索入口
- [ ] T053 [P0] [US1] 在 `apps/web/src/app/library/page.tsx` 和 `apps/web/src/app/library/[id]/page.tsx` 中构建知识库以及项目详情/编辑路由
- [ ] T054 [P0] [US1] 在 `tests/e2e/taste-us1.spec.ts` 中为 P1 页面应用 `design-taste-frontend`，包含截图证据、人工评审、修复以及 1440/768/390 检查
- [ ] T055 [P0] [US1] 在 `tests/e2e/manual-knowledge.spec.ts` 中添加 P1 端到端测试，覆盖身份验证、手动保存、来源类型显示、全部五个可搜索字段、待处理状态可见性以及恰好一个搜索输入
- [ ] T056 [P0] [US1] 在 `apps/web/src/app/setup/page.tsx`、`apps/web/src/app/login/page.tsx` 和 `apps/web/src/components/app-shell.tsx` 中基于 Better Auth 端点构建最小设置/登录路由以及受保护的私有工作区外壳
- [ ] T057 [P0] [US1] 在 `apps/web/src/app/api/v1/trash/route.ts` 和 `apps/web/src/app/trash/page.tsx` 中为 P1 删除流程实现最小软删除和回收站恢复行为
- [ ] T058 [P0] [US1] 在不修改生产 Nginx 的前提下，仅在 `deploy/nginx/kb-staging.conf` 和 `tests/integration/nginx-staging.test.ts` 中通过回环暂存代理实现并验证 `/kb` 路由

**检查点**：US1 是独立的 MVP，并且在 AI 和互联网服务不可用时仍完全可用。

---

## 阶段 4：用户故事 2 - AI 自动归类并生成标签（优先级：P2）

**目标**：每个成功项目都会获得自动分类和标签集；低置信度值以暂定方式应用，用户更正会
锁定项目字段。

**独立测试**：在没有手动分类体系的情况下保存未分类文本，验证自动分类/标签，然后锁定
一个已编辑值，并证明重新分析无法覆盖它。

### 用户故事 2 的测试

- [ ] T059 [P0] [P] [US2] 在 `packages/domain/src/classification/assignment.test.ts` 中为暂定/已应用/已锁定分配优先级添加单元测试
- [ ] T060 [P0] [P] [US2] 在 `tests/contract/ai-provider.test.ts` 中为有效、格式错误、超时和限流 AI 输出添加提供方契约测试
- [ ] T061 [P0] [P] [US2] 在 `tests/integration/classification.test.ts` 中为分类任务、重试、过期修订和锁定字段保留添加集成测试

### 用户故事 2 的实施

- [ ] T062 [P0] [US2] 在 `packages/domain/src/ai/authorization-service.ts` 中按 `service_code` 和 `content_type` 实现范围受限的 AI 处理授权检查，包括数据范围/提供方/用途披露以及撤销行为
- [ ] T063 [P0] [US2] 在 `packages/providers/src/ai/deepseek-classification.ts` 中实现具有严格结构化输出校验和相关项目建议的 DeepSeek 分类适配器
- [ ] T064 [P0] [US2] 在 `packages/domain/src/classification/repository.ts` 中实现分类/标签分配仓库和项目字段锁定更新
- [ ] T065 [P0] [US2] 在 `apps/worker/src/jobs/classify-item.ts` 中实现包含暂定回退、修订检查和有界重试的分类 worker
- [ ] T066 [P0] [US2] 在 `packages/domain/src/classification/tag-normalization.ts` 中为未锁定值实现自动标签规范化与合并建议
- [ ] T067 [P0] [US2] 在 `apps/web/src/app/api/v1/ai-authorizations/route.ts`、`apps/web/src/app/api/v1/ai-authorizations/[authorizationId]/route.ts`、`apps/web/src/app/api/v1/knowledge-items/[id]/classifications/route.ts` 和 `apps/web/src/app/api/v1/knowledge-items/[id]/reanalyze/route.ts` 中实现授权授予/列出/撤销、重新分析和分类更正端点
- [ ] T068 [P0] [P] [US2] 在 `apps/web/src/components/settings/ai-authorization-matrix.tsx` 和 `apps/web/src/app/settings/authorizations/page.tsx` 中构建包含数据范围、提供方、内容类型、用途、撤销和重新同意的 AI 授权矩阵
- [ ] T069 [P0] [P] [US2] 在 `apps/web/src/components/knowledge/classification-editor.tsx` 中构建暂定、已锁定、已更正、相关项目、来源类型和合并建议分类控件
- [ ] T070 [P0] [US2] 在 `apps/web/src/components/knowledge/processing-status.tsx` 中构建待处理、运行中、等待授权、失败和暂定项目状态
- [ ] T071 [P0] [US2] 在 `tests/e2e/classification.spec.ts` 中添加 US2 端到端测试，覆盖自动分类、授权范围拒绝、撤销后不再未来传输、锁定保留、跨项目锁定隔离和重试
- [ ] T072 [P0] [US2] 在 `tests/security/prompt-injection.test.ts` 中为分类、相关项目输出和恶意导入内容添加提示注入测试
- [ ] T073 [P0] [US2] 在 `tests/e2e/taste-us2.spec.ts` 中为授权和分类页面应用 `design-taste-frontend`，包含截图证据、人工评审、修复以及 1440/768/390 检查
- [ ] T074 [P2] [US2] 在 `tests/performance/classification-quality.test.ts` 中为 SC-003 和 SC-010 添加 100 个已标注项目的分类质量以及自动分类/标签覆盖率基准测试

**检查点**：US2 在 US1 之上运行，且绝不会阻塞保存或搜索。

---

## 阶段 5：用户故事 3 - 通过网页、文档和截图采集知识（优先级：P3）

**目标**：已接受的文本、文档、截图和单页 URL 导入会立即创建可搜索项目，并在后台安全
完成提取。

**独立测试**：导入一个文档、一个 JPG/PNG 图片文件、一张剪贴板截图和一个本地/公开网页；验证即时可搜索性、来源保留、
密钥扫描以及不遍历链接。

### 用户故事 3 的测试

- [ ] T075 [P1] [P] [US3] 在 `tests/contract/imports.test.ts` 中为文本、网页、文档/图片文件上传和剪贴板截图导入端点添加 OpenAPI 契约测试，覆盖合法 `screenshot+clipboard`、`document+file_upload` 组合和非法组合 `422`
- [ ] T076 [P1] [P] [US3] 在 `tests/integration/import-safety.test.ts` 中为暂存拒绝、文档提取、OCR、脱敏图像和重试幂等性添加集成测试
- [ ] T077 [P1] [P] [US3] 在 `tests/security/page-capture.test.ts` 中为回环/私有/CGNAT/IPv6 ULA/元数据/保留 IP、DNS 重绑定、凭据 URL、强制使用 80/443、最多三个重新校验的重定向、禁止 sitemap/预取/无限滚动/多页、单一顶层请求图以及无持久 Cookie/Service Worker/弹窗/下载添加 browser-worker E2E 安全测试

### 用户故事 3 的实施

- [ ] T078 [P1] [US3] 在 `packages/domain/src/import/staging-service.ts` 中实现安全上传暂存、MIME 检测、大小限制和清理
- [ ] T079 [P1] [P] [US3] 在 `packages/providers/src/extraction/document-ocr.ts` 和 `packages/config/src/upload.ts` 中实现支持的 MIME 校验、文档提取以及带语言包和完整性状态的有界 OCR
- [ ] T080 [P1] [P] [US3] 在 `packages/domain/src/security/image-redactor.ts` 中实现图像敏感区域脱敏和脱敏副本持久化
- [ ] T081 [P1] [P] [US3] 在 `packages/domain/src/import/url-safety.ts` 和 `infra/proxy/egress-proxy.ts` 中实现 URL 校验、连接时 DNS/IP 拒绝、重定向限制和出站代理集成
- [ ] T082 [P1] [US3] 在 `apps/browser-worker/src/capture-page.ts` 和 `apps/browser-worker/src/queues.ts` 中实现隔离的 Playwright 单页采集 worker 及其专用 pg-boss 采集消费者，不进行链接发现且不保持持久会话
- [ ] T083 [P1] [US3] 在 `apps/web/src/app/api/v1/imports/text/route.ts`、`apps/web/src/app/api/v1/imports/webpage/route.ts`、`apps/web/src/app/api/v1/imports/upload/route.ts`、`apps/web/src/app/api/v1/jobs/[jobId]/route.ts`、`apps/web/src/app/api/v1/jobs/[jobId]/retry/route.ts` 和 `apps/web/src/app/api/v1/jobs/[jobId]/cancel/route.ts` 中实现文本、网页、文档/图片文件和剪贴板截图导入，以及任务状态、重试和取消路由处理器
- [ ] T084 [P1] [US3] 在 `apps/worker/src/jobs/process-import.ts` 中为已接受的导入实现提取、OCR、脱敏、处理任务和分类任务入队
- [ ] T085 [P1] [P] [US3] 在 `apps/web/src/components/import/import-panel.tsx` 和 `apps/web/src/app/capture/page.tsx` 中构建文本、URL、文档/图片文件上传和剪贴板截图粘贴表单；截图页不得出现文件选择控件，并提供前往文档入口的安全回退
- [ ] T086 [P1] [P] [US3] 在 `apps/web/src/components/import/import-status.tsx` 和 `apps/web/src/app/processing/page.tsx` 中构建导入扫描、拒绝、待处理、重试、截图回退和处理队列状态
- [ ] T087 [P1] [US3] 在 `tests/e2e/imports.spec.ts` 中为所有导入模式和即时可搜索性添加 US3 端到端测试，覆盖真实剪贴板粘贴、截图页无文件/拖放/相册控件、剪贴板失败回退、文档以及 JPG/PNG/WebP 图片文件上传
- [ ] T088 [P1] [US3] 在 `tests/e2e/taste-us3.spec.ts` 中为导入和处理页面应用 `design-taste-frontend`，包含截图证据、人工评审、修复以及 1440/768/390 检查
- [ ] T089 [P1] [US3] 在 `tests/security/import-injection.test.ts` 中为文档文本、OCR 文本、网页采集和搜索摘要添加提示注入测试
- [ ] T090 [P1] [US3] 在 `tests/performance/import-quality.test.ts` 中为 SC-004、SC-005 和即时搜索标准添加文本/文档/截图导入基准测试

**检查点**：US3 扩展采集能力，且不会削弱 US1 或 US2。

---

## 阶段 6：用户故事 4 - 单框本地优先混合检索（优先级：P4）

**目标**：单一搜索框始终优先返回个人结果，只有明确同意后才会进行互联网搜索；已确认的
互联网候选项会成为可搜索项目。

**独立测试**：搜索一个同时具有本地和互联网匹配的术语，验证个人结果优先顺序，确认仅发送
查询内容，并将一个临时候选项保存到知识库。

### 用户故事 4 的测试

- [ ] T091 [P1] [P] [US4] 在 `tests/contract/external-search.test.ts` 中为外部搜索和候选项保存添加 OpenAPI 契约测试
- [ ] T092 [P1] [P] [US4] 在 `tests/integration/external-search.test.ts` 中为明确同意、临时候选项过期和确认前不创建项目添加集成测试
- [ ] T093 [P1] [P] [US4] 在 `tests/e2e/hybrid-search.spec.ts` 中为个人结果优先顺序和外部故障隔离添加浏览器 E2E 测试

### 用户故事 4 的实施

- [ ] T094 [P1] [P] [US4] 在 `packages/providers/src/search/deepseek-search.ts` 中实现包含超时、重试和提供方错误映射的 DeepSeek Search 适配器
- [ ] T095 [P1] [P] [US4] 在 `packages/domain/src/search/external-candidate-service.ts` 中实现 15 分钟外部候选项缓存和过期清除
- [ ] T096 [P1] [US4] 在 `apps/web/src/app/api/v1/search/external/route.ts` 和 `apps/web/src/app/api/v1/search/external/[candidateId]/save/route.ts` 中实现明确同意的外部搜索和幂等候选项保存 API 处理器
- [ ] T097 [P1] [US4] 在 `packages/domain/src/search/save-candidate.ts` 中将候选项确认实现为 PostgreSQL 事务，原子地占用 `saved_item_id`、创建项目/来源，并将分类任务和尽力而为的索引任务入队
- [ ] T098 [P1] [P] [US4] 在 `apps/web/src/components/search/external-search.tsx` 中构建外部搜索同意、提供方披露、结果分组和保存控件
- [ ] T099 [P1] [US4] 在 `apps/web/src/components/search/search-page.tsx` 中实现外部加载、超时、无结果、部分失败和重试期间保留本地结果
- [ ] T100 [P1] [US4] 在 `tests/e2e/external-search-flow.spec.ts` 中为本地优先顺序和候选项保存添加 US4 验收自动化
- [ ] T101 [P1] [US4] 在 `tests/e2e/taste-us4.spec.ts` 中为混合搜索应用 `design-taste-frontend`，包含截图证据、人工评审、修复以及 1440/768/390 检查
- [ ] T102 [P1] [US4] 在 `tests/security/external-transmission.test.ts` 中添加网络级测试，证明仅发送查询内容、不发送个人正文/备注/附件，且撤销范围后不会产生未来的外部请求
- [ ] T103 [P1] [US4] 在 `tests/performance/single-box-search.test.ts` 中为 SC-011 和本地优先结果排序添加 100 条查询的单输入检索基准测试

**检查点**：US4 在保留个人搜索的同时增加可选的互联网检索。

---

## 阶段 7：用户故事 5 - 双语且安全的个人使用（优先级：P5）

**目标**：所有者可以使用完整的中文和英文体验、管理策略状态，并在不暴露凭据或不更改
用户内容的情况下恢复已删除项目。

**独立测试**：在所有主要流程中切换语言，验证英文覆盖情况，检查密钥金丝雀，并通过
回收站恢复/删除项目。

### 用户故事 5 的测试

- [ ] T104 [P1] [P] [US5] 在 `tests/unit/i18n-messages.test.ts` 中为中文和英文键添加 ICU/消息完整性测试
- [ ] T105 [P0] [P] [US5] 在 `tests/security/secret-canaries.test.ts` 中添加跨源代码管理文件、配置、文档、测试、日志、截图、构建产物、版本控制夹具、诊断信息、DOM、API、数据库、Meilisearch、S3 和遥测的唯一金丝雀测试
- [ ] T106 [P1] [P] [US5] 在 `tests/integration/recycle-bin.test.ts` 中为使用假时钟的回收站生命周期和过期添加测试

### 用户故事 5 的实施

- [ ] T107 [P1] [US5] 在 `apps/web/src/messages/zh-CN.json` 和 `apps/web/src/messages/en.json` 中为每个主要路由、错误、安全提示、破坏性操作和处理状态完善中文和英文消息
- [ ] T108 [P1] [US5] 在 `apps/web/src/server/preferences/language.ts` 中实现持久化语言选择、AI 输出语言偏好以及不翻译用户内容
- [ ] T109 [P0] [US5] 在 `apps/web/src/app/login/page.tsx` 和 `apps/web/src/app/setup/page.tsx` 中完成设置/登录/退出/会话集成，包含 `/kb` 范围内的 Cookie 行为和会话恢复
- [ ] T110 [P0] [P] [US5] 在 `apps/web/src/components/security/secret-notice.tsx` 中构建不显示匹配值的安全密钥脱敏和拒绝提示
- [ ] T111 [P1] [US5] 在 `apps/web/src/app/api/v1/trash/route.ts`、`apps/web/src/app/api/v1/trash/[itemId]/restore/route.ts` 和 `apps/web/src/app/api/v1/trash/[itemId]/route.ts` 中实现回收站列出、恢复、永久清除和已披露过期路由处理器
- [ ] T112 [P1] [P] [US5] 在 `apps/web/src/app/trash/page.tsx` 和 `apps/web/src/app/settings/page.tsx` 中构建回收站以及设置/隐私页面
- [ ] T113 [P1] [US5] 在 `tests/e2e/bilingual-security.spec.ts` 中为默认中文、每个路由/状态、用户内容不变、AI 输出语言和过期消息添加双语及回收站 E2E 覆盖
- [ ] T114 [P2] [US5] 在 `tests/e2e/taste-us5.spec.ts` 中为登录、设置、安全和回收站页面应用 `design-taste-frontend`，包含截图证据、人工评审、修复以及 1440/768/390 检查
- [ ] T115 [P1] [US5] 在 `tests/accessibility/manual-checklist.md` 中添加针对焦点顺序、对话框、标签、状态播报、对比度、重排、目标大小和屏幕阅读器语义的人工 WCAG 2.2 AA 检查
- [ ] T116 [P2] [US5] 为 SC-001 和 SC-009 执行有 10 名参与者引导的可用性研究，并在 `tests/usability/protocol.md` 和 `artifacts/usability/results.md` 中记录观察结果、通过计数和结果
- [ ] T117 [P1] [US5] 在 `tests/integration/recycle-cascade.test.ts` 中为立即从搜索中移除、来源/附件/分析/索引清理、恢复和永久清除添加完整的回收站级联断言

**检查点**：全部五个用户故事均可独立测试并安全共存。

---

## 阶段 8：完善与跨领域关注事项

**目的**：发布门禁、性能、部署、恢复和文档。

- [ ] T118 [P1] 在 `tests/contract/all-endpoints.test.ts` 中运行完整的 OpenAPI 契约套件并记录失败
- [ ] T119 [P1] [P] 在 `tests/e2e/accessibility.spec.ts` 中添加并通过 WCAG 2.2 AA 自动化和键盘检查
- [ ] T120 [P2] [P] 在 `tests/performance/run-search-benchmark.ts` 和 `artifacts/performance/search-report.json` 中执行冻结的 100,000 条项目、500 条查询基准测试，包含冷/热缓存、C2 并发、20 个可见结果以及 95% 低于 2 秒的报告
- [ ] T121 [P1] [P] 在 `.github/workflows/security.yml` 和 `tests/security/leak-surfaces.test.ts` 中为所有禁止表面添加 Semgrep、Gitleaks、Trivy、OWASP ZAP 和金丝雀泄漏检查
- [ ] T122 [P1] 在 `deploy/scripts/backup.sh` 中实现不包含破坏性恢复操作的 pgBackRest/restic 备份脚本
- [ ] T123 [P1] 在 `deploy/scripts/provision-ubuntu.sh` 和 `deploy/scripts/lib/deploy-common.sh` 中实现官方 GitHub 一键配置入口，包含固定 ref 选择、独立的用户/数据库/存储桶/环境/Cookie/日志路径、部署前备份、依赖安装、构建、迁移、启动、健康检查和失败关闭回滚
- [ ] T124 [P1] 在 `deploy/scripts/configure-nginx.sh`、`deploy/nginx/shuati.conf` 和 `deploy/nginx/shuati-prod.conf` 中配置获批的精确 `/` 门户，将原着陆页保留在 `/shuati`，并添加 `/kb` 路由，同时包含备份和 `nginx -t` 回滚
- [ ] T125 [P1] 在 `deploy/scripts/verify-existing-app.sh` 和 `tests/e2e/zero-impact.test.ts` 中通过比较变更前 `/` 与变更后 `/shuati`、断言获批的 `/` 门户例外，并保留 systemd、MySQL、Redis、Cookie、数据、`/app`、`/api`、`/bank`、`/pay`、`/apk` 和证书基线来验证零影响
- [ ] T126 [P1] 在 `.github/workflows/deploy.yml` 中接入 GitHub Actions 发布部署门禁以及 tag/commit 选择
- [ ] T127 [P1] 在 `deploy/portal/index.html`、`deploy/portal/portal.css` 和 `deploy/portal/messages.json` 中添加双语根双应用门户，包含拾题 `/app` 和知识库 `/kb` 条目
- [ ] T128 [P1] 在干净环境中运行完整的 [quickstart.md](quickstart.md) 场景，并将命令/输出证据保存到 `artifacts/quickstart/report.md`
- [ ] T129 [P1] 使用官方 GitHub 仓库 URL、默认分支、固定 ref 部署用法、最终路径和已验证命令更新 [README.md](../../README.md)、[deployment.md](deployment.md) 和 `AGENTS.md`
- [ ] T130 [P2] 在 `tests/integration/restore-drill.test.ts` 中于隔离环境运行备份恢复，从 PostgreSQL 重建 Meilisearch，验证回收窗口恢复，并证明过期备份无法恢复数据
- [ ] T131 [P1] 在 `deploy/scripts/capacity-gate.sh` 和 `artifacts/deployment/capacity-report.md` 中添加包含阈值和批准证据的失败关闭主机容量与部署配置决策门禁
- [ ] T132 [P1] 在 `tests/deploy/one-click-deploy.test.sh` 和 `artifacts/deployment/one-click-report.md` 中从固定 tag/commit 运行两次官方 HTTPS GitHub 一键部署，注入健康检查失败，验证回滚，并扫描日志/命令历史中的密钥
- [ ] T133 [P2] 在 `artifacts/acceptance/success-criteria.md` 中运行 SC-001 至 SC-017 的最终验收证据，验证目标 tag/commit 通过 lint、类型检查、测试、构建和安全门禁，确认失败的门禁会阻止部署，并将每项结果映射到其任务和产物

---

## 阶段 9：付费功能跨文档集成门禁

**目的**：把 001 的 AI 入口与 002 的计费能力连接起来；计费权威规则只存在于 002，本阶段
不复制价格或账本实现。

- [ ] T134 [P0] 在 `packages/domain/src/ai/authorization-service.ts` 和 `apps/worker/src/jobs/classify-item.ts` 中同时校验 AI 隐私授权、自动扣点授权、有效计费任务和 `RESERVED` 状态，缺一即进入 `blocked_billing` 或 `blocked_authorization`
- [ ] T135 [P1] 在 `apps/worker/src/jobs/process-import.ts` 中把网页、文档和截图任务绑定到 002 计费任务，并在每个成功/失败步骤回调结算或释放
- [ ] T136 [P1] 在 `apps/web/src/app/api/v1/search/external/route.ts` 和候选保存流程中区分搜索 3 点、单次回答 2 点和缺失计费任务，不把隐私授权当作扣点授权
- [ ] T137 [P0] 在 `tests/e2e/billing-gate.spec.ts` 中针对已启用的自动分类验证没有报价、余额不足、自动扣点关闭、报价过期和桥接不可用时，AI 不执行但知识仍保存、可搜索并保持待整理
- [ ] T138 [P0] 在 `tests/e2e/paid-ai-happy-path.spec.ts` 中验证测试购买、自动分类报价确认、成功扣费、余额更新、失败释放和订单/点数明细事实一致
- [ ] T139 [P1] 在 `tests/e2e/paid-ai-refund.spec.ts` 中验证退款冻结原批次、已有 AI 任务按规则完成结算、用户看到退款终态
- [ ] T140 [P1] 在 `deploy/scripts/provision-ubuntu.sh`、`infra/nginx/zhishiku.conf` 和 `infra/systemd/` 中将 `/kb/pay/webhooks/waffo`、8091 桥接、独立环境文件、启用开关和回滚纳入一键部署
- [ ] T141 [P1] 在 `tests/e2e/zero-impact.test.ts` 中把识海独立订单、点数、Cookie、Waffo 回调、桥接和拾题 `/pay` 的变更前后基线一并验证
- [ ] T142 [P1] 在 `artifacts/acceptance/billing-success-criteria.md` 中映射 002 SC-001 至 SC-022、测试证据、目标提交和产品批准记录
- [ ] T143 [P2] 在 `artifacts/acceptance/project-scope.md` 中汇总 001 SC-001 至 SC-017 与 002 SC-001 至 SC-022 的最终项目验收状态，任何 P0/P1 失败均阻止生产发布

---

## 成功标准覆盖

| 标准 | 优先级 | 主要验证任务 |
|-----------|----------|----------------------------|
| SC-001 | P2 | T116 |
| SC-002 | P2 | T120 |
| SC-003 | P2 | T074 |
| SC-004 | P2 | T090 |
| SC-005 | P1 | T077, T090 |
| SC-006 | P1 | T093, T100 |
| SC-007 | P0 | T105, T121 |
| SC-008 | P1 | T107, T113 |
| SC-009 | P2 | T116 |
| SC-010 | P2 | T074 |
| SC-011 | P2 | T103 |
| SC-012 | P0 | T071, T102 |
| SC-013 | P1 | T117, T130 |
| SC-014 | P0 | T076, T105 |
| SC-015 | P0 | T071, T102 |
| SC-016 | P0 | T045, T087 |
| SC-017 | P0 | T059, T061, T071 |

**付费扩展验收**：002 的 SC-001 至 SC-022 由 T134 至 T143 与
[002 tasks.md](../002-ai-credit-billing/tasks.md) 共同验证；不得只引用开发自测报告。

| 付费扩展需求 | 优先级 | 主要验证任务 |
|--------------|--------|--------------|
| FR-063 有效计费任务门禁 | P0 | T134、T135、T136、T137 |
| FR-064 独立自动扣点授权 | P0 | T134、T137、T138 |
| FR-065 余额不足不丢内容 | P0 | T137、T138 |

---

## 依赖与执行顺序

### 阶段依赖

- **阶段 0 可行性（T001–T006）**：立即开始；提供方和搜索 PoC 决定
  相应的实施决策。
- **阶段 1 环境搭建（T007–T016）**：可与不依赖它的 PoC 工作同时开始。
- **阶段 2 基础能力（T017–T043）**：依赖环境搭建，并阻塞所有用户故事。
- **US1（T044–T058）**：在阶段 2 之后开始，并建立 MVP。
- **US2（T059–T074）**：需要 US1 持久化和分类入口。
- **US3（T075–T090）**：需要 US1 项目创建和 US2 分类 worker 接口。
- **US4（T091–T103）**：需要 US1 搜索和 US2 自动分类。
- **US5（T104–T117）**：横跨产品；其语言和生命周期工作在
  故事路由稳定后完成。
- **完善（T118–T133）**：依赖所有选定的故事和发布产物。

### 关键路径

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
  -> T134-T143
```

### P0 MVP 路径

```text
T005
  -> T007-T043
  -> T044-T073
  -> T105, T109, T110
  -> T134, T137, T138
  -> P0 检查点
```

P0 例外位于后续故事阶段中，会在 US1 和 US2 稳定后有意提前。它们不要求先实施
P1 导入或互联网搜索。

### 用户故事依赖

- **US1**：在阶段 2 之后独立。
- **US2**：需要 US1 项目持久化，但必须保留 US1 行为。
- **US3**：需要 US1 项目创建和 US2 分类 worker 接口。
- **US4**：需要 US1 搜索和 US2 自动分类。
- **US5**：横跨所有故事，但可以在阶段 2 之后增量实施。
- **阶段 9**：T134、T137、T138 是 001 P0 的自动分类付费门禁，必须与 US1/US2 一起完成；
  T135、T136 随 001 P1 导入和搜索实现，T139-T142 在公开收费前完成。

---

## 并行机会

### 环境搭建

```text
T008-T016 使用不同文件，并可在 T007 之后并行运行。
```

### 用户故事 1

```text
T044-T046 可以并行编写。
T050 定义 API 行为后，T051 和 T052 可以并行构建。
```

### 用户故事 2

```text
T059-T061 可以并行运行。
T062 和 T064 定义契约后，T068 和 T069 可以并行运行。
```

### 用户故事 3

```text
T075-T077 可以并行运行。
T078-T081 使用独立模块，可以并行运行。
T083 之后，T085 和 T086 可以并行运行。
```

### 用户故事 4

```text
T091-T093 可以并行运行。
外部候选项契约稳定后，T094、T095 和 T098 可以并行运行。
```

### 用户故事 5

```text
T104-T106 可以并行运行。
T107-T109 稳定路由和消息键后，T110 和 T112 可以并行运行。
```

### 完善

```text
T119-T121 可以并行运行。
T122、T123 和 T130 是顺序执行的部署和恢复任务，不是并行编辑。
```

---

## 实施策略

### MVP 优先 — 仅 P0

1. 按依赖顺序完成每个 `[P0]` 任务。P0 包括环境搭建、基础持久化、
   安全、私有身份验证、手动采集、自动分类、单框本地
   搜索、核心 UI 和核心安全验证。
2. 在 P0 检查点停止，并在本地/暂存环境验证最小可用的安全端到端产品。
3. 在 P0 检查点通过前，不得开始 P1 扩展或公开 `/kb` 路由。
4. P0 范围包含 71 个任务，因为环境搭建、平台基础和自动分类付费门禁属于真实 MVP 的一部分，
   而不是模拟实现。

### 增量交付 — 先 P1，再 P2

1. **P1 核心扩展（64 个任务）**：文档/截图/网页导入、互联网搜索、
   完整双语和回收站工作流、生产路由、GitHub 一键部署以及
   其余发布控制。
2. **P2 规模与加固（8 个任务）**：100,000 条项目性能基准测试、分类和
   导入质量基准测试、单框检索基准测试、人工可访问性和可用性
   研究、备份恢复演练以及最终成功标准证据。
3. 仅在 P1 稳定后完成 P2。

### 并行团队策略

- 开发子智能体 A：US1 以及共享知识/搜索路径。
- 开发子智能体 B：US2 分类和 AI 授权。
- 开发子智能体 C：US3 浏览器 worker 和导入流水线。
- 开发子智能体 D：US4 外部搜索。
- 开发子智能体 E：US5 i18n、安全用户体验和回收站。
- 测试子智能体：独立故事验收、金丝雀、SSRF、性能和零影响检查。

---

## 备注

- 项目章程要求测试，并且测试必须在相应的
  实施任务之前失败。
- [P] 任务不得与另一个活动任务编辑同一文件。
- P0 任务定义 MVP，必须在 P1 或 P2 工作开始前完成。
- P1 和 P2 对最终发布并非可选；它们安排在 MVP 验证之后。
- 全部五个故事检查点都必须独立演示。
- 在公开 `/kb` 前，部署工作必须通过零影响检查。
- 根门户变更是一项明确获批的产品决策；现有应用
  必须仍可通过 `/app` 访问，其原着陆页必须仍可通过 `/shuati` 访问。
- 在每个任务或一组连贯任务后提交。
