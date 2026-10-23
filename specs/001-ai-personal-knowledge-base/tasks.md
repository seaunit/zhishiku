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

- [x] T014 [P0] [P] [US1] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/auth/` 中先验证多账号公开注册、邮箱唯一、登录图形验证码、注册邮箱验证码、频控、临时邮箱拒绝、跨账号隔离、密码、会话、CSRF、Cookie 路径和撤销
- [ ] T015 [P0] [P] [US1] 在 `backend/kb-core/src/test/java/site/seaunit/zhishiku/domain/` 和 `backend/kb-app/src/test/java/site/seaunit/zhishiku/jobs/` 中为乐观锁、保留期、双 Worker `SKIP LOCKED`、租约过期、fencing token 和旧结果拒绝编写失败测试
- [x] T016 [P0] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/auth/` 中实现 Spring Security、BCrypt/Argon2、Session JDBC、多账号公开注册、按账号隔离及 CSRF/login/logout/register 端点；兼容字段 `owner_id` 的语义为账号 ID
- [x] T081 [P0] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/auth/captcha/` 和登录页中实现 Kaptcha 图形验证码、Caffeine 服务端缓存、一次性登录校验与中文错误刷新
- [x] T082 [P0] [US1] 在注册 API、SMTP 和注册页中实现多账号公开邮箱验证注册、邮箱唯一、确认密码、6 位邮箱验证码、频控、一次性使用与临时邮箱域名拒绝
- [x] T083 [P0] [US1] 在登录页始终显示注册入口，注册页始终允许新邮箱创建账号；已有账号不得关闭其他邮箱的注册，状态接口失败也不得隐藏入口
- [x] T084 [P0] [US1] 更新项目宪法、协作约定、README 和 001/002 SDD 文档，将识海从单所有者模型改为多账号公开注册、按账号隔离知识、附件、搜索、会话、点数、订单和账本；保留 `owner_id` 兼容字段但语义为账号 ID，不扩展团队共享
- [x] T085 [P0] [US1] 在数据库迁移、账号服务、注册 API、登录响应和前端注册页中落实多账号模型；移除实例唯一键，允许已初始化实例继续注册新邮箱，并验证两个账号之间的知识、搜索和详情访问隔离
- [x] T086 [P0] [US1] 移除注册图形验证码和发码接口的验证码字段，只保留邮箱验证码；注册页同步删除图形验证码控件，并在服务端强制同一邮箱发码冷却不得短于 60 秒
- [x] T087 [P0] [US1] 将注册、登录和初始化账号的密码最小长度统一为 8 位，最大长度保持 256 位，并同步前端校验、提示、OpenAPI、SDD 文档和边界测试
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
- [x] T088 [P1] [US3] 在采集页恢复手动录入、网页链接、文档和截图四个入口；网页链接改为单页截图 OCR 和 AI 拆分入库，TXT/Markdown 与图片文件入库，剪贴板截图后续由 008 改为 OCR 拆分；PDF/Word 解析后续由 007 接入
- [x] T089 [P0] [US4] 在首页恢复“继续搜索互联网”，接入 DeepSeek Search/Fetch Provider、显式同意、短期候选、来源打开与收入知识库，并保持本地结果优先
- [x] T090 [P0] [US1] 统一首页、知识库、待处理和详情页的分类占位与处理状态映射；无分类显示“未分类”，`not_requested` 显示“未整理”，未知状态不再默认显示“已整理”
- [x] T091 [P0] [US4] 保持首页“继续搜索互联网”入口初始可见；未完成本地检索时禁用并提示，本地检索完成后启用，点击后才发送查询。该版本的二次点击流程已由 T104 取代
- [x] T092 [P0] [US3] [US4] 统一采集边界：手动录入唯一可直存，AI 文字、网页、文档、文档图片、截图和互联网候选全部在 AI 整理成功后入库；同步宪法、AGENTS、SDD、OpenAPI、前端、原型和测试

## 阶段 6：用户故事 4 — 互联网搜索与候选保存

- [ ] T053 [P1] [P] [US4] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/search/external/` 中验证单击 1 点同意、自动报价确认、搜索扣点、候选 TTL 和保存幂等
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
- [x] T069 [P1] [P] 在 `deploy/nginx/kb.conf` 和 `backend/kb-app/src/test/java/site/seaunit/zhishiku/deployment/nginx/` 中实现 Vue 构建产物、SPA fallback、`/kb/api`、Cookie 路径、CSRF 和增量 `/` 门户的 staging 冒烟
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

## 阶段 10：2026-10-22 需求缺口收口

- [x] T093 [P0] 在 `V0010__knowledge_classification.sql`、分类服务、知识 API 和详情页中实现
  AI 摘要、结构化分类/标签、用户纠正锁定、暂定与应用状态，并把分类标签纳入单框搜索和匹配原因。
- [x] T094 [P0] 在知识控制器、`TrashView.vue` 和详情页中接通移入回收站、回收站列表、恢复、
  永久清除、附件清理和移动端确认流程。
- [x] T095 [P0] 在 `V0012__ai_processing_consent.sql`、授权 API、设置页和采集页中实现按账号、
  服务和内容类型持久化 AI 外发授权，撤销后阻止后续外发。
- [x] T096 [P0] 在 `V0013__api_idempotency.sql`、幂等服务和 AI 采集入口中实现请求哈希、
  同键同体响应重放、同键异体冲突和前端稳定幂等键。
- [x] T097 [P0] 在重新分析服务、计费报价和详情页中实现“报价→确认→重新分析→更新摘要/分类/标签”
  的付费闭环，并保持用户锁定字段不被覆盖。
- [x] T098 [P0] 在计费服务中强制服务端操作到计费项映射、预检账号/环境/类型/有效期校验、
  唯一图片数量校验、有效签名和商品 ID 匹配，并移除失败释放的事务锁等待路径。
- [x] T099 [P0] 修正退款实际回收结论、管理员目标账号与点数投影、财务日期过滤、异常对账状态、
  过期收银台以及桥接买家标识/成功地址/未知订单语义。
- [x] T100 [P1] 在部署脚本中增加 PostgreSQL 预部署备份、桥接 unit/Nginx snippet 回滚、
  计费显式开关、管理员邮箱和邮件退款入口；桥接 systemd 权限收敛到只读运行目录。
- [x] T101 [P1] 同步原型计费入口、七项移动导航、44px 触控、来源 URL、中文/英文页面标题与 ARIA，
  并更新 OpenAPI、环境变量示例和部署说明。
- [ ] T102 [P0] 在真实 Docker/PostgreSQL、真实 Waffo 测试商户、真实 DeepSeek/Tesseract 和生产
  `seaunit.site` 上执行迁移、支付、AI、回滚与零影响验收。当前本机 Docker 引擎不可用，生产验收未执行。
- [x] T103 [P0] 在知识库列表页实现服务端检索、命中字段、单条移入回收站、三态全选、原子批量回收、
  持久化幂等重试、失败保留和选择清理；同步 OpenAPI、数据模型、原型、中英文文案及桌面/平板/手机
  E2E，并验证 320–430px 触控与溢出。
- [x] T104 [P0] 将纯互联网搜索统一为固定 1 点、入口点击一次即自动创建并确认报价后直接搜索，
  取消搜索本身的二次确认；同步 001 规格、计划、数据模型、OpenAPI、快速验证、检查清单、原型说明，
  并在 002 同步价格、报价事务和验收任务。本文档任务只记录 SDD 同步完成，代码、迁移和自动化测试
  仍由后续开发/测试任务执行。
- [x] T109 [P0] 将 AI 整理、AI 采集和重新分析结果统一置为 `review`，新增账号隔离且受 CSRF
  保护的确认复核接口；确认后转为 `succeeded`。手动录入保持 `not_requested` 且不进入待复核
  队列。同步待处理页、详情页、中英文文案、交互原型、OpenAPI、数据模型和自动化验收。
- [x] T110 [P0] 在应用侧栏“待处理”入口显示当前账号的服务端待处理总数，使用与待处理页一致的
  状态集合；在初始登录、路由切换、确认复核和重新分析后刷新，并与原型徽标位置及无条目隐藏规则
  对齐。
