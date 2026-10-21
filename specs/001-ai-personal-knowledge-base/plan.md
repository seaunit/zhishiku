# 实施计划：AI 个人知识库

**功能**：`001-ai-personal-knowledge-base`

**日期**：2026-10-21

**规格**：[spec.md](spec.md)

**下游功能**：
[002 点数计费](../002-ai-credit-billing/plan.md)、
[003 移动网页](../003-mobile-web-access/spec.md)、
[004 富文本编辑](../004-rich-text-editor/plan.md)

## 摘要

构建单所有者、私有部署的 AI 个人知识库。支持手动录入、文档与图片文件、剪贴板截图和
用户明确提交的单页网页；系统自动生成摘要、分类和标签，用户纠正后锁定。首页只有一个
搜索框，个人知识优先，互联网搜索必须由用户再次确认。

实现采用 Vue 3 静态前端、Java 21 + Spring Boot 3.x 模块化后端、PostgreSQL 18 权威存储
和 S3 兼容私有对象存储。Web/API 与短后台任务共享领域代码但使用独立运行配置；Playwright
Java browser worker 独立隔离；Waffo 桥接继续使用小范围 Node.js 进程，只处理支付 SDK、
验签和查询，不接触知识数据。

AI 与互联网搜索通过 DeepSeek Provider 适配器接入。所有付费调用必须先取得 002 的服务端
报价、确认、预留和任务标识；余额不足、支付不可用或授权缺失时，内容仍可保存并进入待整理。

## 技术背景

**前端**：Vue 3、TypeScript、Vite、Vue Router、Pinia、TanStack Vue Query、vue-i18n、
Tiptap Vue 3、Reka UI、Lucide Vue

**后端**：Java 21 LTS、Spring Boot 3.x 稳定版本、Maven Wrapper、Spring Security、
Spring Session JDBC、Spring Data JPA、Spring `JdbcClient`、Springdoc OpenAPI、
Spring Actuator、Micrometer

**存储**：PostgreSQL 18 保存权威业务数据和 PostgreSQL-first 搜索投影；S3 兼容对象存储
保存附件和脱敏派生图；不引入 Redis 或消息总线

**任务**：PostgreSQL `processing_job`/`capture_job` 任务表、事务租约和
`FOR UPDATE SKIP LOCKED`；Spring Scheduler 触发轮询和周期任务

**平台工具**：Apache Tika、PDFBox、Tesseract CLI、libvips CLI、Playwright Java、
AWS SDK for Java v2

**测试**：JUnit 5、AssertJ、ArchUnit、Testcontainers、RestAssured、WireMock、
Vitest、Vue Test Utils、Playwright、axe-core、k6、Gitleaks、Trivy、
OWASP Dependency-Check

**部署**：Ubuntu 24.04、原生 Nginx 与 systemd；Vue 构建为静态文件；Spring Boot API
绑定回环端口；browser worker 不开放公共端口；Waffo Node 桥接绑定
`127.0.0.1:8091`

**性能目标**：100,000 条知识基准下，95% 查询在 2 秒内显示至少 20 条个人结果；
2 个浏览器会话和 2 个后台任务为正式并发

## 宪法检查

| 门禁 | 设计前 | 设计后 | 证据 |
|------|--------|--------|------|
| SDD 与中文文档 | 通过 | 通过 | 规格、研究、计划、数据、契约和任务均先更新再实施 |
| 产品/开发/测试分离 | 通过 | 通过 | 产品冻结范围和验收，开发实现，测试独立验收 |
| 默认中文、完整英文 | 通过 | 通过 | vue-i18n 默认 `zh-CN`，服务端消息同样支持中英文 |
| 准确性与可追溯 | 通过 | 通过 | 来源、授权、任务、附件、修订和审计实体保留 |
| Taste 视觉验证 | 通过 | 通过 | Vue 页面在 1440/768/390/360 真实浏览器检查 |
| 禁止爬虫与密钥安全 | 通过 | 通过 | 单页 Playwright worker、SSRF 门禁、环境变量和密钥扫描 |
| GitHub 一键部署 | 通过 | 通过 | Maven/Vite 构建、Flyway、systemd、Nginx 和回滚均来自仓库 |
| 拾题零影响 | 通过 | 通过 | 独立端口、数据库、Cookie、进程、环境文件和 `/kb` 路由 |
| 任务可追溯 | 通过 | 通过 | 每项任务保留摘要、证据和提交 SHA |
| 支付与点数完整性 | 通过 | 通过 | Spring 事务账本、Node 桥接、002 报价与预留门禁 |

无需违反章程。

## 架构决策

