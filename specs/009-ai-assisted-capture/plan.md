# 实施计划：AI 辅助采集统一规则

**分支**：`009-ai-assisted-capture` | **日期**：2026-10-22

**规格**：[spec.md](spec.md)

**输入**：`specs/009-ai-assisted-capture/spec.md`

## 摘要

本计划把“只有手动录入可以直存，其他采集方式必须 AI 整理”固化为服务端边界。现有网页、文档
文本和剪贴板截图已经具备 AI 整理链路；需要补齐独立 AI 文字入口、文档入口图片 OCR、互联网
搜索候选的 AI 整理，以及普通知识创建接口对非手动来源的防绕过校验。实现复用 Spring Boot、
Tesseract、libvips、DeepSeek 和现有事务边界，不新增依赖或外部服务。

## 技术上下文

**语言与版本**：Java 17、TypeScript、Vue 3。

**核心依赖**：Spring Boot 3.3、Spring Security、Apache Tika、Tesseract、libvips、
DeepSeek API、Pinia、Vue Router、Playwright。

**存储**：PostgreSQL 保存最终知识、处理状态和来源元数据；原始文档、截图和 OCR 全文不进入
对象存储。

**测试**：JUnit 5、Mockito、MockMvc、Vitest、Playwright、Taste 浏览器检查。

**目标平台**：桌面、平板和 320–430 CSS 像素移动浏览器。

**项目类型**：Vue + Spring Boot 的模块化单体 Web 应用，包含独立 browser worker 和支付侧车。

**性能目标**：沿用现有 OCR、AI 和解析超时；非手动失败必须在单个请求或既有任务边界内返回，
不得留下部分知识。

**约束**：不使用爬虫；不新增长期保存原文件；不绕过 002 计费门禁；不得影响手动录入可用性。

**范围**：统一采集规则、AI 文字入口、文档图片 OCR、互联网候选 AI 整理、服务端防绕过、
前端文案和回归测试。

## 宪法检查

*门禁：Phase 0 前通过，Phase 1 后复核。*

| 原则 | 检查 | 结论 |
|------|------|------|
| I 规格驱动开发 | 已完成 009 规格，本计划与任务随后生成 | 通过 |
| II 多子智能体协作 | 产品先评审，开发实现，测试独立验收 | 通过 |
| III 默认中文双语交付 | SDD 使用中文，界面补齐中英文文案 | 通过 |
| IV 知识准确性与可追溯性 | 保留来源类型、采集时间和 AI 整理状态 | 通过 |
| V Taste 视觉验证 | 采集页按 App Screen 规则执行真实浏览器检查 | 通过 |
| VI 外部数据与密钥安全 | 不使用爬虫；外部 AI 前脱敏；密钥只读环境变量 | 通过 |
| VII GitHub 一键部署 | 不改变已有 GitHub 部署入口，不新增手工服务器修改 | 通过 |
| VIII 现有应用零影响 | 本功能只修改 `/kb` 应用逻辑，不触碰原应用 | 通过 |
| IX 任务完成可追溯 | 009 任务表记录实现、验证和未提交状态 | 通过 |
| X 支付与点数完整性 | 所有非手动 AI 入口继续受生产计费门禁保护 | 通过 |
| XI 原型与前端一致性 | 原型、前端文案和文档同步修改并截图核对 | 通过 |

## 项目结构

### 本功能文档

```text
specs/009-ai-assisted-capture/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── capture-policy.openapi.yaml
├── checklists/
│   └── requirements.md
├── spec.md
└── tasks.md
```

### 受影响源码

```text
backend/kb-app/src/main/java/site/seaunit/zhishiku/
├── documentimport/
├── knowledge/
├── ocr/
├── search/external/
├── textimport/
└── webimport/

backend/kb-app/src/test/java/site/seaunit/zhishiku/
├── documentimport/
├── knowledge/
├── search/external/
└── textimport/

frontend/kb-web/
├── src/views/CaptureView.vue
├── src/views/HomeView.vue
├── src/api/client.ts
├── src/types/api.ts
└── e2e/app.spec.ts

prototype/
├── app.js
└── README.md
```

**结构决策**：沿用现有领域包，不创建新服务。文档图片 OCR 扩展
`DocumentTextExtractor`；互联网候选复用 `DeepSeekKnowledgeOrganizer` 并新增结果类型；
防绕过校验放在知识创建服务入口。

## 实施阶段

### Phase 0：研究与口径统一

1. 确认“手动录入”是唯一直存入口。
2. 枚举所有非手动入口和现有绕过路径。
3. 确认文档图片处理为临时 OCR，不转成附件。
4. 确认互联网候选通过 Fetch 取得内容后仍必须 AI 整理。
5. 确认生产计费门禁和外部 AI 授权仍是前置条件。

### Phase 1：设计与契约

1. 扩展文档导入契约，接受 JPG/PNG/WebP 并保持 `source.kind=document`。
2. 增加 AI 文字导入契约，要求 `externalAiConsent` 并返回一条或多条知识。
3. 调整互联网候选保存契约，要求 `externalAiConsent` 并返回一条或多条知识。
4. 在普通知识创建契约中明确仅用于手动录入，拒绝 `document`、`screenshot` 和
   `internet_result` 来源。
5. 更新主 OpenAPI、007 契约、AGENTS、README 和相关 SDD。

### Phase 2：实现

1. AI 文字：长度校验、密钥脱敏、AI 整理、多知识事务入库。
2. 文档图片：安全检查、libvips 规范化、Tesseract OCR、AI 整理、事务入库、临时文件删除。
3. 互联网候选：授权校验、计费门禁、Fetch、脱敏、AI 整理、多知识事务入库。
4. 手动入口：保留直接保存，拒绝可被外部采集接口复用的来源类型。
5. 前端：增加 AI 文字入口；文档图片与文档文本统一调用 `document-imports`；互联网按钮改为
   AI 整理后收入。
6. 原型：同步 AI 文字、文档图片 AI 识别和互联网候选 AI 整理语义。

### Phase 3：验证

1. 后端定向测试覆盖 AI 文字、文档图片、互联网候选、手动入口防绕过和失败关闭。
2. 后端全量测试、Spotless 和构建。
3. 前端 lint、typecheck、Vitest、E2E 和生产构建。
4. Taste 检查 1440、768、430、390、320 视口。
5. 记录未完成的生产真实 OCR、DeepSeek、计费与部署验收。

## 复杂度跟踪

本功能不新增服务、数据库表或运行时依赖。主要复杂度来自三类既有导入链路需要统一行为，
通过复用现有 OCR、脱敏、整理器和事务服务吸收。
