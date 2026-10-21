# 任务：识海富文本知识编辑

**输入**：`specs/004-rich-text-editor/`

**前置条件**：[spec.md](spec.md)、[plan.md](plan.md)、[research.md](research.md)、
[data-model.md](data-model.md)、[contracts/rich-text.schema.json](contracts/rich-text.schema.json)、
[contracts/image-upload.openapi.yaml](contracts/image-upload.openapi.yaml)、[quickstart.md](quickstart.md)

**测试**：已纳入并遵循测试先行。富文本涉及 XSS、远程图片、附件安全、来源追溯、唯一图片计费、
迁移和移动编辑，必须先写失败测试或安全夹具，再由测试子智能体独立验收。

## 优先级

- **P0 — 富文本核心 MVP**：安全结构化文档、基础格式、正文图片、详情渲染、派生纯文本、
  唯一图片计费、基础移动布局和基础无障碍。
- **P1 — 完整发布**：Word/网页粘贴、表格、高级图片操作、冲突、旧数据迁移、完整移动工具栏、
  回收站清理、双语和零影响发布门禁。
- **P2 — 持续加固**：性能、故障恢复、可用性研究、模糊测试扩展和长期维护。

### P0 完成定义

- 手动采集和编辑使用受限富文本编辑器，支持标题、段落、粗体、斜体、下划线、列表、引用、
  链接和代码。
- 正文图片必须通过现有扫描、密钥检测、脱敏和格式限制才能保存；任一图片失败时整次保存失败，
  但编辑器保留未提交内容。
- 详情按结构化文档安全显示，不执行粘贴 HTML，不自动加载远程或伪协议图片。
- 搜索和 AI 使用派生纯文本；图片分析按服务端解析后的唯一内容哈希报价和计费。
- 手机 320–430px 的基础编辑、图片插入、详情阅读和键盘操作可用。

## 格式

- `[ID] [Priority] [P?] [Story] 描述 + 精确文件路径`
- `[P]` 表示可并行。
- `[US1]` 至 `[US5]` 对应规格用户故事。

## 阶段 0：产品确认与原型

- [x] T001 [P0] 产品角色先在 `specs/004-rich-text-editor/spec.md`、`plan.md` 和 `tasks.md` 中完成需求评审、任务拆分、P0/P1/P2 分层和跨功能验收边界确认，不修改应用代码
- [x] T002 [P0] 在 `prototype/app.js`、`prototype/styles.css` 和 `prototype/index.html` 中把手动采集升级为可交互富文本编辑器，包含格式工具栏、正文、图片插入、撤销/重做和清除格式
- [x] T003 [P0] 在 `prototype/app.js`、`prototype/styles.css` 和 `prototype/index.html` 中让知识详情按富文本 HTML 安全展示，并显示图片说明和替代文本
- [x] T004 [P0] 在 `prototype/README.md` 和 `artifacts/rich-text/prototype-report.md` 中记录 `data-terminal` 方向、移动工具栏、图片状态、截图剪贴板入口、文档图片入口和 Taste 检查结果

**检查点**：原型必须证明“格式化正文 + 内嵌图片 + 富文本详情”的完整阅读路径；截图入口只允许
剪贴板图片，文档入口同时接收文档和 JPG/PNG/WebP/HEIC 图片。

---

## 阶段 1：环境、夹具与门禁

- [ ] T005 [P0] 在 `apps/web/package.json` 和 `pnpm-lock.yaml` 中加入固定版本的 Tiptap/ProseMirror 编辑器依赖
- [ ] T006 [P0] [P] 在 `tests/fixtures/rich-text/` 中准备有效文档、Word/Google Docs/网页粘贴、恶意 HTML、远程图片、data/blob/cid/file 图片、来源凭据和旧纯文本夹具
- [ ] T007 [P0] [P] 在 `infra/compose.yml` 和 `packages/config/src/rich-text.ts` 中配置正文、20 张图片、单图源文件 25 MiB、处理后合计 50 MiB、8192px 和 4000 万像素上限
- [ ] T008 [P0] [P] 在 `.github/workflows/rich-text-ci.yml` 中建立 schema、编辑器、迁移、来源、图片计费、安全和浏览器测试门禁
- [ ] T009 [P1] [P] 在 `tests/fixtures/rich-text/performance.ts` 中创建 200,000 字符、20 张图片和深度嵌套的基准文档生成器
- [ ] T010 [P0] 在 `artifacts/rich-text/acceptance-scope.md` 中冻结截图仅剪贴板、文档入口接收图片、富文本正文图片和唯一图片计费的跨功能边界

