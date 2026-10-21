import { connect } from 'node:tls'
import { expect, request, test } from '../support/playwright'

const liveEnabled = process.env.KB_ZERO_IMPACT_LIVE === '1'
const baseURL = process.env.KB_LIVE_BASE_URL || 'https://seaunit.site'

test.skip(!liveEnabled, 'set KB_ZERO_IMPACT_LIVE=1 to run read-only production checks')

test('keeps the existing routes, certificate, security headers and /kb cookie scope intact', async ({
  playwright,
}) => {
  const api = await request.newContext({
    baseURL,
    ignoreHTTPSErrors: false,
  })
  try {
    const expectations = [
      { path: '/', status: 200, contentType: 'text/html' },
      { path: '/app', status: 200, contentType: 'text/html' },
      { path: '/login', status: 200, contentType: 'text/html' },
      { path: '/bank/1', status: 200, contentType: 'text/html' },
      { path: '/pay', status: 200, contentType: 'text/html' },
      { path: '/apk', status: 200, contentType: 'text/html' },
      { path: '/kb/', status: 200, contentType: 'text/html' },
      { path: '/api', status: 301, contentType: 'text/html' },
      { path: '/api/health', status: 401, contentType: 'application/json' },
    ]

    for (const expectation of expectations) {
      const response = await api.get(expectation.path, { maxRedirects: 0 })
      expect(response.status(), expectation.path).toBe(expectation.status)
      expect(response.headers()['content-type'], expectation.path).toContain(
        expectation.contentType,
      )
    }

    const portal = await api.get('/')
    const portalHtml = await portal.text()
    expect(portalHtml).toContain('拾题')
    expect(portalHtml).toContain('识海')

    const health = await api.get('/kb/api/actuator/health')
    expect(health.status()).toBe(200)
    expect(await health.json()).toMatchObject({ status: 'UP' })

    const csrf = await api.get('/kb/api/v1/auth/csrf')
    expect(csrf.status()).toBe(200)
    const setCookie = csrf.headers()['set-cookie'] || ''
    expect(setCookie).toContain('Path=/kb')
    expect(setCookie).toContain('Secure')
    expect(setCookie).toContain('SameSite=Lax')
    expect(csrf.headers()['strict-transport-security']).toBeTruthy()
  } finally {
    await api.dispose()
  }

  const target = new URL(baseURL)
  const certificate = await new Promise<{
    validTo: string
    protocol: string | null
    authorized: boolean
  }>((resolve, reject) => {
    const socket = connect(
      {
        host: target.hostname,
        port: 443,
        servername: target.hostname,
        rejectUnauthorized: true,
      },
      () => {
        const peer = socket.getPeerCertificate()
        resolve({
          validTo: peer.valid_to,
          protocol: socket.getProtocol(),
          authorized: socket.authorized,
        })
        socket.end()
      },
    )
    socket.once('error', reject)
  })
  expect(certificate.authorized).toBe(true)
  expect(certificate.protocol).toMatch(/^TLSv1\.[23]$/)
  expect(new Date(certificate.validTo).getTime()).toBeGreaterThan(Date.now())

  void playwright
})

test('checks the internal payment sidecar when its read-only health URL is supplied', async ({
  request,
}) => {
  const sidecarURL = process.env.KB_SIDECAR_URL
  test.skip(!sidecarURL, 'KB_SIDECAR_URL is only available from the production host')
  const response = await request.get(sidecarURL as string)
  expect(response.ok()).toBe(true)
})
