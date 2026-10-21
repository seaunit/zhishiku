# 实施计划：账户菜单与个人资料

**功能**：`005-account-profile`

**日期**：2026-10-22

**规格**：[spec.md](spec.md)

## 摘要

把头像从直接退出改为账户菜单入口。菜单提供当前账号摘要、个人资料和退出登录；个人资料页展示
头像预览、只读邮箱，并允许编辑显示名称；退出登录使用确认对话框。

后端复用现有 Spring Security Session 和 `owner_account`，增加 `display_name` 迁移、当前账号
资料查询与更新接口。前端复用 Pinia 会话状态、现有 AppShell、REST 客户端和双语 i18n。

## 技术背景

**前端**：Vue 3、TypeScript、Vite、Vue Router、Pinia、vue-i18n、Lucide Vue。

**后端**：Java 17、Spring Boot 3.3、Spring Security、Spring Data JPA、Bean Validation。

**存储**：PostgreSQL 18；Flyway 新增 `V0007__account_profile.sql`。

**测试**：JUnit 5、Mockito、MockMvc/RestAssured、Vitest、Vue Test Utils、Playwright、Taste。

**目标平台**：桌面、平板和 320–430 CSS 像素移动浏览器。

## 宪法检查

| 门禁 | 结果 | 证据 |
|------|------|------|
| SDD 与中文文档 | 通过 | 规格、计划、数据、契约、快速验证和任务均为中文 |
| 产品/开发/测试分离 | 通过 | 产品先冻结范围，开发实现，测试独立验收 |
| 默认中文的双语交付 | 通过 | 菜单、资料、错误和退出确认补齐中英文 |
| 知识安全与多账号隔离 | 通过 | 只从会话确定账号，不接收客户端账号标识 |
| Taste 视觉验证 | 通过 | 组件沿用现有深海蓝 App Shell，执行真实浏览器检查 |
| 密钥安全 | 通过 | 不新增凭据；错误响应不返回会话令牌或密码哈希 |
| GitHub 一键部署 | 通过 | Flyway 前向迁移，release 可回滚 |
| 拾题零影响 | 通过 | 只改 `/kb` 的账号资料与前端交互 |
| 任务可追溯 | 通过 | 任务完成后勾选并记录验证与 SHA |

## 架构决策

1. **复用 `owner_account`。**
   仅新增 `display_name`，不为资料建立第二套账户表。

2. **显示名称是界面身份，邮箱是登录身份。**
   邮箱只读；显示名称不参与登录、唯一性、搜索或 AI。

3. **服务端从认证会话确定账号。**
   `GET/PATCH /api/v1/profile` 不接受客户端 `ownerId`；只操作当前认证账号。

4. **资料更新与退出继续使用 CSRF 保护。**
   `PATCH /profile` 和 `POST /auth/logout` 均为非安全方法，由现有请求客户端附加 CSRF。

5. **头像继续使用首字。**
   显示名称优先，缺失时回退邮箱首字；本次不引入头像上传、对象存储和图片扫描。

6. **退出采用确认对话框。**
   头像点击只打开菜单；退出操作必须由菜单项触发并再次确认。

7. **原型与正式前端同一交付边界。**
   账号菜单、资料页和退出确认同步更新 `prototype/`、Vue 实现和 SDD 任务。

## 项目结构

```text
backend/kb-app/src/main/resources/db/migration/V0007__account_profile.sql
backend/kb-app/src/main/java/site/seaunit/zhishiku/owner/
backend/kb-app/src/main/java/site/seaunit/zhishiku/profile/
backend/kb-app/src/test/java/site/seaunit/zhishiku/profile/
frontend/kb-web/src/components/AccountMenu.vue
frontend/kb-web/src/components/ConfirmDialog.vue
frontend/kb-web/src/views/ProfileView.vue
frontend/kb-web/src/stores/session.ts
frontend/kb-web/src/api/client.ts
frontend/kb-web/src/i18n.ts
frontend/kb-web/e2e/app.spec.ts
prototype/index.html
prototype/app.js
prototype/styles.css
```

## 数据与接口

- 数据模型见 [data-model.md](data-model.md)。
- 资料 API 见 [contracts/profile.openapi.yaml](contracts/profile.openapi.yaml)。
- `SessionResponse` 增加 `displayName`，用于顶部头像和账户菜单即时显示。
- 更新成功后返回最新资料，并同步 Pinia 会话状态。

## 交互与视觉

- 头像按钮使用 `aria-haspopup="menu"`、`aria-expanded` 和明确账户名称。
- 菜单浮层使用现有深色表面和 1px 边界，不使用渐变、玻璃或装饰性光晕。
- 显示名称、邮箱和菜单项保持单行省略；长内容不得撑破视口。
- 320–430px 下菜单宽度受视口约束，菜单项和退出按钮保持至少 44px 高。
- 资料页沿用设置页的单栏或标签/面板结构，不形成新的视觉系统。

## 验证策略

- 后端：显示名称规范化、危险输入、当前账号隔离、CSRF、会话失效。
- 前端：头像点击不退出、菜单键盘与点击外部、资料保存同步、退出取消与确认。
- 视觉：Taste app-screen 检查 1440/768/390，并补充 320/430 溢出检查。
- 零影响：原有 `/`、`/app`、`/bank`、`/login`、`/kb` 和健康检查保持。

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| 旧账号无显示名称 | 迁移时回填邮箱本地部分并截断到 40 字符 |
| 长名称撑破菜单 | 服务端限制 40 字符，前端单行省略和最大宽度 |
| 菜单误触退出 | 头像不直接退出，退出必须二次确认 |
| 跨账号资料写入 | 服务端只使用认证会话身份 |
| 退出后浏览器后退 | 服务端会话失效，受保护 API 返回 401 |
