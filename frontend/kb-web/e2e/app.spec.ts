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

test('searches from the prototype-aligned home screen', async ({ page }) => {
  await page.goto('/kb/')
  await expect(page.getByRole('heading', { level: 1, name: '识海' })).toBeVisible()
  await expect(page.getByText('深海检索笔记')).toBeVisible()
  await expect(page.locator('body')).toHaveCSS('background-color', 'rgb(6, 21, 34)')
  const navigation = page.getByRole('navigation', { name: '主导航' })
  await expect(navigation).toBeVisible()
  for (const label of ['首页', '知识库', '采集', '待处理', '回收站', '设置']) {
    await expect(navigation.getByRole('link', { name: label, exact: true })).toBeVisible()
  }

  await page.getByRole('searchbox', { name: '搜索全部知识' }).fill('深海')
  await page.getByRole('button', { name: '潜入检索' }).click()
  await expect(page.getByText('找到 1 条知识')).toBeVisible()
  await page.screenshot({ path: 'test-results/home-desktop.png', fullPage: true })
})

test('uses the prototype bottom navigation on a phone', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/kb/')

  await expect(page.getByRole('button', { name: '打开菜单' })).toHaveCount(0)
  const navigation = page.getByRole('navigation', { name: '主导航' })
  await expect(navigation).toBeVisible()
  await expect(navigation.getByRole('link')).toHaveCount(6)
  await expect(navigation.getByRole('link', { name: '设置', exact: true })).toBeVisible()
  await page.screenshot({ path: 'test-results/home-mobile.png', fullPage: true })
})

test('opens every prototype navigation destination', async ({ page }) => {
  await page.goto('/kb/')
  const navigation = page.getByRole('navigation', { name: '主导航' })

  await navigation.getByRole('link', { name: '知识库', exact: true }).click()
  await expect(page).toHaveURL(/\/kb\/library$/)
  await expect(page.getByRole('heading', { level: 1, name: '知识库' })).toBeVisible()

  await navigation.getByRole('link', { name: '待处理', exact: true }).click()
  await expect(page).toHaveURL(/\/kb\/processing$/)
  await expect(page.getByRole('heading', { level: 1, name: '待处理' })).toBeVisible()

  await navigation.getByRole('link', { name: '回收站', exact: true }).click()
  await expect(page).toHaveURL(/\/kb\/trash$/)
  await expect(page.getByRole('heading', { level: 1, name: '回收站' })).toBeVisible()

  await navigation.getByRole('link', { name: '设置', exact: true }).click()
  await expect(page).toHaveURL(/\/kb\/settings$/)
  await expect(page.getByRole('heading', { level: 1, name: '设置' })).toBeVisible()
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
      password: 'password',
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
  await page.getByLabel('密码').fill('password')
  await page.getByLabel('验证码', { exact: true }).fill('A1B2C')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/kb\/$/)
  await expect(page.getByRole('heading', { level: 1, name: '识海' })).toBeVisible()
})

test('keeps the registration form available on an initialized instance', async ({
  page,
}) => {
  await page.route('**/kb/api/v1/auth/registration-status', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ open: false }),
    })
  })

  await page.goto('/kb/login')
  await page.getByRole('link', { name: '注册账户' }).click()

  await expect(page).toHaveURL(/\/kb\/register$/)
  await expect(
    page.getByRole('heading', { level: 2, name: '创建我的知识库账户' }),
  ).toBeVisible()
  await expect(page.getByLabel('邮箱', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '创建账户' })).toBeVisible()
  await expect(page.getByText('注册已关闭')).toHaveCount(0)
  await expect(page.getByLabel('图形验证码', { exact: true })).toHaveCount(0)
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

