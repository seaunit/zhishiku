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
- Detail: read the full item, source trail, locked classification, and related knowledge.
- Capture: manual text, one-page URL capture, document upload, and screenshot analysis.
- Processing: monitor provisional classification, failure, and retry.
- Trash: restore or permanently remove deleted items.
- Settings: manage AI service/content-type authorization, language, and retention.

## Interactions

- Search local knowledge first.
- Continue to internet search only after a separate user action.
- Save an internet candidate into the knowledge library.
- Open a knowledge item and trigger re-analysis.
- Create a manual item or switch among capture modes.
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
- Taste browser inspection at 1440, 768, and 390 px: `0 FAIL`
- One retained warning: 57% of text is below 13px. This is an intentional dense App Screen
  decision: 13–14px carries content, while 11–12px is reserved for sublines, metadata, and chart
  labels.
