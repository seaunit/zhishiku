# 任务：网页截图 AI 拆分导入

**输入**：`specs/006-webpage-ai-import/`

**前置条件**：[spec.md](spec.md)、[plan.md](plan.md)、[research.md](research.md)、
[data-model.md](data-model.md)、[quickstart.md](quickstart.md)、
[contracts/web-import.openapi.yaml](contracts/web-import.openapi.yaml)

**测试**：测试先行。后端使用 JUnit 5、Mockito、Spring MVC Test，前端使用 Vitest 与
Playwright E2E，部署使用 Node 脚本和 Bash 语法检查。

## 优先级

- **P0**：单页截图、OCR、AI 拆分、多知识入库、正文不为 URL、前端入口和认证 API。
- **P1**：最终 URL 校验、失败语义、原型对齐、部署依赖、真实页面验收和文档。
- **P2**：幂等去重、分类标签持久化、并发队列、生产容量与质量基准。

## 阶段 1：产品与原型

- [x] T001 [P0] 产品角色在 `specs/006-webpage-ai-import/` 中冻结“网页链接必须截图 OCR 后
  AI 拆分，不得 URL-only 入库”的范围、验收标准和 P0/P1/P2 边界，不修改代码
- [x] T002 [P1] 在 `prototype/app.js` 中把网页链接模拟结果改为一个页面生成多条知识，
  移除 URL 收藏文案，并保持单页、无爬虫说明

## 阶段 2：测试与安全边界

- [x] T003 [P0] [P] [US1] 在
  `backend/kb-app/src/test/java/site/seaunit/zhishiku/webimport/WebPageImportServiceTest.java`
  中验证多条知识、原始 URL、最终 URL 传给 AI、正文不为 URL 和失败零写入
- [x] T004 [P0] [P] [US1] 在
  `backend/kb-app/src/test/java/site/seaunit/zhishiku/webimport/DeepSeekKnowledgeOrganizerTest.java`
  中使用本地 HTTP 服务验证 Bearer、模型、JSON 模式和结果解析
- [x] T005 [P0] [P] [US1] 在
  `backend/kb-app/src/test/java/site/seaunit/zhishiku/webimport/WebPageImportControllerTest.java`
  中验证未登录、CSRF 和成功导入
- [x] T006 [P1] [P] [US2] 在 `backend/kb-browser-worker/` 中为 HTTP/HTTPS 单页、
  公开地址、非 HTTP 子请求和最终 URL 元数据增加可执行检查

## 阶段 3：后端实现

- [x] T007 [P0] [US1] 在
  `backend/kb-browser-worker/src/main/java/site/seaunit/zhishiku/browser/WebPageCaptureWorker.java`
  中实现单页 Playwright 截图、公开地址请求过滤和最终 URL 输出
- [x] T008 [P0] [US1] 在
  `backend/kb-app/src/main/java/site/seaunit/zhishiku/webimport/PlaywrightOcrWebPageCaptureService.java`
  中实现 worker 调用、最终 URL 二次校验、Tesseract OCR 和临时文件清理
- [x] T009 [P0] [US1] 在
  `backend/kb-app/src/main/java/site/seaunit/zhishiku/webimport/DeepSeekKnowledgeOrganizer.java`
  中实现 DeepSeek 调用、超时、结构化 JSON 校验、长度限制和重复标题过滤
- [x] T010 [P0] [US1] 在
  `backend/kb-app/src/main/java/site/seaunit/zhishiku/webimport/WebPageImportService.java`
  中实现 AI 多知识到现有 `KnowledgeService` 的原子映射，正文不得是 URL
- [x] T011 [P0] [US1] 在
  `backend/kb-app/src/main/java/site/seaunit/zhishiku/webimport/WebPageImportController.java`
  与 `ApiExceptionHandler.java` 中实现认证、CSRF、错误码和 `201` 响应
- [x] T012 [P0] [US1] 在 `AppProperties.java`、`application.yml` 和 `.env.example` 中配置
  worker、OCR、文本上限、条数上限和 DeepSeek 参数，不写入真实密钥

## 阶段 4：前端与原型对齐

- [x] T013 [P0] [US1] 在 `frontend/kb-web/src/api/client.ts` 和 `types/api.ts` 中接入
  `POST /api/v1/web-imports`，不再调用知识创建 API
- [x] T014 [P0] [US1] 在 `frontend/kb-web/src/views/CaptureView.vue` 中显示执行中状态、
  隐藏网页备注输入、处理分类错误并成功后进入知识库
- [x] T015 [P1] [US1] 在 `frontend/kb-web/src/i18n.ts` 中补齐网页截图拆分、无文字、
  整理失败和服务不可用的中英文文案
- [x] T016 [P1] [US1] 在 `frontend/kb-web/e2e/app.spec.ts` 中验证只调用网页导入 API、
  不调用 `POST /knowledge-items` 并返回知识库

## 阶段 5：部署与文档

- [x] T017 [P1] [P] [US3] 在 `deploy/scripts/provision-ubuntu.sh` 和 `deploy.sh` 中安装
  Tesseract 中文语言包、构建 worker、安装 Chromium 并验证浏览器目录
