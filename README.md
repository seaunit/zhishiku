# Zhishiku

AI 个人知识库项目，采用规格驱动开发（Spec-Driven Development，SDD）流程。

## 项目状态

当前处于需求、宪法、实施计划与任务阶段，尚未创建应用代码。功能 001 定义个人知识库，
功能 002 定义 AI 点数计费与 Waffo 支付，功能 003 定义手机和平板网页访问；真实收费在
P0/P1 门禁通过前保持关闭。

已批准的实现技术栈为 Vue 3 + TypeScript、Java 21 + Spring Boot 3.x、PostgreSQL 18
和 S3 兼容对象存储。搜索优先使用 PostgreSQL 派生索引；Waffo 继续使用隔离的 Node 桥接，
单页截图使用 Playwright Java worker。

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
