# 文档解析与 AI 拆分验证记录

**日期**：2026-10-22

**功能**：`007-document-ai-import`

**状态**：本地 P0/P1 自动化验证通过；生产部署和真实业务文档验收待完成

**Git commit**：未提交

## 已通过

| 检查 | 结果 | 证据 |
|------|------|------|
| DOCX 真实解析 | 通过 | `DocumentTextExtractorTest` 使用 Apache POI 生成 DOCX |
| PDF 真实解析 | 通过 | `DocumentTextExtractorTest` 使用 PDFBox 生成 PDF |
| MIME 内容检测 | 通过 | DOCX/PDF 以 `application/octet-stream` 上传仍正确识别 |
| ZIP 拒绝 | 通过 | 返回 `DOCUMENT_UNSUPPORTED` |
| 空文本拒绝 | 通过 | 返回 `DOCUMENT_NO_TEXT` |
| 25 MiB 上限 | 通过 | 读取前返回 `DOCUMENT_TOO_LARGE` |
| 60k 文本上限 | 通过 | 返回 `DOCUMENT_TOO_LONG`，不静默截断 |
| 多知识拆分 | 通过 | `DocumentImportServiceTest` 生成两条 `document` 知识 |
| 密钥脱敏 | 通过 | AI 输入含 `[REDACTED]`，不含测试秘密 |
| 同意和计费门禁 | 通过 | 未同意和未计费均不读取文件 |
| Controller | 通过 | 认证、CSRF、Multipart、成功和同意错误 |
| 后端全量测试 | 143 项通过；worker 1 项通过 | Maven reactor |
| Spotless | 通过 | `.\mvnw.cmd spotless:check` |
| 应用构建 | 通过 | `kb-app.jar` 128,701,782 字节 |
| 前端 lint/typecheck | 通过 | `pnpm lint`、`pnpm typecheck` |
| Vitest | 45 项通过 | `pnpm test` |
| 前端生产构建 | 通过 | `pnpm build` |
| E2E | 32 项通过 | `pnpm exec playwright test e2e/app.spec.ts --workers=1` |
| 文档移动端 | 1 项通过 | `tests/e2e/document-import-taste.spec.ts` |
| YAML 和原型语法 | 通过 | 两个 OpenAPI、application.yml、`node --check` |
| 部署脚本检查 | 通过 | `node tests/deploy/verify-deploy-scripts.mjs` |

## 关键实现证据

- Tika 3.3.3 负责 PDF、DOCX、DOC、RTF、TXT 和 Markdown。
- `commons-lang3` 固定为 3.21.0，修复 Tika/commons-compress 的真实运行冲突。
- 文档解析默认 25 秒超时，超时返回 `DOCUMENT_PARSE_TIMEOUT`。
- 原始文件不写对象存储，不写日志；知识只保存 AI 整理正文。
- 文档多条知识通过 `KnowledgeService.createOrganized` 写入 `succeeded` 状态。
- 前端选择非图片文档后才显示外部 AI 同意控件；图片继续走原附件路径。

## 未完成或外部条件

- 未执行生产部署，因此没有真实业务 PDF/DOCX、生产日志和原应用零影响证据。
- 本机没有真实 DeepSeek 业务 Key，因此未执行真实 Provider 调用；Provider 协议由本地 HTTP
  测试验证，真实 PDF/DOCX 到文档解析器的链路已实际执行。
- 生产 `KB_WEB_IMPORT_ALLOW_UNBILLED_AI=false`，002 点数门禁接入前返回
  `402 AI_BILLING_REQUIRED`，不会执行未计费 AI。
- 扫描版 PDF 自动 OCR、长文档分块和任务恢复属于 P2。
- 本机无 Docker，Testcontainers PostgreSQL 用例未启动；现有仓库全量测试仍成功。