test('registers an account with email verification and password confirmation', async ({
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
    page.getByRole('heading', { level: 2, name: '创建我的知识库账户' }),
  ).toBeVisible()
  await expect(page.getByLabel('图形验证码', { exact: true })).toHaveCount(0)
  await expect(page.getByLabel('密码', { exact: true })).toHaveAttribute(
    'minlength',
    '8',
  )
  await page.getByLabel('邮箱', { exact: true }).fill('owner@example.com')
  await page.getByRole('button', { name: '发送邮箱验证码' }).click()
  await page.getByLabel('邮箱验证码', { exact: true }).fill('123456')
  await page.getByLabel('密码', { exact: true }).fill('password')
  await page.getByLabel('确认密码', { exact: true }).fill('password')
  await page.getByRole('button', { name: '创建账户' }).click()

  await expect(page).toHaveURL(/\/kb\/login\?registered=1$/)
  await expect(page.getByRole('status')).toHaveText('账户创建成功，请登录。')
  expect(emailCodeRequest).toMatchObject({
    email: 'owner@example.com',
  })
  expect(registrationRequest).toMatchObject({
    email: 'owner@example.com',
    password: 'password',
    confirmPassword: 'password',
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
  await page.getByRole('button', { name: '发送邮箱验证码' }).click()

  await expect(page.getByRole('alert')).toHaveText(
    '不支持使用临时邮箱，请换用常用邮箱。',
  )
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
  let savedItem: Record<string, unknown> | undefined
  await page.route('**/kb/api/v1/knowledge-items', async (route) => {
    if (route.request().method() !== 'POST') {
      await route.fallback()
      return
    }
    posted = route.request().postDataJSON() as Record<string, unknown>
    savedItem = {
      ...summary,
      body: 'Tiptap 粗体正文',
      bodyDocument: posted.bodyDocument,
      bodySchemaVersion: 1,
      bodyMigrationState: 'completed',
      note: '采集备注',
      redactionApplied: false,
    }
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify(savedItem),
    })
  })
  await page.route(
    '**/kb/api/v1/knowledge-items/123e4567-e89b-42d3-a456-426614174000',
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(savedItem),
      })
    },
  )
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
  await page.getByRole('button', { name: '粗体' }).click()
  await page.keyboard.type('Tiptap 粗体正文')
  await page.getByLabel('备注').fill('采集备注')
  await page.getByRole('button', { name: '保存知识' }).click()

  await expect(page).toHaveURL(/\/kb\/library\/123e4567-e89b-42d3-a456-426614174000/)
  expect(posted?.title).toBe('Tiptap 采集测试')
  expect(posted?.note).toBe('采集备注')
  expect((posted?.bodyDocument as { type?: string }).type).toBe('doc')
  expect(
    (
      posted?.bodyDocument as {
        content?: Array<{ content?: Array<{ marks?: Array<{ type?: string }> }> }>
      }
    ).content?.[0]?.content?.[0]?.marks?.[0]?.type,
  ).toBe('bold')
  await expect(page.locator('.rich-text-view strong')).toContainText('Tiptap 粗体正文')
})

test('applies rich text formats and restores a safe local draft', async ({ page }) => {
  await page.goto('/kb/capture')
  await page.getByRole('textbox', { name: '标题', exact: true }).fill('草稿恢复测试')

  const editor = page.locator('.ProseMirror')
  await editor.click()
  await page.getByRole('button', { name: '粗体' }).click()
  await page.keyboard.type('粗体内容')
  await page.getByRole('button', { name: '斜体' }).click()
  await page.keyboard.type('斜体内容')
  await page.getByRole('button', { name: '插入链接' }).click()
  await page.getByRole('textbox', { name: '链接地址' }).fill('https://example.com/reference')
  await page.getByRole('button', { name: '应用链接' }).click()
  await page.keyboard.type('链接内容')

  await expect(page.locator('.ProseMirror strong')).toContainText('粗体内容')
  await expect(page.locator('.ProseMirror em')).toContainText('斜体内容')
  await expect(page.locator('.ProseMirror a')).toHaveAttribute(
    'href',
    'https://example.com/reference',
  )
  await expect(page.getByTestId('draft-status')).toContainText('草稿已保存在本机')

  await page.reload()
  await expect(page.getByRole('textbox', { name: '标题', exact: true })).toHaveValue(
    '草稿恢复测试',
  )
  await expect(page.locator('.ProseMirror')).toContainText('粗体内容')
  await expect(page.locator('.ProseMirror strong')).toContainText('粗体内容')
})

