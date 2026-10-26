import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ApiError,
  API_BASE,
  confirmKnowledgeReview,
  createBillingOrder,
  grantAdminUserPoints,
  getCaptcha,
  getRegistrationStatus,
  importDocument,
  importScreenshot,
  importText,
  login,
  listAdminUsers,
  registerAccount,
  resetCsrfToken,
  searchKnowledge,
  sendRegistrationEmailCode,
  streamKnowledgeChat,
  uploadRichImage,
  updateAdminUserStatus,
} from './client'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const sessionStore = new Map<string, string>()

beforeEach(() => {
  sessionStore.clear()
  vi.stubGlobal('sessionStorage', {
    getItem: (key: string) => sessionStore.get(key) ?? null,
    setItem: (key: string, value: string) => sessionStore.set(key, value),
    removeItem: (key: string) => sessionStore.delete(key),
    clear: () => sessionStore.clear(),
  })
})

afterEach(() => {
  resetCsrfToken()
  sessionStore.clear()
  vi.unstubAllGlobals()
})

describe('API client errors', () => {
  it('preserves server error code, message, field errors, and request id', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          {
            code: 'VALIDATION_ERROR',
            message: 'Query is invalid',
            requestId: 'req-123',
            retryable: false,
            fieldErrors: { q: 'Too short' },
          },
          422,
        ),
      ),
    )

    await expect(searchKnowledge('x')).rejects.toMatchObject({
      name: 'ApiError',
      status: 422,
      code: 'VALIDATION_ERROR',
      message: 'Query is invalid',
      requestId: 'req-123',
      fieldErrors: { q: 'Too short' },
    })
  })

  it('uses a stable fallback when the server returns a non-JSON error', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('gateway unavailable', {
          status: 503,
          headers: { 'Content-Type': 'text/plain' },
        }),
      ),
    )

    const error = await searchKnowledge('ocean').catch((caught: unknown) => caught)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 503,
      code: 'HTTP_503',
      message: 'gateway unavailable',
    })
  })
})

describe('knowledge chat streaming', () => {
  it('parses token and completion events from the SSE response', async () => {
    const encoder = new TextEncoder()
    const stream = new ReadableStream<Uint8Array>({
      start(controller) {
        controller.enqueue(
          encoder.encode(
            'event: token\ndata: {"text":"根据知识"}\n\n' +
              'event: token\ndata: {"text":"库回答[1]。"}\n\n' +
              'event: complete\ndata: {"answer":"根据知识库回答[1]。","needsInternet":false,"sources":[],"reason":null}\n\n',
          ),
        )
        controller.close()
      },
    })
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ token: 'csrf-stream', headerName: 'X-XSRF-TOKEN' }),
      )
      .mockResolvedValueOnce(
        new Response(stream, {
          status: 200,
          headers: { 'Content-Type': 'text/event-stream' },
        }),
      )
    vi.stubGlobal('fetch', fetchMock)
    const tokens: string[] = []

    const answer = await streamKnowledgeChat(
      {
        query: '问题',
        history: [],
        externalAiConsent: true,
        billingTaskId: '123e4567-e89b-42d3-a456-426614174999',
      },
      {
        idempotencyKey: 'chat-stream-key-0001',
        onToken: (text) => {
          tokens.push(text)
        },
      },
    )

    expect(tokens).toEqual(['根据知识', '库回答[1]。'])
    expect(answer.answer).toBe('根据知识库回答[1]。')
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})

