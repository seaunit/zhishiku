# AI 辅助采集统一规则 Taste 验收

**日期**：2026-10-22

**功能**：`009-ai-assisted-capture`

## 设计方向

沿用已批准的 `data-terminal` App Screen 和深海蓝主题。新增第五个“AI 文字”模式，使用现有
标签、输入框、同意行和主按钮层级；未新增卡片、渐变、图标装饰或第二套视觉系统。

## 浏览器检查

- 1440、768、430、390、320 五档视口均能显示五个采集模式。
- AI 文字输入、备注、外部 AI 同意和“AI 整理文字”按钮完整可用。
- 320/390 手机宽度无页面级横向滚动；采集标签在组件内部横向滚动。
- 底部导航不遮挡主按钮或同意控件。
- 文档、截图和网页既有采集场景继续通过五档视口检查。

## Taste 结果

- 变更源文件 lint：`0 FAIL / 1 WARN`。
- App Screen inspector：`0 FAIL / 1 WARN`。
- 已人工查看 `.taste-skill/inspect/desktop-fold.jpg` 和 `mobile-fold.jpg`。

## 保留差异

- 字体继续使用已批准原型和产品设计系统中的 Public Sans；inspector 的 `font-not-dealt` 规则通过
  `--allow font-not-dealt` 明确允许，原因是本次没有字体系统变更。
- `tiny-text` WARN 来自采集页既有的 11–12px 标签、状态和辅助文字，符合 App Screen 高密度标签
  规则；本次没有新增更小字号。
