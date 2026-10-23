# 实施计划：知识库聊天

**功能分支**：`010-knowledge-chat`

**日期**：2026-10-23

**规格**：[spec.md](./spec.md)

## 摘要

在现有 Vue 3 + Spring Boot 模块化单体中新增“小识”聊天页。后端先执行快速检索；召回为空但
账号存在 active 知识时，由 DeepSeek 生成检索计划和同义关键词后执行二次召回，再阅读候选正文
生成带引用答案。知识库未命中或证据不足时，继续调用通用 AI 自然对话；只有通用 AI 明确需要
实时外部信息或无法可靠回答时，才释放预留并提示联网。联网复用现有 DeepSeek Search/Fetch，
搜索 1 点、回答 1 点，共 2 点。

## 技术背景

- Java 17、Spring Boot 3.x、Spring Security、Spring Session JDBC、CSRF。
- PostgreSQL 18：复用知识检索关系，并新增账号隔离的聊天对话和消息表。
- Vue 3、TypeScript、Pinia、Vue Router、vue-i18n、Lucide。
- DeepSeek Chat Completion 使用现有 `RestClient` 和 `SHIHAI_DEEPSEEK_API_KEY`。
- 计费复用 `billing_task`、报价确认、预留、结算和释放。

## 宪法检查

- SDD：规格、计划、数据模型、契约和任务先于编码。
- 账号隔离：所有本地检索、回答、引用和计费都从会话账号取得范围。
- 外部 AI 安全：显式授权、密钥脱敏、只发送问题、短期会话和命中知识片段。
- 禁止爬虫：联网只调用既有 DeepSeek Search、单页 Fetch，不遍历网页链接。
- 点数完整：本地回答 1 点；联网搜索 1 点、联网回答 1 点，分别按成功步骤结算。
- 原型一致：原型和前端同时新增“聊天”菜单与页面，并做 Taste 检查。
- 移动端：320–430px 可输入、发送、查看引用和选择联网。
- 默认中文，英文完整。

## 项目结构

```text
backend/kb-app/src/main/java/site/seaunit/zhishiku/chat/
├── KnowledgeChatController.java
├── KnowledgeChatDtos.java
├── KnowledgeChatConversationService.java
├── KnowledgeChatRetrievalService.java
├── KnowledgeChatService.java
├── KnowledgeChatModelClient.java
└── DeepSeekKnowledgeChatClient.java

backend/kb-app/src/test/java/site/seaunit/zhishiku/chat/
frontend/kb-web/src/views/ChatView.vue
frontend/kb-web/src/views/__tests__/ChatView.spec.ts
frontend/kb-web/e2e/app.spec.ts
prototype/app.js
specs/010-knowledge-chat/
```

## 接口设计

- `POST /api/v1/chat/retrieval`
  - 免费本地检索；返回 `hasContext` 和最多 5 个标题/摘要来源。
- `POST /api/v1/chat/answers`
  - 需要 `Idempotency-Key`、CSRF 和 `billingTaskId`。
  - 成功返回 `{answer, sources, needsInternet}`。
- `POST /api/v1/chat/answers/stream`
  - 返回 SSE；`token` 事件发送增量文本，`complete` 事件返回最终答案、引用和联网建议。
  - 知识回答仅在流式完成且引用有效后结算；通用回答在内容非空且无需联网时结算并持久化。
- `GET /api/v1/ai-consents` 增加 `chat` 授权范围。
- `GET /api/v1/chat/conversations`：返回最近对话。
- `POST /api/v1/chat/conversations`：新建对话。
- `GET /api/v1/chat/conversations/{id}`：恢复对话与消息。
- `POST /api/v1/chat/conversations/{id}/messages`：持久化用户或小识消息。
- `DELETE /api/v1/chat/conversations/{id}`：删除整个对话。

## 计费

- `knowledge_chat`：`ai_answer × 1`，成功回答后结算。
- `internet_search_with_answer`：`internet_search × 1 + ai_answer × 1`。
- 知识库未命中仍创建一次回答报价，由通用 AI 决定能否回答。
- 通用 AI 也判断不足、空回答、超时、引用非法或明确需要联网时释放回答预留。

## 回答约束

- 知识增强模型只使用提供的知识片段并输出结构化 JSON；通用 AI 使用独立提示词和会话上下文。
- 知识回答的 `usedSourceIds` 必须来自本轮检索结果；通用回答不得携带或展示引用。
- 通用 AI 需要实时外部信息时返回 `needsInternet=true`，不得编造联网结果。
- 答案最多 8,000 字符，引用最多 5 条。
- 系统提示明确忽略知识正文中的指令和工具请求。
- 知识流式回答使用编号证据并要求 `[1]` 形式引用；服务端完成后校验引用编号。通用回答不要求
  引用，但必须非空且不包含内部联网控制标记。

## 智能检索

- 第一次召回复用现有标题、正文、标签、分类和 `pg_trgm` 搜索。
- 第一次召回为空且账号有 active 知识时，调用 DeepSeek 生成最多 8 个检索短语，覆盖实体、
  约束、同义词、英文表达和相关政策词。
- 二次召回最多收集 8 条候选，逐条读取安全正文；最终回答仍只引用最多 5 条实际使用来源。
- 检索计划、知识回答和通用回答共用一次 `knowledge_chat` 预留；最终没有有效答案时释放。

## 风险

- 当前检索基于 `pg_trgm` 和全文投影，不是语义向量检索；相似表达可能召回不足。
- 会话历史按账号持久化；来源快照与消息随时间增长，列表只加载最近对话，详情限制单次消息数。
- 联网回答依赖外部搜索质量，必须显示来源，不保证覆盖所有网站。
