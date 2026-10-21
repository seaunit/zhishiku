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

## Interactions

- Search local knowledge first.
- Continue to internet search only after a separate user action.
- Save an internet candidate into the knowledge library.
- Open a knowledge item, edit its rich text, or trigger re-analysis.
- Create a rich-text manual item with headings, emphasis, lists, quotes, links, code, and inline images.
- Pasted HTML and images are converted into the supported formatting and local-image flow; an
  unsupported or oversized document/image file remains in the form with a visible error.
- Switch among capture modes while preserving the current note and rich-text draft.
- Paste a screenshot from the clipboard in the Screenshot tab; it uses a focusable paste field and
  intentionally has no file chooser, drag/drop target, album picker, or camera picker.
- Drop or choose a document or JPG/PNG/WebP/HEIC image in the Document tab; the visible file button
  is keyboard accessible and the input enforces the 25 MiB prototype limit.
- Toggle Chinese and English interface copy.
- Restore a trashed item.
- Switch application views in the rail or mobile bottom navigation.

## Sample Data

The prototype currently contains:

- 12 local knowledge items
- 3 simulated internet results
- 2 recycled items
- 5 automatic categories
- 4 pending classification/review jobs

The item titles, summaries, source labels, external results, and authorization state are sample
content for design review. They must be replaced with real user data or API-driven content during
implementation.

## Validation

- Taste source lint: `0 FAIL`, `0 WARN`
- Taste browser inspection at 1440, 768, 390, and 360 px: `0 FAIL`
- Screenshot empty state, pasted-preview state, and Document/Image state were reviewed separately.
- Rich-text editor state was reviewed separately at all three widths; source lint and browser
  inspection both report `0 FAIL`.
- Retained warnings: the compact form uses dense 11–12px helper text and a quiet lower page rest.
  These are intentional App Screen choices, not decorative empty space.

## Focused previews

Open these URLs while the prototype server is running:

```text
http://127.0.0.1:8088/?view=capture&mode=screenshot
http://127.0.0.1:8088/?view=capture&mode=screenshot&demoScreenshot=1
http://127.0.0.1:8088/?view=capture&mode=document
http://127.0.0.1:8088/?view=capture&mode=manual
```

`demoScreenshot=1` is a visual-review fixture only. Normal interaction uses a real ClipboardEvent;
the Screenshot tab contains no file input.
