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
- [x] T080 [P0] 在 Vue 应用壳、首页、知识库、采集、待处理、回收站、设置和详情页中按最新原型实现深海蓝主题、图标轨、移动底栏、知识海图和中英文导航，并完成 Taste 对照验收

## 阶段 1：跨语言契约与工程基础

- [x] T005 [P0] [P] 在 `frontend/kb-web/src/features/editor/contracts/` 和 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/richtext/` 中建立共享 schema 版本、DTO 和夹具映射
- [x] T006 [P0] [P] 在 `tests/fixtures/rich-text/` 中准备合法、未知节点、危险属性、超限、图片、表格和迁移夹具
- [x] T007 [P0] [P] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/contract/` 中使用 networknt/Jackson 验证 JSON Schema，并用 RestAssured/OpenAPI Validator 验证图片上传 API 和 CSRF
- [x] T008 [P0] [P] 在 `frontend/kb-web/src/lib/rich-text.test.ts` 中验证 Tiptap 输出、非法输入和安全渲染辅助函数
- [x] T009 [P0] [P] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/migration/` 中先验证全新安装、001→002→004 顺序升级、重复迁移、表、约束、JSONB、部分索引和版本字段
- [x] T010 [P0] [P] 在 `backend/kb-app/src/main/resources/db/migration/V0004__rich_text.sql` 中创建文档、修订、来源、转换报告、图片引用和上传表

## 阶段 2：文档验证、投影与保存事务

### 测试

- [x] T011 [P0] [P] [US1] 在 `backend/kb-core/src/test/java/site/seaunit/zhishiku/core/richtext/RichTextValidatorTest.java` 中验证节点、标记、属性、图片和表格约束
- [x] T012 [P0] [P] [US5] 在 `backend/kb-core/src/test/java/site/seaunit/zhishiku/core/richtext/PlainTextProjectorTest.java` 中验证正文、列表、图片说明和替代文本投影
- [x] T013 [P0] [P] [US4] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/provenance/` 中验证来源、URL、时间、converter version、哈希和转换报告
- [x] T014 [P0] [P] [US3] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/transaction/` 中验证文档、来源、图片引用和搜索修订原子提交

### 实施

- [x] T015 [P0] 在 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/richtext/RichTextValidator.java` 中实现 JSONB 节点白名单、深度、字符、图片和表格语义验证
- [x] T016 [P0] [P] [US5] 在 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/richtext/PlainTextProjector.java` 中实现纯文本投影
- [x] T017 [P0] [P] [US4] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/knowledge/KnowledgeService.java` 中实现来源、转换报告和内容哈希
- [x] T018 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/knowledge/`、`richtext/` 和 `attachment/` 中实现 JPA 实体和 `JdbcClient` 保存事务
- [x] T019 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/knowledge/KnowledgeService.java` 中实现临时 `assetId` 到正式附件 ID 的解析和 `version` 冲突检查
- [x] T020 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/richtext/image/`、`attachment/` 和 `knowledge/KnowledgeController.java` 中实现上传、保存、详情和冲突 API

## 阶段 3：用户故事 1 — Vue 富文本编辑器

- [x] T021 [P0] [P] [US1] 在 `frontend/kb-web/src/features/editor/__tests__/formatting.spec.ts` 中验证标题、粗体、斜体、下划线、删除线、列表、引用、链接、代码和清除格式
- [x] T022 [P0] [P] [US1] 在 `tests/e2e/richtext-editor.spec.ts` 中验证键盘、撤销、重做、保存状态和重新打开
- [x] T023 [P0] [P] [US1] 在 `tests/e2e/richtext-mobile.spec.ts` 中先验证 320/360/390/430、软键盘、选区、44px 和安全区
- [x] T024 [P0] [US1] 在 `frontend/kb-web/src/components/RichEditor.vue` 和 `src/extensions/RichImage.ts` 中使用 Tiptap Vue 3 实现受限编辑器和 ProseMirror schema
- [x] T025 [P0] [US1] 在 `frontend/kb-web/src/components/RichEditor.vue` 中实现格式状态、撤销/重做、链接和图片入口
- [x] T026 [P0] [US1] 在 `frontend/kb-web/src/features/knowledge/` 中实现手动采集、编辑、保存状态和安全草稿
- [x] T027 [P0] [US1] 在 `frontend/kb-web/src/i18n.ts` 中补齐编辑器、格式、链接、错误和无障碍中英文文案
- [x] T028 [P0] [US1] 在 `frontend/kb-web/src/components/RichEditor.vue` 和 `src/styles.css` 中实现可见焦点、工具栏名称和上传状态播报

## 阶段 4：用户故事 2 — 正文图片

- [x] T029 [P0] [P] [US2] 在 `backend/kb-core/src/test/java/site/seaunit/zhishiku/core/richtext/image/` 中验证 assetId、说明、替代文本、宽高比、装饰、20 张和 50 MiB
- [x] T030 [P0] [P] [US2] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/image/` 中验证远程、data、blob、cid、file、SVG、未扫描和扫描失败
- [x] T031 [P1] [P] [US2] 在 `tests/e2e/richtext-images.spec.ts` 中验证文件、相册、相机、拖放、粘贴、说明、尺寸和保存位置
- [x] T032 [P0] [P] [US2] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/image/limits/` 中验证 25 MiB、8192px、4000 万像素和 HEIC
- [x] T033 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/richtext/image/` 中实现图片节点、配额、libvips 调用、哈希和正式附件提升
- [x] T034 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/richtext/image/` 中实现扫描/脱敏全次失败和暂存清理
- [x] T035 [P0] [US2] 在 `frontend/kb-web/src/components/RichEditor.vue`、`editor/RichImageNodeView.vue` 和 `extensions/RichImage.ts` 中实现图片选择、状态、说明、替代文本、尺寸和对齐
- [x] T036 [P1] [US2] 在 `frontend/kb-web/src/features/editor/image/` 中实现图片粘贴、拖放和失败恢复
- [x] T037 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/richtext/image/` 中实现唯一 `content_hash` 查询和 002 `imageAssetIds` 输出
- [x] T038 [P0] [US2] 在 `tests/e2e/richtext-image-safety.spec.ts` 中验证失败不产生版本、编辑区保留和重试

