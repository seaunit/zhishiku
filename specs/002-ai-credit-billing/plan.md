# 实施计划：识海 AI 点数计费与 Waffo 支付

**功能**：`002-ai-credit-billing`

**日期**：2026-10-22

**规格**：[spec.md](spec.md)

**上游功能**：[001 AI 个人知识库](../001-ai-personal-knowledge-base/spec.md)

## 摘要

为识海增加一次性点数包、Waffo 个人支付、点数批次账本、原子报价、预留与结算、自动扣点
授权、退款复核和对账。支付只改变订单与点数事实；AI 任务只有取得有效计费任务后才能执行。

计费核心使用 Java 17 + Spring Boot 3.x 和 PostgreSQL 18；所有账务更新在 Spring 事务、
行锁、唯一约束和条件更新下完成。Waffo 继续由独立 Node.js 桥接持有 SDK 和私钥，只负责
商品映射、收银台、原始体验签、受控查询和安全事件转发。桥接不拥有业务数据库，不读取知识
正文，也不处理本地点数结算。

## 技术背景

**前端**：Vue 3、TypeScript、Vite、Vue Router、Pinia、TanStack Vue Query、vue-i18n

**计费后端**：Java 17、Spring Boot 3.x、Spring Data JPA、`JdbcClient`、Flyway、
Spring Security、Springdoc OpenAPI

**Waffo 桥接**：Node.js 20/22 LTS、TypeScript、现有 Pancake SDK；没有数据库

**数据库**：PostgreSQL 18，保存订单、事件、点数批次、账本、预留、报价、退款和幂等记录

**任务**：PostgreSQL 任务表、事务租约和 `FOR UPDATE SKIP LOCKED`；Spring Scheduler
执行通知对账、预留过期和退款检查

**测试**：JUnit 5、AssertJ、Testcontainers、RestAssured、WireMock、Vitest、
Playwright、k6、Gitleaks、Trivy

## 宪法检查

| 门禁 | 设计前 | 设计后 | 证据 |
|------|--------|--------|------|
| SDD 与中文文档 | 通过 | 通过 | 规格、计划、研究、契约、数据和任务均中文 |
| 产品/开发/测试分离 | 通过 | 通过 | 产品冻结价格，开发实现账务，测试独立验收 |
| 默认中文、完整英文 | 通过 | 通过 | Vue/Spring 消息覆盖价格、余额、扣费和退款 |
| 事实与可追溯 | 通过 | 通过 | 订单、事件、批次、报价步骤、退款和复核可审计 |
| Taste 视觉验证 | 通过 | 通过 | 价格、余额、确认和退款页面 1440/768/390/360 |
| 禁止爬虫与密钥安全 | 通过 | 通过 | 桥接不访问知识内容；Waffo 私钥只进桥接环境 |
| GitHub 一键部署 | 通过 | 通过 | Maven、Vite、Node 桥接、Flyway、systemd 和回滚 |
| 拾题零影响 | 通过 | 通过 | 独立端口 8091、进程、数据库、Cookie 和 `/kb` 路由 |
| 任务可追溯 | 通过 | 通过 | 每项任务记录证据和提交 SHA |
| 点数完整性 | 通过 | 通过 | PostgreSQL 账本、事务锁、报价和预留门禁 |

无需违反章程。

## 架构决策

1. **Spring Boot 拥有计费领域。**
   订单、点数批次、账本、报价、预留、结算、释放、退款和对账均由 Java 领域服务实现。
   Node 桥接不能直接增加余额或写账本。

2. **Node Waffo 桥接保留。**
   桥接只持有 Pancake SDK、商户配置和私钥，处理收银台、签名通知和受控查询；通过内部
   OpenAPI 与 Spring Boot 通信。桥接故障不会破坏知识库免费功能。

3. **PostgreSQL 是唯一账务权威。**
   点数变更使用只追加账本和可重算投影。金额、点数、任务和批次均使用整数；禁止浮点财务。

