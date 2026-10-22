# 识海部署脚本

部署脚本从 GitHub 拉取指定 ref，不在服务器上使用本地工作树。首次部署和后续更新均支持
重复执行。

## 首次部署

```bash
curl -fsSL https://raw.githubusercontent.com/seaunit/zhishiku/main/deploy/scripts/provision-ubuntu.sh \
  | sudo bash -s -- --ref main
```

生产环境应将 `main` 替换为验收通过的 tag 或完整 commit SHA。

脚本会安装 Java 17、Node.js、pnpm、PostgreSQL 18、libvips 和必要工具，创建独立的
`zhishiku` 用户、数据库、环境文件、systemd 服务、`/kb` Nginx 片段和双应用门户。初始登录信息
写入 `/root/zhishiku-initial-password`，登录后应删除。

`libvips-tools` 是富文本正文图片和剪贴板图片粘贴的必需依赖。更新部署在切换 release 前会
检查 `vips` 命令，并用真实 PNG 执行一次 `autorot` 和元数据清理；缺失或无法运行时立即停止，
不重启现有服务。

新部署也可以不预置账号。任何用户都能在 `/kb/register` 通过图形验证码和 SMTP 邮箱验证创建
独立账号；已有账号不会关闭其他邮箱的注册。每个账号独立隔离知识、附件、搜索、会话、点数、
订单和账本。默认 SMTP 主机为 `smtpdm.aliyun.com:465`，发件与登录账号为
`no-reply@mail.seaunit.site`，可通过 `SMTP_HOST`、`SMTP_PORT`、`SMTP_USERNAME`、
`SMTP_FROM`、`SMTP_FROM_NAME` 和 `SMTP_SSL` 覆盖。生产必须以 TLS 投递。

首次安装会使用 `/swapfile-zhishiku` 补充 2 GiB swap，避免小内存服务器在 Maven 和
Vite 构建期间触发 OOM；可通过 `KB_SWAP_SIZE_MB=0` 禁用。

## 交互式密钥

首次执行会从 `/dev/tty` 隐藏读取以下值，因此可以通过 `curl | sudo bash` 执行：

- `DEEPSEEK_API_KEY`
- `DEEPSEEK_SEARCH_API_KEY`，留空时复用主 AI Key
- `DEEPSEEK_FETCH_API_KEY`，留空时复用主 AI Key
- `SMTP_PASSWORD`

输入内容不会回显，也不会写入 Git、命令行参数或部署日志。值保存在
`/etc/zhishiku/kb.env`，权限为 `600`。每次部署都会重新提示；直接回车保留现有值，
输入新值才更新。
每项输入默认等待 60 秒，超时按回车处理；可通过 `KB_PROMPT_TIMEOUT=<秒数>` 调整。
首次安装和其内部的部署步骤共享一次提示结果，因此同一轮部署只会输入一次。

常规更新部署：

```bash
curl -fsSL https://raw.githubusercontent.com/seaunit/zhishiku/main/deploy/scripts/deploy.sh \
  | sudo bash -s -- --ref v0.1.8-mvp
```

在完全非交互的 CI 中，可先由受管密钥系统写入环境文件，再设置
`KB_PROMPT_SECRETS=0` 执行。

## 后续更新

```bash
curl -fsSL https://raw.githubusercontent.com/seaunit/zhishiku/main/deploy/scripts/deploy.sh \
  | sudo bash -s -- --ref <tag-or-commit>
```

## Waffo 测试与生产配置

首次安装会交互输入 `WAFFO_MERCHANT_ID`、`WAFFO_STORE_ID`、
`WAFFO_PRIVATE_KEY_BASE64`、`WAFFO_PRODUCT_MAP` 和双向内部密钥，写入
`/etc/zhishiku/waffo.env`。`KB_RUNTIME_ENV` 与 `WAFFO_ENV` 必须同为 `test` 或
`production`；生产服务器不得接受测试支付，测试服务器也不得写生产账本。

三个一次性商品使用独立编码 `kb_pack_9`、`kb_pack_29`、`kb_pack_99`，不得复用拾题
`pack_*` 商品。商品初始化不会自动执行，必须在明确确认后运行：

```bash
cd /opt/zhishiku/current/waffo-bridge
set -a
. /etc/zhishiku/waffo.env
set +a
pnpm catalog:setup -- --apply
```

确认商品 ID 后更新 `WAFFO_PRODUCT_MAP`。创建一次真实测试收银台前，先使用现有本地待支付
订单号：

```bash
KB_SMOKE_ORDER_ID=<existing-pending-order-id> \
  pnpm smoke:checkout -- --confirm
```

冒烟脚本不会打印带 token 的完整收银台地址。完成测试卡支付、通知、主动查询、重复通知、
退款和乱序事件验收前，`/etc/zhishiku/kb.env` 中的 `BILLING_ENABLED` 必须保持 `false`。

## 路由边界

- `/`：SEAUNIT 静态门户，提供拾题和识海两个入口。
- `/kb/`：识海 Vue 静态文件。
- `/kb/api/`：受保护的 Spring Boot API。
- `/kb/assets/`：带长期缓存的 Vue 静态资源。
- `/app`、`/login`、`/api`、`/bank`、`/pay`、`/apk`：继续由现有拾题应用处理。

脚本把门户安装到 `/opt/seaunit-portal/releases/`，由 `current` 指向当前版本。门户样式、脚本和
字体使用独立的 `/seaunit-portal/` 路径，不与拾题现有资源冲突。对现有 `shuati.conf` 只做三项
受管理修改：把精确的 `location = /` 替换为静态门户，增加 `/seaunit-portal/` 静态资源
location，并注入一行受管理的 `/kb` `include`。修改前备份，`nginx -t` 失败时自动恢复配置和
上一 release。

## 回滚

应用 release 位于 `/opt/zhishiku/releases/`，静态门户 release 位于
`/opt/seaunit-portal/releases/`；两个 `current` 符号链接分别指向当前版本。健康检查或 Nginx
校验失败时脚本会自动切回上一版本并恢复配置。手工回滚应用：

```bash
ln -sfn /opt/zhishiku/releases/<previous-release> /opt/zhishiku/current.next
mv -Tf /opt/zhishiku/current.next /opt/zhishiku/current
systemctl restart zhishiku-api
```

数据库迁移为向前兼容；回滚应用版本时不删除 PostgreSQL 数据或
`/var/lib/zhishiku/uploads`。
