# 任务：识海富文本知识编辑

**输入**：`specs/004-rich-text-editor/`

**前置条件**：[spec.md](spec.md)、[plan.md](plan.md)、[research.md](research.md)、
[data-model.md](data-model.md)、[quickstart.md](quickstart.md)、
[contracts/rich-text.schema.json](contracts/rich-text.schema.json)、
[contracts/image-upload.openapi.yaml](contracts/image-upload.openapi.yaml)

**测试**：测试先行。Spring Boot 使用 JUnit 5/Testcontainers/RestAssured，Vue 使用
Vitest/Vue Test Utils，E2E 使用 Playwright，富文本 JSON 契约由 Vue 与 Java 共用夹具。

## 优先级

- **P0**：JSONB schema、基础编辑器、正文图片、详情、纯文本投影、唯一图片计费和基础移动。
- **P1**：粘贴、表格、拖放、冲突、迁移、完整工具栏、双语、Taste 和零影响。
- **P2**：恢复、可用性和 schema 演进。

## 阶段 0：产品确认与原型

- [x] T001 [P0] 产品角色在 `specs/004-rich-text-editor/spec.md`、`plan.md` 和 `tasks.md` 中完成需求评审、任务拆分、优先级和跨功能验收边界，不修改代码
- [x] T002 [P0] 在 `prototype/app.js`、`prototype/styles.css` 和 `prototype/index.html` 中把手动采集升级为交互式富文本编辑器，包含格式、图片、撤销/重做和清除格式
- [x] T003 [P0] 在 `prototype/app.js`、`prototype/styles.css` 和 `prototype/index.html` 中让详情按富文本安全展示，并显示图片说明和替代文本
- [x] T004 [P0] 在 `prototype/README.md` 和 `artifacts/rich-text/prototype-report.md` 中记录 `data-terminal` 方向、移动工具栏、截图/文档入口和 Taste 检查

## 阶段 1：跨语言契约与工程基础

