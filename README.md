# Zhishiku

AI 个人知识库项目，采用规格驱动开发（Spec-Driven Development，SDD）流程。

## 项目状态

当前已完成可运行的 MVP：带一次性图形验证码的多账号登录、邮箱验证码公开注册、
头像账户菜单、个人显示名称编辑与确认退出、富文本知识录入与编辑、正文图片、知识列表、
详情查看和 PostgreSQL 单框搜索。手动录入是唯一直接保存原文的入口；AI 文字入口会先脱敏，
再由 DeepSeek 拆分、摘要、分类和打标签。网页链接入口会用 Playwright 只渲染用户提交的一个公开
页面，通过截图 OCR 读取可见文字，并单独提取页面 DOM 中实际展示的内容图片、SVG 和图表，
再由 DeepSeek 整理成一条或多条富文本知识；整页截图只用于 OCR，不会成为来源图，原始 URL
只作为来源，不会成为正文。TXT/Markdown 与图片
文件入口、剪贴板截图入口、DeepSeek 联网搜索和单页 Fetch 保存已经可用。文档入口中的
JPG、PNG、WebP 会先经过安全检查和 OCR，不直接保存为附件；普通文档通过
Apache Tika 按文件内容解析 PDF、DOCX、DOC、RTF、TXT 和 Markdown，再由 AI 拆分为多条知识；
原始文档不长期保存。剪贴板截图会经过图片安全检查、OCR 和 AI 拆分，创建多条文字知识；
截图本身不长期保存，也不会成为附件。互联网候选只有在 Fetch、OCR、DOM 内容图提取、脱敏和
AI 整理成功后才入库，不会直接保存搜索摘要、整页截图或抓取原文。独立分类标签表、Waffo
点数、正式支付桥接和 P2
文档/网页/截图加固已完成本地实现与自动化；真实收费在 Waffo 测试商户和物理 PostgreSQL
门禁通过前保持关闭。网页、文档和截图 AI 的解析、脱敏、
同意和拆分代码已接入；生产环境默认
`SHIHAI_WEB_IMPORT_ALLOW_UNBILLED_AI=false`，在 002 点数门禁完成前会返回 `AI_BILLING_REQUIRED`，
不会执行未计费的外部 AI 调用。扫描版 PDF 自动 OCR 和超长文档分块属于 P2。

已批准的实现技术栈为 Vue 3 + TypeScript、Java 17 + Spring Boot 3.x、PostgreSQL 18
和 S3 兼容对象存储。当前 MVP 的图片保存在服务器的本地持久目录；Playwright Java 单页
截图 worker 已实现。Waffo 桥接已按 `@waffo/pancake-ts@0.26.0` 收口；S3 适配器仍待完成。

## 核心文档

- [项目协作约定](AGENTS.md)
- [项目宪法](.specify/memory/constitution.md)
- [交互原型](prototype/index.html)
- [功能规格](specs/001-ai-personal-knowledge-base/spec.md)
- [实施计划](specs/001-ai-personal-knowledge-base/plan.md)
- [数据模型](specs/001-ai-personal-knowledge-base/data-model.md)
- [部署共存方案](specs/001-ai-personal-knowledge-base/deployment.md)
- [生产部署验收记录](artifacts/001/deployment-verification.md)
- [验证指南](specs/001-ai-personal-knowledge-base/quickstart.md)
- [OpenAPI 契约](specs/001-ai-personal-knowledge-base/contracts/openapi.yaml)
- [付费功能规格](specs/002-ai-credit-billing/spec.md)
- [付费实施计划](specs/002-ai-credit-billing/plan.md)
- [计费数据模型](specs/002-ai-credit-billing/data-model.md)
- [计费 OpenAPI](specs/002-ai-credit-billing/contracts/openapi.yaml)
- [Waffo 桥接契约](specs/002-ai-credit-billing/contracts/waffo-bridge.yaml)
- [付费验收指南](specs/002-ai-credit-billing/quickstart.md)
- [付费任务表](specs/002-ai-credit-billing/tasks.md)
- [移动端网页规格](specs/003-mobile-web-access/spec.md)
- [移动端实施计划](specs/003-mobile-web-access/plan.md)
- [移动端任务表](specs/003-mobile-web-access/tasks.md)
- [移动端快速验证](specs/003-mobile-web-access/quickstart.md)
- [富文本编辑规格](specs/004-rich-text-editor/spec.md)
- [富文本实施计划](specs/004-rich-text-editor/plan.md)
- [富文本数据模型](specs/004-rich-text-editor/data-model.md)
- [富文本任务表](specs/004-rich-text-editor/tasks.md)
- [账户菜单与个人资料规格](specs/005-account-profile/spec.md)
- [账户资料实施计划](specs/005-account-profile/plan.md)
- [账户资料数据模型](specs/005-account-profile/data-model.md)
- [账户资料任务表](specs/005-account-profile/tasks.md)
- [网页截图 AI 拆分规格](specs/006-webpage-ai-import/spec.md)
- [网页截图 AI 拆分计划](specs/006-webpage-ai-import/plan.md)
- [网页截图 AI 拆分任务表](specs/006-webpage-ai-import/tasks.md)
- [网页截图 AI 拆分验证指南](specs/006-webpage-ai-import/quickstart.md)
- [文档解析 AI 拆分规格](specs/007-document-ai-import/spec.md)
- [文档解析 AI 拆分计划](specs/007-document-ai-import/plan.md)
- [文档解析 AI 拆分任务表](specs/007-document-ai-import/tasks.md)
- [文档解析 AI 拆分验证指南](specs/007-document-ai-import/quickstart.md)
- [截图 OCR AI 拆分规格](specs/008-screenshot-ai-import/spec.md)
- [截图 OCR AI 拆分计划](specs/008-screenshot-ai-import/plan.md)
- [截图 OCR AI 拆分任务表](specs/008-screenshot-ai-import/tasks.md)
- [截图 OCR AI 拆分验证指南](specs/008-screenshot-ai-import/quickstart.md)
- [AI 辅助采集统一规则](specs/009-ai-assisted-capture/spec.md)

