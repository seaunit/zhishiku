# 实施计划：网页截图 AI 拆分导入

**功能**：`006-webpage-ai-import`

**日期**：2026-10-22

**规格**：[spec.md](spec.md)

**上游功能**：
[001 个人知识库](../001-ai-personal-knowledge-base/plan.md)、
[003 移动网页](../003-mobile-web-access/spec.md)、
[004 富文本编辑](../004-rich-text-editor/plan.md)

## 摘要

网页链接入口由“保存 URL”改为同步单页整理流程：Spring Boot 校验 URL，交由独立 Playwright
Java worker 打开一次页面并截图；后端用 Tesseract OCR 读取可见文字；DeepSeek 将文字整理为
1–20 条结构化知识；Spring 在同一事务中调用现有 `KnowledgeService` 创建多条知识。原始 URL
仅进入来源信息，绝不作为正文。

## 技术背景

**前端**：Vue 3、TypeScript、Vite、vue-i18n、现有原型主题

**后端**：Java 17、Spring Boot 3.x、Spring Security、`RestClient`、Jackson

**浏览器工作进程**：独立 Playwright Java 可执行 jar，Chromium headless，无公共端口

**OCR**：Tesseract CLI，`chi_sim+eng`，截图经 `stdout` 读取后不落库

**AI**：DeepSeek Chat Completions JSON Object，环境变量提供 endpoint、model 和 API Key

**存储**：复用现有 PostgreSQL 知识表；本功能不新增迁移和持久化任务表

**测试**：JUnit 5、Mockito、Spring MVC Test、Vitest、Playwright E2E、部署脚本检查

**目标平台**：Ubuntu 24.04 服务端、桌面与 320–430 CSS 像素移动浏览器

## 宪法检查

| 门禁 | 设计前 | 设计后 | 证据 |
|------|--------|--------|------|
| SDD 与中文文档 | 通过 | 通过 | 规格、计划、研究、数据、契约、验证和任务均为中文 |
| 产品/开发/测试分离 | 通过 | 通过 | 产品冻结“不保存 URL-only”，开发实现，测试独立验收 |
| 默认中文、完整英文 | 通过 | 通过 | 网页入口、执行中和错误状态均有中英文文案 |
| 知识准确性与可追溯性 | 通过 | 通过 | 保存原始 URL、采集时间和 AI 整理正文 |
| Taste 与原型一致 | 通过 | 通过 | 原型同步为“一个页面拆成多条知识”，正式页保持原型主题 |
| 禁止爬虫与密钥安全 | 通过 | 通过 | 单 URL、不遍历、公开地址校验、AI Key 仅环境变量 |
| GitHub 一键部署 | 通过 | 通过 | 安装 Tesseract、Chromium、worker，固定 ref 部署 |
| 拾题零影响 | 通过 | 通过 | 仅 `/kb` 应用、独立进程、数据库和端口 |
| 任务完成可追溯 | 通过 | 通过 | 任务表含摘要、详情、验证与未提交状态 |

无需违反章程。

## 架构决策

1. **单页采集是硬边界。**
   worker 只接收一个初始 URL，不点击、不滚动、不发现链接，也不提供分页参数。

2. **截图是 OCR 输入，不是持久知识。**
   Playwright 使用 1440×1200 视口，页面最长截图 12,000 像素。截图和最终 URL 元数据均为
   临时文件，请求结束立即删除。

3. **初始 URL 和最终 URL 双重校验。**
   Spring 先拒绝凭据、非 HTTP/HTTPS、本机和私网地址。worker 对每个 HTTP/HTTPS 请求执行
   公开地址检查，并回传最终 URL；后端再次走同一 URL 策略和 DNS 校验。

4. **AI 输出只接受受限结构。**
   DeepSeek 必须返回 `items` 数组；后端校验 1–20 条、字段类型、长度、空正文和重复标题。
   任何整理失败都不产生部分知识。

5. **外部处理采用明确同意和密钥脱敏。**
   请求必须携带用户对 DeepSeek 处理的明确同意。OCR 文本在发送前扫描常见秘密并替换为
   `[REDACTED]`；未同意时在截图前拒绝，扫描或处理失败时不外发。原始 URL、最终 URL 和
   查询参数不得进入 AI 请求。

6. **多知识原子写入。**
   `WebPageImportService.importPage` 使用单事务依次调用 `KnowledgeService.createOrganized`。
   任一条失败时回滚整个网页导入，避免半套知识。

7. **来源与正文严格分离。**
   正文由 AI 整理的纯文本转换为现有 ProseMirror JSON 段落；来源字段保存原始 URL 和统一
   采集时间。标题、备注和正文都不能退化为 URL。

8. **复用现有知识模型。**
   MVP 不新增网页导入任务表。分类和标签暂写入知识备注中的 `AI 分类`、`AI 标签`，后续标签
   归一化功能再把它们迁移到独立关系表。

9. **成功整理使用 `succeeded` 状态。**
   网页导入只调用内部 `KnowledgeService.createOrganized`，避免复用普通创建的
   `not_requested` 状态。

