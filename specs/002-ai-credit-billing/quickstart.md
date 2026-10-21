# 快速入门与验证：识海 AI 点数计费与 Waffo 支付

**功能**：`002-ai-credit-billing`

**目的**：在实现完成后验证套餐、测试支付、点数报价、扣费、退款、对账、安全和双语闭环。
本文是验收指南，不代表当前代码已经实现。

## 前置条件

- Java 17 LTS、Maven Wrapper、Node.js 20/22 LTS、pnpm 和 Docker Compose v2。
- PostgreSQL、Spring Boot API/Worker 和 Node Waffo 桥接本地运行。
- Waffo 测试环境已配置独立识海商品和测试凭据。
- Vue 可通过 `http://127.0.0.1:4173/kb/` 访问，Spring API 位于
  `http://127.0.0.1:8082/kb/api`。
- 测试支付凭据由 Waffo 测试环境提供，不写入仓库或本文。

## 环境变量名称

`.env.example` 只能包含安全占位值。Spring API/Worker 与 Waffo 桥接使用不同环境文件。

```dotenv
APP_ENV=test
BILLING_DB_ENV=test
BILLING_ENABLED=false
BILLING_BRIDGE_URL=http://127.0.0.1:8091
BILLING_CATALOG_VERSION=2026-10-mvp-test
BILLING_WEBHOOK_URL=https://seaunit.site/kb/pay/webhooks/waffo
CHECKOUT_SUCCESS_URL=https://seaunit.site/kb/billing/return
REFUND_SUPPORT_URL=https://support.example.invalid/refund
REFUND_RESPONSE_TARGET=3 个工作日
```

仅 Waffo 桥接进程可见的变量：

```dotenv
WAFFO_ENV=test
WAFFO_STORE_ID=replace-me
WAFFO_MERCHANT_ID=replace-me
WAFFO_PRIVATE_KEY_BASE64=replace-me
WAFFO_PRODUCT_MAP=kb_pack_9=replace-me,kb_pack_29=replace-me,kb_pack_99=replace-me
WAFFO_CURRENCY=CNY
WAFFO_DRY_RUN=false
INTERNAL_EVENT_URL=http://127.0.0.1:8082/kb/api/internal/billing/waffo-events
```

Spring API/Worker 与桥接两端必须持有相同值的内部服务密钥，但 Spring 不读取 Waffo 私钥：

```dotenv
APP_TO_BRIDGE_SECRET=replace-me
BRIDGE_TO_APP_SECRET=replace-me
```

启动检查只允许报告缺失变量名，绝不能打印值。`APP_ENV=production` 时发现 `WAFFO_ENV=test`、
`BILLING_DB_ENV` 不一致、测试商品或测试回跳地址必须失败关闭。测试与生产必须使用不同的
物理 PostgreSQL 数据库，不能只切换环境字段。

## 启动计划中的技术栈

```powershell
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,worker
cd ../frontend/kb-web
pnpm install
pnpm dev
cd ../../bridges/waffo
pnpm install
pnpm dev
```

预期端点：

- Vue：`http://127.0.0.1:4173/kb/`
- Spring API：`http://127.0.0.1:8082/kb/api`
- Waffo 桥接：`http://127.0.0.1:8091`
- PostgreSQL：仅内部网络
- Worker：无公开端口

## 验证场景

### 1. 目录、SKU 与价格

1. 打开 `/kb/pricing`，确认三档完整显示。
2. 确认 SKU 为 `kb_pack_9`、`kb_pack_29`、`kb_pack_99`。
3. 确认价格为 CNY 9、29、99，点数为 400、1,400、5,000。
4. 确认页面显示一次性购买、点数用途、不可提现/转让、退款入口和 3 个工作日复核目标。
5. 确认页面没有使用拾题商品代码或拾题支付信息。

### 2. 测试购买与可靠到账

1. 使用测试环境完成最低档购买。
2. 从 Waffo 返回识海时，先确认订单显示“确认中”。
3. 确认签名通知到达后订单变为已支付，400 点只到账一次。
4. 重放同一通知，确认余额和账本不变化。
5. 重复点击购买，确认待支付有效期内只出现一个可付款收银台会话。
6. 关闭收银台后确认订单关闭，余额不变化。

### 3. 迟到支付与主动对账

1. 拦截或延迟测试通知，让订单保持待支付或确认中。
2. 运行对账 worker，确认 10 分钟内通过受控查询恢复。
3. 构造关闭后到达的有效测试支付事件，确认订单只到账一次。
4. 让通知与查询返回相反状态，确认不自动撤回已入账点数，并生成异常。

### 4. AI 报价、预留与成功结算

1. 发起一个网页截图分析 + 自动归类组合。
2. 确认页面显示分项 10 + 5 点和总上限 15 点。
3. 确认前查询账户，余额不变化。
4. 确认后查询，15 点进入预留且任务为 `RESERVED`。
5. 模拟截图分析成功、归类失败，确认只结算 10 点并释放 5 点预留。
6. 刷新页面、重启 Worker 后再查询，任务与余额保持一致。

### 5. 无效结果与超时释放

