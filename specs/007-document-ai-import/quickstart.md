# 快速验证：文档解析与 AI 拆分导入

## 自动化验证

```powershell
cd backend
./mvnw -pl kb-app -am `
  "-Dtest=DocumentTextExtractorTest,DocumentImportServiceTest,DocumentImportControllerTest,DeepSeekKnowledgeOrganizerTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test

cd ../frontend/kb-web
pnpm lint
pnpm typecheck
pnpm test
pnpm build
pnpm exec playwright test e2e/app.spec.ts --workers=1
```

预期：

- 真实生成的 DOCX 和 PDF 均能提取文字。
- JPG/PNG/WebP 通过安全检查、libvips 规范化和 OCR，不创建图片附件。
- ZIP 被拒绝为 `DOCUMENT_UNSUPPORTED`。
- 文档结果创建多条 `succeeded` 知识。
- AI 失败时不调用 `createOrganized`。
- 生产未计费时返回 `AI_BILLING_REQUIRED`。

## 本地真实文档验证

1. 设置本地未计费测试开关和 DeepSeek Key。
2. 进入 `/kb/capture`，选择“文档”。
3. 选择一份带文字层的 PDF/DOCX，或一张带文字的 PNG。
4. 勾选外部 AI 处理同意并提交。
5. 确认进入知识库，至少创建一条正文非空的 `document` 知识。
6. 打开详情，确认处理状态为“已整理”。
7. 搜索文档中的独有关键词，确认命中。

## 负向验证

- 上传 ZIP，预期 `415 DOCUMENT_UNSUPPORTED`。
- 上传空文本，预期 `422 DOCUMENT_NO_TEXT`。
- 上传超过 25 MiB 文件，预期 `413 DOCUMENT_TOO_LARGE`。
- 上传没有文字层的扫描 PDF，预期 `DOCUMENT_NO_TEXT` 并提示截图入口。
- 上传坏图或动画图片，预期 `DOCUMENT_IMAGE_INVALID`，不得调用 AI。
- 不勾选同意，预期按钮禁用；直接调用 API 返回 `AI_EXTERNAL_CONSENT_REQUIRED`。

## 生产前置

- `KB_WEB_IMPORT_ALLOW_UNBILLED_AI=false`。
- 002 报价、预留、结算和释放接入完成。
- 真实 PDF/DOCX 解析、原文不落库、日志无正文和原应用零影响验收通过。
