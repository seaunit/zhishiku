# 研究报告：文档解析与 AI 拆分导入

## 1. 解析器选择

**决策**：Spring Boot 内嵌 Apache Tika 3.3.3 和 `tika-parsers-standard-package`。

**理由**：同一套 API 支持 PDF、DOCX、DOC、RTF、TXT、Markdown，内部使用 PDFBox 和 POI；
无需部署额外服务、端口或密钥。

**未采用方案**：

- 外置 Tika Server：增加进程、认证、网络和运维面。
- 浏览器解析：不能可靠处理 DOC/PDF，且把不可信文件交给前端。
- 仅按扩展名分支：容易绕过安全边界。

## 2. 格式检测

**决策**：`Tika.detect(TikaInputStream, filename)` 检测内容，只接受明确的文档 MIME。

**理由**：扩展名和浏览器 MIME 可伪造；TikaInputStream 能更准确检测 ZIP/OOXML 容器。

## 3. 输出边界

**决策**：输入上限 25 MiB，提取文本上限 60,000 字符，小于 80 个字母数字字符视为无文字。

**理由**：与现有 Multipart 限制一致，避免一次 AI 请求过大；超限失败而不是静默截断，防止
用户误以为整份文档已整理。

## 4. 扫描 PDF

**决策**：P0 不支持自动 OCR；提示使用截图入口。P2 引入逐页 OCR、分块和任务恢复。

**理由**：扫描 OCR 需要页级并发、点数预估和临时图片管理，不适合与首版文字层解析混合。

## 5. AI 拆分

**决策**：复用 `DeepSeekKnowledgeOrganizer`，把 `sourceLabel` 设为 `document`。

**理由**：网页和文档都需要同样严格的 JSON、必填字段、URL-only 过滤和 1–20 条限制。
共享实现减少行为漂移。

## 6. 同意、脱敏和计费

**决策**：与网页导入相同：明确同意、`AiTextSecretRedactor` 和未计费生产门禁。

**理由**：文档可能包含合同、密码和内部信息；不能因为来源不同降低隐私标准。

## 7. 原件保留

**决策**：MVP 不长期保存原始文档，只保存整理知识。

**理由**：避免新增对象存储、保留期限、删除和访问控制。若未来需要原件下载，必须新增安全
附件规格、加密和生命周期。

## 8. 依赖版本

**决策**：Tika 3.3.3，并覆盖 `commons-lang3` 3.21.0。

**理由**：Tika 3.3.3 的 commons-compress 依赖使用新 `SystemProperties` API；Spring Boot 默认
管理的 3.14.0 会在真实检测时触发 `NoSuchMethodError`。固定 3.21.0 并通过真实 DOCX/PDF
测试验证。
