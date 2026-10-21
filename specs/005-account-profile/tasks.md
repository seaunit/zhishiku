# 任务：账户菜单与个人资料

**输入**：`specs/005-account-profile/`

**优先级**：P0 为本次 MVP 必须完成项；P1 为完整交付和视觉验收。

## 阶段 1：账户资料基础

- [x] T001 [P0] [P] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/profile/` 中先验证显示名称规范化、1–40 字符、控制字符、HTML 片段和当前账号隔离
- [x] T002 [P0] [P] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/profile/` 中先验证资料 GET/PATCH 的认证、CSRF、错误响应和不回显敏感字段
- [x] T003 [P0] 在 `backend/kb-app/src/main/resources/db/migration/V0007__account_profile.sql` 中新增 `display_name`、回填旧账号、非空约束和 1–40 字符约束
- [x] T004 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/owner/OwnerAccountEntity.java` 和 `OwnerService.java` 中实现显示名称读取、规范化和当前账号更新
- [x] T005 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/profile/` 中实现 Profile DTO、Controller 和当前会话资料 API
- [x] T006 [P0] 在 `frontend/kb-web/src/types/api.ts`、`api/client.ts` 和 `stores/session.ts` 中增加资料类型、读写请求、会话同步和资料更新测试

## 阶段 2：用户故事 1 — 头像账户菜单

- [x] T007 [P0] [US1] 在 `frontend/kb-web/e2e/app.spec.ts` 中验证点击头像不退出、菜单内容、点击外部、`Esc`、焦点返回和键盘操作
- [x] T008 [P0] [US1] 在 `frontend/kb-web/src/components/AccountMenu.vue` 和 `AppShell.vue` 中实现可访问账户菜单和头像首字同步
- [x] T009 [P1] [US1] 在 `frontend/kb-web/src/i18n.ts` 中补齐账户菜单中文和英文文案

## 阶段 3：用户故事 2 — 查看与编辑个人资料

- [x] T010 [P0] [US2] 在 `frontend/kb-web/src/views/ProfileView.vue` 和相关 Vitest 中实现资料查看、显示名编辑、取消、保存成功和错误保留输入
- [x] T011 [P0] [US2] 在 `frontend/kb-web/src/router.ts` 中增加 `/profile` 路由并从账户菜单进入
- [x] T012 [P1] [US2] 在 `frontend/kb-web/src/i18n.ts` 和 `styles.css` 中补齐资料页双语、桌面与 320–430px 移动布局
- [x] T013 [P1] [US2] 在 `prototype/index.html`、`app.js` 和 `styles.css` 中同步账户菜单与个人资料原型

## 阶段 4：用户故事 3 — 确认退出

- [x] T014 [P0] [US3] 在 `frontend/kb-web/e2e/app.spec.ts` 中验证退出取消保持登录、确认后跳转、后退不能读取资料
- [x] T015 [P0] [US3] 在 `frontend/kb-web/src/components/ConfirmDialog.vue` 和 `AccountMenu.vue` 中实现退出确认、焦点管理和双语文案

## 阶段 5：验收与发布

- [x] T016 [P1] 执行 Taste app-screen 检查 1440/768/390、补充 320/430，并记录 `.taste-skill/account-profile/` 和 `artifacts/account-profile/`
- [x] T017 [P1] 执行后端、前端、Playwright、构建和零影响检查，更新 `artifacts/account-profile/verification.md`
- [x] T018 [P1] 从 GitHub 部署固定 tag，验证 `/kb`、`/kb/profile`、原拾题入口、资料保存和退出，并记录生产 release

## 依赖关系

- T001–T006 阻塞所有用户故事。
- US1 完成后可进入资料页和退出确认；US2 与 US3 共用菜单。
- T016–T018 必须在所有 P0 任务完成后执行。

## 完成记录

### 批量完成记录：2026-10-22 账户菜单 MVP

**执行角色**：产品、开发、测试。**Git commit**：`e4ccf5b`。

| 任务 | 摘要与详情 | 验证与证据 | 风险与回滚 |
|------|------------|------------|------------|
| T001–T002 | 先建立显示名称、资料 API、认证、CSRF、账号隔离和敏感字段测试，再实现后端。 | 资料专项 16 项通过；后端全量 131 项通过。 | 删除 V0007 与资料模块需恢复数据库备份。 |
| T003–T005 | 新增 `display_name` 迁移和回填；实现当前账号资料 GET/PATCH 与会话响应 displayName。 | Flyway 契约通过；生产 `0007:true`，非法空值 0。 | 迁移只向前；回滚恢复数据库备份。 |
| T006 | 增加前端资料类型、API、会话恢复和资料同步。 | lint、typecheck、Vitest 和 build 通过。 | 前端 release 可回滚，数据不变。 |
| T007–T009 | 实现头像账户菜单、焦点管理、双语和显示名称同步。 | Playwright 账户菜单、点击外部、Esc、320px 共 4 项通过。 | 菜单组件可独立回滚。 |
| T010–T013 | 实现个人资料页、资料原型、只读邮箱、账户边界和 Unicode 显示名称校验。 | profile E2E、原型交互、Taste `0 FAIL`；生产资料保存与恢复通过。 | 头像上传仍不在范围，可回滚页面与原型。 |
| T014–T015 | 实现退出确认、焦点陷阱、失败保留会话和取消/确认路径。 | Playwright 退出取消、Esc、确认、500 失败保留会话通过。 | 回滚菜单与确认框即可，不影响服务端 logout。 |
| T016 | 执行 Taste 源检查与 1440/768/390 App Screen 检查，补充 320px。 | `0 FAIL`，保留既有导航 tiny-text WARN；证据在 `.taste-skill/account-profile/`。 | 视觉回滚仅涉及 CSS 和组件。 |
| T017 | 执行完整后端、前端、E2E、构建、原型和零影响验证。 | 后端 131、Vitest 41、Playwright 31、构建和 Taste lint 通过。 | 无业务数据变更；见 `artifacts/account-profile/verification.md`。 |
| T018 | 从 GitHub 部署 `v0.1.21-mvp`，执行生产资料保存、原名恢复和退出。 | release `e4ccf5bb6f63-20261021223455`；登录/资料/更新/恢复 200，logout 204，随后 me 401。 | release 可回滚；数据库迁移不可逆向，需备份。 |
