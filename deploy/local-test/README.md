# 识海本地测试环境

本目录用于在隔离的本地数据库和支付环境中启动识海，不连接生产数据库、生产 Waffo
凭据或生产邮件服务。

## 默认配置

- PostgreSQL：`127.0.0.1:5432/zhishiku_test`
- 数据库用户和密码：`postgres` / `postgres`
- 后端：`http://127.0.0.1:8082/kb/`
- 前端：`http://127.0.0.1:4173/kb/`
- Waffo 桥接：`http://127.0.0.1:8091/health`
- 邮件：Spring `local` Profile 的模拟发送，不发真实邮件
- 注册验证码：本地 API 直接返回调试码
- Waffo 支付：默认 `dry-run`，不会创建真实收银台或产生扣款
- Waffo 测试回调：真实测试模式下自动创建临时 Cloudflare 隧道并更新识海专用 Webhook

## 启动

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\local-test\start.ps1
```

脚本会依次：

1. 检查 PostgreSQL 并创建 `zhishiku_test` 数据库。
2. 构建后端和 Waffo 桥接。
3. 启动后端，由 Flyway 创建或升级全部表。
4. 启动 Waffo 桥接；真实测试模式下启动 Cloudflare 隧道。
5. 创建或更新只接收 `order.completed`、`refund.succeeded` 的识海测试 Webhook。
6. 启动 Vite 前端并等待服务健康。

日志和 PID 文件位于 `.runtime/local-test/`，不会进入 Git。

## 停止

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\local-test\stop.ps1
```

## 切换真实 Waffo 测试商户

根目录的 `.env.local-test` 已被 Git 忽略，可按需覆盖以下变量：

```dotenv
SHIHAI_WAFFO_DRY_RUN=false
SHIHAI_WAFFO_ENV=test
SHIHAI_WAFFO_STORE_ID=<测试店铺>
SHIHAI_WAFFO_MERCHANT_ID=<测试商户>
SHIHAI_WAFFO_PRODUCT_MAP=kb_pack_9=<测试商品>,kb_pack_29=<测试商品>,kb_pack_99=<测试商品>
SHIHAI_WAFFO_PRIVATE_KEY_FILE=<测试私钥 PEM 的绝对路径>
```

脚本会把 PEM 文件临时编码到当前进程环境，不会复制或提交私钥。

## Waffo 测试回调

Waffo 测试支付完成通知不会发往本地 `127.0.0.1`，必须有一个公网 HTTPS 地址。启动脚本
在 `SHIHAI_WAFFO_DRY_RUN=false` 时会：

1. 使用 `cloudflared` 为本地桥接创建临时公网隧道。
2. 查询 Waffo 测试店铺现有 Webhook，避免改动拾题的回调。
3. 创建或更新只包含 `order.completed`、`refund.succeeded` 的识海专用测试 Webhook。
4. 停止本地环境时关闭隧道进程。

Cloudflare 临时域名每次启动都会变化，脚本会自动更新 Webhook。旧域名失效后，Waffo
不会把此前失败的支付事件自动补送到新域名，因此完成配置后需要重新发起一笔测试支付。

可通过 `SHIHAI_WAFFO_TEST_WEBHOOK_ID` 固定识海测试 Webhook 的 ID。未设置时，脚本按
`channel=http`、`testMode=true` 和固定的两个事件自动识别。

## 隔离边界

- 不读取 `/etc/zhishiku`、生产 systemd 环境或生产 Waffo 私钥。
- 不使用生产数据库、Cookie、订单、点数账本或邮件密码。
- 真实测试商户模式仍只连接本地 `zhishiku_test` 数据库。
