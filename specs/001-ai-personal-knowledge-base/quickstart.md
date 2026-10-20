# 第 1 阶段快速入门与验证：AI 个人知识库

**功能**：`001-ai-personal-knowledge-base`

**目的**：在实现完成后对计划中的功能进行端到端验证。本文不是当前的实现报告；仓库中尚未包含该应用。

## 前置条件

当前生产主机未安装 Docker，并且已经在运行原生 systemd 应用。Docker Compose 仅作为本地开发或经容量评估批准的部署选项，不是当前的生产基线。修改 `seaunit.site` 前，请参阅 [deployment.md](deployment.md)。

- 安装了 Compose v2 的 Docker Engine
- Node.js 24 LTS
- pnpm
- 用于 Playwright 的 Chromium 兼容浏览器
- 用于实时提供商测试的可选真实 DeepSeek 凭据

## 计划使用的环境变量

根据已提交的 `.env.example` 创建本地 `.env`。示例文件只包含变量名和安全占位符，不包含真实凭据。

```dotenv
APP_ENV=development
APP_BASE_URL=http://localhost:3000
SESSION_SECRET=replace-me
DATABASE_URL=postgresql://app:replace-me@localhost:5432/zhishiku
MEILI_URL=http://localhost:7700
MEILI_MASTER_KEY=replace-me
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
LOG_LEVEL=info
```

当某个值缺失时，应用必须让功能特定的启动检查失败，并且只显示变量名。应用绝不能打印变量值。

## 启动计划中的技术栈

```powershell
pnpm install
docker compose -f infra/compose.yml up -d
pnpm db:migrate
pnpm dev
```

预期服务：

- Web/API：`http://localhost:3000`
- PostgreSQL：仅限内部网络
- Meilisearch：内部网络或本地诊断端口
- S3/MinIO：内部网络或本地诊断端口
- Worker 和 browser worker：不开放公共端口

## 生产环境共存

当前生产路由计划如下：

```text
https://seaunit.site/       -> portal with 拾题 and 知识库 entries
https://seaunit.site/app    -> existing 拾题 SPA
https://seaunit.site/kb     -> new knowledge base
```

现有的 `/api`、`/bank`、`/pay` 和 `/apk` 路由必须继续由现有服务处理。知识库应用必须使用 `/kb` 和 `/kb/api`，绑定到 `127.0.0.1:3000`，并通过现有 Nginx 代理。当前主机内存有限；安装或启动应用技术栈前，请参阅 [deployment.md](deployment.md)。

## 验证场景

### 1. 引导初始化与访问控制

1. 使用一次性 bootstrap token 打开 `/setup`。
2. 创建唯一的所有者账户。
3. 确认 bootstrap 完成后 `/setup` 关闭。
4. 退出登录，并确认受保护路由拒绝匿名访问。
5. 登录并确认默认界面语言为中文。

### 2. 手动保存与即时搜索

1. 创建一条包含标题、正文、笔记和来源链接的笔记。
2. 确认该项目立即显示在 `/library` 中。
3. 使用唯一搜索框，分别以标题、正文、笔记、分类和标签中出现的值进行搜索。
4. 确认每次查询都返回该项目，并显示匹配字段。
5. 确认资源库、处理中和回收站页面中没有第二个搜索框。

### 3. AI 授权与分类

1. 在未授权的情况下保存一个新的文本项目。
2. 确认该项目在等待分类期间保持可用且可搜索。
3. 为选定的提供商和 `text` 内容类型授予分类权限。
4. 确认该项目获得摘要、分类和至少一个标签。
5. 模拟低置信度，并确认出现临时结果和审核标记，同时不会阻止搜索。
6. 修正分类或标签，并确认该字段变为锁定状态。
7. 运行重新分析，并确认锁定字段不会改变。

### 4. 密钥扫描与脱敏

1. 提交包含合成 canary 凭据的文本。
2. 确认凭据已被移除、其余文本已保存，并且消息中绝不显示匹配到的值。
3. 确认 canary 不会出现在 PostgreSQL、Meilisearch、对象存储、日志、截图或遥测中。
4. 上传包含可检测合成密钥的图片，并确认仅保留脱敏后的派生图片。
5. 模拟无法完整扫描的图片或文档，并确认整个提交被拒绝，且不会留下持久化行或对象。

### 5. 文档与截图导入

