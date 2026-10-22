# 网页截图 AI 拆分导入验证记录

**日期**：2026-10-22

**功能**：`006-webpage-ai-import`

**状态**：本地 P0/P1 自动化验证通过；生产部署和真实 Tesseract OCR 待验收

**Git commit**：未提交

## 已验证

| 检查 | 结果 | 证据 |
|------|------|------|
| 后端定向测试 | 14 项通过 | 服务、Organizer、Controller、脱敏、worker 地址策略和大输出子进程测试 |
| 后端全量测试 | 127 项通过，0 失败；worker 另 1 项通过 | Maven `kb-core`、`kb-app` 与 worker reactor |
| Java 格式 | Spotless 通过 | `.\mvnw.cmd spotless:check` |
| 应用与 worker 构建 | 两个 Spring Boot 可执行 jar 构建成功 | `kb-app-0.1.0-SNAPSHOT.jar`、`kb-browser-worker-0.1.0-SNAPSHOT.jar` |
| 前端 lint | 通过 | `pnpm lint` |
| 前端类型检查 | 通过 | `pnpm typecheck` |
| Vitest | 44 项通过 | `pnpm test` |
| 前端生产构建 | 通过 | `pnpm build` |
| 前端 E2E | 32 项通过 | `pnpm exec playwright test e2e/app.spec.ts --workers=1` |
| 网页同意移动端检查 | 1 项通过 | `tests/e2e/web-import-taste.spec.ts` |
| 部署脚本检查 | 通过 | `node tests/deploy/verify-deploy-scripts.mjs` |
| 原型语法 | 通过 | `node --check prototype/app.js` |
| YAML 解析 | 通过 | 主 OpenAPI、006 OpenAPI 和 `application.yml` |
| 空白检查 | 通过 | `git diff --check` |

## 真实浏览器截图

使用已构建 worker 对 `https://example.com/` 执行一次单页截图：

- 退出码：`0`
- PNG 大小：44,153 字节
- 最终 URL：`https://example.com/`
- 截图经人工查看，页面内容正常，非空白。
- 截图和最终 URL 文件位于本机临时目录，验证后未加入仓库。

## 未完成或外部条件

- 本机未安装 `tesseract`，因此未在本机执行真实 OCR。生产预检会拒绝缺少 Tesseract、
  `chi_sim` 或 `eng` 的环境。
- 本机没有真实 DeepSeek API Key，未执行外部 AI 调用；本地 HTTP 测试已验证 Bearer、
  模型、JSON 模式和响应解析。
- 未执行生产部署，因此尚未完成真实公开页面、AI 生成多条知识、URL-only 次数为 0、
  原应用零影响和回滚验收。
- 生产网页 AI 默认保持 `KB_WEB_IMPORT_ALLOW_UNBILLED_AI=false`，在 002 点数门禁接入前会返回
  `402 AI_BILLING_REQUIRED`。这是有意设置的发布阻塞，不是功能通过。
- 全量测试中的 Testcontainers PostgreSQL 用例因本机没有可用 Docker 环境未启动；该外部
  条件不代表新增网页导入测试失败。

## 生产验收要求

生产发布后必须补充：

1. release ID 和固定 Git ref。
2. 一个真实公开页面的导入结果。
3. AI 返回条数、最终创建条数和正文非 URL 证据。
4. Tesseract 版本与 `chi_sim+eng` 语言列表。
5. Chromium 安装目录和 worker jar 检查。
6. `/`、`/app`、`/bank/1`、`/login` 和 `/kb/` 检查。
7. 失败时回滚到上一 release 的证据。
