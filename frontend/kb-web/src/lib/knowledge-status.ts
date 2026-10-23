export type KnowledgeStatusTone = 'neutral' | 'review' | 'processing' | 'done' | 'failed'

export const PENDING_PROCESSING_STATUSES = [
  'pending',
  'queued',
  'running',
  'processing',
  'review',
  'failed',
] as const

export const PENDING_PROCESSING_STATUS_QUERY = PENDING_PROCESSING_STATUSES.join(',')
export const REVIEW_QUEUE_CHANGED_EVENT = 'kb:review-queue-changed'

export function notifyReviewQueueChanged() {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(REVIEW_QUEUE_CHANGED_EVENT))
  }
}

export function knowledgeStatusTranslationKey(status: string) {
  if (status === 'not_requested') return 'processing.notRequested'
  if (status === 'failed') return 'processing.failed'
  if (status === 'queued' || status === 'running' || status === 'processing') {
    return 'processing.processing'
  }
  if (status === 'review' || status === 'pending') return 'processing.review'
  if (status === 'succeeded') return 'common.organized'
  return 'processing.unknown'
}

export function knowledgeStatusTone(status: string): KnowledgeStatusTone {
  if (status === 'failed') return 'failed'
  if (status === 'queued' || status === 'running' || status === 'processing') {
    return 'processing'
  }
  if (status === 'review' || status === 'pending') return 'review'
  if (status === 'succeeded') return 'done'
  return 'neutral'
}