test('sanitizes pasted HTML, preserves supported tables, and explains removed formats', async ({
  page,
}) => {
  await page.goto('/kb/capture')
  const editor = page.locator('.ProseMirror')
  await editor.click()

  await page.evaluate(() => {
    const transfer = new DataTransfer()
    transfer.setData(
      'text/html',
      [
        '<h2>粘贴标题</h2>',
        '<p><strong>保留粗体</strong><a href="javascript:alert(1)">危险链接</a></p>',
        '<iframe src="https://tracker.example"></iframe>',
        '<p><img src="https://tracker.example/pixel.png" alt="远程图"></p>',
        '<table><tr><td>项目</td><td>结果</td></tr><tr><td>A</td><td>B</td></tr></table>',
      ].join(''),
    )
    transfer.setData('text/plain', '粘贴标题 保留粗体 危险链接 项目 结果 A B')
    const event = new ClipboardEvent('paste', {
      bubbles: true,
      cancelable: true,
      clipboardData: transfer,
    })
    document.querySelector('.ProseMirror')?.dispatchEvent(event)
  })

  await expect(page.locator('.ProseMirror h2')).toContainText('粘贴标题')
  await expect(page.locator('.ProseMirror strong')).toContainText('保留粗体')
  await expect(page.getByTestId('paste-report')).toContainText('已移除')
  await expect(page.getByTestId('paste-report')).toContainText('iframe')
  await expect(page.getByText('图片未导入：远程图')).toBeVisible()
  await expect(page.locator('.ProseMirror iframe')).toHaveCount(0)
  await expect(page.locator('.ProseMirror a[href^="javascript"]')).toHaveCount(0)
  await expect(page.locator('.ProseMirror table')).toBeVisible()
  await expect(page.locator('.ProseMirror th')).toHaveCount(2)
})

test('pastes an image through the safe upload pipeline and removes its node', async ({ page }) => {
  const uploadId = '123e4567-e89b-42d3-a456-426614174099'
  await page.route('**/kb/api/v1/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'csrf-image', headerName: 'X-XSRF-TOKEN' }),
    })
  })
  await page.route('**/kb/api/v1/rich-text/image-uploads', async (route) => {
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        uploadId,
        status: 'ready',
        scanState: 'clean',
        redactionState: 'none',
        mediaType: 'image/png',
        sourceSizeBytes: 12,
        sizeBytes: 12,
        normalizedWidth: 1,
        normalizedHeight: 1,
        aspectRatio: 1,
        expiresAt: '2026-10-23T00:00:00Z',
      }),
    })
  })

  await page.goto('/kb/capture')
  await page.locator('.ProseMirror').click()
  await page.evaluate(() => {
    const bytes = Uint8Array.from(
      atob('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2nGQAAAAASUVORK5CYII='),
      (char) => char.charCodeAt(0),
    )
    const file = new File([bytes], 'paste.png', { type: 'image/png' })
    const transfer = new DataTransfer()
    transfer.items.add(file)
    document.querySelector('.ProseMirror')?.dispatchEvent(
      new ClipboardEvent('paste', {
        bubbles: true,
        cancelable: true,
        clipboardData: transfer,
      }),
    )
  })

  await expect(page.getByRole('img', { name: 'paste.png' })).toBeVisible()
  await page.getByRole('button', { name: '移除图片' }).click()
  await expect(page.getByRole('img', { name: 'paste.png' })).toHaveCount(0)

  const imageInput = page.getByLabel('选择正文图片')
  await expect(imageInput).toHaveAttribute(
    'accept',
    'image/jpeg,image/png,image/webp,image/heic,image/heif',
  )
  await imageInput.setInputFiles({
    name: 'chosen.png',
    mimeType: 'image/png',
    buffer: Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2nGQAAAAASUVORK5CYII=',
      'base64',
    ),
  })
  await expect(page.getByRole('img', { name: 'chosen.png' })).toBeVisible()
  await page
    .getByRole('textbox', { name: '替代文本', exact: true })
    .fill('选中图片替代文本')
  await page.getByRole('textbox', { name: '图片说明', exact: true }).fill('图片说明内容')
  await page.getByRole('spinbutton', { name: '显示宽度', exact: true }).fill('480')
  await page.getByRole('combobox', { name: '对齐方式', exact: true }).selectOption('left')
  await expect(page.getByText('图片说明内容')).toBeVisible()
  await expect(page.getByRole('img', { name: '选中图片替代文本' })).toBeVisible()
  await expect(page.locator('.editor-image')).toHaveAttribute(
    'style',
    /--image-width: 480px/,
  )
})