4. **原子报价与任务级结算。**
   组合任务生成版本化分项报价，一次性预留总额；成功步骤结算，失败步骤释放。最终扣费不得
   超过用户确认上限。

5. **图片唯一资源计费。**
   Spring 解析 `imageAssetIds` 的所有权和扫描状态，按规范化 `content_hash` 去重；Node
   桥接和前端都不能决定实际计费数量。

6. **通知与主动查询共享状态机。**
   Waffo 通知和主动查询转为同一安全事件；Spring 事务执行订单锁、环境校验、金额/币种/
   商品核验和幂等入账。

7. **环境硬隔离。**
   测试/生产使用独立物理 PostgreSQL 数据库、商户、商品、密钥、通知、返回地址、订单和
   账本。启动时 `BILLING_DB_ENV` 不一致立即失败。

8. **任务不依赖 Redis。**
   对账、过期释放、退款和重试存储在 PostgreSQL 任务表中。Worker 使用租约和
   `SKIP LOCKED`，进程重启后恢复。

9. **Vue 只展示服务端事实。**
   余额、报价、订单、扣费和退款状态来自 API。前端缓存不能作为余额权威，也不得在支付返回
   页自行加点。

10. **与拾题零共享。**
    不读取或写入拾题 MySQL、Redis、Cookie、支付 sidecar、`/pay`、订单或密钥。识海回调固定
    为 `/kb/pay/webhooks/waffo`。

## 正式支付接入收口（2026-10-22）

- Node 桥接固定使用 `@waffo/pancake-ts@0.26.0`，收银台请求的 `metadata` 同时携带
  `orderId`、`itemCode` 和 `waffoProductId`；SDK 幂等键固定为本地订单 ID。
- 收银台会话默认 15 分钟，以 SDK 返回的 `expiresAt` 写入本地会话表；前端返回地址自动附带
  `orderId`，缺少参数时从最近待支付订单恢复。
- Webhook 事件按 `orderMetadata`、`chargedAmount`、`refundedAmount`、
  `refundTicketMerchantExternalId` 等 0.26 字段归一化，并兼容旧字段。
- 主动查询使用 `snapshotAmountDetails` 和 `onetimeOrder.onetimeProduct.id`，支付状态
  `failed/canceled` 映射为关闭。
- Webhook 验签先自动识别环境，再由 Spring 根据事件环境拒绝跨环境入账。
- 价格接口返回 `billingEnabled`；前端在支付关闭时禁用购买并明确说明免费功能不受影响。
- `KB_RUNTIME_ENV` 同步派生 `APP_ENV`、`BILLING_DB_ENV` 和 `WAFFO_ENV`，不匹配时部署失败。
- 商品初始化与收银台冒烟脚本要求显式 `--apply`/`--confirm`，脚本不打印私钥或完整 token URL。

## 项目结构

```text
backend/kb-core/src/main/java/site/seaunit/zhishiku/core/billing/
backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/
backend/kb-app/src/main/resources/db/migration/V0009__billing.sql
backend/kb-app/src/main/resources/db/migration/V0014__billing_environment_guard.sql
frontend/kb-web/src/views/BillingView.vue
frontend/kb-web/src/api/client.ts
bridges/waffo/src/
specs/002-ai-credit-billing/contracts/openapi.yaml
specs/002-ai-credit-billing/contracts/waffo-bridge.yaml
deploy/systemd/zhishiku-waffo-bridge.service
```

## 数据与接口

- 实体和状态机见 [data-model.md](data-model.md)。
- 计费 API 见 [contracts/openapi.yaml](contracts/openapi.yaml)。
- 桥接 API 见 [contracts/waffo-bridge.yaml](contracts/waffo-bridge.yaml)。
- 订单状态：`PENDING -> CONFIRMING -> PAID -> REFUNDING -> REFUNDED`；
  异常进入 `EXCEPTION`。
