import type {
  RichTextDocument,
  RichTextMark,
  RichTextNode,
} from '@/types/rich-text'

export type PasteSourceKind = 'clipboard' | 'word' | 'google_docs' | 'webpage'

export type FormatFeature =
  | 'heading'
  | 'paragraph'
  | 'bold'
  | 'italic'
  | 'underline'
  | 'strike'
  | 'list'
  | 'link'
  | 'quote'
  | 'code'
  | 'table'
  | 'image'

export interface PasteConversionReport {
  sourceKind: PasteSourceKind
  sourceUrl?: string
  preserved: FormatFeature[]
  downgraded: FormatFeature[]
  removed: string[]
  imagePlaceholders: number
}

export interface PasteConversion {
  document: RichTextDocument
  report: PasteConversionReport
}

export interface PasteInput {
  html: string
  text: string
  sourceUrl?: string
}

const BLOCKED_ELEMENTS = new Set([
  'script',
  'style',
  'iframe',
  'form',
  'input',
  'button',
  'object',
  'embed',
  'svg',
  'canvas',
  'video',
  'audio',
])

export function detectPasteSource(input: PasteInput): PasteSourceKind {
  const html = input.html.toLowerCase()
  if (
    html.includes('urn:schemas-microsoft-com:office:word') ||
    html.includes('microsoft word')
  ) {
    return 'word'
  }
  if (
    html.includes('docs-internal-guid') ||
    html.includes('docs.google.com/document')
  ) {
    return 'google_docs'
  }
  if (input.sourceUrl || /<article|<main|<section/i.test(input.html)) {
    return 'webpage'
  }
  return 'clipboard'
}

export function sanitizeExternalUrl(raw: string | null | undefined): string | null {
  if (!raw || raw.length > 2048) {
    return null
  }

  let parsed: URL
  try {
    parsed = new URL(raw)
  } catch {
    return null
  }

  if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
    return null
  }
  if (parsed.username || parsed.password) {
    return null
  }
  if (isPrivateHostname(parsed.hostname)) {
    return null
  }

  return parsed.toString()
}

function isPrivateHostname(hostname: string): boolean {
  const host = hostname.toLowerCase().replace(/^\[|\]$/g, '')
  if (
    host === 'localhost' ||
    host.endsWith('.localhost') ||
    host.endsWith('.local')
  ) {
    return true
  }
  if (host === '::1' || host.startsWith('fc') || host.startsWith('fd') || host.startsWith('fe80:')) {
    return true
  }

  const ipv4 = /^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/.exec(host)
  if (!ipv4) {
    return false
  }

  const octets = ipv4.slice(1).map(Number)
  if (octets.some((part) => part > 255)) {
    return true
  }
  const [first, second] = octets
  return (
    first === 0 ||
    first === 10 ||
    first === 127 ||
    (first === 169 && second === 254) ||
    (first === 172 && second >= 16 && second <= 31) ||
    (first === 192 && second === 168)
  )
}

export function plainTextToRichTextDocument(text: string): RichTextDocument {
  const paragraphs = text.replace(/\r\n?/g, '\n').split(/\n{2,}/)
  return {
    type: 'doc',
    content: paragraphs.map((paragraph) => {
      const lines = paragraph.split('\n')
      const content: RichTextNode[] = []
      lines.forEach((line, index) => {
        if (line) {
          content.push({ type: 'text', text: line })
        }
        if (index < lines.length - 1) {
          content.push({ type: 'hardBreak' })
        }
      })
      return content.length ? { type: 'paragraph', content } : { type: 'paragraph' }
    }),
  }
}

export function convertClipboardHtml(input: PasteInput): PasteConversion {
  const sourceKind = detectPasteSource(input)
  const sourceUrl = sanitizeExternalUrl(input.sourceUrl)
  const report: PasteConversionReport = {
    sourceKind,
    sourceUrl: sourceUrl ?? undefined,
    preserved: [],
    downgraded: [],
    removed: [],
    imagePlaceholders: 0,
  }

  if (typeof DOMParser === 'undefined') {
    report.downgraded.push('paragraph')
    return {
      document: plainTextToRichTextDocument(input.text || stripTags(input.html)),
      report,
    }
  }

  const parsed = new DOMParser().parseFromString(input.html || '', 'text/html')
  const body = parsed.body
  const blocks = convertBlocks(Array.from(body.childNodes), report)

  if (!blocks.length) {
    report.downgraded.push('paragraph')
    return {
      document: plainTextToRichTextDocument(input.text || body.textContent || ''),
      report,
    }
  }

  report.preserved = [...new Set(report.preserved)]
  report.downgraded = [...new Set(report.downgraded)]
  report.removed = [...new Set(report.removed)]
  return {
    document: { type: 'doc', content: blocks },
    report,
  }
}

