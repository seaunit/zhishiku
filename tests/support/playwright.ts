import playwrightTest from '../../frontend/kb-web/node_modules/@playwright/test/index.js'

const { expect, request, test: base } = playwrightTest

const session = {
  ownerId: '123e4567-e89b-42d3-a456-426614174910',
  email: 'tester@example.com',
  displayName: '测试用户',
  locale: 'zh-CN',
  csrfToken: 'csrf-test',
}

export const test = base.extend({
  page: async ({ page }, use) => {
    await page.route('**/kb/api/v1/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(session),
      })
    })
    await page.route('**/kb/api/v1/auth/csrf', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ token: session.csrfToken, headerName: 'X-XSRF-TOKEN' }),
      })
    })
    await page.route('**/kb/api/v1/ai-consents', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(
          ['text', 'webpage', 'document', 'screenshot', 'search'].map((scope) => ({
            scope,
            service: scope === 'search' ? 'deepseek:search' : 'deepseek:chat',
            contentType: scope,
            granted: false,
            consentVersion: 'ai-consent-v1',
            updatedAt: null,
          })),
        ),
      })
    })
    await page.route('**/kb/api/v1/ai-consents/*', async (route) => {
      if (route.request().method() !== 'PUT') {
        await route.fallback()
        return
      }
      const scope = route.request().url().split('/').pop()
      const body = route.request().postDataJSON() as { granted: boolean }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          scope,
          service: scope === 'search' ? 'deepseek:search' : 'deepseek:chat',
          contentType: scope,
          granted: body.granted,
          consentVersion: 'ai-consent-v1',
          updatedAt: new Date().toISOString(),
        }),
      })
    })
    await use(page)
  },
})

export { expect, request }
export type {
  BrowserContext,
  Page,
  Route,
} from '../../frontend/kb-web/node_modules/@playwright/test/index.js'
