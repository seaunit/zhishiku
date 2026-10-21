# 第 1 阶段快速入门与验证：AI 个人知识库

**功能**：`001-ai-personal-knowledge-base`

**目的**：实现完成后验证 Vue 3 + Spring Boot + PostgreSQL 知识库的完整闭环。本文是
验收指南，不代表当前应用代码已实现。

## 前置条件

- Java 21 LTS 和 Maven Wrapper。
- Vue 3 构建所需的 Node.js 20/22 LTS 和 pnpm；生产不运行 Node 前端服务器。
- PostgreSQL 18，启用 `pg_trgm`。
- S3 兼容对象存储，本地可使用 MinIO。
- Playwright Java 对应 Chromium。
- Tesseract、libvips 和 Apache Tika/PDFBox 所需系统包。
- Waffo 测试桥接使用独立 Node.js 环境和测试凭据。

```powershell
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,worker
cd ../frontend/kb-web
pnpm install
pnpm dev
```

预期端点：

- Vue 开发服务器：`http://localhost:5173/kb/`
- Spring Boot API：`http://127.0.0.1:8082/kb/api`
- Waffo 桥接：`http://127.0.0.1:8091`
- PostgreSQL、MinIO、browser worker：仅内部访问

## 环境变量

```dotenv
APP_ENV=test
APP_BASE_URL=http://localhost:8082/kb
DATABASE_URL=jdbc:postgresql://localhost:5432/zhishiku
DATABASE_USERNAME=zhishiku
DATABASE_PASSWORD=replace-me
SESSION_COOKIE_NAME=zhishiku_session
S3_ENDPOINT=http://localhost:9000
S3_BUCKET=zhishiku
S3_ACCESS_KEY=replace-me
S3_SECRET_KEY=replace-me
DEEPSEEK_API_KEY=replace-me
DEEPSEEK_SEARCH_API_KEY=replace-me
DEEPSEEK_FETCH_API_KEY=replace-me
TRASH_RETENTION_DAYS=30
BACKUP_RETENTION_DAYS=30
MAX_UPLOAD_BYTES=26214400
BILLING_ENABLED=false
BILLING_BRIDGE_URL=http://127.0.0.1:8091
BILLING_DB_ENV=test
```

Waffo 私钥只进入桥接环境文件。应用启动只报告缺失变量名，不打印变量值。

## 验证场景

### 1. 引导、会话与访问控制

1. 使用一次性 bootstrap token 打开 `/kb/setup`。
2. 创建唯一所有者，确认 Spring Security 会话写入数据库。
3. 确认 `/kb/setup` 关闭，匿名访问 `/kb/library` 被拒绝。
4. 检查 Cookie 名为 `zhishiku_session`、路径 `/kb`、具备 HttpOnly/SameSite/安全属性。
5. 获取 `/kb/api/v1/auth/csrf`，确认缺少或错误 `X-XSRF-TOKEN` 的写请求返回 403。
6. 退出登录并确认会话撤销。

### 2. 手动保存与 PostgreSQL 搜索

1. 创建包含标题、正文、备注和来源链接的条目。
2. 确认条目的 `search_document` 投影在保存事务范围内更新。
3. 分别用标题、正文、备注、分类和标签中的词搜索。
4. 确认个人结果匹配原因和高亮正确，且只有一个搜索输入框。
5. 确认列表/搜索摘要不携带完整 `bodyDocument`。

### 3. 搜索性能

1. 加载冻结的 100,000 条语料。
2. 运行 500 条性能查询和 100 条正确性查询。
3. 覆盖中文、英文、混合、一至两个汉字、标签、正文和零结果。
4. 确认 cold/warm 条件下 95% 查询在 2 秒内显示至少 20 条结果。
5. 若未达标，记录证据并由产品/测试决定是否启用可选 Meilisearch。

### 4. 附件与安全扫描

1. 从文档入口上传 PDF/DOCX、JPG、PNG、WebP 和 HEIC。
2. 确认 Java 调用 Tika/PDFBox、Tesseract 和 libvips 的状态可见。
3. 确认附件先进入暂存对象存储，扫描和脱敏完成后才提升为正式附件。
4. 插入合成密钥 canary，确认文本或图片脱敏结果不泄漏原始值。
5. 模拟扫描不完整，确认整次提交拒绝且暂存对象清理。
6. 截图入口只接受剪贴板图片，不出现文件、拖放、相册或相机控件。

### 5. PostgreSQL 任务与 AI

1. 保存内容后确认 `processing_job` 被事务创建。
2. 同时启动两个 Worker，确认 `SKIP LOCKED` 不会重复领取同一任务。
3. 模拟 Worker 崩溃，确认租约到期后任务可恢复。
4. 授权 DeepSeek 文本处理并请求分类。
5. 确认 AI 请求只使用已脱敏文本，输出通过 JSON Schema。
6. 模拟低置信度，确认暂定结果不阻塞搜索，用户修正后锁定。

### 6. 单页网页采集

1. 使用批准的公开 fixture 或本地允许页面。
2. 确认 Playwright Java worker 只打开一个 URL。
3. 验证重定向上限、只允许 80/443、私网/loopback/CGNAT/元数据拒绝。
4. 确认没有链接发现、sitemap、预取、无限滚动或多页采集。
5. 失败时确认提示粘贴剪贴板截图，而不是改为爬虫。

### 7. 互联网搜索

1. 输入同时存在个人和互联网结果的查询。
2. 确认个人结果先显示。
3. 用户明确点击后才发送查询给 DeepSeek Search/Fetch。
4. 确认不发送知识正文、附件或授权记录。
5. 候选项默认 15 分钟过期，确认保存才创建知识条目。

### 8. 回收站、备份与恢复

1. 删除条目，确认搜索投影立即移除。
2. 恢复后确认条目再次可搜索。
3. 运行短保留期 purge，确认数据库、对象存储和可选索引同步清理。
4. 使用 pgBackRest 恢复 PostgreSQL、restic 恢复对象存储。
5. 确认过期备份不能恢复已清除知识。

### 9. 双语与移动页面

1. 首次访问默认中文，切换英文后检查全部路由。
2. 确认 Vue 文案和服务端错误事实一致。
3. 在 1440、768、390、360、430px 和手机横屏检查布局。
4. 验证软键盘、安全区、44px 触控目标和 200% 字体。
5. 使用 Taste 记录截图和最终检查结果。

### 10. 计费与零影响

1. Waffo 测试回调只命中 `/kb/pay/webhooks/waffo`。
2. Spring Boot 只通过内部契约接收安全事件。
3. 确认桥接不读取知识正文、不连接识海数据库。
4. 记录部署前后 `/`、`/app`、`/api`、`/bank`、`/pay`、`/apk`、证书、认证和 sidecar。
5. 任何原有应用差异都阻止发布。

## 自动化命令

```powershell
cd backend
./mvnw spotless:check
./mvnw test
./mvnw verify -Pintegration
./mvnw verify -Pcontract
./mvnw verify -Psecurity

cd ../frontend/kb-web
pnpm lint
pnpm typecheck
pnpm test
pnpm build
pnpm test:e2e
```

部署门禁还应运行 k6 搜索/计费负载、Trivy、Gitleaks、OWASP Dependency-Check、
Testcontainers 集成测试和部署前后零影响脚本。