---

## 阶段 2：结构化文档与追溯基础

**关键**：契约、投影、来源和迁移测试先行；未通过安全测试前不得接入真实编辑或详情渲染。

### 测试

- [ ] T011 [P0] [P] 在 `tests/contract/rich-text/schema.test.ts` 中验证合法文档通过，未知节点、未知标记、危险链接、悬空图片资产和超限文档被拒绝
- [ ] T012 [P0] [P] 在 `tests/unit/rich-text/validate-document.test.ts` 中验证节点数量、嵌套深度、200,000 字符、稳定 nodeId 和图片属性约束
- [ ] T013 [P0] [P] 在 `tests/unit/rich-text/plain-text.test.ts` 中验证段落、标题、列表、引用、代码、表格、图片说明和替代文本投影
- [ ] T014 [P0] [P] 在 `tests/integration/rich-text/provenance.test.ts` 中验证来源类型、清理后 URL、采集/转换时间、converter version、内容哈希和转换报告
- [ ] T015 [P0] [P] 在 `tests/security/rich-text/document-security.test.ts` 中验证脚本、事件属性、iframe、表单、危险链接、远程图片和凭据 URL
- [ ] T016 [P0] [P] 在 `tests/integration/rich-text/schema.test.ts` 中验证迁移、默认值、图片配额、来源报告、唯一约束和版本字段

### 实施

- [ ] T017 [P0] [P] 在 `packages/contracts/src/rich-text/schema.ts` 中根据 [rich-text.schema.json](contracts/rich-text.schema.json) 定义共享节点、标记、属性和版本类型
- [ ] T018 [P0] 在 `packages/domain/src/rich-text/validate-document.ts` 中实现未知节点、未知标记、未知属性、节点数量、嵌套深度、字符数和稳定节点 ID 验证
- [ ] T019 [P0] [P] 在 `packages/domain/src/rich-text/normalize-document.ts` 中实现文档规范化、属性清理、临时 `assetId` 检查、内容哈希和正式附件 ID 解析入口
- [ ] T020 [P0] [P] 在 `packages/domain/src/rich-text/plain-text.ts` 中实现正文、表格、代码、图片说明和替代文本的纯文本投影
- [ ] T021 [P0] [P] 在 `packages/domain/src/rich-text/security.ts` 中实现危险链接、脚本、事件属性、iframe、表单、远程图片和未扫描附件拒绝
- [ ] T022 [P0] [P] 在 `packages/domain/src/rich-text/provenance.ts` 中实现来源 URL 清理、来源记录、converter version 和格式转换报告
- [ ] T023 [P0] [P] 在 `packages/db/src/schema/rich-text.ts` 中创建 `rich_document`、`rich_document_revision`、`rich_document_source`、`rich_document_conversion_report`、`rich_image_reference` 和 `rich_image_upload` 模式
- [ ] T024 [P0] 在 `packages/db/migrations/0004_rich_text.sql` 中创建表、来源报告、唯一约束、图片配额、内容哈希索引和旧正文迁移标记
- [ ] T025 [P0] 在 `tests/integration/rich-text/document-transaction.test.ts` 中先验证文档、修订、来源、转换报告、纯文本、图片引用和搜索修订原子提交，并确认实现前失败
- [ ] T026 [P0] 在 `packages/db/src/repositories/rich-text-repository.ts` 中实现文档读取、版本条件更新、同事务来源/投影/引用/搜索修订写入
- [ ] T027 [P0] 在 `packages/domain/src/rich-text/errors.ts` 中定义 `INVALID_RICH_TEXT`、`IMAGE_SCAN_REQUIRED`、`VERSION_CONFLICT`、`TOO_MANY_IMAGES`、`CONTENT_TOO_LARGE` 和 `PROVENANCE_REQUIRED`