## 交互原型

“识海”前端原型位于 [prototype/index.html](prototype/index.html)，可直接打开，也可在仓库根目录运行：

```powershell
python -m http.server 8088 --bind 127.0.0.1 --directory prototype
```

然后访问 `http://127.0.0.1:8088/`。

## 正式应用本地运行

后端使用 Java 17 + Spring Boot，前端使用 Vue 3。开发阶段可以分别启动：

```powershell
cd infra
docker compose -f compose.yml up -d postgres
cd ../backend
./mvnw -pl kb-core install -DskipTests
./mvnw -pl kb-app spring-boot:run "-Dspring-boot.run.profiles=local"
```

另开终端：

```powershell
cd frontend/kb-web
pnpm install --frozen-lockfile
pnpm dev
```

访问 `http://127.0.0.1:4173/kb/`。`local` profile 会使用开发占位账号
`owner@zhishiku.local` 和密码 `local-dev-password-change-me` 初始化一个本地开发账号；生产环境
必须通过环境变量覆盖并保持 `SHIHAI_AUTO_BOOTSTRAP=false`。占位账号只用于开发，不限制其他账号注册。

后端测试：

```powershell
cd backend
./mvnw -pl kb-app -am test
```

前端测试与构建：

```powershell
cd frontend/kb-web
pnpm lint
pnpm typecheck
pnpm test
pnpm build
```

## 开发流程

1. `$speckit-specify`：建立或更新功能规格。
2. `$speckit-clarify`：澄清高影响需求歧义。
3. `$speckit-plan`：生成技术计划、数据模型与接口契约。
4. `$speckit-tasks`：生成可执行任务。
5. `$speckit-implement`：开发实现。
6. 独立执行测试与验收，不要求使用子智能体或并行协作。

本地隔离测试环境可使用独立的 `zhishiku_test` PostgreSQL 数据库、模拟邮件和 Waffo dry-run：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\local-test\start.ps1
powershell -ExecutionPolicy Bypass -File .\deploy\local-test\stop.ps1
```

详细配置和切换测试商户的方法见
[`deploy/local-test/README.md`](deploy/local-test/README.md)。

## 部署约束

- GitHub 是源码、迁移和部署脚本的唯一权威来源。
- 部署必须支持指定 Git tag 或 commit、重复执行、健康检查和失败回滚。
- 知识库部署在现有 `seaunit.site` 的 `/kb` 路径。
- 根路径 `/` 是 SEAUNIT 门户，同时提供拾题和识海入口；拾题原有页面继续使用
  `/app`、`/login`、`/bank` 等路径。
- 不得影响现有应用的 `/api`、`/app`、`/login`、`/bank`、`/pay`、`/apk`、证书或认证配置。
- 所有密钥仅通过服务器环境变量或受管密钥服务提供，不得进入仓库。
- 识海使用独立 `kb_pack_*` 商品、Waffo 桥接、订单、点数账本和
  `/kb/pay/webhooks/waffo`；不得复用拾题 `pack_*`、`8090` 侧车或 `/pay` 路由。
- 测试与生产计费使用独立物理 PostgreSQL 数据库、环境文件和密钥，不能只靠 schema 或
  环境字段隔离。
- AI 付费采用一次性点数制。所有付费 AI 必须经过服务端报价、确认、预留和成功步骤结算；
  自动整理扣点默认关闭，余额不足不得阻止内容保存和本地检索。

## GitHub 部署

仓库公开后，可以从 GitHub 固定 tag、commit 或分支直接安装。首次部署：

```bash
curl -fsSL https://raw.githubusercontent.com/seaunit/zhishiku/main/deploy/scripts/provision-ubuntu.sh \
  | sudo bash -s -- --ref main
