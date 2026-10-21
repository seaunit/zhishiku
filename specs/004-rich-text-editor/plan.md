# 实施计划：识海富文本知识编辑

**功能**：`004-rich-text-editor`

**日期**：2026-10-21

**规格**：[spec.md](spec.md)

**上游功能**：
[001 个人知识库](../001-ai-personal-knowledge-base/plan.md)、
[002 点数计费](../002-ai-credit-billing/plan.md)、
[003 移动网页](../003-mobile-web-access/spec.md)

## 摘要

把手动采集和知识编辑从纯文本升级为受限、语义化富文本，支持正文图片、粘贴转换、富文本
详情、派生纯文本搜索和 AI 输入。前端使用 Tiptap Vue 3 + ProseMirror JSON；后端使用
Spring Boot、Jackson 和 JSON Schema/领域验证器执行同一白名单。图片继续经过附件扫描、
密钥检测和脱敏门禁，正文只保存安全资产引用。

## 技术背景

**前端**：Vue 3、TypeScript、Vite、Tiptap Vue 3、ProseMirror、Reka UI、Lucide Vue、
vue-i18n

**后端**：Java 21、Spring Boot 3.x、Jackson、Spring Data JPA、`JdbcClient`、
networknt JSON Schema Validator、Springdoc OpenAPI

**存储**：PostgreSQL 18 保存结构化 JSONB、纯文本投影、修订、来源、转换报告和图片引用；
S3 兼容对象存储保存安全附件

**搜索**：PostgreSQL `search_document` 投影；Meilisearch 仅在 001 搜索基准失败后启用

**平台工具**：libvips、Tesseract、Apache Tika/PDFBox、AWS SDK for Java v2

**测试**：JUnit 5、AssertJ、Testcontainers、RestAssured、Vitest、Vue Test Utils、
Playwright、axe-core

**目标平台**：桌面、平板和 320–430 CSS 像素手机浏览器

## 宪法检查

| 门禁 | 设计前 | 设计后 | 证据 |
|------|--------|--------|------|
| SDD 与中文文档 | 通过 | 通过 | 规格、研究、数据、契约、快速验证和任务均中文 |
| 产品/开发/测试分离 | 通过 | 通过 | 产品冻结格式，开发实现 schema，测试独立验证安全 |
| 默认中文、完整英文 | 通过 | 通过 | Vue/Spring 消息完整覆盖编辑器、错误和图片说明 |
| 知识可追溯 | 通过 | 通过 | 文档修订、来源、转换报告和纯文本投影独立保存 |
| Taste 视觉验证 | 通过 | 通过 | 1440/768/390/360 真实浏览器检查 |
| 禁止爬虫与密钥安全 | 通过 | 通过 | 不自动抓取图片；远程图片不渲染；附件 fail-closed |
| GitHub 一键部署 | 通过 | 通过 | Maven、Vite、Flyway 和固定 ref 部署 |
| 拾题零影响 | 通过 | 通过 | 只修改 `/kb` 的 Vue、Spring API、数据库和对象存储 |
| 任务可追溯 | 通过 | 通过 | 每项任务记录摘要、验证和 SHA |
| 点数完整性 | 通过 | 通过 | 唯一图片引用接入 002 报价、预留和结算 |

无需违反章程。

## 架构决策

1. **ProseMirror JSON 是正文权威格式。**
   Tiptap Vue 只生成受限节点/标记。Spring Boot 使用共享 JSON Schema 和领域验证器重新
   验证、规范化、重建纯文本，不接受任意 HTML。

2. **前后端共享语言中立契约。**
   `rich-text.schema.json` 和 OpenAPI 是权威契约。Vue 使用 TypeScript 类型，Spring 使用
   Jackson DTO + networknt 校验；契约测试同时覆盖两端。

3. **正文资产使用统一 `assetId`。**
   编辑提交时可为临时 `uploadId`；Spring 保存事务解析后持久化为正式 `attachmentId`。
   临时 ID 不得落盘或出现在读取响应中。

4. **正文、来源、投影和引用同事务提交。**
   Spring 事务同时写 `rich_document`、修订、来源、转换报告、图片引用、纯文本投影和
   `knowledge_item.search_revision`。

5. **图片 fail-closed。**
   任一图片未扫描、脱敏不完整或格式不安全时，整次保存失败；当前未提交编辑内容保留在
   Vue 编辑器和安全草稿中。

6. **附件处理使用平台工具。**
   libvips 负责方向、HEIC/HEIF 静态化、缩放和元数据清理；Tesseract 负责 OCR；
   Spring 负责内容哈希、状态、对象提升和配额。

7. **来源与转换独立留痕。**
   记录来源类型、清理后 HTTP/HTTPS URL、采集时间、转换时间、converter version、哈希和
   格式转换报告。正文不保存 Cookie、Authorization、完整剪贴板或原始 HTML。

8. **详情从结构化文档渲染。**
   Vue 组件只接受受验证节点，图片通过 `/kb/api` 安全附件端点加载。不得使用
   `v-html` 直接渲染用户 HTML。

