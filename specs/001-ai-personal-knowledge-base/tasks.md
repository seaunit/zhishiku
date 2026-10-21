# 任务：AI 个人知识库

**输入**：`specs/001-ai-personal-knowledge-base/`

**前置条件**：[spec.md](spec.md)、[plan.md](plan.md)、[research.md](research.md)、
[data-model.md](data-model.md)、[deployment.md](deployment.md)、
[contracts/openapi.yaml](contracts/openapi.yaml)

**测试**：测试先行。Java 使用 JUnit 5/Testcontainers/RestAssured，Vue 使用
Vitest/Vue Test Utils，端到端使用 Playwright 和 axe-core。

## 优先级

- **P0 MVP**：工程基础、安全、PostgreSQL、手动采集、单框搜索、分类和计费门禁。
- **P1 完整发布**：文档/截图/网页导入、互联网搜索、完整双语、部署和零影响。
- **P2 持续加固**：规模、可用性、恢复和长期维护。

## 阶段 0：架构 PoC 与技术门禁

- [ ] T001 [P0] [P] 在 `tests/poc/postgres-search/` 中验证 100,000 条中文/英文混合语料下 `pg_trgm` 与受控全文查询的延迟、召回和短查询回退
- [ ] T002 [P0] [P] 在 `tests/poc/playwright-java/` 中验证 Playwright Java 单页截图、Chromium 版本、内存峰值和失败关闭
- [ ] T003 [P0] [P] 在 `tests/poc/libvips/` 中验证 JPEG/PNG/WebP/HEIC 方向、缩放、EXIF/XMP 清理和资源限制
- [ ] T004 [P0] [P] 在 `tests/poc/waffo-bridge/` 中验证 Spring Boot 8082 与 Node 8091 的双向内部鉴权、通知转发和超时
- [ ] T005 [P0] 在 `artifacts/001/architecture-poc.md` 中记录四项 PoC 的命令、版本、结果和是否允许进入工程实现

## 阶段 1：工程与本地环境

