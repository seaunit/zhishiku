# 部署共存计划：seaunit.site

**功能**：`001-ai-personal-knowledge-base`

**检查日期**：2026-10-21

**状态**：仅为检查与设计；未进行任何生产变更

## 当前服务器基线

| 项目 | 观察结果 |
|------|----------|
| 主机 | `8.210.73.129` |
| 域名 | `https://seaunit.site/` 和 `https://www.seaunit.site/` |
| 操作系统 | Ubuntu 24.04.2 LTS |
| 资源 | 2 vCPU、3.4 GiB RAM、无 swap、49 GiB 磁盘、当前约 1.8 GiB 可用 |
| Docker | 未安装 |
| Web 入口 | 原生 Nginx 1.24.0，使用 80 和 443 端口 |
| 运行时 | 原生 systemd 服务 |
| DNS/CDN | Cloudflare 边缘节点 |

当前主机为名为“拾题”的现有应用提供服务。该应用并非 Docker 部署。

## 现有进程拓扑

| 组件 | 运行时 | 绑定 |
|-----------|---------|---------|
| Spring Boot 后端 | `systemd: shuati.service` | `*:8080` |
| 支付 sidecar | `systemd: shuati-payments.service` | `127.0.0.1:8090` |
| MySQL | 系统软件包 | `127.0.0.1:3306` |
| Redis | 系统软件包 | `127.0.0.1:6379` |
| Nginx | 系统软件包 | `0.0.0.0:80`、`0.0.0.0:443` |
| SSH | 系统服务 | 端口 22 |
| Cloudflare Tunnel 辅助进程 | 手动运行的 `cloudflared` 进程 | 本地 `127.0.0.1:20241` |

`cloudflared.service` 当前处于 inactive 状态；正在运行的隧道进程不由活动的 systemd unit 管理。在依赖它保障生产可用性之前，应单独审查此问题。

## 现有文件与服务

| 路径或服务 | 用途 |
|-----------------|---------|
| `/opt/shuati/` | 应用根目录 |
| `/opt/shuati/shuati-backend.jar` | Spring Boot 后端 |
| `/opt/shuati/frontend/` | 现有 SPA 和静态文件 |
| `/opt/shuati/payments/` | 支付 sidecar |
| `/opt/shuati/deploy/` | Nginx、systemd 和部署脚本 |
| `/etc/shuati/env` | 现有后端环境变量文件 |
| `/etc/shuati/payments.env` | 现有支付环境变量文件 |
| `/etc/nginx/conf.d/shuati.conf` | 活动的 Nginx 站点配置 |
| `/etc/systemd/system/shuati.service` | 现有后端 unit |
| `/etc/systemd/system/shuati-payments.service` | 现有支付 unit |

`/opt/shuati/deploy/scripts/deploy.sh` 中的部署脚本会上传 JAR 文件、前端、支付 sidecar 和 `deploy/` 目录，重启两个 systemd unit，验证 Nginx 并重新加载。因此，必须在以下两个位置同时更新 Nginx 源模板：

- `/opt/shuati/deploy/nginx/shuati.conf`
- `/opt/shuati/deploy/nginx/shuati-prod.conf`

仅更新 `/etc/nginx/conf.d/shuati.conf` 并不可靠，因为未来的部署或配置刷新可能会重新生成该文件。

## 当前 HTTP 路由

| 路径 | 上游或文件 |
|------|------------------|
| `/` | `127.0.0.1:8080` 上的 Spring Boot 落地页 |
| `/app/**` | 来自 `/opt/shuati/frontend` 的现有 SPA |
| `/api/**` | `127.0.0.1:8080` 上的 Spring Boot |
| `/api/auth/**` | 带更严格限流的 Spring Boot 认证路由 |
| `/bank/**` | Spring Boot 公开题库页面 |
| `/sitemap.xml` | Spring Boot |
| `/actuator/health` | Spring Boot |
| `/pay/webhooks/**` | `127.0.0.1:8090` 上的支付 sidecar |
| `/apk/**` | `/opt/shuati/apk` |
| `/assets/**` | 现有 SPA 资源缓存 |

当前 `/` 和 `/app` 均返回 `200`。`/` 是由后端渲染的落地页，`/app` 是 SPA。

## 目标 URL 布局

```text
https://seaunit.site/
  -> new portal with two entries:
       - 拾题 -> /app
       - 知识库 -> /kb

https://seaunit.site/app
  -> existing 拾题 SPA, unchanged

https://seaunit.site/kb
  -> new AI personal knowledge base, path-prefixed deployment

https://seaunit.site/kb/pay/webhooks/waffo
  -> new isolated Waffo bridge at 127.0.0.1:8091

https://seaunit.site/bank/...
  -> existing public question-bank pages, unchanged

https://seaunit.site/api/...
  -> existing Spring Boot API, unchanged

https://seaunit.site/pay/webhooks/...
  -> existing payment sidecar, unchanged
```

