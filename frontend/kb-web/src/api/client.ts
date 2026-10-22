import { createIdempotencyKey } from '@/lib/id'
import type {
  ApiErrorPayload,
  AiConsent,
  AiConsentScope,
  BillingAccount,
  BillingAutoChargePolicy,
  BillingCatalog,
  BillingLedgerPage,
  BillingOrder,
  BillingOrderPage,
  BillingQuote,
  BillingRefundCase,
  BillingTask,
  BulkRecycleResponse,
  CaptchaResponse,
  CreateKnowledgeItemRequest,
  CsrfResponse,
  DocumentImportResponse,
  ExternalSearchResponse,
  ExternalSearchSaveResponse,
  KnowledgeItem,
  KnowledgeItemPage,
  ProfileResponse,
  RegistrationEmailCodeResponse,
  RegistrationRequest,
  RegistrationResponse,
  RegistrationStatusResponse,
  RichImageUpload,
  ScreenshotImportResponse,
  SearchResponse,
  SessionResponse,
  TextImportResponse,
  UpdateKnowledgeItemRequest,
  UpdateProfileRequest,
  WebPageImportResponse,
} from '@/types/api'

export const API_BASE = '/kb/api/v1'
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

let csrfToken = ''
let csrfHeader = 'X-XSRF-TOKEN'
const billingOrderKeys = new Map<string, string>()

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly requestId?: string
  readonly retryable: boolean
  readonly fieldErrors?: Record<string, string>

  constructor(status: number, payload: ApiErrorPayload = {}) {
    super(payload.message || `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.code = payload.code || `HTTP_${status}`
    this.requestId = payload.requestId
    this.retryable = payload.retryable ?? false
    this.fieldErrors = payload.fieldErrors
  }
}

function notifyUnauthorized(path: string) {
  if (typeof window === 'undefined' || path.startsWith('/auth/')) {
    return
  }
  window.dispatchEvent(new CustomEvent('kb:unauthorized'))
}

async function readErrorPayload(response: Response): Promise<ApiErrorPayload> {
  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    try {
      return (await response.json()) as ApiErrorPayload
    } catch {
      return {}
    }
  }

  const text = await response.text()
  return text ? { message: text.slice(0, 500) } : {}
}

export async function getCsrfToken(force = false): Promise<string> {
  if (csrfToken && !force) {
    return csrfToken
  }

  const response = await fetch(`${API_BASE}/auth/csrf`, {
    credentials: 'include',
    headers: { Accept: 'application/json' },
  })

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorPayload(response))
  }

  const payload = (await response.json()) as CsrfResponse
  if (!payload.token) {
    throw new ApiError(response.status, {
      code: 'CSRF_MISSING',
      message: 'The server did not return a CSRF token.',
    })
  }

  csrfToken = payload.token
  csrfHeader = payload.headerName || 'X-XSRF-TOKEN'
  return csrfToken
}

export function setCsrfToken(token: string, headerName = 'X-XSRF-TOKEN') {
  csrfToken = token
  csrfHeader = headerName
}

export function resetCsrfToken() {
  csrfToken = ''
  csrfHeader = 'X-XSRF-TOKEN'
}

interface RequestOptions extends RequestInit {
  skipAuthRedirect?: boolean
}

async function request<T>(path: string, init: RequestOptions = {}): Promise<T> {
  const method = (init.method || 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')

  if (!SAFE_METHODS.has(method)) {
    const token = await getCsrfToken()
    headers.set(csrfHeader, token)
  }

  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
    credentials: 'include',
  })

  if (response.status === 401 && !init.skipAuthRedirect) {
    notifyUnauthorized(path)
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorPayload(response))
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export function getCaptcha(): Promise<CaptchaResponse> {
  return request<CaptchaResponse>('/auth/captcha')
}

export function getRegistrationStatus(): Promise<RegistrationStatusResponse> {
  return request<RegistrationStatusResponse>('/auth/registration-status')
}

export function sendRegistrationEmailCode(
  email: string,
): Promise<RegistrationEmailCodeResponse> {
  return request<RegistrationEmailCodeResponse>('/auth/register/email-code', {
    method: 'POST',
    body: JSON.stringify({ email }),
  })
}

export function registerAccount(
  payload: RegistrationRequest,
): Promise<RegistrationResponse> {
  return request<RegistrationResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function login(
  email: string,
  password: string,
  captchaId: string,
  captchaCode: string,
): Promise<SessionResponse> {
  const session = await request<SessionResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password, captchaId, captchaCode }),
    skipAuthRedirect: true,
  })
  setCsrfToken(session.csrfToken)
  return session
}

export async function getCurrentSession(): Promise<SessionResponse> {
  const session = await request<SessionResponse>('/auth/me')
  setCsrfToken(session.csrfToken)
  return session
}

export async function logout(): Promise<void> {
  await request<void>('/auth/logout', { method: 'POST' })
  resetCsrfToken()
}

export function getProfile(): Promise<ProfileResponse> {
  return request<ProfileResponse>('/profile')
}

export function getAiConsents(): Promise<AiConsent[]> {
  return request<AiConsent[]>('/ai-consents')
}

export function updateAiConsent(
  scope: AiConsentScope,
  granted: boolean,
  consentVersion = 'ai-consent-v1',
): Promise<AiConsent> {
  return request<AiConsent>(`/ai-consents/${encodeURIComponent(scope)}`, {
    method: 'PUT',
    body: JSON.stringify({ granted, consentVersion }),
  })
}

export function getBillingCatalog(): Promise<BillingCatalog> {
  return request<BillingCatalog>('/billing/catalog')
}

export function getBillingAccount(): Promise<BillingAccount> {
  return request<BillingAccount>('/billing/account')
}

export function getBillingLedger(limit = 20): Promise<BillingLedgerPage> {
  return request<BillingLedgerPage>(`/billing/ledger?limit=${limit}`)
}

export function getBillingOrders(limit = 20): Promise<BillingOrderPage> {
  return request<BillingOrderPage>(`/billing/orders?limit=${limit}`)
}

export function getBillingOrder(orderId: string): Promise<BillingOrder> {
  return request<BillingOrder>(`/billing/orders/${encodeURIComponent(orderId)}`)
}

function billingOrderKey(sku: string): string {
  const storageKey = `kb:billing-order-key:${sku}`
  try {
    const stored = sessionStorage.getItem(storageKey)
    if (stored) return stored
    const created = createIdempotencyKey(`billing-order-${sku}`)
    sessionStorage.setItem(storageKey, created)
    return created
  } catch {
    const existing = billingOrderKeys.get(storageKey)
    if (existing) return existing
    const created = createIdempotencyKey(`billing-order-${sku}`)
    billingOrderKeys.set(storageKey, created)
    return created
  }
}

function clearBillingOrderKey(sku: string) {
  const storageKey = `kb:billing-order-key:${sku}`
  billingOrderKeys.delete(storageKey)
  try {
    sessionStorage.removeItem(storageKey)
  } catch {
    // In-memory fallback already cleared.
  }
}

export async function createBillingOrder(sku: string): Promise<BillingOrder> {
  const key = billingOrderKey(sku)
  const order = await request<BillingOrder>('/billing/orders', {
    method: 'POST',
    headers: { 'Idempotency-Key': key },
    body: JSON.stringify({ sku }),
  })
  clearBillingOrderKey(sku)
  return order
}

export function openBillingCheckout(
  orderId: string,
): Promise<{ orderId: string; checkoutUrl: string; expiresAt: string; environment: string }> {
  return request(`/billing/orders/${encodeURIComponent(orderId)}/checkout`, {
    method: 'POST',
  })
}

export function getAutoChargePolicy(): Promise<BillingAutoChargePolicy> {
  return request<BillingAutoChargePolicy>('/billing/auto-charge')
}

export function updateAutoChargePolicy(payload: {
  enabled: boolean
  maxPointsPerTask: number
  maxPointsPerDay: number
  consentTextVersion: string
  expectedVersion: number
}): Promise<BillingAutoChargePolicy> {
  return request<BillingAutoChargePolicy>('/billing/auto-charge', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function createBillingPreflight(payload: {
  kind: 'document' | 'screenshot' | 'webpage'
  mediaType: string
  sizeBytes: number
  pageCount?: number | null
  viewCount?: number
}): Promise<{
  preflightId: string
  measuredCharactersUpperBound: number
  viewCount: number
  pricingBasis: string
  expiresAt: string
}> {
  return request('/billing/preflights', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function createBillingQuote(payload: {
  operation: string
  itemId?: string | null
  preflightId?: string | null
  imageAssetIds?: string[]
  lines: Array<{ feature: string; quantity: number }>
  useAutoCharge?: boolean
}): Promise<BillingQuote> {
  return request<BillingQuote>('/billing/quotes', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function confirmBillingQuote(
  quoteId: string,
  expectedMaxPoints: number,
): Promise<BillingTask> {
  return request<BillingTask>(`/billing/quotes/${encodeURIComponent(quoteId)}/confirm`, {
    method: 'POST',
    body: JSON.stringify({ expectedMaxPoints, explicitConsent: true }),
  })
}

export function getBillingTask(taskId: string): Promise<BillingTask> {
  return request<BillingTask>(`/billing/tasks/${encodeURIComponent(taskId)}`)
}

export function cancelBillingTask(taskId: string): Promise<BillingTask> {
  return request<BillingTask>(`/billing/tasks/${encodeURIComponent(taskId)}/cancel`, {
    method: 'POST',
  })
}

export function getBillingRefunds(): Promise<BillingRefundCase[]> {
  return request<BillingRefundCase[]>('/billing/refunds')
}

export function updateProfile(payload: UpdateProfileRequest): Promise<ProfileResponse> {
  return request<ProfileResponse>('/profile', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function listKnowledgeItems(limit = 20): Promise<KnowledgeItemPage> {
  return request<KnowledgeItemPage>(`/knowledge-items?limit=${limit}`)
}

export function listTrash(limit = 100): Promise<KnowledgeItemPage> {
  return request<KnowledgeItemPage>(`/knowledge-items/trash?limit=${limit}`)
}

export function recycleKnowledgeItem(id: string): Promise<void> {
  return request<void>(`/knowledge-items/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
}

export function bulkRecycleKnowledgeItems(
  itemIds: string[],
  idempotencyKey = createIdempotencyKey('bulk-recycle'),
): Promise<BulkRecycleResponse> {
  return request<BulkRecycleResponse>('/knowledge-items/bulk-recycle', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify({ itemIds }),
  })
}