- [x] T111 [P0] 将待复核状态从全局棕黄色警告样式改为深海蓝主题内的紧凑状态标记，避免表格网格
  拉伸成整列色块；同步原型、前端主题变量、桌面和移动端对齐规则。
- [x] T112 [P0] 加宽知识详情页正文和说明栏，桌面整页上限提升到 1680px，正文阅读上限 1080px，
  说明栏 320–380px；宽屏优先使用可用空间，移动端保持单列并验证无横向溢出。
- [x] T113 [P0] 调整知识详情页顶部操作：删除按钮固定单行，待复核时把复核按钮放到编辑按钮
  右侧，并将窄屏顶部栏改为返回链接与操作按钮上下两行，保持 44px 触控尺寸和按钮顺序。
- [x] T114 [P0] 将 AI 整理结果映射为受控 Markdown 富文本，并支持网页、文档、DOCX/PDF 和截图
  安全图示候选进入正文；限制单次 6 张，图片失败不影响文字知识，同步采集说明、原型、SDD 和测试。
- [x] T115 [P0] 修复本地采集依赖与文档计费：补齐 Tesseract `chi_sim+eng`、libvips 路径配置，
  文档提取报价按预检字符数每 10,000 字符计算单位，并完成网页与 DOCX 真实全链路验收。
- [x] T116 [P0] [US4] 将互联网候选“AI 整理后收入”接入单页截图、OCR、安全图示和富文本映射，
  支持 Markdown 表格，按网页视觉 10 点加分类 5 点计费，并在视觉失败时只结算分类。
- [x] T117 [P0] [US3] [US4] 网页链接和互联网候选改为从页面 DOM 提取实际内容图片、SVG 和 Canvas，
  排除导航、页头、页脚、侧栏、广告位、小图标和全屏背景；整页截图只用于 OCR，不作为来源图。

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

### 完成记录：T069

- 日期：2026-10-22
- 状态：已完成
- 执行子智能体：开发、测试
- Git tag：`v0.1.11-mvp`
- 部署提交：`8ba235907f7382a3b286cf34b1dd8a61fd3d5694`
- 摘要：根路径改为 SEAUNIT 双应用门户，拾题与识海共用同一域名并提供独立入口。
- 详情：门户安装到 `/opt/seaunit-portal/releases/`，`current` 指向当前 release；Nginx 只替换
  精确的 `location = /`，新增独立的 `/seaunit-portal/` 样式、脚本和字体路径，再保留现有
  `/kb` include。拾题的 `/app`、`/login`、`/bank/1`、`/api`、`/pay`、`/apk` 和支付侧车
  未改动。
- 验证：Taste lint 为 0 FAIL；生产 `https://seaunit.site/` 在 1440、768、390 像素真实浏览器
  检查为 0 FAIL；`/`、`/app`、`/login`、`/bank/1`、`/kb/`、`/terms`、`/privacy` 和
  `/kb/api/actuator/health` 均返回 HTTP 200；Nginx、`zhishiku-api`、`shuati` 和
  `shuati-payments` 均为 active。
- 证据：`deploy/portal/`、`deploy/scripts/deploy.sh`、`deploy/README.md`、
  `artifacts/001/deployment-verification.md`。
- 风险与回滚：门户资源与拾题资源使用不同 URL 前缀；Nginx 校验失败时脚本恢复配置与上一
  release。门户是静态文件，不接触拾题数据库、Cookie 或支付数据。

### 完成记录：T081

- 日期：2026-10-21
- 状态：已完成
- 执行子智能体：开发
- Git commit：`ae943a9`
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

### 完成记录：T082

- 日期：2026-10-21
- 状态：已完成
- 执行子智能体：开发
- Git commit：`06d0961`
- 邮件样式补充：`61ae299`
- 摘要：新增未初始化实例的单所有者邮箱验证注册。
- 详情：注册页包含邮箱、图形验证码、邮箱验证码、密码和确认密码；发送邮箱验证码前必须消费
  Kaptcha 图形验证码。邮箱验证码为 6 位安全随机数字，默认 10 分钟过期、哈希保存、最多尝试
  5 次，重发立即替换旧码。同一邮箱 60 秒冷却、每小时 5 次，同一 IP 每小时 20 次；SMTP 失败
  不留下可用验证码。已有所有者或并发创建时统一返回 `REGISTRATION_CLOSED`。验证码邮件使用
  识海蓝色系统风格 HTML，附带纯文本回退、醒目的 6 位验证码、10 分钟有效期和系统邮件页脚。
- 临时邮箱：仓库内置高置信度域名清单，按规范化域名的精确值或子域边界拒绝；
  `daniel.parkerca61598b@mailinator.com` 必须拒绝，`notmailinator.com` 和普通邮箱中的类似
  局部名称允许。
- 验证：邮箱策略 4 项、验证码服务 8 项、注册服务 4 项、邮件模板 2 项、SMTP MIME 1 项、
  注册控制器 6 项、
  登录/注册前端 API 8 项；注册页 mock E2E、临时邮箱拒绝 E2E、真实空库注册 E2E；
  后端全量 34 项、Vitest 14 项、Playwright 9 项通过；Taste app-screen 0 FAIL/0 WARN 和
  inspector 0 FAIL/1 个可解释的品牌面板空带警告。独立测试子智能体验收 0 功能失败。
- 证据：`EmailAddressPolicyTest`、`EmailVerificationServiceTest`、
  `RegistrationServiceTest`、`RegistrationEmailTemplateTest`、`SmtpRegistrationEmailSenderTest`、
  `AuthControllerRegistrationTest`、`frontend/kb-web/e2e/registration-live.spec.ts`、
  `.taste-skill/registration-after/`、`.taste-skill/registration-email/`。
- 覆盖：FR-002、FR-069 至 FR-073；单所有者边界、SMTP、邮箱验证码与临时邮箱过滤。
- 风险与回滚：邮箱验证码与频控缓存为单实例内存数据，重启后失效；多实例前需共享缓存。
  注册只创建唯一所有者，不扩展多用户。回滚时关闭注册入口并恢复登录页，不影响现有 owner 数据。
- 治理修订（2026-10-22）：本记录描述旧版单所有者实现，已被 T084 的多账号隔离规格取代；
  其中的注册关闭和唯一所有者结论不得作为当前验收依据。
- 注册安全修订（2026-10-22）：注册图形验证码已由 T086 移除；当前只要求邮箱验证码，登录仍
  保留图形验证码。

### 完成记录：T083

- 日期：2026-10-22
- 状态：已完成
- 执行子智能体：开发、测试
- Git commit：`dbf5c7f`
- 摘要：修复登录页在注册状态关闭或状态接口失败时不显示注册入口的问题。
- 详情：移除登录页对注册状态的前置隐藏逻辑，始终显示“注册账户”链接；点击后进入 `/kb/register`，
  注册页继续通过服务端状态展示注册表单或“注册已关闭”。单所有者边界和注册关闭规则不变。
- 验证：先新增 Playwright 失败测试并复现入口缺失；实现后目标测试通过。前端 lint、typecheck、
  Vitest 14 项、Playwright `app.spec.ts` 10 项和生产构建全部通过；Taste app-screen 在 1440、
  768、390 像素检查为 0 FAIL/0 WARN，登录页面截图已人工复核。
- 证据：`frontend/kb-web/src/views/LoginView.vue`、`frontend/kb-web/src/i18n.ts`、
  `frontend/kb-web/e2e/app.spec.ts`、`.taste-skill/registration-entry-after/`。
- 覆盖：FR-002、US1；注册入口可发现性、关闭状态说明和单所有者约束。
- 风险与回滚：已有所有者的实例仍不能创建第二个账户；注册页只显示关闭说明。回滚时恢复登录页
  条件入口，不影响既有账户、会话或注册数据。
- 治理修订（2026-10-22）：本记录中的注册关闭规则已被 T084 废止；当前注册页必须允许新邮箱
  创建独立账号。

