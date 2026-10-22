# 实施计划：截图 OCR 与 AI 拆分导入

**功能**：`008-screenshot-ai-import`

**日期**：2026-10-22

**规格**：[spec.md](spec.md)

**上游功能**：[006 网页截图导入](../006-webpage-ai-import/plan.md)、
[007 文档解析导入](../007-document-ai-import/plan.md)

## 摘要

截图入口从“上传图片并保存附件”改为“剪贴板截图 -> 图片安全检查 -> Tesseract OCR ->
文本脱敏 -> DeepSeek 拆分 -> 多知识事务”。原始截图写入请求期临时文件，OCR 后立即删除。
知识来源为 `screenshot`，状态为 `succeeded`，不保留图片附件。

## 技术背景

- 前端：Vue 3 剪贴板 PasteEvent、Multipart、vue-i18n。
- 后端：Spring Boot Multipart、现有 `ImageInspector`、共享 `TesseractOcrService`、
  `AiTextSecretRedactor`、`DeepSeekKnowledgeOrganizer`。
- 存储：复用 PostgreSQL 知识模型；不新增截图附件或数据库表。
- 测试：JUnit/Mockito/MockMvc、Vitest、Playwright。

## 宪法检查

| 门禁 | 结果 | 证据 |
|------|------|------|
| SDD 与中文文档 | 通过 | 008 规格、计划、数据、契约、任务均为中文 |
| 多子智能体分离 | 通过 | 产品冻结语义，开发实现，测试独立验收 |
| 默认中文、完整英文 | 通过 | 同意、识别、失败和完成状态均双语 |
| 禁止爬虫与密钥安全 | 通过 | 仅处理用户剪贴板图片；OCR 前图片安全检查和文本脱敏 |
| Taste 与原型一致 | 通过 | 截图模式沿用授权行和既有深海主题 |
| 生产计费门禁 | 通过 | 未计费开关保持 false |
| 原应用零影响 | 通过 | 仅 `/kb` API 和前端 |
| 任务可追溯 | 通过 | 完成记录包含验证、证据和未提交状态 |

## 架构决策

1. **共享 Tesseract 服务。**
   网页截图和用户截图使用同一 OCR 进程边界、输出排空、超时和中文语言包。

2. **图片安全检查先行。**
   `ImageInspector` 校验 PNG/JPEG、尺寸、像素和明显嵌入字节签名；通过后才写临时文件。
   像素中的可视密钥由 OCR 后文本脱敏处理。

3. **截图只用于 OCR。**
   临时文件在 finally 中删除，不写对象存储，不生成正文图片节点。

4. **复用 AI 整理与事务。**
   `DeepSeekKnowledgeOrganizer` 使用 `sourceLabel=screenshot`；所有知识通过
   `KnowledgeService.createOrganized` 写入。

5. **前端不再调用图片上传。**
   截图模式调用 `/api/v1/screenshot-imports`，不调用 `/rich-text/image-uploads` 或普通知识创建。

6. **生产和隐私门禁。**
   明确同意、文本脱敏、未计费 402 与网页/文档一致。

## 项目结构

```text
backend/kb-app/src/main/java/site/seaunit/zhishiku/screenshotimport/
backend/kb-app/src/main/java/site/seaunit/zhishiku/ocr/TesseractOcrService.java
backend/kb-app/src/test/java/site/seaunit/zhishiku/screenshotimport/
frontend/kb-web/src/views/CaptureView.vue
frontend/kb-web/src/api/client.ts
frontend/kb-web/e2e/app.spec.ts
prototype/app.js
specs/008-screenshot-ai-import/contracts/screenshot-import.openapi.yaml
```

## 接口

- `POST /kb/api/v1/screenshot-imports`，Multipart。
- 必填 `file` 和 `externalAiConsent=true`；可选 `note` ≤20,000 字符。
- 成功 `201 {items: KnowledgeItem[]}`。
- 错误：`413 SCREENSHOT_TOO_LARGE`、`415 SCREENSHOT_UNSUPPORTED`、
  `422 SCREENSHOT_IMAGE_INVALID/NO_TEXT/TOO_LONG/ORGANIZATION_FAILED`、
  `402 AI_BILLING_REQUIRED`、`503 SCREENSHOT_IMPORT_UNAVAILABLE`。

## 验证策略

- PNG/JPEG 安全检查、空图、坏图、无文字、OCR 不可用。
- 多知识、来源 `screenshot`、无附件、脱敏、同意和计费。
- MockMvc 认证、CSRF、Multipart。
- E2E 验证不调用 rich-text 上传和普通知识创建。
- 320/390/430 移动端和 Taste 检查。

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| 截图被当附件保存 | 前端调用独立 API，后端无附件写入；E2E 断言 |
| 图片像素炸弹 | ImageInspector 限 8192 边和 4000 万像素 |
| 图片内秘密 | 原始字节安全检查，失败整次拒绝 |
| OCR 卡住 | 共享进程排空与超时 |
| AI 结果只含 URL | 共享 Organizer 过滤 |
| 未计费外发 | 生产 402 门禁 |
