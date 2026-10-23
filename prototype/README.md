# 识海交互原型

## Preview

```powershell
python -m http.server 8088 --bind 127.0.0.1 --directory prototype
```

Open `http://127.0.0.1:8088/`.

## Design Direction

- Taste App Screen style: `data-terminal`
- Seed: `360653649`
- Palette adaptation: the style's neutral dark surfaces were mapped to deep-sea blues because the
  product is explicitly named and directed as “识海”.
- Product face: a three-wave mark and an interactive knowledge-depth map.
- Signature element: the knowledge map treats automatic categories as depth layers and every node
  is a real search entry point.
- Number style: tabular Public Sans. Monospace was removed after inspection to keep one typeface.

## Screens

- Home: find saved knowledge and see what still needs organization.
- Library: scan all saved material without a second search field.
- Detail: read the full item with rich formatting, inline images, source attachments, locked
  classification, and related knowledge.
- Capture: rich-text manual editing, one-page URL capture, document/image upload, and clipboard
  screenshot paste.
- Processing: monitor provisional classification, failure, and retry.
- Trash: restore or permanently remove deleted items.
- Settings: manage AI service/content-type authorization, language, and retention.
- Profile: view the current account, edit the display name, and review account boundaries.

## Interactions

- Search local knowledge first.
- 互联网搜索入口显示“互联网搜索 · 1 点”（英文为“Internet search · 1 point”）。
- 点击该入口一次即构成明确的扣费同意：客户端自动创建并确认 1 点报价后立即搜索，不再显示
  搜索本身的二次确认。
- 如果同时选择 AI 回答，单击前必须显示“搜索 1 点 + 回答 1 点，共 2 点”的分项与总价。
- 查询完成后即使零结果也扣 1 点；超时、服务不可用或协议失败不扣点。
- Save an internet candidate only through an AI-organized Fetch result flow.
- Open a knowledge item, edit its rich text, or trigger re-analysis.
- Create a rich-text manual item with headings, emphasis, lists, quotes, links, code, and inline images.
- Pasted HTML and images are converted into the supported formatting and local-image flow; an
  unsupported or oversized document/image file remains in the form with a visible error.
- Switch among manual, AI text, web, document, and screenshot capture modes.
- Paste a screenshot from the clipboard in the Screenshot tab; it uses a focusable paste field and
  intentionally has no file chooser, drag/drop target, album picker, or camera picker.
- Drop or choose a document or JPG/PNG/WebP image in the Document tab; documents and images proceed
  through OCR/parsing and AI organization, and the visible file button is keyboard accessible.
- Toggle Chinese and English interface copy.
- Open the account menu from the avatar; choose Profile or a separately confirmed sign-out.
- Edit the display name and see the avatar initial and account menu update immediately.
- Restore a trashed item.
- Switch application views in the rail or mobile bottom navigation.

## Sample Data

The prototype currently contains:

- 12 local knowledge items
- 3 simulated internet results
- 2 recycled items
- 5 automatic categories
- 9 AI review, processing, or retry jobs; manual entries never enter this queue

The item titles, summaries, source labels, external results, and authorization state are sample
content for design review. They must be replaced with real user data or API-driven content during
implementation.

## Validation

- Taste source lint: `0 FAIL`, `0 WARN`
- Taste browser inspection at 1440, 768, and 390 px: `0 FAIL`
- Account profile inspection: `0 FAIL`, one retained `tiny-text` warning for the established
  11–12px navigation metadata.
- Account menu, profile save, and logout confirmation were exercised in Chrome at 1440 and 390px.
- Screenshot empty state, pasted-preview state, and Document/Image state were reviewed separately.
- Rich-text editor state was reviewed separately at all three widths; source lint and browser
  inspection both report `0 FAIL`.
- Retained warnings: the compact form uses dense 11–12px helper text and a quiet lower page rest.
  These are intentional App Screen choices, not decorative empty space.

## Focused previews

Open these URLs while the prototype server is running:

```text
http://127.0.0.1:8088/login.html
http://127.0.0.1:8088/?view=capture&mode=screenshot
http://127.0.0.1:8088/?view=capture&mode=screenshot&demoScreenshot=1
http://127.0.0.1:8088/?view=capture&mode=document
http://127.0.0.1:8088/?view=capture&mode=manual
http://127.0.0.1:8088/?view=profile
```

`demoScreenshot=1` is a visual-review fixture only. Normal interaction uses a real ClipboardEvent;
the Screenshot tab contains no file input.