test('renders semantic detail tables and closes the image lightbox with Escape', async ({
  page,
}) => {
  const item = {
    ...summary,
    body: '正文',
    bodyDocument: {
      type: 'doc',
      content: [
        {
          type: 'table',
          attrs: { headerRow: true },
          content: [
            {
              type: 'tableHeaderRow',
              content: [
                {
                  type: 'tableHeader',
                  content: [
                    {
                      type: 'paragraph',
                      content: [{ type: 'text', text: '项目' }],
                    },
                  ],
                },
                {
                  type: 'tableHeader',
                  content: [
                    {
                      type: 'paragraph',
                      content: [{ type: 'text', text: '结果' }],
                    },
                  ],
                },
              ],
            },
            {
              type: 'tableBodyRow',
              content: [
                {
                  type: 'tableCell',
                  content: [
                    {
                      type: 'paragraph',
                      content: [{ type: 'text', text: '检索' }],
                    },
                  ],
                },
                {
                  type: 'tableCell',
                  content: [
                    {
                      type: 'paragraph',
                      content: [{ type: 'text', text: '命中' }],
                    },
                  ],
                },
              ],
            },
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
    note: '',
    redactionApplied: false,
  }

  await page.route(
    '**/kb/api/v1/knowledge-items/123e4567-e89b-42d3-a456-426614174000',
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(item),
      })
    },
  )
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
  await expect(page.getByRole('table')).toBeVisible()
  await expect(page.getByRole('columnheader', { name: '项目' })).toBeVisible()
  await expect(page.getByRole('cell', { name: '命中' })).toBeVisible()

  await page.getByRole('button', { name: '放大图片：知识图片' }).click()
  await expect(page.getByRole('dialog', { name: '图片预览' })).toBeVisible()
  await expect(page.getByRole('dialog')).toContainText('图片说明')
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog', { name: '图片预览' })).toHaveCount(0)
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth),
  ).toBe(true)

  await page.setViewportSize({ width: 390, height: 844 })
  await page.reload()
  await expect(page.getByRole('table')).toBeVisible()
  await page.getByRole('button', { name: '放大图片：知识图片' }).click()
  await expect(page.getByRole('dialog', { name: '图片预览' })).toBeVisible()
  await page.keyboard.press('Escape')
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth),
  ).toBe(true)
})

test('offers reload, copy, and explicit overwrite after a version conflict', async ({ page }) => {
  const id = '123e4567-e89b-42d3-a456-426614174000'
  let copied = false
  const item = {
    ...summary,
    body: '原文',
    bodyDocument: {
      type: 'doc',
      content: [
        {
          type: 'paragraph',
          content: [{ type: 'text', text: '原文' }],
        },
      ],
    },
    bodySchemaVersion: 1,
    bodyMigrationState: 'completed',
    note: '',
    redactionApplied: false,
  }

  await page.route(`**/kb/api/v1/knowledge-items/${id}`, async (route) => {
    if (route.request().method() === 'PATCH') {
      await route.fulfill({
        status: 409,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'RICH_TEXT_VERSION_CONFLICT',
          message: 'Current version is 2.',
          requestId: 'conflict-1',
          retryable: true,
        }),
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(item),
    })
  })
  await page.route('**/kb/api/v1/knowledge-items', async (route) => {
    if (route.request().method() === 'POST') {
      copied = true
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({ ...item, id: '123e4567-e89b-42d3-a456-426614174088' }),
      })
      return
    }
    await route.fallback()
  })
  await page.route('**/kb/api/v1/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'csrf-conflict', headerName: 'X-XSRF-TOKEN' }),
    })
  })

  await page.goto(`/kb/library/${id}/edit`)
  await page.getByRole('textbox', { name: '标题', exact: true }).fill('本地修改标题')
  await page.getByRole('button', { name: '保存修改' }).click()

  await expect(page.getByRole('alertdialog', { name: '检测到新版本' })).toBeVisible()
  await expect(page.getByRole('button', { name: '重新加载最新版本' })).toBeVisible()
  await expect(page.getByRole('button', { name: '明确覆盖' })).toBeVisible()
  await page.getByRole('button', { name: '另存为新知识' }).click()

  await expect(page).toHaveURL(/\/kb\/library\/123e4567-e89b-42d3-a456-426614174088$/)
  expect(copied).toBe(true)
})

