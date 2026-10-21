import { describe, expect, it } from 'vitest'
import type { RichTextMark, RichTextNode } from '@/types/rich-text'
import {
  imageAccessibleText,
  safeLinkHref,
  tableDimensions,
} from '../rendering'
import { validateRichTextDocument } from '@/lib/rich-text'

describe('rich text read-only rendering helpers', () => {
  it('renders only safe HTTP(S) link targets', () => {
    expect(safeLinkHref(linkMark('https://example.com'))).toBe('https://example.com/')
    expect(safeLinkHref(linkMark('javascript:alert(1)'))).toBeNull()
    expect(safeLinkHref(linkMark('http://127.0.0.1/private'))).toBeNull()
  })

  it('uses visible fallback text for unavailable images', () => {
    const image: RichTextNode = {
      type: 'image',
      attrs: {
        alt: '',
        caption: '架构说明',
        decorative: false,
      },
    }

    expect(imageAccessibleText(image)).toBe('架构说明')
  })

  it('summarizes semantic table dimensions', () => {
    const table: RichTextNode = {
      type: 'table',
      attrs: { headerRow: true },
      content: [
        {
          type: 'tableHeaderRow',
          content: [{ type: 'tableHeader', content: [] }],
        },
        {
          type: 'tableBodyRow',
          content: [{ type: 'tableCell', content: [] }],
        },
      ],
    }

    expect(tableDimensions(table)).toEqual({ rows: 2, columns: 1 })
  })

  it('rejects script nodes and unsafe link marks before rendering', () => {
    expect(
      validateRichTextDocument({
        type: 'doc',
        content: [{ type: 'script', text: 'alert(1)' }],
      }).ok,
    ).toBe(false)
    expect(
      validateRichTextDocument({
        type: 'doc',
        content: [
          {
            type: 'paragraph',
            content: [
              {
                type: 'text',
                text: '危险链接',
                marks: [
                  {
                    type: 'link',
                    attrs: { href: 'javascript:alert(1)' },
                  },
                ],
              },
            ],
          },
        ],
      }).ok,
    ).toBe(false)
  })
})

function linkMark(href: string): RichTextMark {
  return { type: 'link', attrs: { href } }
}
