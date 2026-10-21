import { expect, test } from '@playwright/test'

test.skip(process.env.KB_LIVE_E2E !== '1', 'requires the local Spring Boot and PostgreSQL stack')

test('logs in, saves rich text with an image, and renders the detail', async ({ page }) => {
  const png = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9ZQmcAAAAASUVORK5CYII=',
    'base64',
  )

  const [captchaResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().endsWith('/kb/api/v1/auth/captcha') &&
        response.request().method() === 'GET',
    ),
    page.goto('/kb/login'),
  ])
  expect(captchaResponse.ok()).toBeTruthy()
  const captcha = (await captchaResponse.json()) as {
    captchaId: string
    image: string
    debugAnswer: string
  }
  expect(captcha.image).toMatch(/^data:image\/png;base64,/)
  expect(captcha.debugAnswer).toHaveLength(5)
  await page.getByLabel('邮箱').fill('owner@zhishiku.local')
  await page.getByLabel('密码').fill('local-dev-password-change-me')
  await page.getByLabel('验证码', { exact: true }).fill(captcha.debugAnswer)
  await page.getByRole('button', { name: '登录' }).click()
  await page.waitForURL('**/kb/')

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
  await expect(page.locator('.detail-sheet')).toContainText('从 Vue 经 Spring 保存到 PostgreSQL。')
  await expect(page.locator('.document-image img')).toBeVisible()
})
