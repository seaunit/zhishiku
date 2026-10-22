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

export interface RegistrationStatusResponse {
  open: boolean
}

export interface RegistrationEmailCodeResponse {
  expiresInSeconds: number
  resendAfterSeconds: number
  debugCode?: string
}

export interface RegistrationRequest {
  email: string
  password: string
  confirmPassword: string
  emailCode: string
  locale: 'zh-CN' | 'en'
}

export interface RegistrationResponse {
  email: string
}

export interface SessionResponse {
  ownerId: string
  email: string
  displayName: string
  locale: 'zh-CN' | 'en'
  csrfToken: string
}

export interface ProfileResponse {
  ownerId: string
  email: string
  displayName: string
  locale: 'zh-CN' | 'en'
}

export interface UpdateProfileRequest {
  displayName: string
}

export type AiConsentScope = 'text' | 'webpage' | 'document' | 'screenshot' | 'search'

export interface AiConsent {
  scope: AiConsentScope
  service: string
  contentType: string
  granted: boolean
  consentVersion: string
  updatedAt: string | null
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
  pageInfo?: KnowledgePageInfo
}

export interface BulkRecycleResponse {
  recycled: number
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
  pageInfo?: KnowledgePageInfo
}

export interface KnowledgePageInfo {
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
  sourceCounts: Record<string, number>
}

export interface ExternalSearchCandidate {
  candidateId: string
  title: string
  url: string
  snippet: string
  provider: string
  organizable: boolean
  expiresAt: string
}

export interface ExternalSearchResponse {
  items: ExternalSearchCandidate[]
  answer?: string | null
}

export interface ExternalSearchSaveResponse {
  items: KnowledgeItem[]
}

export interface TextImportResponse {
  items: KnowledgeItem[]
}

export interface WebPageImportResponse {
  items: KnowledgeItem[]
}

export interface DocumentImportResponse {
  items: KnowledgeItem[]
}

export interface ScreenshotImportResponse {
  items: KnowledgeItem[]
}

export interface CreateKnowledgeItemRequest {
  title: string
  bodyDocument: RichTextDocument
  note: string
  bodySource?: BodySourceRequest | null
}

export interface UpdateKnowledgeItemRequest extends CreateKnowledgeItemRequest {
  expectedVersion: number
}

export interface BodySourceRequest {
  kind: string
  sourceUrl?: string
  capturedAt?: string
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

export type BillingEnvironment = 'test' | 'production'

export interface BillingPointPack {
  sku: 'kb_pack_9' | 'kb_pack_29' | 'kb_pack_99'
  name: string
  nameZh: string
  nameEn: string
  priceMinor: number
  currency: 'CNY'
  points: number
  oneTime: boolean
}

export interface BillingAiRate {
  feature: string
  unit: string
  pointsPerUnit: number
  description: string
  descriptionZh: string
  descriptionEn: string
}

export interface BillingCatalog {
  environment: BillingEnvironment
  billingEnabled: boolean
  versionCode: string
  packs: BillingPointPack[]
  aiRates: BillingAiRate[]
  refundPolicy: {
    supportUrl: string
    responseTarget: string
    automaticFullRefundRecovery: boolean
    partialRefundManualReview: boolean
  }
}

export interface BillingAccount {
  availablePoints: number
  reservedPoints: number
  trialPoints: number
  purchasedPoints: number
  updatedAt: string
}

export interface BillingOrder {
  id: string
  sku: string
  environment: BillingEnvironment
  amountMinor: number
  currency: 'CNY'
  points: number
  providerProductId: string | null
  status: string
  checkoutUrl: string | null
  paidAt: string | null
  closedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface BillingOrderPage {
  items: BillingOrder[]
  nextCursor: string | null
}

export interface BillingLedgerEntry {
  id: string
  operationType: string
  availableDelta: number
  reservedDelta: number
  consumedDelta: number
  availableAfter: number
  reservedAfter: number
  referenceType: string
  referenceId: string
  businessOperation: string | null
  description: string | null
  createdAt: string
}

export interface BillingLedgerPage {
  items: BillingLedgerEntry[]
  nextCursor: string | null
}

export interface BillingAutoChargePolicy {
  featureScope: 'classify'
  enabled: boolean
  maxPointsPerTask: number
  maxPointsPerDay: number
  timezone: string
  consentTextVersion: string
  version: number
  updatedAt: string
}

export interface BillingQuoteLine {
  feature: string
  unit: string
  quantity: number
  pointsPerUnit: number
  maxPoints: number
  successRule: string
}

export interface BillingQuote {
  quoteId: string
  status: string
  lines: BillingQuoteLine[]
  imageResources: Array<{
    assetId: string
    contentHash: string
    mediaType: string
    normalizedSizeBytes: number
  }>
  maxPoints: number
  availablePoints: number
  projectedRemainingPoints: number
  expiresAt: string
  autoChargeEligible: boolean
}

export interface BillingTask {
  id: string
  status: string
  lines: BillingQuoteLine[]
  maxPoints: number
  settledPoints: number
  reservedPoints: number
  expiresAt: string
  autoChargeApplied: boolean
  createdAt: string
  updatedAt: string
}

export interface BillingRefundCase {
  id: string
  orderId: string
  status: string
  refundAmountMinor: number
  expectedRecoveryPoints: number
  recoveredPoints: number
  unrecoveredPoints: number
  reviewDueAt: string
  version: number
  resolution: string | null
  createdAt: string
  resolvedAt: string | null
}

