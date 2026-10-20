# 任务：识海 AI 点数计费与 Waffo 支付

**输入**：设计文档位于 `specs/002-ai-credit-billing/`

**前置条件**：[plan.md](plan.md)、[spec.md](spec.md)、[research.md](research.md)、
[data-model.md](data-model.md)、[contracts/openapi.yaml](contracts/openapi.yaml)、
[contracts/waffo-bridge.yaml](contracts/waffo-bridge.yaml)、[quickstart.md](quickstart.md)、
[deployment.md](deployment.md)

**测试**：已纳入。支付、点数、退款和零影响属于高风险财务边界，每个用户故事必须先建立
失败测试或契约夹具，再由测试子智能体独立验收。

**组织方式**：按用户故事分组，并标注实施优先级。

## 优先级

- **P0 — 付费门禁与测试支付闭环（MVP 最小安全实现）**：目录、订单、测试 Waffo、点数
  批次、原子报价、自动分类的预留/结算/释放、自动扣点授权、余额不足不丢内容、并发幂等
  和零影响基础门禁。P0 期间真实收费保持关闭。
- **P1 — 公开收费就绪**：生产 Waffo 环境、退款复核、完整主动对账、财务汇总、完整双语、
  网页截图分析、文档提取、OCR、互联网搜索和 AI 回答计费门禁、Taste 验证、GitHub
  一键部署和正式发布门禁。
- **P2 — 规模与运营加固**：恢复演练、负载、长期事件、单位经济性复测和可用性研究。

P0 是 MVP；P0 通过后仍不得开放真实收费。P1 全部通过并经产品批准商业参数后，才允许在
生产环境启用 `BILLING_ENABLED=true`。

US3 在规格中标记为 P2，但其“余额不足不丢内容”属于 P0 财务安全底线，因此相关实现和
测试任务提升为 P0；用户故事优先级本身不改变。

### P0 完成定义

- 001 P0 中实际启用的自动分类不能绕过报价、预留与结算；其余五类付费 AI 在对应 P1
  入口实现时同样不得绕过门禁。
- 首次体验点来源、消费顺序、任务结算、失败释放和并发不透支均可独立验证。
- Waffo 测试订单可完成创建、验签通知、主动查询和到账；返回页不会直接加点。
- 自动整理扣点默认关闭，单次/每日上限生效，余额不足时内容仍可保存。
- 测试事件不会进入生产账本，日志和证据不含密钥、签名或完整支付载荷。
- 拾题 `/`、`/app`、`/api`、`/bank`、`/pay`、`/apk`、认证和支付 sidecar 基线不变。

## 格式：`[ID] [Priority] [P?] [Story] 描述`

- **[Priority]**：`[P0]`、`[P1]` 或 `[P2]`。
- **[P]**：先决条件完成后可并行，且不修改同一文件。
- **[Story]**：仅用户故事阶段使用 `[US1]` 至 `[US5]`。
- 每个任务必须包含至少一个明确的仓库相对文件路径。

## 阶段 0：测试支付与契约夹具

**目的**：在实现账务前固定 Waffo 测试事件、金额、状态和错误契约。

- [ ] T001 [P0] 在 `tests/fixtures/billing/catalog.json` 中固定 `kb_pack_9`、`kb_pack_29`、`kb_pack_99` 的金额、点数、环境和目录版本夹具
- [ ] T002 [P0] [P] 在 `tests/fixtures/billing/waffo-events/` 中准备脱敏的支付成功、失败、关闭、退款、重复、乱序、金额不符和环境不符事件
- [ ] T003 [P0] [P] 在 `tests/contract/billing/waffo-bridge.test.ts` 中先行为 [waffo-bridge.yaml](contracts/waffo-bridge.yaml) 编写收银台、查询、健康与验签错误契约测试
- [ ] T004 [P0] [P] 在 `tests/contract/billing/openapi.test.ts` 中先行为 [openapi.yaml](contracts/openapi.yaml) 编写目录、订单、预检、报价、账户、退款、管理员与内部事件契约测试
- [ ] T005 [P0] [P] 在 `tests/poc/waffo-test-account.test.ts` 中验证 Waffo 测试商户、独立店铺、三个识海商品、收银台和主动查询可用
- [ ] T006 [P0] 在 `specs/002-ai-credit-billing/research.md` 和 `plan.md` 中记录测试账户 PoC 结果、字段差异、限制和不可自动化项

**检查点**：Waffo 商品或事件字段未验证时，不得冻结账务迁移或开放测试支付。

---

## 阶段 1：环境搭建

**目的**：建立计费领域、桥接进程、环境模板、部署模板和测试基础设施。

