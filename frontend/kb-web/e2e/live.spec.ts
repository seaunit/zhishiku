import { expect, test } from '@playwright/test'
import { randomBytes } from 'node:crypto'

test.skip(process.env.KB_LIVE_E2E !== '1', 'requires the local Spring Boot and PostgreSQL stack')
test.describe.configure({ timeout: 150_000 })

interface CaptchaPayload {
  captchaId: string
  image: string
  debugAnswer: string
}

async function registerAndLogin(page: import('@playwright/test').Page) {
  const suffix = `${Date.now()}-${randomBytes(4).toString('hex')}`
  const email = `chat.${suffix}@seaunit.site`
  const password = `${randomBytes(18).toString('base64url')}Aa1!`
  let captcha: CaptchaPayload | undefined
  await page.route('**/kb/api/v1/auth/captcha', async (route) => {
    const response = await route.fetch()
    const body = await response.text()
    captcha = JSON.parse(body) as CaptchaPayload
    await route.fulfill({ response, body })
  })

  await page.goto('/kb/register')
  await page.getByLabel('邮箱', { exact: true }).fill(email)
  const codeResponsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith('/kb/api/v1/auth/register/email-code') &&
      response.request().method() === 'POST',
  )
  await page.getByRole('button', { name: '发送邮箱验证码' }).click()
  const codeResponse = await codeResponsePromise
  expect(codeResponse.ok()).toBeTruthy()
  const code = (await codeResponse.json()) as { debugCode?: string }
  expect(code.debugCode).toMatch(/^\d{6}$/)

  await page.getByLabel('邮箱验证码').fill(code.debugCode!)
  await page.getByLabel('密码', { exact: true }).fill(password)
  await page.getByLabel('确认密码').fill(password)
  await page.getByRole('button', { name: '创建账户' }).click()
  await page.waitForURL(/\/kb\/login(?:\?.*)?$/)
  await expect.poll(() => captcha?.debugAnswer || '').toMatch(/^\w{5}$/)
  expect(captcha!.image).toMatch(/^data:image\/png;base64,/)
  await page.getByLabel('邮箱').fill(email)
  await page.getByLabel('密码').fill(password)
  await page.getByLabel('验证码', { exact: true }).fill(captcha!.debugAnswer)
  await page.getByRole('button', { name: '登录' }).click()
  await page.waitForURL('**/kb/')
  await page.unroute('**/kb/api/v1/auth/captcha')
  const consentResponse = await page.request.get('/kb/api/v1/ai-consents')
  expect(consentResponse.ok()).toBeTruthy()
  const consents = (await consentResponse.json()) as Array<{ scope: string }>
  expect(consents.some((consent) => consent.scope === 'chat')).toBe(true)
}

test('logs in, saves rich text with an image, and renders the detail', async ({ page }) => {
  const png = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9ZQmcAAAAASUVORK5CYII=',
    'base64',
  )

  await registerAndLogin(page)

  await page.goto('/kb/capture')
  await page.getByRole('textbox', { name: '标题', exact: true }).fill('真实全栈图文验证')
  await page.locator('.ProseMirror').click()
  await page.keyboard.type('从 Vue 经 Spring 保存到 PostgreSQL。')
  await page.locator('input[type=file]').setInputFiles({
    name: 'pixel.png',
    mimeType: 'image/png',
    buffer: png,
  })
  await page.locator('.editor-image img').waitFor({ state: 'visible' })
  await page.getByRole('button', { name: '保存知识' }).click()

  await page.waitForURL(/\/kb\/library\/[0-9a-f-]+$/)
  await expect(page.locator('.detail-main')).toContainText('从 Vue 经 Spring 保存到 PostgreSQL。')
  await expect(page.locator('.document-image img')).toBeVisible()
})

test('answers a greeting with general AI, saves it, and charges one point', async ({ page }) => {
  await registerAndLogin(page)

  const pointsBefore = await readAvailablePoints(page)
  await page.goto('/kb/chat')
  await page.getByLabel('向小识提问').fill('你好')
  await page.getByRole('button', { name: '发送 · 1 点' }).click()

  const answer = page
    .locator('.chat-message.is-assistant .chat-message-body')
    .filter({ hasNotText: /INSUFFICIENT_EVIDENCE|NEEDS_INTERNET/ })
    .last()
  await expect(answer).toBeVisible({ timeout: 75_000 })
  const answerText = (await answer.textContent())?.trim() || ''
  expect(answerText.length).toBeGreaterThan(2)
  expect(answerText).not.toContain('当前知识库')
  await expect(page.getByRole('button', { name: /联网搜索并回答 · 2 点/ })).toHaveCount(0)
  await expect(page.locator('.chat-sources')).toHaveCount(0)
  expect(pointsBefore - (await readAvailablePoints(page))).toBe(1)

  await page.reload()
  await expect(page.locator('.chat-message.is-assistant .chat-message-body').first()).toContainText(
    answerText,
  )
})