- [x] T006 [P0] 在 `backend/pom.xml`、各模块 `pom.xml`、`mvnw`/`mvnw.cmd` 和 `.mvn/` 中创建 Java 17 + Spring Boot Maven Wrapper 多模块，并启用 Java 17 Enforcer
- [x] T007 [P0] [P] 在 `frontend/kb-web/package.json`、`vite.config.ts` 和 `pnpm-lock.yaml` 中创建 Vue 3 + TypeScript + Vite 工程
- [x] T008 [P0] [P] 在 `infra/compose.yml` 中配置 PostgreSQL 18、MinIO 和本地浏览器依赖；不引入 Redis 或 Meilisearch
- [x] T009 [P0] [P] 在 `.env.example`、`backend/kb-app/src/main/resources/application.yml` 和配置类中定义 Java、Spring、PostgreSQL、S3、DeepSeek、桥接和上传变量
- [ ] T010 [P0] [P] 在 `.github/workflows/kb-ci.yml` 和 `backend/kb-app/src/test/java/site/seaunit/zhishiku/migration/` 中先建立 Maven、pnpm、Flyway 全新安装/重复迁移测试骨架、契约、安全、Playwright 和零影响门禁
- [x] T011 [P0] 在 `backend/kb-app/src/main/resources/db/migration/V0001__foundation.sql` 中启用 `pg_trgm` 并建立基础 schema、任务和搜索投影迁移
- [ ] T012 [P0] [P] 在 `frontend/kb-web/src/main.ts`、`router/index.ts`、`stores/` 和 `locales/` 中建立 Vue Router、Pinia、TanStack Vue Query 和 vue-i18n
- [x] T013 [P0] [P] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/common/` 中建立与 OpenAPI 一致的 `ErrorResponse`、请求 ID、JSON 日志和异常映射

## 阶段 2：安全、认证与共享基础

- [ ] T014 [P0] [P] [US1] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/auth/` 中先验证 bootstrap、密码、会话、CSRF、Cookie 路径和撤销
- [ ] T015 [P0] [P] [US1] 在 `backend/kb-core/src/test/java/site/seaunit/zhishiku/domain/` 和 `backend/kb-app/src/test/java/site/seaunit/zhishiku/jobs/` 中为乐观锁、保留期、双 Worker `SKIP LOCKED`、租约过期、fencing token 和旧结果拒绝编写失败测试
- [x] T016 [P0] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/auth/` 中实现 Spring Security、BCrypt/Argon2、Session JDBC、一次性所有者初始化及 CSRF/login/logout/setup 端点
- [x] T081 [P0] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/auth/captcha/` 和登录页中实现 Kaptcha 图形验证码、Caffeine 服务端缓存、一次性登录校验与中文错误刷新
- [x] T017 [P0] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/common/persistence/` 中实现 BaseEntity、UUID、时间戳和 `@Version`
- [ ] T018 [P0] [P] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/jobs/` 中实现 PostgreSQL 任务表、租约、指数退避和 `SKIP LOCKED`
- [ ] T019 [P0] [P] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/storage/` 中实现 S3 暂存、内容哈希、MIME 检测、正式附件提升和对象清理
- [x] T020 [P0] [P] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/knowledge/KnowledgeService.java` 中实现 `search_document` 更新、修订和 PostgreSQL 查询
- [x] T021 [P0] [US1] 在 `frontend/kb-web/src/components/AppShell.vue`、`views/` 和 `stores/` 中建立受保护工作区、导航、语言切换和错误边界
- [ ] T022 [P0] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/health/` 中实现 Actuator 健康、数据库迁移、对象存储和任务 worker 状态，并加入 Springdoc 运行时契约与冻结 OpenAPI 差异检查

## 阶段 3：用户故事 1 — 手动保存与单框搜索

**目标**：保存手动知识并立即可搜索。

**独立测试**：创建知识后通过标题、正文、备注、分类和标签命中。

- [ ] T023 [P0] [P] [US1] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/knowledge/` 中为创建、编辑、版本冲突、列表 DTO 和即时搜索编写测试
- [ ] T024 [P0] [P] [US1] 在 `frontend/kb-web/src/features/knowledge/__tests__/` 中为表单、列表、详情、空状态和双语编写 Vitest
- [ ] T025 [P0] [US1] 在 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/knowledge/` 中创建知识、来源、分类和标签领域模型
- [x] T026 [P0] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/knowledge/KnowledgeService.java` 中实现 JPA 仓库、`JdbcClient` 搜索和事务服务
- [ ] T027 [P0] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/api/knowledge/` 中实现 CRUD、搜索、回收站和冲突 API
- [ ] T028 [P0] [US1] 在 `frontend/kb-web/src/features/knowledge/` 中实现首页单搜索框、结果摘要、匹配原因和分页
- [x] T029 [P0] [US1] 在 `frontend/kb-web/src/views/HomeView.vue` 和 `DetailView.vue` 中实现知识库和详情页面
- [x] T030 [P0] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/knowledge/KnowledgeService.java` 中实现中文/英文排序、短查询回退和高亮
- [ ] T031 [P0] [US1] 在 `tests/e2e/us1-knowledge.spec.ts` 中验证保存、刷新、搜索、详情、冲突和回收站

## 阶段 4：用户故事 2 — 自动分类、标签与授权

**目标**：AI 生成摘要、分类和标签，用户修正后锁定。

- [ ] T032 [P0] [P] [US2] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/classification/` 中为暂定、应用、锁定、过期修订和授权范围编写测试
- [ ] T033 [P0] [P] [US2] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/provider/` 中为 DeepSeek 超时、限流、错误 JSON 和结构化输出编写测试
- [ ] T034 [P0] [US2] 在 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/classification/` 中实现分类、标签、置信度和锁定状态机
- [ ] T035 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/auth/ai/` 中实现按服务和内容类型的隐私授权
- [ ] T036 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/provider/deepseek/` 中使用 `WebClient` 实现分类与结构化响应校验
- [ ] T037 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/jobs/classification/` 中实现租约、重试、过期修订和计费门禁
- [ ] T038 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/api/classification/` 中实现授权、分类修正、重新分析和状态 API
- [ ] T039 [P0] [US2] 在 `frontend/kb-web/src/features/authorization/` 和 `classification/` 中实现授权矩阵、暂定/锁定控件和状态
- [ ] T040 [P0] [US2] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/security/` 中添加提示注入、跨项目分类和撤销后外发拒绝测试
- [ ] T041 [P0] [US2] 在 `tests/e2e/us2-classification.spec.ts` 中验证授权、分类、锁定、撤销和重试

## 阶段 5：用户故事 3 — 文档、图片与截图采集

**目标**：文档、图片文件和剪贴板截图安全进入知识库。

