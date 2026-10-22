# 实施计划：文档解析与 AI 拆分导入

**功能**：`007-document-ai-import`

**日期**：2026-10-22

**规格**：[spec.md](spec.md)

**上游功能**：
[001 个人知识库](../001-ai-personal-knowledge-base/plan.md)、
[002 点数计费](../002-ai-credit-billing/plan.md)、
[006 网页截图导入](../006-webpage-ai-import/plan.md)

## 摘要

文档入口接收用户明确选择的单个文件，由 Spring Boot 内嵌 Apache Tika 按文件内容检测并解析
PDF、DOCX、DOC、RTF、TXT 和 Markdown。后端清洗提取文字、执行秘密脱敏和外部 AI 明确同意
校验，再把文字交给现有 DeepSeek 结构化整理器拆分为 1–20 条知识。所有知识在同一事务中
创建，正文不是文件二进制或文件名。

## 技术背景

**前端**：Vue 3、TypeScript、现有 Multipart API Client、vue-i18n

**后端**：Java 17、Spring Boot 3.x、Spring MVC Multipart、Apache Tika 3.3.3、
PDFBox、Apache POI

**AI**：现有 `DeepSeekKnowledgeOrganizer`，复用网页导入的结构化 JSON、字段和 URL-only 过滤

**存储**：复用 PostgreSQL 知识模型；原始文档不长期保存，本功能不新增数据库表

**测试**：JUnit 5、Mockito、Spring MVC Test、PDFBox/POI 生成真实 PDF/DOCX、
Vitest、Playwright

**目标平台**：Ubuntu 24.04 服务端、桌面与 320–430 CSS 像素移动浏览器

## 宪法检查

| 门禁 | 设计前 | 设计后 | 证据 |
|------|--------|--------|------|
| SDD 与中文文档 | 通过 | 通过 | 规格、计划、研究、数据、契约、任务均为中文 |
| 产品/开发/测试分离 | 通过 | 通过 | 产品冻结范围，开发实现，测试独立验证 |
| 默认中文、完整英文 | 通过 | 通过 | 文件入口、同意、解析和失败状态全部双语 |
| 禁止爬虫与密钥安全 | 通过 | 通过 | 只处理用户上传文件；发送前脱敏；不记录原文 |
| Taste 与原型一致 | 通过 | 通过 | 文档模式沿用授权行和既有采集布局 |
| GitHub 一键部署 | 通过 | 通过 | Maven 构建包含 Tika；无需新增系统服务 |
| 拾题零影响 | 通过 | 通过 | 仅 `/kb` API、独立进程和数据库 |
| 支付完整性 | 通过 | 通过 | 生产未计费开关保持 false，接入 002 前不外发 |
| 任务可追溯 | 通过 | 通过 | 任务完成后逐项记录验证和未提交状态 |

无需违反章程。

## 架构决策

1. **内嵌 Tika 而非外置解析服务。**
   当前单机部署只需 Maven 依赖，无新端口、新进程或新密钥。后续大文件并发再拆分 worker。

2. **格式以内容检测为准。**
   使用 `Tika.detect(TikaInputStream, filename)`；扩展名只用于解析提示，不用于授权。

3. **原始文件仅请求期存在。**
   Multipart 字节在内存中处理，不写对象存储；请求结束由 Spring 释放。知识只保存提取后的
   AI 整理正文。

4. **文本上限 fail closed。**
   `BodyContentHandler(maxTextChars + 1)` 与后端长度检查共同阻止超长输出。当前上限
   60,000 字符，超过时拒绝而不是静默截断。解析在可取消的守护线程中执行，25 秒未完成时
   返回 `DOCUMENT_PARSE_TIMEOUT`。

5. **复用统一 DeepSeek 整理器。**
   `DeepSeekKnowledgeOrganizer` 同时实现网页和文档接口，按 `sourceLabel` 切换提示语义，
   复用结构化 JSON、字段校验和 URL-only 过滤。