- [ ] T007 [P0] 在 `packages/billing/package.json`、`packages/billing/tsconfig.json`、`apps/billing-bridge/package.json` 和 `apps/billing-bridge/tsconfig.json` 中创建计费领域与桥接工作区
- [ ] T008 [P0] [P] 在 `packages/config/src/billing-env.ts` 和 `packages/config/src/billing-env.test.ts` 中定义 Web/Worker 与桥接分离的环境变量校验，包括 `BILLING_DB_ENV` 与 `APP_ENV`、`WAFFO_ENV`、物理数据库身份一致，并确保错误只报告变量名
- [ ] T009 [P0] [P] 在 `.env.billing.example` 和 `specs/002-ai-credit-billing/quickstart.md` 中同步不含真实值的 Web、Worker、退款客服和 Waffo 桥接变量模板
- [ ] T010 [P0] [P] 在 `packages/contracts/src/billing.ts` 和 `scripts/check-billing-openapi.ts` 中生成计费 API 与桥接契约类型并添加漂移检查
- [ ] T011 [P0] [P] 在 `infra/systemd/zhishiku-billing-bridge.service` 和 `infra/nginx/zhishiku-billing.conf` 中创建端口 8091、精确回调、无缓存和不影响 `/pay` 的模板
- [ ] T012 [P0] [P] 在 `tests/integration/billing/containers.ts` 中扩展 Testcontainers，支持 PostgreSQL、pg-boss 和桥接 mock
- [ ] T013 [P0] [P] 在 `apps/billing-bridge/src/env.ts`、`packages/config/src/billing-env.ts` 和 `apps/billing-bridge/src/health.ts` 中实现桥接环境标识、退款客服 URL、配置完整性、dry-run 和安全健康状态
- [ ] T014 [P0] [P] 在 `apps/web/src/messages/zh-CN.json` 和 `en.json` 中预留价格、余额、报价、支付、退款、异常和余额不足消息键
- [ ] T015 [P0] [P] 在 `tests/fixtures/billing/factories.ts` 中为订单、支付事件、点数批次、账户、报价、预留和退款创建测试工厂
- [ ] T016 [P1] [P] 在 `artifacts/billing/cost-model.md` 中建立三档套餐的 90 天可变成本和毛利测算模板，引用真实测试账单但不记录凭据
- [ ] T017 [P0] 在 `.github/workflows/billing-ci.yml` 中将计费契约、单元、集成、安全和迁移测试接入质量门禁

**检查点**：两个运行时的环境变量、端口、契约和测试基础设施彼此隔离。

---

## 阶段 2：计费基础能力

**目的**：构建所有付费故事依赖的数据库、状态机、事务、桥接验签、对账和错误映射。

**关键**：本阶段未通过集成测试前，不得将 001 的 AI 调用接入计费。