---

## 阶段 3：用户故事 1 — 富文本编辑器（优先级：P1）

**目标**：手动采集与编辑支持格式、撤销/重做、清除格式、移动工具栏和版本保存。

**独立测试**：创建包含标题、粗体、列表、引用、链接和代码的知识，保存后重新打开，格式和顺序一致。

### 测试

- [ ] T028 [P0] [P] [US1] 在 `tests/unit/rich-text/formatting.test.ts` 中验证标题、粗体、斜体、下划线、删除线、列表、引用、链接、代码和清除格式
- [ ] T029 [P0] [P] [US1] 在 `tests/integration/rich-text/editor-roundtrip.test.ts` 中验证编辑、保存、重新加载和版本冲突
- [ ] T030 [P0] [P] [US1] 在 `tests/e2e/rich-text/editor.spec.ts` 中验证键盘快捷操作、撤销/重做、清除格式和自动保存状态
- [ ] T031 [P0] [P] [US1] 在 `tests/e2e/rich-text/editor-mobile.spec.ts` 中先验证 320/360/390/430px 软键盘、选区保留和工具栏可达，失败后再实现移动布局

### 实施

- [ ] T032 [P0] [US1] 在 `apps/web/src/components/editor/rich-text-editor.tsx` 和 `packages/ui/src/editor/schema.ts` 中实现受限 Tiptap 编辑器和共享 schema
- [ ] T033 [P0] [US1] 在 `apps/web/src/components/editor/editor-toolbar.tsx` 中实现格式按钮、当前状态、撤销/重做、键盘标签和清除格式
- [ ] T034 [P0] [US1] 在 `apps/web/src/components/editor/link-editor.tsx` 中实现只允许 HTTP/HTTPS 的安全链接编辑
- [ ] T035 [P0] [US1] 在 `apps/web/src/components/editor/editor-save-state.tsx` 中实现保存中、已保存、失败、冲突和重试状态
- [ ] T036 [P0] [US1] 在 `apps/web/src/app/capture/page.tsx` 中把手动采集正文替换为富文本编辑器并提交结构化文档和可选 `bodySource`
- [ ] T037 [P0] [US1] 在 `apps/web/src/components/knowledge/knowledge-editor.tsx` 中把手动编辑接入富文本文档、版本号和幂等保存
- [ ] T038 [P0] [US1] 在 `apps/web/src/app/api/v1/knowledge-items/route.ts` 和 `[itemId]/route.ts` 中验证结构化文档、重建纯文本、记录来源并返回正式附件 ID
- [ ] T039 [P0] [US1] 在 `packages/domain/src/rich-text/draft.ts` 中实现安全草稿、自动保存失败恢复和本地内容保护
- [ ] T040 [P0] [US1] 在 `apps/web/src/components/editor/editor-toolbar.mobile.tsx` 中实现 320–430px 工具栏、“更多”菜单、44px 控件和选区保留

**检查点**：不带图片的富文本知识可独立编辑、保存、重新打开、搜索和移动操作。

---

## 阶段 4：用户故事 2 — 正文内嵌图片（优先级：P1）

**目标**：图片通过安全附件链路插入正文，支持说明、替代文本、尺寸、对齐和清理。

**独立测试**：插入两张图片，调整顺序、宽度和说明；扫描失败时整次保存失败，修复后可保存。

### 测试