1. **Vue 静态前端与 Spring Boot API 分离。**
   Vue 由 Nginx 直接提供，不运行 Node Web 服务器。API/Worker 使用同一 Maven 领域代码，
   避免领域规则重复。

2. **PostgreSQL 为唯一权威数据源。**
   PostgreSQL 同时保存业务实体、任务租约和搜索投影。Meilisearch 不再是 MVP 硬依赖；
   只有冻结的中文搜索基准不达标时才启用可选派生索引。

3. **JPA 与 `JdbcClient` 分工。**
   JPA 负责普通 CRUD、关联和乐观锁；搜索、任务租用、账本、批次分配和复杂生命周期更新
   使用明确 SQL。不同时维护 JPA、MyBatis 和 jOOQ 三套持久层。

4. **Flyway 是 schema 唯一迁移入口。**
   DDL、索引、部分唯一约束、JSONB 检查、触发器和扩展安装均进入版本化迁移。生产部署
   先备份、再迁移、再健康检查，失败回滚。

5. **Spring Security 使用数据库会话。**
   一次性 bootstrap token 创建唯一所有者，此后关闭公开注册。会话 Cookie 独立命名为
   `zhishiku_session`，路径为 `/kb`，启用 CSRF、SameSite 和安全属性。

6. **PostgreSQL 任务表替代 pg-boss。**
   Worker 使用事务租约、`SKIP LOCKED`、尝试次数、退避时间、幂等键和死信状态处理任务。
   不引入 Redis、RabbitMQ 或 Kafka。

7. **单页浏览器隔离。**
   独立 Playwright Java worker 只打开用户提交的一个 URL；可加载渲染所需子资源，但不得
   发现链接、遍历站点、预取、无限滚动或保持持久会话。

8. **平台工具负责不可信文件处理。**
   Tika/PDFBox 提取文档，Tesseract 执行 OCR，libvips 处理图片方向、HEIC、缩放和元数据
   清理。Java 负责调用边界、超时、资源限制、扫描和审计。

9. **Waffo 桥接保留 Node。**
   现有 Pancake SDK 和参考实现属于 TypeScript。桥接只处理支付字段，减少 Java 重写导致
   验签和查询语义变化；桥接不拥有业务数据库，不读取知识正文。

10. **API 契约优先。**
    OpenAPI 保持语言中立，Springdoc 校验服务端实现，Vue 客户端由契约生成或经过契约测试；
    桥接 OpenAPI 继续约束 Node 与 Spring Boot 的内部调用。

## 项目结构

```text
backend/
├── pom.xml
├── kb-core/
│   └── src/main/java/site/seaunit/zhishiku/core/
├── kb-app/
│   ├── src/main/java/site/seaunit/zhishiku/
│   ├── src/main/resources/db/migration/
│   └── src/test/java/site/seaunit/zhishiku/
└── kb-browser-worker/
    ├── pom.xml
    └── src/main/java/site/seaunit/zhishiku/browser/

frontend/
└── kb-web/
    ├── package.json
    ├── pnpm-lock.yaml
    └── src/

bridges/
└── waffo/
    ├── package.json
    └── src/

contracts/
├── 001-openapi.yaml
├── 002-openapi.yaml
└── waffo-bridge.yaml

deploy/
├── nginx/
├── systemd/
├── scripts/
└── env/

tests/
├── fixtures/
├── performance/
├── security/
└── e2e/
```

`kb-app` 通过 Spring profile 运行 API 或 Worker 角色。MVP 可在同一 JVM 运行短任务，
browser worker 和 Waffo 桥接始终独立。

## 数据与接口

- 实体、字段、索引和状态机见 [data-model.md](data-model.md)。
- 对外 HTTP 契约见 [contracts/openapi.yaml](contracts/openapi.yaml)。
- 所有 `/kb/api/**` 请求进入 Spring Boot；Vue 只调用公开 API，不直接访问数据库或 Provider。
- 搜索先使用 PostgreSQL `search_document` 投影和 `pg_trgm`。查询字段包含标题、可见正文、
  备注、分类和标签，响应返回匹配原因和高亮。
- 个人搜索与互联网搜索保持分离；互联网搜索请求必须显式同意并引用 002 计费任务。
- 互联网候选保存在 PostgreSQL 短期表中，默认 15 分钟过期；确认保存前不创建知识条目。
- 保存知识、来源、搜索修订和任务入队尽量在同一事务完成；索引和 AI 处理异步执行。
- 附件先进入暂存对象存储，完成 MIME 检测、扫描、OCR/脱敏后才能提升为正式附件。

## 搜索与性能

- PostgreSQL 迁移启用 `pg_trgm`，为标题、搜索投影、分类和标签建立 GIN/局部索引。
- 维护派生 `search_text`，只包含可搜索的已脱敏文字、分类、标签和图片说明。
- 查询使用受控权重：
  `title > category/tag > summary > body/note > image text`。