- [ ] T005 [P0] [P] 在 `frontend/kb-web/src/features/editor/contracts/` 和 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/richtext/` 中建立共享 schema 版本、DTO 和夹具映射
- [ ] T006 [P0] [P] 在 `tests/fixtures/rich-text/` 中准备合法、未知节点、危险属性、超限、图片、表格和迁移夹具
- [ ] T007 [P0] [P] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/contract/` 中使用 networknt/Jackson 验证 JSON Schema，并用 RestAssured/OpenAPI Validator 验证图片上传 API 和 CSRF
- [x] T008 [P0] [P] 在 `frontend/kb-web/src/lib/rich-text.test.ts` 中验证 Tiptap 输出、非法输入和安全渲染辅助函数
- [ ] T009 [P0] [P] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/migration/` 中先验证全新安装、001→002→004 顺序升级、重复迁移、表、约束、JSONB、部分索引和版本字段
- [x] T010 [P0] [P] 在 `backend/kb-app/src/main/resources/db/migration/V0004__rich_text.sql` 中创建文档、修订、来源、转换报告、图片引用和上传表

## 阶段 2：文档验证、投影与保存事务

### 测试

- [x] T011 [P0] [P] [US1] 在 `backend/kb-core/src/test/java/site/seaunit/zhishiku/core/richtext/RichTextValidatorTest.java` 中验证节点、标记、属性、图片和表格约束
- [x] T012 [P0] [P] [US5] 在 `backend/kb-core/src/test/java/site/seaunit/zhishiku/core/richtext/PlainTextProjectorTest.java` 中验证正文、列表、图片说明和替代文本投影
- [ ] T013 [P0] [P] [US4] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/provenance/` 中验证来源、URL、时间、converter version、哈希和转换报告
- [ ] T014 [P0] [P] [US3] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/transaction/` 中验证文档、来源、图片引用和搜索修订原子提交

### 实施

- [x] T015 [P0] 在 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/richtext/RichTextValidator.java` 中实现 JSONB 节点白名单、深度、字符、图片和表格语义验证
- [x] T016 [P0] [P] [US5] 在 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/richtext/PlainTextProjector.java` 中实现纯文本投影
- [x] T017 [P0] [P] [US4] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/knowledge/KnowledgeService.java` 中实现来源、转换报告和内容哈希
- [x] T018 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/knowledge/`、`richtext/` 和 `attachment/` 中实现 JPA 实体和 `JdbcClient` 保存事务
- [x] T019 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/knowledge/KnowledgeService.java` 中实现临时 `assetId` 到正式附件 ID 的解析和 `version` 冲突检查
- [x] T020 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/richtext/image/`、`attachment/` 和 `knowledge/KnowledgeController.java` 中实现上传、保存、详情和冲突 API

## 阶段 3：用户故事 1 — Vue 富文本编辑器

- [ ] T021 [P0] [P] [US1] 在 `frontend/kb-web/src/features/editor/__tests__/formatting.spec.ts` 中验证标题、粗体、斜体、下划线、删除线、列表、引用、链接、代码和清除格式
- [ ] T022 [P0] [P] [US1] 在 `tests/e2e/richtext-editor.spec.ts` 中验证键盘、撤销、重做、保存状态和重新打开
- [ ] T023 [P0] [P] [US1] 在 `tests/e2e/richtext-mobile.spec.ts` 中先验证 320/360/390/430、软键盘、选区、44px 和安全区
- [x] T024 [P0] [US1] 在 `frontend/kb-web/src/components/RichEditor.vue` 和 `src/extensions/RichImage.ts` 中使用 Tiptap Vue 3 实现受限编辑器和 ProseMirror schema
- [x] T025 [P0] [US1] 在 `frontend/kb-web/src/components/RichEditor.vue` 中实现格式状态、撤销/重做、链接和图片入口
- [ ] T026 [P0] [US1] 在 `frontend/kb-web/src/features/knowledge/` 中实现手动采集、编辑、保存状态和安全草稿
- [x] T027 [P0] [US1] 在 `frontend/kb-web/src/i18n.ts` 中补齐编辑器、格式、链接、错误和无障碍中英文文案
- [x] T028 [P0] [US1] 在 `frontend/kb-web/src/components/RichEditor.vue` 和 `src/styles.css` 中实现可见焦点、工具栏名称和上传状态播报

## 阶段 4：用户故事 2 — 正文图片

- [ ] T029 [P0] [P] [US2] 在 `backend/kb-core/src/test/java/site/seaunit/zhishiku/core/richtext/image/` 中验证 assetId、说明、替代文本、宽高比、装饰、20 张和 50 MiB
- [ ] T030 [P0] [P] [US2] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/image/` 中验证远程、data、blob、cid、file、SVG、未扫描和扫描失败
- [ ] T031 [P1] [P] [US2] 在 `tests/e2e/richtext-images.spec.ts` 中验证文件、相册、相机、拖放、粘贴、说明、尺寸和保存位置
- [ ] T032 [P0] [P] [US2] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/image/limits/` 中验证 25 MiB、8192px、4000 万像素和 HEIC
- [ ] T033 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/richtext/image/` 中实现图片节点、配额、libvips 调用、哈希和正式附件提升
- [ ] T034 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/richtext/image/` 中实现扫描/脱敏全次失败和暂存清理
- [x] T035 [P0] [US2] 在 `frontend/kb-web/src/components/RichEditor.vue`、`editor/RichImageNodeView.vue` 和 `extensions/RichImage.ts` 中实现图片选择、状态、说明、替代文本、尺寸和对齐
- [ ] T036 [P1] [US2] 在 `frontend/kb-web/src/features/editor/image/` 中实现图片粘贴、拖放和失败恢复
- [ ] T037 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/richtext/image/` 中实现唯一 `content_hash` 查询和 002 `imageAssetIds` 输出
- [ ] T038 [P0] [US2] 在 `tests/e2e/richtext-image-safety.spec.ts` 中验证失败不产生版本、编辑区保留和重试

## 阶段 5：用户故事 3 — 富文本详情

- [ ] T039 [P0] [P] [US3] 在 `frontend/kb-web/src/features/knowledge/__tests__/richtext-view.spec.ts` 中验证节点、标记、图片和危险内容
- [ ] T040 [P0] [P] [US3] 在 `tests/e2e/richtext-detail.spec.ts` 中验证桌面/手机、图片顺序、说明、放大和编辑返回
- [ ] T041 [P0] [P] [US3] 在 `tests/security/richtext-render.spec.ts` 中验证脚本、事件、表单、iframe 和远程图片来源
- [ ] T042 [P0] [P] [US3] 在 `tests/security/richtext-network.spec.ts` 中验证网络请求计数 0 和 DOM 图片来源白名单
- [x] T043 [P0] [US3] 在 `frontend/kb-web/src/components/RichTextDocument.vue` 和 `DocumentNode.vue` 中实现结构化只读渲染
- [x] T044 [P0] [US3] 在 `frontend/kb-web/src/components/SafeImage.vue` 中实现安全附件、占位、替代文本和失败状态
- [ ] T045 [P0] [US3] 在 `frontend/kb-web/src/features/knowledge/components/ImageLightbox.vue` 中实现键盘可访问放大
- [ ] T046 [P1] [US3] 在 `frontend/kb-web/src/features/knowledge/components/RichTable.vue` 中实现表头、语义和窄屏局内滚动

