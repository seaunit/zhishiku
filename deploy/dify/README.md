# 识海 Dify 本地工作流

本目录只包含识海专用 Dify 工作流。不得导入、覆盖或修改拾题和其他项目的工作流。

## 工作流

- `shihai-knowledge-organizer.yml`：知识整理，输出结构化 JSON。
- `shihai-xiaoshi-chat.yml`：小识对话、检索计划、联网回答和流式输出。

两个工作流使用本地 Dify 已安装的 DeepSeek 插件。真实 DeepSeek API Key 配置在 Dify 工作区中，不进入本仓库。

## 导入

1. 打开本地 Dify 控制台。
2. 进入“工作室”，选择从 DSL 导入。
3. 分别导入两个 YAML 文件。
4. 检查模型提供商是否为本机已配置的 DeepSeek。
5. 分别发布工作流。
6. 为每个应用创建独立 API Key。
7. 将 Key 保存到当前 Windows 用户环境变量：

```powershell
[Environment]::SetEnvironmentVariable(
  'SHIHAI_DIFY_ORGANIZER_API_KEY',
  '<知识整理工作流 Key>',
  'User'
)
[Environment]::SetEnvironmentVariable(
  'SHIHAI_DIFY_CHAT_API_KEY',
  '<聊天工作流 Key>',
  'User'
)
```

真实 Key 不得写入 `.env.example`、README、日志或截图。

## 本地 API 桥接

当前 Docker Desktop 对 Dify 原 `8501` 端口的宿主转发可能失效，而 Dify 容器内部链路正常。使用以下脚本在 `127.0.0.1:8502` 创建独立回环桥接：

```powershell
.\deploy\dify\start-local-api-proxy.ps1
```

Dify 控制台必须使用 `http://localhost:8502/`，不要用 `http://127.0.0.1:8502/`。
Dify Web 配置把 API 固定为 `http://localhost:8501`；使用 `127.0.0.1` 打开页面会让登录请求变成
跨站访问，登录接口虽然返回成功，但 `SameSite=Lax` Cookie 不会随后续请求发送，表现为登录后
立即返回登录页。后端服务端调用仍使用 `http://127.0.0.1:8502/v1`，不受浏览器 Cookie 规则影响。

验证：

```powershell
curl.exe --noproxy "*" http://127.0.0.1:8502/console/api/setup
```

停止：

```powershell
.\deploy\dify\stop-local-api-proxy.ps1
```

该桥接复用现有 Dify Nginx 镜像和配置，不修改 Dify API、Worker、数据库或拾题服务，也不用于生产部署。

## 后端配置

```powershell
$env:SHIHAI_AI_ORCHESTRATION_PROVIDER = 'dify'
$env:SHIHAI_DIFY_BASE_URL = 'http://127.0.0.1:8502/v1'
$env:SHIHAI_DIFY_ORGANIZER_API_KEY = '<知识整理工作流 Key>'
$env:SHIHAI_DIFY_CHAT_API_KEY = '<聊天工作流 Key>'
$env:SHIHAI_DIFY_TIMEOUT_SECONDS = '60'
$env:SHIHAI_DIFY_MAX_RETRIES = '1'
```

回退到 DeepSeek 直连：

```powershell
$env:SHIHAI_AI_ORCHESTRATION_PROVIDER = 'deepseek'
```
