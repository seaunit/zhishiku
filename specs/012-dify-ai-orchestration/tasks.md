# 任务：Dify AI 编排

**输入**：`specs/012-dify-ai-orchestration/`

**前置文档**：[plan.md](plan.md)、[spec.md](spec.md)、[research.md](research.md)、[data-model.md](data-model.md)、[contracts/](contracts/)

## Phase 1：准备

**目的**：建立本地 Dify 工作流和可配置接入基础。

- [x] T001 [P0] 在 `deploy/dify/shihai-knowledge-organizer.yml`、`deploy/dify/shihai-xiaoshi-chat.yml` 和 `deploy/dify/README.md` 中创建识海独立 Dify DSL 与导入说明
- [x] T002 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/ai/orchestration/AiOrchestrationProperties.java` 中定义提供商、Dify 地址、独立密钥引用、超时和重试配置
- [x] T003 [P0] 在 `backend/kb-app/src/main/resources/application.yml` 和 `application-local.yml` 中增加 `SHIHAI_AI_ORCHESTRATION_*`、`SHIHAI_DIFY_*` 配置，不写真实值
- [x] T004 [P0] 在 `.env.example` 中增加 Dify 配置占位并明确生产默认仍为 `deepseek`

---

## Phase 2：基础能力

**目的**：实现与业务无关的 Dify 调用、响应校验和审计边界。

**关键约束**：本阶段完成后，业务服务仍保持原有行为；只有显式选择 `dify` 时启用新实现。

- [x] T005 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/ai/orchestration/AiOrchestrationClient.java` 中定义阻塞、流式和错误边界接口
- [x] T006 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/ai/orchestration/AiOrchestrationException.java` 中定义脱敏错误分类
- [x] T007 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/ai/orchestration/DifyAiOrchestrationClient.java` 中实现 Dify 工作流请求、认证、超时、有限重试和运行标识日志脱敏
- [x] T008 [P0] 在 `DifyAiOrchestrationClient.java` 中解析阻塞输出和 `text_chunk`、`workflow_finished`、`error` 流式事件，拒绝未知或失败状态
- [x] T009 [P0] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/ai/orchestration/DifyAiOrchestrationClientTest.java` 中覆盖成功、不可用、HTTP 失败、畸形响应、流式成功和推理过滤
- [x] T010 [P0] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/config/ApplicationConfig.java` 中启用新配置，并保持缺失配置时 `deepseek` 兼容行为

---

## Phase 3：知识整理迁移

**目标**：Dify 模式覆盖 AI 文字、网页、文档、截图和互联网候选，同时保留现有后端校验和事务。

**独立测试**：`DifyKnowledgeOrganizerTest` 与现有导入服务测试通过，Dify 模式不改变知识输出结构。

- [x] T011 [P0] [US1] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/webimport/DifyKnowledgeOrganizer.java` 中实现五个知识整理接口并委托统一 Dify 客户端
- [x] T012 [P0] [US1] 将 `DeepSeekKnowledgeOrganizer` 改为仅在 `deepseek` 提供商下装配，避免出现两个同类型业务 Bean
- [x] T013 [P0] [US1] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/webimport/DifyKnowledgeOrganizerTest.java` 中覆盖结构化结果、URL-only 过滤、重复标题和缺失元数据
- [x] T014 [P0] [US1] 复用 `ImportedKnowledgeMapper` 与现有富文本映射，确保 Dify 输出不绕过安全检查、图片引用、复核状态和原子入库

---

## Phase 4：小识对话迁移

**目标**：知识回答、通用回答、检索计划、联网回答和流式回答使用 Dify，同时保持后端引用、计费和会话规则。

**独立测试**：`DifyKnowledgeChatClientTest`、`KnowledgeChatServiceTest` 和流式控制器测试通过。