describe('CSRF handling', () => {
  it('confirms an AI review through the CSRF-protected item endpoint', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ token: 'csrf-review', headerName: 'X-XSRF-TOKEN' }),
      )
      .mockResolvedValueOnce(jsonResponse({ processingStatus: 'succeeded' }))
    vi.stubGlobal('fetch', fetchMock)

    await confirmKnowledgeReview('item-1')

    const request = fetchMock.mock.calls[1]
    expect(request[0]).toBe(`${API_BASE}/knowledge-items/item-1/review`)
    expect(request[1].method).toBe('POST')
    expect(request[1].headers.get('X-XSRF-TOKEN')).toBe('csrf-review')
  })

  it('fetches a token before multipart uploads and sends it in the configured header', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({
          token: 'csrf-token',
          headerName: 'X-XSRF-TOKEN',
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          uploadId: '123e4567-e89b-42d3-a456-426614174000',
          status: 'ready',
          scanState: 'clean',
          redactionState: 'none',
          mediaType: 'image/png',
          sourceSizeBytes: 12,
          sizeBytes: 12,
          contentHash: 'a'.repeat(64),
          normalizedWidth: 100,
          normalizedHeight: 100,
          aspectRatio: 1,
          expiresAt: '2026-10-21T12:00:00Z',
        }),
      )
    vi.stubGlobal('fetch', fetchMock)

    await uploadRichImage(new File(['image'], 'image.png', { type: 'image/png' }))

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      `${API_BASE}/auth/csrf`,
      expect.objectContaining({ credentials: 'include' }),
    )

    const uploadCall = fetchMock.mock.calls[1]
    expect(uploadCall[0]).toBe(`${API_BASE}/rich-text/image-uploads`)
    expect(uploadCall[1].method).toBe('POST')
    expect(uploadCall[1].body).toBeInstanceOf(FormData)
    expect(uploadCall[1].headers.get('X-XSRF-TOKEN')).toBe('csrf-token')
    expect(uploadCall[1].headers.get('Idempotency-Key')).toBeTruthy()
  })

  it('sends document files, explicit consent, and note as multipart form data', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ token: 'csrf-document', headerName: 'X-XSRF-TOKEN' }),
      )
      .mockResolvedValueOnce(jsonResponse({ items: [] }, 201))
    vi.stubGlobal('fetch', fetchMock)

    await importDocument(
      new File(['document'], 'brief.pdf', { type: 'application/pdf' }),
      true,
      '项目背景',
    )

    const request = fetchMock.mock.calls[1]
    expect(request[0]).toBe(`${API_BASE}/document-imports`)
    expect(request[1].headers.get('X-XSRF-TOKEN')).toBe('csrf-document')
    const form = request[1].body as FormData
    expect(form).toBeInstanceOf(FormData)
    expect(form.get('externalAiConsent')).toBe('true')
    expect(form.get('note')).toBe('项目背景')
    expect((form.get('file') as File).name).toBe('brief.pdf')
  })

  it('sends clipboard screenshots as multipart form data', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ token: 'csrf-screenshot', headerName: 'X-XSRF-TOKEN' }),
      )
      .mockResolvedValueOnce(jsonResponse({ items: [] }, 201))
    vi.stubGlobal('fetch', fetchMock)

    await importScreenshot(
      new File(['screenshot'], 'clipboard.png', { type: 'image/png' }),
      true,
      '截图背景',
    )

    const request = fetchMock.mock.calls[1]
    expect(request[0]).toBe(`${API_BASE}/screenshot-imports`)
    const form = request[1].body as FormData
    expect(form).toBeInstanceOf(FormData)
    expect(form.get('externalAiConsent')).toBe('true')
    expect(form.get('note')).toBe('截图背景')
    expect((form.get('file') as File).name).toBe('clipboard.png')
  })

  it('sends AI text with explicit consent and note', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ token: 'csrf-text', headerName: 'X-XSRF-TOKEN' }),
      )
      .mockResolvedValueOnce(jsonResponse({ items: [] }, 201))
    vi.stubGlobal('fetch', fetchMock)

    await importText('需要 AI 整理的文字内容', true, '文字备注')

    const request = fetchMock.mock.calls[1]
    expect(request[0]).toBe(`${API_BASE}/text-imports`)
    expect(JSON.parse(request[1].body as string)).toEqual({
      text: '需要 AI 整理的文字内容',
      externalAiConsent: true,
      note: '文字备注',
    })
  })
})

describe('billing request idempotency', () => {
  it('reuses the same order key after a retryable network failure', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ token: 'csrf-billing', headerName: 'X-XSRF-TOKEN' }),
      )
      .mockResolvedValueOnce(
        jsonResponse({ code: 'BILLING_BRIDGE_UNAVAILABLE', message: 'retry' }, 503),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          id: '123e4567-e89b-42d3-a456-426614174180',
          sku: 'kb_pack_9',
          status: 'PENDING',
        }),
      )
    vi.stubGlobal('fetch', fetchMock)

    await expect(createBillingOrder('kb_pack_9')).rejects.toBeInstanceOf(ApiError)
    await createBillingOrder('kb_pack_9')

    const orderCalls = fetchMock.mock.calls.filter(
      (call) => call[0] === `${API_BASE}/billing/orders`,
    )
    const firstKey = orderCalls[0][1].headers.get('Idempotency-Key')
    const retryKey = orderCalls[1][1].headers.get('Idempotency-Key')
    expect(firstKey).toBeTruthy()
    expect(retryKey).toBe(firstKey)
  })
})