## 阶段 6：用户故事 4 — 粘贴、表格和来源

- [ ] T047 [P1] [P] [US4] 在 `backend/kb-core/src/test/java/site/seaunit/zhishiku/core/richtext/paste/` 中验证 Word、Google Docs、网页、表格和纯文本回退
- [ ] T048 [P1] [P] [US4] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/provenance/` 中验证粘贴来源、URL 清理和转换报告
- [ ] T049 [P1] [P] [US4] 在 `tests/security/richtext-paste.spec.ts` 中验证脚本、事件、iframe、危险链接、凭据和服务端网络请求为 0
- [ ] T050 [P1] [P] [US4] 在 `tests/e2e/richtext-table.spec.ts` 中验证表格创建、增删行列、表头、键盘和窄屏
- [ ] T051 [P1] [US4] 在 `frontend/kb-web/src/features/editor/paste/` 中实现保留格式、纯文本回退和转换提示
- [ ] T052 [P1] [US4] 在 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/richtext/paste/` 中实现 Java HTML 到受限 ProseMirror 的转换
- [ ] T053 [P1] [US4] 在 `frontend/kb-web/src/features/editor/table/` 中实现 Tiptap Table 和工具栏
- [ ] T054 [P1] [US4] 在 `frontend/kb-web/src/features/editor/` 中实现远程来源图片占位、链接清理和格式报告

## 阶段 7：用户故事 5 — 搜索、AI、冲突与迁移

- [ ] T055 [P0] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/search/richtext/` 中验证正文、表格、代码、说明和替代文本可检索
- [ ] T056 [P0] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/search/richtext/` 中验证不匹配标签名、存储键、隐藏样式和脚本
- [ ] T057 [P0] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/ai/richtext/` 中验证 AI 只接收投影和明确授权的唯一图片
- [ ] T058 [P0] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/billing/richtext/` 中验证唯一内容哈希、重复位置、失败不扣费和 002 报价
- [ ] T059 [P1] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/conflict/` 中验证版本冲突、重载、另存和明确覆盖
- [ ] T060 [P1] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/migration/` 中验证旧文本、链接、换行、幂等、并发和来源
- [ ] T061 [P1] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/lifecycle/` 中验证取消引用、回收站、恢复、purge、共享附件和孤儿清理
- [x] T062 [P0] [US5] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/knowledge/KnowledgeService.java` 中接入 PostgreSQL 搜索投影和摘要 DTO
- [ ] T063 [P0] [US5] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/ai/richtext/` 中实现 AI 输入和唯一图片计费桥接
- [ ] T064 [P1] [US5] 在 `frontend/kb-web/src/features/editor/conflict/` 中实现冲突对话框和本地草稿
- [ ] T065 [P1] [US5] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/richtext/migration/` 中实现迁移状态、`bodyDocument=null` 过渡和链接识别
- [ ] T066 [P1] [US5] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/richtext/lifecycle/` 中实现生命周期清理

## 阶段 8：完整移动、无障碍、双语与发布

- [ ] T067 [P1] [P] [US5] 在 `tests/accessibility/richtext.spec.ts` 中验证工具栏、焦点、图片说明、状态播报、表格、键盘和 200% 字体
- [ ] T068 [P0] [P] [US5] 在 `tests/e2e/richtext-mobile-complete.spec.ts` 中验证 320/360/390/430、横屏、软键盘和安全区
- [ ] T069 [P1] [P] [US5] 在 `tests/e2e/richtext-bilingual.spec.ts` 中验证中英文事实一致
- [ ] T070 [P1] [US5] 在 `frontend/kb-web/src/features/editor/` 中完成移动更多菜单、44px 控件和选区恢复
- [ ] T071 [P1] [P] 在 `tests/security/richtext-corpus.spec.ts` 中执行 XSS、来源、协议和图片模糊语料
- [ ] T072 [P1] [P] 在 `tests/e2e/richtext-taste.spec.ts` 中执行 1440/768/430/390/360/320、手机横屏、安全区和 200% 字体 Taste 检查
- [ ] T073 [P1] 在 `tests/e2e/richtext-zero-impact.spec.ts` 中验证 `/`、`/app`、`/api`、`/bank`、`/pay`、`/apk`、Cookie、证书和 sidecar
- [ ] T074 [P1] 在 `artifacts/acceptance/rich-text.md` 中映射 FR-001 至 FR-057、SC-001 至 SC-019
- [ ] T075 [P1] 在新环境执行 [quickstart.md](quickstart.md) 并写入 `artifacts/rich-text/quickstart-report.md`

## 阶段 9：P2 加固

- [ ] T076 [P2] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/recovery/` 中验证刷新、进程重启、上传暂存恢复
- [ ] T077 [P2] 在 `tests/performance/richtext/` 中验证 200,000 字符、20 图、详情和移动输入
- [ ] T078 [P2] 在 `tests/usability/richtext.md` 中执行 10 人编辑和图片研究
- [ ] T079 [P2] 在 `artifacts/rich-text/maintenance.md` 中记录 schema 演进、迁移回滚和依赖升级