export function restoreKnowledgeItem(id: string): Promise<void> {
  return request<void>(`/knowledge-items/${encodeURIComponent(id)}/restore`, {
    method: 'POST',
  })
}

export function purgeKnowledgeItem(id: string): Promise<void> {
  return request<void>(`/knowledge-items/${encodeURIComponent(id)}/permanent`, {
    method: 'DELETE',
  })
}

export function searchKnowledge(query: string, limit = 20): Promise<SearchResponse> {
  const params = new URLSearchParams({ q: query, limit: String(limit) })
  return request<SearchResponse>(`/search?${params.toString()}`)
}

export function searchInternet(
  query: string,
  billingTaskId?: string,
  includeAnswer = false,
  idempotencyKey = createIdempotencyKey('internet-search'),
): Promise<ExternalSearchResponse> {
  return request<ExternalSearchResponse>('/search/external', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify({
      query,
      explicitConsent: true,
      includeAnswer,
      billingTaskId,
    }),
  })
}

export function saveInternetCandidate(
  candidateId: string,
  billingTaskId?: string,
  idempotencyKey = createIdempotencyKey('external-save'),
): Promise<ExternalSearchSaveResponse> {
  return request<ExternalSearchSaveResponse>(
    `/search/external/${encodeURIComponent(candidateId)}/save`,
    {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify({ externalAiConsent: true, billingTaskId }),
    },
  )
}