- [x] T018 [P1] [P] [US3] 在 `deploy/systemd/zhishiku-api.service` 中配置
  `PLAYWRIGHT_BROWSERS_PATH`，保持 worker 无公共端口
- [x] T019 [P1] [P] [US3] 在 `tests/deploy/verify-deploy-scripts.mjs` 中验证 Tesseract、
  `chi_sim`、`eng`、worker、浏览器路径和命令列表
- [x] T020 [P1] [US3] 在 `AGENTS.md`、`README.md`、主规格和部署文档中同步单页截图 OCR、
  AI 拆分、正文非 URL、部署依赖和密钥约束
- [x] T021 [P1] [US3] 在 `specs/006-webpage-ai-import/` 中完成中文规格、计划、研究、
  数据模型、接口契约、快速验证和任务表

## 阶段 6：验证与发布

- [x] T022 [P0] 执行 `pnpm lint`、`pnpm typecheck`、Vitest、生产构建和网页导入 E2E
- [x] T023 [P0] 执行 Maven 定向测试与 worker/应用生产包构建
- [x] T024 [P1] 执行部署脚本自检、Bash 语法检查和 `git diff --check`
- [ ] T025 [P1] 在生产用真实公开页面完成截图 OCR、多条知识、URL-only 次数为 0 和原应用
  零影响验收，记录 release ID 与证据
- [ ] T026 [P2] 增加每账号网页导入频率限制、服务端幂等去重和浏览器并发队列
- [ ] T027 [P2] 把分类和标签从备注文本迁移到独立词表、关系和锁定状态
- [ ] T028 [P2] 建立真实页面 OCR 质量、P95 耗时、内存和重定向安全基准
- [x] T029 [P1] [US2] 在网页请求、服务和前端中加入外部 AI 明确同意、OCR 密钥脱敏和
  安全处理提示
- [x] T030 [P1] [US1] 让网页整理成功的知识使用 `succeeded` 状态，而不是普通创建的
  `not_requested`
- [x] T031 [P1] [US3] 增加未计费 AI 生产门禁，只有本地自动化可显式开启；生产缺少 002
  计费任务时必须返回 `402 AI_BILLING_REQUIRED`

## 依赖关系

- T003–T006 先于对应后端实现。
- T007–T012 是网页导入核心，T013–T016 依赖其 API 契约。
- T017–T021、T029–T031 可与前端实现并行，但生产发布必须等待 T022–T025 和 002 计费接入。
- P2 任务不阻塞本次 P0/P1 MVP。

## Completion Log

- 日期：2026-10-22
- 状态：以下任务已完成；Git commit 均为“未提交”
- 执行角色：产品、开发、测试子智能体