- [ ] T018 [P0] 在 `packages/db/src/schema/billing-catalog.ts` 中创建计费目录版本、点数包和 AI 点率模式，包含环境、独立 `kb_pack_*` SKU、金额整数、点率和版本唯一约束；桥接商品映射为唯一权威，数据库不保存可漂移的第二份映射
- [ ] T019 [P0] 在 `packages/db/src/schema/billing-orders.ts` 中创建支付订单、收银台会话和支付事件模式，包含待支付套餐部分唯一约束、`source_event_key` 环境去重、退款 `provider_refund_id` 唯一约束和订单对账扫描字段
- [ ] T020 [P0] 在 `packages/db/src/schema/credit-ledger.ts` 中创建点数账户、批次、账本、AI 计费任务、报价行、预留和批次分配模式，账本使用 `available_delta`、`reserved_delta`、`consumed_delta`，并包含非负余额、结算不超上限和账户/批次唯一业务引用
- [ ] T021 [P0] 在 `packages/db/src/schema/billing-governance.ts` 中创建自动扣点授权、每日额度、退款案例、退款回收、对账尝试和计费异常模式
- [ ] T022 [P0] 在 `packages/db/migrations/0002_billing.sql` 中生成 002 全部表、枚举、检查约束、部分唯一索引、扫描索引、最小种子目录，并增加 `processing_job.billing_task_id`、`blocked_billing` 状态、外键和索引
- [ ] T023 [P0] [P] 在 `tests/integration/billing/migrations.test.ts` 中验证全新迁移、001 到 002 向前迁移、`processing_job` 字段/状态回填、重复迁移、物理数据库环境身份和计费表不随知识条目删除级联
- [ ] T024 [P0] [P] 在 `packages/billing/src/errors.ts` 中定义 `INSUFFICIENT_POINTS`、`QUOTE_EXPIRED`、`PAYMENT_MISMATCH`、`ENVIRONMENT_MISMATCH`、`RESERVATION_EXPIRED` 和 `IDEMPOTENCY_CONFLICT` 安全错误
- [ ] T025 [P0] [P] 在 `packages/billing/src/state-machines.ts` 中实现订单、AI 任务、预留、批次和退款状态转换，并拒绝跳级和终态回退
- [ ] T026 [P0] [P] 在 `packages/billing/src/repositories/account-repository.ts` 中实现账户与批次的锁定、投影更新和按体验点/最早购买点排序查询
- [ ] T027 [P0] [P] 在 `packages/billing/src/repositories/ledger-repository.ts` 中实现只追加账本、可用/冻结/已消费三列变化、批次与账户级唯一业务键、余额重算和冲突检测
- [ ] T028 [P0] [P] 在 `packages/billing/src/repositories/order-repository.ts` 中实现订单快照、状态条件更新、幂等查询和有效收银台复用
- [ ] T029 [P0] [P] 在 `packages/billing/src/repositories/task-repository.ts` 中实现报价、确认、预留、步骤结算、释放和过期查询
- [ ] T030 [P0] 在 `packages/billing/src/services/credit-service.ts` 中实现 PostgreSQL 事务内的原子预留、结算、释放、余额重算和幂等封装
- [ ] T031 [P0] 在 `packages/billing/src/services/idempotency-service.ts` 和 `tests/integration/billing/idempotency.test.ts` 中复用 001 幂等键并添加不同请求哈希冲突与响应重放测试
- [ ] T032 [P0] 在 `apps/web/src/server/api/billing-error-handler.ts` 中将点数不足映射为 `402`、报价过期映射为 `409`、支付不可用映射为 `503`
- [ ] T033 [P0] 在 `apps/billing-bridge/src/internal-auth.ts` 和 `tests/security/billing/internal-auth.test.ts` 中实现 Web 到桥接、桥接到 Web 的两套独立内部密钥和拒绝日志
- [ ] T034 [P0] 在 `apps/billing-bridge/src/waffo-client.ts` 中封装 SDK 初始化、商品映射解析、收银台创建和规范化状态查询，不记录私钥、签名或原始载荷
- [ ] T035 [P0] 在 `apps/billing-bridge/src/signature.ts` 和 `tests/contract/billing/signature.test.ts` 中实现原始请求体验签、时间窗口、缺失签名、伪造签名和稳定 `sourceEventKey` 测试
- [ ] T036 [P0] 在 `apps/billing-bridge/src/event-normalizer.ts` 中把通知和查询转换为同一安全化事件结构，包含 sourceEventKey、providerProductId 和 productMatch，剔除银行卡、签名和原始交易载荷
- [ ] T037 [P0] 在 `apps/web/src/app/api/internal/billing/waffo-events/route.ts` 中实现内部事件入口、来源认证、快速校验和统一订单仓库调用
- [ ] T038 [P0] 在 `packages/billing/src/services/payment-event-service.ts` 中实现 sourceEventKey 去重、订单匹配、桥接商品映射与订单 productId 快照核验、金额/币种/环境核验和唯一支付到账事务
- [ ] T039 [P0] 在 `packages/billing/src/repositories/reconciliation-repository.ts`、`apps/worker/src/queues/billing.ts` 和 `packages/contracts/src/billing.ts` 中定义订单 `next_check_at`/尝试次数投影、退避字段、队列契约和可恢复处理接口
- [ ] T040 [P0] 在 `apps/worker/src/jobs/billing/expire-reservations.ts` 和 `expire-quotes.ts` 中实现 30 分钟报价/预留到期释放和迟到结果忽略
- [ ] T041 [P0] 在 `packages/config/src/logger.ts` 和 `tests/security/billing/log-redaction.test.ts` 中为支付、订单、金额、点数和退款日志添加字段白名单与金丝雀测试
- [ ] T042 [P0] 在 `apps/web/src/app/health/route.ts` 中扩展计费健康，区分支付桥接不可用与免费知识库不可用
- [ ] T043 [P0] [P] 在 `tests/integration/billing/foundation.test.ts` 中验证迁移、目录、环境失败关闭、订单仓库、账本事务、桥接 mock 和对账队列

**检查点**：账务基础通过后，购买与扣费故事才可开始。

---

## 阶段 3：用户故事 1 — 购买点数并确认到账

**目标**：通过测试 Waffo 完成订单创建、收银台、可靠通知/查询、一次性到账和订单历史。

**独立测试**：在测试环境购买最低档，验证返回页不直接加点、重复通知只到账一次、主动对账可恢复漏通知。

### 测试

- [ ] T044 [P0] [P] [US1] 在 `tests/unit/billing/order-state.test.ts` 中覆盖待支付、确认中、已支付、关闭、退款中、已退款、异常、迟到支付和终态不可回退
- [ ] T045 [P0] [P] [US1] 在 `tests/integration/billing/checkout-reuse.test.ts` 中覆盖同一 SKU 快照跨目录版本/新旧幂等键并发购买仍只有一笔待支付订单、有效会话复用、过期关闭、迟到支付原子关闭新待支付订单、下架/错误环境商品拒绝和订单快照一致性
- [ ] T046 [P0] [P] [US1] 在 `tests/integration/billing/payment-webhook.test.ts` 中覆盖签名、金额、币种、商品、订单、环境、重复、乱序、重放、重复事件 HTTP 成功确认，以及卡号/CVC/完整支付凭据不进入 API、事件或数据库
- [ ] T047 [P0] [P] [US1] 在 `tests/integration/billing/payment-query.test.ts` 中覆盖通知缺失、查询到账、关闭后迟到支付和冲突状态
- [ ] T048 [P0] [P] [US1] 在 `tests/e2e/billing/purchase.spec.ts` 中覆盖价格页、最终含税应付金额、收银台前条款确认、禁止订阅/自动续费、首次恰好 100 点体验额度、测试收银台返回、确认中、已支付余额和订单全部字段

### 实施