9. **唯一图片 AI 计费。**
   用户明确选择图片分析时，004 将去重后的 `imageAssetIds` 交给 002；Spring 服务端按
   `content_hash` 再次去重，重复节点只计一次。

10. **旧数据迁移是独立过渡。**
    `knowledge_item.body_migration_state` 为 `pending/failed` 时返回 `bodyDocument=null`，
    详情只读显示纯文本；迁移完成后再开放富文本编辑。

## 项目结构

```text
backend/kb-core/src/main/java/site/seaunit/zhishiku/core/richtext/
backend/kb-app/src/main/java/site/seaunit/zhishiku/richtext/
backend/kb-app/src/main/resources/db/migration/V0004__rich_text.sql
frontend/kb-web/src/components/editor/
frontend/kb-web/src/components/knowledge/
frontend/kb-web/src/composables/editor/
specs/004-rich-text-editor/contracts/
```

## 数据与接口

- 实体与约束见 [data-model.md](data-model.md)。
- 文档 schema 见 [contracts/rich-text.schema.json](contracts/rich-text.schema.json)。
- 图片暂存接口见 [contracts/image-upload.openapi.yaml](contracts/image-upload.openapi.yaml)。
- 001 的详情返回完整 `bodyDocument`；列表和搜索只返回摘要 DTO。
- 429 请求体包含 `expectedVersion`；唯一客户端锁是 `knowledge_item.version`。
- 冲突响应使用 `RICH_TEXT_VERSION_CONFLICT`，客户端可选择重载、另存或明确覆盖。
- 图片 AI 报价请求携带去重后的 `imageAssetIds`；002 返回唯一资源清单和内容哈希。

## 图片与文档限制

- 每条知识最多 20 张正文图片。
- 单图源文件不超过 25 MiB；处理后合计不超过 50 MiB。
- 每边不超过 8192 像素，总解码像素不超过 4000 万。
- 支持 JPEG、PNG、WebP、HEIC/HEIF；SVG、动画和未扫描内容拒绝或安全静态化。
- 文档入口接受文档和图片文件；截图入口只接受剪贴板图片。

## 搜索、AI 与移动端

- PostgreSQL 搜索投影包含正文、表格、代码、图片说明和替代文本。
- 搜索不索引标签名、附件键、隐藏样式、脚本或内部 JSON。
- AI 文本请求只包含纯文本投影；图片分析必须引用 002 计费任务。
- Vue 工具栏在 320–430px 保留 44px 触控目标、选区、焦点和安全区。
- 表格 P1 能力使用 Tiptap Table 扩展，并执行窄屏局内滚动和无障碍导航。

## 实施顺序

### P0 MVP

1. 契约、JSONB/修订/来源/图片引用和纯文本投影。
2. Vue 基础编辑器、正文图片、详情和移动布局。
3. 附件扫描/脱敏、全次保存失败和正式附件解析。
4. 搜索/AI 投影、唯一图片报价、基础键盘和屏幕阅读器。

### P1 完整发布

1. Word/网页粘贴、表格、拖放和编辑器内图片粘贴。
2. 冲突处理、旧数据迁移、来源追溯和生命周期清理。
3. 完整移动工具栏、双语、Taste、零影响和性能门禁。

### P2 持续加固

1. 浏览器刷新和进程重启恢复。
2. 可用性研究、schema 演进和迁移回滚演练。

## 验证策略

- JUnit/JSON Schema：未知节点、超限、危险链接、表格语义和来源规则。
- Testcontainers：文档、修订、来源、图片引用和搜索修订原子提交。
- RestAssured：001/004 OpenAPI 和冲突/错误契约。
- Vitest：编辑器状态、工具栏、详情组件和双语。
- Playwright：格式、图片、粘贴、详情、移动、冲突和无障碍。
- 安全：远程图片网络请求为 0、脚本/iframe/表单不执行、密钥金丝雀不落盘。
- 视觉：Taste 在 1440/768/390/360 检查。

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| Vue 与 Spring schema 漂移 | 语言中立 JSON Schema、共享夹具和双端契约测试 |
| 临时 uploadId 落盘 | 保存事务解析、数据库检查、响应断言和负向测试 |
| 远程图片追踪 | Vue 不渲染非白名单来源；CSP；网络请求计数为 0 |
| HEIC/libvips 依赖缺失 | 启动探测、固定版本、失败关闭和明确占位 |
| 大正文造成列表响应膨胀 | 摘要 DTO，详情才返回完整文档 |
| 表格语义不统一 | 首行 header、其余 data 的 schema 和领域验证 |
| 旧数据迁移阻塞 | `bodyDocument=null` 过渡、只读纯文本和独立迁移状态 |
| 重复图片重复计费 | 服务端按内容哈希去重并回传唯一资源 |

## 复杂度跟踪

结构化文档、来源报告和独立图片暂存是安全与可追溯性的必要条件。没有新增运行时服务；
富文本复用 Vue、Spring 和 PostgreSQL，附件复用 S3 与平台图像/文档工具。
