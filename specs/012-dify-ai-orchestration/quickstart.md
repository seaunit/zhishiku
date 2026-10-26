# 本地快速验证：Dify AI 编排

**日期**：2026-10-27

## 前置条件

- 本地 Dify 1.13.3 正常运行，并已配置 DeepSeek 插件。
- 已导入 `deploy/dify/` 下两个识海工作流。
- 已分别创建识海专用 API Key，并设置到用户或进程环境变量。
- 本地 PostgreSQL、后端和前端测试环境按仓库现有说明启动。

## 环境变量

```powershell
$env:SHIHAI_AI_ORCHESTRATION_PROVIDER = "dify"
$env:SHIHAI_DIFY_BASE_URL = "http://127.0.0.1:8502/v1"
$env:SHIHAI_DIFY_ORGANIZER_API_KEY = "<识海知识整理工作流 Key>"
$env:SHIHAI_DIFY_CHAT_API_KEY = "<识海聊天工作流 Key>"
$env:SHIHAI_DIFY_TIMEOUT_SECONDS = "60"
$env:SHIHAI_DIFY_MAX_RETRIES = "1"
```

真实 Key 不得写入仓库、截图或命令输出。

## 后端测试

```powershell
cd backend
.\mvnw.cmd -pl kb-app -Dtest=DifyAiOrchestrationClientTest,DifyKnowledgeOrganizerTest,DifyKnowledgeChatClientTest test
.\mvnw.cmd -pl kb-app test
```

预期：

- Dify 请求携带正确操作、账号外部键和脱敏内容。
- 阻塞响应和流式事件均通过解析测试。
- 超时、畸形 JSON、空结果和失败状态均失败关闭。
- 现有 DeepSeek 测试继续通过。

## 本地端到端

1. 启动后端并确认 `/kb/api/actuator/health` 返回 `UP`。
2. 登录识海，使用 AI 文字整理提交一段测试内容。
3. 确认生成至少一条待复核知识，正文、摘要、分类和标签完整。
4. 打开聊天，确认知识回答流式显示、引用来自当前账号知识并刷新后恢复历史。
5. 提交一个知识库未覆盖的通用问题，确认小识继续自然回答。
6. 停用或临时移开 Dify 聊天 Key，确认接口返回统一 AI 不可用错误，且不写入成功会话。
7. 将 `SHIHAI_AI_ORCHESTRATION_PROVIDER` 改为 `deepseek` 并重启，确认现有直连测试行为可用。

## 回滚

```powershell
$env:SHIHAI_AI_ORCHESTRATION_PROVIDER = "deepseek"
```

无需数据库回滚。Dify 工作流可单独停用；识海业务数据和点数账本不受 Dify 容器状态影响。