## 阶段 5：用户故事 3 — 富文本详情

- [x] T039 [P0] [P] [US3] 在 `frontend/kb-web/src/features/knowledge/__tests__/richtext-view.spec.ts` 中验证节点、标记、图片和危险内容
- [x] T040 [P0] [P] [US3] 在 `tests/e2e/richtext-detail.spec.ts` 中验证桌面/手机、图片顺序、说明、放大和编辑返回
- [x] T041 [P0] [P] [US3] 在 `tests/security/richtext-render.spec.ts` 中验证脚本、事件、表单、iframe 和远程图片来源
- [x] T042 [P0] [P] [US3] 在 `tests/security/richtext-network.spec.ts` 中验证网络请求计数 0 和 DOM 图片来源白名单
- [x] T043 [P0] [US3] 在 `frontend/kb-web/src/components/RichTextDocument.vue` 和 `DocumentNode.vue` 中实现结构化只读渲染
- [x] T044 [P0] [US3] 在 `frontend/kb-web/src/components/SafeImage.vue` 中实现安全附件、占位、替代文本和失败状态
- [x] T045 [P0] [US3] 在 `frontend/kb-web/src/features/knowledge/components/ImageLightbox.vue` 中实现键盘可访问放大
- [x] T046 [P1] [US3] 在 `frontend/kb-web/src/features/knowledge/components/RichTable.vue` 中实现表头、语义和窄屏局内滚动

## 阶段 6：用户故事 4 — 粘贴、表格和来源