知识库必须使用 `basePath=/kb`。因此其路由和 API 如下：

```text
/kb
/kb/library
/kb/library/:id
/kb/capture
/kb/processing
/kb/trash
/kb/settings
/kb/api/...
/kb/_next/...
/kb/pricing
/kb/billing
/kb/settings/billing
/kb/pay/webhooks/waffo
```

新应用不得复用现有的 `/api`、`/auth`、`/assets` 或 `/app` 路由前缀。
识海 Waffo 通知必须由独立桥接处理，详细契约见
[002 部署方案](../002-ai-credit-billing/deployment.md)。

## Nginx 共存规则

1. 保留所有现有的 `/api`、`/api/auth`、`/bank`、`/sitemap.xml`、`/actuator`、`/pay`、`/apk` 和 `/app` 行为。
2. 为新的双产品门户添加精确匹配的 `location = /`。
3. 在 SPA catch-all 规则之前添加 `/kb`，并在代理时保留 `/kb` 前缀。
4. 将知识库应用绑定到 `127.0.0.1:3000`；不得公开暴露 3000 端口。
5. 将 Waffo 桥接绑定到 `127.0.0.1:8091`，不得占用拾题 `8090`。
6. 在通用 `/kb/` 之前添加精确 `location = /kb/pay/webhooks/waffo`，原样保留请求体、
   Waffo 签名头且关闭缓存和敏感访问日志。
7. 积极缓存带哈希的 `_next/static` 资源，但不得缓存 HTML、搜索或支付响应。
8. 使用独立的 session cookie 名称和限定于 `/kb` 的路径；不得替换现有应用的 cookie。
9. 仅针对 `/kb` 将 `client_max_body_size` 设置为已批准的上传限制；Waffo 回调阈值单独限制。
10. 在部署流程中保留 Nginx 验证与回滚步骤：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

路由形态示例：

```nginx
location = / {
    root /opt/seaunit-portal;
    try_files /index.html =404;
    add_header Cache-Control "no-store";
}

location = /kb {
    return 308 /kb/;
}

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

location /kb/_next/static/ {
    proxy_pass http://127.0.0.1:3000;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto https;
    add_header Cache-Control "public, max-age=31536000, immutable";
}

location /kb/ {
    proxy_pass http://127.0.0.1:3000;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $shuati_real_ip;
    proxy_set_header X-Forwarded-For $shuati_real_ip;
    proxy_set_header X-Forwarded-Proto https;
    proxy_read_timeout 300s;
    client_max_body_size 25m;
}
```

最终配置必须基于实际现有 `shuati.conf` 进行测试；此片段是设计指南，不可不经核对直接应用。

## 门户首页

在以下位置创建独立的静态门户：

```text
/opt/seaunit-portal/index.html
```

门户应：

- 标明站点身份，并提供两个清晰的应用入口。
- 将“拾题”链接到 `/app`，将“知识库”链接到 `/kb`。
- 保持现有公开题库和 SEO 路由可访问。
- 避免使用 JavaScript 和外部依赖，以便在应用部署期间保持稳定。
- 如果门户本身已本地化，则默认使用中文，并提供英文选项。

如果必须临时保留当前由后端渲染的 `/` 落地页的 SEO 内容，则迁移期间应通过单独路径提供该页面，例如 `/shuati`。

## 资源约束与部署配置

现有服务器当前仅有约 1.8 GiB RAM 可用，且没有 swap。在此主机上运行完整知识库技术栈将新增：

- PostgreSQL
- Meilisearch
- S3 兼容对象存储或等效的本地方案
- Web/API 进程
- 后台 worker
- 浏览器截图 worker

在 3.4 GiB 主机上，与现有 Spring Boot、MySQL、Redis 和支付 sidecar 同时运行该配置会造成较高的内存压力。未经容量审查，不建议部署完整技术栈。

### 推荐的生产配置

- 将主机升级到至少 4 vCPU / 8 GiB RAM，或将知识库部署到单独主机。
- 将 Nginx 保留在现有主机上，并将 `/kb` 路由到知识库服务。
- 如果已安装 Docker 且强制实施资源限制，则仅对知识库技术栈使用 Docker Compose。
- 保持现有拾题服务为原生方式且不做更改。
- 将所有新服务绑定到 loopback，并且仅暴露 Nginx。

### 当前主机受限配置

如果必须立即使用同一主机：

- 启动新技术栈前添加 swap 和明确的内存限制。
- 在未测量空闲状态和截图突发期间内存的情况下，不得部署完整的 Meilisearch + 对象存储 + 浏览器技术栈。
- 优先采用尽可能小的部署配置，并禁用可选 AI 集成，直到提供商和资源 PoC 通过。
- 保持 browser worker 隔离，并将并发截图任务上限设为 1。
- 为门户和 Nginx 变更定义维护窗口与回滚路径。

