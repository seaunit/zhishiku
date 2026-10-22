# 数据模型：截图 OCR 与 AI 拆分导入

## 临时实体

### ScreenshotImportInput

| 字段 | 约束 |
|------|------|
| `file` | 单个 Multipart PNG/JPEG；最大 25 MiB |
| `externalAiConsent` | 必须为 true |
| `note` | 可选，最多 20,000 字符 |

### ImageInspection

- `mediaType`：image/png 或 image/jpeg。
- `width/height`：最大 8192；总像素最多 4000 万。
- 原始字节中的高置信密钥必须拒绝。

### ExtractedScreenshot

- `text`：OCR 清洗结果，1–30,000 字符。
- `mediaType`：PNG 或 JPEG。
- `formatLabel`：PNG 截图或 JPEG 截图。
- 原图和 OCR 文本均不持久化。

## 持久化知识

- `BodySource.kind=screenshot`
- `BodySource.source_url=null`
- `captured_at`：OCR 采集时间
- `processing_status=succeeded`
- 正文为 AI 拆分的 ProseMirror 段落
- `note` 包含来源格式、用户备注、AI 元数据和安全处理说明

不创建附件、不保存图片对象键、不创建图片正文节点。
