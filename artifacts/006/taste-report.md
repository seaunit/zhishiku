# 网页截图 AI 拆分导入 Taste 验收

**日期**：2026-10-22

**功能**：`006-webpage-ai-import`

## 设计方向

沿用已批准的 `data-terminal` App Screen 方向与深海蓝品牌色，不重新设计应用壳。本功能只新增
网页导入执行状态和外部 AI 同意控件，采用设置页已有的授权行样式，避免引入新的卡片体系。

品牌字体继续使用 Public Sans；Taste 检查器以 `font-not-dealt` 忽略该规则，因为项目宪法和
现有设计系统已经固定字体，不在本功能中更换。

## 视觉验证

- 真实浏览器覆盖 1440、768、390 像素。
- 网页链接标签、URL 输入、单页说明、外部 AI 同意、执行按钮均可见。
- 未勾选同意时提交按钮禁用；勾选后启用。
- 三个宽度均无页面级横向滚动，同意控件不超出视口。
- 390px 下按钮变为全宽，底部导航和触控目标保持可用。
- 截图：
  - `.runtime/rich-text-tests/results/.../taste/webpage-desktop-1440.png`
  - `.runtime/rich-text-tests/results/.../taste/webpage-tablet-768.png`
  - `.runtime/rich-text-tests/results/.../taste/webpage-phone-390.png`

## Taste 检查

- App Screen inspector：`0 FAIL / 1 WARN`。
- 唯一 WARN 为 `hero-void`：采集页是表单优先的工作面，URL 模式内容完成后首屏下方保留背景空区。
  这是既有 App Shell 的页面节奏，不在本功能内重做采集页布局。
- 通用源 lint 仍报告既有深海主题的 hairline、outlined-box 和浮层 shadow；这些规则来自已批准的
  App Screen 设计系统，不是本次新增的装饰。本功能新增的同意行只使用现有授权行体系。

## 评审结论

网页导入模式与原型保持同一信息层级：单页边界先于外部 AI 处理，用户同意是提交前置条件，
执行按钮状态明确。没有发现文字遮挡、控件溢出或不可触达的主要操作。