1. 上传受支持的文本、PDF 或文字处理文档。
2. 确认在提取完成前，项目已创建且可搜索。
3. 确认处理状态按照文档中定义的状态机变化。
4. 上传带有清晰文字的截图，并确认 OCR、摘要、分类和标签均被填充。
5. 重试模拟的提取失败，并确认不会创建重复项目或附件。

### 6. 单页网页导入

1. 使用本地 public-HTML fixture 或已批准的公开页面。
2. 仅提交一个 URL。
3. 确认 browser worker 只打开该页面，并捕获限定尺寸的截图。
4. 确认不会发生链接发现、sitemap 遍历、无限滚动或多页面任务。
5. 确认重定向受到限制，并拒绝私有 IP、loopback、CGNAT、元数据和凭据 URL。
6. 确认登录、CAPTCHA 或失败情况返回截图上传回退方案，而不是继续爬取。

### 7. 本地优先的互联网搜索

1. 输入一个既有个人结果又有互联网结果的查询。
2. 确认个人结果先渲染。
3. 确认在用户选择“继续互联网搜索”之前，浏览器不会将查询发送给互联网提供商。
4. 确认仅传输查询，不传输知识正文、笔记、分类、附件或授权。
5. 确认外部候选项在视觉上彼此分离，并保持临时状态。
6. 确认保存候选项会创建可搜索项目，并保留其来源。
7. 确认在确认前刷新不会创建知识项目。

### 8. 回收站与清除

1. 删除一个可用项目。
2. 确认它立即从搜索中消失。
3. 确认它显示在 `/trash` 中，并带有可见的清除日期。
4. 恢复该项目，并确认它再次可搜索。
5. 再次删除该项目，使用较短的测试保留期运行 purge job，并确认项目、来源、附件、分析和索引文档均被永久移除。
6. 确认在已披露的备份过期后，旧备份无法恢复该项目。

### 9. 双语行为

1. 确认首次访问时默认使用中文。
2. 切换到英文并访问每个主要路由。
3. 确认导航、错误、安全提示、破坏性操作确认和处理状态均有完整的英文内容。
4. 确认切换语言不会改变用户输入的内容。
5. 确认 AI 输出语言遵循设置或明确的来源语言选择。

### 10. Taste 视觉验证

实现期间使用 `design-taste-frontend` skill，并在以下尺寸检查真实应用：

- 桌面端（Desktop）：1440 px
- 平板端（Tablet）：768 px
- 移动端（Mobile）：390 px

必需检查：

- 搜索是主页最主要的操作，并且是唯一的文本搜索输入框。
- 本地结果在视觉上位于互联网搜索操作之前。
- 加载、空、失败、待处理、临时、锁定、脱敏和已回收状态在布局不发生偏移的情况下仍然可读。
- 两种语言下文字都不会溢出控件。
- 键盘焦点、标签、对话框和破坏性操作确认均可访问。

## 自动化命令

实现创建工作区和脚本后，以下命令将成为发布门禁：

```powershell
pnpm lint
pnpm typecheck
pnpm test
pnpm test:integration
pnpm test:contract
pnpm test:e2e
pnpm test:a11y
pnpm test:security
pnpm test:perf
pnpm build
```

## 性能验证

1. 加载冻结的 100,000 项基准语料库。
2. 确认数据集 manifest 和 hash 与已提交的基准定义一致。
3. 运行 500 次性能查询，以及单独运行的 100 次正确性查询。
4. 使用两个浏览器会话和两个后台任务作为正式并发。
5. 测量从提交搜索到 20 个结果卡片可见所需的时间。
6. 确认在 cold-query 和 warm-cache 条件下，95% 的正式查询均在 2 秒内完成。
7. 运行四个并发搜索持续 60 秒，作为诊断性压力测试；不允许出现 5xx 响应。

## 备份与恢复验证

1. 备份 PostgreSQL、对象存储、分析元数据、授权和删除 tombstone。
2. 从 PostgreSQL 重建 Meilisearch。
3. 恢复到隔离的 Compose 项目。
4. 验证项目、附件、分类、语言和来源的一致性。
5. 验证已回收项目可在其窗口内恢复，并且在已披露的备份过期后不会重新出现。
6. 确认日志和指标不包含用户内容、原始查询、文件名、token 或提供商响应。

## 相关设计产物

- [spec.md](spec.md)
- [plan.md](plan.md)
- [research.md](research.md)
- [data-model.md](data-model.md)
- [deployment.md](deployment.md)
- [contracts/openapi.yaml](contracts/openapi.yaml)
