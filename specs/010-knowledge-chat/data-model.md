# 数据模型：知识库聊天

## 本地检索结果

| 字段 | 类型 | 约束 |
|---|---|---|
| `knowledgeId` | UUID | 当前账号 `active` 知识 |
| `title` | string | 1–300 字符 |
| `excerpt` | string | 最多 600 字符，已移除明显密钥 |
| `matchedFields` | string[] | 标题、正文、备注、图片、分类或标签 |

## 聊天请求

### 本地检索

- `query`：1–500 字符，去除首尾空白。
- 不调用外部 AI，不扣点。

### 回答请求

- `query`：1–500 字符。
- `history`：最多 6 条，每条最多 2,000 字符。
- `externalAiConsent`：必须为 true 或已有 `chat` 授权。
- `billingTaskId`：已按 `knowledge_chat` 报价确认并预留 1 点。

### 检索计划

- `queries`：AI 生成的 1–8 个检索短语，只用于当前请求。
- `entities`：问题中的主体、地区、组织和产品名称。
- `constraints`：时间、政策、否定、范围和比较条件。
- `synonyms`：中英文同义表达和相关术语。

### 回答结果

| 字段 | 类型 | 约束 |
|---|---|---|
| `answer` | string | 最多 8,000 字符；知识增强或通用 AI 均无法回答时为空 |
| `needsInternet` | boolean | true 时释放回答预留 |
| `sources` | source[] | 最多 5 条，只允许本轮实际使用的知识 ID；通用回答为空 |

### 流式事件

| 事件 | 载荷 |
|---|---|
| `token` | `{ "text": "增量文本" }` |
| `complete` | `{ "answer", "needsInternet", "sources", "reason" }` |
| `error` | `{ "code", "message", "retryable" }` |

流式完成后才追加完整 assistant 消息。知识增强回答必须通过引用校验，通用回答必须非空且不含
内部联网控制标记；客户端断连、服务端异常、空回答、知识引用非法或明确需要联网时，只释放计费
预留，不写入部分回答。

## 互联网来源

复用 ExternalSearch 候选：`title`、`url`、`siteName`、`snippet`。只有公开 HTTP(S) 来源可以
展示；不持久化搜索正文。

## 计费状态

- `knowledge_chat`：报价 1 点；知识增强或通用 AI 成功答案结算，两者都无法回答或失败时释放。
- `internet_search_with_answer`：搜索 1 点 + 回答 1 点；分别按有效步骤结算。

## 会话生命周期

### 聊天对话

| 字段 | 类型 | 约束 |
|---|---|---|
| `id` | UUID | 主键 |
| `ownerId` | UUID | 对话所属账号，所有读取和写入必须匹配当前会话 |
| `title` | string | 1–100 字符，默认取首条问题 |
| `createdAt` | instant | 创建时间 |
| `updatedAt` | instant | 最近一条消息时间，用于排序 |

### 聊天消息

| 字段 | 类型 | 约束 |
|---|---|---|
| `id` | UUID | 主键 |
| `conversationId` | UUID | 所属对话 |
| `ownerId` | UUID | 冗余账号边界，必须与对话账号一致 |
| `role` | enum | `user` 或 `assistant` |
| `content` | string | 1–8,000 字符 |
| `sources` | source[] | 最多 5 条，持久化当时的引用快照；通用回答为空；二次召回候选池最多 8 条 |
| `needsInternet` | boolean | 恢复后仍能识别当时需要联网才能继续回答的状态 |
| `reason` | string/null | 最多 100 字符 |
| `createdAt` | instant | 消息时间 |

对话在用户主动删除前保留。列表按 `updatedAt` 倒序返回最近 20 个对话，详情单次最多返回最近
200 条消息。模型上下文仍只使用最近 6 条消息。
