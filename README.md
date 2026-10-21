# Zhishiku

AI 个人知识库项目，采用规格驱动开发（Spec-Driven Development，SDD）流程。

## 项目状态

当前已完成可运行的 MVP：单所有者登录、富文本知识录入与编辑、正文图片、知识列表、
详情查看和 PostgreSQL 单框搜索。AI 自动分类、网页采集、OCR、外部搜索、冲突恢复和
Waffo 点数计费仍在后续任务中，真实收费在 P0/P1 门禁通过前保持关闭。

已批准的实现技术栈为 Vue 3 + TypeScript、Java 17 + Spring Boot 3.x、PostgreSQL 18
和 S3 兼容对象存储。当前 MVP 的图片保存在服务器的本地持久目录；S3 适配器、Waffo
桥接和 Playwright Java worker 尚未实现。

## 核心文档

- [项目协作约定](AGENTS.md)
- [项目宪法](.specify/memory/constitution.md)
- [交互原型](prototype/index.html)
- [功能规格](specs/001-ai-personal-knowledge-base/spec.md)
- [实施计划](specs/001-ai-personal-knowledge-base/plan.md)
- [数据模型](specs/001-ai-personal-knowledge-base/data-model.md)
- [部署共存方案](specs/001-ai-personal-knowledge-base/deployment.md)
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
`owner@zhishiku.local` 和密码 `local-dev-password-change-me` 初始化唯一所有者；生产环境必须通过
环境变量覆盖并保持 `KB_AUTO_BOOTSTRAP=false`。

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
6. 测试子智能体独立执行测试与验收。

## 部署约束

- GitHub 是源码、迁移和部署脚本的唯一权威来源。
- 部署必须支持指定 Git tag 或 commit、重复执行、健康检查和失败回滚。
- 知识库部署在现有 `seaunit.site` 的 `/kb` 路径。
- 不得影响现有应用的 `/api`、`/app`、`/bank`、`/pay`、`/apk`、证书或认证配置。
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
- 保留上一 release；健康检查失败时自动切回。
- 只向现有 Nginx 配置注入受管理的 `/kb` 片段，并在 `nginx -t` 失败时恢复备份。
- 不修改拾题进程、MySQL、Redis、支付 sidecar 或 `/app`、`/api`、`/pay` 路由。

首次部署生成的初始账号和密码保存在服务器的 `/root/zhishiku-initial-password`，权限为 `600`。
登录成功后应立即删除该文件。详细的手工检查和回滚说明见
[部署共存方案](specs/001-ai-personal-knowledge-base/deployment.md)。
