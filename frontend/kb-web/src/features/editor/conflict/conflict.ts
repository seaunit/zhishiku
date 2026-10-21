import { ApiError } from '@/api/client'

export function isRichTextConflict(error: unknown): error is ApiError {
  return (
    error instanceof ApiError &&
    error.status === 409 &&
    error.code === 'RICH_TEXT_VERSION_CONFLICT'
  )
}