10. **浏览器、OCR、AI 独立失败。**
   缺少 worker、Chromium、Tesseract、语言包或 API Key 时分别返回安全错误；不自动切换为
   直接 HTML 抓取，也不产生 URL-only 占位条目。

11. **部署在切换前验证依赖。**
   `provision-ubuntu.sh` 安装 Tesseract 中文语言包；`deploy.sh` 构建 worker、安装 Chromium、
   校验 `chi_sim` 与 `eng`，再切换 release。缺少依赖时保留上一版本。

## 项目结构

```text
backend/kb-app/src/main/java/site/seaunit/zhishiku/webimport/
backend/kb-app/src/test/java/site/seaunit/zhishiku/webimport/
backend/kb-browser-worker/src/main/java/site/seaunit/zhishiku/browser/WebPageCaptureWorker.java
frontend/kb-web/src/views/CaptureView.vue
frontend/kb-web/src/api/client.ts
frontend/kb-web/src/types/api.ts
frontend/kb-web/e2e/app.spec.ts
prototype/app.js
deploy/scripts/
deploy/systemd/zhishiku-api.service
specs/006-webpage-ai-import/contracts/web-import.openapi.yaml
```

## 接口与数据

- 接口契约见 [contracts/web-import.openapi.yaml](contracts/web-import.openapi.yaml)。
- 临时与持久实体见 [data-model.md](data-model.md)。
- `POST /kb/api/v1/web-imports` 要求认证和 CSRF；客户端同时发送 `Idempotency-Key` 作为重试
  关联键，服务端自动去重列入 P2。
- 成功返回 `201` 和 `items` 数组，每条沿用现有知识响应结构。
- 前端成功后进入 `/kb/library`，失败时保留输入 URL 并显示具体错误。

## 配置

```text
DEEPSEEK_API_KEY
DEEPSEEK_AI_ENDPOINT=https://api.deepseek.com/chat/completions
DEEPSEEK_MODEL=deepseek-chat
DEEPSEEK_AI_TIMEOUT_SECONDS=35
KB_BROWSER_WORKER_JAR=/opt/zhishiku/current/kb-browser-worker.jar
KB_BROWSER_TIMEOUT_SECONDS=35
KB_OCR_TIMEOUT_SECONDS=30
KB_WEB_IMPORT_MAX_TEXT_CHARS=30000
KB_WEB_IMPORT_MAX_ITEMS=20
PLAYWRIGHT_BROWSERS_PATH=/opt/zhishiku/playwright
```

所有密钥只通过 `/etc/zhishiku/kb.env` 注入，权限为 `600`。

## 实施顺序

### P0 MVP

1. 添加单页截图 worker 和最终 URL 元数据输出。
2. 添加公网 URL 校验、OCR、DeepSeek 结构化整理和多知识事务。
3. 添加认证、CSRF 的网页导入 API。
4. 前端网页入口改调导入 API，移除 URL 正文拼接。

### P1 安全与交付

1. 覆盖重定向最终 URL、私网子请求和 OCR 无文字失败。
2. 同步原型、中英文文案、SDD、README 和部署文档。
3. 安装 Chromium、Tesseract 中文包并执行部署自检。

### P2 后续加固

1. 增加每账号频率限制、浏览器并发队列和幂等去重。
2. 将分类和标签持久化到独立表。
3. 在生产真实页面基准上验证 OCR 质量、耗时和内存峰值。

## 验证策略

- JUnit：多知识映射、原始 URL、正文非 URL、AI 失败零写入、最终 URL 二次校验。
- MockMvc：未登录、缺少 CSRF、成功导入。
- 本地 HTTP 服务：DeepSeek Bearer、模型、JSON 模式和结果解析。
- Vitest/Playwright：网页模式调用 `/web-imports`，不调用知识 POST，成功后返回知识库。
- 部署检查：Tesseract、`chi_sim`、`eng`、worker、浏览器路径和原应用路由。
- 生产验收：真实公开页面截图 OCR、多条知识、URL-only 次数为 0、原应用零影响。

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| DNS 重绑定 | 初始和最终 URL 双重解析；worker 对每个请求拒绝非公开地址 |
| 截图和 OCR 耗时 | 独立进程超时、Nginx 120 秒、单并发部署和内存上限 |
| AI 生成 URL-only 正文 | 系统提示、后端空正文校验和负向测试 |
| AI 返回部分有效结果 | 整页整理先完成，再统一事务入库 |
| 页面为登录墙或 Canvas | 明确提示改用用户主动粘贴的截图入口 |
| Chromium/OCR 缺失 | 部署预检 fail closed，保留上一 release |
| 标签只存在备注文本 | P2 迁移到独立分类和标签关系表 |
| 重定向到私网 | 请求阶段阻断最终跳转，后端再次校验最终 URL |

## 复杂度跟踪

独立 browser worker 是隔离 Chromium 生命周期、内存和 SSRF 边界的必要组件，不引入公共端口。
OCR 使用系统进程，DeepSeek 使用现有 HTTP 客户端，没有新增消息队列、Redis、搜索服务或数据库表。