export function importText(
  text: string,
  externalAiConsent: boolean,
  note: string,
  billingTaskId?: string,
  idempotencyKey = createIdempotencyKey('text-import'),
): Promise<TextImportResponse> {
  return request<TextImportResponse>('/text-imports', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify({ text, externalAiConsent, note, billingTaskId }),
  })
}

export function importWebPage(
  url: string,
  externalAiConsent: boolean,
  billingTaskId?: string,
  idempotencyKey = createIdempotencyKey('web-import'),
): Promise<WebPageImportResponse> {
  return request<WebPageImportResponse>('/web-imports', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify({ url, externalAiConsent, billingTaskId }),
  })
}

export function importDocument(
  file: File,
  externalAiConsent: boolean,
  note: string,
  billingTaskId?: string,
  idempotencyKey = createIdempotencyKey('document-import'),
): Promise<DocumentImportResponse> {
  const form = new FormData()
  form.append('file', file, file.name)
  form.append('externalAiConsent', String(externalAiConsent))
  if (note.trim()) {
    form.append('note', note.trim())
  }
  if (billingTaskId) form.append('billingTaskId', billingTaskId)
  return request<DocumentImportResponse>('/document-imports', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: form,
  })
}

export function importScreenshot(
  file: File,
  externalAiConsent: boolean,
  note: string,
  billingTaskId?: string,
  idempotencyKey = createIdempotencyKey('screenshot-import'),
): Promise<ScreenshotImportResponse> {
  const form = new FormData()
  form.append('file', file, file.name || 'clipboard-screenshot.png')
  form.append('externalAiConsent', String(externalAiConsent))
  if (note.trim()) {
    form.append('note', note.trim())
  }
  if (billingTaskId) form.append('billingTaskId', billingTaskId)
  return request<ScreenshotImportResponse>('/screenshot-imports', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: form,
  })
}