- [ ] T049 [P0] [US1] 在 `packages/billing/src/catalog.ts` 和 `packages/db/seeds/billing-catalog.ts` 中实现三个识海 SKU、三档金额与点数以及版本化目录读取
- [ ] T050 [P0] [US1] 在 `apps/web/src/app/api/v1/billing/orders/route.ts` 中实现订单列表和创建/恢复待支付订单
- [ ] T051 [P0] [US1] 在 `apps/web/src/app/api/v1/billing/orders/[orderId]/route.ts` 中实现所有者隔离订单查询和只读安全状态
- [ ] T052 [P0] [US1] 在 `apps/web/src/app/api/v1/billing/orders/[orderId]/checkout/route.ts` 中实现收银台创建、复用、过期和桥接失败降级
- [ ] T053 [P0] [US1] 在 `apps/billing-bridge/src/router.ts` 和 `apps/billing-bridge/src/checkout-service.ts` 中实现内部鉴权收银台端点，按 orderId 幂等创建/恢复会话，响应丢失后可查询恢复，并拒绝未知商品或错误金额
- [ ] T054 [P0] [US1] 在 `apps/billing-bridge/src/webhook.ts` 中实现原始通知接收、验签、规范化、转发和可重试错误
- [ ] T055 [P0] [US1] 在 `apps/billing-bridge/src/order-query-service.ts` 中实现按会话查询 Waffo 状态并返回规范化结果
- [ ] T056 [P0] [US1] 在 `packages/billing/src/services/order-service.ts` 中实现 SKU 快照、单 PENDING 订单、单活跃收银台、关闭，以及迟到支付确认时原子关闭同 SKU 新待支付订单
- [ ] T057 [P0] [US1] 在 `packages/billing/src/services/credit-grant-service.ts` 中实现订单唯一购买批次与首次 100 点体验批次，包含首次判定、重复启用不重复赠送、账户投影和账本追加的同一事务
- [ ] T058 [P0] [US1] 在 `packages/billing/src/services/checkout-service.ts` 中实现本地订单状态与 Waffo 收银台的创建/复用协调、按 SKU 单活跃订单锁、orderId 幂等、会话过期和桥接失败降级
- [ ] T059 [P0] [US1] 在 `apps/web/src/app/api/v1/billing/catalog/route.ts` 中实现当前环境套餐、点率、退款客服 URL、答复时限和退款摘要查询
- [ ] T060 [P0] [US1] 在 `apps/web/src/app/pricing/page.tsx` 和 `apps/web/src/components/billing/point-pack-card.tsx` 中构建三档价格、点数、一次性属性、退款说明和购买入口
- [ ] T061 [P0] [US1] 在 `apps/web/src/app/billing/return/page.tsx` 和 `apps/web/src/components/billing/order-status.tsx` 中构建返回页“确认中”、已支付、关闭和桥接不可用状态
- [ ] T062 [P0] [US1] 在 `apps/web/src/app/billing/orders/page.tsx` 中构建订单号、套餐、金额、点数、状态和时间历史
- [ ] T063 [P0] [US1] 在 `apps/web/src/app/api/v1/billing/account/route.ts` 和 `apps/web/src/components/billing/credit-balance.tsx` 中展示可用、预留、体验点和购买点
- [ ] T064 [P1] [US1] 在 `tests/e2e/billing/taste-pricing.spec.ts` 中为价格和订单页面应用 `design-taste-frontend`，保存 1440/768/390 截图与修复证据

**检查点**：测试购买已可完成，但真实收费保持关闭，等待 P1 发布门禁。

---

## 阶段 4：用户故事 2 — 按 AI 使用准确扣点

**目标**：六类 AI 能力只能通过原子报价、确认、预留和成功步骤结算执行。

**独立测试**：分别执行网页、文档、截图、搜索、回答和自动分类，验证免费/扣费边界、并发不透支和失败释放。

### 测试

- [ ] T065 [P0] [P] [US2] 在 `tests/unit/billing/quote-calculation.test.ts` 中覆盖 5/10/10/10/3/2 点率、文档 10,000 字符边界、网页视图、组合任务总额、未来点率版本只影响新任务以及购买点无过期时间
- [ ] T066 [P0] [P] [US2] 在 `tests/unit/billing/result-success.test.ts` 中覆盖空输出、低置信度但非空暂定结果、协议错误、超时、合法回答、搜索零结果扣 3 点、搜索失败不扣点、回答无依据但正常展示只扣 2 点
- [ ] T067 [P0] [P] [US2] 在 `tests/integration/billing/atomic-reservation.test.ts` 中覆盖余额等于、低于和高于报价，全有或全无预留，以及演练批次顺序
- [ ] T068 [P0] [P] [US2] 在 `tests/integration/billing/concurrent-settlement.test.ts` 中覆盖 100 组并发预留/结算、同一任务多 worker、同一幂等键重放和账本只追加且不可静默修改
- [ ] T069 [P0] [P] [US2] 在 `tests/integration/billing/auto-charge.test.ts` 中覆盖默认关闭、单次上限、每日上限、余额不足和关闭授权
- [ ] T070 [P0] [P] [US2] 在 `tests/integration/billing/document-preflight.test.ts` 中覆盖字符未知、页数/文件大小上限、preflightId 所有者/过期校验、超限重新确认和网页视图增加
- [ ] T071 [P1] [P] [US2] 在 `tests/e2e/billing/ai-charge.spec.ts` 中分别覆盖网页、文档、OCR、搜索、回答和自动分类的报价/确认/执行/结算/失败释放，并断言成功扣费 100% 展示功能、数量、变化和余额

