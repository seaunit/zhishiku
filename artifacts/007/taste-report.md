# 文档解析 Taste 验收

**日期**：2026-10-22

**功能**：`007-document-ai-import`

## 设计约束

沿用已批准的 `data-terminal` App Screen 与深海蓝主题。文档模式不新增视觉体系，只把已有的
授权行复用于“允许外部 AI 解析并整理本文档”，并更新支持格式提示。

## 真实浏览器检查

- 1440、768、430、390、320 像素均完成长文件名 PDF、显示同意控件和禁用提交检查。
- 未勾选同意时“保存文档”禁用。
- 文件卡、同意行、备注和主按钮均不超出视口。
- 手机宽度下文件卡与按钮自然纵向排列；滚动到底部时主按钮位于固定底部导航上方。
- 截图：
  - `.runtime/rich-text-tests/results/.../taste/document-desktop-1440.png`
  - `.runtime/rich-text-tests/results/.../taste/document-tablet-768.png`
  - `.runtime/rich-text-tests/results/.../taste/document-phone-390.png`

## Taste 结果

- 变更源文件 lint：`0 FAIL / 0 WARN`。
- App Screen inspector：`0 FAIL / 1 WARN`。
- WARN 为采集页既有 `hero-void`：表单完成后首屏下方保留背景空区，不是本次新增控件导致。
- 未发现文字遮挡、横向滚动、控件越界或触控目标明显过小。