- [ ] T041 [P0] [P] [US2] 在 `tests/unit/rich-text/image-node.test.ts` 中验证图片节点属性、正式附件解析、说明、替代文本、宽高比、装饰标记和 20 张限制
- [ ] T042 [P0] [P] [US2] 在 `tests/unit/rich-text/image-limits.test.ts` 中验证单图源文件 25 MiB、处理后合计 50 MiB、8192px 和 4000 万像素边界
- [ ] T043 [P0] [P] [US2] 在 `tests/security/rich-text/image-sources.test.ts` 中验证远程、data、blob、cid、file、SVG 和未扫描附件不能成为图片节点
- [ ] T044 [P0] [P] [US2] 在 `tests/integration/rich-text/image-scan.test.ts` 中验证扫描、密钥脱敏、派生图、拒绝、全次保存失败和编辑区保留
- [ ] T045 [P1] [P] [US2] 在 `tests/e2e/rich-text/image-insert.spec.ts` 中验证文件选择、相册、相机、拖放、粘贴、说明、替代文本、尺寸和保存后位置
- [ ] T046 [P0] [P] [US2] 在 `tests/contract/rich-text/image-upload.test.ts` 中验证 `uploadId` 只作临时 assetId，保存响应只返回正式附件 ID
- [ ] T047 [P1] [P] [US2] 在 `tests/unit/rich-text/image-normalize.test.ts` 中验证 HEIC/HEIF、动画、方向、EXIF/XMP、8192px 和 4000 万像素边界
- [ ] T048 [P0] [P] [US2] 在 `tests/e2e/imports.spec.ts` 中验证截图入口无文件/拖放/相册/相机控件，文档入口接受文档和 JPG/PNG/WebP/HEIC 图片

### 实施

- [ ] T049 [P0] [US2] 在 `packages/domain/src/rich-text/image-node.ts` 中实现安全图片节点、说明、替代文本、宽高比、显示宽度、对齐和装饰属性
- [ ] T050 [P0] [US2] 在 `packages/providers/src/attachments/rich-image-upload.ts` 中接入现有扫描、脱敏、格式规范化、内容哈希和对象提升流程
- [ ] T051 [P0] [US2] 在 `packages/providers/src/attachments/image-normalizer.ts` 中实现 JPEG/PNG/WebP/HEIC 静态化、方向修正和元数据清理
- [ ] T052 [P0] [US2] 在 `apps/web/src/components/editor/editor-image-dialog.tsx` 中实现图片选择、上传进度、扫描状态、失败、重试和取消
- [ ] T053 [P0] [US2] 在 `apps/web/src/components/editor/editor-image.tsx` 中实现图片节点、说明、替代文本、尺寸和对齐编辑
- [ ] T054 [P0] [US2] 在 `packages/domain/src/rich-text/save-service.ts` 中实现图片全部安全后才提交文档、来源和引用，并解析临时 assetId
- [ ] T055 [P1] [US2] 在 `apps/web/src/components/editor/paste-images.ts` 中实现编辑器图片粘贴和拖放进入相同上传/扫描流程
- [ ] T056 [P1] [US2] 在 `apps/worker/src/jobs/cleanup-rich-images.ts` 中实现删除节点后的引用清理、共享附件保护和孤儿清理
- [ ] T057 [P0] [US2] 在 `packages/domain/src/rich-text/search-projection.ts` 中加入图片说明和替代文本

**检查点**：正文图片不会绕过附件安全门禁，保存失败不会产生文档版本，临时 upload ID 不会落盘。

---

## 阶段 5：用户故事 3 — 富文本详情（优先级：P1）

**目标**：详情按保存结构显示，图片响应式加载，危险内容不执行。

**独立测试**：打开包含全格式和多图的详情，桌面与手机顺序一致，恶意 HTML 不执行。

### 测试