test('keeps rich text controls reachable at 320, 360, 390, and 430 pixels', async ({
  page,
}) => {
  for (const width of [320, 360, 390, 430]) {
    await page.setViewportSize({ width, height: 780 })
    await page.goto('/kb/capture')

    const more = page.getByRole('button', { name: '更多格式' })
    await expect(more).toBeVisible()
    await expect(more).toHaveCSS('width', '44px')
    await expect(more).toHaveCSS('height', '44px')
    await more.click()
    await expect(page.getByRole('menu', { name: '更多格式' })).toBeVisible()
    await page.getByRole('button', { name: '清除格式' }).focus()
    await expect(page.getByRole('button', { name: '清除格式' })).toHaveCSS(
      'min-height',
      '44px',
    )

    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
      ),
    ).toBe(true)
  }

  await page.setViewportSize({ width: 844, height: 390 })
  await page.goto('/kb/capture')
  await expect(page.getByRole('button', { name: '更多格式' })).toBeVisible()
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
    ),
  ).toBe(true)
})

test('supports keyboard undo and table row and column editing', async ({ page }) => {
  await page.goto('/kb/capture')
  const editor = page.locator('.ProseMirror')
  await editor.click()
  await page.keyboard.type('撤销测试')
  await page.keyboard.press('Control+b')
  await page.keyboard.type('粗体')
  await expect(page.locator('.ProseMirror strong')).toContainText('粗体')

  await page.keyboard.press('Control+z')
  await expect(page.locator('.ProseMirror strong')).toHaveCount(0)
  await page.keyboard.press('Control+Shift+z')
  await expect(page.locator('.ProseMirror strong')).toContainText('粗体')

  await page.getByRole('button', { name: '更多格式' }).click()
  await page.getByRole('button', { name: '插入表格' }).click()
  await expect(page.locator('.ProseMirror table')).toBeVisible()
  await expect(page.locator('.ProseMirror tr')).toHaveCount(3)

  await page.locator('.ProseMirror td').first().click()
  await page.getByRole('button', { name: '更多格式' }).click()
  await page.getByRole('button', { name: '增加一行' }).click()
  await expect(page.locator('.ProseMirror tr')).toHaveCount(4)

  await page.getByRole('button', { name: '更多格式' }).click()
  await page.getByRole('button', { name: '增加一列' }).click()
  await expect(page.locator('.ProseMirror tr').first().locator('th')).toHaveCount(4)
})

test('recovers after a dropped image upload fails without losing text', async ({ page }) => {
  const uploadId = '123e4567-e89b-42d3-a456-426614174099'
  let attempts = 0
  await page.route('**/kb/api/v1/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'csrf-drop', headerName: 'X-XSRF-TOKEN' }),
    })
  })
  await page.route('**/kb/api/v1/rich-text/image-uploads', async (route) => {
    attempts += 1
    if (attempts === 1) {
      await route.fulfill({
        status: 422,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'IMAGE_REJECTED',
          message: '图片未通过安全检查。',
          requestId: 'image-rejected',
          retryable: true,
        }),
      })
      return
    }
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        uploadId,
        status: 'ready',
        scanState: 'clean',
        redactionState: 'none',
        mediaType: 'image/png',
        sourceSizeBytes: 12,
        sizeBytes: 12,
        normalizedWidth: 1,
        normalizedHeight: 1,
        aspectRatio: 1,
        expiresAt: '2026-10-23T00:00:00Z',
      }),
    })
  })

  await page.goto('/kb/capture')
  const editor = page.locator('.ProseMirror')
  await editor.click()
  await page.keyboard.type('拖放后仍保留的正文')
  await page.evaluate(() => {
      const bytes = Uint8Array.from(
        atob('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2nGQAAAAASUVORK5CYII='),
        (char) => char.charCodeAt(0),
      )
      const transfer = new DataTransfer()
      transfer.items.add(new File([bytes], 'drop.png', { type: 'image/png' }))
      document.querySelector('.ProseMirror')?.dispatchEvent(
        new DragEvent('drop', {
          bubbles: true,
          cancelable: true,
          dataTransfer: transfer,
          clientX: 120,
          clientY: 160,
        }),
      )
  })

  await expect(page.getByText('图片未通过安全检查。')).toBeVisible()
  await expect(editor).toContainText('拖放后仍保留的正文')
  await expect(page.getByRole('button', { name: '重试' })).toBeVisible()
  await page.getByRole('button', { name: '重试' }).click()
  await expect(page.getByRole('img', { name: 'drop.png' })).toBeVisible()
  expect(attempts).toBe(2)
})

