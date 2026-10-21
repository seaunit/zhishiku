import { describe, expect, it } from 'vitest'
import { emptyRichTextDocument } from '@/types/rich-text'
import {
  DRAFT_TTL_MS,
  clearDraft,
  loadDraft,
  saveDraft,
  type DraftStorage,
} from './drafts'

function memoryStorage(): DraftStorage {
  const values = new Map<string, string>()
  return {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => {
      values.set(key, value)
    },
    removeItem: (key) => {
      values.delete(key)
    },
  }
}

describe('rich text drafts', () => {
  it('round-trips a validated document with contract version and item version', () => {
    const storage = memoryStorage()
    const bodyDocument = {
      type: 'doc' as const,
      content: [
        {
          type: 'paragraph',
          content: [{ type: 'text', text: '仍未保存的内容' }],
        },
      ],
    }

    saveDraft(storage, {
      itemId: 'item-1',
      expectedVersion: 4,
      title: '草稿标题',
      note: '备注',
      bodyDocument,
    })

    expect(loadDraft(storage, 'item-1')).toMatchObject({
      status: 'restored',
      draft: {
        title: '草稿标题',
        note: '备注',
        expectedVersion: 4,
        bodyDocument,
      },
    })
  })

  it('rejects expired or tampered drafts', () => {
    const storage = memoryStorage()
    saveDraft(storage, {
      itemId: 'item-1',
      expectedVersion: 1,
      title: '旧草稿',
      note: '',
      bodyDocument: emptyRichTextDocument(),
    })

    expect(loadDraft(storage, 'item-1', Date.now() + DRAFT_TTL_MS + 1)).toEqual({
      status: 'expired',
    })

    storage.setItem('zhishiku.editor.draft.item-1', '{"schemaVersion":1,"bodyDocument":{"type":"doc","content":[{"type":"script"}]}}')
    expect(loadDraft(storage, 'item-1')).toEqual({ status: 'invalid' })
  })

  it('clears only the requested draft', () => {
    const storage = memoryStorage()
    for (const itemId of ['one', 'two']) {
      saveDraft(storage, {
        itemId,
        expectedVersion: 1,
        title: itemId,
        note: '',
        bodyDocument: emptyRichTextDocument(),
      })
    }

    clearDraft(storage, 'one')
    expect(loadDraft(storage, 'one')).toEqual({ status: 'missing' })
    expect(loadDraft(storage, 'two').status).toBe('restored')
  })
})
