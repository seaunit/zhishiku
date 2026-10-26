import { expect, test } from '@playwright/test'

const live = process.env.KB_LIVE_ADMIN_TEST === '1'

test.describe('administrator live environment', () => {
  test.skip(!live, 'Set KB_LIVE_ADMIN_TEST=1 to run against a local backend.')

  test('signs in with the administrator username and renders real account data', async ({
    page,
  }) => {
    const username = process.env.KB_LIVE_ADMIN_USERNAME || 'a115846'
    const password = process.env.KB_LIVE_ADMIN_PASSWORD
    expect(password, 'KB_LIVE_ADMIN_PASSWORD is required').toBeTruthy()

    const csrfResponse = await page.request.get('/kb/api/v1/auth/csrf')
    expect(csrfResponse.ok()).toBeTruthy()
    const csrf = (await csrfResponse.json()) as { token: string; headerName: string }

    const captchaResponse = await page.request.get('/kb/api/v1/auth/captcha')
    expect(captchaResponse.ok()).toBeTruthy()
    const captcha = (await captchaResponse.json()) as {
      captchaId: string
      debugAnswer?: string
    }
    expect(captcha.debugAnswer, 'local captcha debug answer is required').toBeTruthy()

    const loginResponse = await page.request.post('/kb/api/v1/auth/login', {
      headers: { [csrf.headerName]: csrf.token },
      data: {
        email: username,
        password,
        captchaId: captcha.captchaId,
        captchaCode: captcha.debugAnswer,
      },
    })
    expect(loginResponse.ok()).toBeTruthy()

    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/kb/admin/users')
    await expect(page.getByRole('heading', { name: '账号管理', exact: true })).toBeVisible()
    await expect(page.locator('.admin-account-row')).not.toHaveCount(0)
    await page.screenshot({
      path: '../../.taste-skill/admin-live/desktop.png',
      fullPage: true,
    })

    await page.getByRole('searchbox', { name: '搜索账号' }).fill('a115846')
    await expect(page.getByText('a115846@admin.seaunit.local')).toBeVisible()
    await page.setViewportSize({ width: 390, height: 844 })
    await expect(page.getByRole('heading', { name: '账号管理', exact: true })).toBeVisible()
    await page.screenshot({
      path: '../../.taste-skill/admin-live/mobile.png',
      fullPage: true,
    })
    await page.screenshot({
      path: '../../.taste-skill/admin-live/mobile-fold.png',
    })
  })
})
