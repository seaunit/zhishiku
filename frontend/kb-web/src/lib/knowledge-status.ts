export type KnowledgeStatusTone = 'neutral' | 'review' | 'processing' | 'done' | 'failed'

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
