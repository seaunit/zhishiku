# 快速验证：一键全部复核

**日期**：2026-10-27

## 后端

```powershell
cd backend
.\mvnw.cmd -pl kb-app -am '-Dtest=KnowledgeLifecycleControllerTest,KnowledgeServiceReviewAllTest' test
```

预期：

- 接口只允许 POST 并受 CSRF 保护。
- 当前账号所有 `review` 知识变为 `succeeded`。
- 其他账号、处理中和手动知识不变。
- 重复执行返回 `reviewed=0`。

## 前端

```powershell
cd frontend/kb-web
pnpm lint
pnpm typecheck
pnpm test
pnpm exec playwright test e2e/app.spec.ts -g "全部复核"
pnpm build
```

## 手工验收

1. 登录拥有多条待复核知识的账号。
2. 打开 `/kb/processing`。
3. 确认标题右侧显示“全部复核 N 条”。
4. 点击并确认，确认框说明跨分页影响范围。
5. 页面刷新后待复核条目清空，导航数量同步减少，显示“已复核 N 条”。
6. 分别在 1440、768、390 像素检查按钮、确认框和列表。
7. 切换英文，确认按钮、确认框和结果提示完整。
