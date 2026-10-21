# 任务：识海 AI 点数计费与 Waffo 支付

**输入**：`specs/002-ai-credit-billing/`

**前置条件**：[spec.md](spec.md)、[plan.md](plan.md)、[research.md](research.md)、
[data-model.md](data-model.md)、[deployment.md](deployment.md)、
[contracts/openapi.yaml](contracts/openapi.yaml)、
[contracts/waffo-bridge.yaml](contracts/waffo-bridge.yaml)

**测试**：测试先行。Spring Boot 使用 JUnit 5/Testcontainers/RestAssured，Vue 使用
Vitest/Vue Test Utils，Node 桥接使用 Vitest/WireMock 风格夹具，E2E 使用 Playwright。

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

## 依赖关系

- T001-T005 在冻结账务迁移前完成。
- Spring 账本与桥接契约稳定后，Vue 购买页面和 Node 收银台可并行。
- US1 测试购买依赖 Spring 订单/账本和 Node 桥接。
- US2 依赖报价、余额和任务事务。
- US4 依赖 US1 的订单事件、US2 的批次和预留。
- P0 未通过时 `BILLING_ENABLED=false`；P1 未通过不得公开收费。

## 完成记录格式

每个任务完成后勾选，并记录任务 ID、日期、执行角色、提交 SHA、摘要、详情、验证命令、
证据路径、FR/SC 映射、风险和回滚。支付证据不得包含真实密钥或完整支付载荷。
