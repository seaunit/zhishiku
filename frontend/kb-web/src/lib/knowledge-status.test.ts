import { describe, expect, it } from 'vitest'
import {
  knowledgeStatusTone,
  knowledgeStatusTranslationKey,
  PENDING_PROCESSING_STATUS_QUERY,
} from './knowledge-status'

describe('knowledge processing status presentation', () => {
  it('does not present a manual item as organized when processing was not requested', () => {
    expect(knowledgeStatusTranslationKey('not_requested')).toBe('processing.notRequested')
    expect(knowledgeStatusTone('not_requested')).toBe('neutral')
  })

  it('maps active, review, completed, and failed states explicitly', () => {
    expect(knowledgeStatusTranslationKey('queued')).toBe('processing.processing')
    expect(knowledgeStatusTranslationKey('running')).toBe('processing.processing')
    expect(knowledgeStatusTone('running')).toBe('processing')
    expect(knowledgeStatusTranslationKey('pending')).toBe('processing.review')
    expect(knowledgeStatusTone('review')).toBe('review')
    expect(knowledgeStatusTranslationKey('succeeded')).toBe('common.organized')
    expect(knowledgeStatusTone('succeeded')).toBe('done')
    expect(knowledgeStatusTranslationKey('failed')).toBe('processing.failed')
    expect(knowledgeStatusTone('failed')).toBe('failed')
  })

  it('keeps unknown statuses neutral and explicit', () => {
    expect(knowledgeStatusTranslationKey('future_state')).toBe('processing.unknown')
    expect(knowledgeStatusTone('future_state')).toBe('neutral')
  })

  it('uses one shared server-side status set for every pending-count surface', () => {
    expect(PENDING_PROCESSING_STATUS_QUERY).toBe(
      'pending,queued,running,processing,review,failed',
    )
  })
})