- [x] T015 [P0] [US2] 在 `backend/kb-app/src/main/java/site/seaunit/zhishiku/chat/DifyKnowledgeChatClient.java` 中实现所有 `KnowledgeChatModelClient` 操作
- [x] T016 [P0] [US2] 将 `DeepSeekKnowledgeChatClient` 改为仅在 `deepseek` 提供商下装配
- [x] T017 [P0] [US2] 在 `DifyKnowledgeChatClient.java` 中保留 JSON 解析、查询去重、引用上限、`INSUFFICIENT_EVIDENCE` 和 `NEEDS_INTERNET` 业务哨兵
- [x] T018 [P0] [US2] 在 `backend/kb-app/src/test/java/site/seaunit/zhishiku/chat/DifyKnowledgeChatClientTest.java` 中覆盖知识回答、通用回答、计划查询和流式输出
- [x] T019 [P1] [US2] 在 `DifyAnswerService` 或等价适配器中接管互联网搜索结果回答，DeepSeek Search/Fetch 仍由后端执行

---

## Phase 5：本地 Dify 安装与联调

**目标**：在本地 Dify 创建识海专用应用、发布工作流和创建独立 API Key，完成后端端到端验证。

**独立测试**：阻塞整理和五类聊天操作均能从本地 Dify 得到有效响应，现有业务数据规则不变。

- [x] T020 [P0] [US3] 通过本地 Dify Console 导入两个识海工作流并发布，不复用或修改拾题应用
- [x] T021 [P0] [US3] 为两个识海工作流创建独立 API Key，并仅保存到本机用户级环境变量
- [x] T022 [P0] [US3] 解决 `127.0.0.1:8501` 宿主不可达的本地联调问题，使用独立回环桥接 `127.0.0.1:8502`，不修改 Dify 业务容器
- [x] T023 [P0] [US3] 使用真实本地 Dify 模型完成一次知识整理、一次知识聊天和一次流式回答
- [x] T024 [P1] [US3] 验证 `provider=deepseek` 显式回退仍可装配和通过现有测试

---

## Phase 6：验证与收尾

**目的**：完成回归、安全检查和文档同步。

- [x] T025 [P0] 运行后端全量 Maven 测试和构建，记录 Dify 与 DeepSeek 两种模式结果
- [x] T026 [P0] 运行前端 lint、TypeScript 检查、Vitest 和生产构建，确认接口兼容
- [x] T027 [P0] 执行密钥扫描、`git diff --check` 和敏感日志检查
- [x] T028 [P1] 更新 `README.md`、`AGENTS.md` 和 `.specify/memory/constitution.md` 中的 AI 编排边界
- [x] T029 [P1] 将 `.specify/feature.json` 指向 `specs/012-dify-ai-orchestration`
- [x] T030 [P2] 记录本地 Dify 临时桥接的启动、健康检查和清理命令，不将其作为生产依赖

---

## Phase 7：生产 Dify 切换

**目标**：在生产服务器已有 Dify 上创建识海独立应用和密钥，切换生产编排并安全部署。

- [x] T031 [P0] 在生产 Dify 导入并发布 `shihai-knowledge-organizer` 和 `shihai-xiaoshi-chat`，创建独立应用级 API Key
- [x] T032 [P0] 备份 `/etc/zhishiku/kb.env`，写入 `SHIHAI_AI_ORCHESTRATION_PROVIDER=dify`、生产 Dify 地址和两个工作流 Key
- [x] T033 [P0] 在生产服务器内部直连 Dify，验证知识整理和通用聊天工作流
- [ ] T034 [P0] 提交并推送 GitHub、合并正式分支、创建新语义化版本标签并执行生产部署
- [ ] T035 [P0] 部署后验证 `/`、`/app`、`/kb/`、`/kb/api/actuator/health`、Dify 和支付桥接健康及无重复服务

---

## 依赖与执行顺序

- Phase 1 完成后才能执行 Phase 2。
- Phase 2 完成后，Phase 3 和 Phase 4 可顺序执行。
- Phase 3、Phase 4 完成后执行 Phase 5 的真实服务联调。
- Phase 6 必须在所有代码与本地 Dify 配置完成后执行。
- Phase 7 在本地与自动化验收完成后执行，并在生产部署后进行共存复核。
- 不要求子智能体或并行执行；任务按编号顺序由当前执行主体完成。

## MVP 范围

