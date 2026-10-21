# 识海部署脚本

部署脚本从 GitHub 拉取指定 ref，不在服务器上使用本地工作树。首次部署和后续更新均支持
重复执行。

## 首次部署

```bash
curl -fsSL https://raw.githubusercontent.com/seaunit/zhishiku/main/deploy/scripts/provision-ubuntu.sh \
  | sudo bash -s -- --ref main
```

生产环境应将 `main` 替换为验收通过的 tag 或完整 commit SHA。

脚本会安装 Java 17、Node.js、pnpm、PostgreSQL 18 和必要工具，创建独立的
`zhishiku` 用户、数据库、环境文件、systemd 服务、`/kb` Nginx 片段和双应用门户。初始登录信息
写入 `/root/zhishiku-initial-password`，登录后应删除。

新部署也可以不预置所有者，在 `/kb/register` 通过 SMTP 邮箱验证完成唯一所有者初始化；已有
所有者后注册自动关闭。默认 SMTP 主机为 `smtpdm.aliyun.com:465`，发件与登录账号为
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

## 路由边界

- `/`：SEAUNIT 静态门户，提供拾题和识海两个入口。
- `/kb/`：识海 Vue 静态文件。
- `/kb/api/`：受保护的 Spring Boot API。
- `/kb/assets/`：带长期缓存的 Vue 静态资源。
- `/app`、`/login`、`/api`、`/bank`、`/pay`、`/apk`：继续由现有拾题应用处理。

脚本把门户安装到 `/opt/seaunit-portal/releases/`，由 `current` 指向当前版本。对现有
`shuati.conf` 只做两项受管理修改：把精确的 `location = /` 替换为静态门户，并注入一行受管理
的 `/kb` `include`。修改前备份，`nginx -t` 失败时自动恢复配置和上一 release。

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
