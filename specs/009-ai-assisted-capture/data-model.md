# 数据模型：AI 辅助采集统一规则

## 采集方式

| 值 | 用户入口 | 是否允许直存 | 处理结果 |
|----|----------|--------------|----------|
| `manual` | 手动录入和编辑 | 是 | 用户原文；`processing_status=not_requested` |
| `ai_text` | AI 辅助文字 | 否 | 脱敏 + AI 整理；`succeeded`，来源可为 `clipboard/native` |
| `webpage` | 网页链接 | 否 | 单页 OCR + AI 整理；`succeeded` |
| `document` | PDF/Word/RTF/TXT/Markdown | 否 | 解析 + AI 整理；`succeeded` |
| `document_image` | 文档入口 JPG/PNG/WebP | 否 | OCR + AI 整理；`succeeded`，不保存原图附件 |
| `screenshot` | 剪贴板截图 | 否 | OCR + AI 整理；`succeeded`，不保存原图附件 |
| `internet_result` | 互联网候选“AI 整理后收入” | 否 | Fetch + AI 整理；`succeeded` |

`document_image` 是产品语义，不新增来源枚举；持久化来源仍为 `document`。

## 知识条目

| 字段 | 规则 |
|------|------|
| `title` | 手动来源为用户标题；非手动来源为 AI 生成标题，最大 300 字符 |
| `body` | 手动来源为用户原文投影；非手动来源为 AI 生成正文 |
| `body_document` | 与正文同事务写入的结构化文档 |
| `processing_status` | 手动为 `not_requested`；非手动为 `succeeded` |
| `source.kind` | 手动来源可用 `native/clipboard/word/google_docs/webpage/legacy_text`；非手动专用值为 `document/screenshot/internet_result` |
| `captured_at` | 非手动来源请求期采集时间 |

## AI 整理结果

| 字段 | 规则 |
|------|------|
| `title` | 非空，1–300 字符 |
| `body` | 非空，不得只有 URL |
| `summary` | 非空 |
| `category` | 非空 |
| `tags` | 至少 1 个，最多 8 个 |
| `items` | 每个请求 1–20 条 |

当前分类、摘要和标签作为知识备注保存；独立分类标签关系表仍属于后续迁移。

## 原文件与临时数据

| 数据类型 | 生命周期 | 持久化 |
|----------|----------|--------|
| 文档二进制 | 当前请求 | 否 |
| 文档提取文字 | 当前请求和 AI 调用 | 只保存 AI 结果 |
| 图片原文件 | 当前请求 | 否 |
| 规范化临时图片 | OCR 调用期 | 否，`finally` 删除 |
| OCR 全文 | 当前请求和 AI 调用 | 不保存 |
| 网页/候选 Fetch 原文 | 当前请求 | 不保存 |
| 原始 URL | 作为来源元数据 | 可保存 |

## 状态与失败

```text
manual -> not_requested
non_manual -> extracted -> redacted -> organized -> succeeded
non_manual failure -> no knowledge item
```

任何非手动状态都不得跳转为直接保存。AI 返回多条知识时，全部写入在同一事务内；任一条失败时
整体回滚。

## 防绕过规则

1. 普通知识创建接口只用于手动录入；显式提交 `document`、`screenshot`、`internet_result`
   或 `text` 来源时拒绝。
2. AI 文字必须使用独立接口，不得把普通手动创建接口伪装成 AI 入口。
3. 专用导入服务只能通过 `createOrganized` 写入非手动知识。
4. 文档图片不得调用富文本图片上传接口作为采集替代。
5. 互联网候选保存不得调用普通知识创建接口。
6. 来源 URL 和文件名只作为元数据，不得代替 AI 正文。