### 完成记录：T084

- 日期：2026-10-22
- 状态：已完成（仅文档）
- 执行子智能体：产品、开发
- Git commit：`9743b4c`
- 摘要：将项目治理和 001/002 SDD 文档从单所有者模型改为多账号隔离模型。
- 详情：宪法升级为 2.0.0，新增公开注册、邮箱验证码、图形验证码、频控、临时邮箱拒绝、
  邮箱全局唯一和按账号隔离要求；统一 `owner_id` 为账号 ID 的兼容字段语义；明确每个账号
  独立拥有知识、附件、上传、搜索、会话、授权、点数、订单、退款和账本；保留禁止团队共享、
  跨账号读取和转赠的边界。同步更新 AGENTS、README、001 的规格/计划/研究/数据模型/
  快速验证及 002 的规格/研究/数据模型。
- 验证：使用关键词检索复核单所有者、唯一所有者、注册关闭、团队共享和 `owner_id` 语义；
  `git diff --check` 检查文档空白错误；核对宪法版本为 `2.0.0`、最后修订日期为
  `2026-10-22`。
- 证据：`.specify/memory/constitution.md`、`AGENTS.md`、`README.md`、`specs/001-ai-personal-knowledge-base/`
  中受影响的 SDD 文档、`specs/002-ai-credit-billing/spec.md`、
  `specs/002-ai-credit-billing/research.md`、`specs/002-ai-credit-billing/data-model.md`。
- 覆盖：FR-001、FR-002、FR-069 至 FR-073、FR-077、SC-018、SC-024；多账号注册与隔离边界。
- 风险与回滚：本任务只修改文档，不修改代码、前端、测试或部署；对应运行实现由 T085 完成。
  回滚时恢复旧文档版本会重新引入不兼容的单所有者治理，必须先获得产品批准。

### 完成记录：T085

- 日期：2026-10-22
- 状态：已完成
- 执行子智能体：开发、测试
- Git commit：`9743b4c`
- 摘要：将运行代码和前端从实例单所有者改为多账号公开注册和按账号隔离。
- 详情：新增 V0006 迁移，移除 `owner_account.instance_key` 唯一约束和列，保留邮箱唯一约束并
  回填 `email_verified_at`；注册状态由 `SHIHAI_REGISTRATION_ENABLED` 控制，默认开启，已有账号不再
  关闭其他邮箱注册。登录和 `/me` 从当前认证邮箱解析账号；重复邮箱返回
  `EMAIL_ALREADY_REGISTERED`。前端注册页不再依赖注册关闭状态，始终显示账户创建表单。
- 验证：独立 PostgreSQL 18.1 全新数据库成功执行 V0001→V0004→V0005→V0006；通过两个浏览器
  上下文注册两个账号，账号 A 创建知识后，账号 B 列表为 0、详情访问返回 400、搜索命中为 0；
  `registration-status` 返回 `open=true`。后端 111 项与前端 41 项测试通过，注册相关
  Playwright 3 项和真实多账号隔离 Playwright 1 项通过。
- 证据：`backend/kb-app/src/main/resources/db/migration/V0006__multi_account.sql`、
  `backend/kb-app/src/test/java/site/seaunit/zhishiku/owner/OwnerServiceTest.java`、
  `frontend/kb-web/e2e/app.spec.ts`、`tests/e2e/multi-account-isolation.spec.ts`、
  `artifacts/001/multi-account-verification.md`、`artifacts/001/deployment-verification.md`。
- 生产部署：`v0.1.14-mvp`（release 提交 `83d2a824cb1a5c68060850276c1095baa8476cde`）已从
  GitHub 部署到服务器；生产迁移为 V0006，注册状态为 `open=true`，现有账号保留，原拾题应用、
  支付侧车和双应用入口健康检查通过。
- 覆盖：FR-001、FR-002、FR-069 至 FR-073、FR-077、SC-018、SC-024。
- 风险与回滚：V0006 为向前迁移；回滚应用版本不会恢复实例唯一键，旧版本不能安全写入多账号
  数据库。团队共享、成员邀请、跨账号转赠和公开分享仍不在范围内。

### 完成记录：T086

- 日期：2026-10-22
- 状态：已完成
- 执行子智能体：产品、开发、测试
- Git commit：`b7177aa`
- 摘要：注册改为只输入邮箱验证码，不再要求图形验证码，并强制 60 秒发码冷却。
- 详情：注册发码请求体由邮箱、图形验证码 ID 和答案改为只包含邮箱；注册页删除图形验证码
  输入框、图片和加载逻辑；登录图形验证码保持不变。服务端 `EmailVerificationService.Config`
  对发码冷却执行 60 秒硬下限，环境变量即使配置更短也不能绕过。宪法升级为 `3.0.0`，同步
  AGENTS、README、001 规格/计划/研究/数据模型/快速验证/OpenAPI 和注册测试。
- 验证：后端全量 111 项通过；前端 lint、类型检查、Vitest 41 项、生产构建和 E2E 20 项通过；
  59 秒发码继续返回 `RATE_LIMITED`，60 秒后允许重新发码；注册页 Taste 检查在
  1440/768/390 为 `0 FAIL`。
- 证据：`EmailVerificationServiceTest`、`AuthControllerRegistrationTest`、
  `frontend/kb-web/e2e/app.spec.ts`、`artifacts/001/email-only-registration.md`、
  `.taste-skill/email-only-registration/`。
- 生产部署：`v0.1.16-mvp` 已部署，release 为
  `0ddc96ba9713-20261021212647`；生产注册页图形验证码字段数为 0，只提交邮箱的发码请求
  进入注册业务逻辑，原应用和健康检查全部通过。
- 覆盖：FR-068 至 FR-073；登录验证码、注册邮箱验证码、60 秒频控和邮箱唯一性。
- 风险与回滚：回滚会重新要求注册图形验证码并使旧前端请求失败，必须前后端与契约同版本回滚；
  已签发邮箱验证码和账号数据不受影响。

### 完成记录：T087

- 日期：2026-10-22
- 状态：已完成
- 执行子智能体：产品、开发、测试
- Git commit：`6e45585`
- 摘要：账号密码最小长度由 12 位调整为 8 位，最大长度保持 256 位。
- 详情：注册请求、登录请求、一次性初始化请求和账号创建服务统一使用 8 位下限；注册页与
  登录页同步 `minlength=8`、256 位上限和中文/英文提示；OpenAPI、AGENTS 和 001 规格同步。
- 验证：8 位账号创建与登录测试通过，7 位创建被拒绝；注册页浏览器校验下限为 8；完整前后端
  测试和 Taste 检查通过。
- 证据：`OwnerServiceTest`、`AuthControllerRegistrationTest`、`frontend/kb-web/e2e/app.spec.ts`、
  `artifacts/001/password-minimum-eight.md`、`.taste-skill/password-minimum-eight/`。
- 生产部署：`v0.1.17-mvp` 已部署，release 为
  `e153900ac05f-20261021213349`；注册与登录输入框均为 8 位下限，8 位密码通过服务端
  DTO 校验，7 位密码被拒绝，原应用和健康检查全部通过。
- 覆盖：FR-073 及登录/注册密码校验。
- 风险与回滚：降低下限会削弱密码强度，但用户已明确批准；已有账号和密码哈希不受影响。
  回滚需前后端和 OpenAPI 同时恢复为 12 位下限，避免契约漂移。

### 完成记录：T088

- 日期：2026-10-22
- 状态：已完成
- 执行子智能体：产品、开发、测试
- Git commit：`e1badd0`
- 摘要：恢复原型中的四种采集方式，并接通现有安全能力支持的真实保存路径。
- 详情：采集页新增手动录入、网页链接、文档和截图标签；网页入口只保存用户提交的单个公开
  URL 及其来源时间，不使用爬虫；文档入口支持 JPG、PNG、WebP、HEIC、TXT 和 Markdown，
  PDF/Word 明确提示解析服务未接入且不创建空条目；截图入口只接受剪贴板图片并复用安全图片
  暂存、扫描和正文图片事务。
