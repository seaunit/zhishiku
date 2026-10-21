import { sanitizeExternalUrl } from '@/features/editor/paste/html-to-rich-text'
import type { RichTextMark, RichTextNode } from '@/types/rich-text'

export function safeLinkHref(mark: RichTextMark): string | null {
  if (mark.type !== 'link') {
    return null
  }
  return sanitizeExternalUrl(String(mark.attrs?.href || ''))
}

export function imageAccessibleText(node: RichTextNode): string {
  const alt = String(node.attrs?.alt || '').trim()
  const caption = String(node.attrs?.caption || '').trim()
  return alt || caption || '正文图片'
}

export function tableDimensions(node: RichTextNode): {
  rows: number
  columns: number
} {
  return {
    rows: node.content?.length || 0,
    columns: Math.max(0, ...(node.content || []).map((row) => row.content?.length || 0)),
  }
}