- [x] T047 [P1] [P] [US4] 在 `backend/kb-core/src/test/java/site/seaunit/zhishiku/core/richtext/paste/` 中验证 Word、Google Docs、网页、表格和纯文本回退
- [x] T048 [P1] [P] [US4] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/provenance/` 中验证粘贴来源、URL 清理和转换报告
- [x] T049 [P1] [P] [US4] 在 `tests/security/richtext-paste.spec.ts` 中验证脚本、事件、iframe、危险链接、凭据和服务端网络请求为 0
- [x] T050 [P1] [P] [US4] 在 `tests/e2e/richtext-table.spec.ts` 中验证表格创建、增删行列、表头、键盘和窄屏
- [x] T051 [P1] [US4] 在 `frontend/kb-web/src/features/editor/paste/` 中实现保留格式、纯文本回退和转换提示
- [x] T052 [P1] [US4] 在 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/richtext/paste/` 中实现 Java HTML 到受限 ProseMirror 的转换
- [x] T053 [P1] [US4] 在 `frontend/kb-web/src/features/editor/table/` 中实现 Tiptap Table 和工具栏
- [x] T054 [P1] [US4] 在 `frontend/kb-web/src/features/editor/` 中实现远程来源图片占位、链接清理和格式报告

## 阶段 7：用户故事 5 — 搜索、AI、冲突与迁移

- [x] T055 [P0] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/search/richtext/` 中验证正文、表格、代码、说明和替代文本可检索
- [x] T056 [P0] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/search/richtext/` 中验证不匹配标签名、存储键、隐藏样式和脚本
- [x] T057 [P0] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/ai/richtext/` 中验证 AI 只接收投影和明确授权的唯一图片
- [x] T058 [P0] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/billing/richtext/` 中验证唯一内容哈希、重复位置、失败不扣费和 002 报价
- [x] T059 [P1] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/conflict/` 中验证版本冲突、重载、另存和明确覆盖
- [x] T060 [P1] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/migration/` 中验证旧文本、链接、换行、幂等、并发和来源
- [x] T061 [P1] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/lifecycle/` 中验证取消引用、回收站、恢复、purge、共享附件和孤儿清理
- [x] T062 [P0] [US5] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/knowledge/KnowledgeService.java` 中接入 PostgreSQL 搜索投影和摘要 DTO
- [x] T063 [P0] [US5] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/ai/richtext/` 中实现 AI 输入和唯一图片计费桥接
- [x] T064 [P1] [US5] 在 `frontend/kb-web/src/features/editor/conflict/` 中实现冲突对话框和本地草稿
- [x] T065 [P1] [US5] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/richtext/migration/` 中实现迁移状态、`bodyDocument=null` 过渡和链接识别
- [x] T066 [P1] [US5] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/richtext/lifecycle/` 中实现生命周期清理

## 阶段 8：完整移动、无障碍、双语与发布

- [x] T067 [P1] [P] [US5] 在 `tests/accessibility/richtext.spec.ts` 中验证工具栏、焦点、图片说明、状态播报、表格、键盘和 200% 字体
- [x] T068 [P0] [P] [US5] 在 `tests/e2e/richtext-mobile-complete.spec.ts` 中验证 320/360/390/430、横屏、软键盘和安全区
- [x] T069 [P1] [P] [US5] 在 `tests/e2e/richtext-bilingual.spec.ts` 中验证中英文事实一致
- [x] T070 [P1] [US5] 在 `frontend/kb-web/src/features/editor/` 中完成移动更多菜单、44px 控件和选区恢复
- [x] T071 [P1] [P] 在 `tests/security/richtext-corpus.spec.ts` 中执行 XSS、来源、协议和图片模糊语料
- [x] T072 [P1] [P] 在 `tests/e2e/richtext-taste.spec.ts` 中执行 1440/768/430/390/360/320、手机横屏、安全区和 200% 字体 Taste 检查
- [x] T073 [P1] 在 `tests/e2e/richtext-zero-impact.spec.ts` 中验证 `/`、`/app`、`/api`、`/bank`、`/pay`、`/apk`、Cookie、证书和 sidecar
- [x] T074 [P1] 在 `artifacts/acceptance/rich-text.md` 中映射 FR-001 至 FR-057、SC-001 至 SC-019
- [ ] T075 [P1] 在新环境执行 [quickstart.md](quickstart.md) 并写入 `artifacts/rich-text/quickstart-report.md`
- [x] T081 [P0] 修复生产富文本图片粘贴的 libvips 依赖，安装系统包并在部署切换前执行真实图片规范化检查
- [x] T082 [P0] 修复部署健康检查和门户内容检查在 `pipefail` 下的 `curl | grep -q` 竞态，避免成功 release 被误回滚

## 阶段 9：P2 加固

- [x] T076 [P2] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/richtext/recovery/` 中验证刷新、进程重启、上传暂存恢复
- [x] T077 [P2] 在 `tests/performance/richtext/` 中验证 200,000 字符、20 图、详情和移动输入
- [ ] T078 [P2] 在 `tests/usability/richtext.md` 中执行 10 人编辑和图片研究
- [x] T079 [P2] 在 `artifacts/rich-text/maintenance.md` 中记录 schema 演进、迁移回滚和依赖升级

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

