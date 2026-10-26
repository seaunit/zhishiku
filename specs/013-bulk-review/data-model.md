# 数据模型：一键全部复核

**日期**：2026-10-27

本功能不新增数据库表。运行模型如下：

## 批量复核请求

- 目标账号：来自已认证会话，客户端不可指定。
- 目标生命周期：`active`。
- 来源处理状态：`review`。
- 目标处理状态：`succeeded`。
- 事务边界：单次请求一个事务。

## 批量复核结果

| 字段 | 类型 | 规则 |
|---|---|---|
| `reviewed` | 整数 | 本次实际从 `review` 变为 `succeeded` 的数量，最小为 0 |

## 状态转换

```text
active + review -> active + succeeded
```

以下状态不变：

- `not_requested`
- `pending`
- `queued`
- `running`
- `processing`
- `failed`
- `succeeded`
- `recycled`

## 并发规则

- 操作前以悲观写锁读取当前账号的待复核条目。
- 已被其他请求复核的条目不再次修改。
- 重复请求返回当时实际可复核数量，不产生错误。
- 任一条目状态转换失败时事务整体回滚。
