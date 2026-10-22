# AI 辅助采集统一规则验证记录

**日期**：2026-10-22

**功能**：`009-ai-assisted-capture`

**状态**：本地 P0 核心链路和视觉检查通过；P1 幂等、真实数据库回滚及生产验收待完成

**Git commit**：未提交

## 已通过

| 检查 | 结果 | 证据 |
|------|------|------|
| 手动直存边界 | 通过 | `KnowledgeServiceManualSourceTest` 验证普通创建和冲突另存副本都拒绝非手动来源 |
| AI 文字 | 通过 | `TextImportServiceTest` 3 项、`TextImportControllerTest` 1 项 |
| 文档图片 OCR | 通过 | `DocumentTextExtractorTest` 11 项、`DocumentImportServiceTest` 6 项 |
| 文档图片无附件 | 通过 | Service 测试断言正文不含 `image` 节点或 `assetId` |
| 互联网候选 AI 整理 | 通过 | `ExternalSearchServiceTest` 3 项验证授权、脱敏、AI 整理和计费门禁 |
| 来源策略 | 通过 | `SourceProvenancePolicyTest` 4 项；`V0008__ai_text_source.sql` 增加 `text` 来源 |
| 后端核心测试 | 18 项通过 | `kb-core` |
| 后端应用测试 | 175 项通过 | `kb-app` |
| 浏览器 worker | 1 项通过 | `kb-browser-worker` |
| Spotless | 通过 | `mvnw.cmd spotless:check` |
| 后端生产包 | 通过 | `kb-app` 和 `kb-browser-worker` jar 构建成功 |
| 前端 lint/typecheck | 通过 | `pnpm lint`、`pnpm typecheck` |
| Vitest | 47 项通过 | `pnpm test -- --run` |
| 前端生产构建 | 通过 | `pnpm build` |
| 应用 E2E | 34 项通过 | `e2e/app.spec.ts` |
| AI 采集五档视口 | 1 项通过 | `tests/e2e/ai-capture-policy-taste.spec.ts` |
| 采集移动 Taste | 4 项通过 | AI 文字、文档、截图和网页四条场景 |
| OpenAPI/YAML | 通过 | 主 OpenAPI、007、008、009 和 `application.yml` 解析成功 |
| 原型和部署脚本 | 通过 | `node --check prototype/app.js`、部署依赖/网络检查、`git diff --check` |

## 关键证据

- 手动录入仍调用普通知识创建接口；AI 文字调用 `/api/v1/text-imports`，E2E 断言普通创建请求为 0。
- 手动富文本 E2E 断言文本、网页、文档和截图四类 AI 导入请求均为 0。
- 文档入口 JPG/PNG/WebP 统一调用 `/api/v1/document-imports`，E2E 断言图片上传和普通创建请求均为 0。
- 互联网候选保存请求携带 `externalAiConsent=true`，响应为一条或多条 AI 整理知识。
- 文档图片、截图和互联网 Fetch 原文都不会直接写入知识正文。
- 失败、授权缺失、生产未计费或 AI 空结果时不创建原文或部分知识。

## Taste 结果

- 变更源文件 lint：`0 FAIL / 1 WARN`；WARN 为全文件既有 em dash 统计。
- App Screen inspector：`0 FAIL / 1 WARN`。
- 唯一 inspector WARN 是采集页 53% 标签文本为 11–12px；这是已批准的高密度 App Screen 标签层级，
  不是本次新增控件回归。
- 1440、768、390 截图已人工查看；320/430 由浏览器 E2E 覆盖，无横向溢出或操作按钮遮挡。

## 未完成或外部条件

- 本机没有 Docker，Testcontainers PostgreSQL 用例未启动。
- AI 文字、网页、文档和互联网候选尚未实现服务端幂等，网络重试仍可能重复调用 AI；截图入口已有
  10 分钟内存幂等。
- AI 生成的摘要、分类和标签当前写入备注，尚未迁移到独立分类/标签关系表，因此列表中的结构化
  分类字段仍为空。该能力属于既有后续任务，不在本次内容真实性边界内。
- “第二条知识写入失败后整次回滚”尚未在真实 PostgreSQL 执行，当前只有服务事务注解和
  AI 首次写入前失败测试；不得把本机结果解释为数据库事务验收通过。
- 生产真实 Tesseract、libvips、DeepSeek AI 文字/文档图片/互联网候选流程未验收。
- 002 点数报价、预留、结算和释放未接入；生产继续返回 `402 AI_BILLING_REQUIRED`。
- HEIC/HEIF 文档图片 OCR、多图合并和复杂公式属于 P2。
- 工作区尚未提交或部署；GitHub 一键部署不会包含本次实现，直到提交并发布。
