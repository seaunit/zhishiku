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

## 启动

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\local-test\start.ps1
```

脚本会依次：

1. 检查 PostgreSQL 并创建 `zhishiku_test` 数据库。
2. 构建后端和 Waffo 桥接。
3. 启动后端，由 Flyway 创建或升级全部表。
4. 启动 Waffo dry-run 桥接。
5. 启动 Vite 前端并等待三个服务健康。

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

## 隔离边界

- 不读取 `/etc/zhishiku`、生产 systemd 环境或生产 Waffo 私钥。
- 不使用生产数据库、Cookie、订单、点数账本或邮件密码。
- 真实测试商户模式仍只连接本地 `zhishiku_test` 数据库。