| 任务 | 摘要与任务详情 | 验证与证据 | 风险与回滚 |
|------|----------------|------------|------------|
| T001 | 产品冻结网页链接必须是单页截图、OCR、AI 拆分和多知识入库，明确禁止 URL-only。 | [spec.md](spec.md) 范围、FR、SC 和 P0/P1/P2；产品子智能体独立复核。 | 范围变化需重新打开规格和任务。 |
| T002 | 原型网页提交模拟为一个页面生成两条知识，加入外部 AI 同意和“不爬取、不只保存 URL”说明。 | `node --check prototype/app.js`；[Taste 报告](../../artifacts/006/taste-report.md)。 | 原型可随提交回滚，不影响正式数据。 |
| T003 | 服务测试覆盖多条知识、原始 URL、最终 URL、正文非 URL、同意、计费门禁、URL 错误和失败零写入。 | `WebPageImportServiceTest` 5 项通过。 | 测试与实现同一提交；回滚不涉及数据。 |
| T004 | Organizer 测试覆盖 Bearer、模型、JSON 模式、裸域名/FTP/Markdown URL 过滤和不发送 URL。 | `DeepSeekKnowledgeOrganizerTest` 通过。 | 仅测试，无数据回滚。 |
| T005 | Controller 测试覆盖未登录、CSRF、成功、至少一条知识、来源和明确同意错误。 | `WebPageImportControllerTest` 4 项通过。 | 仅测试，无数据回滚。 |
| T006 | worker 测试覆盖公开 HTTP、私网、`0.0.0.0/8`、CGNAT、链路本地、IPv6 本机和 `file:`。 | `WebPageCaptureWorkerTest` 通过；真实 Chromium 截图冒烟成功。 | DNS 重绑定仍需网络层防护；代码可独立回滚。 |
| T007 | worker 实现 1440×1200 单页截图、最长 12,000 像素、HTTP/HTTPS 公网过滤和最终 URL 元数据。 | worker 构建、地址测试、真实 `example.com` 截图 44,153 字节。 | 回滚 worker jar 与部署脚本即可。 |
| T008 | 捕获服务实现 worker 调用、最终 URL 二次校验、Tesseract CLI、超时、临时清理和并发 stdout 排空。 | `TesseractOcrServiceTest` 大输出 1 秒内失败关闭。 | 本机缺 Tesseract，真实 OCR 待生产验收。 |
| T009 | DeepSeek 实现结构化 JSON、1–20 条、必填摘要/分类/标签、URL-only 过滤和纯文本输出。 | 本地 HTTP Provider 测试通过。 | 外部 AI 受同意与生产计费门禁保护。 |
| T010 | 导入服务实现脱敏 OCR、AI 拆分和同一事务多知识创建，正文只来自整理结果。 | 服务测试与 Maven 编译通过。 | 任一条失败整次回滚；截图和元数据请求结束删除。 |
| T011 | Controller 与异常处理器实现认证、CSRF、`201`、非法 URL、同意、无文字、整理失败、计费和不可用错误。 | MockMvc 4 项通过。 | 错误码变更需同步前端和 OpenAPI。 |
| T012 | 配置加入 worker、OCR、文本/条数上限、AI 参数和未计费开关；仓库无真实密钥。 | YAML 解析通过；Spotless 通过。 | 生产未计费开关固定 false。 |
| T013 | API Client 改调 `/web-imports`，发送 URL 和明确同意，不再拼 URL 正文。 | `pnpm typecheck`、`pnpm lint`。 | 回滚前端 API 即可。 |
| T014 | 网页模式增加执行中、同意控件、禁用提交、错误映射，隐藏不适用的备注框。 | Playwright 32 项 E2E 通过；390px 视觉检查通过。 | 不影响其他三种采集模式。 |
| T015 | 中英文补齐同意、无文字、整理失败、计费不可用和截图入口回退提示。 | i18n 类型检查、E2E 与双语测试通过。 | 文案可独立回滚。 |
| T016 | E2E 验证网页入口只调用导入 API、不调用 `POST /knowledge-items`。 | `e2e/app.spec.ts` 32 项通过。 | 测试只覆盖 mock API，真实 Provider 由后端测试覆盖。 |
| T017 | provision 安装 Tesseract 中文包；deploy 构建 worker、每次校验并安装 Chromium、复制 worker jar。 | 部署脚本自检、Git Bash `bash -n` 通过。 | 安装或构建失败时保留上一 release。 |
| T018 | systemd 固定 `PLAYWRIGHT_BROWSERS_PATH=/opt/zhishiku/playwright`，worker 无公共端口。 | unit 文件已同步；deploy 检查通过。 | 回滚 unit 和上一 release。 |
| T019 | 部署自检覆盖 `tesseract`、`chi_sim`、`eng`、worker、浏览器路径和未计费开关。 | `node tests/deploy/verify-deploy-scripts.mjs` 通过。 | 自检移除会削弱 fail-closed，不得回退。 |
| T020 | AGENTS、README、主规格、主 OpenAPI、部署文档同步单页截图、脱敏、同意、计费和零影响约束。 | `git diff --check`、YAML 解析和链接人工复核。 | 文档回滚不影响运行，但会恢复旧语义风险。 |
| T021 | 006 完成中文规格、计划、研究、数据模型、OpenAPI、快速验证、质量清单和任务表。 | YAML 解析通过；spec 与实现逐项对照。 | 计费与生产验收仍保持未完成。 |
| T022 | 执行前端 lint、typecheck、Vitest、生产构建、32 项 E2E 与三视口 Taste 检查。 | 44 项 Vitest、32 项 E2E、Taste `0 FAIL / 1 WARN`。 | WARN 为既有采集页空区；无横向溢出。 |
| T023 | 执行后端全量测试、定向测试、Spotless、应用和 worker 生产包构建。 | 后端全量 127 项、定向 14 项和 worker 1 项通过；两个 jar 构建成功。 | Testcontainers 因本机无 Docker 未启动。 |
| T024 | 执行部署自检、Bash 语法、YAML、原型语法和 `git diff --check`。 | 所有命令退出码 0。 | 未执行生产部署。 |
| T029 | 请求增加明确外部 AI 同意；OCR 扫描 API Key、Token、密码和私钥；URL 不进入 AI 请求。 | 服务/脱敏 Provider 测试；前端同意 E2E。 | 正则扫描不能识别所有秘密形态，仍需持续扩充语料。 |
| T030 | KnowledgeService 增加内部 `createOrganized`，网页整理成功状态为 `succeeded`。 | `KnowledgeItemEntityTest` 与服务测试通过。 | 普通创建仍保持 `not_requested`。 |
| T031 | 生产默认 `KB_WEB_IMPORT_ALLOW_UNBILLED_AI=false`，未接入 002 时返回 `402 AI_BILLING_REQUIRED`。 | 服务测试与部署自检通过。 | 这是当前生产阻塞；接入 002 后才能开放。 |

### 未完成

- **T025**：生产真实页面、真实 OCR、多条知识、URL-only 为 0 和原应用零影响尚未验收。
- **T026**：每账号限流、服务端幂等去重和浏览器并发队列尚未实现。
- **T027**：分类与标签仍保存在备注文本，独立词表和锁定关系尚未实现。
- **T028**：真实页面 OCR 质量、P95 耗时、内存和重定向安全基准尚未执行。
