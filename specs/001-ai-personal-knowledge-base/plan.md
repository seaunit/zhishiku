# 实施计划：AI 个人知识库

**分支**：`001-ai-personal-knowledge-base` | **日期**：2026-10-21 | **规格**：[spec.md](spec.md)

**输入**：来自 `specs/001-ai-personal-knowledge-base/spec.md` 的功能规格

## 摘要

构建一个单用户、私有部署的 AI 个人知识库网站。用户可以手动录入，或通过文字、文档、
JPG/PNG 等图片文件、剪贴板截图和单页网页采集知识；系统自动生成摘要、分类和标签，
用户纠正后锁定。
首页只提供一个搜索框，先检索个人知识，再由用户明确触发互联网搜索。

技术路线采用 TypeScript 模块化单体：Next.js Web/API、独立后台 worker、强隔离的
Playwright 单页浏览器进程。PostgreSQL 是唯一权威数据源，Meilisearch 是可重建派生索引，
pg-boss 使用 PostgreSQL 执行异步任务，S3 兼容对象存储保存附件。AI、互联网搜索和单页读取
通过 DeepSeek 适配器接入；Web 请求进程不执行 OCR、截图或长时间 AI 任务。

AI 能力通过独立功能 [002-ai-credit-billing](../002-ai-credit-billing/plan.md) 接入一次性
点数计费：所有付费 AI 调用先取得服务端报价、确认并预留点数，再执行和结算。手动录入、
本地检索和已有知识不依赖计费或 Waffo 服务。识海使用独立 Waffo 桥接，不复用拾题支付
sidecar、商品、订单、余额、密钥或 `/pay` 路由。

## 技术背景

**语言/版本**：Node.js 24 LTS；TypeScript 使用与 Next.js 构建链兼容的最新稳定 5.x

**主要依赖**：Next.js 16 App Router、React 19、Better Auth、Drizzle ORM、
pg-boss、Meilisearch、S3 SDK、Playwright、Sharp、Tesseract OCR、Secretlint、
next-intl、Zod、Pino、OpenTelemetry、Radix UI primitives、Lucide icons、
`@waffo/pancake-ts`（仅隔离桥接进程）

**存储**：PostgreSQL 18 为权威存储；Meilisearch 为派生全文索引；S3 兼容私有对象存储
保存附件和脱敏派生图；不引入 Redis

**测试**：Vitest、fast-check、Testcontainers、Playwright、axe-core、Schemathesis、
k6、Semgrep、Gitleaks、Trivy

**目标平台**：Ubuntu 24.04 x86_64、原生 Nginx 与 systemd；新知识库可在容量允许
时使用 Docker Compose，也可使用独立主机；现代桌面与移动浏览器

**项目类型**：Web 应用，包含 web、后台 worker、隔离的 browser worker 和 Waffo 计费桥接

**性能目标**：100,000 条知识基准下，95% 查询在 2 秒内显示至少 20 条个人结果；
目标并发为 2 个用户会话/标签页加 2 个后台任务

**约束**：单用户；默认中文并支持英文；禁止爬虫、链接遍历和批量抓取；网页只处理
用户明确提交的单页；所有密钥仅来自环境变量；无法完整完成密钥扫描的附件整次拒绝；
AI 外发按服务和内容类型授权；实际前端必须通过 Taste 视觉检查；必须与现有
`https://seaunit.site/app` 应用共存，不修改其 `/api`、`/bank`、`/pay`、`/apk` 和认证边界
；付费 AI 必须遵循 002 的报价、点数批次、预留、幂等、退款和环境隔离规则；支付不可用时
手动知识与本地检索继续可用

**规模/范围**：1 个所有者账户、最多 3 至 5 个设备/会话、100,000 条知识、
约 10 个核心页面、文件上限在实施时冻结

## 章程检查

*门禁：必须在阶段 0 研究前通过。阶段 1 设计后重新检查。*

