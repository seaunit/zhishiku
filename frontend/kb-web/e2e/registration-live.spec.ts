import { expect, test } from '@playwright/test'

test.skip(
  process.env.KB_REGISTRATION_E2E !== '1',
  'requires a fresh local Spring Boot database instead of the initialized owner database',
)

test('registers the only owner through a real email verification code', async ({ page }) => {
  const [captchaResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().endsWith('/kb/api/v1/auth/captcha') &&
        response.request().method() === 'GET',
    ),
    page.goto('/kb/register'),
  ])
  const captcha = (await captchaResponse.json()) as {
    captchaId: string
    debugAnswer: string
  }

  await page.getByLabel('邮箱', { exact: true }).fill('owner@example.com')
  await page.getByLabel('图形验证码', { exact: true }).fill(captcha.debugAnswer)
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
  await page.getByLabel('密码', { exact: true }).fill('correct-horse-battery')
  await page
    .getByLabel('确认密码', { exact: true })
    .fill('correct-horse-battery')
  await page.getByRole('button', { name: '创建账户' }).click()

  await expect(page).toHaveURL(/\/kb\/login\?registered=1$/)
  await expect(page.getByRole('status')).toHaveText('账户创建成功，请登录。')
  await expect
    .poll(async () => {
      const response = await page.request.get('/kb/api/v1/auth/registration-status')
      return (await response.json()) as { open: boolean }
    })
    .toEqual({ open: false })
})
