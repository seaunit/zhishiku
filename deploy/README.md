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
`zhishiku` 用户、数据库、环境文件、systemd 服务和 `/kb` Nginx 片段。初始登录信息写入
`/root/zhishiku-initial-password`，登录后应删除。

首次安装会使用 `/swapfile-zhishiku` 补充 2 GiB swap，避免小内存服务器在 Maven 和
Vite 构建期间触发 OOM；可通过 `KB_SWAP_SIZE_MB=0` 禁用。

## 交互式密钥

首次执行会从 `/dev/tty` 隐藏读取以下值，因此可以通过 `curl | sudo bash` 执行：

- `DEEPSEEK_API_KEY`
- `DEEPSEEK_SEARCH_API_KEY`，留空时复用主 AI Key
- `DEEPSEEK_FETCH_API_KEY`，留空时复用主 AI Key
- `SMTP_PASSWORD`

输入内容不会回显，也不会写入 Git、命令行参数或部署日志。值保存在
`/etc/zhishiku/kb.env`，权限为 `600`。已有值默认保留。

需要轮换时执行：

```bash
curl -fsSL https://raw.githubusercontent.com/seaunit/zhishiku/main/deploy/scripts/deploy.sh \
  | sudo env KB_ROTATE_SECRETS=1 bash -s -- --ref v0.1.1-mvp
```

在完全非交互的 CI 中，可先由受管密钥系统写入环境文件，再设置
`KB_PROMPT_SECRETS=0` 执行。

## 后续更新

```bash
curl -fsSL https://raw.githubusercontent.com/seaunit/zhishiku/main/deploy/scripts/deploy.sh \
  | sudo bash -s -- --ref <tag-or-commit>
```

## 路由边界

- `/kb/`：识海 Vue 静态文件。
- `/kb/api/`：受保护的 Spring Boot API。
- `/kb/assets/`：带长期缓存的 Vue 静态资源。
- `/`、`/app`、`/api`、`/bank`、`/pay`、`/apk`：继续由现有拾题应用处理。

脚本只向现有 `shuati.conf` 注入一行受管理的 `include`。注入前备份，`nginx -t` 失败时
自动恢复。

## 回滚

应用 release 位于 `/opt/zhishiku/releases/`，`/opt/zhishiku/current` 指向当前版本。
健康检查失败时脚本会自动切回上一版本。手工回滚：

```bash
ln -sfn /opt/zhishiku/releases/<previous-release> /opt/zhishiku/current.next
mv -Tf /opt/zhishiku/current.next /opt/zhishiku/current
systemctl restart zhishiku-api
```

数据库迁移为向前兼容；回滚应用版本时不删除 PostgreSQL 数据或
`/var/lib/zhishiku/uploads`。
