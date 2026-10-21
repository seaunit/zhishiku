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
- [ ] T027 [P1] 在 `artifacts/003/acceptance.md` 中映射 SC-001 至 SC-016 和证据

## 阶段 7：P2 加固

- [ ] T028 [P2] 在 `frontend/kb-web/tests/e2e/mobile-slow-network.spec.ts` 中验证慢网、超时和恢复
- [ ] T029 [P2] 在 `frontend/kb-web/tests/e2e/mobile-orientation.spec.ts` 中验证方向切换、软键盘和安全区
- [ ] T030 [P2] 在 `artifacts/003/usability.md` 中执行真实手机和平板可用性研究

## 依赖关系

- 001 的 Vue 外壳、搜索和 Spring 认证必须先完成。
- 004 的编辑器基础完成后执行移动工具栏。
- 002 的报价确认和测试 Waffo 完成后执行支付返回。
- Taste 和 Nginx staging 是发布门禁。
