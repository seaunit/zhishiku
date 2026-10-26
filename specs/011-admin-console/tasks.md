# 任务：管理员账号管理台

**输入**：[spec.md](./spec.md)、[plan.md](./plan.md)、[research.md](./research.md)、
[data-model.md](./data-model.md)、[contracts/openapi.yaml](./contracts/openapi.yaml)

**测试**：规格明确要求权限、停用、赠送和移动端验收，因此以下测试任务必须在对应实现之前完成。

## Phase 1: 设置

- [x] T001 复核管理员规格、计划、数据模型和 API 契约，在 `specs/011-admin-console/` 中确认没有未决澄清
- [x] T002 [P] 按 Taste `framed-sheet` 方向创建管理员页原型 `prototype/admin.html` 和 `prototype/admin.css`

---

## Phase 2: 基础能力

**目的**：完成账号角色、状态、管理员初始化、会话失效和管理权限基础，任何用户故事都依赖此阶段。

- [x] T003 新增 Flyway 迁移 `backend/kb-app/src/main/resources/db/migration/V0019__admin_accounts.sql`，包含用户名、角色、账号状态和管理审计表
- [x] T004 扩展 `backend/kb-app/src/main/java/site/seaunit/zhishiku/owner/OwnerAccountEntity.java` 和 `OwnerAccountRepository.java`，实现用户名、角色、状态与登录标识解析
- [x] T005 [P] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/config/AppProperties.java` 与 `application*.yml` 增加 `SHIHAI_ADMIN_USERNAME`、`SHIHAI_ADMIN_PASSWORD` 配置，不添加任何真实密码
- [x] T006 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/owner/OwnerService.java` 实现管理员首次初始化、固定用户名和普通账号默认值
- [x] T007 [P] 新增 `backend/kb-app/src/main/java/site/seaunit/zhishiku/security/OwnerSessionStatusFilter.java`，让禁用账号已有会话在下一次请求失效
- [x] T008 新增 `backend/kb-app/src/main/java/site/seaunit/zhishiku/admin/AdminAuthorizationService.java`，仅允许启用、`ADMIN` 角色且用户名为 `a115846` 的账号
- [x] T009 [P] 新增 `backend/kb-app/src/test/java/site/seaunit/zhishiku/security/OwnerSessionStatusFilterTest.java`，覆盖启用、禁用、匿名和会话清理
- [x] T010 [P] 更新 `backend/kb-app/src/test/java/site/seaunit/zhishiku/auth/AuthControllerCaptchaTest.java`，覆盖管理员用户名登录和禁用账号拒绝

**检查点**：管理员身份可从数据库安全解析，普通账号状态默认正确，禁用会话可被拦截。

---

## Phase 3: 用户故事 1 - 查看账号和点数概况（优先级：P1）🎯 MVP

**目标**：管理员可搜索、分页查看账号状态、知识数和真实点数聚合。

**独立测试**：管理员取得列表和详情；普通账号、匿名用户和错误用户名均被拒绝；无点数账号返回真实 0。

### 测试

- [x] T011 [P] [US1] 新增 `backend/kb-app/src/test/java/site/seaunit/zhishiku/admin/AdminAccountControllerTest.java`，覆盖 `GET /admin/users` 和 `GET /admin/users/{id}` 权限、搜索、分页和错误
- [x] T012 [P] [US1] 新增 `backend/kb-app/src/test/java/site/seaunit/zhishiku/admin/AdminAccountServiceTest.java`，覆盖点数聚合、知识计数和零值语义

### 实现