- [ ] T058 [P0] [P] [US3] 在 `tests/unit/rich-text/render.test.ts` 中验证节点、标记、图片和危险内容的安全渲染
- [ ] T059 [P0] [P] [US3] 在 `tests/e2e/rich-text/detail.spec.ts` 中验证桌面和手机格式一致、图片顺序、说明、替代文本、放大视图和编辑返回
- [ ] T060 [P0] [P] [US3] 在 `tests/security/rich-text/render-xss.test.ts` 中验证脚本、事件、表单、iframe、远程资源和非白名单图片来源
- [ ] T061 [P0] [P] [US3] 在 `tests/security/rich-text/render-network.test.ts` 中验证网络请求计数为 0，并断言 DOM 图片来源只属于安全附件白名单
- [ ] T062 [P1] [P] [US3] 在 `tests/e2e/rich-text/table-render.spec.ts` 中验证表头、行列语义、局内滚动和 320/360px 页面级横向滚动为 0

### 实施

- [ ] T063 [P0] [US3] 在 `apps/web/src/components/knowledge/rich-text-view.tsx` 和 `packages/ui/src/editor/render.tsx` 中实现结构化只读渲染
- [ ] T064 [P0] [US3] 在 `apps/web/src/components/knowledge/rich-image.tsx` 中实现安全附件解析、响应式占位、说明、替代文本和加载失败
- [ ] T065 [P0] [US3] 在 `apps/web/src/components/knowledge/image-lightbox.tsx` 中实现键盘可访问的图片放大与关闭
- [ ] T066 [P0] [US3] 在 `apps/web/src/app/library/[itemId]/page.tsx` 中替换纯文本详情渲染并显示正文来源提示
- [ ] T067 [P1] [US3] 在 `apps/web/src/components/knowledge/rich-table.tsx` 中实现表头、行列语义和窄屏局内滚动

**检查点**：详情是同一文档模型的只读投影，不直接信任输入 HTML，也不自动加载任何外部图片。

---

## 阶段 6：用户故事 4 — 外部富文本粘贴（优先级：P1）

**目标**：Word/Google Docs/网页粘贴保留受支持结构、记录来源、移除危险内容，并支持纯文本回退和表格编辑。

**独立测试**：从混合 HTML 粘贴标题、列表、链接和表格，危险内容与远程图片不进入文档，来源报告完整。

### 测试

- [ ] T068 [P1] [P] [US4] 在 `tests/unit/rich-text/paste.test.ts` 中验证 Word、Google Docs、网页、表格和纯文本回退
- [ ] T069 [P1] [P] [US4] 在 `tests/integration/rich-text/paste-provenance.test.ts` 中验证 clipboard/word/google_docs/webpage 来源、URL 清理、转换时间和 converter version
- [ ] T070 [P1] [P] [US4] 在 `tests/security/rich-text/paste-xss.test.ts` 中验证脚本、事件、表单、iframe、危险链接、凭据 URL 和来源图片请求为 0
- [ ] T071 [P1] [P] [US4] 在 `tests/e2e/rich-text/paste.spec.ts` 中验证格式保留、转换提示、远程图片占位和纯文本回退
- [ ] T072 [P1] [P] [US4] 在 `tests/e2e/rich-text/table-edit.spec.ts` 中验证表格创建、表头、插入/删除行列、键盘导航和窄屏表现

### 实施

- [ ] T073 [P1] [US4] 在 `packages/domain/src/rich-text/paste-converter.ts` 中实现 HTML 到受限 schema 的转换、来源记录和格式报告
- [ ] T074 [P1] [US4] 在 `apps/web/src/components/editor/paste-menu.tsx` 中实现保留格式和粘贴为纯文本
- [ ] T075 [P1] [US4] 在 `apps/web/src/components/editor/editor-link-paste.ts` 中实现链接清理和可见地址回退
- [ ] T076 [P1] [US4] 在 `packages/domain/src/rich-text/image-placeholder.ts` 中实现远程/data/blob/cid/file 图片来源占位
- [ ] T077 [P1] [US4] 在 `apps/web/src/components/editor/conversion-notice.tsx` 中显示已保留、降级、移除格式和恢复路径
- [ ] T078 [P1] [US4] 在 `packages/ui/src/editor/table-extension.ts` 和 `apps/web/src/components/editor/editor-table-menu.tsx` 中实现受限表格创建、增删行列和键盘导航

