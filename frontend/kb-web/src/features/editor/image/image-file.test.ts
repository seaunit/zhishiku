import { describe, expect, it } from 'vitest'
import {
  MAX_RICH_IMAGE_BYTES,
  validateRichImageFile,
} from './image-file'

describe('validateRichImageFile', () => {
  it('accepts supported static image formats at the size limit', () => {
    expect(
      validateRichImageFile({
        type: 'image/png',
        size: MAX_RICH_IMAGE_BYTES,
      }),
    ).toEqual({ ok: true })
  })

  it('rejects unsupported formats and oversized files', () => {
    expect(
      validateRichImageFile({
        type: 'image/svg+xml',
        size: 12,
      }),
    ).toMatchObject({ ok: false, code: 'unsupported' })

    expect(
      validateRichImageFile({
        type: 'image/jpeg',
        size: MAX_RICH_IMAGE_BYTES + 1,
      }),
    ).toMatchObject({ ok: false, code: 'too-large' })
  })
})