1. 模拟超时、空输出、协议错误和无法解析结果。
2. 确认每种情况都不收费。
3. 让任务超过 30 分钟不结算，运行过期任务后确认全部预留释放。
4. 注入迟到成功结果，确认不会产生第二次扣费。
5. 主动重跑已成功任务，确认创建新计费任务而不是复用旧扣费。

### 6. 原子报价与余额不足

1. 让账户只有 14 点，发起上限 15 点的组合任务。
2. 确认返回 `402 INSUFFICIENT_POINTS`，不执行任何子步骤。
3. 确认原网址、文档、截图或文字仍保留并可手动保存。
4. 充值后返回原任务，确认可重新报价并确认。
5. 将余额调整为刚好 15 点，确认任务可执行且不会透支。

### 7. 并发扣费与幂等

1. 运行 100 组并发扣费夹具，将可用余额设置为低于总请求量。
2. 确认只有余额足够数量的任务进入 `RESERVED`。
3. 确认负余额次数为 0、同一任务重复结算次数为 0。
4. 使用同一幂等键重放确认与结算请求，确认返回原结果。
5. 同一幂等键提交不同请求体时确认返回 `409`。

### 8. 自动整理扣点

1. 保持自动扣点关闭并保存手动内容。
2. 确认内容保存并进入待整理，不产生扣费。
3. 开启自动扣点，设置单次 15 点、每日 100 点。
4. 在限额内保存内容，确认保存前显示自动扣点，保存后只扣一次。
5. 让下一任务超过单次或每日上限，确认暂停自动 AI 并要求用户确认。
6. 余额不足时确认内容仍保存。

### 9. 退款与批次

1. 使用体验点完成一次消费，再购买最小套餐，确认购买点未被优先消费。
2. 对未消费购买订单发送全额退款测试事件。
3. 确认原订单批次冻结、只剩原批次点数被回收，其他批次不变。
4. 对一个已部分消费的订单执行退款，确认不产生负余额并进入人工复核。
5. 确认复核记录有负责人、期限、最终结论和用户可见状态。
6. 重放退款事件，确认只回收一次。

### 10. 环境隔离与密钥安全

1. 在生产配置中放入测试商户、测试商品或测试通知地址，确认启动失败关闭。
2. 将测试支付事件发送到生产，确认不改变生产订单或余额。
3. 在私钥、SMTP 密码、内部密钥和支付载荷位置放置唯一金丝雀。
4. 扫描仓库、DOM、日志、截图、API、数据库和证据文件，确认不出现原始值。
5. 确认生产服务不读取 `/etc/shuati/payments.env` 或拾题商品映射。

### 11. 双语与 Taste 视觉验证

1. 确认首次访问默认中文并完整切换英文。
2. 核对价格、点数、点率、订单状态、余额不足、扣费和退款事实一致。
3. 在 1440/768/430/390/360/320 px、手机横屏、安全区和 200% 字体下使用
   `design-taste-frontend` 检查价格、余额、确认、异常和退款页面。
4. 确认文字不溢出、按钮尺寸稳定、金额表单单行可读、状态不只依赖颜色。
5. 保存截图和浏览器检查证据。

### 12. 与拾题零影响

1. 记录变更前 `/`、`/app`、`/api`、`/bank`、`/pay`、`/apk`、证书和认证基线。
2. 部署识海计费变更，执行测试购买、通知重放、通知丢失和回滚。
3. 重复检查所有拾题路由、支付侧车、MySQL、Redis、Cookie 和 systemd 状态。
4. 确认识海通知只命中 `/kb/pay/webhooks/waffo`，不命中拾题 `/pay/webhooks/**`。
5. 注入桥接启动失败和 Nginx 配置失败，确认旧应用不中断且部署回滚。

## 自动化命令

实现工作区和脚本后，以下命令构成发布门禁：

```powershell
cd backend
./mvnw spotless:check
./mvnw test
./mvnw verify -Pintegration
./mvnw verify -Pcontract
./mvnw verify -Psecurity
cd ../frontend/kb-web
pnpm lint
pnpm typecheck
pnpm test
pnpm test:e2e:billing
pnpm build
cd ../../bridges/waffo
pnpm install --frozen-lockfile
pnpm lint
pnpm typecheck
pnpm test
pnpm build
```

## 证据要求

每个付费验收场景至少保存：

- 使用脱敏 fixture 的订单状态时间线。
- 扣费前后余额、预留和结算点数。
- 重复通知/查询事件的计数和处理结果。
- 退款回收、未回收和人工复核证据。
- 测试/生产环境标识及配置缺失失败关闭截图。
- 原有拾题变更前后健康检查报告。
- 双语与 1440/768/390 截图和 Taste 检查报告。

任何证据都不得包含真实密钥、签名原文、完整支付载荷、银行卡信息或用户隐私。

## 相关设计产物

- [spec.md](spec.md)
- [plan.md](plan.md)
- [research.md](research.md)
- [data-model.md](data-model.md)
- [deployment.md](deployment.md)
- [contracts/openapi.yaml](contracts/openapi.yaml)
- [contracts/waffo-bridge.yaml](contracts/waffo-bridge.yaml)
- [checklists/acceptance.md](checklists/acceptance.md)
