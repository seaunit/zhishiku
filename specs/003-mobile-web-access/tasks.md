# 任务：识海移动网页访问

**输入**：`specs/003-mobile-web-access/`

**前置条件**：[spec.md](spec.md)、[plan.md](plan.md)，以及 001/002/004 的 Vue 与 Spring 基础任务。

**测试**：测试先行。Vue 使用 Vitest/Vue Test Utils，E2E 使用 Playwright 和 axe-core。

## 阶段 1：共享移动门禁

- [ ] T001 [P0] [P] [US1] 在 `frontend/kb-web/tests/e2e/mobile-matrix.spec.ts` 中建立 1440/768/430/390/360/320、手机横屏和 200% 字体矩阵
- [ ] T002 [P0] [P] [US1] 在 `frontend/kb-web/tests/e2e/mobile-shell.spec.ts` 中验证底部导航、安全区、Toast 和页面无横向滚动
- [ ] T003 [P0] [P] [US2] 在 `frontend/kb-web/src/features/knowledge/__tests__/mobile-search.spec.ts` 中验证单搜索框、结果摘要和触控操作
- [ ] T004 [P0] [P] [US3] 在 `frontend/kb-web/src/features/editor/__tests__/mobile-toolbar.spec.ts` 中验证软键盘、选区、44px、更多菜单和图片入口
- [ ] T005 [P0] [P] [US4] 在 `frontend/kb-web/src/features/billing/__tests__/mobile-checkout.spec.ts` 中验证报价确认、余额不足、Waffo 跳转和返回恢复
- [ ] T006 [P0] [P] [US5] 在 `frontend/kb-web/tests/accessibility/mobile.spec.ts` 中使用 axe-core 验证标签、焦点、触控和 200% 字体

## 阶段 2：移动外壳与导航

- [ ] T007 [P0] [US1] 在 `frontend/kb-web/src/layouts/AppShell.vue` 中实现桌面侧栏、平板顶栏和手机底部导航
- [ ] T008 [P0] [US1] 在 `frontend/kb-web/src/styles/tokens.css` 和 `mobile.css` 中实现安全区、动态 viewport、间距和 44px 触控令牌
- [ ] T009 [P0] [US1] 在 `frontend/kb-web/src/components/NavigationDrawer.vue` 中实现移动菜单、工作区标识和当前路由状态
- [ ] T010 [P0] [US1] 在 `frontend/kb-web/src/components/ToastHost.vue` 和 `DialogHost.vue` 中处理安全区、软键盘和焦点返回

## 阶段 3：搜索、知识库与详情

- [ ] T011 [P0] [US2] 在 `frontend/kb-web/src/features/search/SearchHome.vue` 中实现手机单搜索框和个人优先结果
- [ ] T012 [P0] [US2] 在 `frontend/kb-web/src/features/library/KnowledgeList.vue` 中把表格转为两行列表，保留标题、标签、来源和更新时间
- [ ] T013 [P0] [US2] 在 `frontend/kb-web/src/features/knowledge/KnowledgeDetail.vue` 中实现手机富文本、图片、代码、表格局内滚动和编辑入口
- [ ] T014 [P0] [US2] 在 `frontend/kb-web/src/features/capture/CapturePage.vue` 中实现手机手动、网页、文档和截图入口

## 阶段 4：移动富文本与附件

- [ ] T015 [P0] [US3] 在 `frontend/kb-web/src/features/editor/EditorToolbar.vue` 中实现两行或更多菜单、固定链接/图片和焦点恢复
- [ ] T016 [P0] [US3] 在 `frontend/kb-web/src/features/editor/EditorSurface.vue` 中实现 VisualViewport 跟随和保存操作可达
- [ ] T017 [P0] [US3] 在 `frontend/kb-web/src/features/editor/ImageActions.vue` 中实现图片说明、替代文本、尺寸和对齐的手机操作
- [ ] T018 [P1] [US3] 在 `frontend/kb-web/src/features/editor/TableActions.vue` 中实现表格增删行列和局内滚动

## 阶段 5：计费与弱网恢复

- [ ] T019 [P0] [US4] 在 `frontend/kb-web/src/features/billing/QuoteSheet.vue` 中实现移动报价、上限、余额和确认
- [ ] T020 [P0] [US4] 在 `frontend/kb-web/src/features/billing/CheckoutReturn.vue` 中实现非敏感订单上下文、确认中和主动查询恢复
- [ ] T021 [P0] [US4] 在 `frontend/kb-web/src/composables/useNetworkRecovery.ts` 中实现刷新、重试、离线提示和本地草稿恢复
- [ ] T022 [P0] [US4] 在 `frontend/kb-web/tests/e2e/mobile-payment-return.spec.ts` 中验证弱网、返回、重复通知和内容保留

## 阶段 6：双语、Taste 与生产形态

