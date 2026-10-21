import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  ApiError,
  API_BASE,
  getCaptcha,
  getRegistrationStatus,
  login,
  registerAccount,
  resetCsrfToken,
  searchKnowledge,
  sendRegistrationEmailCode,
  uploadRichImage,
} from './client'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

afterEach(() => {
  resetCsrfToken()
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

describe('CSRF handling', () => {
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