### 实施

- [ ] T072 [P0] [US2] 在 `packages/billing/src/quote-service.ts` 中实现服务端点率读取、数量校验、组合报价、取整和有效期
- [ ] T073 [P0] [US2] 在 `packages/billing/src/services/task-billing-service.ts` 中实现报价确认、全量余额检查、原子预留和返回计费任务
- [ ] T074 [P0] [US2] 在 `packages/billing/src/services/settlement-service.ts` 中实现步骤成功结算、失败释放、实际额不超上限、终态幂等和迟到结果忽略
- [ ] T075 [P0] [US2] 在 `packages/billing/src/services/batch-allocator.ts` 中实现体验点优先、购买点最早批次优先、退款冻结批次不参与分配
- [ ] T076 [P0] [US2] 在 `apps/web/src/app/api/v1/billing/preflights/route.ts` 和 `apps/web/src/app/api/v1/billing/quotes/route.ts` 中实现免费预检与报价端点，并拒绝客户端金额、点率和未知功能
- [ ] T077 [P0] [US2] 在 `apps/web/src/app/api/v1/billing/quotes/[quoteId]/confirm/route.ts` 中实现确认、`expectedMaxPoints` 校验和 `402` 余额不足
- [ ] T078 [P0] [US2] 在 `apps/web/src/app/api/v1/billing/tasks/[taskId]/route.ts` 和 `cancel/route.ts` 中实现任务详情、取消和预留释放
- [ ] T079 [P0] [US2] 在 `apps/web/src/app/api/v1/billing/ledger/route.ts` 中实现点数明细分页和安全描述
- [ ] T080 [P0] [US2] 在 `apps/web/src/app/api/v1/billing/auto-charge/route.ts` 中实现带 version 的自动扣点授权读取、乐观并发更新和启用时授权文本版本校验
- [ ] T081 [P0] [US2] 在 `packages/billing/src/services/auto-charge-service.ts` 中实现授权、单次上限、每日额度行锁、保存前扣点提示和服务端校验
- [ ] T082 [P0] [US2] 在 `apps/web/src/app/settings/billing/page.tsx` 和 `apps/web/src/components/billing/auto-charge-policy.tsx` 中构建扣点开关、单次/每日上限和隐私授权区分说明
- [ ] T083 [P0] [US2] 在 `apps/web/src/components/billing/quote-confirmation.tsx` 中构建分项费用、总上限、余额、预计剩余、有效期和确认操作
- [ ] T084 [P0] [US2] 在 `apps/web/src/components/billing/ledger-table.tsx` 和 `apps/web/src/app/billing/page.tsx` 中构建余额、预留、来源批次和明细历史
- [ ] T085 [P0] [US2] 在 `apps/web/src/server/billing/ai-gate.ts` 和 `packages/domain/src/ai/authorization-service.ts` 中把计费任务与 001 的外部 AI 隐私授权校验串联
- [ ] T086 [P1] [US2] 在 `apps/web/src/components/import/import-panel.tsx` 和 `apps/web/src/app/capture/page.tsx` 中接入网页、文档和截图费用预估与保存前确认
- [ ] T087 [P1] [US2] 在 `apps/web/src/app/api/v1/imports/text/route.ts`、`webpage/route.ts` 和 `upload/route.ts` 中保证内容先保存，并在没有有效 `billingTaskId` 或余额不足时跳过付费 AI、返回待整理状态而不是 402
- [ ] T088 [P0] [US2] 在 `apps/web/src/app/api/v1/knowledge-items/[itemId]/reanalyze/route.ts` 中要求新报价和新幂等键，不默认复用历史扣费
- [ ] T089 [P1] [US2] 在 `apps/web/src/app/api/v1/search/external/route.ts` 中实现“搜索 3 点”和“搜索 + 回答 5 点”的可选组合报价，不把搜索和回答合并成模糊费用
- [ ] T090 [P0] [US2] 在 `apps/worker/src/jobs/classify-item.ts` 中接入自动分类步骤开始、成功、失败、取消和过期结算回调
- [ ] T091 [P0] [US2] 在 `apps/worker/src/jobs/billing/settle-partial-task.ts` 中实现部分步骤成功只扣成功项并释放差额
- [ ] T092 [P0] [US2] 在 `apps/worker/src/jobs/billing/expire-reservations.ts` 中实现重启恢复、过期释放和迟到结果不扣费
- [ ] T093 [P0] [US2] 在 `apps/web/src/app/settings/billing/page.tsx` 中显示用户所有批次来源、余额、自动扣点授权和退款入口

**检查点**：所有付费 AI 入口均受报价与余额门禁保护，且免费手动路径仍可用。

---

