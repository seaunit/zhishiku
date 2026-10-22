# 部署共存计划：seaunit.site

**功能**：`001-ai-personal-knowledge-base`

**检查日期**：2026-10-21

**状态**：已实现增量门户与一键部署；生产变更和回滚由部署脚本管理

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
  -> 静态门户 /opt/seaunit-portal/current，提供 /app 和 /kb

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
zhishiku-waffo-bridge.service
```

- API 绑定 `127.0.0.1:8082`。
- MVP 的 Playwright browser worker 由 API 以短生命周期子进程启动，不开放任何端口。
- OCR 使用本机 Tesseract CLI，与 API 共享只读语言数据目录。
- Waffo bridge 绑定 `127.0.0.1:8091`。
- Waffo bridge 使用独立 Node.js 20/22 运行时和依赖目录；不得复用拾题支付 sidecar 的
  `node_modules`、环境文件或进程。
- 所有 unit 使用独立用户、工作目录、环境文件、内存上限和 journald 标识。

## Nginx 路由

```nginx
location = / {
    root /opt/seaunit-portal/current;
    try_files /index.html =404;
}

location ^~ /seaunit-portal/ {
    root /opt/seaunit-portal/current;
    try_files $uri =404;
    add_header Cache-Control "public, max-age=3600";
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
    client_max_body_size 26m;
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

最终配置必须基于现有 Nginx 文件做增量修改：替换精确的 `location = /`，增加独立的
`/seaunit-portal/` 静态资源 location，再注入 `/kb` include。执行 `nginx -t` 后 reload；
失败时恢复 Nginx 备份和上一门户 release。

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
DEEPSEEK_AI_ENDPOINT=https://api.deepseek.com/chat/completions
DEEPSEEK_MODEL=deepseek-chat
DEEPSEEK_AI_TIMEOUT_SECONDS=35
DEEPSEEK_SEARCH_API_KEY=<环境变量>
DEEPSEEK_FETCH_API_KEY=<环境变量>
KB_BROWSER_WORKER_JAR=/opt/zhishiku/current/kb-browser-worker.jar
KB_WEB_IMPORT_ALLOW_UNBILLED_AI=false
KB_BROWSER_TIMEOUT_SECONDS=35
KB_OCR_TIMEOUT_SECONDS=30
KB_WEB_IMPORT_MAX_TEXT_CHARS=30000
KB_WEB_IMPORT_MAX_ITEMS=20
KB_DOCUMENT_IMPORT_MAX_TEXT_CHARS=60000
KB_DOCUMENT_IMPORT_TIMEOUT_SECONDS=25
KB_SCREENSHOT_IMPORT_MAX_TEXT_CHARS=30000
PLAYWRIGHT_BROWSERS_PATH=/opt/zhishiku/playwright
SMTP_HOST=smtpdm.aliyun.com
SMTP_PORT=465
SMTP_USERNAME=no-reply@mail.seaunit.site
SMTP_PASSWORD=<环境变量>
SMTP_FROM=no-reply@mail.seaunit.site
SMTP_FROM_NAME=识海
SMTP_SSL=true
REGISTRATION_ENABLED=true
REGISTRATION_CODE_TTL_SECONDS=600
REGISTRATION_RESEND_COOLDOWN_SECONDS=60
REGISTRATION_SEND_WINDOW_SECONDS=3600
REGISTRATION_MAX_SENDS_PER_EMAIL=5
REGISTRATION_MAX_SENDS_PER_IP=20
REGISTRATION_MAX_ATTEMPTS=5
REGISTRATION_MAX_CACHED_CODES=10000
REGISTRATION_EXPOSE_CODE=false
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
SMTP 必须启用 TLS，连接、读取和写入超时默认 10 秒。生产响应不得返回邮箱验证码；仅 local
profile 可以返回 `debugCode` 供自动化测试。临时邮箱域名清单位于仓库受控资源中，不得运行时爬取。

## 网页截图与 OCR 依赖

网页链接入口不是 URL 收藏。生产主机必须安装：

- `tesseract-ocr` 和 `tesseract-ocr-chi-sim`，且 `tesseract --list-langs` 同时包含
  `chi_sim` 和 `eng`。
- Playwright Chromium，安装目录固定为 `/opt/zhishiku/playwright`。
- release 内的 `kb-browser-worker.jar`，由 API 通过受控子进程调用。

部署脚本在切换 release 前构建 worker、安装 Chromium 并校验 OCR 语言包。网页请求只允许打开
用户提交的一个公开 URL；Playwright 不点击、不滚动触发分页、不发现链接。worker 对 HTTP/HTTPS
请求执行公开地址检查，并输出最终 URL；API 删除临时截图和元数据前再次校验最终 URL。

截图识别出的文字由 DeepSeek 拆分为 1–20 条知识，再在同一事务中写入知识表。原始 URL 只进入
来源字段，不进入正文。截图、OCR 文本和 AI 原始响应不写日志、不进对象存储；任一环节失败都
不得创建 URL-only 占位知识。API 到 Nginx 的超时当前为 120 秒，浏览器和 OCR 分别有独立子进程
超时。生产环境必须保持 `KB_WEB_IMPORT_ALLOW_UNBILLED_AI=false`；在 002 的点数报价、预留、
结算和释放接入前，网页 AI 请求必须返回 `402 AI_BILLING_REQUIRED`，不得调用 DeepSeek。
该变量只允许本地自动化或隔离测试环境临时设为 `true`，不得作为生产绕过开关。

用户主动粘贴的剪贴板截图使用同一 Tesseract 中文语言包，但不需要 Playwright。截图会先通过
PNG/JPEG、尺寸、像素和图片内秘密检查，再写临时文件做 OCR；完成后立即删除。最终只保存 AI
拆分后的文字知识，来源类型为 `screenshot`，原截图不进入对象存储或知识附件。

“AI 文字”入口不依赖浏览器或 OCR，但会调用 DeepSeek 整理。文字必须先通过长度和密钥脱敏，
生产环境与其他 AI 入口共用 `KB_WEB_IMPORT_ALLOW_UNBILLED_AI=false` 计费门禁。

## 文档解析依赖

PDF、DOCX、DOC、RTF、TXT 和 Markdown 解析使用 Spring Boot 内嵌 Apache Tika 3.3.3，
不新增系统服务或公共端口。Maven 构建会把 `tika-parsers-standard-package`、PDFBox 和
Apache POI 打入 `kb-app.jar`，因此发布包会明显增大，但部署步骤和 Nginx 路由不变。

- 文档入口的 JPG、PNG 和 WebP 使用受控 libvips 规范化后交给 Tesseract OCR；图片原文件在
  OCR 完成后删除，不进入对象存储或知识附件。
- 单文件上限 25 MiB；解析文字上限 60,000 字符；单次解析超时 25 秒。
- 格式按文件内容检测，不信任扩展名。
- 原始文档和图片仅在请求期处理，不写入对象存储或日志。
- 扫描版 PDF 当前返回 `DOCUMENT_NO_TEXT`，提示改用截图入口。
- 生产未接入 002 点数门禁时必须返回 `402 AI_BILLING_REQUIRED`。

## 一键部署

入口脚本必须支持固定 ref，执行：

1. 验证 GitHub 官方 HTTPS 地址。
2. 记录现有应用健康基线。
3. 检查磁盘、内存、端口、数据库和 Nginx 冲突。
4. 备份当前 release、Nginx、环境文件映射和数据库。
5. 从 GitHub 拉取指定 commit/tag。
6. Maven 构建 Java 与 browser worker、安装 Chromium、验证 Tesseract 语言包、pnpm 构建 Vue，
   并安装或验证 Node 桥接依赖。
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
