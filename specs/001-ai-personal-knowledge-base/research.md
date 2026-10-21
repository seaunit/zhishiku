# 阶段 0 研究：AI 个人知识库

**功能**：`001-ai-personal-knowledge-base`

**日期**：2026-10-21

**状态**：已完成；无未解决澄清项

## 决策 1：Vue 3 静态前端与 Spring Boot 模块化核心

**决策**：前端使用 Vue 3、TypeScript 和 Vite；后端使用 Java 17 + Spring Boot 3.x。
Spring Boot API 与短后台任务共享领域代码，通过运行配置区分 Web 与 Worker 角色。
Playwright Java browser worker 独立进程。

**理由**：用户更熟悉 Java/Vue，长期维护成本低于双栈 Node。Vue 构建后是静态文件，生产不
运行 Node Web 服务器；Spring Boot 提供成熟事务、安全、任务调度、可观测性和 PostgreSQL
支持。模块化单体避免微服务复杂度。

**备选方案**：

- Next.js/Node：生态成熟，但不符合当前维护偏好。
- 纯微服务/Kubernetes：当前多账号隔离和 MVP 规模不需要，拒绝。
- 每个角色独立 JVM：资源浪费；MVP 使用同一 JAR 的 API/Worker profile，browser worker
  独立。

## 决策 2：PostgreSQL 为权威存储和 MVP 搜索

**决策**：业务数据、任务状态和搜索投影均保存在 PostgreSQL 18。MVP 启用 `pg_trgm` 和
受控全文检索，不把 Meilisearch 作为必选进程。若冻结的 100,000 条中文基准未达到
95%/2 秒，再启用可重建 Meilisearch 索引。

**理由**：减少一个常驻服务和内存占用，便于个人维护。100,000 条数据对 PostgreSQL 可行，
但中文短查询必须通过基准验证。

**备选方案**：

- Meilisearch 强制依赖：中文相关性强，但增加进程和部署成本，保留为升级路径。
- OpenSearch：运维过重。
- 语义向量搜索：不作为关键词搜索的替代。

## 决策 3：PostgreSQL 派生搜索文档

**决策**：维护 `search_document` 表或同构投影，包含标题、已脱敏正文、备注、分类、标签、
摘要和图片文字。保存修订时同步更新 `search_revision`，异步重建可在同一数据库完成。

**规则**：

- 只索引可显示、可搜索且已脱敏的内容。
- 不索引内部 JSON、存储键、隐藏样式或脚本。
- 搜索命中补全前检查 active/recycled 状态。
- 查询记录匹配字段和有限长度高亮。

## 决策 4：搜索权重与短查询回退

**决策**：权重为 `title > category/tag > summary > body/note > image text`。中文查询
使用 `pg_trgm` GIN 索引与 `similarity`/排名组合；一至两个汉字查询执行有界回退，不得
无限扫描。

**发布门禁**：

- 100,000 条合成知识、500 条性能查询、100 条正确性查询。
- 95% 查询在 2 秒内显示至少 20 条结果。
- 覆盖中文、英文、混合、短查询、标签、正文、零结果和拼写错误。
- 达不到目标时由产品与测试决定是否启用 Meilisearch。

## 决策 5：PostgreSQL 任务表替代 pg-boss

**决策**：使用 `processing_job` 和 `capture_job` 任务表。Worker 通过事务、租约、
`FOR UPDATE SKIP LOCKED`、尝试次数、退避和幂等键领取任务。Spring Scheduler 触发轮询、
过期释放、对账和清理。

**默认重试**：

- AI 分类最多 5 次，指数退避和抖动。
- Search/Fetch/浏览器最多 2 次，仅重试超时、429 和 5xx。
- 安全、策略和永久格式错误不重试。
- 同一 `(item revision, job type)` 使用稳定幂等键。

## 决策 6：Spring Security 多账号认证与隔离

**决策**：使用 Spring Security、Spring Session JDBC 和公开邮箱验证注册。每个邮箱可创建一个
独立账号，已有账号不会关闭注册；同一邮箱重复注册必须被拒绝。

**规则**：

- 密码使用 BCrypt 或 Argon2 强哈希。
- 会话 Cookie 名 `zhishiku_session`，路径 `/kb`，数据库持久化。
- 启用 CSRF、SameSite、安全 Cookie 和会话撤销。
- 所有受保护请求从会话取得账号 ID，并以兼容字段 `owner_id` 校验知识、附件、搜索、会话、
  点数、订单和账本的数据边界；不得接受客户端覆盖账号 ID。
- 知识、附件、上传、搜索、会话、AI 授权、点数、订单、退款和账本均按账号隔离。
- 不提供团队成员、跨账号共享、账号间转赠或公开分享。
- 注册发码不要求图形验证码；密码需要二次确认；邮箱码使用哈希缓存、一次性消费、60 秒冷却、频控和
  临时邮箱域名拒绝。

## 决策 7：Spring `WebClient` Provider 适配器

**决策**：DeepSeek 分类、Search、Fetch 和视觉分析分别定义 Provider 适配器，使用
`WebClient`、显式超时、速率限制、结构化输出验证和授权范围。

**门禁**：

- PoC 验证模型 ID、结构化输出、数据政策、限流和错误格式。
- Fetch 无法保证单 URL 时禁用，不得退化为爬虫。
- Provider 故障不得影响手动录入、本地搜索、回收站和设置。

## 决策 8：Playwright Java 单页采集