### 批量完成记录：2026-10-22 富文本 MVP/P1/P2

执行角色：开发子智能体、测试子智能体。Git commit：`4ee4115`。以下每行对应一个
独立任务；验证命令均为本轮实际执行，未运行的物理设备和外部系统在“风险与回滚”中说明。

| 任务 | 摘要与详情 | 验证与证据 | 风险与回滚 |
|------|------------|------------|------------|
| T005 | 建立前后端 schema 版本、DTO、语言中立映射和共享契约测试，未复制第二份权威 schema。 | `RichTextSchemaContractTest`、`rich-text.contract.test.ts`；Maven 与 Vitest 通过。 | 删除新增契约模块即可回滚；旧 schemaVersion=1 保持不变。 |
| T006 | 建立合法、未知节点、危险属性、超限、图片、表格、迁移和安全语料夹具。 | `tests/fixtures/rich-text/manifest.json`、`validate-fixtures.mjs`；11 个 manifest 和 100 条语料检查通过。 | 夹具可独立删除；不影响运行时数据。 |
| T007 | 增加 JSON Schema、上传 API 和 CSRF 契约测试。 | `backend/.../richtext/contract/ImageUploadApiContractTest.java`；kb-app 全量测试通过。 | 契约变化必须同步 OpenAPI 和 Vue DTO。 |
| T009 | 增加 V0005 完整性迁移以及全新安装、顺序升级、约束和版本字段测试。 | 独立 PostgreSQL 18.1 实际执行 `0001 → 0004 → 0005` 成功；迁移契约测试通过。 | 迁移只能向前；回滚恢复数据库备份，不执行逆向 DDL。 |
| T013 | 实现来源类型、URL 清理、时间、converter version、哈希、所有者与转换报告。 | `RichTextProvenanceServiceTest`、`RichTextSourcePolicyTest`、`SourceProvenancePolicyTest`。 | 来源校验失败会拒绝保存；回滚需恢复旧 KnowledgeService。 |
| T014 | 建立保存计划、原子事务 writer、版本冲突和搜索修订提交边界。 | `RichTextTransactionTest`；独立数据库实际保存后文档、修订、来源和 search 行各 1 条。 | 数据库事务失败自动回滚；不删除已提交内容。 |
| T021 | 增加完整格式矩阵测试，覆盖 H2/H3/H4、行内格式、列表、引用、代码、链接、清除、撤销和重做。 | `formatting.spec.ts` 6 项通过；浏览器格式矩阵 E2E 通过。 | 仅新增测试；无运行时回滚需求。 |
| T022 | 验证键盘、撤销/重做、保存状态和重新打开。 | `frontend/kb-web/e2e/app.spec.ts` 相关用例 20 项全部通过。 | 前端源文件可按提交回滚；数据 schema 不变。 |
| T023 | 验证 320/360/390/430、横屏、44px、选区与安全区布局。 | `keeps rich text controls reachable...` 与 accessibility 用例通过。 | 真实软键盘和物理刘海仍需设备复验。 |
| T026 | 实现手动采集、编辑、保存状态、本地安全草稿和恢复。 | `drafts.test.ts`、草稿恢复 E2E、独立 PostgreSQL 实际保存与刷新通过。 | 草稿仅保存在浏览器本机；可清除本地草稿回滚。 |
| T029 | 校验 assetId、说明、替代文本、宽高比、装饰、20 图和 50 MiB。 | `RichTextImageValidationTest` 7 项通过。 | 限额变化必须同步 schema、前端和迁移。 |
| T030 | 覆盖远程、data、blob、cid、file、SVG、未扫描和失败图片。 | `ImageUploadServiceTest`、安全网络与渲染测试通过。 | 不安全来源仍 fail closed；无需数据迁移。 |
| T031 | 验证文件选择、粘贴、拖放、图片说明、尺寸、对齐和移除。 | app E2E 图片用例通过；文件输入接受 JPEG/PNG/WebP/HEIC/HEIF。 | 真实相册/相机选择器依赖设备，回滚不影响已保存附件。 |
| T032 | 验证 25 MiB、8192px、4000 万像素、SVG/动画和 HEIC 失败关闭。 | `ImageInspectorTest`、`ImageUploadServiceTest`、fixture 安全语料通过。 | 本机无真实 libvips；生产保留失败关闭。 |
| T033 | 实现配额、哈希、libvips 进程边界、安全规范化和正式附件提升。 | 相关 Java 测试与 Spotless 通过；本地原生工具缺失时拒绝上传。 | 部署前必须安装并探测 libvips；失败时恢复上一 release。 |
| T034 | 实现扫描/脱敏失败整体拒绝、对象回滚和临时上传清理边界。 | `ImageUploadServiceTest` 的 scan/storage/persistence 失败用例通过。 | 真实脱敏引擎仍需部署验收；不使用部分保存。 |
| T036 | 实现图片粘贴、拖放、失败恢复和重试。 | app E2E dropped-upload 用例通过，正文在失败后保留。 | 重试只重新上传当前文件，不创建知识版本。 |
| T037 | 实现安全资产解析、所有者隔离、content_hash 去重和 imageAssetIds 输出。 | `RichImageAssetResolutionServiceTest`、`RichTextAiInputAssemblerTest` 通过。 | 002 账本实际消费仍待外部实现；不改变图片引用。 |
| T038 | 验证图片失败不产生版本、编辑区保留并可重试。 | app E2E 与上传服务失败用例通过。 | 失败对象会被清理；可回滚服务类。 |
| T039 | 增加详情节点、标记、图片和危险内容测试。 | `features/knowledge/__tests__/richtext-view.test.ts` 4 项通过。 | 测试新增，无数据回滚。 |
| T040 | 验证桌面/手机详情、图片顺序、灯箱和编辑返回。 | app E2E detail/lightbox 用例与独立数据库刷新实际通过。 | 真实屏幕阅读器仍需人工复验。 |
| T041 | 验证脚本、事件、表单、iframe 和远程图片不执行。 | `tests/security/richtext-render.spec.ts` 2 项通过。 | 渲染逻辑保持结构化白名单；回滚仅涉及 Vue 组件。 |
| T042 | 验证外部请求为 0，图片只从认证附件端点加载。 | `tests/security/richtext-network.spec.ts` 2 项通过。 | 远程附件仍不自动加载；无需迁移。 |
| T045 | 实现键盘可访问图片灯箱、焦点恢复和 Escape 关闭。 | 图片灯箱 E2E 在桌面和 390px 通过。 | 可回滚组件；正文图片引用不受影响。 |
| T046 | 实现语义表头、窄屏局内滚动和 RichTable 渲染。 | 表格 E2E、无障碍测试与详情渲染通过。 | 表结构 schema 不变，回滚组件即可。 |
| T047 | 增加服务端 Word/Google Docs 风格 HTML、表格和纯文本回退测试。 | `HtmlToRichTextConverterTest` 4 项通过。 | 转换器仅处理输入，不修改已有文档。 |
| T048 | 验证粘贴来源、URL 清理和转换报告。 | provenance 3 组测试、浏览器粘贴安全测试通过。 | 不安全 URL 被移除；来源记录可独立保留。 |
| T049 | 验证脚本、事件、iframe、危险链接、凭据和外部请求为 0。 | `tests/security/richtext-paste.spec.ts` 4 项通过。 | 失败关闭；可回滚前端转换器。 |
| T050 | 验证表格创建、增删行列、表头、键盘和窄屏。 | app E2E 表格与无障碍用例通过。 | 表格扩展可回滚；已有表格仍按 schema 渲染。 |
| T051 | 实现保留支持格式、纯文本回退、链接清理和转换提示。 | paste Vitest 4 项、app E2E 与安全测试通过。 | 原始 HTML 不持久化；回滚后仅保留纯文本路径。 |
| T052 | 使用 jsoup 实现 Java HTML 到受限 ProseMirror 的安全转换。 | `HtmlToRichTextConverterTest` 与 kb-core 18 项测试通过。 | 新增 jsoup 依赖；删除转换器和依赖即可回滚。 |
| T053 | 实现 Tiptap Table、工具栏和增删行列模型。 | `table-model.test.ts`、app 表格 E2E 通过。 | 表头不合法时 schema 拒绝；可回滚扩展集合。 |
| T054 | 实现远程图片占位、链接清理和格式移除报告。 | paste/security 测试验证不发起网络请求并生成占位。 | 图片不会自动入库；回滚不涉及附件数据。 |
| T055 | 验证正文、表格、代码、说明和替代文本进入搜索投影。 | `SearchProjectionBuilderTest` 正向 2 组通过；独立数据库实际搜索命中 1 条。 | 搜索投影可重建，不修改正文。 |
| T056 | 验证标签名、存储键、隐藏样式和脚本不进入搜索。 | `SearchProjectionBuilderTest` 负向断言通过。 | 投影失败不回写正文；可重新生成。 |
| T057 | 验证 AI 只接收纯文本投影，并仅在授权后接收唯一图片。 | `RichTextAiInputAssemblerTest` 3 项通过。 | 未授权时图片列表为空；不调用外部 AI。 |
| T059 | 验证版本冲突、重载、另存和明确覆盖。 | `RichTextConflictPolicyTest` 4 项、app 冲突三出口 E2E 通过。 | 冲突不静默覆盖；回滚保留最新数据库版本。 |
| T060 | 验证旧文本、链接、换行、幂等、并发和来源。 | `LegacyTextDocumentConverterTest`、迁移契约与 fixture 通过。 | 迁移可重试；失败保留纯文本和迁移状态。 |
| T061 | 验证取消引用、回收站、恢复、purge、共享附件和清理状态。 | `RichTextLifecyclePolicyTest` 3 项通过。 | 清理 worker 未连接真实对象存储；回滚保留数据库引用。 |
| T063 | 实现 AI 纯文本输入和唯一图片资产桥接。 | `RichTextAiInputAssemblerTest` 通过；未授权图片不查询。 | 002 实际扣费仍为外部依赖；桥接失败不得扣费。 |
| T064 | 实现冲突对话框、本地草稿和三个明确出口。 | conflict Vitest 2 项、app E2E 通过。 | 本地草稿可清除；不覆盖服务端最新版本。 |
| T065 | 实现迁移状态、legacy 转换、链接识别和 `bodyDocument=null` 过渡。 | 迁移转换与状态测试通过；详情迁移状态测试通过。 | 未启动 Worker；可回滚状态机但保留迁移表。 |
| T066 | 实现引用取消、恢复、清理队列和共享附件保护。 | 生命周期策略测试通过；数据库服务边界可调用。 | 物理对象 worker 未实现；回滚不删除对象。 |
| T067 | 验证工具栏、焦点、图片说明、状态播报、表格、键盘和 200% 字体。 | `tests/accessibility/richtext.spec.ts` 3 项通过。 | 自动无障碍不等于人工屏幕阅读器验收。 |
| T068 | 验证 320/360/390/430、横屏、软键盘适配和安全区。 | app 视口 E2E、Taste 7 视口和 200% 字体检查通过。 | 真实软键盘和安全区仍需设备复验。 |
| T069 | 验证中英文富文本事实一致。 | app bilingual E2E 与 i18n 文案检查通过。 | 英文内容保持中文权威事实；可回滚文案。 |
| T070 | 完成移动更多菜单、44px 控件和选区恢复。 | app 四档手机宽度与横屏 E2E 通过。 | 桌面工具栏不变，可回滚 CSS/组件。 |
| T071 | 增加 100 条 XSS、来源、协议和图片模糊语料。 | `richtext-corpus.spec.ts` 全部 100 条拒绝通过。 | 新增语料只影响测试；运行时保持 fail closed。 |
| T072 | 执行多视口 Taste、横屏、安全区和 200% 字体检查。 | `richtext-taste.spec.ts` 通过；报告 `.taste-skill` 与 `artifacts/rich-text/taste-report.md`。 | 视觉回归可回滚样式提交。 |
| T073 | 验证原应用入口、健康、TLS、HSTS、`/kb` Cookie 和支付 sidecar。 | 生产公开检查 1 项通过；生产主机内部 sidecar `/health` 返回 `{"ok":true,...}`，服务 active。 | 只读检查，不修改支付、订单或凭据。 |
| T074 | 映射 FR-001–FR-057 与 SC-001–SC-019。 | `artifacts/acceptance/rich-text.md` 已生成并记录部分覆盖与外部条件。 | 文档回滚不影响运行；未覆盖项不得声明通过。 |
| T076 | 验证刷新、服务重建、上传状态和过期恢复。 | `ImageUploadRecoveryTest` 2 项通过；前端草稿恢复 E2E 通过。 | 上传状态依赖数据库；回滚服务不删除临时对象。 |
| T077 | 验证 200,000 字符、20 图、详情和移动输入性能。 | 性能测试 3 项预算通过；`artifacts/rich-text/performance-report.md`。 | 本机性能不等于生产容量；回滚不涉及数据。 |
| T079 | 记录 schema 演进、迁移回滚、浏览器和依赖升级维护流程。 | `artifacts/rich-text/maintenance.md` 已生成并通过链接/格式检查。 | 维护文档可独立回滚。 |