function stripTags(html: string): string {
  return html.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
}

function convertBlocks(
  nodes: Node[],
  report: PasteConversionReport,
): RichTextNode[] {
  const blocks: RichTextNode[] = []

  nodes.forEach((node) => {
    if (node.nodeType === Node.TEXT_NODE) {
      const text = node.textContent?.trim()
      if (text) {
        blocks.push({
          type: 'paragraph',
          content: [{ type: 'text', text }],
        })
      }
      return
    }
    if (node.nodeType !== Node.ELEMENT_NODE) {
      return
    }

    const element = node as HTMLElement
    const tag = element.tagName.toLowerCase()
    if (BLOCKED_ELEMENTS.has(tag)) {
      report.removed.push(tag)
      return
    }

    if (/^h[1-6]$/.test(tag)) {
      const level = Math.max(2, Math.min(4, Number(tag.slice(1))))
      if (Number(tag.slice(1)) > 4 || Number(tag.slice(1)) < 2) {
        report.downgraded.push('heading')
      } else {
        report.preserved.push('heading')
      }
      blocks.push({
        type: 'heading',
        attrs: { level },
        content: convertInlineNodes(element.childNodes, report),
      })
      return
    }

    if (tag === 'p' || tag === 'div') {
      report.preserved.push('paragraph')
      blocks.push({
        type: 'paragraph',
        content: convertInlineNodes(element.childNodes, report),
      })
      return
    }

    if (tag === 'ul' || tag === 'ol') {
      report.preserved.push('list')
      const items: RichTextNode[] = Array.from(element.children)
        .filter((child) => child.tagName.toLowerCase() === 'li')
        .map((item) => ({
          type: 'listItem',
          content: [
            {
              type: 'paragraph',
              content: convertInlineNodes(item.childNodes, report),
            },
          ],
        }))
      if (items.length) {
        blocks.push({
          type: tag === 'ol' ? 'orderedList' : 'bulletList',
          content: items,
        })
      }
      return
    }

    if (tag === 'blockquote') {
      report.preserved.push('quote')
      blocks.push({
        type: 'blockquote',
        content: [
          {
            type: 'paragraph',
            content: convertInlineNodes(element.childNodes, report),
          },
        ],
      })
      return
    }

    if (tag === 'pre') {
      report.preserved.push('code')
      blocks.push({
        type: 'codeBlock',
        content: [{ type: 'text', text: element.textContent || '' }],
      })
      return
    }

    if (tag === 'table') {
      report.preserved.push('table')
      const table = convertTable(element, report)
      if (table) {
        blocks.push(table)
      }
      return
    }

    if (tag === 'img') {
      report.removed.push('remote-image')
      report.imagePlaceholders += 1
      blocks.push(imagePlaceholder(element, report))
      return
    }

    if (tag === 'hr') {
      blocks.push({ type: 'horizontalRule' })
      return
    }

    const nested = convertBlocks(Array.from(element.childNodes), report)
    if (nested.length) {
      blocks.push(...nested)
    } else {
      const inline = convertInlineNodes(element.childNodes, report)
      if (inline.length) {
        blocks.push({ type: 'paragraph', content: inline })
      }
    }
  })

  return blocks
}

