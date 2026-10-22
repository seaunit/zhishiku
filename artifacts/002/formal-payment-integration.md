# 正式 Waffo 支付接入记录

日期：2026-10-22

状态：代码与本地自动化完成（`990f4d8`）；真实测试商户支付仍待验收，
`BILLING_ENABLED=false`。

## 参考基线

- 识海基线：`71da83dde03023eccf4da89931de94d3d6f469e6`
- 拾题参考：`7ce4b501aae72bf12f3a392b1618e33fda785a2d`
- Waffo SDK：`@waffo/pancake-ts@0.26.0`

未读取、复制或修改拾题私钥、商品映射、订单、数据库和支付环境文件。

## 已收口差异

| 领域 | 处理 |
|------|------|
| Webhook 商品身份 | 收银台 metadata 写入 `orderId`、`itemCode`、`waffoProductId`；从官方 `orderMetadata` 恢复，兼容旧 `metadata` |
| 支付/退款金额 | 支付优先 `chargedAmount`，退款优先 `refundedAmount`，旧字段仅回退 |
| 退款 ID | 优先 `refundTicketMerchantExternalId`，缺失时使用稳定 `eventId` |
| Webhook 验签 | SDK 自动环境验签；桥接按事件 `mode` 输出环境，Spring 拒绝跨环境入账 |
| Checkout 幂等 | 本地订单 UUID 同时作为 HTTP 幂等键和 SDK `idempotencyKey` |
| Checkout 有效期 | 请求 15 分钟有效期，使用 SDK 返回的 `expiresAt`，不本地猜测 |
| 主动查询 | 使用 `payments(orderMerchantExternalId)`、`snapshotAmountDetails` 和 `onetimeOrder.onetimeProduct.id` |
| 私钥输入 | 支持 PEM、转义 PEM、Base64 包装 PEM 和裸 PKCS#8 Base64 |
| 前端恢复 | 返回地址自动带本地订单号；缺少时恢复最近待支付/确认中订单 |
| 弹窗拦截 | 保留“返回收银台”链接，不创建第二笔订单 |
| 支付关闭 | 价格接口返回 `billingEnabled=false`，购买按钮禁用且免费功能说明可见 |
| 环境一致性 | `KB_RUNTIME_ENV` 派生 `APP_ENV`、`BILLING_DB_ENV` 和 `WAFFO_ENV`，不一致拒绝部署 |

## 自动化证据

- Waffo 桥接：38 项 Vitest、lint、typecheck、build 通过。
- 后端：完整 Maven 测试 230 项通过；新增内部通知鉴权、公开目录支付状态和收银台返回 URL
  测试。
- 前端：lint、typecheck、48 项 Vitest、生产构建通过。
- Playwright：完整应用 E2E 42 项通过、2 项真实后端用例跳过；支付关闭、购买入口、弹窗
  拦截、订单返回恢复、余额刷新和退款复核专项通过。
- 部署：`tests/deploy/verify-deploy-scripts.mjs` 通过。

## 尚未完成

- 使用独立识海 Waffo 测试商户和店铺创建/发布三档商品。
- 使用测试卡完成收银台、签名通知、主动查询、退款、重复和乱序事件验收。
- 在独立测试和生产物理 PostgreSQL 上执行计费并发、迁移和恢复演练。
- 完成上述门禁前不得打开 `BILLING_ENABLED`。
