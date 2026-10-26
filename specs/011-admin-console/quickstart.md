# 快速验证：管理员账号管理台

## 前置条件

- Java 17
- Node.js 22+
- PostgreSQL 18，本地测试库 `zhishiku_test`
- 已执行 `deploy/local-test/start.ps1` 所需的本地依赖

管理员密码只通过当前进程环境变量设置，不写入任何文件或命令示例：

```powershell
$env:SHIHAI_ADMIN_USERNAME = "a115846"
$env:SHIHAI_ADMIN_PASSWORD = "<由部署环境安全提供>"
```

本地测试启动脚本会保留进程内环境变量；如测试库中管理员已存在，密码变量为空时使用原密码哈希。

## 启动

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\local-test\start.ps1
```

打开 `http://127.0.0.1:4173/kb/login`，输入管理员用户名、部署密码和图形验证码。

## 端到端检查

1. 使用 `a115846` 登录，确认侧栏出现“账号管理”，打开
   `http://127.0.0.1:4173/kb/admin/users`。
2. 搜索一个普通账号，确认列表展示邮箱、状态、可用点数、冻结点数、累计发放和累计消耗。
3. 禁用该普通账号并填写原因，确认页面显示禁用状态。
4. 使用另一无痕窗口登录被禁用账号，确认登录失败。
5. 如已有旧会话，在原窗口刷新或请求知识列表，确认返回登录页。
6. 重新启用账号，确认可以重新登录且原有知识和点数仍在。
7. 向普通账号赠送点数并填写原因，确认余额增加与账本一致。
8. 重复提交同一幂等请求，确认余额只增加一次。
9. 尝试禁用 `a115846`，确认被拒绝。
10. 使用普通账号直接访问 `/kb/admin/users` 和 API，确认被拒绝。
11. 切换中英文，并在 1440、768、390 像素宽度完成搜索、停用/启用和赠送操作。

## 自动化验证

```powershell
Push-Location backend
try {
  .\mvnw.cmd -pl kb-app -am test
  .\mvnw.cmd -pl kb-app -am verify
} finally {
  Pop-Location
}

Push-Location frontend/kb-web
try {
  pnpm.cmd lint
  pnpm.cmd test
  pnpm.cmd build
  pnpm.cmd exec playwright test e2e/admin-users.spec.ts
} finally {
  Pop-Location
}
```

## 视觉检查

```powershell
node C:\Users\95611\.codex\skills\taste-skill\scripts\lint.mjs prototype/admin.html
node C:\Users\95611\.codex\skills\taste-skill\scripts\inspect.mjs prototype/admin.html --app
```

验收要求：

- 1440、768、390 像素无 `FAIL`。
- 表格数字右对齐并使用等宽数字。
- 账号状态在行内显示，不使用饱和色块。
- 手机端没有横向页面滚动，操作按钮触控高度不少于 44 像素。
- 中英文切换后布局不溢出、不遮挡。

## 预期结果

- 管理员是唯一可以访问管理页和 API 的主体。
- 禁用账号对新登录和旧会话都生效。
- 赠送点数只通过可审计批次和账本到账。
- 普通用户的原有知识、附件、聊天、点数和支付功能不发生回归。
