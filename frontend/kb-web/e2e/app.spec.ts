import { expect, test } from '@playwright/test'

const summary = {
  id: '123e4567-e89b-42d3-a456-426614174000',
  title: '深海检索笔记',
  bodyPreview: '把搜索、采集和整理放在同一个工作流里。',
  summary: '一条关于个人知识检索的笔记。',
  processingStatus: 'succeeded',
  lifecycleState: 'active',
  source: { kind: 'manual', inputMethod: 'manual' },
  category: { id: 'cat-1', name: '产品设计', status: 'applied', origin: 'ai' },
  tags: [{ id: 'tag-1', name: '检索', status: 'applied', origin: 'ai' }],
  version: 1,
  createdAt: '2026-10-20T08:00:00Z',
  updatedAt: '2026-10-21T08:00:00Z',
}

test.beforeEach(async ({ page }) => {
  await page.route('**/kb/api/v1/knowledge-items**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ items: [summary], nextCursor: null }),
    })
  })

  await page.route('**/kb/api/v1/search?**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [{ item: summary, matchedFields: ['title'], highlights: {} }],
        nextCursor: null,
        degraded: false,
      }),
    })
  })
})

test('searches from the single-field home screen', async ({ page }) => {
  await page.goto('/kb/')
  await expect(page.getByRole('heading', { level: 1, name: '搜索知识' })).toBeVisible()
  await expect(page.getByText('深海检索笔记')).toBeVisible()

  await page.getByRole('searchbox', { name: '搜索全部知识' }).fill('深海')
  await page.getByRole('button', { name: '搜索' }).click()
  await expect(page.getByText('找到 1 条知识')).toBeVisible()
  await page.screenshot({ path: 'test-results/home-desktop.png', fullPage: true })
})

test('folds navigation into a touch-sized header on a phone', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/kb/')

  const menu = page.getByRole('button', { name: '打开菜单' })
  await expect(menu).toBeVisible()
  await expect(menu).toHaveCSS('width', '44px')
  await expect(menu).toHaveCSS('height', '44px')
  await page.screenshot({ path: 'test-results/home-mobile.png', fullPage: true })

  await menu.click()
  await expect(page.getByRole('navigation', { name: '主导航' })).toBeVisible()
  await page.waitForTimeout(250)
  await page.screenshot({ path: 'test-results/home-mobile-menu.png', fullPage: true })
})

test('logs in after fetching a CSRF token', async ({ page }) => {
  await page.route('**/kb/api/v1/auth/captcha', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        captchaId: 'captcha-login',
        image: 'data:image/png;base64,abc',
        expiresInSeconds: 120,
      }),
    })
  })
  await page.route('**/kb/api/v1/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'csrf-login', headerName: 'X-XSRF-TOKEN' }),
    })
  })
  await page.route('**/kb/api/v1/auth/login', async (route) => {
    expect(route.request().headers()['x-xsrf-token']).toBe('csrf-login')
    expect(route.request().postDataJSON()).toMatchObject({
      captchaId: 'captcha-login',
      captchaCode: 'A1B2C',
    })
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        ownerId: '123e4567-e89b-42d3-a456-426614174000',
        email: 'owner@seaunit.site',
        locale: 'zh-CN',
        csrfToken: 'csrf-session',
      }),
    })
  })

  await page.goto('/kb/login')
  await page.getByLabel('邮箱').fill('owner@seaunit.site')
  await page.getByLabel('密码').fill('correct-horse-battery')
  await page.getByLabel('验证码', { exact: true }).fill('A1B2C')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/kb\/$/)
  await expect(page.getByRole('heading', { level: 1, name: '搜索知识' })).toBeVisible()
})