**检查点**：外部输入只进入允许格式，远程图片不会触发自动请求，来源与转换报告可复核。

---

## 阶段 7：用户故事 5 — 搜索、AI、移动与旧数据（优先级：P2）

**目标**：搜索和 AI 使用纯文本投影；图片分析只按唯一资源计费；移动、无障碍和旧数据迁移可用。

**独立测试**：用正文/图片说明/替代文本检索，执行 AI 整理和图片分析，迁移旧内容并验证来源。

### 测试

- [ ] T079 [P0] [P] [US5] 在 `tests/integration/rich-text/search-projection.test.ts` 中验证标题、正文、表格、代码、图片说明和替代文本可检索
- [ ] T080 [P0] [P] [US5] 在 `tests/integration/rich-text/search-security.test.ts` 中验证不匹配原始标签名、内部图片键、存储键、脚本和隐藏样式，且列表/搜索响应不返回完整 `bodyDocument`
- [ ] T081 [P0] [P] [US5] 在 `tests/integration/rich-text/ai-projection.test.ts` 中验证 AI 不接收原始 JSON、脚本、隐藏样式和附件存储键
- [ ] T082 [P0] [P] [US5] 在 `tests/integration/rich-text/image-billing.test.ts` 中验证 002 报价解析 `imageAssetIds`、按内容哈希去重、重复位置不重复扣费、扫描失败不扣费
- [ ] T083 [P1] [P] [US5] 在 `tests/integration/rich-text/conflict.test.ts` 中验证两标签页冲突、重新加载、另存为和明确覆盖
- [ ] T084 [P1] [P] [US5] 在 `tests/integration/rich-text/migration.test.ts` 中验证旧纯文本、可识别链接、换行、来源记录、重复执行、并发和失败重试
- [ ] T085 [P1] [P] [US5] 在 `tests/integration/rich-text/lifecycle.test.ts` 中验证取消图片引用、回收站保留、恢复、永久清除、共享附件和孤儿清理
- [ ] T086 [P0] [P] [US5] 在 `tests/accessibility/rich-text.spec.ts` 中验证工具栏、图片说明、状态播报、键盘、焦点恢复和屏幕阅读器
- [ ] T087 [P0] [P] [US5] 在 `tests/e2e/rich-text/mobile-complete.spec.ts` 中验证 320/360/390/430、横屏、200% 字体和 MVP 格式入口
- [ ] T088 [P1] [P] [US5] 在 `tests/e2e/rich-text/bilingual.spec.ts` 中验证中英文事实一致、格式名称、错误、图片说明和用户内容不自动翻译

### 实施

- [ ] T089 [P0] [US5] 在 `packages/domain/src/search/index-document.ts` 和 `apps/web/src/app/api/v1/knowledge-items/route.ts` 中使用富文本纯文本投影更新索引，并返回不含完整正文的列表/搜索摘要 DTO
- [ ] T090 [P0] [US5] 在 `packages/providers/src/ai/rich-text-input.ts` 中只生成授权纯文本和用户明确选择的唯一图片引用
- [ ] T091 [P0] [US5] 在 `packages/domain/src/rich-text/image-billing.ts` 中把去重后的 `imageAssetIds` 接入 002 报价、确认、结算和失败释放流程
- [ ] T092 [P1] [US5] 在 `apps/web/src/components/editor/editor-conflict-dialog.tsx` 中实现重新加载、另存为和明确覆盖
- [ ] T093 [P1] [US5] 在 `packages/domain/src/rich-text/legacy-migration.ts` 和 `apps/worker/src/jobs/migrate-rich-text.ts` 中实现幂等纯文本迁移、链接识别和来源报告
- [ ] T094 [P1] [US5] 在 `packages/domain/src/rich-text/lifecycle.ts` 中实现文档、修订、来源、图片引用的回收站/永久删除行为
- [ ] T095 [P0] [US5] 在 `apps/web/src/components/editor/rich-text-accessibility.ts` 中实现状态播报、焦点恢复和表格导航
- [ ] T096 [P0] [US5] 在 `apps/web/src/components/editor/editor-toolbar.mobile.tsx` 中完成 320–430px 更多菜单、选区保留、软键盘和安全区适配
- [ ] T097 [P1] [US5] 在 `apps/web/src/messages/zh-CN.json` 和 `en.json` 中补齐编辑、格式、图片、来源、粘贴、冲突、迁移和错误文案

