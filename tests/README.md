# 富文本验收测试

本目录补充 `004-rich-text-editor` 的跨语言夹具和安全、E2E、无障碍、Taste、性能与零影响检查。
测试只读取前端构建产物或真实页面，不修改应用源码。

## 前置

```powershell
pnpm --dir frontend/kb-web install --frozen-lockfile
node tests/fixtures/rich-text/validate-fixtures.mjs
node tests/fixtures/rich-text/generate-security-corpus.mjs --check
```

Playwright 使用仓库现有的 `@playwright/test` 和 Chrome，不新增依赖。

## 本地安全与体验测试

```powershell
pnpm --dir frontend/kb-web exec playwright test --config ../../tests/playwright.config.ts
```

单独运行：

```powershell
pnpm --dir frontend/kb-web exec playwright test --config ../../tests/playwright.config.ts security
pnpm --dir frontend/kb-web exec playwright test --config ../../tests/playwright.config.ts security
pnpm --dir frontend/kb-web exec playwright test --config ../../tests/playwright.config.ts accessibility
pnpm --dir frontend/kb-web exec playwright test --config ../../tests/playwright.config.ts e2e/richtext-taste.spec.ts
pnpm --dir frontend/kb-web exec playwright test --config ../../tests/playwright.config.ts performance
```

## 生产零影响检查

以下测试只执行无副作用的 GET/HEAD：

```powershell
$env:KB_ZERO_IMPACT_LIVE='1'
$env:KB_LIVE_BASE_URL='https://seaunit.site'
pnpm --dir frontend/kb-web exec playwright test --config ../../tests/playwright.config.ts e2e/richtext-zero-impact.spec.ts
```

检查支付 sidecar 时需要从生产主机内部提供只读健康端点：

```powershell
$env:KB_SIDECAR_URL='http://127.0.0.1:8090/health'
```

该变量不在本机时，sidecar 用例会明确跳过，不能把跳过报告为通过。

## 外部条件

- T075 新环境 quickstart 需要一台未初始化且配置了独立 PostgreSQL、S3、Playwright 和 AI/SMTP
  测试凭据的环境。
- T073 的证书、Cookie 与公开入口可以直接验证；sidecar 只能从生产主机内部验证。
- T078 的 10 人可用性研究必须由真实参与者执行，本仓库只提供协议和空白记录表。

运行产物写入 `.runtime/rich-text-tests/`，该目录已被 `.gitignore` 忽略。