test('keeps bilingual rich text labels and 200 percent text usable', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/kb/capture')
  await expect(page.getByRole('toolbar', { name: '正文格式工具栏' })).toBeVisible()

  await page.getByRole('button', { name: '切换到英文' }).click()
  await expect(page.getByRole('navigation', { name: 'Main navigation' })).toBeVisible()
  await expect(
    page.getByRole('navigation', { name: 'Main navigation' }).getByRole('link', {
      name: 'Capture',
      exact: true,
    }),
  ).toBeVisible()
  await expect(page.getByRole('toolbar', { name: 'Body formatting toolbar' })).toBeVisible()
  await page.getByRole('button', { name: 'More formats' }).click()
  await expect(page.getByRole('button', { name: 'Clear formatting' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Insert link' })).toBeVisible()

  await page.evaluate(() => {
    document.documentElement.style.fontSize = '200%'
  })
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
    ),
  ).toBe(true)
  await page.getByRole('button', { name: 'Clear formatting' }).focus()
  await expect(page.getByRole('button', { name: 'Clear formatting' })).toBeVisible()
})

test('exposes the complete supported formatting matrix', async ({ page }) => {
  await page.goto('/kb/capture')
  const editor = page.locator('.ProseMirror')
  await editor.click()

  await page.getByRole('button', { name: '二级标题' }).click()
  await page.keyboard.type('二级标题')
  await page.keyboard.press('Enter')
  await page.getByRole('button', { name: '三级标题' }).click()
  await page.keyboard.type('三级标题')
  await page.keyboard.press('Enter')

  await page.getByRole('button', { name: '更多格式' }).click()
  await page.getByRole('button', { name: '四级标题' }).click()
  await page.keyboard.type('四级标题')
  await page.keyboard.press('Enter')

  await page.getByRole('button', { name: '粗体' }).click()
  await page.keyboard.type('粗体')
  await page.getByRole('button', { name: '粗体' }).click()
  await page.getByRole('button', { name: '斜体' }).click()
  await page.keyboard.type('斜体')
  await page.getByRole('button', { name: '斜体' }).click()
  await page.getByRole('button', { name: '下划线' }).click()
  await page.keyboard.type('下划线')
  await page.getByRole('button', { name: '下划线' }).click()
  await page.getByRole('button', { name: '删除线' }).click()
  await page.keyboard.type('删除线')
  await page.getByRole('button', { name: '删除线' }).click()
  await page.keyboard.press('Enter')

  await page.getByRole('button', { name: '无序列表' }).click()
  await page.keyboard.type('无序项目')
  await page.keyboard.press('Enter')
  await page.keyboard.press('Enter')
  await page.getByRole('button', { name: '有序列表' }).click()
  await page.keyboard.type('有序项目')
  await page.keyboard.press('Enter')
  await page.keyboard.press('Enter')
  await page.getByRole('button', { name: '引用' }).click()
  await page.keyboard.type('引用内容')
  await page.keyboard.press('Enter')
  await page.keyboard.press('Enter')
  await page.getByRole('button', { name: '代码块' }).click()
  await page.keyboard.type('const value = 1')
  await page.keyboard.press('ArrowDown')

  await page.getByRole('button', { name: '更多格式' }).click()
  await page.getByRole('button', { name: '行内代码' }).click()
  await page.keyboard.type('行内代码')
  await page.getByRole('button', { name: '更多格式' }).click()
  await page.getByRole('button', { name: '行内代码' }).click()
  await page.getByRole('button', { name: '分隔线' }).click()

  const html = await editor.innerHTML()
  expect(html).toMatch(/<h2>二级标题<\/h2>/)
  expect(html).toMatch(/<h3>三级标题<\/h3>/)
  expect(html).toMatch(/<h4>四级标题<\/h4>/)
  expect(html).toMatch(/<strong>粗体/)
  expect(html).toMatch(/<em>斜体/)
  expect(html).toMatch(/<u>下划线/)
  expect(html).toMatch(/<s>/)
  expect(html).toContain('删除线')
  expect(html).toContain('无序项目')
  expect(html).toContain('有序项目')
  expect(html).toContain('引用内容')
  expect(html).toContain('const value = 1')
  expect(html).toContain('行内代码')
  await expect(page.locator('.ProseMirror hr')).toHaveCount(1)

  await editor.press('Control+a')
  await page.getByRole('button', { name: '更多格式' }).click()
  await page.getByRole('button', { name: '清除格式' }).click()
  await expect(page.locator('.ProseMirror')).toContainText('二级标题')
  await expect(page.locator('.ProseMirror h2')).toHaveCount(0)
  await expect(page.locator('.ProseMirror strong')).toHaveCount(0)
})