**检查点**：001/002/003 的核心契约在富文本下保持成立，来源可追溯、图片只计费一次、迁移和清理可恢复。

---

## 阶段 8：完善与发布

- [ ] T098 [P1] 在 `tests/security/rich-text/corpus.test.ts` 中加入 XSS、粘贴 HTML、协议、来源 URL 和图片模糊测试语料
- [ ] T099 [P1] 在 `tests/contract/rich-text/all-fixtures.test.ts` 中执行全部合法/非法 schema、来源、上传和计费夹具
- [ ] T100 [P2] 在 `tests/performance/rich-text/editor-benchmark.ts` 和 `artifacts/rich-text/performance.md` 中验证 200,000 字符、20 图、详情加载和移动输入
- [ ] T101 [P1] 在 `tests/e2e/rich-text/taste.spec.ts` 和 `artifacts/rich-text/screenshots/` 中完成 1440/768/390/360 Taste 检查并保存截图证据
- [ ] T102 [P1] 在 `tests/integration/deployment/zero-impact.test.ts` 和 `deploy/scripts/verify-kb-coexistence.sh` 中验证 `/`、`/app`、`/api`、`/bank`、`/pay`、`/apk`、认证、Cookie、证书和支付 sidecar 前后差异为 0
- [ ] T103 [P1] 在 `docs/`、`README.md` 和 `AGENTS.md` 中记录编辑器使用方式、格式限制、来源追踪、图片安全和清理规则
- [ ] T104 [P1] 在 `artifacts/acceptance/rich-text.md` 中映射 FR-001 至 FR-057、SC-001 至 SC-019 和目标任务/证据
- [ ] T105 [P1] 在新环境中执行 [quickstart.md](quickstart.md)，将命令和结果写入 `artifacts/rich-text/quickstart-report.md`
- [ ] T106 [P2] 在 `tests/integration/rich-text/restart-recovery.test.ts` 中验证浏览器刷新、进程重启和上传临时文件恢复
- [ ] T107 [P1] 在 `tests/usability/rich-text-study.md` 和 `artifacts/rich-text/usability-results.md` 中执行 10 人编辑和图片研究，为 SC-001、SC-014 提供发布证据
- [ ] T108 [P1] 在 `artifacts/rich-text/maintenance-report.md` 中记录 schema 演进、`0004_rich_text.sql` 迁移回滚、图片保留和依赖升级演练

---

## 依赖关系

- 原型 T001–T004 可先于正式实现完成，用于产品和视觉确认。
- 阶段 1–2 阻塞所有用户故事；来源、schema、图片配额和投影契约必须先稳定。
- US1 完成后可并行 US2 与 US3；US4 依赖 schema 转换器和来源记录；US5 依赖文档、图片、详情和 002 报价链路。
- 对外发布依赖 001、002、003 的 P0/P1 门禁与 004 全部 P0/P1 任务。

## 实施策略

### P0 MVP

1. 完成产品边界、schema、投影、来源、数据库和保存事务。
2. 完成 US1、US2、US3 以及 US5 的搜索/AI/唯一图片计费基础。
3. 完成基础移动布局、键盘和屏幕阅读器支持。

### P1 完整发布

1. 完成粘贴、表格、图片高级操作、冲突、迁移和生命周期清理。
2. 完成安全语料、Taste、双语、零影响回归和最终验收。

### P2 持续加固

