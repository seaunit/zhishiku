# 前端原型对齐验收报告

**日期**：2026-10-22

**原型**：`prototype/index.html`

**设计方向**：Taste App Screen `data-terminal`，seed `360653649`

**实现入口**：`frontend/kb-web/src/prototype-theme.css`

## 对齐内容

- 深海蓝主题令牌与原型一致：`#061522` 页面、`#081d2c` 表面、`#66d7e6` 强调色。
- Public Sans 作为界面字体，中文保留系统字体回退。
- 桌面端使用 72px 图标轨，包含首页、知识库、采集、待处理、回收站和设置。
- 移动端使用六项固定底部导航，不再使用旧版汉堡菜单和抽屉侧栏。
- 顶部栏显示个人知识域、识别状态、语言切换和当前账户。
- 首页包含单一搜索框、四项知识概况、我的知识、知识海图和待处理侧栏。
- 详情页使用正文与分类、标签、来源、状态侧栏的双栏布局。
- 登录、注册、采集和富文本工具栏同步采用深海蓝主题与相同控件尺寸。

## 浏览器证据

| 页面 | 证据目录 | 结果 |
|------|----------|------|
| 登录 | `.taste-skill/prototype-alignment-login-final/` | 0 FAIL，1 个品牌区大面积留白警告 |
| 首页 | `.taste-skill/prototype-alignment-home-inspect/` | 0 FAIL，0 WARN |
| 采集 | `.taste-skill/prototype-alignment-capture-inspect/` | 0 FAIL，1 个编辑器输入留白警告 |
| 详情 | `.taste-skill/prototype-alignment-detail-inspect/` | 0 FAIL，2 个非对称详情警告 |
| 前后对照 | `.taste-skill/prototype-alignment-before/`、`.taste-skill/prototype-alignment-after/` | 桌面、平板、手机人工复核 |

登录态首页检查覆盖 1440、768 和 390 CSS 像素。采集与详情检查覆盖相同宽度。

## 自动化

```powershell
cd frontend/kb-web
pnpm lint
pnpm typecheck
pnpm test
pnpm build
pnpm exec playwright test e2e/app.spec.ts --workers=1
pnpm exec playwright test --config ..\..\tests\playwright.config.ts `
  tests/e2e/richtext-taste.spec.ts --workers=1
```

结果：Vitest 41 项、前端 E2E 20 项、富文本 Taste E2E 1 项和生产构建通过。

```powershell
node C:\Users\95611\.codex\skills\taste-skill\scripts\lint.mjs --app `
  frontend\kb-web\src\views frontend\kb-web\src\components `
  frontend\kb-web\src\styles.css frontend\kb-web\src\prototype-theme.css
```

结果：源检查 `0 FAIL`；保留 3 个浮层阴影和 1 组应用文案破折号警告。

## 允许的视觉差异

- 登录品牌区保留大面积留白，作为品牌标题与表单之间的安静分隔。
- 采集页编辑器的空白区域是正文输入画布，不是缺失内容。
- 详情正文很长时右侧信息栏自然短于正文；它不是同组指标卡，不强制拉伸等高。
- Taste 检查使用固定 Public Sans，不采用 `.taste-skill/direction.json` 中另一个门户页面的
  Libre Franklin 字体方向；最新原型是本项目的视觉权威。

## 未完成边界

- 设置页 AI 授权开关目前保存当前浏览器偏好；服务端授权 API 接入后必须切换为服务端状态。
- 回收站页面使用真实空状态；删除、恢复和永久清理接口尚未接入前端。
- 原型中的网页抓取、文档导入和剪贴板截图是独立采集流程，本次只完成应用壳与现有能力对齐，
  不伪造尚未实现的后端导入结果。
