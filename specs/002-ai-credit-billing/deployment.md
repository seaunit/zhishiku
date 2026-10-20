# 部署共存计划：识海计费与 Waffo 支付

**功能**：`002-ai-credit-billing`

**检查日期**：2026-10-21

**状态**：设计草案；未进行生产变更

## 当前基线

现有 `seaunit.site` 使用原生 Nginx、systemd、Spring Boot、MySQL、Redis 和 Node 支付
sidecar，不使用 Docker。识海不得复用或修改该支付 sidecar 的订单、商品、密钥、端口或
公开路由。

| 现有项目 | 值 | 识海规则 |
|----------|----|----------|
| 拾题后端 | `systemd: shuati.service`，`*:8080` | 不修改 |
| 拾题支付 sidecar | `systemd: shuati-payments.service`，`127.0.0.1:8090` | 不复用 |
| 拾题支付通知 | `/pay/webhooks/**` | 不占用、不转发、不重写 |
| 拾题环境文件 | `/etc/shuati/env`、`/etc/shuati/payments.env` | 不读取、不追加 |
| Nginx | 公共 `80/443` | 仅增量添加 `/kb` 精确规则 |
| 识海 Web | 计划 `127.0.0.1:3000` | 只经 `/kb` 代理 |
| 识海 Waffo 桥接 | 计划 `127.0.0.1:8091` | 只经精确通知路由访问 |

服务器上的私钥、商户 ID、店铺 ID、商品 ID 和支付环境值不得写入仓库或本文件。

## 目标路由

```text
https://seaunit.site/                              -> 双应用门户
https://seaunit.site/app                           -> 拾题 SPA
https://seaunit.site/kb                            -> 识海
https://seaunit.site/kb/pay/webhooks/waffo         -> 识海 Waffo 桥接
https://seaunit.site/pay/webhooks/**               -> 拾题支付侧车，原样保留
https://seaunit.site/api/**、/bank/**、/apk/**     -> 拾题，原样保留
```

识海回调不得使用 `/pay/webhooks/waffo`、`/api/payments/**` 或拾题内部通知路径。

## 运行时边界

```text
/opt/zhishiku/                            release
/etc/zhishiku/kb.env                     Web/Worker 环境，mode 600
/etc/zhishiku/billing.env                Waffo 桥接环境，mode 600
/var/lib/zhishiku/                       持久数据（如本地存储启用）
/var/log/zhishiku/                       可选日志目录；优先 journald
```

建议 unit：

```text
zhishiku-web.service
zhishiku-worker.service
zhishiku-browser-worker.service
zhishiku-billing-bridge.service
```

门禁：

- Web 只绑定 `127.0.0.1:3000`。
- Waffo 桥接只绑定 `127.0.0.1:8091`，不得使用 8090。
- Worker 不开放公共端口。
- 所有服务使用独立系统用户、日志标识和环境文件。
- 桥接不拥有数据库连接或知识存储凭据。
- Web/Worker 不读取 Waffo 私钥。

## 环境变量边界

Web/Worker 的 `kb.env` 只包含应用配置与桥接调用密钥名称，不包含 Waffo 私钥：

```text
APP_ENV
BILLING_DB_ENV
BILLING_ENABLED
BILLING_BRIDGE_URL
APP_TO_BRIDGE_SECRET
BRIDGE_TO_APP_SECRET
BILLING_CATALOG_VERSION
CHECKOUT_SUCCESS_URL
BILLING_WEBHOOK_URL
REFUND_SUPPORT_URL
REFUND_RESPONSE_TARGET
```

桥接的 `billing.env` 只包含 Waffo 和事件转发配置：

```text
WAFFO_ENV
WAFFO_STORE_ID
WAFFO_MERCHANT_ID
WAFFO_PRIVATE_KEY_BASE64
WAFFO_PRODUCT_MAP
WAFFO_CURRENCY
WAFFO_DRY_RUN
INTERNAL_EVENT_URL
BRIDGE_TO_APP_SECRET
APP_TO_BRIDGE_SECRET
```

规则：

- 两个文件权限均为 `600`，所有者分别为对应服务用户。
- 不得把变量值写入 Nginx、systemd unit、GitHub Actions 明文、命令历史或文档。
- 启动校验只列出缺失变量名。
- `APP_ENV=production` 与 `WAFFO_ENV=test` 同时出现时失败关闭。
- 测试和生产必须使用不同物理 PostgreSQL 数据库；`BILLING_DB_ENV` 必须与
  `APP_ENV`、`WAFFO_ENV` 一致，否则启动失败。不得通过同一数据库的不同 schema 代替。
- 生产商品映射必须只包含 `kb_pack_*` 商品；发现拾题商品或其他环境商品时失败关闭。
- 生产环境缺少 `REFUND_SUPPORT_URL`、使用 `.invalid` 测试地址或答复目标不是已批准值时
  必须失败关闭购买。

## Nginx 增量配置

回调规则必须在通用 `/kb/` 之前，且不得改写请求体：

```nginx
location = /kb/pay/webhooks/waffo {
    proxy_pass http://127.0.0.1:8091/webhooks/waffo;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto https;
    proxy_request_buffering on;
    proxy_buffering off;
    client_max_body_size 1m;
    access_log off;
}

location ^~ /kb/api/internal/ {
    return 404;
}
```

要求：

