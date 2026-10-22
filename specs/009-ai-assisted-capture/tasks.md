# 任务：AI 辅助采集统一规则

**输入**：[spec.md](spec.md)、[plan.md](plan.md)、[research.md](research.md)、
[data-model.md](data-model.md)、[quickstart.md](quickstart.md)

## 优先级

- **P0**：手动直存边界、AI 文字、文档图片 OCR、互联网候选 AI 整理、服务端防绕过。
- **P1**：双语、移动端、原型同步、Taste 验收和全量回归。
- **P2**：HEIC/HEIF、多图合并、公式和复杂表格增强。

## Phase 1：规格与治理

- [x] T001 [P0] 产品角色完成需求评审，确认 AI 文字、文档图片和互联网候选保存是主要绕过路径
- [x] T002 [P0] 在 `specs/009-ai-assisted-capture/` 中完成中文规格、质量检查、计划、研究、数据模型、契约和快速验证
- [x] T003 [P0] 在 `AGENTS.md` 和 `.specify/memory/constitution.md` 固化“手动录入唯一可直存”规则并把宪法升级为 `3.1.0`
- [x] T004 [P1] [P] 同步 `specs/001-ai-personal-knowledge-base/`、`003`、`004`、`006`、`007`、`008`、`README.md` 和主 OpenAPI

## Phase 2：服务端统一边界