test('answers from real local knowledge, charges one point, and suggests internet only when needed', async ({
  page,
}) => {
  await registerAndLogin(page)

  await page.goto('/kb/capture')
  await page.getByRole('textbox', { name: '标题', exact: true }).fill('DeepSeek 出口限制政策')
  await page.locator('.ProseMirror').click()
  await page.keyboard.type(
    '美国发布禁令，限制 DeepSeek 模型和相关技术出口。识海聊天验收标记是海蓝七号。',
  )
  await page.getByRole('button', { name: '保存知识' }).click()
  await page.waitForURL(/\/kb\/library\/[0-9a-f-]+$/)

  const pointsBefore = await readAvailablePoints(page)
  await page.goto('/kb/chat')
  await page.evaluate(() => {
    const lengths: number[] = []
    const stream = document.querySelector('.chat-stream')
    const observer = new MutationObserver(() => {
      for (const body of document.querySelectorAll(
        '.chat-message.is-assistant .chat-message-body',
      )) {
        const length = body.textContent?.length || 0
        if (length > 0 && lengths[lengths.length - 1] !== length) {
          lengths.push(length)
        }
      }
    })
    if (stream) {
      observer.observe(stream, {
        subtree: true,
        childList: true,
        characterData: true,
      })
    }
    ;(window as unknown as { __chatStreamLengths: number[] }).__chatStreamLengths = lengths
    ;(window as unknown as { __chatObserver: MutationObserver }).__chatObserver = observer
  })
  await page.getByLabel('向小识提问').fill('美国对DeepSeek的禁令是什么？')
  await page.getByRole('button', { name: '发送 · 1 点' }).click()
  await expect(
    page
      .locator('.chat-message.is-assistant .chat-message-body')
      .filter({ hasText: /禁令|限制/ }),
  ).toBeVisible({ timeout: 75_000 })
  const streamLengths = await page.evaluate(
    () => (window as unknown as { __chatStreamLengths: number[] }).__chatStreamLengths,
  )
  expect(new Set(streamLengths).size).toBeGreaterThan(1)
  await expect(page.getByRole('link', { name: /DeepSeek 出口限制政策/ })).toBeVisible()
  expect(pointsBefore - (await readAvailablePoints(page))).toBe(1)

  const pointsBeforeMissing = await readAvailablePoints(page)
  await page.getByLabel('向小识提问').fill('今天北京天气怎么样？')
  await page.getByRole('button', { name: '发送 · 1 点' }).click()
  const internetButton = page.getByRole('button', { name: /联网搜索并回答 · 2 点/ })
  await expect(internetButton).toBeVisible({ timeout: 30_000 })
  expect(await readAvailablePoints(page)).toBe(pointsBeforeMissing)

  await internetButton.click()
  await expect(page.getByText('正在联网搜索并组织回答')).toHaveCount(0, {
    timeout: 90_000,
  })
  const internetCharge = pointsBeforeMissing - (await readAvailablePoints(page))
  const noInternetResult =
    (await page.getByText(/互联网没有返回可用结果|互联网结果不足以支持可靠回答/).count()) > 0
  if (noInternetResult) {
    expect(internetCharge).toBe(1)
  } else {
    expect(internetCharge).toBe(2)
    await expect(page.locator('.chat-sources a[href^="https://"]').last()).toBeVisible()
  }

  await page.reload()
  await expect(
    page
      .locator('.chat-message.is-assistant .chat-message-body')
      .filter({ hasText: /禁令|限制/ }),
  ).toBeVisible()
  await expect(page.getByRole('button', { name: /美国对DeepSeek的禁令/ }).first()).toBeVisible()
  await expect(page.locator('.chat-avatar.is-user').first()).toBeVisible()
  await expect(page.locator('.chat-avatar.is-assistant').first()).toBeVisible()
  await page.locator('.chat-stream').evaluate((element) => {
    element.scrollTop = 0
  })

  for (const viewport of [
    { width: 1440, height: 900 },
    { width: 768, height: 900 },
    { width: 390, height: 844 },
  ]) {
    await page.setViewportSize(viewport)
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
      ),
    ).toBe(true)
    await page.screenshot({
      path: `test-results/chat-live-${viewport.width}.png`,
      fullPage: true,
    })
  }
})

async function readAvailablePoints(page: import('@playwright/test').Page) {
  const response = await page.request.get('/kb/api/v1/billing/account')
  expect(response.ok()).toBeTruthy()
  const account = (await response.json()) as { availablePoints: number }
  return account.availablePoints
}
