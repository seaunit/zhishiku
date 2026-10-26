# 实施计划：Dify AI 编排

**分支**：`codex/dify-ai-orchestration` | **日期**：2026-10-27 | **规格**：[spec.md](spec.md)

**输入**：`specs/012-dify-ai-orchestration/spec.md`

## 摘要

在 Spring Boot 后端增加统一的 AI 编排边界。业务服务继续负责账号、知识、检索、授权、脱敏、计费、幂等和持久化；Dify 通过两个识海独立工作流承接知识整理与“小识”对话。现有 DeepSeek 直连实现保留为显式回退，本地默认使用 Dify。本次不部署生产。

## 技术背景

**语言与版本**：Java 17 LTS、Spring Boot 3.3.x、Vue 3、TypeScript、Dify 1.13.3

**主要依赖**：Spring `RestClient`/JDK `HttpClient`、Jackson、PostgreSQL 18、Flyway、Dify 工作流 API、DeepSeek Dify 插件

**存储**：沿用 PostgreSQL；本功能不新增业务表或数据库迁移

**测试**：JUnit 5、AssertJ、Mockito、临时 HTTP 服务、Maven Wrapper、前端 Vitest/Playwright

**目标平台**：本地 Windows 开发机；后端监听 `127.0.0.1:8082`，Dify 本地测试桥接监听 `127.0.0.1:8502`

**项目类型**：Web 应用

**性能目标**：阻塞式整理沿用现有超时上限；流式聊天首字节在 Dify 正常时与现有模型直连保持同一数量级；Dify 连接和总响应均设置硬超时。

**约束**：不改变现有 API 与 SSE 协议；不把真实密钥写入仓库；不让 Dify 接触业务数据库和点数；生产不自动切换到 Dify。

**规模**：两个识海 Dify 工作流、一个后端 AI 编排适配层、两种提供商配置、现有知识整理与聊天调用点。

## 宪法检查

*门禁：研究前检查，设计后复核。*

- **规格驱动**：规格、计划、契约和任务在同一功能目录生成。
- **角色阶段**：需求、计划、实现和验证由当前执行智能体顺序完成，不引入并行子智能体。
- **默认中文**：本功能全部 SDD 文档使用中文。
- **知识准确性与可追溯性**：Dify 输出必须由后端校验，引用仍来自真实候选。
- **外部数据与密钥安全**：只发送脱敏文本；密钥仅从环境变量读取；不复用拾题工作流。
- **账号与计费边界**：Dify 不拥有业务数据和点数；后端继续执行身份、授权、预留、结算和释放。
- **GitHub 与部署边界**：本次只做本地改造，不触发生产部署。
- **现有应用零影响**：不修改拾题容器、数据库、支付或工作流。
- **任务可追溯**：完成状态、验证证据和未提交状态写入 `tasks.md`。
- **支付与点数完整性**：AI 提供商替换不得绕过任何计费门禁。
- **原型与前端一致性**：本功能不改变用户界面，原型无需修改。

**结论**：通过，无宪法例外。

## 项目结构

### 本文档

```text
specs/012-dify-ai-orchestration/
├── spec.md
├── checklists/requirements.md
├── plan.md
├── research.md
├── data-model.md
├── contracts/
│   ├── ai-orchestration.schema.json
│   └── dify-workflows.md
├── quickstart.md
└── tasks.md
```

### 源代码

```text
backend/kb-app/src/main/java/site/seaunit/zhishiku/
├── ai/orchestration/
│   ├── AiOrchestrationClient.java
│   ├── AiOrchestrationException.java
│   ├── AiOrchestrationProperties.java
│   ├── AiOrchestrationRequest.java
│   ├── AiOrchestrationResult.java
│   ├── DifyAiOrchestrationClient.java
│   └── DeepSeekAiOrchestrationClient.java
├── chat/
│   ├── DeepSeekKnowledgeChatClient.java
│   └── DifyKnowledgeChatClient.java
├── search/external/
│   ├── DeepSeekAnswerService.java
│   └── DifyAnswerService.java
└── webimport/
    ├── DeepSeekKnowledgeOrganizer.java
    └── DifyKnowledgeOrganizer.java

backend/kb-app/src/main/resources/
├── application.yml
└── application-local.yml

backend/kb-app/src/test/java/site/seaunit/zhishiku/
├── ai/orchestration/
├── chat/
└── webimport/

deploy/dify/
├── shihai-knowledge-organizer.yml
├── shihai-xiaoshi-chat.yml
└── README.md
```

**结构决策**：采用模块化单体中的提供商适配层。业务服务面向 `KnowledgeOrganizer`、`KnowledgeChatModelClient` 和外部回答接口，不直接依赖 Dify。Dify 与 DeepSeek 实现由配置条件装配，避免在业务代码中写分支。

## 复杂度跟踪

本功能不引入新的运行时数据存储或业务服务。Dify 是外部 AI 编排依赖，新增一个本地受限桥接容器仅用于解决 Docker Desktop 宿主端口映射异常；生产部署不在本次范围内。

## 设计后宪法复核

- 所有契约均保持后端为业务事实源。
- 工作流只输出文本或受控 JSON，不包含数据库写入、账号身份或计费工具。
- 配置示例只写变量名，不写真实值。
- 不修改前端和原型，不改变用户可见业务流程。

**结论**：设计后通过，无例外。