- 验证：四个入口切换、网页链接保存、Markdown 文档保存和剪贴板截图保存 E2E 通过；桌面与
  390px 四个入口截图已人工复核；Taste 采集页检查为 `0 FAIL`。
- 证据：`frontend/kb-web/src/views/CaptureView.vue`、`frontend/kb-web/e2e/app.spec.ts`、
  `artifacts/001/capture-modes.md`、`.taste-skill/capture-modes/`。
- 生产部署：`v0.1.18-mvp` 已部署，release 为
  `62e69e39553a-20261021214427`；生产采集页显示四个入口，网页和文档控件可切换，截图入口
  文件输入数为 0，原应用和健康检查全部通过。
- 覆盖：FR-017 至 FR-022、FR-031、FR-066、FR-067。
- 初次完成时的剩余风险：PDF/Word 内容解析仍由 T045、T051-T052 承载；当时网页 AI 截图分析
  和 OCR 尚未接入。回滚前端可恢复单入口采集，不影响已保存知识、上传或来源记录。
- 重新打开与完成（2026-10-22）：产品确认“网页链接收藏 URL”不符合需求。开发新增
  Playwright 单页截图 worker、Tesseract OCR、DeepSeek 多知识拆分和同一事务入库；原始 URL
  仅作为来源，正文不得等于 URL，失败时不创建占位知识。前端与原型同步为“AI截图拆分入库”，
  部署脚本增加 Chromium 和 `chi_sim+eng` 依赖。验证命令与证据见
  `specs/006-webpage-ai-import/tasks.md`；本次提交 SHA 未提交。
- 再次重新打开与完成（2026-10-22）：文档入口的 PDF/Word “未接入”提示已由
  `007-document-ai-import` 取代。后端新增 Apache Tika 内容检测与文字提取，PDF、DOCX、DOC、
  RTF、TXT 和 Markdown 均通过 AI 拆分为多条知识；前端选择非图片文档后显示外部 AI 同意。
  测试证据和未完成的生产验收见 `specs/007-document-ai-import/tasks.md`。本次提交 SHA 未提交。
- 第三次重新打开与完成（2026-10-22）：剪贴板截图不再作为图片附件保存。开发新增独立截图
  OCR API，复用 Tesseract、密钥脱敏、明确同意、AI 多知识拆分和 `succeeded` 状态；原截图只在
  请求期临时存在。验证证据见 `specs/008-screenshot-ai-import/tasks.md`。本次提交 SHA 未提交。

### 完成记录：T089

- 日期：2026-10-22
- 状态：已完成
- 执行子智能体：产品、开发、测试
- Git commit：`bd9972e`
- 摘要：恢复本地优先的联网搜索，并接入 DeepSeek Search 与单 URL Fetch。
- 详情：首页在本地搜索完成后显示“继续搜索互联网”；该历史版本要求用户再次点击且显式同意，后端使用
  `SHIHAI_DEEPSEEK_SEARCH_API_KEY` 调用官方 Search API，只发送搜索词。候选绑定当前账号并短期缓存；
  用户选择“收入识海”后，后端使用 `SHIHAI_DEEPSEEK_FETCH_API_KEY` 获取单个 URL 的 Markdown，转换为
  富文本并以 `internet_result` 来源保存。
- 后续变更：T104 已将交互调整为“互联网搜索 · 1 点”，入口单击即自动确认报价并直接搜索，
  不再保留搜索本身的二次确认。
- 验证：Provider Search/Fetch 协议、Bearer 认证和响应映射测试通过；候选归属与保存来源测试
  通过；前端 E2E 验证本地结果先显示、点击前不联网、显式同意字段和候选保存。生产
  `v0.1.19-mvp` 已确认前端入口存在，Search 返回 3 条真实结果，单 URL Fetch 返回 2866
  字符正文；未登录调用联网接口返回 HTTP 401。
- 证据：`DeepSeekWebProvider.java`、`ExternalSearchService.java`、
  `DeepSeekWebProviderTest.java`、`ExternalSearchServiceTest.java`、
  `frontend/kb-web/e2e/app.spec.ts`、`artifacts/001/internet-search.md`、
  `artifacts/001/deployment-verification.md`。
- 生产部署：commit `ed0f515`，tag `v0.1.19-mvp`，release
  `ed0f515a8952-20261021215401`；原拾题入口、识海入口和健康检查均正常。
- 覆盖：FR-008 至 FR-012、FR-040 至 FR-041、FR-060。
- 风险与回滚：MVP 当前不扣除 002 点数，正式收费前必须接入计费门禁；候选缓存重启后失效；
  Search/Fetch Provider 故障只影响联网区，不清空本地结果。回滚可移除互联网入口和后端端点，
  不影响已保存的 `internet_result` 知识。

### 完成记录：T090

- 日期：2026-10-22
- 状态：已完成
- 执行子智能体：产品、开发、测试
- Git commit：`b3e6a89`
- 摘要：修复“待整理”分类与“已整理”处理状态同时出现的语义冲突。
- 详情：移除分类缺失对 `home.noCategory` 的复用，统一显示“未分类”；新增共享处理状态映射，
  `not_requested` 显示“未整理”，未知状态显示“状态未知”；首页、知识库、待处理、详情和原型
  同步使用同一语义。
- 验证：`knowledge-status.test.ts` 3 项、Vitest 44 项、Playwright 32 项、lint、typecheck、build
  通过；E2E 使用无分类且 `not_requested` 的详情数据确认显示“未分类 / 未整理”。
- 证据：`frontend/kb-web/src/lib/knowledge-status.ts`、`frontend/kb-web/src/i18n.ts`、
  `frontend/kb-web/e2e/app.spec.ts`、`artifacts/001/status-semantics.md`。
- 生产部署：commit `b3e6a89`，tag `v0.1.22-mvp`，release
  `b3e6a89993d2-20261021225015`；生产条目“测试知识库”状态为 `not_requested`。
- 覆盖：FR-013、FR-023、FR-049。
- 风险与回滚：仅前端展示和 i18n 变化，不改数据库或 API；回滚 release 即可恢复旧映射。

### 完成记录：T091

- 日期：2026-10-22
- 状态：已完成
- 执行子智能体：产品、开发、测试
- Git commit：`0a7ff59`
- 摘要：恢复首页联网搜索入口的可发现性。
- 详情：入口不再依赖 `searchedQuery` 才渲染；初始状态显示“继续搜索互联网”并禁用，提示先完成
  本地检索。本地搜索完成后按钮启用，该版本仍需再次点击才调用 DeepSeek Search；原型同步更新。
  后续由 T104 改为入口显示 1 点，单击即自动确认报价并直接搜索。
- 验证：Playwright 验证初始按钮可见且禁用、本地检索后启用、点击前外部请求为 0；Vitest 44 项、
  Playwright 32 项、lint、typecheck、build 和 Taste lint 通过。
- 证据：`frontend/kb-web/src/views/HomeView.vue`、`frontend/kb-web/e2e/app.spec.ts`、
  `prototype/index.html`、`.taste-skill/internet-entry-production.png`。
- 生产部署：commit `0a7ff59`，tag `v0.1.23-mvp`，release
  `0a7ff592520f-20261021225621`；生产浏览器检查 `visible=true`、`disabled=true`。
- 覆盖：FR-047 至 FR-049；入口可发现性通过 E2E 与生产浏览器证据验证。
- 风险与回滚：仅前端展示条件变化；回滚 release 会恢复为搜索后才显示入口。

### 完成记录：T092

- 日期：2026-10-22
- 状态：已完成，本地自动化通过；生产真实 AI/OCR/计费验收未完成
- 执行子智能体：产品、开发、测试
- Git commit：未提交
- 摘要：把“手动录入唯一可直存，其他采集全部 AI 整理”固化为跨功能服务端边界。
- 详情：新增独立 AI 文字入口、`text` 来源和 `/api/v1/text-imports`；文档入口 JPG/PNG/WebP
  改为安全检查、libvips 规范化、Tesseract OCR 和 AI 整理；互联网候选改为明确授权、Fetch、
  脱敏和 AI 多知识事务入库；普通知识创建接口拒绝 `document`、`screenshot`、`internet_result`
  和 `text` 来源。原型新增五种采集模式，宪法升级为 `3.1.0`。