| 门禁 | 研究前 | 设计后 | 证据 |
|------|--------------|-------------|----------|
| SDD 与已批准的澄清事项 | 通过 | 通过 | 计划源自包含 62 项需求的功能规格和五项已接受的澄清事项 |
| 产品/开发/测试分离 | 通过 | 通过 | 产品评审已完成；实施门禁与独立测试门禁已定义 |
| 默认中文并完整覆盖英文 | 通过 | 通过 | `next-intl` 以 `zh-CN` 为默认语言；要求双语端到端测试与字符串验证 |
| 准确性与可追溯性 | 通过 | 通过 | 来源、采集时间、Provider、分析修订版和安全审计元数据均已建模 |
| Taste 视觉验证 | 通过 | 通过 | UI 实施必须运行 `design-taste-frontend`；1440/768/390 检查是发布门禁 |
| 禁止爬虫与外部数据安全 | 通过 | 通过 | 单页浏览器沙箱、SSRF 出站代理、重定向限制、不进行链接发现 |
| 仅环境变量中的密钥与不泄露 | 通过 | 通过 | Zod 启动校验、密钥扫描器/脱敏器、金丝雀测试、无密钥列或密钥日志 |
| 搜索规模与可度量验收 | 通过 | 通过 | Meilisearch 配合 PostgreSQL 回退方案，并采用冻结的 100,000 条基准 |
| 支付与点数完整性 | 通过 | 通过 | 002 独立桥接、PostgreSQL 账本、原子报价、预留、退款批次和零影响门禁 |

本设计无需违反任何章程约束。

## 架构决策

1. **模块化单体，独立进程。** `web`、`worker` 与 `browser-worker` 共享同一套 TypeScript
   领域代码和 schema。这样既能保持部署在运维上足够轻量，又能隔离不受信任的浏览器渲染。
2. **PostgreSQL 为权威数据源。** Meilisearch 和派生文件可以重建。搜索命中后的数据补全
   始终检查 PostgreSQL 中的当前生命周期状态，确保已回收的条目不会通过陈旧索引泄露。
3. **即时可搜索性。** 每次接受手动录入或 AI 导入时，都在同一事务中创建 `active` 条目
   并写入其基础搜索字段。Meilisearch 采用尽力而为（best-effort）的更新方式；
   PostgreSQL 回退方案覆盖索引待处理或暂时不可用的情况。
4. **不使用 Redis 或外部队列。** pg-boss 将持久化任务存储在 PostgreSQL 中，并支持
   锁定、重试、死信和周期任务。
5. **单页浏览器沙箱。** 用户提交的 URL 会在隔离的 Playwright 进程中仅打开一次。
   它可以加载渲染所需的子资源，但不得发现或跟随链接、枚举路由、无限滚动或批量处理
   URL。所有重定向和 DNS 结果都通过出站代理重新验证。
6. **通过端口接入 DeepSeek。** 分类、Search 与 Fetch 是独立的 Provider 适配器，
   各自具有不同的授权范围以及超时/重试行为。在单 URL 行为和数据政策通过 Provider
   PoC 前，Fetch 保持可选。
7. **密钥安全采用 fail-closed（失败即拒绝）。** 文本、提取后的文档文本、OCR 输出和
   截图像素在持久化或外发前均会扫描。无法扫描的内容会被拒绝；高置信度密钥会从文本中
   移除，或在派生图像中脱敏。
8. **单所有者与封闭注册。** Better Auth 使用数据库会话。一次性 bootstrap token
   创建唯一所有者；此后禁用公开注册。
9. **AI 计费作为独立领域。** `packages/billing` 提供订单、点数批次、报价、预留、结算、
   退款和对账；`apps/billing-bridge` 只持有 Waffo 凭据并处理验签/查询。001 的导入、
   重新分析和外部搜索必须在获得有效计费任务后才能调用付费 AI。

## 项目结构

### 文档（本功能）

```text
specs/001-ai-personal-knowledge-base/
├── plan.md
├── research.md
├── data-model.md
├── deployment.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
└── tasks.md               # 后续由 $speckit-tasks 创建
```

### 源代码（仓库根目录）

```text
apps/
├── web/
│   ├── src/app/
│   ├── src/components/
│   ├── src/messages/
│   └── src/server/
├── worker/
│   └── src/jobs/
├── billing-bridge/
│   └── src/
└── browser-worker/
    └── src/

packages/
├── config/
│   └── src/
├── contracts/
│   └── src/
├── db/
│   ├── migrations/
│   └── src/schema/
├── domain/
│   └── src/
├── billing/
│   └── src/
├── providers/
│   └── src/
└── ui/
    └── src/

tests/
├── billing/
├── contract/
├── e2e/
├── integration/
├── performance/
├── security/
└── unit/

infra/
├── compose.yml
├── nginx/
├── proxy/
├── systemd/
└── backup/
```