```

后续版本更新：

```bash
curl -fsSL https://raw.githubusercontent.com/seaunit/zhishiku/main/deploy/scripts/deploy.sh \
  | sudo bash -s -- --ref main
```

生产发布应把 `main` 替换为经过验收的 tag 或完整 commit SHA。脚本默认：

- 从 `https://github.com/seaunit/zhishiku.git` 拉取源码，不使用本地工作树。
- 使用 Java 17、PostgreSQL 18、Vue 构建和 Nginx 静态文件。
- 服务只监听 `127.0.0.1:8082`，公开入口为 `/kb/`。
- 部署安装 Playwright Chromium 和 Tesseract `chi_sim+eng`，把 worker 放入 release，
  并通过 `PLAYWRIGHT_BROWSERS_PATH=/opt/zhishiku/playwright` 隔离浏览器文件。
- 网页链接只截图、OCR 和 AI 拆分一个公开页面；任何失败都不会创建 URL-only 知识。
- AI 文字、网页、文档、文档图片、截图和互联网候选都只在 AI 整理成功后入库；手动录入保持在
  AI 不可用时仍可保存。
- 文档入口使用内嵌 Apache Tika 解析文档，并对 JPG/PNG/WebP 执行 OCR；所有内容脱敏后由 AI
  整理，原始文件不长期保存，解析失败或超限时不创建部分知识。
- 剪贴板截图只用于临时 OCR，AI 拆分后的文字才入库；截图模式不调用图片附件上传接口。
- 生产网页 AI 必须等待 002 点数报价、预留、结算和释放接入；本地自动化可使用
  `SHIHAI_WEB_IMPORT_ALLOW_UNBILLED_AI=true`，该值不得用于生产绕过计费。
- 静态门户安装到 `/opt/seaunit-portal/current`，根路径 `/` 同时提供拾题和识海入口。
- 保留上一 release；健康检查失败时自动切回。
- 只替换现有 Nginx 配置中精确的 `location = /`，增加独立的 `/seaunit-portal/` 静态资源
  location，并注入受管理的 `/kb` 片段；`nginx -t` 失败时恢复配置备份。
- 不修改拾题进程、MySQL、Redis、支付 sidecar 或 `/app`、`/login`、`/api`、`/bank`、
  `/pay` 路由。

首次执行部署脚本时会通过终端隐藏输入 DeepSeek AI、Search、Fetch API Key 和 SMTP
密码/应用专用密钥。密钥只写入权限为 `600` 的 `/etc/zhishiku/kb.env`，不会进入 Git、
命令行参数或部署日志。每次部署都会重新提示；直接回车保留现有值，输入新值才更新。
首次安装脚本和内部部署共享同一轮提示，不会重复询问。

任何用户都可以在 `/kb/register` 通过 SMTP 邮箱验证码和密码确认创建账号，注册页不要求图形
验证码；同一邮箱两次成功发码至少间隔 60 秒。每个账号
独立隔离知识、附件、搜索、会话、点数和账本，不提供团队共享或跨账号访问。邮箱必须全局唯一；
临时邮箱域名清单随仓库发布，`mailinator.com` 及其子域会被拒绝。

部署脚本预置的初始开发/运维账号和密码保存在服务器的 `/root/zhishiku-initial-password`，
权限为 `600`；它不是唯一账号，也不会关闭公开注册。首次登录后应立即删除该文件。详细的手工
检查和回滚说明见
[部署共存方案](specs/001-ai-personal-knowledge-base/deployment.md)。
