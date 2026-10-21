import { expect, test } from '@playwright/test'

test.skip(
  process.env.KB_REGISTRATION_E2E !== '1',
  'requires a local Spring Boot stack with registration code exposure enabled',
)

test('registers an account through a real email verification code', async ({ page }) => {
  const email = `owner-${Date.now()}@example.com`
  await page.goto('/kb/register')
  await expect(page.getByLabel('图形验证码', { exact: true })).toHaveCount(0)
  await page.getByLabel('邮箱', { exact: true }).fill(email)
  const [emailCodeResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().endsWith('/kb/api/v1/auth/register/email-code') &&
        response.request().method() === 'POST',
    ),
    page.getByRole('button', { name: '发送邮箱验证码' }).click(),
  ])
  expect(emailCodeResponse.ok()).toBeTruthy()
  const issued = (await emailCodeResponse.json()) as { debugCode: string }

  await page.getByLabel('邮箱验证码', { exact: true }).fill(issued.debugCode)
  await page.getByLabel('密码', { exact: true }).fill('password')
  await page
    .getByLabel('确认密码', { exact: true })
    .fill('password')
  await page.getByRole('button', { name: '创建账户' }).click()

  await expect(page).toHaveURL(/\/kb\/login\?registered=1$/)
  await expect(page.getByRole('status')).toHaveText('账户创建成功，请登录。')
  await expect
    .poll(async () => {
      const response = await page.request.get('/kb/api/v1/auth/registration-status')
      return (await response.json()) as { open: boolean }
    })
    .toEqual({ open: true })
})
