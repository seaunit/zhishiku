# 研究报告：截图 OCR 与 AI 拆分导入

## OCR 复用

**决策**：抽取 `TesseractOcrService`，网页截图和剪贴板截图共用。

**理由**：统一中文语言包、进程输出排空、超时和错误处理，避免两套 OCR 行为漂移。

## 图片校验

**决策**：复用 `ImageInspector`。

**理由**：已有 PNG/JPEG/WebP 内容识别、8192 单边、4000 万像素和明显字节签名检测。
截图导入额外限制为 PNG/JPEG，保证 Tesseract 兼容性；像素中可见的秘密由 OCR 后文本脱敏处理。

## 原图策略

**决策**：不持久化原图。

**理由**：用户明确要求分析截图内容，而不是保存截图。临时文件只用于 OCR，finally 删除。

## AI 拆分

**决策**：复用 `DeepSeekKnowledgeOrganizer`，sourceLabel 为 `screenshot`。

**理由**：与网页/文档共享 JSON 校验、URL-only 过滤和 1–20 条限制。

## 前端入口

**决策**：继续使用只读 textarea 的 PasteEvent，不增加文件输入。

**理由**：满足剪贴板唯一入口，粘贴预览只存在内存。

## 计费

**决策**：生产沿用 `SHIHAI_WEB_IMPORT_ALLOW_UNBILLED_AI=false` 和 002 门禁。

**理由**：OCR 和 AI 都是付费能力，不能因入口不同绕过计费。
