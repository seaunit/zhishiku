# 快速验证：AI 辅助采集统一规则

## 前置条件

- Java 17、Node.js、pnpm 和仓库依赖已安装。
- 本地测试可使用 Mock OCR 和 Mock AI。
- 若验证真实链路，需要配置 DeepSeek API Key；密钥只能通过环境变量提供。

## 1. 后端定向测试

```powershell
cd backend
.\mvnw.cmd -pl kb-app "-Dtest=DocumentTextExtractorTest,DocumentImportServiceTest,DocumentImportControllerTest,ExternalSearchServiceTest,KnowledgeControllerTest" test
```

预期：

- 文档图片通过安全检查、OCR 和 AI 整理。
- AI 文字入口经脱敏和 AI 整理，不使用普通创建接口。
- 文档图片不创建附件，不调用图片上传接口。
- 互联网候选保存前必须取得 AI 授权。
- 普通创建接口拒绝 `document`、`screenshot`、`internet_result` 来源。
- 失败时没有部分知识写入。

## 2. 前端接口和 E2E

```powershell
cd frontend/kb-web
pnpm lint
pnpm typecheck
pnpm test -- --run
pnpm exec playwright test e2e/app.spec.ts --workers=1
```

预期：

- 文档入口选择 JPG/PNG/WebP 后调用 `document-imports`。
- AI 文字入口调用 `text-imports`。
- 不调用 `rich-text/image-uploads`。
- 不调用普通 `knowledge-items` 创建接口。
- 互联网候选按钮显示 AI 整理语义，并提交 `externalAiConsent=true`。

## 3. 手工验收

1. 手动录入并保存一条富文本知识，确认没有等待 AI。
2. 在文档入口上传带文字的 PNG，确认生成文字知识且详情没有原图附件。
3. 选择 AI 文字入口，粘贴一段文字并授权，确认生成 AI 整理结果。
4. 在文档入口上传 PDF，确认仍生成一条或多条 AI 知识。
5. 粘贴剪贴板截图，确认只调用截图识别入口。
6. 搜索互联网并选择“AI 整理后收入”，确认生成结构化知识而不是搜索摘要。
7. 在 320、390 和 430 像素宽度重复步骤 2、5、6，确认无横向溢出。

## 4. Taste 检查

```powershell
node C:\Users\95611\.codex\skills\taste-skill\scripts\lint.mjs --app frontend/kb-web/src
node C:\Users\95611\.codex\skills\taste-skill\scripts\inspect.mjs --app http://127.0.0.1:4173/kb/capture
```

预期：截图、文档和互联网保存状态在桌面与移动端可用；无新增 `FAIL`。

## 5. 生产验收边界

- 002 点数门禁未完成时，非手动入口必须返回稳定错误，不调用真实 AI。
- 真实 Tesseract、libvips、DeepSeek 和原应用零影响检查只能在部署环境完成。
