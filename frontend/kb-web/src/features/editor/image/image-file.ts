export const MAX_RICH_IMAGE_BYTES = 25 * 1024 * 1024

export const RICH_IMAGE_MIME_TYPES = [
  'image/jpeg',
  'image/png',
  'image/webp',
  'image/heic',
  'image/heif',
] as const

const supportedTypes = new Set<string>(RICH_IMAGE_MIME_TYPES)

export type RichImageValidation =
  | { ok: true }
  | { ok: false; code: 'unsupported' | 'too-large' }

export function validateRichImageFile(file: {
  type: string
  size: number
}): RichImageValidation {
  if (!supportedTypes.has(file.type.toLowerCase())) {
    return { ok: false, code: 'unsupported' }
  }
  if (!Number.isFinite(file.size) || file.size <= 0) {
    return { ok: false, code: 'unsupported' }
  }
  if (file.size > MAX_RICH_IMAGE_BYTES) {
    return { ok: false, code: 'too-large' }
  }
  return { ok: true }
}
