import type {
  RichTextDocument,
  RichTextMark,
  RichTextNode,
  ValidationResult,
} from '@/types/rich-text'
import { isUuid } from './id'

const ALLOWED_NODE_TYPES = new Set([
  'doc',
  'paragraph',
  'heading',
  'bulletList',
  'orderedList',
  'listItem',
  'blockquote',
  'codeBlock',
  'horizontalRule',
  'image',
  'table',
  'tableHeaderRow',
  'tableBodyRow',
  'tableHeader',
  'tableCell',
  'text',
  'hardBreak',
])

const ALLOWED_MARKS = new Set(['bold', 'italic', 'underline', 'strike', 'code', 'link'])
const MAX_DEPTH = 40

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function validateAttributes(node: RichTextNode, path: string): ValidationResult {
  const attrs = node.attrs ?? {}

  if (node.type === 'heading') {
    if (![2, 3, 4].includes(Number(attrs.level))) {
      return { ok: false, message: `${path}.attrs.level must be 2, 3, or 4` }
    }
  }

  if (node.type === 'image') {
    const allowedImageAttrs = new Set([
      'assetId',
      'uploadId',
      'nodeId',
      'alt',
      'caption',
      'displayWidthPx',
      'aspectRatio',
      'alignment',
      'decorative',
    ])
    const unsupportedAttr = Object.keys(attrs).find((key) => !allowedImageAttrs.has(key))
    if (unsupportedAttr) {
      return { ok: false, message: `${path}.attrs.${unsupportedAttr} is not supported` }
    }
    if (!isUuid(attrs.assetId)) {
      return { ok: false, message: `${path}.attrs.assetId must be a UUID` }
    }
    if (attrs.uploadId != null && !isUuid(attrs.uploadId)) {
      return { ok: false, message: `${path}.attrs.uploadId must be a UUID` }
    }
    if (typeof attrs.nodeId !== 'string' || attrs.nodeId.length === 0) {
      return { ok: false, message: `${path}.attrs.nodeId is required` }
    }
    if (
      typeof attrs.displayWidthPx !== 'number' ||
      attrs.displayWidthPx < 80 ||
      attrs.displayWidthPx > 1600
    ) {
      return { ok: false, message: `${path}.attrs.displayWidthPx is invalid` }
    }
    if (
      typeof attrs.aspectRatio !== 'number' ||
      attrs.aspectRatio < 0.01 ||
      attrs.aspectRatio > 100
    ) {
      return { ok: false, message: `${path}.attrs.aspectRatio is invalid` }
    }
    if (!['left', 'center', 'right'].includes(String(attrs.alignment))) {
      return { ok: false, message: `${path}.attrs.alignment is invalid` }
    }
    if (typeof attrs.decorative !== 'boolean') {
      return { ok: false, message: `${path}.attrs.decorative is required` }
    }
    if (attrs.decorative === false && (typeof attrs.alt !== 'string' || !attrs.alt.trim())) {
      return { ok: false, message: `${path}.attrs.alt is required for non-decorative images` }
    }
  }

  if (node.type === 'table' && node.content?.length) {
    if (node.content[0]?.type !== 'tableHeaderRow') {
      return { ok: false, message: `${path} must begin with a tableHeaderRow` }
    }
    if (node.content.slice(1).some((row) => row.type !== 'tableBodyRow')) {
      return { ok: false, message: `${path} body rows must use tableBodyRow` }
    }
  }

  return { ok: true }
}

function validateMarks(marks: RichTextMark[] | undefined, path: string): ValidationResult {
  if (!marks) {
    return { ok: true }
  }

  if (!Array.isArray(marks) || marks.length > 6) {
    return { ok: false, message: `${path}.marks is invalid` }
  }

  for (const mark of marks) {
    if (!ALLOWED_MARKS.has(mark.type)) {
      return { ok: false, message: `${path}.marks contains ${mark.type}` }
    }
    if (mark.type === 'link') {
      const href = mark.attrs?.href
      if (typeof href !== 'string' || !/^https?:\/\//i.test(href) || href.length > 2048) {
        return { ok: false, message: `${path}.link href must be an HTTP(S) URL` }
      }
    }
  }

  return { ok: true }
}