### 完成记录：T080

- 日期：2026-10-22
- 状态：已完成
- 执行子智能体：产品、开发、测试
- Git commit：`9d16794`
- 摘要：将 Vue 前端从浅色双菜单侧栏改为最新原型批准的深海蓝数据工作台。
- 详情：应用壳采用 72px 图标轨、顶部知识域栏和移动端六项底部导航；首页补齐统计条、
  单框检索、结果列表、知识海图和待处理侧栏；新增知识库、待处理、回收站和设置路由；
  详情改为正文与分类/来源/状态双栏；登录注册同步深海主题；字体切换为 Public Sans。
- 验证：`pnpm lint`、`pnpm typecheck`、Vitest 41 项、生产构建、20 项前端 E2E 和
  `richtext-taste.spec.ts` 全部通过；登录态 Taste inspector 在 1440/768/390 为
  `0 FAIL / 0 WARN`，采集和详情页均为 `0 FAIL`；Taste 源检查为 `0 FAIL`。
- 证据：`frontend/kb-web/src/prototype-theme.css`、`frontend/kb-web/e2e/app.spec.ts`、
  `artifacts/rich-text/prototype-alignment-report.md`、
  `.taste-skill/prototype-alignment-home-inspect/`、
  `.taste-skill/prototype-alignment-capture-inspect/`、
  `.taste-skill/prototype-alignment-detail-inspect/`。