6. **复用同意、脱敏和事务。**
   文档请求要求 `externalAiConsent=true`。文字文档经 Tika 提取，JPG/PNG/WebP 经安全检查、
   libvips 规范化和 Tesseract OCR；提取文本或识别文本经 `AiTextSecretRedactor` 处理，
   多知识通过 `KnowledgeService.createOrganized` 在同一外层事务创建。

7. **状态为 succeeded。**
   AI 已完成文档摘要、分类和标签，不得显示“未整理”。

8. **扫描 PDF 延后。**
   P0 只处理带文字层文档；扫描版提示改用截图入口。P2 再接入逐页 OCR 和分块任务。

## 项目结构

```text
backend/kb-app/src/main/java/site/seaunit/zhishiku/documentimport/
backend/kb-app/src/test/java/site/seaunit/zhishiku/documentimport/
backend/kb-app/src/main/java/site/seaunit/zhishiku/webimport/DeepSeekKnowledgeOrganizer.java
backend/kb-app/src/main/java/site/seaunit/zhishiku/webimport/ImportedKnowledgeMapper.java
frontend/kb-web/src/views/CaptureView.vue
frontend/kb-web/src/api/client.ts
frontend/kb-web/src/types/api.ts
frontend/kb-web/e2e/app.spec.ts
prototype/app.js
specs/007-document-ai-import/contracts/document-import.openapi.yaml
```

## 接口与数据

- `POST /kb/api/v1/document-imports`，`multipart/form-data`。
- 必填：`file`、`externalAiConsent=true`。
- 可选：`note`，最多 20,000 字符。
- 成功：`201`，返回 `{items: KnowledgeItem[]}`。
- 错误：`415 DOCUMENT_UNSUPPORTED`、`413 DOCUMENT_TOO_LARGE`、
  `422 DOCUMENT_NO_TEXT/DOCUMENT_TOO_LONG/DOCUMENT_PARSE_FAILED`、
  `402 AI_BILLING_REQUIRED`、`503 DOCUMENT_IMPORT_UNAVAILABLE`。

## 实施顺序

### P0 MVP

1. 增加 Tika/PDFBox/POI 依赖和文件解析器。
2. 增加文档导入 API、同意/计费门禁、脱敏和事务入库。
3. 前端文档模式调用新 API，选择文件后展示同意控件。
4. 解析真实 DOCX 和 PDF 并验证多条知识。

### P1 完整发布

1. 失败关闭、超限、移动端、双语和 OpenAPI。
2. 部署构建、文档和原应用零影响。
3. 真实生产文档验收和回滚。

### P2 后续

1. 长文档分块和任务恢复。
2. 扫描 PDF 逐页 OCR。
3. 原始文档安全对象存储和保留策略（若产品另行批准）。

## 验证策略

- PDFBox 生成真实 PDF、POI 生成真实 DOCX，解析文字并校验 MIME。
- MockMultipartFile 覆盖 ZIP、空文本、超限和备注。
- MockMvc 覆盖认证、CSRF、成功和同意错误。
- E2E 覆盖文档入口、同意按钮、Multipart 请求和进入知识库。
- 全量 Maven、Spotless、前端 lint/typecheck/Vitest/build/Playwright。

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| 压缩炸弹/恶意文档 | Tika 检测、输出上限、25 MiB 输入、异常关闭 |
| 依赖版本冲突 | 固定 Tika 3.3.3，覆盖与 commons-compress 兼容的 commons-lang3 3.21.0 |
| 扫描 PDF 无文字 | `DOCUMENT_NO_TEXT` 并提示截图入口 |
| 大文档内存 | 单请求 25 MiB、文本 60k；P2 分块任务 |
| 未计费外发 | 生产 `KB_WEB_IMPORT_ALLOW_UNBILLED_AI=false` |
| 原文泄露 | 不落对象存储、不写日志，只发送脱敏文本 |
