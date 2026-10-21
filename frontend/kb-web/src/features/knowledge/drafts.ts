import { RICH_TEXT_SCHEMA_VERSION } from '@/features/editor/contracts'
import { validateRichTextDocument } from '@/lib/rich-text'
import type { RichTextDocument } from '@/types/rich-text'

export const DRAFT_TTL_MS = 7 * 24 * 60 * 60 * 1000
const DRAFT_PREFIX = 'zhishiku.editor.draft.'

export interface DraftStorage {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
  removeItem(key: string): void
}

export interface SaveDraftInput {
  itemId?: string
  expectedVersion: number
  title: string
  note: string
  bodyDocument: RichTextDocument
}

export interface EditorDraft {
  schemaVersion: typeof RICH_TEXT_SCHEMA_VERSION
  itemId?: string
  expectedVersion: number
  title: string
  note: string
  bodyDocument: RichTextDocument
  savedAt: string
}

export type DraftLoadResult =
  | { status: 'missing' | 'expired' | 'invalid' }
  | { status: 'restored'; draft: EditorDraft }

export function draftKey(itemId?: string): string {
  return `${DRAFT_PREFIX}${itemId || 'new'}`
}

export function saveDraft(
  storage: DraftStorage,
  input: SaveDraftInput,
  now = new Date(),
): EditorDraft {
  const draft: EditorDraft = {
    schemaVersion: RICH_TEXT_SCHEMA_VERSION,
    itemId: input.itemId,
    expectedVersion: input.expectedVersion,
    title: input.title.slice(0, 300),
    note: input.note.slice(0, 20_000),
    bodyDocument: input.bodyDocument,
    savedAt: now.toISOString(),
  }
  storage.setItem(draftKey(input.itemId), JSON.stringify(draft))
  return draft
}

export function loadDraft(
  storage: DraftStorage,
  itemId?: string,
  now = Date.now(),
): DraftLoadResult {
  const key = draftKey(itemId)
  const serialized = storage.getItem(key)
  if (!serialized) {
    return { status: 'missing' }
  }

  let parsed: Partial<EditorDraft>
  try {
    parsed = JSON.parse(serialized) as Partial<EditorDraft>
  } catch {
    return { status: 'invalid' }
  }

  if (
    parsed.schemaVersion !== RICH_TEXT_SCHEMA_VERSION ||
    typeof parsed.title !== 'string' ||
    typeof parsed.note !== 'string' ||
    typeof parsed.expectedVersion !== 'number' ||
    typeof parsed.savedAt !== 'string' ||
    (itemId !== undefined && parsed.itemId !== itemId) ||
    !validateRichTextDocument(parsed.bodyDocument).ok
  ) {
    return { status: 'invalid' }
  }

  const savedAt = new Date(parsed.savedAt).getTime()
  if (!Number.isFinite(savedAt) || now - savedAt > DRAFT_TTL_MS) {
    return { status: 'expired' }
  }

  return {
    status: 'restored',
    draft: parsed as EditorDraft,
  }
}

export function clearDraft(storage: DraftStorage, itemId?: string) {
  storage.removeItem(draftKey(itemId))
}
