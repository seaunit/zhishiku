# 任务：识海 AI 点数计费与 Waffo 支付

**输入**：`specs/002-ai-credit-billing/`

**前置条件**：[spec.md](spec.md)、[plan.md](plan.md)、[research.md](research.md)、
[data-model.md](data-model.md)、[deployment.md](deployment.md)、
[contracts/openapi.yaml](contracts/openapi.yaml)、
[contracts/waffo-bridge.yaml](contracts/waffo-bridge.yaml)

**测试**：测试先行。Spring Boot 使用 JUnit 5/Testcontainers/RestAssured，Vue 使用
Vitest/Vue Test Utils，Node 桥接使用 Vitest/WireMock 风格夹具，E2E 使用 Playwright。

> 状态说明（2026-10-22）：T001-T072 是最初的规划草案，部分路径已被实际模块结构取代；
> 其未勾选状态只表示没有按原始措辞逐项留存证据，不代表当前代码必然缺失。当前发布状态以
> T073-T086、`artifacts/002/acceptance.md` 和 `artifacts/002/formal-payment-integration.md`
> 为准。真实 Waffo、物理 PostgreSQL、生产零影响和商业门禁仍以显式未完成项为准。

## 优先级

- **P0**：Spring 账本、报价/预留/结算、测试 Waffo、Node 桥接、Vue 购买闭环。
- **P1**：主动对账、退款、完整双语、Taste、生产隔离和零影响部署。
- **P2**：持续负载、恢复、长期事件和单位经济性复测。

## 阶段 0：Waffo PoC 与契约冻结

- [ ] T001 [P0] 在 `tests/poc/waffo/` 中验证测试商户、三个识海商品、收银台、查询、通知样本和退款事件
- [ ] T002 [P0] [P] 在 `bridges/waffo/src/contracts/` 中生成或校验共享 TypeScript 契约类型
- [ ] T003 [P0] [P] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/billing/contract/` 中为 Spring 8082 计费 OpenAPI 编写 RestAssured 契约测试，覆盖 CSRF 和内部事件鉴权
- [ ] T004 [P0] [P] 在 `tests/contract/waffo-bridge.test.ts` 中为桥接健康、收银台、查询、验签和转发错误编写测试
- [ ] T005 [P0] 在 `artifacts/002/waffo-poc.md` 中冻结脱敏事件、商品映射规则、环境字段和 Java/Node 内部错误码

## 阶段 1：工程与迁移

- [ ] T006 [P0] [P] 在 `bridges/waffo/package.json`、`tsconfig.json` 和 `pnpm-lock.yaml` 中创建 Node Waffo 桥接，并提供 `lint`、`typecheck`、`test` 和 `build` 脚本
- [ ] T007 [P0] [P] 在 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/billing/` 中创建计费领域模块
- [ ] T008 [P0] [P] 在 `frontend/kb-web/src/features/billing/` 中创建价格、余额、订单和退款功能目录
- [ ] T009 [P0] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/billing/migration/` 中先验证全新安装、001→002 升级、重复迁移、约束、部分唯一索引、环境字段和账本不可删除
- [ ] T010 [P0] [P] 在 `backend/kb-app/src/main/resources/db/migration/V0002__billing.sql` 中创建订单、事件、批次、账本、报价、预留、退款、图片资源和任务表
- [ ] T011 [P0] [P] 在 `.env.billing.example` 和 `specs/002-ai-credit-billing/quickstart.md` 中同步 Java、Vue 与 Node 环境变量
- [ ] T012 [P0] [P] 在 `deploy/systemd/zhishiku-waffo-bridge.service` 和 `deploy/nginx/kb-billing.conf` 中创建 8091、精确回调和内部路径拒绝模板

## 阶段 2：订单、账本与预留基础

### 测试

- [ ] T013 [P0] [P] [US2] 在 `backend/kb-core/src/test/java/site/seaunit/zhishiku/core/billing/order/` 中验证订单状态机、终止态和重复支付冲突
- [ ] T014 [P0] [P] [US1] 在 `backend/kb-core/src/test/java/site/seaunit/zhishiku/core/billing/ledger/` 中验证体验点优先、购买点最早优先和非负余额
- [ ] T015 [P0] [P] [US2] 在 `backend/kb-core/src/test/java/site/seaunit/zhishiku/core/billing/quote/` 中验证五/十点率、组合上限、唯一图片和失败释放
- [ ] T016 [P0] [P] [US2] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/billing/transaction/` 中使用 Testcontainers 验证并发预留、结算、释放和幂等