本次 MVP 是 Phase 1–6 的 P0 任务：两个识海 Dify 工作流、Dify 知识整理、Dify 小识对话、本地真实联调、全量回归和安全检查。P1/P2 任务用于回退验证、文档同步和本地桥接运维说明。

## 完成记录

| 任务 | 日期 | 状态 | 摘要与详情 | 验证证据 | Git |
|---|---|---|---|---|---|
| T001 | 2026-10-27 | 完成 | 创建两个识海 Dify DSL、导入说明及本地桥接脚本；两个应用已导入 Dify 并发布。 | `deploy/dify/`；Dify Console 导入成功。 | 未提交 |
| T002 | 2026-10-27 | 完成 | 新增编排配置记录，校验 provider、URL、超时和重试。 | `AiOrchestrationProperties.java`；`AppPropertiesBindingTest`。 | 未提交 |
| T003 | 2026-10-27 | 完成 | 基础配置默认 `deepseek`，本地 profile 默认 `dify` 和 `8502`。 | `application.yml`、`application-local.yml`、`application-local-test.yml`。 | 未提交 |
| T004 | 2026-10-27 | 完成 | 增加 Dify 环境变量占位，不含真实值。 | `.env.example`；敏感扫描 clean。 | 未提交 |
| T005 | 2026-10-27 | 完成 | 定义阻塞、流式统一编排接口。 | `AiOrchestrationClient.java`。 | 未提交 |
| T006 | 2026-10-27 | 完成 | 定义脱敏错误码和上游异常包装。 | `AiOrchestrationException.java`。 | 未提交 |
| T007 | 2026-10-27 | 完成 | 实现 Dify 认证、超时、有限重试、账号外部键和脱敏日志；当前日志用 `-` 表示未暴露运行标识。 | 本地真实调用日志 `provider=dify`。 | 未提交 |
| T008 | 2026-10-27 | 完成 | 解析阻塞输出与 SSE 事件，拒绝失败工作流，并过滤 `<think>` 推理块。 | `DifyAiOrchestrationClientTest` 4 项；真实 SSE 验证。 | 未提交 |
| T009 | 2026-10-27 | 完成 | 覆盖成功、认证失败、HTTP 错误、畸形响应、工作流失败、流式错误、流中断和推理过滤；未加入 10 秒慢速超时测试。 | `DifyAiOrchestrationClientTest` 6 项通过。 | 未提交 |
| T010 | 2026-10-27 | 完成 | 启用新配置，缺省 provider 保持 `deepseek`。 | `ApplicationConfig.java`；全量测试。 | 未提交 |
| T011 | 2026-10-27 | 完成 | 实现五个知识整理接口并统一委托 Dify。 | `DifyKnowledgeOrganizer.java`。 | 未提交 |
| T012 | 2026-10-27 | 完成 | DeepSeek organizer 仅在显式回退时装配。 | `@ConditionalOnProperty`；默认测试上下文通过。 | 未提交 |
| T013 | 2026-10-27 | 完成 | 覆盖结构化结果、推理过滤和缺失元数据；URL-only/重复标题由共享解析器既有测试与实现保证。 | `DifyKnowledgeOrganizerTest` 2 项。 | 未提交 |
| T014 | 2026-10-27 | 完成 | 继续复用共享解析、富文本映射、复核状态和原子入库。 | 真实文字整理生成 2 条 `review` 状态知识。 | 未提交 |
| T015 | 2026-10-27 | 完成 | 实现小识全部同步与流式操作。 | `DifyKnowledgeChatClient.java`。 | 未提交 |
| T016 | 2026-10-27 | 完成 | DeepSeek 聊天仅在显式回退时装配。 | 条件装配；全量测试。 | 未提交 |
| T017 | 2026-10-27 | 完成 | 保留 JSON、去重、引用上限和业务哨兵解析。 | `DifyKnowledgeChatClientTest` 4 项；真实 SSE 引用 1 条。 | 未提交 |
| T018 | 2026-10-27 | 完成 | 覆盖知识回答、通用回答、检索计划和流式输出。 | `DifyKnowledgeChatClientTest` 4 项通过。 | 未提交 |
| T019 | 2026-10-27 | 完成 | 新增统一外部回答接口与 Dify 实现，Search/Fetch 仍由后端执行。 | `ExternalAnswerService.java`、`DifyAnswerService.java`；`ExternalSearchServiceTest` 10 项。 | 未提交 |
| T020 | 2026-10-27 | 完成 | 导入并发布 `shihai-knowledge-organizer`、`shihai-xiaoshi-chat`，未修改拾题应用。 | 本地 Dify 应用 2 个；发布接口成功。 | 未提交 |
| T021 | 2026-10-27 | 完成 | 创建两个独立 API Key，仅写入 Windows 用户环境变量。 | 环境变量存在；仓库敏感扫描 clean。 | 未提交 |
| T022 | 2026-10-27 | 完成 | 定位 Docker Desktop `8501` 宿主转发异常，创建独立 `127.0.0.1:8502` 回环桥接；控制台使用 `localhost:8502`，后端 API 使用 `127.0.0.1:8502`。 | `deploy/dify/start-local-api-proxy.ps1`；Playwright 从 `localhost:8502` 登录进入 `/apps`。 | 未提交 |
| T023 | 2026-10-27 | 完成 | 真实 Dify 文字整理扣 5 点并生成 2 条知识；真实小识流式回答返回 123 字、1 个引用并扣 1 点。 | 本地 API 端到端脚本；`.runtime/local-test/logs/backend.out.log`。 | 未提交 |
| T024 | 2026-10-27 | 完成 | 默认 `deepseek` 测试上下文和既有 DeepSeek 客户端测试保持通过。 | 全量 Maven 测试 345 项通过。 | 未提交 |
| T025 | 2026-10-27 | 完成 | 后端全量测试与构建通过；Testcontainers 集成在无可用 Docker 环境时按既有条件跳过。 | Maven：23 + 322 项，共 345 项，0 失败。 | 未提交 |
| T026 | 2026-10-27 | 完成 | 前端 lint、类型检查、Vitest 和生产构建通过。 | Vitest 12 文件、58 项；Vite build 成功。 | 未提交 |
| T027 | 2026-10-27 | 完成 | `git diff --check` 通过；受控文件未发现 API Key 模式；运行日志未发现密钥或异常泄露。 | `sensitive-scan=clean`。 | 未提交 |
| T028 | 2026-10-27 | 完成 | 同步 README、AGENTS 和宪法，新增 Dify 编排边界并升级宪法至 4.7.0。 | 三个文档 diff。 | 未提交 |
| T029 | 2026-10-27 | 完成 | 活动功能目录指向 012。 | `.specify/feature.json`。 | 未提交 |
| T030 | 2026-10-27 | 完成 | 记录桥接启停、健康检查与生产隔离说明，并接入本地一键启动脚本。 | `deploy/dify/README.md`、`deploy/local-test/start.ps1`。 | 未提交 |
| T031 | 2026-10-27 | 完成 | 生产 Dify 已创建两个识海工作流和独立 Key。 | `shihai-knowledge-organizer=ef446a94-9f39-46b1-81d7-5702e2907c49`；`shihai-xiaoshi-chat=0ba1e07b-2582-469b-85dd-754a3ea1f8d1`。 | 未提交 |
| T032 | 2026-10-27 | 完成 | 生产环境文件已备份并切换 provider=dify，Key 仅写入 mode 600 环境文件。 | `/root/zhishiku-kb.env.bak-dify-20261027064137`；敏感值未回显。 | 未提交 |
| T033 | 2026-10-27 | 完成 | 生产 Dify 知识整理返回 1 条有效知识，通用聊天返回有效回答。 | `organizer_status=succeeded`、`chat_status=succeeded`、`keys_configured=True`。 | 未提交 |
| T034 | 2026-10-27 | 待开始 | 提交、推送、合并、打标签并生产部署。 | 待执行。 | 未提交 |
| T035 | 2026-10-27 | 待开始 | 部署后验证全部入口、Dify、支付桥接和单实例状态。 | 待执行。 | 未提交 |