**决策**：独立 browser worker 使用 Playwright Java，只渲染用户明确提交的一个 URL。
可加载页面显示所需子资源，但不得发现链接、遍历 sitemap、预取、无限滚动或多页采集。

**安全限制**：

- 仅 HTTP/HTTPS、端口 80/443、最多三次重定向，每次重新校验。
- 拒绝 loopback、私网、链路本地、CGNAT、IPv6 ULA、云元数据和保留地址。
- 出站代理在连接时重新解析 DNS，防止 DNS 重绑定。
- 不保留 Cookie、登录态、下载、Service Worker 或持久浏览器配置。
- 失败时引导用户粘贴剪贴板截图或从文档入口选择图片。

## 决策 9：Java 编排，平台工具处理文件

**决策**：Spring Boot 负责文件状态、配额、事务和审计；Tika/PDFBox 提取文档，Tesseract
执行 OCR，libvips 处理 JPEG/PNG/WebP/HEIC、方向、缩放和元数据清理。

**扫描规则**：

- 文本和提取文本在持久化/外发前扫描。
- 图片 OCR 产生文本和边界框，检测密钥并脱敏派生图。
- 无法确定完整性时整次拒绝并删除暂存制品。
- 只接受已扫描内容进入正式对象存储。

## 决策 10：数据保留

| 数据 | 默认保留 |
|------|----------|
| active 知识 | 至用户删除 |
| 回收站 | 30 天 |
| 每日备份 | 30 天 |
| WAL/PITR | 7 天 |
| 外部候选 | 15 分钟 |
| 审计 | 365 天 |
| AI 授权历史 | 账户存续期间 |

删除顺序：停止任务、标记 purging、删除搜索文档、删除对象、硬删除业务行、写审计。

## 决策 11：Vue UI 与本地化

**决策**：Vue 页面使用安静的深度知识工作台风格，首页只有一个搜索框。路由由 Vue Router
管理，Nginx fallback 到 `/kb/index.html`。

**路由**：

- `/kb/`
- `/kb/library`
- `/kb/library/:id`
- `/kb/capture`
- `/kb/processing`
- `/kb/trash`
- `/kb/settings`
- `/kb/setup`
- `/kb/login`
- `/kb/register`

`vue-i18n` 默认 `zh-CN`，完整提供 `en`。用户输入不自动翻译。Taste 检查覆盖
1440/768/390/360 和移动横屏。

## 决策 12：测试与运维

**决策**：

- 后端 JUnit 5、AssertJ、ArchUnit、Testcontainers、RestAssured、WireMock。
- 前端 Vitest、Vue Test Utils、ESLint、TypeScript 类型检查。
- E2E 使用 Playwright Java 或 CI Playwright，真实浏览器验证。
- 契约使用 Springdoc/OpenAPI、JSON Schema、RestAssured。
- 性能使用 k6 与真实结果卡片渲染。
- 安全使用 Gitleaks、Trivy、OWASP Dependency-Check、Semgrep。
- 日志使用 Logback JSON，指标使用 Micrometer/OpenTelemetry。
- 备份使用 pgBackRest 和 restic。

## 决策 13：性能参考环境

**语料**：100,000 条，70% 中文、20% 英文、10% 混合；正文中位 2 KiB、P95 20 KiB。

**查询**：500 条性能查询、100 条正确性查询；正式并发 2 浏览器 + 2 后台任务；诊断并发
4 个搜索持续 60 秒。

**参考环境**：Linux x86_64、8 vCPU、32 GiB RAM、NVMe，应用限制 4 vCPU/8 GiB。
当前 3.4 GiB 生产主机不能作为完整栈容量证明。

## 决策 14：原生 Nginx/systemd 共存

**决策**：保留拾题原生部署。识海 Vue 静态文件、Spring Boot API、Java browser worker 和
Node Waffo 桥接均使用独立目录、systemd、环境文件、端口、日志、Cookie 和数据库。

**端口建议**：

- 拾题 Spring Boot：`8080`，不修改。
- 拾题支付 sidecar：`8090`，不修改。
- 识海 Spring Boot：预检后使用 `127.0.0.1:8082`。
- 识海 Waffo 桥接：`127.0.0.1:8091`。
- 识海 browser worker：不开放端口。

部署前必须验证 `/`、`/app`、`/api`、`/bank`、`/pay`、`/apk`、证书、认证和支付 sidecar。

## 决策 15：计费与 Waffo

**决策**：AI 计费由 002 的 Spring Boot 领域实现；Waffo 桥接保留 Node。付费 AI 必须先
取得有效 `billingTaskId`。桥接不接触数据库和知识正文，只通过内部契约转发安全支付事件。

**边界**：

- 独立 `kb_pack_*`、测试/生产环境、物理 PostgreSQL 数据库和商家商品。
- `/kb/pay/webhooks/waffo` 只进入识海桥接。
- 点数不足不阻止内容保存。
- 隐私授权与自动扣点授权分离。

## 已解决未知项

- 前端：Vue 3 + TypeScript + Vite。
- 后端：Java 17 + Spring Boot 3.x。
- 数据库：PostgreSQL 18。
- 搜索：PostgreSQL-first，Meilisearch 可选。
- 任务：PostgreSQL 任务表 + Spring Scheduler。
- 认证：Spring Security + Session JDBC。
- 浏览器：Playwright Java 独立 worker。
- 支付：Spring Boot 计费 + Node Waffo 桥接。
- 文件处理：Tika/PDFBox、Tesseract、libvips。
- 部署：Vue 静态文件 + Spring Boot API + systemd/Nginx。