### 实施

- [ ] T017 [P0] 在 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/billing/order/` 中实现订单、支付事件和状态机
- [ ] T018 [P0] [US1] 在 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/billing/ledger/` 中实现批次、账本、余额投影和退款冻结
- [ ] T019 [P0] [US2] 在 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/billing/quote/` 中实现报价、图片去重、预留、结算和释放
- [ ] T020 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/persistence/` 中实现 JPA 实体和 `JdbcClient` 账本事务
- [ ] T021 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/idempotency/` 中实现请求哈希和安全响应重放
- [ ] T022 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/jobs/` 中实现过期报价、预留释放、对账和退款 PostgreSQL 任务

## 阶段 3：用户故事 1 — 点数购买与到账

**目标**：完成测试 Waffo 购买并可靠到账。

- [ ] T023 [P0] [P] [US1] 在 `bridges/waffo/src/` 中实现环境校验、内部鉴权、收银台和查询客户端
- [ ] T024 [P0] [P] [US1] 在 `bridges/waffo/src/signature.ts` 中实现原始请求体验签、时间窗口和稳定事件键
- [ ] T025 [P0] [US1] 在 `bridges/waffo/src/normalizer.ts` 中实现去银行卡、签名和原始载荷的安全事件
- [ ] T026 [P0] [US1] 在 `bridges/waffo/src/webhook.ts` 中实现验签后转发、成功确认和可重试错误
- [ ] T027 [P0] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/order/` 中实现下单、商品快照、订单复用和收银台协调
- [ ] T028 [P0] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/webhook/` 中实现内部事件端点、验签转发上下文和一次性入账
- [ ] T029 [P0] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/query/` 中实现订单历史、余额和账本查询 API
- [ ] T030 [P0] [US1] 在 `frontend/kb-web/src/features/billing/` 中实现价格、收银台入口、确认中、订单历史和余额
- [ ] T031 [P0] [P] [US1] 在 `frontend/kb-web/src/features/billing/__tests__/` 中验证中英文价格、一次性属性、余额不足和确认中
- [ ] T032 [P0] [US1] 在 `tests/e2e/billing-purchase.spec.ts` 中验证购买、返回、重复通知、查询到账和订单复用

## 阶段 4：用户故事 2 — AI 报价、预留与结算

**目标**：所有付费 AI 使用服务端报价和任务级预留。

- [ ] T033 [P0] [P] [US2] 在 `frontend/kb-web/src/features/billing/__tests__/quote-confirmation.spec.ts` 中验证分项、总上限、余额、有效期和确认
- [ ] T034 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/catalog/` 中实现版本化点率和套餐目录
- [ ] T035 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/quote/` 中实现报价 API、图片资源解析和唯一内容数量
- [ ] T036 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/task/` 中实现确认、余额检查、原子预留和计费任务
- [ ] T037 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/settlement/` 中实现成功步骤结算、失败释放和迟到结果忽略
- [ ] T038 [P0] [US2] 在 `frontend/kb-web/src/features/billing/` 中实现报价确认、余额不足、预计剩余和自动扣点上限
- [ ] T039 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/autocharge/` 中实现独立自动扣点授权、单次和每日上限
- [ ] T040 [P0] [US2] 在 `tests/e2e/billing-ai-charge.spec.ts` 中验证网页、文档、OCR、搜索、回答和分类计费
- [ ] T041 [P0] [US2] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/billing/idempotency/` 中验证同键同体、同键异体、并发结算和重放

## 阶段 5：用户故事 3 — 余额不足与内容保留

- [ ] T042 [P0] [P] [US3] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/billing/degradation/` 中验证余额不足、支付不可用和自动扣点关闭
- [ ] T043 [P0] [US3] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/error/` 中实现 402、409、503 和 `blocked_billing`
- [ ] T044 [P0] [US3] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/jobs/` 中把 001 导入、重新分析和外部搜索接入 `billingTaskId`
- [ ] T045 [P0] [US3] 在 `frontend/kb-web/src/features/billing/` 和 `knowledge/` 中实现余额不足仍保存、待整理状态和重新报价
- [ ] T046 [P0] [US3] 在 `tests/e2e/billing-degradation.spec.ts` 中验证 001 内容不丢失、不调用付费 AI 和不产生扣费

