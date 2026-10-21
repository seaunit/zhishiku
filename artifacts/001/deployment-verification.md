# 生产部署验收记录

**日期**：2026-10-21

**部署提交**：`3bda0d82a47d507a821d51e649a9e047ad7c1252`

**运行 release**：`/opt/zhishiku/releases/3bda0d82a47d-20261021012804`

## 部署结果

- 仓库：`https://github.com/seaunit/zhishiku`，公开可见。
- 首次部署入口：GitHub Raw 上的 `deploy/scripts/provision-ubuntu.sh`。
- 应用入口：`https://seaunit.site/kb/`。
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
| `https://seaunit.site/` | HTTP 200 |
| 登录接口 | HTTP 200，返回中文语言、CSRF 和当前所有者 |
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
- 识海使用独立 cookie、数据库、系统用户、环境文件、release、日志标识和上传目录。
- Waffo 支付、AI 分类、网页截图、OCR、外部搜索和完整生命周期尚未部署，当前入口不应
  对外宣称这些能力已经可用。

## 已知风险

- 主机为约 3.4 GiB 内存的小型共享服务器；已增加 2 GiB swap，并限制 Java 堆和构建内存，
  仍需持续监控 OOM、磁盘和搜索延迟。
- 当前 MVP 使用本地文件存储；正式扩容前应迁移到私有 S3 兼容对象存储。
- 数据库迁移为向前兼容，应用 release 可回滚，但已执行的数据库迁移不得直接回退。