- 对一至两个汉字的短查询保留有界 SQL 回退，并纳入基准；不得因无索引造成无上限扫描。
- 若 100,000 条基准未达到 95%/2 秒，产品与测试评估是否启用 Meilisearch；不得在未达标时
  静默降低验收目标。

## 部署共存

- 现有拾题继续使用 `8080`、支付侧车 `8090`、MySQL、Redis 和现有 Cookie。
- 识海 Spring Boot API 建议绑定 `127.0.0.1:8082`；部署预检必须确认端口空闲。
- Vue 静态资源由 Nginx 提供：`/kb/`、`/kb/assets/`。
- `/kb/api/**` 代理到 Spring Boot，`/kb/pay/webhooks/waffo` 代理到 Node 桥接。
- browser worker 不开放端口；API 和 Worker 使用独立 systemd unit 或同一 JAR 的不同 profile。
- PostgreSQL、S3/MinIO、环境文件、日志、Cookie、数据库和备份目录均与拾题隔离。
- 当前主机约 1.8 GiB 可用；完整 PostgreSQL + Spring Boot + libvips + Chromium + 对象存储
  仍需要独立主机、扩容或严格资源限制。未经容量验证不得部署完整栈。

详细基线与回滚见 [deployment.md](deployment.md)。

## 实施顺序

1. **PoC 门禁**：PostgreSQL 中文搜索、Playwright Java 单页截图、libvips HEIC、
   Spring Boot 与 Node Waffo 桥接内部鉴权。
2. **工程基础**：Maven 多模块、Vue/Vite、Flyway、环境变量校验、Spring Security、
   会话和健康端点。
3. **领域与数据**：知识、来源、附件、分类、标签、任务、授权、审计和回收站。
4. **核心采集**：手动 CRUD、文档/图片上传、剪贴板截图、安全扫描、即时可搜索。
5. **任务与 AI**：PostgreSQL 任务租约、DeepSeek Provider、分类、重试和计费门禁。
6. **搜索**：PostgreSQL 投影、索引、排序、分页、回退和互联网候选交接。
7. **Browser worker**：Playwright Java、单页限制、SSRF 代理和截图回退。
8. **UI**：Vue 页面、双语、富文本和移动端，使用 Taste 真实验证。
9. **发布**：备份恢复、零影响、计费、安全、性能和 GitHub 固定 ref 一键部署。

## 验证策略

- Java 单元测试：状态机、权限、任务租约、幂等、分类锁定、保留期和错误映射。
- Vue 单元测试：组件状态、表单验证、路由和双语消息。
- 集成测试：Testcontainers 启动 PostgreSQL、MinIO 和 WireMock，验证事务、搜索和附件。
- 契约测试：RestAssured/JSON Schema 验证 001/002 OpenAPI；Node 桥接使用同源契约。
- E2E：Playwright 验证登录、采集、搜索、授权、计费、回收站和双语。
- 安全测试：SSRF、危险 HTML、密钥金丝雀、跨环境支付、日志和对象存储扫描。
- 性能测试：冻结语料、500 条查询、cold/warm 条件和真实结果卡片渲染。
- 部署测试：固定 commit/tag、Nginx 增量、迁移回滚和拾题零影响基线。

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| PostgreSQL 中文搜索不达标 | 冻结基准；`pg_trgm` 与短查询回退；必要时启用可选 Meilisearch |
| Spring Boot 内存高于 Node | 独立主机或扩容；JVM 上限、单浏览器并发和资源监控 |
| Playwright Java 与浏览器版本不匹配 | 固定 Playwright 与 Chromium 版本，browser worker 独立发布 |
| Tika/OCR/libvips 原生依赖缺失 | 启动检查、版本探测、容器/主机包清单和失败关闭 |
| Node 桥接与 Java 内部契约漂移 | 固定 OpenAPI、共享密钥轮换、WireMock 契约和部署同版本验证 |
| JPA 复杂查询难以优化 | JPA 只做 CRUD；搜索、任务和账本使用 `JdbcClient` 明确 SQL |
| 迁移和搜索投影不一致 | Flyway、同事务修订号、重建命令和恢复演练 |
| 与拾题共存影响原应用 | 独立端口、数据库、Cookie、进程和环境文件；部署前后零影响门禁 |

## 复杂度跟踪

保留独立 browser worker 和 Node Waffo 桥接是为了安全边界，而不是微服务化。MVP 不强制
Meilisearch、Redis 或消息总线。所有新增进程和原生依赖都必须由安全、搜索或支付门禁证明，
否则回退到更小的部署形态。
