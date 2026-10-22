# 任务：截图 OCR 与 AI 拆分导入

## 优先级

- **P0**：剪贴板截图 OCR、AI 多知识拆分、无附件保存。
- **P1**：图片安全、同意、双语、移动端、部署和真实 OCR 验收。
- **P2**：WebP/HEIC、多图、公式和表格增强。

## 阶段 1：共享 OCR 与产品语义

- [x] T001 [P0] 产品角色冻结“截图只用于 OCR，不保存图片附件”的范围和验收标准
- [x] T002 [P0] 抽取 `backend/kb-app/src/main/java/site/seaunit/zhishiku/ocr/TesseractOcrService.java` 并让网页截图复用
- [x] T003 [P1] 在 `prototype/app.js` 中把截图结果改为多条 OCR 知识且不带图片附件

## 阶段 2：后端截图导入

- [x] T004 [P0] [P] [US1] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/screenshotimport/ScreenshotTextExtractorTest.java` 中验证 PNG、坏图、无文字和 OCR 失败
- [x] T005 [P0] [US1] 在 `ScreenshotTextExtractor.java` 中实现图片安全检查、临时文件 OCR、30k 上限和清理
- [x] T006 [P0] [P] [US1] 在 `ScreenshotImportServiceTest.java` 中验证多知识、来源、无附件、脱敏、同意和计费
- [x] T007 [P0] [US1] 在 `ScreenshotImportService.java` 中实现 OCR 文本脱敏、AI 拆分和事务入库
- [x] T008 [P0] [P] [US1] 在 `ScreenshotImportControllerTest.java` 中验证 Multipart、认证、CSRF 和错误
- [x] T009 [P0] [US1] 在 `ScreenshotImportController.java` 和异常处理器中实现
  `POST /api/v1/screenshot-imports`
- [x] T010 [P1] [US2] 复用 `ImageInspector`、`AiTextSecretRedactor` 和未计费生产门禁

## 阶段 3：前端和文档

- [x] T011 [P0] [US1] 在 `frontend/kb-web/src/api/client.ts` 和 `types/api.ts` 中实现截图 Multipart API
- [x] T012 [P0] [US1] 在 `CaptureView.vue` 中增加截图同意、调用 OCR API，并移除截图图片上传
- [x] T013 [P1] [US1] 在 `i18n.ts` 中补齐截图识别、同意、无文字、不安全、超限和失败文案
- [x] T014 [P1] [US1] 在 `frontend/kb-web/e2e/app.spec.ts` 中验证不调用图片上传和普通知识创建
- [x] T015 [P1] 在 `tests/e2e/screenshot-import-taste.spec.ts` 中验证桌面、平板和 320–430 手机
- [x] T016 [P1] 同步 `AGENTS.md`、README、主规格、主 OpenAPI、部署文档和 008 SDD

## 阶段 4：验证

- [x] T017 [P0] 执行后端定向/全量测试、Spotless 和生产构建
- [x] T018 [P0] 执行前端 lint、typecheck、Vitest、构建和 E2E
- [ ] T019 [P1] 在生产用真实剪贴板截图完成 OCR、多条知识、无附件和原应用零影响验收

## 阶段 5：P2

- [ ] T020 [P2] 支持 WebP/HEIC 与多截图合并
- [ ] T021 [P2] 增加公式、表格和低质量截图识别增强

## Completion Log

- 日期：2026-10-22
- 状态：T001–T018 已完成，Git commit 均为“未提交”
- 执行角色：产品、开发、测试子智能体

| 任务 | 摘要与详情 | 验证与证据 | 风险与回滚 |
|------|------------|------------|------------|
| T001 | 冻结截图必须 OCR 拆分、不得保存原图附件的范围。 | [spec.md](spec.md)、[requirements.md](checklists/requirements.md)。 | 范围变化需重新打开规格。 |
| T002 | 抽取共享 Tesseract 服务，网页截图和剪贴板截图复用。 | `TesseractOcrServiceTest`、网页服务测试通过。 | 回滚需同时恢复网页 OCR 调用。 |
| T003 | 原型截图提交改为多条 OCR 知识且带同意，不创建附件。 | `node --check prototype/app.js`。 | 原型可独立回滚。 |
| T004 | 验证 PNG、空图、坏图、无文字、超限、超长、APNG 和 OCR 失败。 | `ScreenshotTextExtractorTest` 8 项通过。 | 测试图片仅内存。 |
| T005 | 实现安全检查、临时 PNG/JPEG、Tesseract OCR、30k 限制和删除。 | 同上；YAML/Spotless 通过。 | 原图不持久化，失败关闭。 |
| T006 | 验证多知识、来源、脱敏、同意、计费、幂等和无附件。 | `ScreenshotImportServiceTest` 6 项通过。 | 失败不创建部分知识。 |
| T007 | 实现 OCR 文本脱敏、AI 拆分和同事务多知识创建。 | 服务测试与全量测试通过。 | 来源固定 `screenshot`。 |
| T008 | 验证 Multipart、认证、CSRF、成功和同意错误。 | `ScreenshotImportControllerTest` 4 项通过。 | 契约变化需同步 OpenAPI。 |
| T009 | 新增 `POST /api/v1/screenshot-imports` 和稳定错误码。 | MockMvc、YAML 解析通过。 | 可回滚 Controller/Handler。 |
| T010 | 复用图片安全检查、文本脱敏和生产未计费门禁。 | 安全/计费测试通过。 | 生产必须等待 002。 |
| T011 | API Client 实现截图 FormData、同意和备注。 | `client.test.ts` 10 项通过。 | 前端可独立回滚。 |
| T012 | 截图预览后显示同意，调用 OCR API，移除图片上传。 | lint/typecheck/E2E 通过。 | 不再创建图片附件。 |
| T013 | 补齐识别、同意、不安全、无文字和失败中英文。 | 双语测试、lint、typecheck 通过。 | 文案可独立回滚。 |
| T014 | E2E 验证不调用图片上传和普通知识创建。 | app E2E 34 项通过。 | API mock，真实 OCR 待生产。 |
| T015 | 五档视口验证粘贴、同意、按钮和底部导航避让。 | `screenshot-import-taste.spec.ts` 通过。 | WARN 为采集页既有空区。 |
| T016 | 同步 AGENTS、README、主规格、OpenAPI、部署和 008 文档。 | YAML、链接和 `git diff --check` 通过。 | 文档回滚会恢复旧截图语义。 |
| T017 | 后端全量测试、Spotless 和构建。 | `kb-app` 175 项、worker 1 项通过。 | 本机无 Docker。 |
| T018 | 前端 lint、typecheck、47 项 Vitest、生产构建和 34 项 E2E。 | 全部通过。 | 真实 Tesseract/DeepSeek 待验收。 |

### 未完成

- **T019**：生产真实剪贴板截图、真实 OCR、多条知识、无附件和原应用零影响尚未验收。
- **T020–T021**：WebP/HEIC、多截图、公式和表格增强属于 P2。