- 验证：`kb-core` 18 项、`kb-app` 175 项、worker 1 项通过；lint、typecheck、47 项 Vitest、
  34 项 Playwright E2E、前端生产构建、Spotless、YAML、原型和部署脚本检查通过。Taste 五档场景
  4 项通过，inspector 为 `0 FAIL / 1 WARN`。
- 证据：`specs/009-ai-assisted-capture/`、`artifacts/009/verification.md`、
  `tests/e2e/ai-capture-policy-taste.spec.ts`、`.taste-skill/inspect/`。
- 覆盖：FR-016、FR-021、FR-024、FR-041、FR-058、FR-060、FR-067、FR-074。
- 风险与回滚：生产真实 Tesseract、DeepSeek 和 002 点数门禁尚未验收；外部 AI 未计费时继续返回
  `402 AI_BILLING_REQUIRED`。回滚需同时恢复前端路由、专用导入服务和来源白名单，不能只回滚文档。

### 批量完成记录：2026-10-22 需求缺口收口

执行角色：产品、开发、测试子智能体。Git commit：未提交。以下完成项均有代码、迁移、自动化测试
或真实浏览器夹具验证；不包含真实 Waffo、真实 DeepSeek/Tesseract 和生产 PostgreSQL 验收。

| 任务 | 摘要与详情 | 验证与证据 | 风险与回滚 |
|------|------------|------------|------------|
| T093 | 新增 `V0010`、AI 摘要列、账号隔离分类词表和条目分配表；导入时保存摘要/分类/标签，用户纠正后锁定；搜索覆盖标题、正文、备注、图片文本、分类和标签并返回命中字段。 | `KnowledgeLifecycleControllerTest`、五类导入服务测试、后端全量测试、前端 38 项 E2E 中的分类/回收站场景；`ClassificationService.java`、`V0010__knowledge_classification.sql`。 | 历史知识备注中的旧 AI 分类不会自动迁移为关系数据；真实 100k 搜索基准仍为 T065/T066。 |
| T094 | 接通回收站列表、移入回收站、恢复和永久清除；活动搜索立即过滤，附件按引用进入清理队列；新增前端回收站和详情删除确认。 | `KnowledgeLifecycleControllerTest` 5 项、Playwright 分类/回收站用例、移动触控检查。 | 物理对象清理 worker 仍取决于部署任务；回滚 API 不自动恢复已 purge 数据。 |
| T095 | 新增账号级 AI 外发授权表与 API；设置页分别授权文本、网页、文档、截图和搜索，采集页自动读取并撤销，服务端入口同时校验。 | `AiConsentControllerTest` 4 项、采集服务专项测试、前端 lint/typecheck；`V0012__ai_processing_consent.sql`。 | 授权历史只保留当前状态；如需完整审计历史应新增追加式规格。 |
| T096 | 新增持久化 API 幂等表和统一服务；五类 AI 入口保存请求哈希与响应，同键同体重放、异体冲突；前端失败重试复用稳定键。 | 文档/截图/网页/文字/外部搜索专项测试、后端全量测试；`V0013__api_idempotency.sql`、`IdempotencyService.java`。 | 真实并发和进程重启恢复仍待真实 PostgreSQL 测试。 |
| T097 | 新增付费重新分析 API 和详情页报价确认；DeepSeek 更新摘要、分类和标签，用户锁定字段不被覆盖，失败释放预留。 | `KnowledgeReanalysisControllerTest` 3 项、前后端 typecheck/build、详情页交互检查。 | 真实 DeepSeek 和真实扣费仍待生产验收；回滚 API 可保留已有摘要。 |
| T098 | 计费操作由服务端强制计费项/数量；预检账号隔离和有效期；支付签名、商品、币种和订单状态 fail closed；AI 入口在外部调用前释放任务行锁。 | `BillingP0PolicyTest` 9 项、后端 226 项、桥接 3 项。 | 真实 Waffo 测试商户和物理数据库仍由 002 T078/T079 阻塞。 |
| T099 | 退款使用实际可回收批次点数；管理员调整显式目标账号并维护投影；修复财务日期过滤和异常对账；买家标识改为不透明 UUID；桥接增加成功地址、未知订单和双向密钥约束。 | 计费专项、桥接 typecheck/test/build、部署脚本检查。 | 退款 held 批次和自动二次回收仍需真实任务结算演练。 |
| T100 | 部署脚本增加 PostgreSQL 备份、桥接/片段回滚和显式计费开关；管理员邮箱与邮件退款入口写入环境；桥接 unit 去掉不必要写权限。 | `tests/deploy/verify-deploy-scripts.mjs`、Git Bash 语法检查、部署脚本 diff 审计。 | 未在生产主机执行本次新版脚本；必须先在 staging 演练数据库恢复和回滚。 |
| T101 | 原型增加点数和七项移动导航；前端补英文套餐名、ARIA、页面标题、来源 URL、分类纠错和重新分析；OpenAPI 与示例环境变量同步。 | Vite build、39 项前端 E2E（37 通过/2 live 跳过）、原型 `node --check`、Taste lint 0 FAIL/0 WARN。 | 原型与前端仍需在同一固定 commit 完成截图对照归档。 |

### 完成记录：T103

- 日期：2026-10-22
- 状态：已完成，本地自动化、原型交互和桌面/平板/手机视觉检查通过；真实 PostgreSQL 并发与生产验收未执行
- 执行子智能体：产品、开发、测试
- Git commit：`cb59600`
- 摘要：知识库列表页新增服务端检索、匹配字段、逐行删除、三态全选和原子批量回收。
- 详情：列表检索由一个输入框覆盖标题、正文、备注、分类和标签，不触发互联网搜索或 AI 扣点；
  每行复选框与删除按钮固定 44px 触控槽，删除和批量删除均使用确认框。批量接口
  `POST /api/v1/knowledge-items/bulk-recycle` 去重后限制 1–100 条，按稳定 UUID 顺序校验账号所有权
  并执行全有或全无回收；`Idempotency-Key` 由现有持久化幂等服务处理同体重放和异体冲突。搜索、来源
  筛选、刷新和成功删除会清空选择，失败时保留行、选择、检索条件和确认上下文。同步更新 OpenAPI、
  数据模型、计划、快速验证、检查清单、001/003 任务、中英文文案和交互原型。
- 验证：后端 241 项通过；列表专项 Playwright 6 项通过；前端完整 Playwright 49 项可执行用例中
  48 项通过、2 项 live 跳过；Vitest 48 项通过；lint、typecheck、生产构建通过；原型浏览器验证
  搜索 12→2 条、取消单删保持 12 条、批量删除 12→10 条，390px 横向溢出为 0，复选框和删除按钮
  均为 44×44px；Taste app lint 前端 0 FAIL/4 个既有浮层阴影 WARN，原型 0 FAIL/0 WARN。
- 证据：`frontend/kb-web/src/views/LibraryView.vue`、`KnowledgeServiceBulkRecycleTest.java`、
  `KnowledgeLifecycleControllerTest.java`、`frontend/kb-web/e2e/app.spec.ts`、`prototype/`、
  `artifacts/001/library-search-bulk-delete.md`、`frontend/kb-web/test-results/library-*.png`。
- 覆盖：FR-003、FR-006、FR-007、FR-035、FR-036、FR-047、FR-075 至 FR-078；SC-013、SC-019、SC-020。
- 风险与回滚：本机 Docker 不可用，Testcontainers PostgreSQL 用例执行 0 项；100 条以上结果尚无游标
  分页，来源计数可能不完整；真实 iOS Safari、Android Chrome、软键盘、物理安全区和屏幕阅读器仍由
  T073/T102/003 T036 承载。回滚接口和前端交互不需要数据库迁移，已回收数据仍可按既有流程恢复。

### 完成记录：T104

