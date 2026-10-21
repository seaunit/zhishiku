# 多账号注册与数据隔离验收

**功能**：`001-ai-personal-knowledge-base`

**日期**：2026-10-22

**环境**：Windows、Java 17.0.18、PostgreSQL 18.1、独立数据库
`zhishiku_multi_account_20261022`、Spring Boot local profile、Vite 前端。

## 迁移结果

- Flyway 成功执行 `0001 → 0004 → 0005 → 0006`。
- `owner_account.instance_key` 列已删除，实例级唯一账号约束不再存在。
- `email_normalized` 保持唯一约束，重复邮箱不能创建第二个账号。
- `email_verified_at` 已添加，现有账号回填为创建时间。
- 独立数据库中创建了引导账号和两个测试账号，共 3 个账号。

## 账号与隔离结果

| 检查 | 结果 |
|------|------|
| 已初始化实例的 `registration-status` | `open=true` |
| 注册账号 A | HTTP 201 |
| 账号 A 创建知识 | HTTP 201 |
| 账号 B 列表 | HTTP 200，0 条 |
| 账号 B 直接读取账号 A 的知识 | HTTP 400 |
| 账号 B 搜索账号 A 的正文关键词 | HTTP 200，0 条 |
| 账号 B 登录会话 | 返回账号 B 自己的 `ownerId` |

数据库核验：

- `owner_account` 记录数：3。
- `instance_key` 列数：0。
- `email_verified_at` 非空账号数：3。
- 知识条目只归属账号 A，账号 B 无法通过列表、详情或搜索读取。

## 自动化

```powershell
pnpm exec playwright test --config ..\..\tests\playwright.config.ts \
  tests/e2e/multi-account-isolation.spec.ts --workers=1
```

结果：1 项通过。

```powershell
cd backend
.\mvnw.cmd -pl kb-app -am test
.\mvnw.cmd spotless:check
```

结果：kb-core 18 项、kb-app 93 项，共 111 项通过；Spotless 通过。

前端结果：lint、typecheck、Vitest 41 项、注册相关 Playwright 3 项和生产构建通过。

## 结论

运行行为已从“单所有者实例”切换为“多账号公开注册”，数据仍使用兼容字段
`owner_id` 按当前认证账号隔离。生产部署和迁移尚未在本记录日期执行。