## 阶段 5：用户故事 3 — 余额不足时保留内容

**目标**：余额不足或支付不可用时，原输入和知识内容不丢失，本地能力继续可用。

**独立测试**：在 0、14 和刚好 15 点余额下发起组合任务，验证提示、保留内容、充值和返回确认。

### 测试

- [ ] T094 [P0] [P] [US3] 在 `tests/e2e/billing/insufficient-points.spec.ts` 中覆盖文字、网页 URL、文档和截图保留、余额不变和充值后返回
- [ ] T095 [P0] [P] [US3] 在 `tests/e2e/billing/offline-free-path.spec.ts` 中覆盖支付桥接不可用时的登录、手动保存、本地搜索、已有知识读取、浏览、编辑、删除、恢复、标签校正、导出和设置修改不扣点
- [ ] T096 [P0] [P] [US3] 在 `tests/integration/billing/partial-availability.test.ts` 中证明余额不足不会启动任何付费子步骤

### 实施

- [ ] T097 [P0] [US3] 在 `apps/web/src/components/billing/insufficient-points.tsx` 中构建所需、可用、缺少点数和价格页入口
- [ ] T098 [P0] [US3] 在 `apps/web/src/components/import/import-panel.tsx` 中保留未执行的 URL、文字、文档和截图状态，充值返回后恢复
- [ ] T099 [P0] [US3] 在 `apps/worker/src/jobs/process-import.ts` 中把余额不足或自动扣点关闭转换为待整理状态，不执行 AI 且不产生失败知识丢失
- [ ] T100 [P0] [US3] 在 `apps/web/src/app/library/page.tsx` 和 `processing/page.tsx` 中显示待整理、缺少点数和充值后继续处理
- [ ] T101 [P0] [US3] 在 `apps/web/src/app/api/v1/billing/catalog/route.ts` 和 `pricing/page.tsx` 中保持桥接不可用时仍可查看价格与退款说明，但禁用购买并说明原因

**检查点**：支付或 AI 不可用绝不降低手动知识和本地搜索的可用性。

---

## 阶段 6：用户故事 4 — 支付异常、退款与对账

**目标**：通知、查询、退款和人工复核均可追踪，批次回收不产生负余额。

**独立测试**：模拟全额退款、部分消费、重复退款、部分退款、通知丢失、查询冲突和退款先于支付。

### 测试

- [ ] T102 [P1] [P] [US4] 在 `tests/integration/billing/refund-recovery.test.ts` 中覆盖未消费、部分消费、全部消费、其他批次不受影响和重复退款
- [ ] T103 [P1] [P] [US4] 在 `tests/integration/billing/refund-reservation-race.test.ts` 中覆盖退款冻结批次、未开始任务释放、运行中分配进入 refund_hold、任务终态后继续回收和余额非负
- [ ] T104 [P1] [P] [US4] 在 `tests/integration/billing/refund-order-conflict.test.ts` 中覆盖退款先于支付、支付与退款乱序、查询冲突和重复到账/回收
- [ ] T105 [P1] [P] [US4] 在 `tests/integration/billing/reconciliation-retry.test.ts` 中覆盖通知缺失、查询不可用、退避、恢复、至少 20 笔可查询订单的 95%/10 分钟统计口径、财务汇总、管理员越权拒绝和 24 个月保留
- [ ] T106 [P1] [P] [US4] 在 `tests/e2e/billing/refund.spec.ts` 中覆盖用户退款状态、客服申请入口、识海不会自动发起退款、3 个工作日复核期限、部分回收和最终结论

### 实施

- [ ] T107 [P1] [US4] 在 `packages/billing/src/services/refund-service.ts` 中实现 providerRefundId 幂等、原订单批次冻结、未开始任务取消、运行任务等待结算、后续释放点数继续回收和最终余额非负
- [ ] T108 [P1] [US4] 在 `packages/billing/src/services/refund-review-service.ts` 中实现部分退款、重复退款、已消费点退款和冲突异常的人工复核
- [ ] T109 [P1] [US4] 在 `apps/web/src/app/api/v1/billing/refunds/route.ts` 和 `[refundId]/route.ts` 中实现退款列表、详情、waiting_task_settlement 状态、期限和用户可见结论
- [ ] T110 [P1] [US4] 在 `apps/web/src/app/billing/refunds/page.tsx` 中构建退款处理中、已回收、部分回收、驳回和客服入口
- [ ] T111 [P1] [US4] 在 `packages/billing/src/services/payment-conflict-service.ts` 中实现支付通知与查询冲突归类、已支付事实不可回滚和人工异常
- [ ] T112 [P1] [US4] 在 `apps/worker/src/jobs/billing/reconcile-orders.ts` 中实现待支付/确认中订单扫描、1 分钟起退避和可恢复查询
- [ ] T113 [P1] [US4] 在 `apps/worker/src/jobs/billing/retry-payment-events.ts` 中实现事件处理失败重试、死信和人工复核
- [ ] T114 [P1] [US4] 在 `packages/billing/src/services/finance-report-service.ts` 和 `apps/web/src/app/api/v1/admin/billing/summary/route.ts` 中按日期、订单、状态和套餐汇总支付金额、入账、消费、退款与未回收点数
- [ ] T115 [P1] [US4] 在 `apps/web/src/app/admin/billing/page.tsx` 和 `apps/web/src/app/api/v1/admin/billing/refunds/[refundId]/resolve/route.ts` 中构建授权管理员订单、事件、异常、退款复核/结案和财务汇总视图，不显示签名或原始载荷
- [ ] T116 [P1] [US4] 在 `apps/web/src/app/api/v1/admin/billing/adjustments/route.ts` 和 `packages/domain/src/auth/permissions.ts` 中实现人工调整 API、管理员越权拒绝和审计访问
- [ ] T117 [P1] [US4] 在 `packages/billing/src/services/retention-service.ts` 中实现至少 24 个月保留、只追加历史和不可因知识删除级联