test('keeps the captcha error visible after refreshing the challenge', async ({ page }) => {
  let captchaRequests = 0
  await page.route('**/kb/api/v1/auth/captcha', async (route) => {
    captchaRequests += 1
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        captchaId: `captcha-${captchaRequests}`,
        image: 'data:image/png;base64,abc',
        expiresInSeconds: 120,
      }),
    })
  })
  await page.route('**/kb/api/v1/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'csrf-login', headerName: 'X-XSRF-TOKEN' }),
    })
  })
  await page.route('**/kb/api/v1/auth/login', async (route) => {
    await route.fulfill({
      status: 422,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'CAPTCHA_INVALID',
        message: 'Captcha is invalid or expired.',
        requestId: 'request-1',
        retryable: false,
      }),
    })
  })

  await page.goto('/kb/login')
  await page.getByLabel('邮箱').fill('owner@seaunit.site')
  await page.getByLabel('密码').fill('correct-horse-battery')
  await page.getByLabel('验证码', { exact: true }).fill('WRONG')
  await page.getByRole('button', { name: '登录' }).click()

  await expect(page.getByRole('alert')).toHaveText('验证码错误或已过期，请重新输入。')
  expect(captchaRequests).toBe(2)
})

test('registers the only owner with email verification and password confirmation', async ({
  page,
}) => {
  let emailCodeRequest: Record<string, unknown> | undefined
  let registrationRequest: Record<string, unknown> | undefined
  await page.route('**/kb/api/v1/auth/registration-status', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ open: true }),
    })
  })
  await page.route('**/kb/api/v1/auth/captcha', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        captchaId: 'captcha-register',
        image: 'data:image/png;base64,abc',
        expiresInSeconds: 120,
      }),
    })
  })
  await page.route('**/kb/api/v1/auth/register/email-code', async (route) => {
    emailCodeRequest = route.request().postDataJSON() as Record<string, unknown>
    await route.fulfill({
      status: 202,
      contentType: 'application/json',
      body: JSON.stringify({
        expiresInSeconds: 600,
        resendAfterSeconds: 60,
        debugCode: '123456',
      }),
    })
  })
  await page.route('**/kb/api/v1/auth/register', async (route) => {
    registrationRequest = route.request().postDataJSON() as Record<string, unknown>
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({ email: 'owner@example.com' }),
    })
  })

  await page.goto('/kb/register')
  await expect(
    page.getByRole('heading', { level: 2, name: '创建识海账户' }),
  ).toBeVisible()
  await page.getByLabel('邮箱', { exact: true }).fill('owner@example.com')
  await page.getByLabel('图形验证码', { exact: true }).fill('A1B2C')
  await page.getByRole('button', { name: '发送邮箱验证码' }).click()
  await page.getByLabel('邮箱验证码', { exact: true }).fill('123456')
  await page.getByLabel('密码', { exact: true }).fill('correct-horse-battery')
  await page.getByLabel('确认密码', { exact: true }).fill('correct-horse-battery')
  await page.getByRole('button', { name: '创建账户' }).click()

  await expect(page).toHaveURL(/\/kb\/login\?registered=1$/)
  await expect(page.getByRole('status')).toHaveText('账户创建成功，请登录。')
  expect(emailCodeRequest).toMatchObject({
    email: 'owner@example.com',
    captchaId: 'captcha-register',
    captchaCode: 'A1B2C',
  })
  expect(registrationRequest).toMatchObject({
    email: 'owner@example.com',
    password: 'correct-horse-battery',
    confirmPassword: 'correct-horse-battery',
    emailCode: '123456',
    locale: 'zh-CN',
  })
})

test('rejects disposable email domains during registration', async ({ page }) => {
  await page.route('**/kb/api/v1/auth/registration-status', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ open: true }),
    })
  })
  await page.route('**/kb/api/v1/auth/captcha', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        captchaId: 'captcha-register',
        image: 'data:image/png;base64,abc',
        expiresInSeconds: 120,
      }),
    })
  })
  await page.route('**/kb/api/v1/auth/register/email-code', async (route) => {
    await route.fulfill({
      status: 422,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'EMAIL_DOMAIN_NOT_ALLOWED',
        message: 'Disposable email domains are not allowed.',
        requestId: 'request-2',
        retryable: false,
      }),
    })
  })

  await page.goto('/kb/register')
  await page
    .getByLabel('邮箱', { exact: true })
    .fill('daniel.parkerca61598b@mailinator.com')
  await page.getByLabel('图形验证码', { exact: true }).fill('A1B2C')
  await page.getByRole('button', { name: '发送邮箱验证码' }).click()

  await expect(page.getByRole('alert')).toHaveText(
    '不支持使用临时邮箱，请换用常用邮箱。',
  )
})