- 计费任务：`QUOTED -> CONFIRMED -> RESERVED -> RUNNING -> SETTLED`；
  未完成终态可进入 `RELEASED` 或 `EXPIRED`。
- 所有写请求使用幂等键；同键不同请求哈希返回 `409`。
- Spring 内部事件端点不出现在 Nginx 公共代理中；桥接与 Spring 使用双向独立密钥。

## 购买与扣费流程

```text
用户选择点数包
  -> Spring 创建待支付订单和套餐快照
  -> Node 桥接创建/恢复 Waffo 收银台
  -> Waffo 通知或受控查询
  -> 桥接验签并发送安全事件
  -> Spring 事务核验并一次性入账

AI 任务
  -> Spring 生成分项报价
  -> 用户确认或匹配自动扣点授权
  -> 事务预留
  -> 执行 AI
  -> 成功步骤结算，失败步骤释放
```

## 部署共存

- Spring Boot API 建议绑定 `127.0.0.1:8082`；Vue 静态文件由 Nginx 提供。
- Waffo Node 桥接绑定 `127.0.0.1:8091`，继续独立 systemd unit。
- 识海回调 `/kb/pay/webhooks/waffo` 原样转发到桥接，关闭缓存和敏感访问日志。
- Spring 与桥接使用独立环境文件和内部密钥；Spring API/Worker 不读取 Waffo 私钥。
- 不占用拾题 `8080`、支付 sidecar `8090`、`/pay`、MySQL、Redis 或 Cookie。
- 当前主机内存有限，完整栈仍需独立主机或扩容。

详细方案见 [deployment.md](deployment.md)。

## 实施顺序

1. Waffo 测试 PoC、Java/Node 内部契约和脱敏事件夹具。
2. Spring 计费领域、Flyway 迁移、订单/批次/账本/幂等和任务租约。
3. Vue 价格、购买、订单和余额页面。
4. Node 桥接收银台、验签、查询和事件转发。
5. Spring 报价、确认、预留、结算、释放和自动扣点授权。
6. 对账、退款、批次冻结和人工复核。
7. 零影响部署、性能、安全、双语和 Taste 门禁。

## 验证策略

- JUnit：订单状态机、批次顺序、报价取整、预留、退款和错误映射。
- Testcontainers：PostgreSQL 并发事务、通知/查询竞态和任务租约。
- RestAssured：Spring 计费 OpenAPI 和桥接内部契约。
- WireMock/桥接 mock：Waffo 超时、重复、乱序、签名和环境错误。
- Vue Vitest：价格、余额、确认、错误和退款状态。
- Playwright：测试购买、返回页、余额不足、自动扣点和退款。
- k6：100 组并发扣费、对账积压和预留过期。
- 安全：密钥金丝雀、跨环境事件、签名伪造、日志和截图泄漏。

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| Java/Node 契约漂移 | OpenAPI、双向鉴权、同版本部署和 WireMock 契约 |
| 重复到账或扣费 | 唯一事件键、行锁、终态不可回滚和幂等键 |
| 并发透支 | 原子预留、条件更新和 100 组并发门禁 |
| 退款误扣其他批次 | 原订单批次冻结和体验点/购买点消费顺序 |
| 测试支付进入生产 | 环境标识、物理数据库和商品/地址硬校验 |
| 桥接泄漏知识 | 桥接不接数据库、不接收正文，只处理安全支付字段 |
| 支付故障影响内容 | 手动录入、本地搜索和待整理状态独立运行 |
| 主机资源不足 | 独立主机、扩容、进程上限和监控 |

## 复杂度跟踪

Node 桥接保留一个小进程，但避免在 Java 中重复实现不稳定的 Waffo SDK 细节。Spring 是
计费权威，桥接不能直接改账本。MVP 不引入 Redis、消息总线或独立账务数据库。