- [ ] T042 [P1] [P] [US3] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/imports/` 中为组合校验、MIME、大小、扫描、OCR 和重试编写测试
- [ ] T043 [P1] [P] [US3] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/browser/` 中为 SSRF、DNS 重绑定、重定向、Cookie 和单页限制编写测试
- [ ] T044 [P1] [US3] 在 `backend/kb-core/src/main/java/site/seaunit/zhishiku/core/imports/` 中实现导入状态机和 `screenshot+clipboard`、`document+file_upload` 组合
- [ ] T045 [P1] [US3] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/imports/` 中实现暂存、Tika/PDFBox 提取和有界 OCR
- [ ] T046 [P1] [P] [US3] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/images/` 中实现 libvips 调用、HEIC、方向、缩放和元数据清理
- [ ] T047 [P1] [P] [US3] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/security/` 中实现文本和图片密钥扫描、边界框脱敏及失败关闭
- [ ] T048 [P1] [US3] 在 `backend/kb-browser-worker/src/main/java/` 中实现 Playwright Java 单 URL 截图、代理、DNS/IP 校验和资源限制
- [ ] T049 [P1] [US3] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/api/imports/` 中实现文本、上传、网页和任务 API
- [ ] T050 [P1] [US3] 在 `frontend/kb-web/src/features/capture/` 中实现文档/图片入口、仅剪贴板截图入口、状态和回退
- [ ] T051 [P1] [US3] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/contract/imports/` 中使用 RestAssured/OpenAPI Validator 验证上传组合、CSRF、415、413、422 和任务契约
- [ ] T052 [P1] [US3] 在 `tests/e2e/us3-imports.spec.ts` 中验证文档、JPG/PNG/WebP/HEIC、剪贴板截图、拒绝和重试

## 阶段 6：用户故事 4 — 互联网搜索与候选保存

- [ ] T053 [P1] [P] [US4] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/search/external/` 中验证显式同意、计费、候选 TTL 和保存幂等
- [ ] T054 [P1] [US4] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/provider/deepseek/search/` 中实现 Search/Fetch Provider
- [ ] T055 [P1] [US4] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/search/external/` 中实现短期候选、过期清理和保存事务
- [ ] T056 [P1] [US4] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/api/search/` 中实现外部搜索和保存候选 API
- [ ] T057 [P1] [US4] 在 `frontend/kb-web/src/features/search/` 中实现个人优先、外部同意和候选项
- [ ] T058 [P1] [US4] 在 `tests/e2e/us4-search.spec.ts` 中验证本地优先、显式外发和保存幂等

## 阶段 7：用户故事 5 — 生命周期、双语与安全体验

- [ ] T059 [P0] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/security/` 中添加跨数据库、日志、DOM、对象存储和截图的金丝雀测试
- [ ] T060 [P0] [P] [US5] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/lifecycle/` 中验证回收站、恢复、purge、任务取消和对象删除
- [ ] T061 [P1] [US5] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/lifecycle/` 中实现生命周期、搜索投影和对象清理
- [ ] T062 [P1] [P] [US5] 在 `frontend/kb-web/src/locales/` 中补齐全部中文和英文文案
- [ ] T063 [P1] [P] [US5] 在 `tests/e2e/us5-lifecycle.spec.ts` 中验证删除、恢复、purge、双语和错误状态
- [ ] T064 [P0] [P] [US5] 在 `tests/accessibility/us5.spec.ts` 中验证键盘、焦点、标签、对话框和 200% 字体

## 阶段 8：搜索性能、部署与发布

