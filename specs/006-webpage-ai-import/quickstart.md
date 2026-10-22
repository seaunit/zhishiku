# 快速验证：网页截图 AI 拆分导入

## 前置条件

- Java 17、Maven Wrapper、Node.js 20+、pnpm。
- 可运行现有 Spring Boot、PostgreSQL 和 Vue 开发环境。
- `tesseract` 已安装，并能识别 `chi_sim` 和 `eng`。
- 已构建 `kb-browser-worker`，并安装 Playwright Chromium。
- `DEEPSEEK_API_KEY` 已在本地环境变量中配置；不要写入命令、文件或终端历史。

```powershell
tesseract --list-langs
cd backend
./mvnw -pl kb-app,kb-browser-worker -am package -DskipTests
```

可设置本地变量：

```powershell
$env:KB_WEB_IMPORT_ALLOW_UNBILLED_AI = "true"
$env:KB_BROWSER_WORKER_JAR = "$PWD/kb-browser-worker/target/kb-browser-worker-0.1.0-SNAPSHOT.jar"
```

该未计费开关只用于本地开发与自动化。生产环境必须保持
`KB_WEB_IMPORT_ALLOW_UNBILLED_AI=false`，并接入 002 点数门禁。

Chromium 安装：

```powershell
java -jar "$env:KB_BROWSER_WORKER_JAR" --install-browser
```

## 自动化验证

```powershell
cd backend
./mvnw -pl kb-app,kb-browser-worker -am `
  "-Dtest=WebPageImportServiceTest,DeepSeekKnowledgeOrganizerTest,WebPageImportControllerTest,AiTextSecretRedactorTest,TesseractOcrServiceTest,WebPageCaptureWorkerTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test

cd ../frontend/kb-web
pnpm lint
pnpm typecheck
pnpm test
pnpm build
pnpm exec playwright test e2e/app.spec.ts --workers=1

cd ../..
node tests/deploy/verify-deploy-scripts.mjs
```

预期：

- 服务测试证明 AI 的多条结果分别创建知识，正文不是 URL。
- AI 失败时 `knowledgeService.createOrganized` 调用次数为 0。
- OCR 中的常见 API Key、Token、密码和私钥在发送前被替换为 `[REDACTED]`。
- E2E 只调用 `/kb/api/v1/web-imports`，不调用 `POST /knowledge-items`。
- 部署检查发现 Tesseract、中文语言包、worker 或浏览器路径缺失。

## 真实单页验证

1. 启动应用并登录。
2. 进入 `/kb/capture`，选择“网页链接”。
3. 勾选外部 AI 处理同意，并提交一个无需登录、包含两个可区分主题的公开页面。
4. 确认按钮显示处理中状态。
5. 成功后进入知识库，并看到一条或多条正文非 URL 的知识。
6. 打开任一详情，确认来源类型为网页、来源 URL 为用户提交地址、采集时间存在。
7. 在知识库搜索截图中的独有关键词，确认命中整理后的正文。

## 负向验证

- 提交 `http://127.0.0.1/`、`http://10.0.0.1/` 和带用户名密码的 URL，预期 `400`。
- 不勾选外部 AI 同意时按钮不可用；绕过前端调用 API 时预期 `AI_EXTERNAL_CONSENT_REQUIRED`。
- 生产环境未接入 002 点数时，预期 `402 AI_BILLING_REQUIRED` 且 DeepSeek 调用次数为 0。
- 提交无足够可见文字的页面，预期 `WEB_PAGE_NO_TEXT` 且知识数量不增加。
- 临时移除 `DEEPSEEK_API_KEY`，预期 `WEB_IMPORT_UNAVAILABLE` 且不创建 URL-only 知识。
- 模拟 AI 返回空数组或无效 JSON，预期 `WEB_PAGE_ORGANIZATION_FAILED` 且不回滚失败。
- 使用重定向到私网地址的测试站点，确认 worker 阻断请求，后端不接收私网最终 URL。

## 部署环境检查

```bash
command -v tesseract
tesseract --list-langs
command -v chromium
test -f /opt/zhishiku/current/kb-browser-worker.jar
test -d /opt/zhishiku/playwright
systemctl status zhishiku-api --no-pager
curl -fsS https://seaunit.site/kb/api/actuator/health
```

部署后还必须检查：

```text
/
/app
/bank/1
/login
```

任何一个原有入口失败都必须回滚。

## 证据记录

验收记录应包含：

- 执行日期、Git commit、部署 release ID。
- 真实页面 URL 的脱敏形式。
- 截图以及 OCR 都不持久化。
- AI 返回条目数和最终创建条目数。
- URL-only 检查结果。
- 原应用四个入口和 `/kb` 健康检查结果。
- 失败场景的状态码和错误码。