- 日期：2026-10-22
- 状态：SDD 文档同步已完成；生产代码、数据库迁移和自动化验收未由本任务执行
- 执行子智能体：产品
- Git commit：未提交
- 摘要：纯互联网搜索固定 1 点，入口单击即自动确认报价并直接搜索，取消搜索本身的二次确认。
- 详情：001 规格明确入口文案“互联网搜索 · 1 点”、单击同意扣费、自动报价确认和直接搜索；
  AI 回答仍按 2 点另计，选择回答时点击前显示 1 + 2 = 3 点。计划、数据模型、OpenAPI、
  快速验证、检查清单和原型说明同步该流程。历史记录中的“继续搜索互联网”二次点击仅代表旧版本，
  不再是当前验收口径。
- 验证：检索相关文档中的旧“搜索每次 3 点”与搜索二次确认表述；检查标题、任务编号、相对路径和
  中英文事实边界。未运行生产代码、数据库迁移或测试代码的自动化验证。
- 证据：`specs/001-ai-personal-knowledge-base/`、`specs/002-ai-credit-billing/`、
  `prototype/README.md`。
- 覆盖：FR-009、FR-079、SC-021，以及 002 FR-035、FR-038、SC-026。
- 风险与回滚：当前前端和后端实现尚未由本任务修改，部署验收前仍可能出现 3 点报价或二次确认；
  开发与测试任务必须按此规格补齐。文档回滚会恢复旧的 3 点和二次确认口径。

### 完成记录：T105

- 日期：2026-10-23
- 状态：本地实现与自动化验收完成
- 执行主体：Codex
- Git commit：`30c5564`
- 摘要：区分 DeepSeek 搜索服务故障与单个网页来源不可抓取，候选整理失败时保留其他搜索结果。
- 详情：验证同一个 DeepSeek Key 调用 Chat 和 `/v1/tools/search` 均返回 200；故障来自
  `/v1/tools/fetch` 对部分 36Kr/BBC 等来源返回 404。后端新增
  `EXTERNAL_SOURCE_UNAVAILABLE`，只在 Fetch 上游 404 或正文为空时返回；认证、额度和搜索服务
  错误继续使用原有服务不可用语义。前端把该错误显示在当前候选行内，不再用整块搜索错误覆盖结果，
  用户可直接选择其他来源继续整理。
- 验证：DeepSeek Provider 404 回归用例通过；后端完整 Maven 测试通过；前端 ESLint、TypeScript、
  53 项 Vitest、51 项 Playwright 和生产构建通过；真实搜索的 8 个候选抽查中 4 个可抓取、4 个返回 404。
  使用本地真实账号复测“梁文峰”：8 条候选保持可见，36Kr 候选显示行内提示；该次操作只结算搜索
  1 点，候选整理预留 5 点在 Fetch 404 后完整释放，没有把失败整理计入消费。
- 证据：`DeepSeekWebProvider.java`、`ExternalSourceUnavailableException.java`、
  `ApiExceptionHandler.java`、`DeepSeekWebProviderTest.java`、`HomeView.vue`、
  `frontend/kb-web/e2e/app.spec.ts`、`specs/001-ai-personal-knowledge-base/contracts/openapi.yaml`。
- 覆盖：FR-009、FR-079、SC-021。
- 风险与回滚：来源可抓取性由 DeepSeek Fetch 动态决定，不能用静态域名白名单保证；回滚会恢复
  全局错误展示，但不影响已保存知识或账本。

### 完成记录：T106

- 日期：2026-10-23
- 状态：本地实现与自动化验收完成
- 执行主体：Codex
- Git commit：`cc6c4dc`
- 摘要：互联网搜索过滤没有可整理正文的候选，并为单页 Fetch 失败提供同次搜索正文兜底。
- 详情：DeepSeek Search 请求改为 `need_content=true`，候选正文只保存在服务端短期缓存，不返回浏览器；
  正文少于 200 字符的候选不展示。用户选择候选后仍优先调用单页 Fetch；Fetch 返回 404、不可用或
  正文为空时，仅在缓存正文足够时使用该候选正文继续脱敏和 AI 整理。系统不会为预检逐条调用 Fetch，
  也不遍历候选中的链接。
- 验证：DeepSeek Provider 请求与响应测试、无正文过滤测试、Fetch 失败正文兜底测试通过；后端完整
  Maven 测试通过；前端既有 53 项 Vitest 与 51 项 Playwright 通过。使用本地真实账号复测
  “梁文峰”：8 条有正文候选保持可见，先前 Fetch 返回 404 的 36Kr 候选通过同次 Search 正文成功
  整理并生成 6 条知识；本次搜索结算 1 点、整理成功结算 5 点，验证后已清理 6 条测试知识。
- 证据：`DeepSeekWebProvider.java`、`ExternalSearchService.java`、
  `DeepSeekWebProviderTest.java`、`ExternalSearchServiceTest.java`、`AGENTS.md`、
  `contracts/openapi.yaml`。
- 覆盖：FR-010、FR-012、FR-080。
- 风险与回滚：Search 正文会增大单次搜索结果体积，但仍限制在服务端内存短期缓存；若 Search Provider
  不返回正文，候选会被过滤。回滚会恢复无正文过滤和必须依赖单页 Fetch 的行为。

### 完成记录：T107

- 日期：2026-10-23
- 状态：本地实现与自动化验收完成
- 执行主体：Codex
- Git commit：`839e47a`
- 摘要：知识库列表和本地搜索改为服务端分页，来源筛选计数覆盖完整结果集。
- 详情：知识条目与搜索接口新增 `page`、`limit`、`source` 参数，响应返回页码、每页条数、总条数、
  总页数和来源计数；默认每页 10 条，可切换 10/20/50。列表、搜索和来源筛选均使用同一分页元数据，
  切换搜索、筛选或页大小时回到第 1 页，翻页时清空选择；删除成功后重新加载当前页，空页由服务端
  自动夹取到最后一个有效页。原型同步增加分页控件。
- 验证：后端完整 Maven 测试通过；真实账号验证普通列表 24 条为 3 页、每页 10 条，来源筛选返回
  `internet_result=24`；搜索 `AI` 同样返回 10/10 条和 3 页；桌面第 2 页与 390px 手机截图检查通过，
  手机横向溢出为 0。前端分页专项 Playwright、TypeScript 和 ESLint 通过。
- 证据：`KnowledgeService.java`、`KnowledgeController.java`、`KnowledgeDtos.java`、
  `LibraryView.vue`、`app.spec.ts`、`prototype/app.js`、`contracts/openapi.yaml`。
- 覆盖：FR-006、FR-008、FR-035、FR-081。
- 风险与回滚：当前使用页码限制和 `LIMIT/OFFSET`，超大数据量下后续可升级为稳定的
  `updated_at + id` 键集游标；回滚会恢复单次最多 100 条的无分页列表。

### 完成记录：T108

- 日期：2026-10-23
- 状态：本地实现与自动化验收完成
- 执行主体：Codex
- Git commit：`838f52e`
- 摘要：互联网搜索不再过滤不可整理候选，改为禁用收入操作并提示原因。
- 详情：搜索接口为每个候选返回 `organizable`。没有足够正文的候选仍显示标题、摘要和来源，
  “AI 整理后收入”按钮禁用；悬停显示原因，点击操作区域也显示同样的中文/英文提示。服务端对
  直接调用保存且候选不可整理的情况失败关闭并释放已预留点数，不调用 Fetch 或 AI。
- 验证：后端不可整理候选仍返回、直接保存拒绝且不调用 Fetch 的测试通过；前端专项 Playwright
  验证按钮禁用、悬停 title 和点击提示通过；TypeScript 与原型 `node --check` 通过。
- 证据：`ExternalSearchDtos.java`、`ExternalSearchService.java`、`ExternalSearchServiceTest.java`、
  `HomeView.vue`、`types/api.ts`、`prototype/app.js`、`contracts/openapi.yaml`。
- 覆盖：FR-010、FR-012、FR-080。
- 风险与回滚：可整理性基于 Search Provider 返回正文长度阈值 200 字符；阈值调整可能改变按钮状态。
  回滚会恢复隐藏不可整理候选的旧行为。

