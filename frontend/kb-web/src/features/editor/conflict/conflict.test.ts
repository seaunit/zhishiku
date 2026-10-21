import { describe, expect, it } from 'vitest'
import { ApiError } from '@/api/client'
import { isRichTextConflict } from './conflict'

describe('rich text conflict detection', () => {
  it('recognizes the version conflict API code', () => {
    expect(
      isRichTextConflict(
        new ApiError(409, {
          code: 'RICH_TEXT_VERSION_CONFLICT',
          message: 'Current version is 7.',
        }),
      ),
    ).toBe(true)
  })

  it('does not treat unrelated errors as version conflicts', () => {
    expect(isRichTextConflict(new ApiError(500, { code: 'INTERNAL_ERROR' }))).toBe(false)
    expect(isRichTextConflict(new Error('network'))).toBe(false)
  })
})
