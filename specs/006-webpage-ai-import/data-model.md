# 数据模型：网页截图 AI 拆分导入

## 建模原则

- URL、截图和 OCR 文本属于采集输入，不等同于知识正文。
- 临时截图和最终 URL 元数据只在一次请求内存在，结束后删除。
- 只有 AI 整理并通过校验的知识条目进入现有 `knowledge_item` 模型。
- 全部持久化记录按当前认证账号隔离，客户端不能提交替代账号 ID。
- 本功能不新增数据库迁移；分类和标签在 MVP 中复用备注文本。

## 临时实体

### WebPageCapture

| 字段 | 类型 | 约束 |
|------|------|------|
| `requestedUrl` | string | 清理后的公开 HTTP/HTTPS URL；2–2048 字符；无凭据和片段 |
| `finalUrl` | string | worker 重定向后的 URL；必须再次通过公开地址策略 |
| `screenshotPath` | path | 服务端临时 PNG；最大 50 MiB；请求结束删除 |
| `metadataPath` | path | 临时文本文件，只含最终 URL；请求结束删除 |
| `visibleText` | string | OCR 结果；最多 30,000 字符；不可信输入 |
| `externalAiConsent` | boolean | 必须为 true；未同意时不启动截图和外部 AI |
| `redactionApplied` | boolean | OCR 文本发现并替换疑似秘密时为 true |
| `capturedAt` | instant | 同一页面生成的所有知识共享同一采集时间 |

**状态流**：

```text
URL 校验 -> 浏览器渲染 -> 最终 URL 校验 -> OCR -> AI 整理 -> 持久化 -> 删除临时文件
```

任一步失败都进入拒绝状态，不创建 `knowledge_item`。

### OrganizedKnowledge

| 字段 | 类型 | 约束 |
|------|------|------|
| `title` | string | 非空；最多 300 字符；同一结果内去重 |
| `body` | string | 非空；最多 10,000 字符；纯文本段落，不得只有 URL |
| `summary` | string | 最多 500 字符 |
| `category` | string | 最多 100 字符 |
| `tags` | string[] | 最多 8 个；单标签最多 50 字符；去除空值和重复值 |

单次 AI 结果最多 20 条。空标题、空正文和重复标题被过滤；过滤后为空则整次导入失败。

## 持久化实体

### KnowledgeItem

复用现有模型，本功能涉及的字段如下：

| 字段 | 本功能含义 |
|------|------------|
| `id` | 新知识 UUID |
| `owner_id` | 当前认证账号 ID |
| `title` | AI 整理标题 |
| `body_document` | 从 AI 纯文本构造的安全 ProseMirror `doc` |
| `body_text` | 派生纯文本投影，用于搜索和 AI |
| `note` | `AI 摘要`、`AI 分类`、`AI 标签` 三部分安全文本 |
| `processing_status` | 网页整理成功后固定为 `succeeded`，不得为 `not_requested` |
| `created_at` / `updated_at` | 数据库时间 |

### BodySource

| 字段 | 本功能含义 |
|------|------------|
| `kind` | 固定为 `webpage` |
| `source_url` | 用户提交并清理后的原始 URL，不使用最终 URL 替换 |
| `captured_at` | 本次页面采集时间；同一页面的多条知识一致 |
| `converted_at` | 由现有来源服务记录 |
| `converter_version` | 由现有来源服务记录 |
| `content_hash` | 由现有知识保存流程计算 |

## 关系

```text
认证账号 1 ── N 知识条目
知识条目 1 ── 1 正文来源
一次网页采集 1 ── N 知识条目
```

“一次网页采集”不单独持久化，通过同一 `source_url` 和 `captured_at` 可追踪。

## 数据安全规则

- 不持久化截图、OCR 原始输出、AI 原始 JSON、提示词或 API Key。
- 日志只能记录安全错误码、请求 ID、耗时和条目数量。
- `source_url` 不得包含用户名、密码、控制字符或片段标识。
- 正文不得包含 URL-only 占位文本。
- AI 返回的 HTML、脚本和危险链接不直接渲染；MVP 只接受纯文本并构造段落节点。
- OCR 文本必须在发送给外部 AI 前扫描常见 API Key、Token、密码赋值和私钥，并替换为
  `[REDACTED]`；知识备注只说明发生过安全处理，不保存原始秘密。
- 同一账号的多条知识共享一次采集，但不得跨账号读取或复用来源。

## 后续演进

P2 引入分类和标签关系表时，增加：

- `category`、`tag` 规范词表。
- `knowledge_category_assignment`、`knowledge_tag_assignment`。
- 置信度、来源、暂定/锁定状态和用户修订历史。
- `web_import_job` 持久任务表，用于异步重试、幂等和站点级限流。

迁移必须保留现有 `note` 中的摘要、分类和标签文本，并按可审计规则转换。