**结构决策**：使用 pnpm workspace。Web 应用负责 UI 和短时 HTTP 请求；worker 负责提取、
分类、索引、清理和浏览器渲染。共享包用于避免领域规则重复，同时 browser worker 保持
隔离。计费领域与 Waffo 桥接使用独立进程和文件边界，详情见 002 计划。

## 数据与 API 设计

- 完整的关系模型、约束、索引、删除行为和状态机定义在 [data-model.md](data-model.md)。
- 外部 HTTP 行为定义在 [contracts/openapi.yaml](contracts/openapi.yaml)。
- 计费、订单、点数、退款和 Waffo 通知契约由
  [002 OpenAPI](../002-ai-credit-billing/contracts/openapi.yaml) 与
  [桥接契约](../002-ai-credit-billing/contracts/waffo-bridge.yaml) 定义。
- AI 导入、重新分析和互联网搜索请求必须携带有效计费任务标识；没有报价或
  余额不足时不得启动付费 AI。
- 互联网搜索候选项使用带签名、按用户限定且有效期 15 分钟的短期服务器缓存；在所有者
  确认保存前，绝不创建知识条目。
- 每个条目最多归属一个当前分类。标签分配为多对多关系。用户编辑会产生 `locked`
  的条目级分配；共享词表行不携带按条目区分的锁。
- 搜索查询字段为 `title`、`body`、`note`、`category_names` 和 `tag_names`。不提供
  第二个搜索输入框，也不提供由用户选择的搜索模式。

## 部署共存

- 现有“拾题”应用采用原生 systemd 部署，而非 Docker：Spring Boot 运行于
  `127.0.0.1:8080`，支付 sidecar 运行于 `127.0.0.1:8090`，MySQL、Redis 与 Nginx
  运行于同一主机。
- 保持现有应用及其 `/app`、`/api`、`/bank`、`/pay`、`/apk` 和认证路由不变。
- 使用 `basePath=/kb` 在 `/kb` 发布知识库；将其 Web 进程绑定到 `127.0.0.1:3000`，
  并通过现有 Nginx 代理。
- Waffo 桥接绑定 `127.0.0.1:8091`；识海支付通知使用
  `/kb/pay/webhooks/waffo`，与拾题 `/pay/webhooks/**` 完全分离。
- 仅将精确匹配的根落地路由替换为包含 `/app` 与 `/kb` 入口的门户；保留公开的 `/bank`
  与 sitemap 路由。
- 经检查的主机约有 1.8 GiB RAM 可用。若没有额外内存/swap 和资源验证，不建议在该主机
  上运行完整知识库技术栈。
- `/opt/shuati/deploy/nginx/` 下的两个 Nginx 源模板必须与当前生效的服务器配置一同更新。

完整的部署基线、路由设计、资源限制、回滚和密钥规则记录在
[deployment.md](deployment.md)。

## 实施顺序

1. **Provider、搜索与支付 PoC：** 验证 DeepSeek Search/Fetch 约束、Waffo 测试商品与
   通知、Meilisearch 在 100,000
   条目下的中文相关性、截图成功率和 OCR/脱敏完整性。
2. **基础工程：** pnpm workspace、Docker Compose、环境变量 schema、PostgreSQL 迁移、
   Better Auth bootstrap、国际化外壳、结构化日志。
3. **计费基础：** 独立 Waffo 桥接、订单、点数批次、账本、报价、预留、自动扣点授权、
   退款和对账；在 P0 门禁通过前真实收费保持关闭。
4. **核心采集：** 手动 CRUD、密钥扫描/脱敏、对象暂存、导入任务、处理状态、回收站、
   乐观并发。
5. **AI 分类：** Provider 适配器、授权矩阵、计费门禁、结构化输出 schema、
   provisional/applied/locked 分配、重试与幂等性。
6. **搜索：** Meilisearch 索引、PostgreSQL 回退、陈旧索引数据补全保护、单框查询、
   外部搜索交接、候选项确认事务。
