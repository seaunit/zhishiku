# 部署共存计划：seaunit.site

**功能**：`001-ai-personal-knowledge-base`

**检查日期**：2026-10-21

**状态**：设计与只读检查；未执行生产变更

## 当前服务器基线

| 项目 | 观察结果 |
|------|----------|
| 主机 | `8.210.73.129` |
| 域名 | `https://seaunit.site/`、`https://www.seaunit.site/` |
| 系统 | Ubuntu 24.04.2 LTS，2 vCPU、3.4 GiB RAM、无 swap |
| Docker | 未安装 |
| Web | Nginx 1.24.0，原生 systemd |
| 现有后端 | Spring Boot，`shuati.service`，`*:8080` |
| 现有支付 | `shuati-payments.service`，`127.0.0.1:8090` |
| 数据库 | MySQL `127.0.0.1:3306`、Redis `127.0.0.1:6379` |

现有应用不是 Docker 部署。知识库不得修改其进程、MySQL、Redis、Cookie、认证或支付配置。

## 目标架构

```text
https://seaunit.site/
  -> 静态门户，提供 /app 和 /kb

https://seaunit.site/kb/
  -> Nginx 提供 Vue dist 静态文件

https://seaunit.site/kb/api/**
  -> Spring Boot API 127.0.0.1:8082

https://seaunit.site/kb/pay/webhooks/waffo
  -> Node Waffo bridge 127.0.0.1:8091

Java browser worker
  -> 无公共端口
```

端口 `8082` 必须在部署预检中确认空闲；不得复用 `8080`、`8090` 或 `8091`。

## 部署包

GitHub 固定 commit/tag 构建并发布：

```text
/opt/zhishiku/releases/<commit>/
├── kb-app.jar
├── kb-browser-worker.jar
├── frontend/
├── waffo-bridge/
├── migrations/
└── VERSION
```

```text
/etc/zhishiku/kb.env             mode 600
/etc/zhishiku/waffo.env          mode 600
/var/lib/zhishiku/               durable application files
/var/log/zhishiku/               可选，优先 journald
```

## systemd

```text
zhishiku-api.service
zhishiku-worker.service
zhishiku-browser-worker.service
zhishiku-waffo-bridge.service
```

- API 绑定 `127.0.0.1:8082`。
- Worker 关闭 HTTP 端口或仅暴露本地管理端口。
- browser worker 不开放公共端口，最多并发一个 Chromium 实例。
- Waffo bridge 绑定 `127.0.0.1:8091`。
- Waffo bridge 使用独立 Node.js 20/22 运行时和依赖目录；不得复用拾题支付 sidecar 的
  `node_modules`、环境文件或进程。
- 所有 unit 使用独立用户、工作目录、环境文件、内存上限和 journald 标识。

## Nginx 路由

```nginx
location = / {
    root /opt/seaunit-portal;
    try_files /index.html =404;
}

location = /kb {
    return 308 /kb/;
}

location /kb/assets/ {
    alias /opt/zhishiku/current/frontend/assets/;
    add_header Cache-Control "public, max-age=31536000, immutable";
}

location ^~ /kb/api/internal/ {
    return 404;
}

location /kb/api/ {
    proxy_pass http://127.0.0.1:8082;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto https;
    proxy_set_header X-Forwarded-For $remote_addr;
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

location /kb/ {
    alias /opt/zhishiku/current/frontend/;
    try_files $uri $uri/ /kb/index.html;
    add_header Cache-Control "no-store";
    client_max_body_size 25m;
}
```

最终配置必须基于现有 Nginx 文件做增量修改，执行 `nginx -t` 后 reload。失败时恢复备份。

## 数据库

- 识海使用独立 PostgreSQL 18 数据库、系统用户、端口、备份和迁移目录。
- 测试与生产使用不同物理数据库。
- 迁移启用 `pg_trgm`，创建搜索投影 GIN 索引和任务租约索引。
- 不读写拾题 MySQL 或 Redis。
- 不创建跨应用数据库账号。

