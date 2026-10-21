# 任务：账户菜单与个人资料

**输入**：`specs/005-account-profile/`

**优先级**：P0 为本次 MVP 必须完成项；P1 为完整交付和视觉验收。

## 阶段 1：账户资料基础

- [ ] T001 [P0] [P] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/profile/` 中先验证显示名称规范化、1–40 字符、控制字符、HTML 片段和当前账号隔离
- [ ] T002 [P0] [P] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/profile/` 中先验证资料 GET/PATCH 的认证、CSRF、错误响应和不回显敏感字段
- [ ] T003 [P0] 在 `backend/kb-app/src/main/resources/db/migration/V0007__account_profile.sql` 中新增 `display_name`、回填旧账号、非空约束和 1–40 字符约束
- [ ] T004 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/owner/OwnerAccountEntity.java` 和 `OwnerService.java` 中实现显示名称读取、规范化和当前账号更新
- [ ] T005 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/profile/` 中实现 Profile DTO、Controller 和当前会话资料 API
- [ ] T006 [P0] 在 `frontend/kb-web/src/types/api.ts`、`api/client.ts` 和 `stores/session.ts` 中增加资料类型、读写请求、会话同步和资料更新测试

## 阶段 2：用户故事 1 — 头像账户菜单

- [ ] T007 [P0] [US1] 在 `frontend/kb-web/e2e/app.spec.ts` 中验证点击头像不退出、菜单内容、点击外部、`Esc`、焦点返回和键盘操作
- [ ] T008 [P0] [US1] 在 `frontend/kb-web/src/components/AccountMenu.vue` 和 `AppShell.vue` 中实现可访问账户菜单和头像首字同步
- [ ] T009 [P1] [US1] 在 `frontend/kb-web/src/i18n.ts` 中补齐账户菜单中文和英文文案

## 阶段 3：用户故事 2 — 查看与编辑个人资料

- [ ] T010 [P0] [US2] 在 `frontend/kb-web/src/views/ProfileView.vue` 和相关 Vitest 中实现资料查看、显示名编辑、取消、保存成功和错误保留输入
- [ ] T011 [P0] [US2] 在 `frontend/kb-web/src/router.ts` 中增加 `/profile` 路由并从账户菜单进入
- [ ] T012 [P1] [US2] 在 `frontend/kb-web/src/i18n.ts` 和 `styles.css` 中补齐资料页双语、桌面与 320–430px 移动布局
- [ ] T013 [P1] [US2] 在 `prototype/index.html`、`app.js` 和 `styles.css` 中同步账户菜单与个人资料原型

## 阶段 4：用户故事 3 — 确认退出

- [ ] T014 [P0] [US3] 在 `frontend/kb-web/e2e/app.spec.ts` 中验证退出取消保持登录、确认后跳转、后退不能读取资料
- [ ] T015 [P0] [US3] 在 `frontend/kb-web/src/components/ConfirmDialog.vue` 和 `AccountMenu.vue` 中实现退出确认、焦点管理和双语文案

## 阶段 5：验收与发布

- [ ] T016 [P1] 执行 Taste app-screen 检查 1440/768/390、补充 320/430，并记录 `.taste-skill/account-profile/` 和 `artifacts/account-profile/`
- [ ] T017 [P1] 执行后端、前端、Playwright、构建和零影响检查，更新 `artifacts/005-account-profile/verification.md`
- [ ] T018 [P1] 从 GitHub 部署固定 tag，验证 `/kb`、`/kb/profile`、原拾题入口、资料保存和退出，并记录生产 release

## 依赖关系

- T001–T006 阻塞所有用户故事。
- US1 完成后可进入资料页和退出确认；US2 与 US3 共用菜单。
- T016–T018 必须在所有 P0 任务完成后执行。

## 完成记录

每项任务完成后勾选，并记录日期、执行角色、Git commit、摘要、详情、验证命令、证据、风险和回滚。
