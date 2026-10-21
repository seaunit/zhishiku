# 账户菜单与个人资料 Taste 验收

**日期**：2026-10-22

**功能**：`005-account-profile`

## 设计方向

- 沿用已批准的 `data-terminal` App Screen 方向和深海蓝品牌色，不重新设计应用壳。
- 品牌字体保持 Public Sans；Taste 检查器以 `font-not-dealt` 忽略，因为项目宪法和现有设计系统
  已固定该字体。
- 账户菜单是真实浮层，允许使用阴影；退出确认是模态对话框，也允许使用阴影。
- 资料页的问题是“这是哪个账号，以及如何修改界面身份”，账户信息和显示名编辑是页面主线。

## 第一轮

- 发现：资料页最初使用窄单列，桌面右侧出现 960×420 的空区。
- 发现：账户摘要和表单被检查器识别为同排卡片，高度差达到 258px。
- 发现：输入框缺少统一控件样式，浏览器原生外观与深海主题不一致。
- 修复：改为全宽账户摘要、单一资料表单和账户边界说明；补齐输入框、只读状态、焦点和移动端样式。

## 第二轮

```text
ROUND 2
Seen in one second: 个人资料与当前账号
Idea: 用“账户边界”把身份、隔离和显示名称放在同一张资料工作台上
Live thing: 修改显示名称后，头像首字和账户菜单即时同步
Layout words by section: summary=account strip / account=form / boundaries=three-column facts
Scores: design 8 · usability 8.7 · creativity 7.5 · content 8.5 → overall 8.1
Caps applied: none
Top three problems:
1. 现有全局导航元数据使用 11–12px，Taste 报 tiny-text；这是已批准 App Shell 的既有密度，不在本功能内放大。
2. 账户边界位于桌面首屏下缘，需要轻微滚动；它属于辅助说明，不影响主要资料编辑。
3. 头像上传不在 MVP，头像仍由显示名称或邮箱首字生成；这是已冻结范围，不作为视觉缺陷。
```

## 验证

- Taste 源 lint：`0 FAIL / 0 WARN`。
- Taste App Screen 检查：1440/768/390 为 `0 FAIL`，保留一项既有导航 `tiny-text` WARN。
- 手机 390px 和 320px 无页面级横向滚动，菜单不超出视口，菜单项高度不小于 44px。
- 原型和正式 Vue 页面均检查了账户菜单、资料页和退出确认。

## 证据

- `.taste-skill/account-profile/profile/desktop-fold.jpg`
- `.taste-skill/account-profile/profile/tablet-fold.jpg`
- `.taste-skill/account-profile/profile/mobile-fold.jpg`
- `.taste-skill/account-profile/menu-desktop.jpg`
- `.taste-skill/account-profile/menu-mobile.jpg`
- `.taste-skill/account-profile/logout-mobile.jpg`
- `.taste-skill/account-profile/actual-profile-desktop.jpg`
- `.taste-skill/account-profile/actual-profile-mobile.jpg`