## 容量

当前主机可用内存无法证明 Spring Boot + PostgreSQL + MinIO + Chromium + Node bridge 与现有
应用共存安全。推荐独立主机或升级到至少 4 vCPU / 8 GiB RAM。

若必须同机：

- 添加 swap 并设置明确内存上限。
- browser worker 并发限制为 1。
- 使用外部 S3 兼容对象存储，避免本机 MinIO。
- JVM 设置合理 `-Xmx`。
- 在截图和搜索峰值时监控 OOM、磁盘和请求延迟。
- 未完成容量演练不得开放全部 AI/截图功能。

## 环境变量

```text
APP_ENV=production
APP_BASE_URL=https://seaunit.site/kb
DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/zhishiku
DATABASE_USERNAME=zhishiku
DATABASE_PASSWORD=<环境变量>
SESSION_COOKIE_NAME=zhishiku_session
KB_CAPTCHA_TTL_SECONDS=120
KB_CAPTCHA_MAX_ENTRIES=10000
KB_CAPTCHA_EXPOSE_ANSWER=false
S3_ENDPOINT=<环境变量>
S3_BUCKET=<环境变量>
S3_ACCESS_KEY=<环境变量>
S3_SECRET_KEY=<环境变量>
DEEPSEEK_API_KEY=<环境变量>
DEEPSEEK_SEARCH_API_KEY=<环境变量>
DEEPSEEK_FETCH_API_KEY=<环境变量>
SMTP_PASSWORD=<环境变量>
BILLING_ENABLED=false
BILLING_BRIDGE_URL=http://127.0.0.1:8091
BILLING_DB_ENV=production
```

Waffo 私钥只进入 `/etc/zhishiku/waffo.env`。日志不得输出变量值、支付载荷或用户内容。
每次执行部署脚本时，AI API Key 和 SMTP 密码都必须通过 `/dev/tty` 隐藏交互输入；已有密钥
在直接回车时保留，输入新值时才更新。密钥只写入权限为 `600` 的
`/etc/zhishiku/kb.env`，不得放入命令行参数、Git、部署日志或文档示例。
单次输入默认等待 60 秒，超时按回车处理；可用 `KB_PROMPT_TIMEOUT` 调整等待秒数。
首次安装脚本调用内部部署时传递“已完成提示”状态，避免同一次执行询问两轮。

## 一键部署

入口脚本必须支持固定 ref，执行：

1. 验证 GitHub 官方 HTTPS 地址。
2. 记录现有应用健康基线。
3. 检查磁盘、内存、端口、数据库和 Nginx 冲突。
4. 备份当前 release、Nginx、环境文件映射和数据库。
5. 从 GitHub 拉取指定 commit/tag。
6. Maven 构建 Java、pnpm 构建 Vue、安装或验证 Node 桥接依赖。
7. Flyway 迁移。
8. 原子切换 release symlink。
9. 启动/重启识海 unit。
10. 健康检查 `/kb/api/actuator/health`、`/kb/`、桥接本地健康和数据库。
11. 运行原有应用零影响检查。
12. 任一步失败则恢复上一 release、Nginx 和数据库备份。

## 回滚

- 切回上一 release symlink。
- 恢复 Nginx 备份并 `nginx -t`。
- 关闭识海购买和付费 AI。
- 停止识海 unit，但保留数据库、对象和账本。
- 对回滚期间可靠支付执行主动对账。
- 复核 `/`、`/app`、`/api`、`/bank`、`/pay`、`/apk`、证书、认证和支付 sidecar。

## 当前范围外

- 修改拾题代码或数据库。
- 复用拾题端口、Cookie、Redis、支付 sidecar 或密钥。
- 替换 TLS 证书。
- 在未批准情况下安装 Docker 或大幅改变主机软件包。