- [x] T013 [US1] 新增 `backend/kb-app/src/main/java/site/seaunit/zhishiku/admin/AdminAccountDtos.java` 和 `AdminAccountException.java`
- [x] T014 [US1] 新增 `backend/kb-app/src/main/java/site/seaunit/zhishiku/admin/AdminAccountService.java`，实现管理员校验、搜索、分页和详情聚合
- [x] T015 [US1] 新增 `backend/kb-app/src/main/java/site/seaunit/zhishiku/admin/AdminAccountController.java`，提供契约中的只读管理接口
- [x] T016 [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/common/ApiExceptionHandler.java` 映射管理员权限、账号不存在和状态冲突错误
- [x] T017 [US1] 扩展 `frontend/kb-web/src/types/api.ts`、`src/api/client.ts`、`src/stores/session.ts` 支持管理员标识和账号查询
- [x] T018 [US1] 新增 `frontend/kb-web/src/views/AdminUsersView.vue`，按原型实现搜索、分页、账号表格、详情和状态
- [x] T019 [US1] 更新 `frontend/kb-web/src/router.ts` 和 `src/components/AppShell.vue`，仅对管理员显示并允许进入 `/admin/users`

**检查点**：管理员能在桌面和手机找到目标账号并读懂点数使用；普通账号无法读取任何管理数据。

---

## Phase 4: 用户故事 2 - 启用和禁用普通账号（优先级：P1）

**目标**：管理员可带原因启用/禁用普通账号，禁用立即影响新旧会话，管理员不可禁用。

**独立测试**：状态变更、幂等重试、旧会话失效、重新启用和自保护。

### 测试

- [x] T020 [P] [US2] 扩展 `AdminAccountControllerTest.java` 和 `AdminAccountServiceTest.java`，覆盖启用、禁用、管理员保护和幂等
- [x] T021 [P] [US2] 新增 `frontend/kb-web/e2e/admin-users.spec.ts`，覆盖管理员进入、普通账号拒绝、状态切换和确认

### 实现

- [x] T022 [US2] 新增 `backend/kb-app/src/main/java/site/seaunit/zhishiku/admin/AdminAuditRepository.java`，持久化状态与赠送安全审计
- [x] T023 [US2] 扩展 `AdminAccountService` 和 `AdminAccountController`，实现带 `Idempotency-Key` 的状态修改
- [x] T024 [US2] 在 `AdminUsersView.vue` 增加启用/禁用确认、必填原因、错误与成功状态

**检查点**：禁用账号无法登录且旧会话失效；重新启用后原数据完整；管理员账号不能被停用。

---

## Phase 5: 用户故事 3 - 赠送点数（优先级：P2）

**目标**：管理员可向普通账号赠送 1–100,000 点，账本、批次、审计和页面余额一致。

**独立测试**：成功赠送、非法点数、未知账号、管理员自赠、事务回滚和幂等重放。

### 测试

- [x] T025 [P] [US3] 扩展 `AdminAccountServiceTest.java`，覆盖赠送点数成功、边界、事务、幂等和审计
- [x] T026 [P] [US3] 扩展 `frontend/kb-web/e2e/admin-users.spec.ts`，覆盖赠送表单、验证、成功刷新和重试

### 实现

- [x] T027 [US3] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/billing/BillingService.java` 暴露管理员赠送事务入口，复用批次、账户和账本不变量
- [x] T028 [US3] 扩展 `AdminAccountService` 和 `AdminAccountController`，实现 `POST /admin/users/{id}/point-grants`
- [x] T029 [US3] 在 `AdminUsersView.vue` 增加赠送点数面板、原因输入、确认和余额刷新

**检查点**：赠送只会通过可审计账本到账，重复请求不重复加点，任何失败都不会留下部分数据。

---

## Phase 6: 用户故事 4 - 双语与移动端（优先级：P3）

**目标**：管理员页默认中文、支持英文，并在桌面、平板和手机完成核心流程。

**独立测试**：1440、768、390 宽度，中英文，键盘焦点，错误、加载和空状态。

- [x] T030 [P] [US4] 在 `frontend/kb-web/src/i18n.ts` 增加完整管理员中英文文案和路由标题
- [x] T031 [US4] 在 `frontend/kb-web/src/admin-users.css` 对齐管理员原型、响应式表格、状态和操作面板
- [x] T032 [US4] 增加 `AdminUsersView.vue` 的加载、空结果、错误、无权限和操作进行中状态，并完成键盘与无障碍标签

**检查点**：中英文事实一致，手机端无横向页面溢出，所有主要操作可用键盘和触控完成。

---

## Phase 7: 收尾

- [x] T033 [P] 更新 `AGENTS.md`，记录管理员固定身份、密码注入、账号停用和点数赠送约束
- [x] T034 运行后端单元、集成、契约与构建检查，并修复失败
- [x] T035 运行前端 ESLint、TypeScript、Vitest、Playwright 和构建，并修复失败
- [x] T036 使用 Taste lint/inspect 检查原型与前端 1440、768、390 视觉结果，修复全部 `FAIL`
- [x] T037 执行 `specs/011-admin-console/quickstart.md` 端到端验收并更新发布说明
- [x] T038 更新 `README.md` 或部署文档中的管理员初始化变量名和留空保留密码行为，不写入真实密码
- [x] T039 运行 `git diff --check`，核对任务清单、Completion Log、代码和文档一致

---

## 依赖与执行顺序

- 阶段 1 → 阶段 2 → 阶段 3。
- 用户故事 1 是 MVP；用户故事 2 依赖账号角色和状态基础，但不依赖列表之外的赠送。
- 用户故事 3 依赖用户故事 1 的目标账号查找和点数聚合，复用用户故事 2 的审计存储。
- 用户故事 4 可与用户故事 2/3 的文案同步，但必须在最终视觉验收前完成。
- 收尾依赖所有已选择用户故事完成。

## 并行机会

- T002 原型与 T003–T007 后端基础可以并行，但文件不同且视觉方向已确定。
- T009、T010、T011、T012 测试文件不同，可并行编写。
- T005、T009、T011、T012、T020/T021、T025/T026、T030 可并行。

## 实施策略

1. 先完成阶段 1–2，验证身份、迁移和会话失效。
2. 完成用户故事 1，取得只读管理 MVP。
3. 完成用户故事 2，再完成用户故事 3，每阶段独立验证。
4. 最后完成双语、移动端、视觉、全量回归和文档。

## Completion Log

> 每完成一个任务，必须在同一变更中勾选该任务并追加摘要、详情、验证证据和 Git 状态。

> 2026-10-26 完成本轮全部实现任务。当前变更已提交为 `4c12e19`。

| 任务 | 摘要与详情 | 验证证据 | Git |
|------|------------|----------|-----|
| T001 | 完成规格、计划、数据模型、契约与质量清单复核；没有未决澄清，范围限定为固定管理员、账号状态和点数赠送。 | `requirements.md` 全部通过；规格与任务范围一致。 | `4c12e19` |
| T002 | 按 Taste `framed-sheet`、seed `619440299` 创建独立管理员原型，采用深海底色、规则式账号表和安静数据面板。 | `lint.mjs` 0 FAIL；`inspect.mjs --app` 0 FAIL，仅保留导航和数据子行 11–12px 的允许 WARN；已查看 1440/768/390 截图。 | `4c12e19` |
| T003 | 新增 `V0019__admin_accounts.sql`：可选唯一用户名、`USER/ADMIN` 角色、启用状态索引、管理审计及唯一幂等键。 | 本地 PostgreSQL 18 Flyway 从历史版本成功迁移到 `0019`。 | `4c12e19` |
| T004 | 扩展账号实体和仓库，加入用户名、角色、状态、登录标识解析、管理员创建与状态更新。 | 后端编译、`OwnerServiceTest` 和实际管理员初始化通过。 | `4c12e19` |
| T005 | 新增 `SHIHAI_ADMIN_USERNAME`、`SHIHAI_ADMIN_PASSWORD` 配置；仓库没有真实管理员密码。 | 配置加载与本地启动通过；密钥模式扫描未发现给定生产密码。 | `4c12e19` |
| T006 | 在 `OwnerService` 首次启动时创建固定 `a115846` 管理员，数据库只保存 BCrypt 哈希；普通注册仍默认 `USER/enabled`。 | `OwnerServiceTest`；本地首次启动创建管理员并使用用户名登录成功。 | `4c12e19` |
| T007 | 新增 `OwnerSessionStatusFilter`，在受保护请求解析账号并在禁用时清理安全上下文和会话。 | 实际停用后旧会话请求 `/profile` 返回 401；专项单元测试通过。 | `4c12e19` |
| T008 | 新增固定管理员授权服务，同时校验启用状态、`ADMIN` 角色和用户名 `a115846`。 | 管理员测试通过；普通账号访问管理接口实际返回 403。 | `4c12e19` |
| T009 | 覆盖启用账号放行、禁用账号会话失效、匿名请求和上下文清理。 | `OwnerSessionStatusFilterTest` 通过。 | `4c12e19` |
| T010 | 登录兼容邮箱/管理员用户名；测试覆盖图形验证码后使用 `a115846` 登录及管理员标识响应。 | `AuthControllerCaptchaTest` 通过；本地真实登录成功。 | `4c12e19` |
| T011 | 新增管理 Controller 测试，覆盖匿名拒绝、列表、管理员字段、CSRF、幂等头和点数边界。 | `AdminAccountControllerTest` 通过。 | `4c12e19` |
| T012 | 新增服务层测试，覆盖账号保护、状态变化、点数聚合、赠送和幂等重放。 | `AdminAccountServiceTest` 通过；后补真实 SQL 集成验证。 | `4c12e19` |
| T013 | 新增安全 DTO 和领域异常，列表不返回密码、会话、知识正文或支付敏感字段。 | Java 编译及 MockMvc JSON 断言通过。 | `4c12e19` |
| T014 | 实现分页搜索、状态筛选、全局账号汇总、知识数量和点数聚合；无点数账号使用真实 0。 | 本地 PostgreSQL 实际返回 53 个账号、启用/停用计数和累计消耗；分页与搜索通过。 | `4c12e19` |
| T015 | 实现 `GET /admin/users`、单账号详情、状态修改和点数赠送路由。 | Controller 测试与本地 API 调用通过。 | `4c12e19` |
| T016 | 在统一异常处理映射管理员 403、账号不存在 404 和受保护账号/状态冲突 422。 | MockMvc 错误码断言通过。 | `4c12e19` |
| T017 | 扩展前端会话、API 类型和管理客户端；登录接受用户名并携带管理员标识。 | TypeScript、ESLint、Vitest 和构建通过。 | `4c12e19` |
| T018 | 新增管理员 Vue 页面，包含概况、搜索、状态筛选、分页、行操作和移动端布局。 | Playwright 管理流程与 320px 溢出测试通过；实际浏览器截图已检查。 | `4c12e19` |
| T019 | 增加管理员路由守卫和仅在管理员会话下显示的侧栏入口。 | 路由 E2E 通过；普通账号访问 API 返回 403。 | `4c12e19` |
| T020 | 覆盖启用/禁用、管理员保护和状态幂等；管理员账号不能由界面修改。 | 服务与 Controller 测试通过；真实停用、重启用和旧会话 401 验证通过。 | `4c12e19` |
| T021 | 新增管理员 UI E2E，覆盖进入、搜索、停用确认、启用状态与赠送。 | `admin-users.spec.ts` 单测与 5 并发重复均通过。 | `4c12e19` |
| T022 | 新增只追加管理审计表与仓库，保存管理员、目标、动作、脱敏原因、前后状态和幂等键。 | 本地 PostgreSQL 审计行数验证；赠送重放后审计仍为 1 行。 | `4c12e19` |
| T023 | 状态修改使用请求哈希、24 小时持久化幂等、实体版本和审计。 | 真实状态接口、幂等重放、管理员保护测试通过。 | `4c12e19` |
| T024 | 前端增加状态确认对话框、必填原因、处理中和失败提示，成功后刷新列表。 | Playwright 停用流程通过；截图检查确认和行状态。 | `4c12e19` |
| T025 | 覆盖赠送成功、管理员保护、非法范围、首次点数账户初始化、幂等和秘密脱敏。 | `AdminAccountServiceTest` 通过；真实赠送与重放验证通过。 | `4c12e19` |
| T026 | 赠送 E2E 覆盖表单、原因、请求头、成功反馈和刷新后的新余额。 | `admin-users.spec.ts` 通过。 | `4c12e19` |
| T027 | 赠送前自动确保目标点数账户存在，再复用 `manualAdjustment` 的批次、账户投影和账本事务。 | 首次赠送创建体验点与赠送点；账本 `manual_adjustment` 实际 1 行。 | `4c12e19` |
| T028 | 实现 `POST /admin/users/{id}/point-grants`，限制普通账号 1–100,000 点并要求幂等键。 | Controller/Service 测试与本地真实请求通过。 | `4c12e19` |
| T029 | Vue 页面增加点数赠送对话框、数字校验、原因、错误、成功 Toast 和列表刷新。 | Playwright 与真实浏览器检查通过。 | `4c12e19` |
| T030 | 增加完整管理员中英文文案、登录账号文案、菜单和路由标题。 | ESLint、TypeScript、Windows/路由测试与双语页面检查通过。 | `4c12e19` |
| T031 | 新增 `admin-users.css`，对齐已批准原型的信息密度、表格、状态、对话框和 320–1440 响应式布局。 | Taste lint 0 FAIL；Playwright 320px 无横向溢出。 | `4c12e19` |
| T032 | 页面覆盖加载骨架、空结果、错误、无权限守卫、处理中、成功提示、原生 dialog 和可访问标签。 | ESLint、TypeScript、Playwright 通过；键盘/触控控件高度检查通过。 | `4c12e19` |
| T033 | 同步 `AGENTS.md` 和宪法 4.5.0，记录固定管理员身份、密码注入、会话失效、账号保护和账本赠送边界。 | 文档复核与 `git diff --check` 通过。 | `4c12e19` |
| T034 | 执行后端全量单元、WebMvc、契约和构建门禁，并修复赠送首个点数账户与秘密脱敏问题。 | Maven 全量：339 tests，0 failure，0 error，0 skipped；构建通过。 | `4c12e19` |
| T035 | 执行前端 ESLint、TypeScript、Vitest、Playwright 和生产构建。 | Vitest 58 passed；Playwright 65 passed、6 个显式 live-only 测试跳过；Vite 构建通过。 | `4c12e19` |
| T036 | 对原型执行 Taste lint/inspect；最终 Vue 页面用真实管理员登录在 1440 与 390 截图复核，并补 320px 自动化检查。 | 原型 0 FAIL；实现无横向溢出；截图位于 `.taste-skill/admin-prototype/` 和 `.taste-skill/admin-live/`。 | `4c12e19` |
| T037 | 按 quickstart 完成真实本地后端口径验证：账号查询、停用、新登录拒绝、旧会话 401、重新启用、赠送和账本审计。 | 本地 PostgreSQL/Flyway、管理员页面、API 和账本联合验收通过。 | `4c12e19` |
| T038 | 更新 README、部署 README 和本地测试文档；部署脚本首次交互读取管理员密码，创建成功后从运行环境文件移除一次性明文。 | 两套 Shell 脚本通过远端 `bash -n`；本地占位配置启动通过。 | `4c12e19` |
| T039 | 完成工作区检查、空白检查、变更范围核对和任务/日志同步。 | `git diff --check` 通过；任务表无未完成项；未跟踪项为本次新增功能文件和用户原有 `deliverables/`。 | `4c12e19` |