根据检查结果，在现有应用仍承受负载时，当前主机没有足够余量来保证 100,000 项搜索目标。

## 当前主机上的知识库运行时布局

如果选择原生 systemd 部署而非 Docker：

```text
/opt/zhishiku/                 application release
/etc/zhishiku/kb.env           environment file, mode 600
/etc/zhishiku/billing.env      Waffo bridge environment, mode 600
/var/lib/zhishiku/             durable data if local storage is used
/var/log/zhishiku/             optional log directory; journald preferred
```

建议的 unit：

```text
zhishiku-web.service
zhishiku-worker.service
zhishiku-browser-worker.service
zhishiku-billing-bridge.service
```

Web 服务应仅绑定到 `127.0.0.1:3000`；Waffo 桥接仅绑定到 `127.0.0.1:8091`。
Worker 服务不得公开暴露端口。桥接不拥有数据库、知识内容或点数账本凭据。现有的
Node.js 20 安装不满足计划中的 Node.js 24 运行时；请安装独立的 Node.js 24 运行时或
使用容器镜像。

## 环境变量与密钥规则

- 不得将知识库密钥追加到 `/etc/shuati/env`。
- 在 `/etc/zhishiku/kb.env` 创建独立的环境变量文件，模式为 `600`。
- 在 `/etc/zhishiku/billing.env` 创建只供 Waffo 桥接读取的环境文件；Web/Worker 不读取
  Waffo 私钥。
- 使用独立凭据，并在可能的情况下使用与现有应用不同的 DeepSeek 密钥。
- 绝不将真实密钥放入仓库、Nginx 配置、systemd unit 文件、日志或本文档。
- 启动验证可以打印缺失的变量名，但绝不能打印变量值。
- 知识库 session cookie 必须使用与现有应用不同的名称和路径。

预期新增的变量名包括：

```text
APP_BASE_URL
SESSION_SECRET
DATABASE_URL
MEILI_URL
MEILI_MASTER_KEY
S3_ENDPOINT
S3_BUCKET
S3_ACCESS_KEY
S3_SECRET_KEY
DEEPSEEK_API_KEY
DEEPSEEK_SEARCH_API_KEY
DEEPSEEK_FETCH_API_KEY
TRASH_RETENTION_DAYS
BACKUP_RETENTION_DAYS
MAX_UPLOAD_BYTES
LOG_LEVEL
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

桥接环境文件只保存 `WAFFO_ENV`、商户/店铺标识、私钥、`WAFFO_PRODUCT_MAP`、
`INTERNAL_EVENT_URL` 和内部服务密钥的变量名。完整名称与红线见
[002 部署方案](../002-ai-credit-billing/deployment.md)。
测试与生产必须使用独立物理 PostgreSQL 数据库，`BILLING_DB_ENV` 不一致时启动失败。

## 部署顺序

1. 记录现有 Nginx 配置和服务的发布快照。
2. 验证当前的健康检查端点。
3. 执行计费数据库迁移，保持 `BILLING_ENABLED=false`。
4. 在 `127.0.0.1:3000` 上部署知识库运行时，在 `127.0.0.1:8091` 上部署 Waffo 桥接，
   不配置公共路由。
5. 通过 Nginx 在本地验证 `/kb` 和安全健康端点。
6. 添加门户、根路径和 `/kb/pay/webhooks/waffo` 精确匹配 location。
7. 运行 `nginx -t`；如果失败，恢复之前的配置。
8. 使用 Waffo 测试环境验证购买、重复通知、漏通知主动对账、退款和回调回滚。
9. 验证 `/`、`/app`、`/kb`、`/api/*`、`/bank/*`、`/pay/*` 和证书。
10. 检查 cookie、登录/退出、搜索、上传、AI 授权、点数额度、扣费、退款和回收站。
11. 经产品与测试批准后启用生产 Waffo；P0/P1 门禁未通过时保持购买关闭。
12. 至少监控内存、磁盘、Nginx 错误、桥接重启、对账积压和 systemd 重启一天。

## 回滚

- 保留带时间戳的 Nginx 备份。
- 同时回退根门户 location 和 `/kb` 路由。
- 恢复 Waffo 回调变更前的 Nginx 配置，并将 `BILLING_ENABLED=false`。
- 停止 `zhishiku-billing-bridge.service`，但保留订单、支付事件、点数账本、退款和对账数据。
- 停止并禁用知识库 unit 或容器。
- 应用回滚期间不得删除知识库数据。
- 对回滚窗口内的可靠支付执行主动对账，避免已付款未到账。
- 重新检查现有应用的 `/`、`/app`、`/api`、`/bank`、`/pay` 和支付侧车。

## 在单独变更前明确不在范围内

- 修改现有拾题应用代码。
- 更改现有 MySQL 或 Redis 数据。
- 更改现有支付 webhook 路径。
- 替换现有 TLS 证书。
- 安装 Docker 或更改服务器软件包。