- [x] T005 [P0] [P] [US1] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/knowledge/KnowledgeServiceManualSourceTest.java` 验证普通创建接口拒绝 `document`、`screenshot`、`internet_result` 和 `text`
- [x] T006 [P0] [US1] 在 `KnowledgeService.java` 和 `KnowledgeController.java` 增加 `createManual` 手动来源白名单
- [x] T007 [P0] [P] [US2] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/textimport/` 验证 AI 文字脱敏、多知识、授权、计费、失败和 Controller
- [x] T008 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/textimport/` 实现 `POST /api/v1/text-imports`
- [x] T009 [P0] [US2] 在 `V0008__ai_text_source.sql`、`SourceProvenancePolicy.java` 和来源映射中加入 `text`
- [x] T010 [P0] [P] [US2] 在 `DocumentTextExtractorTest.java` 和 `DocumentImportServiceTest.java` 验证文档图片 OCR、安全检查、无附件和 `succeeded`
- [x] T011 [P0] [US2] 在 `DocumentTextExtractor.java` 和 `DocumentImportService.java` 实现 JPG/PNG/WebP OCR 与统一 AI 整理
- [x] T012 [P0] [P] [US2] 在 `ExternalSearchServiceTest.java` 验证互联网候选授权、脱敏、AI 整理和失败关闭
- [x] T013 [P0] [US2] 在 `ExternalSearchService.java`、DTO 和 Controller 中改为 Fetch 后 AI 多知识事务入库

## Phase 3：前端与原型

- [x] T014 [P0] [US1] 保持 `CaptureView.vue` 手动模式直接调用普通知识创建接口，AI 不可用时不阻塞
- [x] T015 [P0] [US2] 在 `CaptureView.vue` 增加独立“AI 文字”入口、授权、错误和调用 `/text-imports`
- [x] T016 [P0] [US2] 在 `CaptureView.vue` 删除文档图片直接附件路径，让 JPG/PNG/WebP 统一调用 `/document-imports`
- [x] T017 [P0] [US2] 在 `HomeView.vue`、`client.ts` 和 `types/api.ts` 改为互联网候选 AI 整理后收入多知识响应
- [x] T018 [P1] [P] [US3] 在 `i18n.ts` 和首页/详情来源映射中补齐 AI 文字、互联网结果、图片错误和授权文案
- [x] T019 [P1] [US2] 在 `prototype/index.html`、`prototype/app.js` 和 `prototype/README.md` 同步五种采集模式和 AI 整理语义
- [x] T020 [P1] [P] [US3] 在 `frontend/kb-web/e2e/app.spec.ts` 验证 AI 文字、文档图片和互联网候选都绕过普通创建/图片上传路径
- [x] T021 [P1] [P] [US3] 在 `tests/e2e/ai-capture-policy-taste.spec.ts` 覆盖 1440/768/430/390/320 五档视口
- [x] T022 [P1] [US3] 执行 AI 文字、文档、截图和网页四条移动 Taste 场景，全部通过

## Phase 4：回归与交付

- [x] T023 [P0] 执行后端定向和全量测试；`kb-core` 18 项、`kb-app` 175 项、worker 1 项通过
- [x] T024 [P0] 执行前端 lint、typecheck、Vitest 和 34 项 E2E
- [x] T025 [P0] 执行 Spotless、后端生产包构建、前端生产构建
- [x] T026 [P1] 执行 Taste 源检查、App Screen inspector 和截图人工复核
- [x] T027 [P1] 校验 OpenAPI/YAML、原型语法、部署脚本和 `git diff --check`
- [x] T028 [P1] 在 `artifacts/009/verification.md` 记录验收证据和剩余风险
- [ ] T029 [P1] 在生产完成真实 AI 文字、文档图片 OCR、互联网候选 AI 整理和计费门禁验收

## Phase 5：后续加固与 P2

- [x] T030 [P1] 为 AI 文字、网页、文档和互联网候选增加服务端幂等与重复扣费防护
- [ ] T031 [P1] 在真实 PostgreSQL 模拟第二条知识写入失败，验证整次事务回滚
- [x] T032 [P2] 支持 HEIC/HEIF 文档图片 OCR
- [ ] T033 [P2] 增加多图合并、公式和复杂表格识别增强

## 依赖与执行顺序

1. T001–T004 冻结规则和契约。
2. T005–T013 建立服务端边界。
3. T014–T022 完成前端、原型和浏览器验收。
4. T023–T028 执行全量回归和证据归档。
5. T029 需要生产 OCR、DeepSeek、计费和原应用零影响条件。

## MVP 范围

T001–T024 构成“手动直存 + 所有非手动入口 AI 整理”的可运行闭环；T025–T028 是交付门禁。

## Completion Log

- 日期：2026-10-22
- 状态：T001–T030、T032 已完成；T031、T033 未完成
- 执行角色：产品、开发、测试子智能体
- Git commit：未提交

| 任务 | 摘要与详情 | 验证与证据 | 风险与回滚 |
|------|------------|------------|------------|
| T001 | 产品评审确认 AI 文字、文档图片和互联网候选是主要绕过路径。 | 独立产品评审结论；本任务只读。 | 范围变化需重开规格。 |
| T002 | 完成 009 中文规格、检查表、计划、研究、数据模型、契约和快速验证。 | YAML 和文档结构检查通过。 | 回滚文档会移除统一边界。 |
| T003 | AGENTS 与宪法写入手动唯一可直存规则，宪法升级为 `3.1.0`。 | [spec.md](../spec.md)、[AGENTS.md](../../AGENTS.md)、[constitution.md](../../.specify/memory/constitution.md)。 | 治理回滚需产品批准。 |
| T004 | 同步 001/003/004/006/007/008、README 和主 OpenAPI。 | 多文档交叉链接和 YAML 解析通过。 | 文档需同代码回滚。 |
| T005 | 验证普通创建和冲突另存副本都拒绝非手动专用来源。 | `KnowledgeServiceManualSourceTest` 2 项。 | 白名单回滚会重开绕过风险。 |
| T006 | 实现 `createManual` 和手动来源白名单。 | 后端定向和全量测试通过。 | 与专用导入服务同回滚。 |
| T007 | 验证 AI 文字脱敏、授权、计费、多知识和 Controller。 | `TextImportServiceTest` 3 项、Controller 1 项。 | 失败不创建部分知识。 |
| T008 | 新增 `POST /api/v1/text-imports`。 | MockMvc 和 E2E 通过。 | 可移除端点恢复旧采集范围。 |
| T009 | 新增 `text` 来源、数据库约束和来源映射。 | `V0008__ai_text_source.sql`、来源策略测试。 | 迁移回滚不得删除已保存知识。 |
| T010 | 验证文档图片 OCR、无文字、坏图、APNG 和无附件。 | 文档提取 11 项、服务 6 项。 | 原图不持久化。 |
| T011 | 文档 JPG/PNG/WebP 接入安全检查、libvips 和 Tesseract。 | 定向与全量测试通过。 | 回滚需恢复旧附件路径并重开 009。 |
| T012 | 验证互联网候选授权、脱敏、AI 整理和失败关闭。 | `ExternalSearchServiceTest` 3 项。 | 失败保留候选供重试。 |
| T013 | 互联网候选保存改为 Fetch 后 AI 多知识事务。 | 服务、E2E 和 YAML 通过。 | 回滚移除 AI 整理会违反新规则。 |
| T014 | 手动录入保持普通创建接口且不依赖 AI。 | 既有手动 E2E 继续通过。 | 与 AI 入口隔离。 |
| T015 | 新增独立 AI 文字入口、授权和错误提示。 | 五模式 E2E 和 AI 文字 E2E 通过。 | 可单独隐藏入口回滚。 |
| T016 | 删除文档图片直传附件，统一调用文档 AI 导入。 | E2E 断言图片上传和普通创建请求均为 0。 | 不得回滚为直存。 |
| T017 | 互联网候选前端改为 AI 整理后收入并处理多知识响应。 | 搜索候选 E2E 通过。 | 可恢复旧按钮但会重开绕过。 |
| T018 | 补齐中英文 AI 文字、互联网来源和图片错误文案。 | lint、typecheck、双语 E2E。 | 文案可独立调整。 |
| T019 | 原型增加第五种采集模式并同步文档图片、互联网语义。 | `node --check` 和 Taste 通过。 | 原型可独立回滚。 |
| T020 | E2E 验证非手动入口不调用普通创建或图片上传。 | app E2E 34 项通过。 | API mock 仍需生产真实验收。 |
| T021 | 新增五档视口 AI 采集 Taste 场景。 | `ai-capture-policy-taste.spec.ts` 1 项通过。 | 仅布局验证，不替代真实 AI。 |
| T022 | AI 文字、文档、截图和网页四条 Taste 场景通过。 | 4 项 Playwright 通过。 | 真实 Tesseract 待生产。 |
| T023 | 后端全量回归。 | `kb-core` 18、`kb-app` 175、worker 1 项通过。 | 本机无 Docker。 |
| T024 | 前端 lint、typecheck、Vitest 和 E2E。 | 47 项 Vitest、34 项 E2E。 | 无生产浏览器证据。 |
| T025 | Spotless、后端包和前端生产构建。 | Maven、Vite 均成功。 | 构建产物不提交。 |
| T026 | Taste 源检查、inspector 和截图人工复核。 | `0 FAIL / 1 WARN`；桌面和移动截图已查看。 | WARN 为既有小字号标签。 |
| T027 | YAML、原型、部署脚本和 diff 检查。 | 全部通过；仅有既有 CRLF 提示。 | 部署未执行。 |
| T028 | 记录验收证据和剩余风险。 | [verification.md](../../artifacts/009/verification.md)、[taste-report.md](../../artifacts/009/taste-report.md)。 | 生产验收仍未完成。 |
| T030 | 新增 `api_idempotency`、请求哈希/响应重放服务，并将 AI 文字、网页、文档、截图、互联网搜索和候选保存接入稳定幂等键；同键异体返回冲突。 | `V0013__api_idempotency.sql`、`IdempotencyService`、五类入口专项测试与全量后端测试通过。 | 真实并发和进程重启恢复仍需 PostgreSQL/Docker 后复验；回滚需同时移除前端稳定键与后端表。 |
| T032 | 文档入口支持 HEIC/HEIF：先用 libvips 规范化，再执行安全检查、Tesseract OCR、脱敏和 AI 整理；浏览器文件选择器同步扩展名与 MIME。 | `DocumentTextExtractorTest` 12 项，其中 HEIC 规范化与 OCR 用例通过；前端 typecheck/build。 | 真实 iPhone HEIC 文件和 libvips 生产环境仍需部署验收。 |

### 未完成

- **T029**：生产真实 AI 文字、文档图片 OCR、互联网候选 AI 整理和 002 计费门禁验收。
- **T031**：真实数据库多知识写入失败回滚仍待 PostgreSQL 环境验收。
- **T033**：多图、公式和复杂表格增强属于 P2。
