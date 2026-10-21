# 账户菜单与个人资料验收记录

**日期**：2026-10-22

**功能**：`005-account-profile`

## 已实现

- 点击头像只打开账户菜单，不再直接退出。
- 菜单显示当前显示名称和邮箱，提供“个人资料”和“退出登录”。
- 菜单支持点击外部关闭、`Esc` 关闭、焦点返回、方向键导航和 44px 触控项。
- `/kb/profile` 展示头像首字、显示名称、只读邮箱和账户边界。
- 显示名称保存后立即同步顶部头像和账户菜单。
- 退出登录先显示确认对话框，取消和 `Esc` 保持会话，确认后失效并进入登录页。
- 页面刷新通过 `/auth/me` 恢复当前会话资料。

## 后端验证

| 检查 | 结果 |
|------|------|
| 显示名称规范化、Unicode 长度、控制字符与 HTML 片段拒绝 | 通过 |
| 当前账号资料读取和更新隔离 | 通过 |
| 未登录访问返回 401 | 通过 |
| PATCH CSRF 校验 | 通过 |
| 响应不包含密码哈希或会话令牌 | 通过 |
| SessionResponse 包含 displayName | 通过 |
| V0007 迁移契约 | 通过 |
| `mvnw spotless:check` | 通过 |
| `mvnw -pl kb-app -am test` | kb-core 18 项、kb-app 113 项，共 131 项通过 |

Testcontainers PostgreSQL 测试因本机无 Docker 跳过，迁移将在生产部署时由 Flyway 实执行验证。

## 前端验证

| 检查 | 结果 |
|------|------|
| `pnpm lint` | 通过 |
| `pnpm typecheck` | 通过 |
| `pnpm test` | 41 项通过 |
| `pnpm build` | 通过 |
| `pnpm exec playwright test e2e/app.spec.ts --workers=1` | 28 项通过 |
| 320px 菜单和资料页 | 无横向溢出，菜单项不小于 44px |
| 原型账号交互脚本 | 资料保存同步、退出确认和取消通过 |
| Taste 源 lint | `0 FAIL / 0 WARN` |
| Taste App Screen | `0 FAIL`，保留既有导航 `tiny-text` WARN |

## 生产验证

待 GitHub 提交和部署后填写。

## 已知边界

- 本次只编辑显示名称，不上传头像、不修改邮箱或密码。
- 显示名称不参与登录、搜索、AI 或计费。
- 多账号通过退出后登录另一账号验证，不提供并行账号切换。
