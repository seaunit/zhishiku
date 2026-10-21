import type { RichTextDocument } from './rich-text'

export interface CsrfResponse {
  token: string
  headerName: string
  expiresAt?: string | null
}

export interface CaptchaResponse {
  captchaId: string
  image: string
  expiresInSeconds: number
  debugAnswer?: string
}

export interface SessionResponse {
  ownerId: string
  email: string
  locale: 'zh-CN' | 'en'
  csrfToken: string
}

export interface ClassificationAssignment {
  id: string
  name: string
  status: string
  origin: string
  confidence?: number | null
}

export interface KnowledgeSource {
  kind: string
  inputMethod: string
  url?: string | null
  provider?: string | null
  capturedAt?: string | null
}

export interface KnowledgeItemSummary {
  id: string
  title: string
  bodyPreview: string
  summary?: string | null
  processingStatus: string
  lifecycleState: string
  source: KnowledgeSource
  category?: ClassificationAssignment | null
  tags: ClassificationAssignment[]
  version: number
  createdAt: string
  updatedAt: string
}

export interface KnowledgeItem extends Omit<KnowledgeItemSummary, 'bodyPreview'> {
  body: string
  bodyDocument: RichTextDocument | null
  bodySchemaVersion: number | null
  bodyMigrationState: 'not_required' | 'pending' | 'completed' | 'failed'
  note: string
  redactionApplied: boolean
}

export interface KnowledgeItemPage {
  items: KnowledgeItemSummary[]
  nextCursor: string | null
}

export interface SearchHit {
  item: KnowledgeItemSummary
  matchedFields: string[]
  highlights: Record<string, string[]>
}

export interface SearchResponse {
  items: SearchHit[]
  nextCursor: string | null
  degraded: boolean
}

export interface CreateKnowledgeItemRequest {
  title: string
  bodyDocument: RichTextDocument
  note: string
}

export interface UpdateKnowledgeItemRequest extends CreateKnowledgeItemRequest {
  expectedVersion: number
}

export interface RichImageUpload {
  uploadId: string
  status: 'staged' | 'scanning' | 'ready' | 'rejected' | 'attached' | 'expired'
  scanState: 'pending' | 'clean' | 'rejected'
  redactionState: 'pending' | 'none' | 'redacted' | 'rejected'
  mediaType?: string
  sourceSizeBytes?: number
  sizeBytes?: number
  contentHash?: string
  normalizedWidth?: number
  normalizedHeight?: number
  aspectRatio?: number
  expiresAt: string
  errorCode?: string | null
}

export interface ApiErrorPayload {
  code?: string
  message?: string
  requestId?: string
  retryable?: boolean
  fieldErrors?: Record<string, string>
}