- 生产部署：`v0.1.15-mvp` 已从 GitHub 部署，运行 release 为
  `5fbf8e6b0042-20261021211906`；门户、拾题原路由、识海登录、健康检查和注册状态接口通过，
  登录页生产 Taste 检查为 `0 FAIL`。
- 覆盖：FR-022 至 FR-028、FR-045 至 FR-048、SC-007、SC-008、SC-013 和项目宪法 XI。
- 风险与回滚：设置页的 AI 授权开关目前只保存当前浏览器偏好，服务端授权 API 接入后必须
  改为服务端权威状态；回收站页面在删除 API 接入前只显示真实空状态。回滚前端提交即可恢复
  旧视觉，不修改数据库或富文本内容。

### 完成记录：T081

- 日期：2026-10-22
- 状态：已完成
- 执行子智能体：产品、开发、测试
- Git commit：`cf76f88`
- 摘要：修复生产富文本粘贴图片因缺少 libvips 而失败的问题。
- 详情：`provision-ubuntu.sh` 安装 `libvips-tools`；`deploy.sh` 把 `vips` 加入必需命令，并在切换
  release 前调用 `check-image-normalizer.sh`，用仓库内真实 PNG 执行 `vips autorot` 和
  `[strip]` 元数据清理。缺少命令、规范化失败或输出为空时，部署在停止现有服务前终止。