## 依赖关系

- T001-T004 为已完成原型与产品证据。
- T005-T020 是 Vue/Java 共享契约、数据库和事务基础，阻塞全部用户故事。
- US1 完成后可并行 US2 与 US3；US4 依赖来源和粘贴转换；US5 依赖搜索、AI 和 002 计费。
- 发布依赖 001/002/003/004 的 P0/P1 门禁。

## 完成记录格式

每项任务完成后勾选并记录 ID、日期、执行角色、提交 SHA、摘要、详情、验证命令、证据路径、
FR/SC 映射、风险和回滚。

## Completion Log

### 完成记录：T001

- 日期：2026-10-21
- 状态：已完成
- 执行子智能体：产品
- Git commit：`337cbf8`
- 摘要：新增需求完成产品复核和优先级拆分。
- 详情：确认富文本、正文图片、截图仅剪贴板、文档入口接收图片、来源追溯和唯一图片计费。
- 验证：规格、计划、数据模型、契约和任务交叉复核；P0 不依赖 P1 的粘贴、表格或拖放。
- 覆盖：FR-001 至 FR-057 的范围冻结，不表示正式应用代码已实现。
- 风险与回滚：文档边界变化需同步规格、契约和任务。

### 完成记录：T002

- 日期：2026-10-21
- 状态：已完成
- 执行子智能体：开发
- Git commit：`337cbf8`
- 摘要：原型已升级为交互式富文本编辑器。
- 详情：支持 H2/H3、粗体、斜体、下划线、删除线、列表、引用、代码、链接、撤销、清除格式和正文图片。
- 验证：真实浏览器格式、图片、保存和重新打开检查。
- 覆盖：FR-001、FR-002 的部分原型能力。
- 风险与回滚：原型使用 `contenteditable`，正式实现改为 Vue Tiptap。

### 完成记录：T003

- 日期：2026-10-21
- 状态：已完成
- 执行子智能体：开发
- Git commit：`337cbf8`
- 摘要：详情支持富文本和来源图片。
- 详情：正文、列表、链接、代码、图片说明、替代文本和编辑回到详情。
- 验证：浏览器图片插入、详情显示和编辑保存通过。
- 覆盖：FR-022 至 FR-028、US3。
- 风险与回滚：原型未连接真实 Spring/S3 扫描。

### 完成记录：T004

- 日期：2026-10-21
- 状态：已完成
- 执行子智能体：测试
- Git commit：`337cbf8`
- 摘要：Taste、截图、文档和富文本入口已完成浏览器验证。
- 详情：截图入口无文件/拖放控件；文档入口接受图片；富文本移动工具栏、焦点和 25 MiB 校验已修复。
- 验证：Taste lint 0 FAIL/0 WARN；1440/768/390/360 均 0 FAIL；粘贴和文件错误交互通过。
- 覆盖：原型视觉和交互证据，不代表生产实现。
- 风险与回滚：保留 `tiny-text` 与短横屏编辑区警告，正式页面继续由 Taste 复验。
