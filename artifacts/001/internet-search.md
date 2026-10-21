# 联网搜索验收

**日期**：2026-10-22

## Provider 协议

依据 DeepSeek 官方 Search & Fetch 文档：

- Search：`POST https://api.deepseek.com/v1/tools/search`
- Fetch：`POST https://api.deepseek.com/v1/tools/fetch`
- 认证：Bearer `DEEPSEEK_SEARCH_API_KEY` / `DEEPSEEK_FETCH_API_KEY`

Search 请求只发送查询词、结果数量和 `need_content=false`。只有用户在本地结果之后再次点击
“继续搜索互联网”才会调用。Fetch 只处理用户明确选择并确认收入知识库的单个候选 URL。

## 交互

- 本地结果始终先显示。
- 本地搜索完成后出现“继续搜索互联网”入口。
- 联网结果独立显示，不覆盖本地结果。
- 每条候选可打开原始来源，或点击“收入识海”。
- 收入时调用单页 Fetch，将 Markdown 内容转换成安全纯文本富文本，并记录
  `internet_result` 来源 URL 和采集时间。
- 候选在服务端内存缓存 15 分钟，并绑定当前账号；其他账号不能保存。
- “继续搜索互联网”在首页初始状态即显示，未完成本地检索时保持禁用并提示先检索；本地结果
  返回后按钮启用，仍需用户第二次点击才会发送查询。

## 验证

- 前端 E2E：首次搜索不调用联网接口；点击后请求包含 `explicitConsent=true`；候选可收入知识库。
- Provider 测试：Search/Fetch 的 Bearer 认证、请求体、响应字段映射通过。
- Service 测试：候选按账号缓存、Fetch 内容转换为 `internet_result` 来源知识。
- 后端编译和定向测试通过。
- 生产前端：`v0.1.19-mvp` 的构建资源包含“继续搜索互联网”和“收入识海”，入口位于本地
  搜索结果下方；只有输入关键词并执行本地搜索后才会显示。
- 生产接口：未登录请求 `POST /kb/api/v1/search/external` 返回 HTTP 401，确认路由已部署且
  受登录保护。
- 生产 Provider 冒烟：Search 返回 HTTP 200 和 3 条结果；对首条结果执行单 URL Fetch，
  返回 HTTP 200，标题为“首次调用 API | DeepSeek API Docs”，正文 2866 字符。
- 生产构建浏览器验证（`v0.1.23-mvp`）：初始首页入口 `visible=true`、`disabled=true`，
  “先完成一次本地检索”提示可见；本地检索和显式点击后才会请求联网接口。

## 当前边界

- 本次 MVP 先接通搜索和 Fetch，不扣除 002 点数；002 报价、预留、结算和释放上线后必须替换
  当前的免费调用路径。
- 搜索结果只保存短期内存候选；服务重启后未确认候选失效，用户可重新搜索。
- 自动摘要、自动分类和标签仍由后续 AI Worker 处理；当前只保存 Fetch 到的正文和候选摘要。
