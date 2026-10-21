import { describe, expect, it } from 'vitest'
import {
  collectImageAssetIds,
  richTextToPlainText,
  validateRichTextDocument,
} from './rich-text'
import type { RichTextDocument } from '@/types/rich-text'

const validImage = '123e4567-e89b-42d3-a456-426614174000'

function documentWithImage(extraAttrs: Record<string, unknown> = {}): RichTextDocument {
  return {
    type: 'doc',
    content: [
      {
        type: 'paragraph',
        content: [{ type: 'text', text: 'Before' }],
      },
      {
        type: 'image',
        attrs: {
          assetId: validImage,
          uploadId: validImage,
          nodeId: 'node-1',
          alt: 'A diagram',
          caption: 'Diagram caption',
          displayWidthPx: 720,
          aspectRatio: 1.5,
          alignment: 'center',
          decorative: false,
          ...extraAttrs,
        },
      },
    ],
  }
}

describe('validateRichTextDocument', () => {
  it('accepts the supported ProseMirror subset and upload image attributes', () => {
    const document: RichTextDocument = {
      type: 'doc',
      content: [
        {
          type: 'heading',
          attrs: { level: 2 },
          content: [
            {
              type: 'text',
              text: 'Private knowledge',
              marks: [{ type: 'bold' }],
            },
          ],
        },
        {
          type: 'table',
          attrs: { headerRow: true },
          content: [
            {
              type: 'tableHeaderRow',
              content: [
                {
                  type: 'tableHeader',
                  content: [
                    {
                      type: 'paragraph',
                      content: [{ type: 'text', text: 'Name' }],
                    },
                  ],
                },
              ],
            },
            {
              type: 'tableBodyRow',
              content: [
                {
                  type: 'tableCell',
                  content: [
                    {
                      type: 'paragraph',
                      content: [{ type: 'text', text: 'Value' }],
                    },
                  ],
                },
              ],
            },
          ],
        },
        documentWithImage().content[1],
      ],
    }

    expect(validateRichTextDocument(document)).toEqual({ ok: true })
  })

  it('rejects unsupported remote image attributes', () => {
    const result = validateRichTextDocument(documentWithImage({ src: 'https://local.test/a.png' }))
    expect(result.ok).toBe(false)
    expect(result.message).toContain('attrs.src')
  })

  it('rejects a table whose first row is not a header row', () => {
    const result = validateRichTextDocument({
      type: 'doc',
      content: [
        {
          type: 'table',
          attrs: { headerRow: true },
          content: [
            {
              type: 'tableBodyRow',
              content: [
                {
                  type: 'tableCell',
                  content: [{ type: 'paragraph' }],
                },
              ],
            },
          ],
        },
      ],
    })

    expect(result.ok).toBe(false)
  })
})

describe('richTextToPlainText', () => {
  it('derives readable text without markup or image sources', () => {
    const document = documentWithImage()
    const plain = richTextToPlainText(document)

    expect(plain).toContain('Before')
    expect(plain).toContain('Diagram caption')
    expect(plain).not.toContain('https://')
    expect(plain).not.toContain('<')
  })

  it('renders list items and captions once', () => {
    const document: RichTextDocument = {
      type: 'doc',
      content: [
        {
          type: 'bulletList',
          content: [
            {
              type: 'listItem',
              content: [
                {
                  type: 'paragraph',
                  content: [{ type: 'text', text: 'First item' }],
                },
              ],
            },
            {
              type: 'listItem',
              content: [
                {
                  type: 'paragraph',
                  content: [{ type: 'text', text: 'Second item' }],
                },
              ],
            },
          ],
        },
      ],
    }

    expect(richTextToPlainText(document)).toBe('- First item\n- Second item')
  })
})

describe('collectImageAssetIds', () => {
  it('returns unique safe image references', () => {
    const document = documentWithImage()
    document.content.push(documentWithImage().content[1])
    expect(collectImageAssetIds(document)).toEqual([validImage])
  })
})