function convertInlineNodes(
  nodes: NodeListOf<ChildNode> | Node[],
  report: PasteConversionReport,
): RichTextNode[] {
  const result: RichTextNode[] = []

  Array.from(nodes).forEach((node) => {
    if (node.nodeType === Node.TEXT_NODE) {
      const text = node.textContent || ''
      if (text) {
        result.push({ type: 'text', text })
      }
      return
    }
    if (node.nodeType !== Node.ELEMENT_NODE) {
      return
    }

    const element = node as HTMLElement
    const tag = element.tagName.toLowerCase()
    if (BLOCKED_ELEMENTS.has(tag)) {
      report.removed.push(tag)
      return
    }
    if (tag === 'br') {
      result.push({ type: 'hardBreak' })
      return
    }
    if (tag === 'img') {
      report.removed.push('remote-image')
      report.imagePlaceholders += 1
      result.push(...(imagePlaceholder(element, report).content || []))
      return
    }

    const marks = marksForElement(element, report)
    const children = convertInlineNodes(element.childNodes, report)
    children.forEach((child) => {
      if (child.type === 'text' && marks.length) {
        result.push({
          ...child,
          marks: mergeMarks(child.marks, marks),
        })
      } else {
        result.push(child)
      }
    })
  })

  return result
}

function marksForElement(
  element: HTMLElement,
  report: PasteConversionReport,
): RichTextMark[] {
  const tag = element.tagName.toLowerCase()
  const marks: RichTextMark[] = []
  const style = element.getAttribute('style')?.toLowerCase() || ''
  const push = (feature: FormatFeature, mark: RichTextMark) => {
    marks.push(mark)
    report.preserved.push(feature)
  }

  if (tag === 'strong' || tag === 'b' || /font-weight\s*:\s*(bold|[6-9]00)/.test(style)) {
    push('bold', { type: 'bold' })
  }
  if (tag === 'em' || tag === 'i' || /font-style\s*:\s*italic/.test(style)) {
    push('italic', { type: 'italic' })
  }
  if (tag === 'u' || /text-decoration[^;]*underline/.test(style)) {
    push('underline', { type: 'underline' })
  }
  if (tag === 's' || tag === 'strike' || /line-through/.test(style)) {
    push('strike', { type: 'strike' })
  }
  if (tag === 'code') {
    push('code', { type: 'code' })
  }
  if (tag === 'a') {
    const href = sanitizeExternalUrl(element.getAttribute('href'))
    if (href) {
      push('link', { type: 'link', attrs: { href } })
    } else if (element.hasAttribute('href')) {
      report.removed.push('unsafe-link')
    }
  }

  return marks
}

function mergeMarks(
  existing: RichTextMark[] | undefined,
  next: RichTextMark[],
): RichTextMark[] {
  const merged = new Map<string, RichTextMark>()
  for (const mark of [...(existing || []), ...next]) {
    merged.set(mark.type, mark)
  }
  return [...merged.values()].slice(0, 6)
}

function imagePlaceholder(
  image: HTMLElement,
  report: PasteConversionReport,
): RichTextNode {
  const alt = image.getAttribute('alt')?.trim()
  const label = alt ? `图片未导入：${alt}` : '图片未导入'
  const source = sanitizeExternalUrl(image.getAttribute('src'))
  const content: RichTextNode[] = [{ type: 'text', text: label }]
  if (source) {
    report.downgraded.push('image')
    content[0] = {
      type: 'text',
      text: label,
      marks: [{ type: 'link', attrs: { href: source } }],
    }
  }
  return { type: 'paragraph', content }
}

function convertTable(
  tableElement: HTMLElement,
  report: PasteConversionReport,
): RichTextNode | null {
  const sourceRows = Array.from(tableElement.querySelectorAll('tr')).slice(0, 50)
  if (!sourceRows.length) {
    return null
  }

  const rows = sourceRows.map((row, rowIndex) => {
    const sourceCells = Array.from(row.querySelectorAll('th,td')).slice(0, 20)
    const cellType = rowIndex === 0 ? 'tableHeader' : 'tableCell'
    const cells = sourceCells.map((cell) => {
      if (rowIndex === 0 && cell.tagName.toLowerCase() !== 'th') {
        report.downgraded.push('table')
      }
      return {
        type: cellType,
        content: [
          {
            type: 'paragraph',
            content: convertInlineNodes(cell.childNodes, report),
          },
        ],
      }
    })
    return {
      type: rowIndex === 0 ? 'tableHeaderRow' : 'tableBodyRow',
      content: cells,
    }
  })

  if (!rows[0]?.content.length) {
    return null
  }
  return {
    type: 'table',
    attrs: { headerRow: true },
    content: rows,
  }
}
