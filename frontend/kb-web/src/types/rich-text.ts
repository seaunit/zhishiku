export interface RichTextMark {
  type: string
  attrs?: Record<string, unknown>
}

export interface RichTextNode {
  type: string
  attrs?: Record<string, unknown>
  content?: RichTextNode[]
  marks?: RichTextMark[]
  text?: string
}

export interface RichTextDocument extends RichTextNode {
  type: 'doc'
  content: RichTextNode[]
}

export interface ValidationResult {
  ok: boolean
  message?: string
}

export function emptyRichTextDocument(): RichTextDocument {
  return {
    type: 'doc',
    content: [{ type: 'paragraph' }],
  }
}

