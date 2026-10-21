import { createIdempotencyKey } from '@/lib/id'
import type {
  ApiErrorPayload,
  CaptchaResponse,
  CreateKnowledgeItemRequest,
  CsrfResponse,
  KnowledgeItem,
  KnowledgeItemPage,
  RichImageUpload,
  SearchResponse,
  SessionResponse,
  UpdateKnowledgeItemRequest,
} from '@/types/api'

export const API_BASE = '/kb/api/v1'
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

let csrfToken = ''
let csrfHeader = 'X-XSRF-TOKEN'

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

export async function logout(): Promise<void> {
  await request<void>('/auth/logout', { method: 'POST' })
  resetCsrfToken()
}

export function listKnowledgeItems(limit = 20): Promise<KnowledgeItemPage> {
  return request<KnowledgeItemPage>(`/knowledge-items?limit=${limit}`)
}

export function searchKnowledge(query: string, limit = 20): Promise<SearchResponse> {
  const params = new URLSearchParams({ q: query, limit: String(limit) })
  return request<SearchResponse>(`/search?${params.toString()}`)
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