**检查点**：退款与对账达到公开销售门禁，仍须完成双语、部署和零影响验证。

---

## 阶段 7：用户故事 5 — 双语价格与费用规则透明

**目标**：中英文在价格、点数、订单、扣费、退款和异常上事实一致。

**独立测试**：切换语言检查每个付费页面，确认金额、点数和状态不发生语义变化。

### 测试

- [ ] T118 [P1] [P] [US5] 在 `tests/unit/billing/i18n.test.ts` 中验证中英文消息键完整、金额、点数和交易事实一致
- [ ] T119 [P1] [P] [US5] 在 `tests/e2e/billing/bilingual.spec.ts` 中覆盖价格、确认、余额不足、订单、退款、异常和客服入口
- [ ] T120 [P1] [P] [US5] 在 `tests/e2e/billing/taste.spec.ts` 中执行 Taste 真实浏览器检查，覆盖 1440、768、390、文字溢出、焦点和状态播报
- [ ] T121 [P1] [P] [US5] 在 `tests/accessibility/billing-checklist.md` 中记录键盘、标签、对比度、重排、目标大小和屏幕阅读器人工检查

### 实施

- [ ] T122 [P1] [US5] 在 `apps/web/src/messages/zh-CN.json` 和 `en.json` 中完善套餐、点率、余额、确认、错误、退款和客服全量消息
- [ ] T123 [P1] [US5] 在 `apps/web/src/components/billing/price-disclosure.tsx` 中实现中文默认、英文完整、含税最终价、一次性属性、不可转让和退款说明
- [ ] T124 [P1] [US5] 在 `apps/web/src/components/billing/quote-confirmation.tsx` 中保证中英文下分项、总价、余额和有效期使用同一服务端事实
- [ ] T125 [P1] [US5] 在 `apps/web/src/components/billing/order-status.tsx` 和 `refund-status.tsx` 中统一状态码到中英文语义，不显示内部供应商错误
- [ ] T126 [P1] [US5] 在 `apps/web/src/app/pricing/page.tsx`、`billing/page.tsx`、`settings/billing/page.tsx` 中完成移动布局、稳定尺寸和键盘操作
- [ ] T127 [P1] [US5] 在 `artifacts/billing/taste-report.md` 和 `artifacts/billing/screenshots/` 中保存 Taste 修复记录和桌面/平板/移动证据

**检查点**：双语和视觉质量达到公开销售门禁。

---

## 阶段 8：完善与上线门禁

**目的**：安全、负载、部署、恢复、成本和零影响。

