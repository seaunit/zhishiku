# 富文本 Taste 与响应式检查报告

**日期**：2026-10-22

**页面**：`/kb/capture`、`/kb/library/{id}`

**执行方式**：Playwright 真实 Chromium，固定 API 夹具。

## 工具检查

```powershell
node C:\Users\95611\.codex\skills\taste-skill\scripts\lint.mjs --app frontend/kb-web/src/components frontend/kb-web/src/styles.css
pnpm --dir frontend/kb-web exec playwright test --config ../../tests/playwright.config.ts e2e/richtext-taste.spec.ts --workers=1
```

| 检查 | 结果 |
| --- | --- |
| Taste lint（组件和样式） | 0 FAIL，2 WARN |
| 两个 WARN | `more-menu` 与 `conflict-dialog/image-lightbox` 的浮层阴影，属于允许的 overlay |
| 页面级横向溢出 | 0 |
| 工具栏覆盖正文 | 0 |
| 手机可见工具按钮小于 44px | 0 |
| 200% 字体页面级横向溢出 | 0 |

## 视口结果

| 视口 | 采集 | 详情 | 结果 |
| --- | --- | --- | --- |
| 1440×900 | 单行工具栏，左右边距稳定 | 正文和表格在画布内 | 通过 |
| 768×1024 | 导航折叠为顶部栏 | 正文、图片和表格不撑宽页面 | 通过 |
| 430×932 | 手机工具栏收缩 | 详情单列，图片和表格可读 | 通过 |
| 390×844 | 主要按钮 44px | 单列详情，无页面级滚动 | 通过 |
| 360×800 | 工具栏进入更多菜单 | 正文和表格保持容器内滚动 | 通过 |
| 320×720 | 保留 H2、粗体、斜体、图片和更多入口 | 无按钮文字溢出 | 通过 |
| 740×360 横屏 | 编辑区仍可用 | 详情可读 | 通过，见残余观察 |

截图由测试写入：

```text
.runtime/rich-text-tests/results/*/taste/capture-*.png
.runtime/rich-text-tests/results/*/taste/detail-*.png
```

该目录为运行产物，不提交用户数据或构建缓存。

## 视觉复核

- 视觉方向延续识海的浅色知识工作区：深青主操作、低饱和灰蓝底、单一字体系统。
- 手机编辑屏保留最关键格式和图片入口，其余格式进入“更多”菜单。
- 详情页将编辑操作与正文分离，表格和代码区块边界清楚。
- 透明 1×1 测试图在截图中看起来是留白，但图片元素、说明和布局占位均已通过自动化断言。

## 残余观察

1. 740×360 横屏出现工具栏分组折行，更多入口位于第二行；没有遮挡正文或导致页面级溢出。
2. 浏览器自动化不能创建真实刘海安全区；已验证源码包含
   `env(safe-area-inset-bottom)`，但实际设备仍需补一次人工检查。
3. 软键盘和触控惯性无法由桌面 Chromium 完整模拟。
