# 第 1 阶段快速入门与验证：AI 个人知识库

**功能**：`001-ai-personal-knowledge-base`

**目的**：验证 Vue 3 + Spring Boot + PostgreSQL 知识库。本文同时记录当前可运行 MVP
的验收方式，以及 AI、外部采集、计费和完整生命周期等后续目标的验收门禁。

## 前置条件

- 当前 MVP：Java 17 LTS、Maven Wrapper、Node.js 20/22 LTS、pnpm 和 PostgreSQL 18。
- 当前 MVP 图片使用 `KB_STORAGE_ROOT` 指定的本地持久目录，不要求 S3 或 MinIO。
- 完整目标另需 S3、Playwright Java、Tesseract、libvips、Tika/PDFBox 和 Waffo 测试桥接；
  这些能力尚未实现，不能作为当前 MVP 的放行依据。

终端 A：

```powershell
cd infra
docker compose -f compose.yml up -d postgres
cd ../backend
./mvnw -pl kb-app spring-boot:run "-Dspring-boot.run.profiles=local"
```

终端 B：

```powershell
cd ../frontend/kb-web
pnpm install --frozen-lockfile
pnpm dev
```

另开终端运行后端测试和前端检查：

```powershell
cd backend
./mvnw -pl kb-app -am test

cd ../frontend/kb-web
pnpm lint
pnpm typecheck
pnpm test
pnpm build
```

预期端点：

- Vue 开发服务器：`http://127.0.0.1:4173/kb/`
- Spring Boot API：`http://127.0.0.1:8082/kb/api/v1`
- Spring Boot 健康：`http://127.0.0.1:8082/kb/api/actuator/health`
- 生产入口：`https://seaunit.site/kb/`
- PostgreSQL、本地附件目录和后续 worker：仅内部访问

## 环境变量

```dotenv
APP_ENV=test
SERVER_PORT=8082
DATABASE_URL=jdbc:postgresql://127.0.0.1:55432/zhishiku
DATABASE_USERNAME=zhishiku
DATABASE_PASSWORD=replace-me
SESSION_COOKIE_SECURE=false
KB_STORAGE_ROOT=.data/uploads
KB_CAPTCHA_TTL_SECONDS=120
KB_CAPTCHA_MAX_ENTRIES=10000
KB_CAPTCHA_EXPOSE_ANSWER=false
KB_AUTO_BOOTSTRAP=true
KB_OWNER_EMAIL=owner@zhishiku.local
KB_OWNER_PASSWORD=local-dev-password-change-me
```

以上仅用于本地开发。生产环境必须使用 `SESSION_COOKIE_SECURE=true`、随机数据库密码，
并在首次初始化后设置 `KB_AUTO_BOOTSTRAP=false`。任何 AI、S3 或 Waffo 密钥只能放在
服务器受限环境文件或受管密钥服务中。

`KB_OWNER_EMAIL` 和 `KB_OWNER_PASSWORD` 是预置开发账号的兼容变量名，不代表实例只能有一个
所有者；生产公开注册不依赖这两个变量。

## 验证场景

### 当前可验证范围

1. 使用本地占位账号或任一已注册账号登录；如生产服务器保留
   `/root/zhishiku-initial-password`，它只是预置账号，不是唯一账号。
2. 创建富文本条目，插入正文图片并保存。
3. 打开详情，确认安全富文本和图片正常显示。
4. 使用首页单一搜索框按标题、正文和标签信息检索。
5. 在 390px 和 360px 宽度检查导航、编辑器和详情无横向溢出。
6. 确认 `/kb/api/actuator/health` 返回 `{"status":"UP"}`。
7. 打开 `/kb/register`，使用图形验证码、SMTP 邮箱验证码和两次一致密码注册账号；再使用另一
   邮箱注册第二个账号，确认已有账号不关闭注册。
8. 使用两个账号分别创建知识，确认列表、详情、搜索、附件和会话互不可见。
9. 使用 `daniel.parkerca61598b@mailinator.com` 请求注册验证码，确认返回临时邮箱拒绝且没有 SMTP 投递。

以下第 3 小节及之后的部分包含尚未实现的完整目标能力；任务表未完成项不得据此宣称已上线。

### 1. 引导、会话与访问控制

1. 打开 `/kb/login`，确认加载一次性图形验证码，并可点击验证码换图。
2. 使用错误验证码登录，确认返回验证码错误、自动换图，且密码认证没有被调用。
3. 使用正确验证码和任一已注册账号登录；确认同一验证码不能再次使用。
4. 确认 Spring Security 会话写入 PostgreSQL，且新邮箱仍可继续注册独立账号。
5. 确认匿名访问 `/kb/` 会跳转到 `/kb/login`。
6. 检查 Cookie 名为 `zhishiku_session`、路径 `/kb`、具备 HttpOnly/SameSite/安全属性。
7. 获取 `/kb/api/v1/auth/csrf`，确认缺少或错误 `X-XSRF-TOKEN` 的写请求返回 403。
8. 退出登录并确认会话撤销。
9. 使用重复邮箱注册，确认返回冲突或安全拒绝；使用新邮箱注册，确认仍可创建独立账号。
10. 交替登录两个账号，确认知识、附件、搜索、点数和账本均按账号隔离。

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
