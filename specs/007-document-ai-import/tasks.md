# 任务：文档解析与 AI 拆分导入

**输入**：`specs/007-document-ai-import/`

**前置条件**：[spec.md](spec.md)、[plan.md](plan.md)、[research.md](research.md)、
[data-model.md](data-model.md)、[quickstart.md](quickstart.md)、
[contracts/document-import.openapi.yaml](contracts/document-import.openapi.yaml)

## 优先级

- **P0**：PDF/Word/TXT/Markdown 解析、JPG/PNG/WebP OCR、AI 拆分、多知识事务和前端文件入口。
- **P1**：失败关闭、密钥脱敏、双语、移动端、部署和真实文档验收。
- **P2**：长文档分块、扫描 PDF OCR、原始文档对象存储。

## 阶段 1：规格与共享基础

- [x] T001 [P0] 产品角色在 `specs/007-document-ai-import/` 中冻结文档格式、25 MiB、
  60k 文本上限、原始文件不保留和 AI 拆分范围，不修改代码
- [x] T002 [P0] 在 `backend/pom.xml` 和 `backend/kb-app/pom.xml` 中接入 Tika 3.3.3、
  兼容的 commons-lang3 和真实 PDF/DOCX 解析依赖
- [x] T003 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/webimport/` 中把
  DeepSeek Organizer 泛化为网页和文档共享接口，并抽取共享知识 DTO 映射

## 阶段 2：解析与安全

- [x] T004 [P0] [P] [US1] 在
  `backend/kb-app/src/test/java/site/seaunit/zhishiku/documentimport/DocumentTextExtractorTest.java`
  中生成真实 PDF/DOCX 并验证内容检测、文字提取、ZIP 拒绝和空文本拒绝
- [x] T005 [P0] [US1] 在
  `backend/kb-app/src/main/java/site/seaunit/zhishiku/documentimport/DocumentTextExtractor.java`
  中实现 25 MiB、MIME 检测、Tika 解析、60k 输出上限、无文字和失败关闭
- [x] T006 [P1] [US2] 在文档导入前复用 `AiTextSecretRedactor` 和外部 AI 明确同意，
  生产未计费时返回 `AI_BILLING_REQUIRED`

## 阶段 3：整理与 API

- [x] T007 [P0] [P] [US1] 在
  `backend/kb-app/src/test/java/site/seaunit/zhishiku/documentimport/DocumentImportServiceTest.java`
  中验证多知识、用户备注、来源格式、密钥脱敏、同意和失败零写入
- [x] T008 [P0] [P] [US1] 在
  `backend/kb-app/src/test/java/site/seaunit/zhishiku/documentimport/DocumentImportControllerTest.java`
  中验证认证、CSRF、Multipart、成功响应和同意错误
- [x] T009 [P0] [US1] 在
  `backend/kb-app/src/main/java/site/seaunit/zhishiku/documentimport/DocumentImportService.java`
  中实现解析文本到 `KnowledgeService.createOrganized` 的同一事务多知识创建
- [x] T010 [P0] [US1] 在
  `backend/kb-app/src/main/java/site/seaunit/zhishiku/documentimport/DocumentImportController.java`
  和 `ApiExceptionHandler.java` 中实现 `POST /api/v1/document-imports` 与稳定错误码

## 阶段 4：前端与原型

- [x] T011 [P0] [US1] 在 `frontend/kb-web/src/api/client.ts` 和 `types/api.ts` 中实现
  Multipart 文档导入请求
- [x] T012 [P0] [US1] 在 `frontend/kb-web/src/views/CaptureView.vue` 中让文档和
  JPG/PNG/WebP 图片统一调用解析 API，选择文件后显示外部 AI 同意
- [x] T013 [P1] [US1] 在 `frontend/kb-web/src/i18n.ts` 中补齐 PDF/Word 支持、同意、
  超大、超长、无文字、解析失败和整理失败中英文文案
- [x] T014 [P1] [US1] 在 `frontend/kb-web/e2e/app.spec.ts` 中验证文档 Multipart API、
  同意前置、不调用普通知识创建 API 和进入知识库
- [x] T015 [P1] [US1] 在 `prototype/app.js` 中把文档模拟结果更新为解析后多条知识

## 阶段 5：文档与验收

- [x] T016 [P1] [US3] 在 `AGENTS.md`、`README.md`、主规格、主 OpenAPI 和部署文档中同步
  PDF/Word 解析、Tika 依赖、原文不保留和计费门禁
- [x] T017 [P1] [US3] 在 `specs/007-document-ai-import/` 中完成中文规格、计划、研究、
  数据模型、OpenAPI、快速验证和任务表
- [x] T018 [P0] 执行 Maven 定向/全量测试、Spotless 和应用生产包构建
- [x] T019 [P0] 执行前端 lint、typecheck、Vitest、生产构建和 Playwright E2E
- [x] T020 [P1] 执行 1440/768/430/390/320 真实浏览器视觉检查和文档同意控件移动布局检查
- [ ] T021 [P1] 在生产用真实 PDF、DOCX、扫描件、损坏文件和原应用路由完成验收

## 阶段 6：P2 后续

- [ ] T022 [P2] 在 `backend/.../documentimport/` 中实现长文档分块、任务检查点和幂等恢复
- [x] T023 [P2] 实现扫描 PDF 逐页 OCR、点数预估和中间图片生命周期
- [ ] T024 [P2] 如产品批准保留原件，新增加密对象存储、访问控制和保留期限规格

## 依赖关系

- T002/T003 阻塞 T005–T010。
- T004–T010 完成后才能实现 T011–T014。
- T016/T017 可与前端并行，生产发布必须等待 T018–T021。
- P2 不阻塞本次 P0/P1。

## Completion Log

- 日期：2026-10-22
- 状态：T001–T020、T023 已完成；Git commit 均为“未提交”
- 执行角色：产品、开发、测试子智能体

| 任务 | 摘要与任务详情 | 验证与证据 | 风险与回滚 |
|------|----------------|------------|------------|
| T001 | 冻结 PDF/DOCX/DOC/RTF/TXT/Markdown 范围、25 MiB、60k 文本和无原文保留。 | [spec.md](spec.md)、[checklists/requirements.md](checklists/requirements.md)。 | 范围变化需重开规格。 |
| T002 | 接入 Tika 3.3.3、PDFBox/POI，并覆盖 commons-lang3 3.21.0 运行兼容。 | 真实 PDF/DOCX 测试与生产 jar 构建通过。 | 回滚 POM 和文档导入代码。 |
| T003 | DeepSeek Organizer 泛化为网页/文档共享接口，抽取共享知识映射。 | `DeepSeekKnowledgeOrganizerTest`、网页服务测试通过。 | 共享接口变化需同步两类导入。 |
| T004 | 真实生成 DOCX/PDF，并验证 PNG OCR、MIME、扫描件、ZIP 拒绝、空文本、大小和文本上限。 | `DocumentTextExtractorTest` 11 项通过。 | 测试图片仅内存生成，不落仓库。 |
| T005 | 实现 TikaInputStream 检测、25 秒可取消解析、25 MiB、60k、无文字和失败关闭。 | 同上；YAML 和 Spotless 通过。 | 扫描件仍按 P2 提示截图入口。 |
| T006 | 文档请求要求明确同意、密钥脱敏，生产未计费时拒绝。 | Service 测试验证同意/计费前不读文件。 | 生产必须等待 002。 |
| T007 | 服务测试覆盖多知识、图片无附件、来源格式、用户备注、脱敏、同意和 AI 失败零写入。 | `DocumentImportServiceTest` 6 项通过。 | 失败不产生部分知识。 |
| T008 | Controller 测试覆盖未登录、CSRF、Multipart、成功和同意错误。 | `DocumentImportControllerTest` 4 项通过。 | 契约变化需同步 OpenAPI。 |
| T009 | 文档解析结果映射为多条 `document` 来源、`succeeded` 状态知识。 | 服务测试与全量测试通过。 | 多知识在同一外层事务内创建。 |
| T010 | 新增 `POST /api/v1/document-imports` 和稳定错误码。 | MockMvc 与 YAML 契约通过。 | 回滚 Controller/Handler 不影响旧端点。 |
| T011 | API Client 实现文档 FormData、同意和备注。 | Vitest 11 项 API Client 测试通过。 | 前端可独立回滚。 |
| T012 | 文档与图片统一调用解析和 AI 整理接口，图片不再进入附件上传路径。 | Typecheck、E2E、五视口检查通过。 | 回滚会恢复旧图片直存路径，必须重新打开 009 规格。 |
| T013 | 补齐支持格式、同意、超大、超长、无文字、解析和 Provider 失败中英文。 | lint、typecheck、双语 E2E 通过。 | 文案可独立回滚。 |
| T014 | E2E 验证 Multipart 文档端点、同意前置、不调用普通创建或图片上传 API。 | 34 项 app E2E 通过。 | 真实解析由后端真实文件测试覆盖。 |
| T015 | 原型模拟文档解析后生成两条知识并显示同意。 | `node --check prototype/app.js`、源 lint 通过。 | 原型可独立回滚。 |
| T016 | 同步 AGENTS、README、主规格、主 OpenAPI 和部署文档。 | YAML、`git diff --check` 通过。 | 文档回滚会恢复旧“未接入”描述。 |
| T017 | 完成 007 中文规格、计划、研究、数据、契约、验证和任务。 | spec-kit 文件相互链接；YAML 解析通过。 | 生产门禁仍保留未完成。 |
| T018 | 全量 Maven、Spotless 和生产包构建。 | `kb-core` 18 项、`kb-app` 175 项、worker 1 项通过；后端和 worker jar 构建成功。 | 本机无 Docker，Testcontainers 未启动。 |
| T019 | 前端 lint、typecheck、47 项 Vitest、生产构建和 34 项 E2E。 | 全部通过。 | 无生产部署证据。 |
| T020 | 1440/768/430/390/320 文档选择、长文件名、同意、禁用和布局检查。 | `document-import-taste.spec.ts` 通过；Taste `0 FAIL / 1 WARN`。 | WARN 为既有采集页空区。 |
| T023 | 扫描 PDF 先用 PDFBox 按 200 DPI 逐页渲染，再调用 Tesseract OCR；临时代码页请求结束删除，最多 50 页并受 25 秒、60k 字符和失败关闭约束。HEIC/HEIF 文档图片先经 libvips 规范化再 OCR。 | `DocumentTextExtractorTest` 13 项，其中扫描 PDF 回退和 HEIC 规范化用例通过；后端专项构建成功。 | 50 页上限、真实扫描质量和生产 Tesseract 仍待部署验收；超限按 `DOCUMENT_TOO_LONG` 拒绝。 |

### 未完成

- **T021**：生产真实 PDF/DOCX、扫描件、损坏文件、原应用零影响和回滚尚未验收。
- **T022、T024**：长文档分块和原始文档安全存储属于 P2。
