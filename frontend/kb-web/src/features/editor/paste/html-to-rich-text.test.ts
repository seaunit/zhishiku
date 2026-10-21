import { describe, expect, it } from 'vitest'
import {
  detectPasteSource,
  plainTextToRichTextDocument,
  sanitizeExternalUrl,
} from './html-to-rich-text'

describe('paste source detection', () => {
  it('recognizes Word, Google Docs, and ordinary web pages', () => {
    expect(
      detectPasteSource({
        html: '<meta name="Generator" content="Microsoft Word 16">',
        text: 'Word',
      }),
    ).toBe('word')
    expect(
      detectPasteSource({
        html: '<b id="docs-internal-guid-demo">Google Docs</b>',
        text: 'Google Docs',
      }),
    ).toBe('google_docs')
    expect(
      detectPasteSource({
        html: '<article><p>Reference</p></article>',
        text: 'Reference',
        sourceUrl: 'https://example.com/article',
      }),
    ).toBe('webpage')
  })
})

describe('sanitizeExternalUrl', () => {
  it('keeps clean public HTTP(S) links', () => {
    expect(sanitizeExternalUrl('https://example.com/a?b=1#part')).toBe(
      'https://example.com/a?b=1#part',
    )
  })

  it('rejects credentials, non-HTTP schemes, and private destinations', () => {
    expect(sanitizeExternalUrl('https://user:pass@example.com/')).toBeNull()
    expect(sanitizeExternalUrl('javascript:alert(1)')).toBeNull()
    expect(sanitizeExternalUrl('http://127.0.0.1/admin')).toBeNull()
    expect(sanitizeExternalUrl('http://192.168.1.2/private')).toBeNull()
    expect(sanitizeExternalUrl('http://localhost:8080/')).toBeNull()
  })
})

describe('plainTextToRichTextDocument', () => {
  it('preserves paragraphs and hard line breaks using only supported nodes', () => {
    const document = plainTextToRichTextDocument('第一段\n第二行\n\n第二段')

    expect(document.type).toBe('doc')
    expect(document.content).toHaveLength(2)
    expect(document.content[0].content).toEqual([
      { type: 'text', text: '第一段' },
      { type: 'hardBreak' },
      { type: 'text', text: '第二行' },
    ])
  })
})
