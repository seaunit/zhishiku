# 生产部署验收记录

**日期**：2026-10-22

**部署提交**：`62e69e39553ae0cad8902aba27409823913b15db`

**发布 tag**：`v0.1.18-mvp`

**运行 release**：`/opt/zhishiku/releases/62e69e39553a-20261021214427`

**门户 release**：`/opt/seaunit-portal/releases/62e69e39553a-20261021214427`

## 部署结果

- 仓库：`https://github.com/seaunit/zhishiku`，公开可见。
- 首次部署入口：GitHub Raw 上的 `deploy/scripts/provision-ubuntu.sh`。
- 应用入口：`https://seaunit.site/kb/`。
- 双应用门户：`https://seaunit.site/`，提供拾题和识海入口。
- 识海前端：已按批准原型切换为深海蓝图标轨、移动底栏、知识海图和双栏详情。
- 注册：只要求邮箱验证码，不显示图形验证码；登录仍保留图形验证码。
- 密码：登录、注册和初始化账号统一为 8–256 位。
- 采集：恢复手动录入、网页链接、文档和截图四个入口；PDF/Word 解析和网页 AI 截图分析待接入。
- API 前缀：`https://seaunit.site/kb/api/`。
- 健康端点：`https://seaunit.site/kb/api/actuator/health`。
- 数据库：独立 PostgreSQL 18.6 数据库 `zhishiku`。
- 附件：`/var/lib/zhishiku/uploads`。
- 反向代理：现有 Nginx 增量加载 `/etc/nginx/snippets/zhishiku-kb.conf`。

## 验收证据

| 检查项 | 结果 |
|--------|------|
| `https://seaunit.site/kb/` | HTTP 200 |
| `https://seaunit.site/kb/login` | HTTP 200 |
| `https://seaunit.site/kb/api/actuator/health` | HTTP 200，`status=UP` |
| `https://seaunit.site/app` | HTTP 200 |
| `https://seaunit.site/` | HTTP 200，显示“拾题与识海”和两个应用入口 |
| `https://seaunit.site/login` | HTTP 200 |
| `https://seaunit.site/bank/1` | HTTP 200 |
| `/kb/login` 注册入口 | 始终显示“注册账户”；已有账号不关闭其他邮箱注册 |
| `/kb/api/v1/auth/registration-status` | HTTP 200，`{"open":true}` |
| `/kb/login` 生产 Taste 检查 | 1440/768/390 全部 `0 FAIL` |
| `owner_account.instance_key` | 列数 0，实例级唯一账号约束不存在 |
| Flyway | 最新成功迁移 `0006`，现有账号保留 |
| `https://seaunit.site/terms` | HTTP 200 |
| `https://seaunit.site/privacy` | HTTP 200 |
| `/seaunit-portal/styles.css` | HTTP 200，`text/css` |
| `/seaunit-portal/app.js` | HTTP 200，`application/javascript` |
| `/seaunit-portal/fonts/libre-franklin.woff2` | HTTP 200，`font/woff2` |
| 登录接口 | HTTP 200，返回当前登录账号的语言、CSRF 和账号 ID |
| `zhishiku-api.service` | active |
| Nginx | active，`nginx -t` 通过 |
| `shuati.service` | active |
| `shuati-payments.service` | active |
| 初始凭据文件权限 | `600` |
| 自动所有者初始化 | 首次成功后已切换为 `false` |

## 运行边界

- Spring Boot 仅监听 `127.0.0.1:8082`。
- Vue 构建产物由 Nginx 提供，不运行生产 Node Web 服务器。
- 现有拾题应用保持 `8080`、MySQL、Redis、`/app`、`/api`、`/bank`、`/pay` 和 `/apk`
  路由不变。
- 门户只接管精确的 `/` 和独立的 `/seaunit-portal/` 静态资源前缀；`/app`、`/login`、
  `/bank`、`/api`、`/pay` 和 `/apk` 继续由拾题处理。
- 识海使用独立 cookie、数据库、系统用户、环境文件、release、日志标识和上传目录。
- Waffo 支付、AI 分类、网页截图、OCR、外部搜索和完整生命周期尚未部署，当前入口不应
  对外宣称这些能力已经可用。

## 已知风险

- 主机为约 3.4 GiB 内存的小型共享服务器；已增加 2 GiB swap，并限制 Java 堆和构建内存，
  仍需持续监控 OOM、磁盘和搜索延迟。
- 当前 MVP 使用本地文件存储；正式扩容前应迁移到私有 S3 兼容对象存储。
- 数据库迁移为向前兼容，应用 release 可回滚，但已执行的数据库迁移不得直接回退。