function validateNode(value: unknown, path: string, depth: number): ValidationResult {
  if (depth > MAX_DEPTH) {
    return { ok: false, message: `${path} exceeds maximum nesting depth` }
  }
  if (!isRecord(value)) {
    return { ok: false, message: `${path} must be an object` }
  }

  const node = value as unknown as RichTextNode
  if (typeof node.type !== 'string' || !ALLOWED_NODE_TYPES.has(node.type)) {
    return { ok: false, message: `${path}.type is not supported` }
  }

  const attributeResult = validateAttributes(node, path)
  if (!attributeResult.ok) {
    return attributeResult
  }

  if (node.type === 'text') {
    if (typeof node.text !== 'string') {
      return { ok: false, message: `${path}.text must be a string` }
    }
    return validateMarks(node.marks, path)
  }

  if (node.content != null) {
    if (!Array.isArray(node.content)) {
      return { ok: false, message: `${path}.content must be an array` }
    }
    for (let index = 0; index < node.content.length; index += 1) {
      const childResult = validateNode(node.content[index], `${path}.content[${index}]`, depth + 1)
      if (!childResult.ok) {
        return childResult
      }
    }
  }

  return { ok: true }
}

export function validateRichTextDocument(value: unknown): ValidationResult {
  if (!isRecord(value) || value.type !== 'doc' || !Array.isArray(value.content)) {
    return { ok: false, message: 'document must be a ProseMirror doc with content' }
  }

  return validateNode(value, 'document', 0)
}

export function isRichTextDocument(value: unknown): value is RichTextDocument {
  return validateRichTextDocument(value).ok
}

function inlineText(node: RichTextNode): string {
  if (node.type === 'text') {
    return node.text ?? ''
  }
  if (node.type === 'hardBreak') {
    return '\n'
  }
  return (node.content ?? []).map(inlineText).join('')
}

function nodeText(node: RichTextNode): string {
  if (node.type === 'text') {
    return node.text ?? ''
  }
  if (node.type === 'hardBreak') {
    return '\n'
  }
  if (node.type === 'image') {
    return String(node.attrs?.caption || node.attrs?.alt || '')
  }
  if (node.type === 'tableHeaderRow' || node.type === 'tableBodyRow') {
    return (node.content ?? [])
      .map((cell) => inlineText(cell).trim())
      .filter(Boolean)
      .join(' | ')
  }
  if (node.type === 'bulletList' || node.type === 'orderedList') {
    return (node.content ?? [])
      .map((item, index) => {
        const prefix = node.type === 'orderedList' ? `${index + 1}. ` : '- '
        return `${prefix}${nodeText(item).trim()}`
      })
      .join('\n')
  }
  if (node.type === 'codeBlock') {
    return inlineText({ ...node, type: 'paragraph' })
  }

  return (node.content ?? []).map(nodeText).join('')
}

export function richTextToPlainText(document: RichTextDocument): string {
  const blocks = document.content ?? []
  const text = blocks
    .map((node) => {
      if (node.type === 'bulletList' || node.type === 'orderedList') {
        return nodeText(node)
      }
      return nodeText(node).trim()
    })
    .filter(Boolean)
    .join('\n\n')

  return text.replace(/\n{3,}/g, '\n\n').trim()
}

export function collectImageAssetIds(document: RichTextDocument): string[] {
  const ids = new Set<string>()

  function visit(node: RichTextNode) {
    if (node.type === 'image' && isUuid(node.attrs?.assetId)) {
      ids.add(node.attrs.assetId)
    }
    node.content?.forEach(visit)
  }

  visit(document)
  return [...ids]
}

