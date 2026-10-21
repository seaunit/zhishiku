// @vitest-environment happy-dom

import { Editor } from '@tiptap/core'
import Link from '@tiptap/extension-link'
import Underline from '@tiptap/extension-underline'
import StarterKit from '@tiptap/starter-kit'
import { afterEach, describe, expect, it } from 'vitest'
import type { RichTextDocument, RichTextNode } from '@/types/rich-text'

const editors: Editor[] = []

function createEditor(): Editor {
  const editor = new Editor({
    extensions: [
      StarterKit.configure({
        heading: { levels: [2, 3, 4] },
      }),
      Underline,
      Link.configure({
        openOnClick: false,
        autolink: false,
        linkOnPaste: false,
      }),
    ],
    content: {
      type: 'doc',
      content: [
        {
          type: 'paragraph',
          content: [{ type: 'text', text: '格式验证' }],
        },
      ],
    },
  })
  editors.push(editor)
  return editor
}

function firstNode(editor: Editor): RichTextNode {
  const document = editor.getJSON() as RichTextDocument
  const node = document.content[0]
  expect(node).toBeDefined()
  return node as RichTextNode
}

function marks(editor: Editor): string[] {
  return (firstNode(editor).content?.[0]?.marks || []).map((mark) => mark.type)
}

afterEach(() => {
  while (editors.length) {
    editors.pop()?.destroy()
  }
})

describe('rich text formatting matrix', () => {
  it('applies H2, H3 and H4 headings', () => {
    const editor = createEditor()
    for (const level of [2, 3, 4] as const) {
      editor.commands.selectAll()
      editor.chain().toggleHeading({ level }).run()
      expect(firstNode(editor)).toMatchObject({ type: 'heading', attrs: { level } })
    }
  })

  it('applies every supported inline mark independently', () => {
    const cases = [
      ['toggleBold', 'bold'],
      ['toggleItalic', 'italic'],
      ['toggleUnderline', 'underline'],
      ['toggleStrike', 'strike'],
      ['toggleCode', 'code'],
    ] as const

    for (const [command, mark] of cases) {
      const editor = createEditor()
      editor.commands.selectAll()
      editor.chain()[command]().run()
      expect(marks(editor)).toContain(mark)
    }
  })

  it('applies a safe external link', () => {
    const editor = createEditor()
    editor.commands.selectAll()
    editor.chain().setLink({ href: 'https://example.com/research' }).run()

    expect(marks(editor)).toContain('link')
    expect(firstNode(editor).content?.[0]?.marks?.[0]?.attrs).toMatchObject({
      href: 'https://example.com/research',
      target: '_blank',
      rel: 'noopener noreferrer nofollow',
    })
  })

  it('creates bullet lists, ordered lists, quotes and code blocks', () => {
    const cases = [
      ['toggleBulletList', 'bulletList'],
      ['toggleOrderedList', 'orderedList'],
      ['toggleBlockquote', 'blockquote'],
      ['toggleCodeBlock', 'codeBlock'],
    ] as const

    for (const [command, type] of cases) {
      const editor = createEditor()
      editor.commands.selectAll()
      editor.chain()[command]().run()
      expect(firstNode(editor).type).toBe(type)
    }
  })

  it('clears headings and marks while preserving visible text', () => {
    const editor = createEditor()
    editor.commands.selectAll()
    editor.chain().toggleHeading({ level: 2 }).toggleBold().run()
    editor.chain().clearNodes().unsetAllMarks().run()

    expect(firstNode(editor).type).toBe('paragraph')
    expect(marks(editor)).toEqual([])
    expect(editor.getText()).toContain('格式验证')
  })

  it('supports undo and redo for formatting changes', () => {
    const editor = createEditor()
    editor.commands.selectAll()
    editor.chain().toggleBold().run()
    expect(marks(editor)).toContain('bold')

    editor.chain().undo().run()
    expect(marks(editor)).not.toContain('bold')

    editor.chain().redo().run()
    expect(marks(editor)).toContain('bold')
  })
})
