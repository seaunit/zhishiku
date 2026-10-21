# 阶段 1 数据模型：账户菜单与个人资料

**功能**：`005-account-profile`

**日期**：2026-10-22

**数据库**：PostgreSQL 18

## 实体：账户资料

**用途**：为当前登录账号提供产品内显示名称。

| 字段 | 类型 | 规则 |
|------|------|------|
| `id` | uuid | 既有主键 |
| `email_normalized` | text | 既有登录邮箱，只读且唯一 |
| `display_name` | text | 必填，规范化后 1–40 个 Unicode 字符 |
| `password_hash` | text | 既有字段，任何资料响应都不得返回 |
| `locale` | text | 既有 `zh-CN` 或 `en` 偏好 |
| `email_verified_at` | timestamptz | 既有验证时间 |
| `created_at` / `updated_at` | timestamptz | 既有审计字段 |
| `version` | bigint | 既有乐观锁字段 |

**迁移**：

- 新增 `display_name TEXT`。
- 现有账号回填 `email_normalized` 的 `@` 前部分；超过 40 字符时截断。
- 设置 `NOT NULL`。
- 增加约束：`char_length(btrim(display_name)) BETWEEN 1 AND 40`。

**验证**：

- 去除首尾空白，连续空白折叠为一个普通空格。
- 拒绝 Unicode 控制字符、换行、制表符和 `<`、`>`。
- 长度按 Unicode code point 计算。
- 显示名称不唯一，不参与登录、搜索或计费。

## API 视图

### ProfileResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| `ownerId` | uuid | 当前账号内部标识，仅本人可见 |
| `email` | string | 只读登录邮箱 |
| `displayName` | string | 当前显示名称 |
| `locale` | enum | 当前账号语言 |

不得返回密码哈希、会话 ID、CSRF 以外的令牌或未验证邮箱。

### SessionResponse 扩展

现有 `ownerId`、`email`、`locale`、`csrfToken` 增加 `displayName`。前端用其生成头像首字和
账户菜单摘要。

## 关系与生命周期

- 账户资料与 `owner_account` 一对一，不新增表。
- 资料随账号存在；知识、附件、搜索和支付关系不变。
- 多账号之间不能读取或修改彼此资料。
- 资料更新不触发会话重建；当前 Spring Session 保持有效。