- [ ] T023 [P1] [P] [US5] 在 `frontend/kb-web/tests/e2e/mobile-bilingual.spec.ts` 中验证默认中文、完整英文和文本不溢出
- [ ] T024 [P1] [P] [US5] 在 `frontend/kb-web/tests/e2e/mobile-taste.spec.ts` 中执行完整 Taste 矩阵并保存截图
- [ ] T025 [P1] [US5] 在 `deploy/nginx/kb.conf` 和 `frontend/kb-web/tests/e2e/mobile-nginx.spec.ts` 中验证生产构建、SPA fallback、`/kb/api`、Cookie Path 和 CSRF
- [ ] T026 [P1] [US5] 在 `frontend/kb-web/tests/e2e/desktop-regression.spec.ts` 中验证移动适配未改变桌面搜索、编辑、计费和回收站
- [x] T027 [P1] 在 `artifacts/003/acceptance.md` 中映射 SC-001 至 SC-016 和证据

## 阶段 7：P2 加固

- [ ] T028 [P2] 在 `frontend/kb-web/tests/e2e/mobile-slow-network.spec.ts` 中验证慢网、超时和恢复
- [ ] T029 [P2] 在 `frontend/kb-web/tests/e2e/mobile-orientation.spec.ts` 中验证方向切换、软键盘和安全区
- [ ] T030 [P2] 在 `artifacts/003/usability.md` 中执行真实手机和平板可用性研究

## 阶段 8：2026-10-22 本地移动收口

- [x] T031 [P0] 在 `AppShell.vue` 与 `prototype-theme.css` 中实现七项手机底部导航、安全区、
  320–430 像素布局和每项不小于 44px 的触控目标。
- [x] T032 [P0] 在 `HomeView.vue`、`LibraryView.vue`、`DetailView.vue` 和 `TrashView.vue`
  中实现手机搜索、结果摘要、富文本详情、分类纠错和回收站恢复。
- [x] T033 [P0] 在 `RichEditor.vue`、移动工具栏样式和富文本移动 E2E 中实现选区保留、
  更多格式菜单、图片入口和软键盘可达布局。
- [x] T034 [P0] 在 `BillingView.vue`、采集报价和详情重新分析中实现移动端余额、套餐、确认中、
  自动扣点、订单和退款状态。
- [x] T035 [P0] 在采集页实现截图文档入口边界、AI 授权记忆、错误保留和稳定重试键。
- [ ] T036 [P1] 在真实 iOS Safari、Android Chrome、弱网、外部支付返回和屏幕阅读器上执行
  发行验收；当前仅有桌面 Chrome 多视口夹具证据。

## 依赖关系

- 001 的 Vue 外壳、搜索和 Spring 认证必须先完成。
- 004 的编辑器基础完成后执行移动工具栏。
- 002 的报价确认和测试 Waffo 完成后执行支付返回。
- Taste 和 Nginx staging 是发布门禁。

## Completion Log

### 批量完成记录：2026-10-22 本地移动收口

执行角色：产品、开发、测试子智能体。Git commit：未提交。

| 任务 | 摘要与详情 | 验证与证据 | 风险与回滚 |
|------|------------|------------|------------|
| T031 | 手机底栏改为首页、知识库、采集、待处理、点数、回收站、设置七项；移动端按剩余宽度均分，每项最小 44px；桌面图标轨保持不变。 | `app.spec.ts` 390px 触控尺寸断言；Vite build；Playwright 37 项通过。 | 320px 下英文长标签可能压缩；回滚 CSS 可恢复六项布局。 |
| T032 | 首页、知识库、详情、分类纠错和回收站完整接入真实 API 契约，长内容和来源 URL 可换行。 | 分类/回收站 Playwright、隐藏表格与 200% 字体测试、Vitest。 | 真实软键盘和物理刘海仍待 T036。 |
| T033 | 移动工具栏保留选区、更多菜单、44px 控件、表格和图片说明；上传失败保留正文。 | 根视觉套件、app 富文本 E2E、无障碍 3 项通过。 | 真实物理键盘和屏幕阅读器未执行。 |
| T034 | 计费页显示余额、三档套餐、订单、账本、自动扣点、退款状态；采集报价和详情重新分析提供移动确认。 | app 计费 E2E、详情报价代码、前端 typecheck/build。 | 真实 Waffo 跳转与返回仍待测试商户和生产验收。 |
| T035 | 采集页读取账号级 AI 授权；文本、网页、文档、截图入口保留内容与错误；服务端幂等键在失败重试时复用。 | 五模式 E2E、AI 文字/网页/文档/截图 E2E、专项后端测试。 | 文件上传进度和取消仍是浏览器原生状态；回滚入口会降低可恢复性。 |
| T027 | 新增移动端验收记录，列出本地自动化证据以及真实设备、弱网支付、屏幕阅读器和生产发布门禁。 | `artifacts/003/acceptance.md`；Playwright 39 项中 37 项通过、2 项 live 跳过；根视觉套件 19 项通过。 | 文档不把夹具结果冒充真实设备或生产验收。 |
