# 实施计划：一键全部复核

**分支**：`codex/dify-ai-orchestration` | **日期**：2026-10-27 | **规格**：[spec.md](spec.md)

**输入**：`specs/013-bulk-review/spec.md`

## 摘要

在待处理页标题区增加“全部复核 N 条”按钮。用户确认后调用单个服务端批量接口，将当前账号全部 `active + review` 知识改为 `succeeded`。前端刷新队列与导航计数，原型、中英文文案和移动端状态同步更新。

## 技术背景

**语言与版本**：Java 17 LTS、Spring Boot 3.3.x、Vue 3、TypeScript

**主要依赖**：Spring Data JPA、PostgreSQL、Vue Router、Vue I18n、Lucide Vue、Playwright

**存储**：沿用 `knowledge_item`，不新增表或迁移

**测试**：JUnit 5、MockMvc、Mockito、Vitest、Playwright

**目标平台**：桌面与移动 Web

**项目类型**：Web 应用

**性能目标**：一次服务端批量更新替代 N 次单条请求；100 条待复核知识的操作不产生 100 次浏览器请求。

**约束**：只处理当前账号；只处理 `review`；单事务；不扣点；不修改正文。

**规模**：一个后端接口、一个服务方法、一个前端按钮和确认框、原型同步。

## 宪法检查

*门禁：研究前检查，设计后复核。*

- **规格驱动**：规格、计划、数据模型、契约、任务和验证记录完整。
- **默认中文双语**：中文优先，英文同步。
- **账号隔离**：查询和更新都以认证账号为边界。
- **知识准确性**：不修改正文、摘要、分类和标签。
- **Taste 视觉验证**：沿用现有深海主题，更新原型并在 1440/768/390 检查。
- **无爬虫、密钥和计费变更**：本功能不涉及。
- **任务可追溯**：完成后逐项勾选并记录验证证据。

**结论**：通过，无例外。

## 项目结构

```text
backend/kb-app/src/main/java/site/seaunit/zhishiku/knowledge/
├── KnowledgeController.java
├── KnowledgeDtos.java
├── KnowledgeItemRepository.java
└── KnowledgeService.java

frontend/kb-web/src/
├── api/client.ts
├── components/ConfirmDialog.vue
├── i18n.ts
├── prototype-theme.css
├── types/api.ts
└── views/ProcessingView.vue

prototype/
├── index.html
├── app.js
└── styles.css

specs/013-bulk-review/
├── spec.md
├── checklists/requirements.md
├── plan.md
├── research.md
├── data-model.md
├── contracts/bulk-review.openapi.yaml
├── quickstart.md
└── tasks.md
```

**结构决策**：后端在既有知识模块内增加批量状态操作；前端复用待处理页和通用确认框。选择服务端按账号批量查询并锁定，而不是由前端逐条调用。

## 复杂度跟踪

无新增服务或依赖。批量处理增加一个带悲观锁的仓库查询，以换取账号一致性、事务性和并发安全。

## 设计后宪法复核

- 账号边界、事务与状态规则明确。
- 原型和前端同步修改。
- 不使用子智能体或并行执行主体。

**结论**：通过，无例外。
