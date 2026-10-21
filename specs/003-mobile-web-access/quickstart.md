# 快速验证：识海移动网页访问

## 前置条件

- Vue 开发服务或 Nginx staging 已启动。
- Spring Boot API 位于 `http://127.0.0.1:8082/kb/api`。
- 浏览器支持 Playwright、VisualViewport、ClipboardEvent 和 CSS safe-area。
- 已准备手机、平板、200% 字体和横屏模拟。

## 启动

```powershell
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,worker
cd ../frontend/kb-web
pnpm install
pnpm dev
```

打开 `http://127.0.0.1:4173/kb/`。

## 验证场景

1. 在 1440/768/430/390/360/320px 检查首页、搜索、知识库、详情、采集、设置和回收站。
2. 确认页面级横向滚动为 0。
3. 验证底部导航、安全区、Toast 和对话框不被软键盘遮挡。
4. 验证单搜索框、个人结果和外部搜索同意。
5. 验证富文本工具栏、图片、链接、撤销和保存操作。
6. 验证 44px 触控目标、键盘焦点和 200% 字体。
7. 验证报价确认、余额不足、Waffo 跳转和返回恢复。
8. 验证默认中文和完整英文，文字不溢出。
9. 在 Nginx staging 验证 Vue 构建产物、SPA fallback、`/kb/api`、Cookie Path 和 CSRF。
10. 执行 Taste 和 axe-core，保存截图和结果。

## 自动化命令

```powershell
cd frontend/kb-web
pnpm lint
pnpm typecheck
pnpm test
pnpm test:e2e:mobile
pnpm test:a11y
pnpm test:taste
pnpm build
```

## 相关产物

- [spec.md](spec.md)
- [plan.md](plan.md)
- [tasks.md](tasks.md)