export function getKnowledgeItem(id: string): Promise<KnowledgeItem> {
  return request<KnowledgeItem>(`/knowledge-items/${encodeURIComponent(id)}`)
}

export function createKnowledgeItem(payload: CreateKnowledgeItemRequest): Promise<KnowledgeItem> {
  return request<KnowledgeItem>('/knowledge-items', {
    method: 'POST',
    headers: { 'Idempotency-Key': createIdempotencyKey('create') },
    body: JSON.stringify(payload),
  })
}

export function updateKnowledgeItem(
  id: string,
  payload: UpdateKnowledgeItemRequest,
): Promise<KnowledgeItem> {
  return request<KnowledgeItem>(`/knowledge-items/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    headers: { 'Idempotency-Key': createIdempotencyKey('update') },
    body: JSON.stringify(payload),
  })
}

export function updateKnowledgeClassification(
  id: string,
  category: string,
  tags: string[],
): Promise<KnowledgeItem> {
  return request<KnowledgeItem>(
    `/knowledge-items/${encodeURIComponent(id)}/classification`,
    {
      method: 'PATCH',
      body: JSON.stringify({ category, tags }),
    },
  )
}

export function reanalyzeKnowledgeItem(
  id: string,
  billingTaskId: string,
  idempotencyKey: string,
): Promise<KnowledgeItem> {
  return request<KnowledgeItem>(
    `/knowledge-items/${encodeURIComponent(id)}/reanalyze`,
    {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify({ externalAiConsent: true, billingTaskId }),
    },
  )
}

export function uploadRichImage(file: File, itemId?: string): Promise<RichImageUpload> {
  const formData = new FormData()
  formData.append('file', file)
  if (itemId) {
    formData.append('itemId', itemId)
  }

  return request<RichImageUpload>('/rich-text/image-uploads', {
    method: 'POST',
    headers: { 'Idempotency-Key': createIdempotencyKey('image') },
    body: formData,
  })
}

export function getRichImageUpload(uploadId: string): Promise<RichImageUpload> {
  return request<RichImageUpload>(
    `/rich-text/image-uploads/${encodeURIComponent(uploadId)}`,
  )
}

export function imageContentUrl(assetId: string): string {
  return `${API_BASE}/attachments/${encodeURIComponent(assetId)}/content`
}