7. **Browser worker：** Playwright 单页采集、出站代理、私有网络拒绝、仅截图回退、
   采集元数据。
   - 用户截图入口只监听剪贴板图片粘贴，不提供文件选择；文档入口同时接收文档和图片文件。
8. **UI：** 使用 Taste 技能实施，然后完成 1440、768 和 390 像素下的真实浏览器检查。
9. **加固：** 契约/安全/性能/可访问性/双语测试套件、支付/退款/对账演练、备份与恢复、
   删除到期验证和拾题零影响回归。

## 验证策略

- 单元测试：状态机、锁优先级、幂等性、密钥模式、版本冲突、保留期计算和 Provider
  响应验证。
- 计费测试：订单与支付事件状态机、点数批次消费、报价上限、并发预留、结算/释放、
  退款与预留竞态、主动对账、环境隔离和 402 余额不足路径。
- 集成测试：使用 Testcontainers 验证 PostgreSQL、pg-boss、Meilisearch 和对象存储。
- 契约测试：OpenAPI 与 Provider 适配器 schema、错误、超时、429 和格式错误的输出。
- 端到端测试：手动保存、所有导入模式、候选项确认、回收站、语言切换、授权和即时
  可搜索性。
- 安全测试：跨 DOM、网络、数据库、索引、日志、截图和制品的金丝雀密钥；
  SSRF/私有 IP/凭据 URL 测试；认证和访问控制测试。
- 性能测试：使用 [research.md](research.md) 中的参数，对冻结的 100,000 条语料集和
  500 条查询进行基准测试，并通过 Playwright 的可见结果渲染进行测量。
- 视觉测试：使用 Taste 检查器，并在 1440、768 和 390 像素下审查截图。
- 零影响测试：支付桥接、Nginx 精确回调、部署和回滚前后比较拾题全部原路由与支付侧车。

## 风险与缓解措施

| 风险 | 缓解措施 |
|------|------------|
| Meilisearch 中文相关性未达到验收标准 | 在实施完整 UI 前运行固定基准；Typesense 与 PostgreSQL 回退方案是已评估的备选方案 |
| DeepSeek Fetch 无法保证单 URL 行为 | 将 Fetch 置于适配器和功能开关之后；若 Provider PoC 失败则禁用它；使用 Search 加用户粘贴的截图 |
| 图像密钥脱敏不可靠 | 仅在 OCR/敏感区域置信度足够时脱敏；否则拒绝并删除临时制品 |
| 索引与数据库不一致 | 数据补全时使用 PostgreSQL 生命周期保护、带修订版的幂等索引任务，并通过别名切换完成全量重建 |
| 浏览器采集导致 SSRF 或意外爬取 | 隔离的浏览器出站代理、连接时 DNS/IP 验证、重定向上限、不进行链接发现、不保留持久会话 |
| AI 输出提示注入 | 将所有导入内容视为数据、禁用工具、使用严格 JSON schema、验证输出，并绝不执行模型建议的操作 |
| 保留窗口被误解 | 设置可配置默认值，显示准确的清除日期和备份暴露日期，并使用假时钟和短保留期沙箱测试到期行为 |
| 付费 AI 绕过扣点或静默消费 | 统一 `billingTaskId` 门禁、原子报价、自动扣点独立授权、402 余额不足和成功步骤结算测试 |
| 支付通知重复、乱序或漏失 | 独立 Waffo 桥接、原始体验签、事件唯一键、主动查询对账和冲突人工复核 |
| 退款误扣其他订单点数 | 来源批次追踪、体验点优先、购买点最早批次优先、退款冻结和 24 个月账本 |
| 支付配置影响拾题 | 独立端口 8091、`/kb` 回调、独立环境/数据库/Cookie，并在部署与回滚前后执行零影响回归 |

## 复杂度跟踪

本设计无需违反任何章程约束。独立的 browser worker 和派生搜索索引由禁止爬虫的安全边界
以及 100,000 条目搜索目标提供合理性；独立 Waffo 桥接由支付凭据隔离、原始体验签和
拾题零影响要求提供合理性。只有当对应 PoC 证明风险可以更低成本地关闭时，才会重新评估
这些进程边界。
