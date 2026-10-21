# 富文本维护手册

**功能**：`004-rich-text-editor`

**更新日期**：2026-10-22

## 事实源

维护顺序固定为：

1. `specs/004-rich-text-editor/contracts/rich-text.schema.json`
2. `specs/004-rich-text-editor/data-model.md`
3. `tests/fixtures/rich-text/manifest.json`
4. Vue/Java 类型与验证器
5. Flyway 迁移和派生搜索投影
6. 安全、E2E、性能与验收文档

任何新节点、属性或图片来源都必须先更新规格和契约，再更新双端代码与夹具。不得只在编辑器内
允许新格式而让 Java 或详情页拒绝。

## Schema 演进

| 变更 | 必做步骤 | 禁止 |
| --- | --- | --- |
| 新增节点或 mark | 更新 JSON Schema、领域验证器、双端序列化、夹具和纯文本投影 | 仅前端放行 |
| 新增属性 | 默认值、未知属性拒绝、旧文档兼容、安全清洗 | 透传任意 HTML 属性 |
| 删除节点 | 先迁移或降级旧文档，再移除读写支持 | 直接让旧详情空白 |
| 修改图片协议 | 更新网络白名单、来源记录和负向语料 | 接受 `data:`/`file:` 作为正文图 |
| 修改字符/图片上限 | 同步数据库约束、领域验证、报价和提示文案 | 静默截断 |

每次变更后执行：

```powershell
node tests/fixtures/rich-text/validate-fixtures.mjs
node tests/fixtures/rich-text/generate-security-corpus.mjs --check
pnpm --dir frontend/kb-web exec playwright test --config ../../tests/playwright.config.ts --workers=1
```

Java 侧还应运行仓库现有 Maven 单元、契约和集成测试。

## 迁移与回滚

### 上线前

1. 冻结 `rich-text.schema.json` 和域验证器版本。
2. 对数据库做可恢复快照，并记录迁移版本。
3. 在独立测试数据库执行 001→002→004 的顺序升级和重复执行。
4. 抽样旧纯文本、空行、长链接、代码和危险 HTML。
5. 验证搜索投影、图片引用、来源记录和转换报告。

### 回滚

1. 停止富文本写入和迁移 worker。
2. 恢复应用和 Nginx 的上一固定 release。
3. 若迁移已改写正文，按迁移状态表逐条恢复原纯文本字段，不直接删除审计记录。
4. 回滚对象提升和临时附件前，确认未被其他知识引用。
5. 重新建立搜索投影，执行旧详情只读检查。

不能把“应用回滚成功”等同于“数据回滚完成”。数据修复必须单独留证。

## 依赖升级

重点依赖包括 Tiptap Vue 3、Playwright、Spring Boot、networknt JSON Schema、PostgreSQL、Tika、
libvips、Tesseract 和 Waffo sidecar。

升级前检查：

- Tiptap schema 或粘贴行为是否变化。
- Playwright 是否仍可按 Chromium 固定视口采集。
- Java JSON Schema 与浏览器校验结果是否一致。
- PostgreSQL 全文/`pg_trgm` 查询计划是否回退。
- 图片解码与 EXIF 清理是否引入新格式或安全变更。
- 支付 sidecar 是否仍只读独立健康端点。

升级后至少执行夹具自检、默认 Playwright 套件、Java 契约/集成测试和性能预算。

## 安全巡检

- 100 条安全语料必须保持稳定生成。
- 远程、私网、凭据 URL 和非 HTTP(S) 图像来源必须拒绝或降级为不可执行文本/安全链接。
- 详情只从 `/kb/api/v1/attachments/{uuid}/content` 获取图片，并转成短期 blob URL。
- 正文、日志、索引、来源记录和对象元数据不得出现 Cookie、Authorization 或完整剪贴板。
- AI 仅接收纯文本投影和明确授权的图片。

## 故障处理

| 现象 | 首先检查 | 禁止操作 |
| --- | --- | --- |
| 详情显示格式错误 | 域验证结果、schema version、迁移状态 | 用 `v-html` 直接渲染 |
| 图片一直占位 | 附件权限、content-type、25 MiB 限制 | 改成远程直链 |
| 粘贴后文字丢失 | 转换报告、纯文本回退 | 静默重试覆盖 |
| 移动端横向滚动 | 表格 frame、代码块、超长 URL | 给 `body` 加 `overflow-x:hidden` 掩盖 |
| 保存冲突 | `expectedVersion` 与本地草稿 | 强制覆盖 |
| 搜索旧内容 | 投影更新时间、索引版本 | 查询原始 JSON 标记 |

## 证据位置

- 夹具：`tests/fixtures/rich-text/`
- 安全与网络：`tests/security/`
- 无障碍：`tests/accessibility/richtext.spec.ts`
- 多视口和零影响：`tests/e2e/`
- 性能：`tests/performance/richtext/`
- FR/SC 验收：`artifacts/acceptance/rich-text.md`
- quickstart：`artifacts/rich-text/quickstart-report.md`
- 可用性协议：`tests/usability/richtext.md`