- 验证：缺失依赖模拟返回退出码 1 和中文提示；生产安装 `vips-8.15.1`，以 `zhishiku` 服务
  用户完成 1×1 PNG 和 640×480 PNG 规范化；部署脚本内置检查输出
  `[preflight] libvips 图片规范化检查通过：vips-8.15.1`。
- 证据：`deploy/scripts/check-image-normalizer.sh`、`deploy/scripts/deploy.sh`、
  `deploy/scripts/provision-ubuntu.sh`、`tests/fixtures/rich-text/images/pixel.png.base64`、
  `artifacts/001/deployment-verification.md`。
- 覆盖：FR-010、FR-013、FR-021、FR-048。
- 风险与回滚：当前只验证了规范化、方向和元数据清理命令，真实密钥脱敏与 OCR 仍由后续任务
  承接。回滚应用 release 不卸载系统包；如需完全回滚，删除新增部署检查并恢复上一 release。

### 完成记录：T082

- 日期：2026-10-22
- 状态：已完成
- 执行子智能体：开发、测试
- Git commit：`48cdb70`
- 摘要：修复部署脚本将已启动 release 误判为健康检查失败的问题。
- 详情：健康检查和首页内容检查不再使用 `set -o pipefail` 下的
  `curl | grep -q`，改为先保存完整响应，再用 Bash 字符串匹配判断。新增部署脚本回归检查，
  同时确认 libvips 安装、命令预检和真实 PNG 冒烟未被后续修改移除。