### 完成记录：T109

- 日期：2026-10-23
- 状态：本地实现与自动化验收完成
- 执行主体：Codex
- Git commit：未提交
- 摘要：AI 整理和 AI 采集结果必须人工确认复核，手动录入不进入复核队列。
- 详情：所有 AI 文字、网页、文档、文档图片、截图和互联网候选通过统一
  `KnowledgeService.createOrganized` 以 `review` 入库；重新分析成功后同样回到 `review`。
  新增 `POST /api/v1/knowledge-items/{itemId}/review`，按当前账号和 active 状态加锁确认，
  将条目转为 `succeeded`，重复确认幂等，非法状态返回 409。手动创建继续使用 `not_requested`。
  待处理页和详情页均增加“确认复核”，AI 采集完成后进入待处理页；原型同步五列桌面结构和
  移动端复核操作。知识列表接口支持服务端处理状态过滤，待处理页以 20 条分页请求
  `pending,queued,running,processing,review,failed`，避免超过首批 100 条后漏项。
- 验证：后端完整 Maven 测试 260 项通过（`kb-core` 23、`kb-app` 236、worker 1）；前端 54 项
  Vitest、55 项 Playwright 通过、2 项真实服务用例跳过；ESLint、TypeScript、生产构建和原型
  `node --check` 通过。Taste 原型 lint 为 `0 FAIL / 0 WARN`，待处理页 1440/768/390 浏览器
  inspector 为 `0 FAIL / 1` 个既有小字号警告；前端复核队列和详情页三档截图检查通过。
  本地测试栈重启健康，实际 API 创建手动知识返回 `not_requested`，直接调用复核接口返回 409，
  状态过滤查询可从 `not_requested` 结果命中但不会进入复核队列，临时数据已回收并永久清除。
- 证据：`KnowledgeItemEntity.java`、`KnowledgeService.java`、`KnowledgeReanalysisService.java`、
  `KnowledgeController.java`、`ProcessingView.vue`、`DetailView.vue`、`prototype/app.js`、
  `contracts/openapi.yaml`、`frontend/kb-web/e2e/app.spec.ts`。
- 覆盖：FR-015、FR-067、FR-082。
- 风险与回滚：上线前已经处于 `succeeded` 的历史 AI 条目不会自动回退，因为旧数据无法可靠区分
  全部手动富文本来源；需要复核时可重新分析或由后续受控迁移处理。回滚代码会恢复 AI 结果直接
  `succeeded`；已确认复核的数据仍按现有状态保留。

### 完成记录：T110

- 日期：2026-10-23
- 状态：本地实现与自动化验收完成
- 执行主体：Codex
- Git commit：未提交
- 摘要：侧栏“待处理”入口显示当前账号待处理总数，并与复核操作实时同步。
- 详情：`AppShell` 在会话恢复后以 `limit=1` 查询服务端待处理状态集合总数，使用
  `pageInfo.totalItems` 渲染徽标；路由变化、确认复核和重新分析会触发安全刷新。待处理页与
  侧栏共用同一状态集合常量。徽标采用原型中的右上角绝对定位，移动端偏移到导航图标右上角，
  0 条时不渲染。
- 验证：前端 55 项 Vitest、TypeScript、专项 Playwright 通过；专项用例验证待处理徽标从 1
  更新为 0，并完成 1440/768/390 截图检查；完整 ESLint、Playwright 和生产构建随后执行。
- 证据：`AppShell.vue`、`knowledge-status.ts`、`ProcessingView.vue`、`DetailView.vue`、
  `prototype-theme.css`、`frontend/kb-web/e2e/app.spec.ts`。
- 覆盖：FR-015、FR-082。
- 风险与回滚：计数刷新失败时保留最近一次成功值，避免徽标闪回 0；回滚只移除侧栏计数查询和
  徽标，不影响待处理页服务端过滤与复核状态机。

### 完成记录：T111

- 日期：2026-10-23
- 状态：本地实现与视觉验收完成
- 执行主体：Codex
- Git commit：未提交
- 摘要：待复核状态改为深海蓝主题内的青蓝紧凑标签，不再使用棕黄色警告背景。
- 详情：新增独立 `--pill-review-bg` 和 `--pill-review-ink` 语义 token，与警告状态分离。
  `processing-row` 收窄状态列，桌面端状态标签按内容宽度靠左，移动端保持卡片右上角对齐；
  日期列保留独立空间，避免状态色块横跨整列。前端与原型同步。
- 验证：TypeScript、ESLint 和复核专项 Playwright 通过；前端 1440/390 截图确认青蓝标签、
  日期对齐和移动端布局正常；原型 Taste lint `0 FAIL / 0 WARN`，inspector `0 FAIL`。
- 证据：`styles.css`、`prototype-theme.css`、`prototype/styles.css`、
  `frontend/kb-web/e2e/app.spec.ts`。
- 覆盖：FR-082。
- 风险与回滚：青蓝色用于信息状态，失败仍保留红色、已整理保留绿色；回滚会恢复棕黄色警告样式，
  不影响状态数据或复核接口。

### 完成记录：T112

- 日期：2026-10-23
- 状态：本地实现与视觉验收完成
- 执行主体：Codex
- Git commit：未提交
- 摘要：知识详情正文与右侧说明栏加宽，宽屏下更充分利用可用空间。
- 详情：详情页上限提升到 1680px，布局调整为自适应的主栏加 320–380px 说明栏；正文和旧版文本
  覆盖基础主题中的 740/780px 限制，阅读区扩展至 1080px，正文提升到 16px 并增加行高。原型在
  进入详情视图时同步切换加宽画布。1100px 以下继续单列，移动端不产生横向滚动。
- 验证：TypeScript、ESLint、详情页专项 Playwright 和原型语法检查通过；1920、1440、768、390
  截图检查确认宽屏铺开、说明栏宽度、移动端单列和触控布局正常。生产构建与完整 E2E 随后执行。
- 证据：`prototype-theme.css`、`DetailView.vue`、`prototype/styles.css`、`prototype/app.js`、
  `frontend/kb-web/e2e/app.spec.ts`。
- 覆盖：FR-083。
- 风险与回滚：极宽的正文在 1080px 处停止扩展，避免形成难以阅读的超长行；回滚会恢复 1040px
  页面、780px 正文和 306px 说明栏。

### 完成记录：T113

- 日期：2026-10-23
- 状态：本地实现与自动化验收完成
- 执行主体：Codex
- Git commit：未提交
- 摘要：修复详情页删除文字换行，并将待复核按钮移到编辑按钮右侧。
- 详情：删除操作改为不可收缩的单行图标文字按钮；顶部操作顺序统一为“删除 / 编辑知识 /
  确认复核”，仅 `review` 状态渲染复核按钮，状态说明区不再重复该操作。窄屏顶部栏改为上下两行，
  返回链接独占第一行，三个操作在第二行保持顺序并保留 44px 触控高度。原型同步顶部复核按钮。
- 验证：TypeScript、ESLint、详情页专项 Playwright 和原型语法检查通过；1920、1440、768、390
  截图确认删除不换行、复核位于编辑右侧、移动端三个操作同排；生产构建与完整 E2E 随后执行。
- 证据：`DetailView.vue`、`prototype-theme.css`、`prototype/index.html`、`prototype/app.js`、
  `frontend/kb-web/e2e/app.spec.ts`。
- 覆盖：FR-082、FR-083。
- 风险与回滚：极窄且使用超大系统字体时允许操作区自然换行，优先保证触控可用性；回滚会把复核
  操作恢复到右侧状态区并恢复旧文字布局。

### 完成记录：T114