- [ ] T128 [P0] 在 `tests/security/billing/secret-canaries.test.ts` 中扫描仓库、环境模板、DOM、API、日志、截图、数据库、事件和制品中的 Waffo 密钥与签名金丝雀
- [ ] T129 [P0] 在 `tests/security/billing/environment-isolation.test.ts` 中验证测试/生产物理数据库、数据库身份、商户、商品、地址、订单、账本和通知硬隔离
- [ ] T130 [P0] 在 `tests/load/billing/concurrent-credits.js` 中执行 100 组并发扣费并输出无负余额、无重复扣费，以及至少 100 笔可靠支付结果 30 秒到账达到 95% 的统计口径和响应分布
- [ ] T131 [P0] 在 `tests/e2e/zero-impact.test.ts` 中扩展拾题 `/`、`/app`、`/api`、`/bank`、`/pay`、`/apk`、Cookie、证书和支付 sidecar 零影响检查
- [ ] T132 [P0] 在 `deploy/scripts/preflight-existing-app.sh` 中增加 8091 端口、环境文件、Waffo 配置、测试/生产标识和 Nginx 回调冲突预检
- [ ] T133 [P1] 在 `deploy/scripts/deploy-billing.sh` 和 `infra/systemd/zhishiku-billing-bridge.service` 中实现桥接、Web/Worker 固定 ref 构建、备份、迁移、`nginx -t` 和失败关闭部署
- [ ] T134 [P1] 在 `tests/deploy/billing-one-click.test.sh` 和 `artifacts/deployment/billing-report.md` 中验证配置缺失、桥接启动失败、回调失败、Nginx 失败和回滚
- [ ] T135 [P1] 在 `tests/e2e/billing/quickstart.spec.ts` 和 `artifacts/billing/quickstart-report.md` 中执行 [quickstart.md](quickstart.md) 全部场景
- [ ] T136 [P1] 在 `checklists/acceptance.md` 中完成 FR-001 至 FR-075 与 SC-001 至 SC-022 的任务映射和证据状态
- [ ] T137 [P1] 在 `artifacts/billing/economic-review.md` 中完成 90 天成本、单价、毛利和供应商涨价敏感性评审
- [ ] T138 [P1] 在 `README.md`、[deployment.md](deployment.md)、[quickstart.md](quickstart.md) 和 `AGENTS.md` 中同步真实环境变量名称、测试/生产发布流程和禁止事项
- [ ] T139 [P2] 在 `tests/integration/billing/restart-recovery.test.ts` 中验证 Web、Worker、桥接、数据库和队列重启后的订单/预留/对账一致性
- [ ] T140 [P2] 在 `tests/integration/billing/restore-drill.test.ts` 中从 PostgreSQL 备份恢复订单与账本，重算余额并重新对账未决订单
- [ ] T141 [P2] 在 `tests/load/billing/sustained.js` 中执行持续订单、对账和扣费负载，报告积压、延迟和资源峰值
- [ ] T142 [P2] 在 `artifacts/billing/time-travel-report.md` 中验证三天延迟事件、30 分钟预留过期、3 个工作日退款期限和 24 个月保留
- [ ] T143 [P2] 在 `tests/usability/billing-user-study.md` 和 `artifacts/billing/usability-results.md` 中执行 10 人套餐、扣费与退款理解度研究
- [ ] T144 [P2] 在 `artifacts/billing/monthly-economics.md` 中建立月度/季度单位经济性和失败重试成本复测记录
- [ ] T145 [P1] 在 `artifacts/acceptance/billing-success-criteria.md` 中生成 SC-001 至 SC-022 的最终证据、目标提交 SHA 和发布批准记录

---

## 依赖关系

### 阶段依赖

- 阶段 0 和阶段 1 可部分并行；测试账户 PoC 必须先于真实迁移冻结。
- 阶段 2 阻塞全部用户故事；未通过账务事务测试前不得接入 001 AI。
- US1 与 US2 在阶段 2 后可并行，但真实收费仍关闭。
- US3 依赖 US2 的报价和预留错误模型。
- US4 依赖 US1 的订单/事件与 US2 的批次/预留。
- US5 依赖 UI 结构稳定后补全；P0 至少保留默认中文和关键错误可用。
- 阶段 8 依赖目标用户故事完成；P1 未通过不得开放生产收费。

### 与功能 001 的依赖

- 001 的 AI 隐私授权不等于扣点授权。
- 001 的导入、重新分析和搜索在调用付费 AI 前必须携带有效 `billingTaskId`。
- 001 的 P0 自动分类在 002 P0 未通过时只能使用受控测试计费或保持关闭。
- 001 的手动保存和本地搜索不得依赖支付桥接。

## 并行机会

- T018-T023 的 schema 与 T024-T031 的领域服务可按文件边界并行，但迁移测试需等待二者汇合。
- US1 桥接、订单 API 和价格页在契约冻结后可并行。
- US2 报价计算、批次分配和前端确认组件可并行。
- US4 退款领域与对账 worker 可并行，但事件冲突测试需共享 fixture。
- US5 文案、视觉和可访问性可并行，但事实文案必须等待 API schema 冻结。

## 实施策略

### P0 MVP

1. 完成阶段 0、阶段 1 和阶段 2。
2. 完成 US1 测试购买、US2 计费门禁和 US3 余额不足路径。
3. 运行并发、幂等、环境失败关闭和拾题零影响检查。
4. 保持 `BILLING_ENABLED=false`，只使用 Waffo 测试资产演示。
5. P0 通过后由产品批准三档价格、点率、100 点体验额度和自动扣点默认值。

### P1 公开收费

1. 完成退款、主动对账、财务汇总、完整双语和 Taste 检查。
2. 完成一键部署、回滚、配置缺失和零影响回归。
3. 完成单位经济性 50%/60% 门禁。
4. 经产品与测试双签后开启生产 Waffo 和真实购买。

### P2 持续运营

1. 恢复演练、持续负载、长期事件和可用性研究。
2. 每月检查成本、退款和通知异常。
3. 任何点率变更通过新目录版本和用户告知执行。

## 完成记录格式

每完成一个任务，必须先验证再勾选，并在任务列表下方追加独立记录：

```markdown
### 完成记录：Txxx

- 日期：YYYY-MM-DD
- 状态：已完成 / 重新打开
- 执行子智能体：开发或测试
- Git commit：<sha 或“未提交”>
- 摘要：一句话说明交付价值和风险关闭情况。
- 详情：实现内容、关键决策、修改文件和前置任务。
- 验证：命令、环境、夹具版本、结果计数和证据路径。
- 覆盖：FR-xxx、SC-xxx、USx 和测试文件。
- 风险与回滚：未覆盖项、残余风险、回滚方式。
```

支付、退款和零影响证据不得包含真实密钥、签名原文、完整支付载荷、银行卡信息或用户隐私。