1. 完成恢复、可用性和长期维护演练。
2. schema 升级必须先发布只读兼容迁移，再更新编辑器，最后清理旧节点。

## 完成记录格式

每完成一个任务，必须同步勾选并按宪法追加：

- 任务 ID、日期、状态、执行子智能体和提交 SHA。
- 摘要、详情、修改文件、前置依赖。
- FR/SC/US 映射、验证命令、夹具版本、精确结果和证据路径。
- 风险、回滚和未覆盖项。

## Completion Log

### 完成记录：T001

- 日期：2026-10-21
- 状态：已完成
- 执行子智能体：产品
- Git commit：`337cbf8`
- 摘要：新增需求完成产品复核、需求分析和任务拆分，并先于原型实现确认边界。
- 详情：确认正文使用富文本、详情富文本展示、正文可插图；截图入口只允许剪贴板粘贴；文档入口接收文档及 JPG/PNG/WebP/HEIC 图片；AI 图片分析按唯一内容计费。
- 验证：规格、计划、数据模型、契约和任务表交叉复核；P0 不再依赖 P1 的粘贴、表格、拖放或完整移动工具栏。
- 覆盖：FR-001 至 FR-057 的范围冻结和 SC-001 至 SC-019 的验收方向；不修改应用代码。
- 风险与回滚：产品边界变更需同步更新规格、计划、契约和任务；回滚本次文档提交并重新生成任务即可。

### 完成记录：T002

- 日期：2026-10-21
- 状态：已完成
- 执行子智能体：开发
- Git commit：`337cbf8`
- 摘要：原型的手动采集已升级为可交互富文本编辑器。
- 详情：已加入 H2/H3、粗体、斜体、下划线、删除线、列表、引用、代码块、链接、撤销/重做、清除格式、内嵌图片插入和移动工具栏；原型使用浏览器原生编辑能力模拟正式 Tiptap 行为。
- 验证：真实浏览器格式、图片、编辑保存和重新打开检查；Taste 证据见 T004。
- 覆盖：FR-001、FR-002 的部分能力及 US1 原型路径；不声称覆盖表格、完整三级标题或正式持久化。
- 风险与回滚：原型使用本地内存和对象 URL，不代表正式 schema、扫描或附件生命周期。

### 完成记录：T003

- 日期：2026-10-21
- 状态：已完成
- 执行子智能体：开发
- Git commit：`337cbf8`
- 摘要：知识详情已按保存的富文本内容渲染。
- 详情：详情正文支持标题、段落、列表、链接、代码和图片；图片显示说明和替代文本，图片文件作为来源附件展示且不自动写入正文。
- 验证：浏览器详情检查、图片插入提交、`.detail-attachments img` 断言和详情编辑保存通过。
- 覆盖：FR-022 至 FR-028、US3。
- 风险与回滚：原型未连接真实附件扫描服务，正式渲染必须使用结构化 schema。

### 完成记录：T004

- 日期：2026-10-21
- 状态：已完成
- 执行子智能体：测试
- Git commit：`337cbf8`
- 摘要：Taste 源检查、真实浏览器检查、截图剪贴板入口和文档图片入口复核完成。
- 详情：保留 `data-terminal` 方向和深海蓝色调；原型包含工具栏、编辑正文、图片状态、来源附件、真正可聚焦的剪贴板粘贴字段、仅剪贴板的截图入口和键盘可操作的文档图片入口。
- 验证：`node --check prototype/app.js` 通过；Taste lint 为 0 FAIL/0 WARN；1440、768、390、360 浏览器检查通过；截图入口无文件/拖放控件；文档入口接受 JPG/PNG/WebP/HEIC 并拒绝超过 25 MiB 的文件。
- 覆盖：原型 T002–T004 的可视与交互证据；不声称覆盖 SC-001、SC-008 或 SC-015 等需要完整用户、生产或零影响验证的指标。
- 风险与回滚：保留密集辅助文字的 `tiny-text` 警告；原型不包含生产级 schema 渲染、真实网络计数或服务端扫描。
