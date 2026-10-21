# 知识分类与整理状态语义修复

**日期**：2026-10-22

**功能**：001 个人知识库

## 问题现象

详情页同时显示分类“待整理”和处理状态“已整理”，看起来互相矛盾。

## 根因

- 分类缺失时复用了 `home.noCategory`，而其中文值是“待整理”，把分类占位与处理队列文案混在一起。
- 处理状态映射使用默认回退：任何未显式识别的状态都会显示“已整理”。
- 手工条目实际状态为 `not_requested`，因此被错误回退成“已整理”。

## 修复

- 分类缺失统一显示“未分类”/“Uncategorized”。
- `not_requested` 明确显示“未整理”/“Not organized”。
- 新增共享状态映射，显式处理 `not_requested`、`queued`、`running`、`processing`、`pending`、
  `review`、`succeeded`、`failed` 和未知状态。
- 首页、知识库、待处理和详情页统一使用同一状态映射。
- 未知状态显示“状态未知”，并使用中性样式，不再伪装成完成状态。
- 原型同步修正手工条目的默认分类和标签。

## 验证

- `knowledge-status.test.ts` 3 项通过。
- E2E 使用无分类、`not_requested` 的详情数据，确认显示“未分类 / 未整理”，不再出现“待整理”。
- Vitest 44 项、Playwright 32 项通过。
- lint、typecheck、build 通过。
- Taste App Screen 在 1440/768/390 为 `0 FAIL`，保留既有导航 `tiny-text` WARN。
- 生产条目“测试知识库”的 `processing_status` 确认为 `not_requested`，证明问题来自展示映射。
- 生产 release `b3e6a89993d2-20261021225015`，tag `v0.1.22-mvp`。