## 阶段 6：用户故事 4 — 对账、退款与批次

- [ ] T047 [P1] [P] [US4] 在 `backend/kb-core/src/test/java/site/seaunit/zhishiku/core/billing/refund/` 中验证全额、部分、重复退款和未回收异常
- [ ] T048 [P1] [P] [US4] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/billing/reconciliation/` 中验证通知/查询冲突、迟到支付和主动对账
- [ ] T049 [P1] [US4] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/refund/` 中实现退款案例、批次冻结和未使用点回收
- [ ] T050 [P1] [US4] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/reconciliation/` 中实现双通道对账和异常复核
- [ ] T051 [P1] [US4] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/api/billing/admin/` 中实现异常、退款和财务汇总 API
- [ ] T052 [P1] [US4] 在 `frontend/kb-web/src/features/billing/` 中实现退款状态、客服入口和人工复核展示
- [ ] T053 [P1] [US4] 在 `tests/e2e/billing-refund.spec.ts` 中验证体验点优先、退款回收和人工复核

## 阶段 7：用户故事 5 — 双语、Taste、环境与零影响

- [ ] T054 [P1] [P] [US5] 在 `frontend/kb-web/src/locales/` 中补齐价格、余额、扣费、退款和异常中英文
- [ ] T055 [P1] [P] [US5] 在 `frontend/kb-web/src/features/billing/__tests__/i18n.spec.ts` 中验证双语事实一致
- [ ] T056 [P1] [US5] 在 `tests/e2e/billing-taste.spec.ts` 中检查 1440/768/430/390/360/320、手机横屏、安全区和 200% 字体的确认与退款页面
- [ ] T057 [P1] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/billing/security/` 中验证商户/环境/数据库/商品/地址不匹配时启动失败
- [ ] T058 [P1] [US5] 在 `tests/security/billing/secret-canaries.spec.ts` 中扫描仓库、DOM、日志、截图、数据库、事件和制品
- [ ] T059 [P1] [US5] 在 `tests/e2e/billing-bridge-recovery.spec.ts` 中验证桥接重启、超时、主动查询和内部鉴权
- [ ] T060 [P1] [US5] 在 `tests/e2e/billing-zero-impact.spec.ts` 中验证拾题 `/`、`/app`、`/api`、`/bank`、`/pay`、`/apk`、Cookie、证书和 sidecar，并在 Nginx staging 验证 Vue `/kb/`、Spring `/kb/api` 和桥接回调

## 阶段 8：部署、性能与发布

- [ ] T061 [P1] [P] 在 `deploy/scripts/preflight-billing.sh` 中检查 8082、8091、环境文件、商品、数据库和 Nginx 冲突
- [ ] T062 [P1] [P] 在 `deploy/scripts/deploy-billing.sh` 中执行 Maven/Vue/Node 冻结锁文件安装与构建、Flyway、健康检查、备份和回滚
- [ ] T063 [P1] [P] 在 `tests/performance/billing/` 中执行 100 组并发扣费、订单峰值和对账积压
- [ ] T064 [P1] 在 `artifacts/002/acceptance.md` 中映射 FR/SC、目标提交 SHA 和发布批准
- [ ] T065 [P1] 在 `artifacts/002/unit-economics.md` 中完成 50%/60% 单位经济性门禁
- [ ] T066 [P1] 在 `README.md`、`AGENTS.md`、[deployment.md](deployment.md) 和 [quickstart.md](quickstart.md) 中同步实际 Java/Vue/Node 命令和端口

## 阶段 9：P2 加固

- [ ] T067 [P2] 在 `tests/integration/billing/restart-recovery/` 中验证 Spring、Node 桥接和任务重启后的订单/预留一致性
- [ ] T068 [P2] 在 `tests/integration/billing/restore-drill/` 中从 PostgreSQL 备份恢复账本并重算余额
- [ ] T069 [P2] 在 `tests/load/billing/sustained/` 中执行持续订单、对账和扣费负载
- [ ] T070 [P2] 在 `artifacts/002/time-travel-report.md` 中验证延迟事件、预留过期、退款期限和 24 个月保留
- [ ] T071 [P2] 在 `tests/usability/billing.md` 中执行 10 人套餐、扣费和退款理解度研究
- [ ] T072 [P2] 在 `artifacts/002/monthly-economics.md` 中建立月度成本复测

## 阶段 10：2026-10-22 资金安全与发布门禁收口

- [x] T073 [P0] 在计费服务中强制操作到必需计费项/数量的服务端映射，校验预检所有权、环境、
  kind、有效期和唯一图片数量，禁止用低价任务执行高价 AI。
- [x] T074 [P0] 在 Waffo 事件链中强制显式有效签名、商品 ID、金额、币种、订单状态和退款事实；
  未知查询返回 `unavailable`，商品缺失不得视为匹配。
- [x] T075 [P0] 修正退款以实际可回收批次点数落账；管理员调整显式指定目标账号，按试用/购买
  批次维护账本投影；财务状态和 SKU 汇总限定请求日期范围。
- [x] T076 [P1] 收敛 Spring 到桥接超时、过期收银台、对账 closed/refunded/mismatch 状态、
  买家不透明 ID、成功返回地址、双向密钥差异和桥接 systemd 写权限。
- [x] T077 [P1] 在部署脚本中增加 PostgreSQL 预部署备份、桥接 unit/Nginx snippet 回滚、
  `SHIHAI_BILLING_ENABLED` 显式选择、管理员邮箱和邮件退款入口。
- [ ] T078 [P0] 作为真实支付总门禁，使用真实 Waffo 测试商户完成商品、收银台、通知、查询、
  退款、重复/乱序事件和物理测试/生产数据库隔离验收。当前无生产凭据与真实测试商户证据。
- [ ] T079 [P0] 在真实 PostgreSQL 执行计费并发放行、账本批次、退款、恢复和 100 组并发门禁。
  当前 Testcontainers 因 Docker 引擎不可用实际执行 0 项。

## 阶段 11：2026-10-22 正式 Waffo 支付接入

实装提交：`990f4d8`。真实 Waffo 测试商户和物理 PostgreSQL 门禁仍未完成。

- [x] T080 [P0] 按官方 `@waffo/pancake-ts@0.26.0` 修复支付事件字段、退款 ID、金额回退、
  自动环境验签、商品元数据和主动查询契约。
- [x] T081 [P0] 为 Waffo Checkout 传 SDK 幂等键、商品/SKU/订单 metadata、价格快照和真实
  会话有效期，使用 SDK 返回的 `expiresAt`。
- [x] T082 [P0] 在价格接口返回 `billingEnabled`，支付关闭时禁用购买；返回页自动携带订单号，
  缺少订单号时恢复最近待支付订单，弹窗被拦截时保留可重试收银台入口。
- [x] T083 [P1] 增加 Waffo 商品初始化、收银台冒烟脚本和部署环境一致性开关；每次部署
  交互提示独立商户、店铺、私钥和商品映射，所有真实操作必须显式
  `--apply`/`--confirm`，不得输出私钥或完整 token URL。
- [x] T084 [P0] 增加桥接 38 项测试，覆盖官方支付/退款事件、真实 RSA 验签、篡改签名、
  配置缺失、幂等参数、查询金额/商品和私钥编码。
- [ ] T085 [P0] [依赖 T078] 在独立测试主机执行商品初始化冒烟，并保存三档商品、一次测试卡
  支付和一次退款证据；完成前 `SHIHAI_BILLING_ENABLED=false`。
- [x] T086 [P0] 让 Waffo release 自包含 `package.json`、商品初始化/冒烟脚本和运行时
  `node_modules`，避免部署后桥接缺少 SDK 或脚本不可执行。
- [x] T087 [P0] 将识海专用环境变量统一为 `SHIHAI_` 前缀，在后端、Waffo 桥接和部署脚本中
  实现新名优先、旧名同值双写、冲突失败关闭，并同步示例、SDD、测试和设备文档。

## 依赖关系

- T001-T005 在冻结账务迁移前完成。
- Spring 账本与桥接契约稳定后，Vue 购买页面和 Node 收银台可并行。
- US1 测试购买依赖 Spring 订单/账本和 Node 桥接。
- US2 依赖报价、余额和任务事务。
- US4 依赖 US1 的订单事件、US2 的批次和预留。
- P0 未通过时 `SHIHAI_BILLING_ENABLED=false`；P1 未通过不得公开收费。

## 完成记录格式

每个任务完成后勾选，并记录任务 ID、日期、执行角色、提交 SHA、摘要、详情、验证命令、
证据路径、FR/SC 映射、风险和回滚。支付证据不得包含真实密钥或完整支付载荷。

## Completion Log

### 批量完成记录：2026-10-22 资金安全与发布门禁收口

执行角色：产品、开发、测试子智能体。Git commit：未提交。以下各项均完成代码与本地自动化验证；
真实 Waffo、物理 PostgreSQL 和生产发布仍由 T078、T079 阻塞。

| 任务 | 摘要与详情 | 验证与证据 | 风险与回滚 |
|------|------------|------------|------------|
| T073 | 服务端按 operation 固定必需计费行和数量；预检校验账号、环境、kind、有效期；图片按内容哈希计数并与 OCR 行一致。 | `BillingP0PolicyTest` 9 项、后端全量测试；`BillingService`、`BillingP0PolicyTest`。 | 计费目录变化需同步规则；回滚会重新暴露低价任务执行高价 AI 风险。 |
| T074 | 支付事件必须显式有效签名，商品、金额、币种和订单状态匹配；查询未知订单返回 `unavailable`。 | `BillingP0PolicyTest`、`server.test.ts` 3 项。 | 真实 Waffo 字段仍待测试商户证据；回滚需保持 fail-closed。 |
| T075 | 退款按批次实际回收量落结论；管理员调整显式目标账号并维护试用/购买投影；财务状态与 SKU 限定日期。 | 后端编译与专项测试；`BillingService`、`002` OpenAPI。 | 真实 PostgreSQL 并发、退款冻结和部分回收仍待 T079。 |
| T076 | Spring→桥接增加 5/15 秒超时；清除过期 open 收银台；补齐对账状态；买家改为不透明 owner UUID；收银台传成功地址；双向密钥必须不同；桥接 unit 移除可写知识目录。 | 后端专项、桥接 typecheck/test/build、部署脚本检查。 | 真实 SDK 查询和回调仍待测试商户；配置变化必须同步 `/etc/zhishiku`。 |
| T077 | 部署前 `pg_dump` 备份；备份/恢复桥接 unit 与 Nginx snippet；`SHIHAI_BILLING_ENABLED` 不因填入凭据自动开启；增加管理员邮箱和 mailto 退款入口。 | `node tests/deploy/verify-deploy-scripts.mjs`、Git Bash `bash -n`（最终验收执行）。 | 数据库备份不自动回滚迁移；回滚仍以应用 release 和人工恢复演练为准。 |

### 正式 Waffo 支付接入：2026-10-22

执行角色：产品、开发、测试子智能体。Git commit：`990f4d8`。真实商户凭据未提供，
`SHIHAI_BILLING_ENABLED` 保持关闭。

| 任务 | 摘要与详情 | 验证与证据 | 风险与回滚 |
|------|------------|------------|------------|
| T080 | 桥接改用官方 0.26 事件字段，支付/退款金额优先新字段，验签自动识别环境，退款 ID 使用退款工单或 eventId。 | Waffo 桥接 34 项 Vitest；`normalizer.test.ts`、`waffo-gateway.test.ts`。 | 真实商户字段仍需 T085 证据；桥接可回滚到上一 release，但不得重新接受缺失签名事件。 |
| T081 | Checkout 传 `orderId` 幂等键、订单/SKU/Product metadata、价格快照和 15 分钟有效期，使用 SDK `expiresAt`。 | `waffo-gateway.test.ts`、`server.test.ts`。 | 若 Waffo 调整会话期限或 metadata 行为需回归测试。 |
| T082 | 价格接口新增 `billingEnabled`；购买按钮在关闭时禁用；返回页恢复本地订单；弹窗被拦截时保留链接；退款复核结果可见。 | `BillingControllerTest`、Vitest 48 项、Playwright 支付专项 6 项。 | 生产支付仍需真实回调验证；UI 回滚不影响已有余额查询。 |
| T083 | 增加商品初始化/冒烟脚本；`SHIHAI_RUNTIME_ENV` 同时派生应用、数据库和 Waffo 环境。 | 桥接 lint/typecheck/test/build、`verify-deploy-scripts.mjs`。 | 脚本不自动执行真实商品或支付；需操作者显式确认。 |
| T084 | 覆盖官方事件、RSA 验签、篡改签名、店铺校验、退款缺失金额、查询多次支付、配置缺失、幂等和私钥编码。 | 桥接 38 项测试；后端全量、前端 Vitest/构建。 | Testcontainers 仍受 Docker 不可用限制。 |
| T086 | release 复制 Waffo `package.json`、锁文件、`scripts/` 和 `node_modules`，部署检查覆盖运行时依赖。 | `verify-deploy-scripts.mjs`、Git Bash `bash -n`。 | release 体积略增；回滚 release 不受影响。 |

### 完成记录：T087

- 日期：2026-10-22
- 状态：已完成本地实现与自动化验证；真实生产环境文件迁移尚未执行
- 执行子智能体：产品、开发、测试
- Git commit：`029617d`
- 摘要：识海专用环境变量统一为 `SHIHAI_` 正式前缀，并保留旧名迁移兼容。
- 详情：Spring 配置全部改为新名优先、旧名回退；新增启动前别名解析器，支持空新值回退、同值
  双写兼容和异值失败关闭，并注册到 `spring.factories`。Waffo 桥接、商品初始化、收银台冒烟、
  部署和 provision 脚本采用相同规则；部署文件同时写入同值旧别名，供上一 release 回滚启动。
  provision 改为只更新受管键，不再整体覆盖环境文件。示例、部署说明、AGENTS、宪法、001/002/
  006/007/008 相关 SDD 和测试控制变量已同步。
- 验证：后端全量 249 项通过（core 22、app 226、browser worker 1）；Waffo 41 项、lint、
  typecheck、build 通过；前端 lint、typecheck、Vitest 48 项、build 通过；Spring 新名、旧名、
  空值、同值、异值和注册测试通过；部署依赖自检、Bash 语法、双写、冲突和环境文件保留黑盒
  检查通过；`git diff --check` 和 `kb-app` Spotless 通过。
- 证据：`ShihaiEnvironmentPostProcessor.java`、`ShihaiEnvironmentPostProcessorTest.java`、
  `application.yml`、`bridges/waffo/src/config.ts`、`deploy/scripts/deploy.sh`、
  `deploy/scripts/provision-ubuntu.sh`、`artifacts/002/environment-prefix-migration.md`。
- 覆盖：FR-084、SC-025；部署环境标识、密钥隔离和回滚兼容。
- 风险与回滚：本机 Docker 不可用，Testcontainers PostgreSQL 用例执行 0 项；本机无
  `shellcheck`，部署脚本仅完成 `bash -n` 与函数级黑盒检查。旧别名至少保留一个稳定版本；
  回滚 release 可读取双写旧名，不得在回滚目标迁移前删除旧别名。