test('closes registration after the only owner exists', async ({ page }) => {
  await page.route('**/kb/api/v1/auth/registration-status', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ open: false }),
    })
  })
  await page.route('**/kb/api/v1/auth/captcha', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        captchaId: 'captcha-login',
        image: 'data:image/png;base64,abc',
        expiresInSeconds: 120,
      }),
    })
  })

  await page.goto('/kb/register')
  await expect(page.getByRole('heading', { name: '注册已关闭' })).toBeVisible()
  await expect(page.getByRole('button', { name: '创建账户' })).toHaveCount(0)

  await page.goto('/kb/login')
  await expect(page.getByRole('link', { name: '首次部署？创建账户' })).toHaveCount(0)
})

test('renders a rich text document recursively without v-html', async ({ page }) => {
  const item = {
    ...summary,
    body: '正文',
    bodyDocument: {
      type: 'doc',
      content: [
        {
          type: 'heading',
          attrs: { level: 2 },
          content: [{ type: 'text', text: '结构化正文' }],
        },
        {
          type: 'paragraph',
          content: [
            { type: 'text', text: '安全渲染', marks: [{ type: 'bold' }] },
          ],
        },
        {
          type: 'image',
          attrs: {
            assetId: '123e4567-e89b-42d3-a456-426614174001',
            nodeId: 'node-image',
            alt: '知识图片',
            caption: '图片说明',
            displayWidthPx: 720,
            aspectRatio: 1,
            alignment: 'center',
            decorative: false,
          },
        },
      ],
    },
    bodySchemaVersion: 1,
    bodyMigrationState: 'completed',
    note: '详情备注',
    redactionApplied: false,
  }

  await page.route('**/kb/api/v1/knowledge-items/123e4567-e89b-42d3-a456-426614174000', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(item),
    })
  })
  await page.route('**/kb/api/v1/attachments/**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'image/png',
      body: Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2nGQAAAAASUVORK5CYII=',
        'base64',
      ),
    })
  })

  await page.goto('/kb/library/123e4567-e89b-42d3-a456-426614174000')
  await expect(page.getByRole('heading', { level: 2, name: '结构化正文' })).toBeVisible()
  await expect(page.getByText('安全渲染')).toBeVisible()
  await expect(page.getByRole('img', { name: '知识图片' })).toBeVisible()
  await expect(page.getByText('图片说明')).toBeVisible()
  await page.screenshot({ path: 'test-results/detail-desktop.png', fullPage: true })
})

test('saves a structured Tiptap body from the capture screen', async ({ page }) => {
  let posted: Record<string, unknown> | undefined
  await page.route('**/kb/api/v1/knowledge-items', async (route) => {
    if (route.request().method() !== 'POST') {
      await route.fallback()
      return
    }
    posted = route.request().postDataJSON() as Record<string, unknown>
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        ...summary,
        body: 'Tiptap 正文',
        bodyDocument: {
          type: 'doc',
          content: [
            {
              type: 'paragraph',
              content: [{ type: 'text', text: 'Tiptap 正文' }],
            },
          ],
        },
        bodySchemaVersion: 1,
        bodyMigrationState: 'completed',
        note: '采集备注',
        redactionApplied: false,
      }),
    })
  })
  await page.route('**/kb/api/v1/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'csrf-create', headerName: 'X-XSRF-TOKEN' }),
    })
  })

  await page.goto('/kb/capture')
  await page.getByRole('textbox', { name: '标题', exact: true }).fill('Tiptap 采集测试')
  await page.locator('.ProseMirror').click()
  await page.keyboard.type('Tiptap 正文')
  await page.getByLabel('备注').fill('采集备注')
  await page.getByRole('button', { name: '保存知识' }).click()

  await expect(page).toHaveURL(/\/kb\/library\/123e4567-e89b-42d3-a456-426614174000/)
  expect(posted?.title).toBe('Tiptap 采集测试')
  expect(posted?.note).toBe('采集备注')
  expect((posted?.bodyDocument as { type?: string }).type).toBe('doc')
})