- 不添加登录 Cookie 校验。
- 不缓存、不压缩、不改写或重新序列化请求体。
- 保留 Waffo 签名头原文。
- 不将桥接 `/health`、`/checkout` 或 `/orders/status` 暴露到公网。
- 内部事件路径 `/kb/api/internal/` 必须在 Nginx 直接返回 404；应用层内部密钥认证保留为
  第二道防线，不能替代网络边界。
- 通用 `/kb/` 继续代理 Web 3000；精确回调规则优先。
- 变更前备份活动配置与仓库模板，执行 `nginx -t` 后才 reload。

## 支付流与内部认证

### 收银台

```text
Web 3000  -> POST 127.0.0.1:8091/checkout
          -> Waffo 收银台
          -> 用户支付
```

Web 到桥接使用 `APP_TO_BRIDGE_SECRET`。桥接不保存本地余额或知识。

### 通知

```text
Waffo
  -> https://seaunit.site/kb/pay/webhooks/waffo
  -> Nginx 精确转发到 127.0.0.1:8091/webhooks/waffo
  -> 桥接验签并向 Web 发送安全化事件
  -> POST http://127.0.0.1:3000/kb/api/internal/billing/waffo-events
  -> Web 事务处理订单、批次与账本
```

桥接到 Web 使用 `BRIDGE_TO_APP_SECRET`。内部事件接口不得对外开放或依赖用户 Cookie。
Web 将事件持久化并返回 `409` 进入人工复核时，桥接必须向 Waffo 返回成功确认 `200`；
只有未持久化或临时故障才返回可重试错误。

### 主动对账

Worker 定期扫描未决订单，通过桥接的 `/orders/status` 获取规范化状态。查询结果与通知进入
同一计费仓库的幂等处理路径，不直接修改余额。

## 部署顺序

1. 记录拾题 `/`、`/app`、`/api`、`/bank`、`/pay`、`/apk`、证书、认证和支付 sidecar
   基线。
2. 备份 PostgreSQL、当前 Nginx 配置、systemd unit 和环境文件清单。
3. 部署数据库迁移，但保持 `BILLING_ENABLED=false`。
4. 部署并启动 `zhishiku-billing-bridge.service`，验证健康仅从本机可访问。
5. 部署 Web/Worker，验证内部通知转发和主动对账。
6. 使用 Waffo 测试商品执行购买、重复通知、通知丢失、退款和回滚演练。
7. 增加 Nginx 精确回调与 `/kb` 路由，执行 `nginx -t`。
8. 部署双应用门户。
9. 通过相同基线检查证明拾题零影响。
10. 产品批准商业参数和单位经济性后，切换生产 Waffo 环境并开放购买。

## 健康检查

必须验证：

- 识海 Web `/kb/health`。
- 桥接本机 `/health`，只报告 `configured`、环境和 dry-run 状态。
- 数据库迁移版本、队列连接和计费 worker 活跃度。
- 数据库身份与 `BILLING_DB_ENV`、`APP_ENV`、`WAFFO_ENV` 一致。
- 支付目录版本、商品映射完整性和环境一致性。
- 订单事件处理延迟、对账积压、过期预留和退款复核期限。
- 拾题全部原路由和支付 sidecar 保持健康。

任何支付配置缺失或环境冲突必须使购买功能失败关闭，但不阻止手动录入、本地检索和已有知识。

## 备份与恢复

- 订单、支付事件、点数批次、账本、退款、自动扣点授权和对账记录纳入 PostgreSQL PITR。
- 财务数据至少保留 24 个月，不得因知识条目删除而级联清除。
- 恢复演练必须验证余额可由批次与账本重算。
- 恢复后对未决订单重新执行主动对账；已支付事实不得被回滚。
- 旧备份不得恢复超过保留期且已完成退款结案的错误余额。

## 回滚

1. 将 `BILLING_ENABLED=false`，停止新订单和新付费 AI 预留。
2. 恢复变更前 Nginx 配置并执行 `nginx -t` 后 reload。
3. 停止并禁用 `zhishiku-billing-bridge.service`。
4. 保留 Web/Worker、订单、账本、退款和对账数据，不回滚删除财务事实。
5. 如必须回退应用版本，使用固定 Git ref 和数据库向前兼容迁移。
6. 对回滚窗口内的可靠支付事件执行主动对账，避免用户已付款但未到账。
7. 重新检查拾题 `/`、`/app`、`/api`、`/bank`、`/pay`、`/apk`、认证和支付侧车。

## 容量与发布门禁

- 当前主机内存余量有限；新增桥接和完整知识库栈必须通过容量报告。
- 浏览器 worker、Meilisearch、PostgreSQL、Web、Worker 和桥接必须设置明确资源上限。
- P0 计费测试未通过不得开放真实收费。
- P1 退款、对账、双语和零影响测试未通过不得公开销售。
- 单位经济性未达到每套餐可变成本不超过收入 50%、整体目标毛利不低于 60% 时不得发布。

## 明确不在范围内

- 修改拾题支付 sidecar、商品、订单、余额或 `/pay` 路由。
- 复用拾题 Waffo 私钥、商户映射或测试状态。
- 由识海直接保存银行卡、CVC 或完整支付凭据。
- 订阅、自动续费、自动退款申请和部分退款自动比例计算。
- 在未通过容量审批时把完整识海技术栈部署到现有主机。
