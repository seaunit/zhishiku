import { expect, test } from '@playwright/test'

const adminSession = {
  ownerId: '33333333-3333-3333-3333-333333333333',
  loginName: 'a115846',
  email: 'a115846@admin.seaunit.local',
  displayName: '识海管理员',
  locale: 'zh-CN',
  admin: true,
  csrfToken: 'csrf-admin',
}

const activeUser = {
  ownerId: '44444444-4444-4444-4444-444444444444',
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

test.beforeEach(async ({ page }) => {
  let user = { ...activeUser }

  await page.route('**/kb/api/v1/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'csrf-admin', headerName: 'X-XSRF-TOKEN' }),
    })
  })

  await page.route('**/kb/api/v1/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(adminSession),
    })
  })

  await page.route('**/kb/api/v1/knowledge-items?**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [],
        nextCursor: null,
        pageInfo: { page: 1, pageSize: 1, totalItems: 0, totalPages: 0, sourceCounts: {} },
      }),
    })
  })

  await page.route('**/kb/api/v1/admin/users?**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [user],
        page: 1,
        limit: 20,
        totalItems: 1,
        totalPages: 1,
        enabledCount: user.status === 'enabled' ? 1 : 0,
        disabledCount: user.status === 'disabled' ? 1 : 0,
        totalConsumed: user.lifetimeConsumed,
      }),
    })
  })

  await page.route('**/kb/api/v1/admin/users/*/status', async (route) => {
    const payload = route.request().postDataJSON() as { enabled: boolean }
    user = {
      ...user,
      status: payload.enabled ? 'enabled' : 'disabled',
    }
    expect(route.request().headers()['idempotency-key']).toBeTruthy()
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(user),
    })
  })

  await page.route('**/kb/api/v1/admin/users/*/point-grants', async (route) => {
    const payload = route.request().postDataJSON() as { points: number }
    user = {
      ...user,
      availablePoints: user.availablePoints + payload.points,
      lifetimeGranted: user.lifetimeGranted + payload.points,
    }
    expect(route.request().headers()['idempotency-key']).toBeTruthy()
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify(user),
    })
  })
})

test('administrator can find an account, disable it, and grant points', async ({ page }) => {
  await page.goto('/kb/admin/users')

  await expect(page.getByRole('heading', { name: '账号管理', exact: true })).toBeVisible()
  await expect(page.getByRole('link', { name: '账号管理' })).toBeVisible()
  await expect(page.getByText('linzhou@seaunit.site')).toBeVisible()
  await expect(page.getByRole('article').getByText('126', { exact: true })).toBeVisible()

  const search = page.getByRole('searchbox', { name: '搜索账号' })
  await search.fill('lin')
  await expect(page.getByText('匹配 1 个账号')).toBeVisible()

  await page.getByRole('button', { name: '停用', exact: true }).click()
  await page.getByPlaceholder('说明本次状态变更原因').fill('Policy review')
  await page.getByRole('button', { name: '确认停用' }).click()
  await expect(page.getByText('账号状态已更新。')).toBeVisible()
  await expect(page.locator('.admin-account-row .admin-status')).toContainText('已停用')

  await page.getByRole('button', { name: '赠送', exact: true }).click()
  await page.getByRole('spinbutton', { name: '赠送点数' }).fill('120')
  await page.getByPlaceholder('例如：服务补偿').fill('Service compensation')
  await page.getByRole('button', { name: '确认赠送' }).click()
  await expect(page.getByText('已向该账号赠送 120 点。')).toBeVisible()
  await expect(page.getByRole('article').getByText('214', { exact: true })).toBeVisible()
})

test('keeps account controls readable at 320 pixels', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 760 })
  await page.goto('/kb/admin/users')

  await expect(page.getByRole('heading', { name: '账号管理', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '停用', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '赠送', exact: true })).toBeVisible()
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
  )
  expect(overflow).toBeLessThanOrEqual(0)
})