describe('captcha login', () => {
  it('loads a server-side captcha challenge', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({
          captchaId: 'captcha-123',
          image: 'data:image/png;base64,abc',
          expiresInSeconds: 120,
        }),
      ),
    )

    await expect(getCaptcha()).resolves.toEqual({
      captchaId: 'captcha-123',
      image: 'data:image/png;base64,abc',
      expiresInSeconds: 120,
    })
  })

  it('sends the captcha id and code with the login request', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ token: 'csrf-login', headerName: 'X-XSRF-TOKEN' }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          ownerId: '123e4567-e89b-42d3-a456-426614174000',
          email: 'owner@seaunit.site',
          displayName: 'Owner',
          locale: 'zh-CN',
          csrfToken: 'csrf-session',
        }),
      )
    vi.stubGlobal('fetch', fetchMock)

    await login('owner@seaunit.site', 'correct-horse-battery', 'captcha-123', 'A1B2C')

    const loginCall = fetchMock.mock.calls[1]
    expect(loginCall[0]).toBe(`${API_BASE}/auth/login`)
    expect(JSON.parse(String(loginCall[1].body))).toEqual({
      email: 'owner@seaunit.site',
      password: 'correct-horse-battery',
      captchaId: 'captcha-123',
      captchaCode: 'A1B2C',
    })
  })
})

describe('administrator account management', () => {
  it('lists accounts with search, status, and pagination parameters', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        items: [],
        page: 2,
        limit: 20,
        totalItems: 0,
        totalPages: 0,
        enabledCount: 8,
        disabledCount: 1,
        totalConsumed: 220,
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await listAdminUsers('lin zhou', 2, 20, 'enabled')

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE}/admin/users?query=lin+zhou&page=2&limit=20&status=enabled`,
      expect.objectContaining({ credentials: 'include' }),
    )
  })

  it('uses CSRF and an idempotency key for status and point changes', async () => {
    const account = {
      ownerId: '123e4567-e89b-42d3-a456-426614174123',
      loginName: 'linzhou@seaunit.site',
      email: 'linzhou@seaunit.site',
      displayName: '林舟',
      role: 'USER',
      status: 'enabled',
      createdAt: '2026-10-18T08:00:00Z',
      knowledgeCount: 8,
      availablePoints: 94,
      reservedPoints: 0,
      lifetimeGranted: 220,
      lifetimeConsumed: 126,
    }
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ token: 'csrf-admin', headerName: 'X-XSRF-TOKEN' }))
      .mockResolvedValueOnce(jsonResponse(account))
      .mockResolvedValueOnce(jsonResponse(account, 201))
    vi.stubGlobal('fetch', fetchMock)

    await updateAdminUserStatus(account.ownerId, false, 'Policy review', 'status-key-000001')
    await grantAdminUserPoints(account.ownerId, 12, 'Compensation', 'points-key-000001')

    const statusRequest = fetchMock.mock.calls[1]
    expect(statusRequest[0]).toBe(`${API_BASE}/admin/users/${account.ownerId}/status`)
    expect(statusRequest[1].headers.get('X-XSRF-TOKEN')).toBe('csrf-admin')
    expect(statusRequest[1].headers.get('Idempotency-Key')).toBe('status-key-000001')

    const grantRequest = fetchMock.mock.calls[2]
    expect(grantRequest[0]).toBe(`${API_BASE}/admin/users/${account.ownerId}/point-grants`)
    expect(JSON.parse(String(grantRequest[1].body))).toEqual({
      points: 12,
      reason: 'Compensation',
    })
  })
})

describe('email registration', () => {
  it('loads whether registration is open', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonResponse({ open: true })),
    )

    await expect(getRegistrationStatus()).resolves.toEqual({ open: true })
  })

  it('sends email code with only the email address', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ token: 'csrf-register', headerName: 'X-XSRF-TOKEN' }),
      )
      .mockResolvedValueOnce(
        jsonResponse(
          { expiresInSeconds: 600, resendAfterSeconds: 60 },
          202,
        ),
      )
    vi.stubGlobal('fetch', fetchMock)

    await sendRegistrationEmailCode('owner@example.com')

    const request = fetchMock.mock.calls[1]
    expect(request[0]).toBe(`${API_BASE}/auth/register/email-code`)
    expect(request[1].headers.get('X-XSRF-TOKEN')).toBe('csrf-register')
    expect(JSON.parse(String(request[1].body))).toEqual({
      email: 'owner@example.com',
    })
  })

  it('submits password confirmation and email code during registration', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ token: 'csrf-register', headerName: 'X-XSRF-TOKEN' }),
      )
      .mockResolvedValueOnce(
        jsonResponse({ email: 'owner@example.com' }, 201),
      )
    vi.stubGlobal('fetch', fetchMock)

    await registerAccount({
      email: 'owner@example.com',
      password: 'correct-horse-battery',
      confirmPassword: 'correct-horse-battery',
      emailCode: '123456',
      locale: 'zh-CN',
    })

    const request = fetchMock.mock.calls[1]
    expect(request[0]).toBe(`${API_BASE}/auth/register`)
    expect(JSON.parse(String(request[1].body))).toEqual({
      email: 'owner@example.com',
      password: 'correct-horse-battery',
      confirmPassword: 'correct-horse-battery',
      emailCode: '123456',
      locale: 'zh-CN',
    })
  })
})