- 日期：2026-10-23
- 状态：本地实现、自动化和视觉验收完成；真实 DeepSeek/Tesseract 生产验收未执行
- 执行主体：Codex
- Git commit：未提交
- 摘要：AI 整理结果改为结构化富文本，并尽量保留网页、文档和截图中的安全来源图示。
- 详情：DeepSeek 提示改为输出受控 Markdown 子集；服务端映射标题、段落、有序/无序列表、引用、
  代码块、粗体、行内代码和安全 HTTP(S) 链接。网页单页截图、文档入口图片、DOCX `word/media`
  位图、PDF 页面资源位图都进入统一图片安全候选；超过 1,800px 的网页截图按阅读顺序切成最多
  6 个不超过 8192px 安全边界的图块，较短页面继续保留单张原图。通过规范化、像素、动图、
  密钥和 OCR 检查后，
  且最小边不少于 120px、面积不少于 40,000 像素的候选，复用 `rich_image_upload`、附件和
  `rich_image_reference` 进入首条知识正文。单次最多 6 张，
  图片失败或检测到密钥时只跳过图片，不丢弃文字知识。详情页增加来源图示卡片、最大 720px
  预览高度和点击放大。
- 验证：后端完整 Maven 测试 271 项通过（`kb-core` 23、`kb-app` 247、worker 1）；相关专项测试
  51 项通过，覆盖 Markdown 富文本、网页长截图切块、DOCX 图片提取、网页/文档/截图首条知识
  附图、图片密钥跳过和附件安全检查。前端 55 项 Vitest、55 项 Playwright 通过、2 项真实服务
  用例跳过；
  TypeScript、ESLint、生产构建、原型语法和 Taste lint 通过；详情图片继续使用现有
  1920/1440/768/390 三档浏览器夹具验证。
- 证据：`ImportedKnowledgeMapper.java`、`ImportImageAssetService.java`、
  `DocumentTextExtractor.java`、`ScreenshotTextExtractor.java`、
  `PlaywrightOcrWebPageCaptureService.java`、`WebPageImportService.java`、
  `DocumentImportService.java`、`ScreenshotImportService.java`、`CaptureView.vue`、
  `prototype-theme.css`、`prototype/app.js`。
- 覆盖：FR-021、FR-022、FR-084、FR-085。
- 风险与回滚：图示保留采用“安全候选尽量保留”策略，不做视觉语义判断，可能保留少量非关键图片，
  也可能因 OCR/扫描失败而跳过关键图片；网页长截图按固定阅读高度切块，跨越切块的流程图可能被
  拆成两段；DOC 和 RTF 内嵌图示尚未提取，PDF 仅提取可解码页面位图，扫描版纯页面依赖截图入口。
  真实 DeepSeek 富文本质量、Tesseract 和 6 张图片 UI 仍需生产验收。
  回滚会恢复纯段落映射和无线索图采集，不影响已有富文本或附件数据。

### 完成记录：T115

- 日期：2026-10-23
- 状态：本地实现和真实全链路验收完成
- 执行主体：Codex
- Git commit：未提交
- 摘要：修复网页采集 OCR 依赖缺失，以及 DOCX 文档提取报价单位固定的问题。
- 详情：日志和手工 worker 验证显示 Playwright 截图正常，失败发生在本机未安装 Tesseract。
  安装 Tesseract 5 和 `chi_sim+eng` 语言包，配置 `SHIHAI_TESSERACT_COMMAND`、
  `TESSDATA_PREFIX` 和 `APP_RICHTEXT_VIPSCOMMAND`。文档入口改为按预检返回的字符上界计算
  `ceil(characters / 10000)`，与后端 `DOCUMENT_EXTRACT` 计费规则一致。
- 验证：网页入口真实返回 `201`，生成 3 条待复核知识，首条包含网页来源截图；DOCX 入口正确报价
  35 点并返回 `201`，生成 7 条待复核知识，首条包含 4 张内嵌图片。后端全量 271 项、Vitest 55
  项、Playwright 56 项通过，2 项真实服务用例跳过；前端 lint、类型检查和生产构建通过。
- 证据：`frontend/kb-web/src/views/CaptureView.vue`、
  `frontend/kb-web/e2e/app.spec.ts`、`deploy/local-test/README.md`。
- 覆盖：FR-021、FR-022、FR-084、FR-085。
- 风险与回滚：`.env.local-test` 中的解释器和语言目录是本机绝对路径，不进入 Git；其他机器仍需
  安装相同依赖并配置路径。回滚代码会把文档提取恢复为固定 1 个单位，较大文档将再次出现报价
  与后端规则不一致。

### 完成记录：T116

- 日期：2026-10-23
- 状态：本地实现、自动化和真实联网验收完成
- 执行主体：Codex
- Git commit：未提交
- 摘要：互联网搜索结果收入改为单页图文采集，和网页链接入口共用截图、OCR、安全图示及富文本能力。
- 详情：候选保存对用户选择的唯一 URL 执行单页截图；截图 OCR 先做密钥检查，通过后复用
  `ImportImageAssetService`、附件和 `rich_image_reference`，把图示加入首条知识。AI Markdown
  继续映射标题、段落、列表、引用、代码和链接，并新增管道表格到 Tiptap 表格节点。完整报价为
  网页视觉 10 点加分类 5 点；截图失败但文字整理成功时只结算分类 5 点。
- 验证：后端全量 275 项通过（`kb-core` 23、`kb-app` 251、worker 1）；前端 Vitest 55 项、
  Playwright 56 项通过，2 项真实服务用例跳过；lint、类型检查和生产构建通过。真实 DeepSeek
  搜索选择“首次调用 API”候选后返回 `201`，生成 3 条待复核知识，首条节点包含标题、段落、
  表格、列表和 1 张来源图示；详情页实际渲染 1 个表格、1 张图片且无横向溢出。
- 证据：`ExternalSearchService.java`、`ImportedKnowledgeMapper.java`、`HomeView.vue`、
  `ExternalSearchServiceTest.java`、`ImportedKnowledgeMapperTest.java`、
  `frontend/kb-web/e2e/app.spec.ts`。
- 覆盖：FR-012、FR-041、FR-080、FR-084、FR-085、FR-086。
- 风险与回滚：该版本曾把整页截图作为来源图，现由 T117 取代；页面视觉失败时仍只保留文字整理
  且按 5 点结算。回滚只会恢复旧链路，不应保留整页截图作为长期正文来源。

### 完成记录：T117

- 日期：2026-10-23
- 状态：本地实现、自动化和真实网页验收完成
- 执行主体：Codex
- Git commit：未提交
- 摘要：网页来源图改为提取网页实际展示的内容图片、SVG 和 Canvas，不再把整页截图收入知识正文。
- 详情：Playwright worker 新增 DOM 视觉元素扫描，只对渲染尺寸至少 240×120 且面积不少于
  40,000 像素的 `img`、`svg`、`canvas` 生成独立图片。导航、页头、页脚、侧栏、广告位、
  固定定位、全屏背景 Canvas、右侧推荐栏和小图标会被排除；相同来源或高度重叠的候选去重，
  单页最多 6 张。整页截图继续仅用于 OCR，页面链接和互联网候选共用该图片提取链路。
- 验证：backend worker 单测、网页入口和候选保存专项测试通过；后端全量 277 项通过
  （`kb-core` 23、`kb-app` 252、worker 2）；Vitest 55 项、Playwright 56 项通过，2 项真实
  服务用例跳过；lint、类型检查、生产构建和原型语法检查通过。真实 `globalpeople` 新闻页从
  DOM 提取 5 张正文图片，圈选的 DeepSeek 推文图为第 4 张，右侧体育横幅被排除；网页链接和
  互联网候选分别入库后，首条知识均含 5 张图片，桌面与 390px 详情页加载正常且无横向溢出。
- 证据：`WebPageCaptureWorker.java`、`PlaywrightOcrWebPageCaptureService.java`、
  `WebPageScreenshotSplitter.java`、`ExternalSearchService.java`、`WebPageImportService.java`、
  `i18n.ts`、`prototype/app.js`、相关后端与前端测试。
- 覆盖：FR-041、FR-080、FR-084、FR-085。
- 风险与回滚：复杂 Web Component、跨域 iframe 内图片或运行时 CSS 背景图可能无法作为独立元素
  提取；此类页面只保留 OCR 文字和可识别的内容元素。回滚会恢复以整页截图作为来源图。
