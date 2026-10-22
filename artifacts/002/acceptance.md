# 计费与 Waffo 验收记录

日期：2026-10-22

状态：本地代码、合同测试和桥接测试通过；真实收费未批准，`SHIHAI_BILLING_ENABLED` 默认关闭。

## 已完成证据

| 门禁 | 证据 |
|------|------|
| 操作到计费项映射 | `BillingP0PolicyTest` 9 项 |
| 预检账号、环境和有效期 | `BillingP0PolicyTest`、`BillingService` |
| 支付签名、商品、金额和币种 | `BillingP0PolicyTest`、桥接 38 项 Vitest、官方 SDK 0.26 事件夹具 |
| 订单、账本、预留、退款 API | `BillingControllerTest`、`BillingInternalControllerTest`、后端全量 230 项 |
| 退款实际回收和人工复核 | `BillingService`、002 OpenAPI |
| 管理员显式目标账号 | `ManualAdjustmentRequest.targetOwnerId` |
| 物理数据库环境标识 | `V0014__billing_environment_guard.sql` |
| AI 入口稳定幂等 | `V0013__api_idempotency.sql`、五类 AI 入口专项测试 |
| 部署备份与配置回滚 | `deploy/scripts/deploy.sh`、`verify-deploy-scripts.mjs` |
| 桥接构建 | lint、38 项 Vitest、typecheck、build 全部通过 |

## 未完成门禁

- 使用真实 Waffo 测试商户完成三档商品、收银台、通知、主动查询和退款联调。
- 在独立测试和生产物理 PostgreSQL 上执行 Flyway、并发预留、账本重算和恢复。
- 真实通知乱序、重复、伪造签名、超时、桥接重启和主动对账演练。
- 生产 Nginx、systemd、桥接回调、管理员账号和原有拾题应用零影响验收。
- 单价、毛利、退款政策和客服答复时限由产品和财务最终批准。

上述门禁通过前不得打开生产收费，也不得把本地 mock 测试视为真实支付证据。
