# 实施计划：管理员账号管理台

**功能分支**：`011-admin-console`

**日期**：2026-10-26

**规格**：[spec.md](./spec.md)

**输入**：仅管理员 `a115846` 可访问的账号管理页，支持账号查询、启用/禁用和赠送点数。

## 摘要

在现有 Spring Boot 账号、会话和点数账本之上增加管理员角色与固定用户名。管理员使用当前登录页，
输入 `a115846`、部署时注入的密码和图形验证码。后端以数据库中的角色、用户名和启用状态作为唯一
权限依据；账号列表通过 PostgreSQL 查询按账号聚合知识数、点数字段和管理审计。赠送点数复用
`manual_adjustment` 批次、点数账户和只追加账本，状态变更与赠送均使用持久化幂等键。

前端新增 `/admin/users` 页面及条件显示的侧栏入口。页面使用已确认的“Framed sheet”方向：现有
深海蓝品牌色只保留在导航标识、选中下划线和点数数据中，账号表格与操作面板保持安静、可扫描。
桌面采用筛选条和规则式表格，手机折叠为两行账号条目和独立操作面板。

## 技术上下文

**语言与版本**：Java 17、TypeScript 5.9、Vue 3.5

**核心依赖**：Spring Boot 3.3、Spring Security、Spring Session JDBC、Spring Data JPA、
`JdbcClient`/`JdbcTemplate`、Flyway、Vue Router、Pinia、Lucide Vue、Tiptap（本功能不新增依赖）

**存储**：PostgreSQL 18；新增 `owner_account.username`、角色、状态和 `admin_audit_event`

**测试**：JUnit 5、Spring MockMvc、Mockito、Vitest、Playwright、Taste lint/inspect

**目标平台**：桌面和移动浏览器，生产入口 `https://seaunit.site/kb/admin/users`

**项目类型**：Vue 单体前端 + 模块化 Spring Boot Web 服务

**性能目标**：1,000 个账号规模下列表首屏查询目标低于 500 ms；搜索与分页均走索引；管理操作
在常规网络条件下 2 秒内返回

**约束**：

- 不新增依赖，不引入 Redis、消息总线或独立管理后台。
- 管理员密码只从 `SHIHAI_ADMIN_PASSWORD` 或受管密钥服务读取。
- 普通账号注册、知识隔离、AI 计费、Waffo 支付和拾题应用边界保持不变。
- 账号禁用必须使已有 Spring Security 会话在后续请求中失效。
- 管理页面和接口默认中文、支持英文，适配 320–1440 CSS 像素。

**规模**：MVP 账号量按 1,000–100,000 条设计；管理页默认 20 条/页，最多 100 条；搜索限定
邮箱、用户名和显示名称。

## 宪法检查

*进入研究与设计后均需复核。*

### 实施前

- **规格驱动开发**：通过。已建立规格、质量检查清单、计划和任务后才编码。
- **角色职责**：通过。需求评审、任务拆分、实现和测试按阶段顺序完成，不使用子智能体。
- **中文双语**：通过。SDD 文档使用中文，界面同时提供中英文。
- **知识隔离**：通过。管理列表仅返回账号元数据和聚合点数，不读取知识正文或聊天内容。
- **密钥安全**：通过。管理员密码不入库明文，不写文档，只保存 BCrypt 哈希。
- **点数完整性**：通过。赠送使用批次、账户投影和账本原子事务，禁止直接改余额。
- **原型一致性**：通过。先用 Taste 技能确定管理页方向并创建原型，再同步 Vue 实现和视觉验证。
- **现有应用零影响**：通过。仅修改 `/kb` 应用，不触碰拾题 Nginx、systemd、数据库或支付 sidecar。

### 设计后复核

- 所有管理 API 都通过统一管理员授权服务，不依赖前端路由守卫。
- 账号状态过滤器只查询已认证会话对应账号，不扫描其他账号数据。
- 管理列表查询不联表知识正文，仅使用聚合计数。
- 审计仅保存安全原因和操作元数据，过滤明文密码、令牌、密钥和知识内容。

## 项目结构

### 本功能文档

```text
specs/011-admin-console/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
├── checklists/
│   └── requirements.md
└── tasks.md
```

### 源码

```text
backend/kb-app/src/main/java/site/seaunit/zhishiku/
├── admin/
│   ├── AdminAccountService.java
│   ├── AdminAccountController.java
│   ├── AdminAccountDtos.java
│   ├── AdminAccountException.java
│   └── AdminAuditRepository.java
├── owner/
│   ├── OwnerAccountEntity.java
│   ├── OwnerService.java
│   └── OwnerAccountRepository.java
├── security/
│   ├── OwnerSessionStatusFilter.java
│   └── OwnerUserDetailsService.java
├── billing/BillingService.java
└── resources/db/migration/V0019__admin_accounts.sql

backend/kb-app/src/test/java/site/seaunit/zhishiku/
├── admin/AdminAccountControllerTest.java
├── admin/AdminAccountServiceTest.java
├── security/OwnerSessionStatusFilterTest.java
└── auth/AuthControllerCaptchaTest.java

frontend/kb-web/src/
├── views/AdminUsersView.vue
├── components/AppShell.vue
├── router.ts
├── api/client.ts
├── types/api.ts
└── i18n.ts

frontend/kb-web/e2e/
└── admin-users.spec.ts

prototype/
├── admin.html
└── admin.css
```

**结构决定**：沿用现有前后端单仓库和领域包结构。管理员功能拆为 `admin` 领域包，账号字段和登录
逻辑归 `owner`，会话状态拦截归 `security`，点数赠送复用 `billing` 服务。前端新增单一列表页，
不引入新的页面框架。

## 复杂度跟踪

无宪法违规。新增内容仅为安全边界内的必要结构：独立管理员领域服务、会话状态过滤器和审计表。

## 阶段与产物

- 阶段 0：研究现有认证、账号、点数和前端模式，结果写入 `research.md`。
- 阶段 1：定义账号/审计数据、管理 API 契约和本地验证步骤。
- 阶段 2：生成按用户故事排序的任务，并在实现时逐项勾选和写入 Completion Log。

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

[Gates determined based on constitution file]

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file ($speckit-plan command output)
├── research.md          # Phase 0 output ($speckit-plan command)
├── data-model.md        # Phase 1 output ($speckit-plan command)
├── quickstart.md        # Phase 1 output ($speckit-plan command)
├── contracts/           # Phase 1 output ($speckit-plan command)
└── tasks.md             # Phase 2 output ($speckit-tasks command - NOT created by $speckit-plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
# [REMOVE IF UNUSED] Option 1: Single project (DEFAULT)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [REMOVE IF UNUSED] Option 2: Web application (when "frontend" + "backend" detected)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [REMOVE IF UNUSED] Option 3: Mobile + API (when "iOS/Android" detected)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure: feature modules, UI flows, platform tests]
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
