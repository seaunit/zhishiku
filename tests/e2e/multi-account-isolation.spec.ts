import { expect, test, type BrowserContext, type Page } from '../support/playwright'

const liveEnabled = process.env.KB_MULTI_ACCOUNT_E2E === '1'
const backendBase = process.env.KB_MULTI_ACCOUNT_API_URL || 'http://127.0.0.1:18082'
const password = 'multi-account-password'

test.skip(
  !liveEnabled,
  'set KB_MULTI_ACCOUNT_E2E=1 with a fresh local Spring Boot database',
)

async function proxyApiToBackend(page: Page) {
  await page.route('**/kb/api/**', async (route) => {
    const target = new URL(route.request().url())
    const backend = new URL(backendBase)
    target.protocol = backend.protocol
    target.hostname = backend.hostname
    target.port = backend.port
    const response = await route.fetch({ url: target.toString() })
    await route.fulfill({ response })
  })
}

async function registerAccount(context: BrowserContext, email: string) {
  const page = await context.newPage()
  await proxyApiToBackend(page)
  const [captchaResponse] = await Promise.all([
    page.waitForResponse((response) =>
      response.url().endsWith('/kb/api/v1/auth/captcha'),
    ),
    page.goto('/kb/register'),
  ])
  const captcha = (await captchaResponse.json()) as { debugAnswer: string }

  await page.getByLabel('邮箱', { exact: true }).fill(email)
  await page.getByLabel('图形验证码', { exact: true }).fill(captcha.debugAnswer)
  const [codeResponse] = await Promise.all([
    page.waitForResponse((response) =>
      response.url().endsWith('/kb/api/v1/auth/register/email-code'),
    ),
    page.getByRole('button', { name: '发送邮箱验证码' }).click(),
  ])
  const issued = (await codeResponse.json()) as { debugCode: string }

  await page.getByLabel('邮箱验证码', { exact: true }).fill(issued.debugCode)
  await page.getByLabel('密码', { exact: true }).fill(password)
  await page.getByLabel('确认密码', { exact: true }).fill(password)
  await page.getByRole('button', { name: '创建账户' }).click()
  await expect(page).toHaveURL(/\/kb\/login\?registered=1$/)
  await page.close()
}

async function loginAccount(context: BrowserContext, email: string) {
  const page = await context.newPage()
  await proxyApiToBackend(page)
  const [captchaResponse] = await Promise.all([
    page.waitForResponse((response) =>
      response.url().endsWith('/kb/api/v1/auth/captcha'),
    ),
    page.goto('/kb/login'),
  ])
  const captcha = (await captchaResponse.json()) as { debugAnswer: string }

  await page.getByLabel('邮箱').fill(email)
  await page.getByLabel('密码').fill(password)
  await page.getByLabel('验证码', { exact: true }).fill(captcha.debugAnswer)
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/kb\/$/)
  return page
}

test('keeps registered accounts and their knowledge isolated', async ({ browser }) => {
  const accountA = await browser.newContext()
  const accountB = await browser.newContext()
  const emailA = `account-a-${Date.now()}@example.com`
  const emailB = `account-b-${Date.now()}@example.com`

  try {
    await registerAccount(accountA, emailA)
    const pageA = await loginAccount(accountA, emailA)
    const created = await pageA.evaluate(async () => {
      const csrf = (await fetch('/kb/api/v1/auth/csrf').then((response) =>
        response.json(),
      )) as { token: string }
      const response = await fetch('/kb/api/v1/knowledge-items', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': csrf.token,
        },
        body: JSON.stringify({
          title: 'Account A private',
          note: 'only account A',
          bodyDocument: {
            type: 'doc',
            content: [
              {
                type: 'paragraph',
                content: [{ type: 'text', text: 'A-only secret' }],
              },
            ],
          },
          bodySource: { kind: 'native' },
        }),
      })
      return {
        status: response.status,
        body: (await response.json()) as { id: string },
      }
    })
    expect(created.status).toBe(201)

    await registerAccount(accountB, emailB)
    const pageB = await loginAccount(accountB, emailB)
    const isolation = await pageB.evaluate(async ({ itemId }) => {
      const list = await fetch('/kb/api/v1/knowledge-items', {
        credentials: 'include',
      })
      const listBody = (await list.json()) as { items: unknown[] }
      const direct = await fetch(`/kb/api/v1/knowledge-items/${itemId}`, {
        credentials: 'include',
      })
      const search = await fetch('/kb/api/v1/search?q=A-only', {
        credentials: 'include',
      })
      const searchBody = (await search.json()) as { items: unknown[] }
      return {
        listStatus: list.status,
        listCount: listBody.items.length,
        directStatus: direct.status,
        searchStatus: search.status,
        searchCount: searchBody.items.length,
      }
    }, { itemId: created.body.id })

    expect(isolation).toEqual({
      listStatus: 200,
      listCount: 0,
      directStatus: 400,
      searchStatus: 200,
      searchCount: 0,
    })
  } finally {
    await accountA.close()
    await accountB.close()
  }
})