- [ ] T065 [P0] [P] 在 `tests/performance/search-benchmark/` 中生成冻结语料、500 条查询和正确性清单
- [ ] T066 [P0] 在 `artifacts/001/search-benchmark.md` 中记录 PostgreSQL 搜索结果并决定是否需要 Meilisearch
- [x] T067 [P1] [P] 在 `deploy/scripts/preflight-existing-app.sh` 中检查端口、内存、磁盘、数据库、Nginx 和现有应用基线
- [ ] T068 [P1] [P] 在 `deploy/systemd/zhishiku-api.service`、`worker` 和 `browser-worker` 中创建独立 unit
- [ ] T069 [P1] [P] 在 `deploy/nginx/kb.conf` 和 `backend/kb-app/src/test/java/site/seaunit/zhishiku/deployment/nginx/` 中实现 Vue 构建产物、SPA fallback、`/kb/api`、Cookie 路径、CSRF 和增量 `/` 门户的 staging 冒烟
- [x] T070 [P1] 在 `deploy/scripts/provision-ubuntu.sh` 和 `deploy/scripts/deploy.sh` 中实现固定 ref 拉取、构建、Flyway、健康检查、备份和回滚
- [ ] T071 [P1] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/deployment/` 中验证迁移失败回滚、配置缺失和健康失败关闭
- [ ] T072 [P1] 在 `tests/e2e/zero-impact.spec.ts` 中验证 `/`、`/app`、`/api`、`/bank`、`/pay`、`/apk`、证书、Cookie 和支付 sidecar 零回归
- [ ] T073 [P1] [P] 在 `tests/e2e/taste.spec.ts` 中执行 1440/768/430/390/360/320、手机横屏、安全区和 200% 字体 Taste 检查
- [ ] T074 [P1] 在 `artifacts/001/acceptance.md` 中映射全部 FR/SC、测试证据和目标提交 SHA
- [x] T075 [P1] 在 `README.md`、`AGENTS.md` 和 `specs/001-ai-personal-knowledge-base/quickstart.md` 中同步实际命令、端口、环境变量和限制

## 阶段 9：P2 加固

- [ ] T076 [P2] 在 `tests/integration/restore-drill/` 中执行 PostgreSQL 与对象存储恢复
- [ ] T077 [P2] 在 `tests/integration/restart-recovery/` 中验证 API、Worker、browser worker 和 bridge 重启恢复
- [ ] T078 [P2] 在 `tests/performance/sustained/` 中执行持续搜索、导入和截图负载
- [ ] T079 [P2] 在 `tests/usability/us1-knowledge.md` 中执行 10 人可用性研究
- [ ] T080 [P2] 在 `artifacts/001/maintenance.md` 中记录 schema 演进、依赖升级、浏览器版本和回滚演练

## 依赖关系

- T001-T005 是 Java/Vue 实现前的硬门禁。
- 阶段 1-2 阻塞所有用户故事。
- US1 完成持久化、搜索和认证后，US2 与 US3 可并行。
- US4 依赖 US1 搜索和 002 计费任务。
- 部署和零影响必须在公开 `/kb` 前完成。
- Vue、Spring、PostgreSQL、browser worker 和 Node bridge 分别测试，最终以 Playwright E2E 串联。

## 实施策略

### P0 MVP

1. 完成四项 PoC、Maven/Vue 工程、Spring Security、Flyway 和 PostgreSQL 任务。
2. 完成手动采集、单框搜索、自动分类门禁和基础 UI。
3. 通过安全、搜索基准和本地端到端检查。

### P1 完整发布

1. 完成文档/图片/截图、网页、外部搜索和生命周期。
2. 完成完整双语、Taste、部署、备份和零影响门禁。

### P2 持续加固

1. 恢复、持续负载、可用性和长期维护演练。

## 完成记录格式

每个任务完成后必须勾选并记录任务 ID、日期、执行角色、提交 SHA、摘要、详情、验证命令、
证据路径、FR/SC 映射、风险和回滚。

## Completion Log

### 完成记录：T067

- 日期：2026-10-21
- 状态：已完成
- 执行子智能体：开发
- Git commit：`ba221ae`
- 摘要：新增现有应用部署前预检脚本。
- 详情：检查内存、磁盘、`8082` 端口、PostgreSQL `5432` 占用、Nginx 配置以及
  `shuati.service`、`shuati-payments.service` 和 Nginx 活跃状态；`8082` 被占用时阻断部署。
- 验证：Git Bash `bash -n` 通过；脚本通过 `--help` 入口解析；生产首次部署前已执行预检，
  原应用、Nginx、`8082` 和 PostgreSQL 状态均符合预期。
- 证据：`deploy/scripts/preflight-existing-app.sh`。
- 覆盖：T067；支持部署约束中的端口隔离、Nginx 共存和现有应用零影响预检。
- 风险与回滚：脚本只读；任何非预期服务状态仅告警，不修改现有 unit。

### 完成记录：T070

- 日期：2026-10-21
- 状态：已完成
- 执行子智能体：开发
- Git commit：`ba221ae`
- 摘要：新增从 GitHub 固定 ref 拉取的一键部署链路。
- 详情：`provision-ubuntu.sh` 安装 Java 17、Node.js、pnpm、PostgreSQL 18，创建独立用户、
  数据库和受限环境文件；`deploy.sh` 从 GitHub fetch 指定分支、tag 或 commit，执行测试与构建，
  生成不可变 release，切换 `current`，安装 systemd 和 Nginx 片段，执行本地健康检查；
  健康或 Nginx 校验失败时回滚上一 release 和配置备份。每次部署都通过 `/dev/tty`
  隐藏提示 AI API Key 和 SMTP 密码；直接回车保留已有值，输入新值才更新。首次安装和
  其内部部署共享同一次提示状态，不会重复询问两轮。
- 验证：`bash -n`、脚本帮助入口、后端 6 项测试、Spotless、前端 lint/typecheck、9 项 Vitest、
  Vite 生产构建和真实全栈 Playwright 通过。
- 证据：`deploy/scripts/provision-ubuntu.sh`、`deploy/scripts/deploy.sh`、
  `deploy/systemd/zhishiku-api.service`、`deploy/nginx/zhishiku-kb.conf`、`deploy/README.md`。
- 覆盖：T070；部署固定 ref、构建、Flyway、健康检查、备份和回滚。
- 验证补充：2026-10-21 使用 GitHub 固定提交 `3bda0d82a47d` 完成生产首次部署；
  `/kb/`、`/kb/login`、`/kb/api/actuator/health`、`/app` 和 `/` 均返回 HTTP 200，
  登录接口返回 HTTP 200，`zhishiku-api`、Nginx、`shuati.service` 和
  `shuati-payments.service` 均为 active。
- 风险与回滚：脚本失败时切回上一 release 并恢复 Nginx 备份，数据库迁移保持向前兼容，
  不删除 PostgreSQL 数据或上传目录。首次部署生成了独立 2 GiB swap 和受限制的初始凭据文件。

### 完成记录：T075

- 日期：2026-10-21
- 状态：已完成
- 执行子智能体：开发
- Git commit：`3bda0d82a47d`
- 摘要：同步当前实现、部署命令、端口、环境变量和范围限制。
- 详情：README 记录 MVP 能力与 GitHub 一键部署；AGENTS 标记应用已初始化；quickstart
  删除“尚未实现应用代码”的过期描述，改为当前端口 `4173`、Actuator 路径、本地 PostgreSQL
  和本地附件配置，并明确 AI、S3、OCR、Waffo 等尚未实现能力。
- 验证：交叉检查 README、AGENTS、部署设计和实际运行入口；本地与生产健康端点均为 HTTP 200。
- 证据：`README.md`、`AGENTS.md`、`specs/001-ai-personal-knowledge-base/quickstart.md`、
  `artifacts/001/deployment-verification.md`。
- 覆盖：T075；命令、端口、环境变量和能力边界。
- 风险与回滚：文档同步不改变运行行为；如实现范围变化，必须同时更新 README、AGENTS、
  quickstart 和任务完成记录。

### 完成记录：T081

- 日期：2026-10-21
- 状态：已完成
- 执行子智能体：开发
- Git commit：`未提交`
- 摘要：登录新增服务端缓存的一次性 Kaptcha 图形验证码。
- 详情：验证码使用 Kaptcha 生成 5 位 PNG，答案写入有界 Caffeine 缓存；默认 120 秒过期，
  每次消费后立即删除，忽略大小写。登录必须先校验验证码，失败返回 422 `CAPTCHA_INVALID`，
  前端自动换图并保留中文错误提示。生产不返回答案，仅 local profile 可返回调试答案。
- 验证：验证码服务 4 项测试、控制器 2 项测试、Kaptcha 渲染 1 项测试；前端 API 5 项测试；
  mock 登录 E2E、错误刷新 E2E、真实全栈验证码登录 E2E；Taste app-screen lint 0 FAIL/0 WARN，
  inspector 0 FAIL/1 个可解释的登录品牌面板空带警告。
- 证据：`CaptchaServiceTest`、`AuthControllerCaptchaTest`、`KaptchaCaptchaRendererTest`、
  `frontend/kb-web/e2e/live.spec.ts`、`.taste-skill/captcha-after/`。
- 覆盖：FR-002、FR-068，登录访问控制与一次性验证码。
- 风险与回滚：验证码缓存为单实例内存数据，应用重启后全部失效并自动重新获取；多实例部署前需改为
  共享缓存或有粘性会话。回滚时恢复登录请求字段和页面字段，不涉及数据库迁移。