- 验证：`node tests/deploy/verify-deploy-scripts.mjs` 先因旧管道写法失败，修复后通过；
  GitHub Raw 部署脚本 `bash -n` 通过；`v0.1.20-mvp` 部署退出码 0，API 健康、门户内容、
  原拾题入口和服务状态全部通过。
- 证据：`tests/deploy/verify-deploy-scripts.mjs`、`deploy/scripts/deploy.sh`、
  `artifacts/001/deployment-verification.md`。
- 覆盖：项目宪法 VII、VIII、IX；FR-048。
- 风险与回滚：本次未改变业务 API 或数据库，回滚部署脚本即可；若旧版本仍含管道竞态，
  部署可能再次出现误回滚，不应作为常规回退目标。

### 未完成与外部阻塞

- **T075**：已在独立 PostgreSQL 新环境执行 V1→V4→V5、登录、富文本保存、刷新、来源、搜索
  和详情；真实 libvips/Tesseract、S3、SMTP/AI、Worker 与物理设备条件仍缺失。
- **T078**：必须由 10 名真实参与者执行。当前仅提供协议和空白记录表，不伪造研究结果。

### 完成记录：T058

- 日期：2026-10-22
- 状态：已完成
- 执行子智能体：开发、测试
- Git commit：未提交
- 摘要：接通富文本唯一图片资产与 002 报价/预留门禁。
- 详情：计费服务按账号读取 `imageAssetIds`，只接受 clean/redacted 的安全资产，并以规范化
  `content_hash` 去重；`screenshot_ocr` 数量必须等于唯一图片数，重复正文位置不重复计费。
  图片引用不足、任务操作不匹配或预检无效时 fail closed。
- 验证：`BillingP0PolicyTest` 9 项通过，其中覆盖图片唯一数量、篡改数量和操作计费行替换；
  后端全量测试与桥接测试通过。
- 证据：`BillingService.java`、`BillingP0PolicyTest.java`、`RichImageAssetResolutionServiceTest`。
- 覆盖：FR-037、FR-044、FR-051、SC-017。
- 风险与回滚：真实资产扫描服务、真实扣费和并发图片任务仍需 PostgreSQL/Waffo 环境验收；
  回滚会重新暴露唯一图片报价缺口。
