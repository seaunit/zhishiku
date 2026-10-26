# 数据模型：Dify AI 编排

**日期**：2026-10-27

本功能不新增数据库表，核心模型为运行时配置、请求、结果和调用审计。

## AI 编排配置

| 字段 | 类型 | 必填 | 规则 |
|---|---|---:|---|
| `provider` | 枚举 | 是 | `dify` 或 `deepseek`；生产默认 `deepseek`，本地测试显式 `dify` |
| `difyBaseUrl` | URL | Dify 时必填 | 指向 Dify API 根路径，不含密钥 |
| `organizerApiKey` | 密钥引用 | Dify 时必填 | 仅从环境变量读取，不持久化到仓库 |
| `chatApiKey` | 密钥引用 | Dify 时必填 | 仅从环境变量读取，不复用拾题 Key |
| `timeoutSeconds` | 整数 | 是 | 10–180 秒 |
| `maxRetries` | 整数 | 是 | 0–2；流式响应发出后不得重试 |

## 知识整理请求

| 字段 | 类型 | 必填 | 规则 |
|---|---|---:|---|
| `operation` | 字符串 | 是 | 固定 `knowledge_organize` |
| `sourceKind` | 字符串 | 是 | 已受控的来源标签，最长 100 字符 |
| `content` | 字符串 | 是 | 已授权、脱敏且通过现有长度校验的文本 |
| `externalUser` | 字符串 | 是 | 账号 UUID，不含邮箱或其他直接身份信息 |

## 知识整理结果

| 字段 | 类型 | 必填 | 规则 |
|---|---|---:|---|
| `items` | 数组 | 是 | 1–20 条 |
| `items[].title` | 字符串 | 是 | 1–300 字符，不重复 |
| `items[].body` | Markdown | 是 | 1–10,000 字符，必须存在非 URL 有效内容 |
| `items[].summary` | 字符串 | 是 | 1–500 字符 |
| `items[].category` | 字符串 | 是 | 1–100 字符 |
| `items[].tags` | 字符串数组 | 是 | 1–8 个，每个 1–50 字符 |

状态变化：

1. 请求进入：`pending`。
2. Dify 返回：`received`。
3. 后端校验通过：`accepted`。
4. 落库成功：`persisted`。
5. 任一步失败：`failed`，不得产生部分知识。

## 对话编排请求

| 字段 | 类型 | 必填 | 规则 |
|---|---|---:|---|
| `operation` | 枚举 | 是 | `knowledge_answer`、`general_answer`、`query_plan`、`internet_answer`、`stream_knowledge`、`stream_general`、`stream_internet` |
| `question` | 字符串 | 是 | 已脱敏，最长 300 字符 |
| `history` | 文本 | 否 | 已脱敏短历史，最长 8,000 字符 |
| `evidence` | 文本 | 否 | 已脱敏候选证据，受现有上限控制 |
| `externalUser` | 字符串 | 是 | 账号 UUID |

## 对话编排结果

| 字段 | 类型 | 必填 | 规则 |
|---|---|---:|---|
| `text` | 字符串 | 是 | 非流式最多 6,000 字符；流式由后端累计后校验 |
| `runId` | 字符串 | 否 | 仅用于脱敏审计，不返回浏览器 |
| `status` | 枚举 | 是 | `succeeded`、`failed`、`stopped` |
| `elapsedMs` | 整数 | 是 | 非负 |

引用、联网判断、计费结算和会话写入不属于 Dify 结果，保持为后端状态。

## 调用审计

调用审计只记录：

- 提供商与操作类型。
- 当前账号 UUID 的哈希或内部关联键。
- 开始时间、耗时、HTTP 分类和 Dify 运行 ID。
- 成功、超时、协议错误、输出校验失败或服务不可用等脱敏状态码。

不得记录请求正文、问题、证据、密钥、Cookie、响应原文或完整异常堆栈中的敏感值。
