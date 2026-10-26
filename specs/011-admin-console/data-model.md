# 数据模型：管理员账号管理台

## 账号（`owner_account`）

**用途**：统一保存普通用户和管理员的登录身份、角色与启用状态。

| 字段 | 类型 | 规则 |
|------|------|------|
| `id` | UUID | 主键 |
| `email_normalized` | text | 必填、唯一、规范化小写 |
| `username` | text | 可空；管理员必填；全局唯一；1–64 字符，只允许字母和数字 |
| `display_name` | text | 必填，沿用现有 1–40 字符规则 |
| `password_hash` | text | BCrypt 哈希，不返回前端 |
| `account_role` | text | `USER` 或 `ADMIN`，默认 `USER` |
| `account_status` | text | `enabled` 或 `disabled`，默认 `enabled` |
| `email_verified_at` | timestamptz | 必填 |
| `created_at` / `updated_at` | timestamptz | 必填 |
| `version` | bigint | 乐观并发控制 |

**约束**：

- 用户名唯一索引只覆盖非空值。
- 普通注册不得创建 `ADMIN`。
- 管理员角色账号不得被管理接口禁用。
- 禁用账号不得通过认证。

**状态转换**：

```text
enabled -> disabled -> enabled
```

管理员账号只能保持 `enabled`。

## 管理审计（`admin_audit_event`）

**用途**：记录管理员对账号状态和点数执行的敏感操作。

| 字段 | 类型 | 规则 |
|------|------|------|
| `id` | UUID | 主键 |
| `actor_owner_id` | UUID | 操作管理员 |
| `target_owner_id` | UUID | 目标账号 |
| `action` | text | `account_disabled`、`account_enabled`、`points_granted` |
| `reason_safe` | text | 1–300 字符；禁止秘密 |
| `previous_state` | jsonb | 仅安全状态快照 |
| `next_state` | jsonb | 仅安全状态快照 |
| `idempotency_key` | text | 写操作幂等键 |
| `created_at` | timestamptz | 必填 |

**约束**：

- 不记录密码、令牌、会话 ID、支付签名或知识正文。
- `(actor_owner_id, action, idempotency_key)` 唯一，防止重复审计。
- 审计记录只追加。

## 账号管理摘要

**用途**：管理列表与详情返回的账号元数据和点数聚合。

| 字段 | 类型 | 规则 |
|------|------|------|
| `ownerId` | UUID | 账号 ID |
| `loginName` | text | 有用户名时返回用户名，否则返回邮箱 |
| `email` | text | 管理接口可见的登录邮箱 |
| `displayName` | text | 显示名称 |
| `role` | text | `USER` 或 `ADMIN` |
| `status` | text | `enabled` 或 `disabled` |
| `createdAt` | instant | 注册时间 |
| `knowledgeCount` | long | 当前 active 知识数 |
| `availablePoints` | integer | 可用点数，缺失时为 0 |
| `reservedPoints` | integer | 已预留给未完成 AI 任务的点数 |
| `lifetimeGranted` | integer | 累计发放 |
| `lifetimeConsumed` | integer | 累计成功结算消耗 |

## 点数账户与账本复用

赠送点数沿用以下现有实体：

- `billing_credit_account`：账户可用、预留和累计投影。
- `billing_credit_batch`：`source_type = manual_adjustment` 的新批次。
- `billing_ledger_entry`：`operation_type = manual_adjustment` 的只追加明细。

赠送事务必须同时完成：

1. 锁定目标点数账户。
2. 创建正数 manual adjustment 批次。
3. 增加可用点数、购买点数投影和累计发放。
4. 追加账本，`reference_id` 使用赠送幂等键。
5. 写管理审计。

任一步失败必须整体回滚。

## 索引

- `UNIQUE(owner_account.username) WHERE username IS NOT NULL`
- `owner_account(account_status, created_at DESC)`
- `owner_account(lower(email_normalized) gin_trgm_ops)` 复用已有唯一索引与搜索需要
- `admin_audit_event(actor_owner_id, created_at DESC)`
- `admin_audit_event(target_owner_id, created_at DESC)`
- `UNIQUE(actor_owner_id, action, idempotency_key)`

## 隐私与保留

- 管理列表不返回知识正文、聊天消息、附件内容、密码哈希和会话。
- 管理员只能查看账号级聚合，不获得跨账号知识正文访问能力。
- 审计记录至少保留 24 个月，与财务和安全管理保留约束一致。
- 禁用账号不删除任何点数、订单、退款或知识数据。
