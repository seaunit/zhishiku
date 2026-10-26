# 任务：一键全部复核

**输入**：`specs/013-bulk-review/`

## Phase 1：基础

- [x] T001 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/knowledge/KnowledgeDtos.java` 中新增批量复核结果 DTO
- [x] T002 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/knowledge/KnowledgeItemRepository.java` 中增加当前账号待复核条目的悲观锁查询

## Phase 2：后端

- [x] T003 [P0] [US1] 在 `KnowledgeService.java` 中实现单事务全部复核，只处理 `active + review`
- [x] T004 [P0] [US1] 在 `KnowledgeController.java` 中增加 `POST /knowledge-items/review-all`
- [x] T005 [P0] [US1] 在 `KnowledgeLifecycleControllerTest.java` 中覆盖成功、未登录和 CSRF 拒绝
- [x] T006 [P0] [US1] 在 `KnowledgeServiceReviewAllTest.java` 中覆盖账号隔离、全量更新、重复执行和事务调用

## Phase 3：原型与前端

- [x] T007 [P0] [US1] 在 `prototype/index.html`、`prototype/app.js` 和 `prototype/styles.css` 中增加全部复核按钮、确认框和状态反馈
- [x] T008 [P0] [US1] 在 `frontend/kb-web/src/api/client.ts` 和 `types/api.ts` 中增加批量复核 API
- [x] T009 [P0] [US1] 在 `ProcessingView.vue` 中增加全量待复核计数、标题按钮、确认框、busy、成功和失败状态
- [x] T010 [P0] [US1] 在 `ConfirmDialog.vue` 和 `prototype-theme.css` 中支持非危险主操作并完成移动端布局
- [x] T011 [P0] [US1] 在 `i18n.ts` 中补齐中英文按钮、确认框和结果文案
- [x] T012 [P0] [US1] 在 `client.test.ts` 和 `e2e/app.spec.ts` 中覆盖接口请求、确认取消、批量成功和三种视口

## Phase 4：验证

- [x] T013 [P0] 运行后端目标测试和全量测试
- [x] T014 [P0] 运行前端 lint、typecheck、Vitest、Playwright 和生产构建
- [x] T015 [P0] 执行 Taste lint/inspect，检查 1440、768、390 的原型与前端截图
- [x] T016 [P0] 执行 `git diff --check`、敏感信息检查并完成记录

## 依赖

- T001、T002 完成后执行 T003。
- T003 完成后执行 T004–T006。
- T007 先完成原型，再执行 T008–T012 的前端实现。
- T013–T016 在所有实现后执行。

## 完成记录

| 任务 | 日期 | 状态 | 摘要与详情 | 验证证据 | Git |
|---|---|---|---|---|---|
| T001 | 2026-10-27 | 完成 | 新增 `ReviewAllResponse`。 | `KnowledgeDtos.java`；Controller 测试。 | 未提交 |
| T002 | 2026-10-27 | 完成 | 新增按 owner、active、review 的悲观锁查询。 | `KnowledgeItemRepository.java`；Service 测试。 | 未提交 |
| T003 | 2026-10-27 | 完成 | 单事务批量复核并返回实际数量。 | `KnowledgeServiceReviewAllTest` 2 项。 | 未提交 |
| T004 | 2026-10-27 | 完成 | 新增 `POST /knowledge-items/review-all`。 | `KnowledgeLifecycleControllerTest`。 | 未提交 |
| T005 | 2026-10-27 | 完成 | 覆盖成功、未登录和 CSRF 拒绝。 | Controller 测试 15 项通过。 | 未提交 |
| T006 | 2026-10-27 | 完成 | 覆盖账号边界、更新和重复执行。 | Service 测试 2 项通过。 | 未提交 |
| T007 | 2026-10-27 | 完成 | 原型新增按钮、数量、确认框和结果反馈。 | `prototype/index.html`、`app.js`、`styles.css`。 | 未提交 |
| T008 | 2026-10-27 | 完成 | 增加前端类型和 CSRF POST 调用。 | `client.test.ts` 17 项通过。 | 未提交 |
| T009 | 2026-10-27 | 完成 | 页面加载全量复核计数，加入确认、busy、成功和失败状态。 | `ProcessingView.vue`。 | 未提交 |
| T010 | 2026-10-27 | 完成 | 确认框支持非危险主操作；手机按钮全宽。 | 1440/768/390 截图。 | 未提交 |
| T011 | 2026-10-27 | 完成 | 中文和英文文案完整。 | `i18n.ts`；类型检查通过。 | 未提交 |
| T012 | 2026-10-27 | 完成 | 覆盖 API、取消、确认、批量成功和三种视口。 | 相关 Playwright 4 项通过。 | 未提交 |
| T013 | 2026-10-27 | 完成 | 后端目标测试和全量测试通过。 | Maven 23 + 326 项，共 349 项，0 失败。 | 未提交 |
| T014 | 2026-10-27 | 完成 | lint、typecheck、Vitest、Playwright、build 通过。 | Vitest 59 项；Vite build 成功。 | 未提交 |
| T015 | 2026-10-27 | 完成 | Taste lint 0 FAIL/0 WARN；inspect 0 FAIL/1 WARN。 | 截图 `.taste-skill/review-all/`、`test-results/review-all-*.png`。 | 未提交 |
| T016 | 2026-10-27 | 完成 | 真实接口复核 2 条，复核后 0 条，重复执行返回 0。 | 本地 API 冒烟；`git diff --check` 通过；敏感扫描 clean。 | 未提交 |

### 视觉警告说明

`inspect.mjs` 的唯一 WARN 是既有高密度队列中 55% 文本小于 13px。该队列的正文标题为 14px，
来源、日期和状态说明使用 11–12px 元数据，符合 app screen 对元信息字号的范围；本次新增按钮和
确认框均保持 13–16px，不存在溢出或重叠。
