# 富文本原型检查报告

**日期**：2026-10-21

**方向**：Taste App Screen `data-terminal`，沿用既有深海蓝色调，seed `360653649`

**页面问题**：用户能否像使用轻量编辑器一样整理知识，并确认图片在保存前经过安全入口？

**签名交互**：正文既是编辑区域，也是知识内容本身；图片插入后进入同一正文语义和详情来源链路。

## 已实现

- 手动采集和编辑使用可交互富文本正文。
- 支持 H2/H3、粗体、斜体、下划线、删除线、列表、引用、代码、链接、撤销/重做和清除格式。
- 支持正文图片插入、说明/替代文本双击编辑、来源附件展示和图片失败状态。
- 粘贴 HTML 经过允许标签和协议清洗；远程图片不自动加载。
- 详情按保存的富文本格式渲染，并保留来源与处理信息。
- 手机工具栏改为可见换行布局，按钮触控高度为 44px，焦点环与安全区已补齐。
- 截图入口使用真正可聚焦的剪贴板粘贴字段，不提供文件、拖放、相册或相机控件。
- 文档入口提供键盘可操作的“选择文件”按钮，接受文档及 JPG/PNG/WebP/HEIC 图片，并执行 25 MiB 原型限制。
- 快捷键和撤销/重做使用浏览器原生编辑能力模拟。

## 验证

```text
node --check prototype/app.js
node <taste-skill>/scripts/lint.mjs --app prototype
node <taste-skill>/scripts/inspect.mjs http://127.0.0.1:8088/?view=capture&mode=manual --app --widths 1440,768,390,360
node <taste-skill>/scripts/inspect.mjs http://127.0.0.1:8088/?view=capture&mode=screenshot --app --widths 1440,768,390,360
node <taste-skill>/scripts/inspect.mjs http://127.0.0.1:8088/?view=capture&mode=document --app --widths 1440,768,390,360
```

结果：

- `node --check`：通过。
- Taste lint：`0 FAIL / 0 WARN`。
- Taste inspect：富文本、截图和文档入口在 1440、768、390、360 均为 `0 FAIL`。
- 保留一条 `tiny-text` warning：编辑器工具栏和辅助说明属于密集 App Screen，11–12px 仅用于辅助文字。
- 保留一个 `blank-gap` warning：360×225 横屏短视口下，空白编辑区被识别为 108px 的留白；该区域是实际写作画布，不是未渲染内容。
- 真实浏览器测试通过：格式应用、正文图片插入、保存、富文本详情、重新编辑和来源附件展示。
- 真实浏览器测试通过：截图页无可绕过的文件控件，剪贴板粘贴生成预览；文档页显示键盘按钮，超限文件显示错误且不保留选择。

## 原型限制

- 原型使用 `contenteditable` 和浏览器命令模拟编辑行为；正式实现采用 Tiptap/ProseMirror 与共享 schema。
- 原型图片使用本地对象 URL，不代表正式扫描、对象存储和附件生命周期已经实现。
- 原型未连接后端，双标签页冲突、真实 AI 图片分析和旧数据迁移仅由任务表覆盖。
- HEIC 预览在原型中以“扫描后生成”占位，正式实现必须由 Sharp 规范化后再展示。
