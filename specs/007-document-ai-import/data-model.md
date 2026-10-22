# 数据模型：文档解析与 AI 拆分导入

## 临时实体

### DocumentImportInput

| 字段 | 约束 |
|------|------|
| `file` | 单个 Multipart 文件；非空；最大 25 MiB |
| `externalAiConsent` | 必须为 true |
| `note` | 可选；最多 20,000 字符 |

### ExtractedDocument

| 字段 | 约束 |
|------|------|
| `mediaType` | PDF、DOCX、DOC、RTF、TXT、Markdown、JPEG、PNG、WebP |
| `text` | 清洗后文字；1–60,000 字符；文字文档至少 80 个、图片 OCR 至少 30 个字母数字字符 |
| `characterCount` | 与清洗后文字长度一致 |

原始字节、文件名、OCR 临时图片和提取全文均不持久化。图片必须经过安全检查、libvips
方向规范和 Tesseract OCR。

### OrganizedKnowledge

复用网页导入的临时结果：

- `title`：非空，最多 300 字符。
- `body`：非空，最多 10,000 字符，不得只有 URL。
- `summary`：必填，最多 500 字符。
- `category`：必填，最多 100 字符。
- `tags`：至少 1 个，最多 8 个。

## 持久化实体

### KnowledgeItem

- `owner_id`：当前认证账号。
- `title` 和 `body_document`：AI 拆分后的正文。
- `processing_status`：`succeeded`。
- `note`：来源格式、用户备注、AI 摘要/分类/标签和安全处理说明。

### BodySource

- `kind`：`document`。
- `source_url`：null。
- `captured_at`：本次解析时间。

## 状态流

```text
校验同意/计费 -> 读取文件 -> 内容检测
-> （文档：Tika 解析 | 图片：安全检查、libvips 规范化、Tesseract OCR）
-> 文本清洗
-> 密钥脱敏 -> AI 拆分 -> 同事务多知识创建 -> 释放临时数据
```

任一步失败都不创建知识。
