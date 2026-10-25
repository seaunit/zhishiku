import { expect, test } from '@playwright/test'
import type { KnowledgeItem } from '@/types/api'
import type { RichTextDocument } from '@/types/rich-text'

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

const profile = {
  ownerId: '123e4567-e89b-42d3-a456-426614174099',
  email: 'linzhou@example.com',
  displayName: '林舟',
  locale: 'zh-CN',
}

const billingCatalog = {
  environment: 'test',
  billingEnabled: true,
  versionCode: '2026-10-mvp',
  packs: [
    {
      sku: 'kb_pack_9',
      name: '轻量加量包',
      nameZh: '轻量加量包',
      nameEn: 'Starter',
      priceMinor: 900,
      currency: 'CNY',
      points: 400,
      oneTime: true,
    },
    {
      sku: 'kb_pack_29',
      name: '常用加量包',
      nameZh: '常用加量包',
      nameEn: 'Standard',
      priceMinor: 2900,
      currency: 'CNY',
      points: 1400,
      oneTime: true,
    },
    {
      sku: 'kb_pack_99',
      name: '超值加量包',
      nameZh: '超值加量包',
      nameEn: 'Pro',
      priceMinor: 9900,
      currency: 'CNY',
      points: 5000,
      oneTime: true,
    },
  ],
  aiRates: [],
  refundPolicy: {
    supportUrl: 'https://seaunit.site/kb/billing',
    responseTarget: '3 个工作日',
    automaticFullRefundRecovery: true,
    partialRefundManualReview: true,
  },
}

function sseBody(events: Array<{ event: string; data: unknown }>) {
  return events
    .map(
      ({ event, data }) =>
        `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`,
    )
    .join('')
}

test.beforeEach(async ({ page }) => {
  await page.route('**/kb/api/v1/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'csrf-session', headerName: 'X-XSRF-TOKEN' }),
    })
  })

  await page.route('**/kb/api/v1/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        ...profile,
        csrfToken: 'csrf-session',
      }),
    })
  })

  await page.route('**/kb/api/v1/knowledge-items?**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [],
        nextCursor: null,
        pageInfo: { page: 1, pageSize: 1, totalItems: 0, totalPages: 1, sourceCounts: {} },
      }),
    })
  })

  await page.route('**/kb/api/v1/profile', async (route) => {
    if (route.request().method() === 'PATCH') {
      const payload = route.request().postDataJSON() as { displayName: string }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ ...profile, displayName: payload.displayName.trim() }),
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(profile),
    })
  })

  await page.route('**/kb/api/v1/ai-consents', async (route) => {
    if (route.request().method() !== 'GET') {
      await route.fallback()
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          scope: 'text',
          service: 'deepseek:chat',
          contentType: 'text',
          granted: false,
          consentVersion: 'ai-consent-v1',
          updatedAt: null,
        },
        {
          scope: 'webpage',
          service: 'deepseek:chat',
          contentType: 'webpage_screenshot',
          granted: false,
          consentVersion: 'ai-consent-v1',
          updatedAt: null,
        },
        {
          scope: 'document',
          service: 'deepseek:chat',
          contentType: 'document_text',
          granted: false,
          consentVersion: 'ai-consent-v1',
          updatedAt: null,
        },
        {
          scope: 'screenshot',
          service: 'deepseek:chat',
          contentType: 'screenshot_ocr',
          granted: false,
          consentVersion: 'ai-consent-v1',
          updatedAt: null,
        },
        {
          scope: 'search',
          service: 'deepseek:search',
          contentType: 'search_query',
          granted: false,
          consentVersion: 'ai-consent-v1',
          updatedAt: null,
        },
        {
          scope: 'chat',
          service: 'deepseek:chat',
          contentType: 'knowledge_chat',
          granted: false,
          consentVersion: 'ai-consent-v1',
          updatedAt: null,
        },
      ]),
    })
  })
  await page.route('**/kb/api/v1/ai-consents/*', async (route) => {
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

  await page.route('**/kb/api/v1/chat/conversations**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const conversationId = '123e4567-e89b-42d3-a456-426614174510'
    if (url.pathname.endsWith('/messages') && request.method() === 'POST') {
      const payload = request.postDataJSON() as {
        role: 'user' | 'assistant'
        content: string
        sources?: unknown[]
        needsInternet?: boolean
        reason?: string | null
      }
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          id: `123e4567-e89b-42d3-a456-${String(Date.now()).slice(-12)}`,
          role: payload.role,
          content: payload.content,
          sources: payload.sources || [],
          needsInternet: payload.needsInternet || false,
          reason: payload.reason || null,
          createdAt: new Date().toISOString(),
        }),
      })
      return
    }
    if (url.pathname.endsWith('/conversations') && request.method() === 'POST') {
      const payload = request.postDataJSON() as { title: string }
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          id: conversationId,
          title: payload.title,
          lastMessage: '',
          messageCount: 0,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }),
      })
      return
    }
    if (url.pathname.endsWith('/conversations') && request.method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ items: [] }),
      })
      return
    }
    if (request.method() === 'DELETE') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ deleted: true }),
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        conversation: {
          id: conversationId,
          title: '历史对话',
          lastMessage: '',
          messageCount: 0,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        },
        messages: [],
      }),
    })
  })

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

  await page.route('**/kb/api/v1/billing/catalog', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(billingCatalog),
    })
  })
  await page.route('**/kb/api/v1/billing/account', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        availablePoints: 100,
        reservedPoints: 0,
        trialPoints: 100,
        purchasedPoints: 0,
        updatedAt: new Date().toISOString(),
      }),
    })
  })
  await page.route('**/kb/api/v1/billing/ledger**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ items: [], nextCursor: null }),
    })
  })
  await page.route('**/kb/api/v1/billing/orders**', async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          id: '123e4567-e89b-42d3-a456-426614174180',
          sku: 'kb_pack_9',
          environment: 'test',
          amountMinor: 900,
          currency: 'CNY',
          points: 400,
          providerProductId: 'PROD_9',
          status: 'PENDING',
          checkoutUrl: null,
          paidAt: null,
          closedAt: null,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }),
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ items: [], nextCursor: null }),
    })
  })
  await page.route('**/kb/api/v1/billing/auto-charge', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        featureScope: 'classify',
        enabled: false,
        maxPointsPerTask: 15,
        maxPointsPerDay: 100,
        timezone: 'Asia/Shanghai',
        consentTextVersion: 'billing-auto-v1',
        version: 1,
        updatedAt: new Date().toISOString(),
      }),
    })
  })
  await page.route('**/kb/api/v1/billing/refunds', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: '[]',
    })
  })
  await page.route('**/kb/api/v1/billing/preflights', async (route) => {
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        preflightId: '123e4567-e89b-42d3-a456-426614174181',
        measuredCharactersUpperBound: 10000,
        viewCount: 1,
        pricingBasis: 'document_chars',
        expiresAt: new Date(Date.now() + 600000).toISOString(),
      }),
    })
  })
  await page.route('**/kb/api/v1/billing/quotes', async (route) => {
    const payload = route.request().postDataJSON() as {
      lines: Array<{ feature: string; quantity: number }>
    }
    const points: Record<string, number> = {
      ai_text: 5,
      webpage_vision: 10,
      document_extract: 10,
      screenshot_ocr: 10,
      internet_search: 1,
      classify: 5,
    }
    const lines = payload.lines.map((line) => ({
      feature: line.feature,
      unit: 'entry',
      quantity: line.quantity,
      pointsPerUnit: points[line.feature] || 1,
      maxPoints: (points[line.feature] || 1) * line.quantity,
      successRule: 'success',
    }))
    const maxPoints = lines.reduce((sum, line) => sum + line.maxPoints, 0)
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        quoteId: '123e4567-e89b-42d3-a456-426614174182',
        status: 'QUOTED',
        lines,
        imageResources: [],
        maxPoints,
        availablePoints: 100,
        projectedRemainingPoints: 100 - maxPoints,
        expiresAt: new Date(Date.now() + 600000).toISOString(),
        autoChargeEligible: false,
      }),
    })
  })
  await page.route('**/kb/api/v1/billing/quotes/*/confirm', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: '123e4567-e89b-42d3-a456-426614174183',
        status: 'RESERVED',
        lines: [],
        maxPoints: 15,
        settledPoints: 0,
        reservedPoints: 15,
        expiresAt: new Date(Date.now() + 600000).toISOString(),
        autoChargeApplied: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }),
    })
  })
})

test('opens the account menu instead of signing out when the avatar is clicked', async ({
  page,
}) => {
  let logoutRequests = 0
  await page.route('**/kb/api/v1/auth/logout', async (route) => {
    logoutRequests += 1
    await route.fulfill({ status: 204 })
  })

  await page.goto('/kb/')
  const avatar = page.getByRole('button', { name: '打开林舟的账户菜单' })
  await avatar.click()

  const menu = page.getByRole('menu', { name: '账户菜单' })
  await expect(menu).toBeVisible()
  await expect(menu.getByText('林舟')).toBeVisible()
  await expect(menu.getByText('linzhou@example.com')).toBeVisible()
  await expect(menu.getByRole('menuitem', { name: '个人资料' })).toBeVisible()
  await expect(menu.getByRole('menuitem', { name: '退出登录' })).toBeVisible()
  expect(logoutRequests).toBe(0)

  await page.getByRole('heading', { level: 1, name: '识海' }).click()
  await expect(menu).toHaveCount(0)
  await expect(avatar).toBeFocused()

  await avatar.click()
  await expect(menu).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(menu).toHaveCount(0)
  await expect(avatar).toBeFocused()

  await avatar.press('Enter')
  await expect(menu).toBeVisible()
  await menu.getByRole('menuitem', { name: '个人资料' }).click()
  await expect(page).toHaveURL(/\/kb\/profile$/)
  await expect(page.getByRole('heading', { level: 1, name: '个人资料' })).toBeVisible()
})

test('keeps the account menu and profile usable at 320 pixels', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 720 })
  await page.goto('/kb/')
  const avatar = page.getByRole('button', { name: '打开林舟的账户菜单' })
  await avatar.click()

  const menu = page.getByRole('menu', { name: '账户菜单' })
  const box = await menu.boundingBox()
  expect(box).not.toBeNull()
  expect(box!.x).toBeGreaterThanOrEqual(0)
  expect(box!.x + box!.width).toBeLessThanOrEqual(320)
  for (const item of await menu.getByRole('menuitem').all()) {
    expect((await item.boundingBox())!.height).toBeGreaterThanOrEqual(44)
  }

  await menu.getByRole('menuitem', { name: '个人资料' }).click()
  await expect(page.getByLabel('显示名称')).toBeVisible()
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
  ).toBe(true)
})

test('edits the display name and immediately updates the account menu', async ({ page }) => {
  let updatePayload: Record<string, unknown> | undefined
  await page.route('**/kb/api/v1/profile', async (route) => {
    if (route.request().method() !== 'PATCH') {
      await route.fallback()
      return
    }
    updatePayload = route.request().postDataJSON() as Record<string, unknown>
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ ...profile, displayName: '海风' }),
    })
  })

  await page.goto('/kb/profile')
  await expect(page.getByLabel('登录邮箱')).toHaveValue('linzhou@example.com')
  await page.getByLabel('显示名称').fill('海风')
  await page.getByRole('button', { name: '保存修改' }).click()
  await expect(page.getByText('个人资料已保存')).toBeVisible()
  expect(updatePayload).toMatchObject({ displayName: '海风' })

  await page.getByRole('button', { name: '打开海风的账户菜单' }).click()
  await expect(page.getByRole('menu', { name: '账户菜单' }).getByText('海风')).toBeVisible()
})

test('accepts 40 Unicode display-name code points and clears stale saved status', async ({
  page,
}) => {
  const emojiName = '界'.repeat(39) + '😀'
  let updatePayload: Record<string, unknown> | undefined
  await page.route('**/kb/api/v1/profile', async (route) => {
    if (route.request().method() !== 'PATCH') {
      await route.fallback()
      return
    }
    updatePayload = route.request().postDataJSON() as Record<string, unknown>
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ ...profile, displayName: emojiName }),
    })
  })

  await page.goto('/kb/profile')
  await page.getByLabel('显示名称').fill(emojiName)
  await page.getByRole('button', { name: '保存修改' }).click()
  await expect(page.getByText('个人资料已保存')).toBeVisible()
  expect(Array.from(String(updatePayload?.displayName))).toHaveLength(40)

  await page.getByLabel('显示名称').fill('深海')
  await expect(page.getByText('个人资料已保存')).toHaveCount(0)
})

test('rejects bidirectional Unicode controls in display names', async ({ page }) => {
  let updateRequests = 0
  await page.route('**/kb/api/v1/profile', async (route) => {
    if (route.request().method() !== 'PATCH') {
      await route.fallback()
      return
    }
    updateRequests += 1
    await route.fulfill({ status: 500 })
  })

  await page.goto('/kb/profile')
  await page.getByLabel('显示名称').fill('Alice\u202eevil')
  await page.getByRole('button', { name: '保存修改' }).click()
  await expect(page.getByRole('alert')).toContainText('不能包含')
  expect(updateRequests).toBe(0)
})

test('confirms sign out and keeps the session when cancelled', async ({ page }) => {
  let logoutRequests = 0
  await page.route('**/kb/api/v1/auth/logout', async (route) => {
    logoutRequests += 1
    await route.fulfill({ status: 204 })
  })

  await page.goto('/kb/')
  await page.getByRole('button', { name: '打开林舟的账户菜单' }).click()
  await page.getByRole('menuitem', { name: '退出登录' }).click()

  const dialog = page.getByRole('alertdialog', { name: '退出登录？' })
  await expect(dialog).toBeVisible()
  await page.keyboard.press('Shift+Tab')
  await expect(dialog.getByRole('button', { name: '确认退出' })).toBeFocused()
  await page.keyboard.press('Escape')
  await expect(dialog).toHaveCount(0)
  expect(logoutRequests).toBe(0)

  await page.getByRole('button', { name: '打开林舟的账户菜单' }).click()
  await page.getByRole('menuitem', { name: '退出登录' }).click()
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: '取消' }).click()
  await expect(dialog).toHaveCount(0)
  await expect(page).toHaveURL(/\/kb\/$/)
  expect(logoutRequests).toBe(0)

  await page.getByRole('button', { name: '打开林舟的账户菜单' }).click()
  await page.getByRole('menuitem', { name: '退出登录' }).click()
  await page.getByRole('alertdialog', { name: '退出登录？' })
    .getByRole('button', { name: '确认退出' })
    .click()
  await expect(page).toHaveURL(/\/kb\/login$/)
  expect(logoutRequests).toBe(1)
})

test('keeps the local session when sign-out fails', async ({ page }) => {
  await page.route('**/kb/api/v1/auth/logout', async (route) => {
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'STATE_CONFLICT',
        message: 'Sign out failed.',
        requestId: 'logout-failed',
        retryable: true,
      }),
    })
  })

  await page.goto('/kb/')
  await page.getByRole('button', { name: '打开林舟的账户菜单' }).click()
  await page.getByRole('menuitem', { name: '退出登录' }).click()
  await page.getByRole('alertdialog', { name: '退出登录？' })
    .getByRole('button', { name: '确认退出' })
    .click()

  const dialog = page.getByRole('alertdialog', { name: '退出登录？' })
  await expect(dialog).toBeVisible()
  await expect(dialog.getByRole('alert')).toContainText('退出登录失败')
  await expect(page.getByRole('button', { name: '打开林舟的账户菜单' })).toBeVisible()
})

test('searches from the prototype-aligned home screen', async ({ page }) => {
  await page.goto('/kb/')
  await expect(page.getByRole('heading', { level: 1, name: '识海' })).toBeVisible()
  await expect(page.getByText('深海检索笔记')).toBeVisible()
  await expect(page.locator('.ocean-chart-backdrop')).toBeVisible()
  await expect(page.locator('.ocean-chart-backdrop path')).toHaveCount(13)
  await expect(page.locator('body')).toHaveCSS('background-color', 'rgb(6, 21, 34)')
  const resultsBox = await page.locator('.home-grid > .results-panel').boundingBox()
  const railBox = await page.locator('.home-grid > .right-rail').boundingBox()
  expect(resultsBox).not.toBeNull()
  expect(railBox).not.toBeNull()
  expect(Math.abs(resultsBox!.y - railBox!.y)).toBeLessThanOrEqual(1)

  const navigation = page.getByRole('navigation', { name: '主导航' })
  await expect(navigation).toBeVisible()
  for (const label of ['首页', '知识库', '采集', '待处理', '回收站', '设置']) {
    await expect(navigation.getByRole('link', { name: label, exact: true })).toBeVisible()
  }

  await page.getByRole('searchbox', { name: '搜索全部知识' }).fill('深海')
  await page.getByRole('button', { name: '潜入检索' }).click()
  await expect(page.getByText('找到 1 条知识')).toBeVisible()
  await page.screenshot({ path: 'test-results/home-desktop.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await page.screenshot({ path: 'test-results/home-mobile-ocean.png', fullPage: true })
})

test('answers from local knowledge with one cited item and one point', async ({ page }) => {
  let quotePayload: Record<string, unknown> | undefined
  let answerPayload: Record<string, unknown> | undefined
  const knowledgeId = '123e4567-e89b-42d3-a456-426614174501'

  await page.route('**/kb/api/v1/chat/retrieval', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        hasContext: true,
        sources: [
          {
            kind: 'knowledge',
            knowledgeId,
            title: '深度检索原则',
            excerpt: '先查自己的知识，再决定是否联网。',
            matchedFields: ['body'],
          },
        ],
      }),
    })
  })
  await page.route('**/kb/api/v1/billing/quotes', async (route) => {
    quotePayload = route.request().postDataJSON() as Record<string, unknown>
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        quoteId: '123e4567-e89b-42d3-a456-426614174502',
        status: 'QUOTED',
        lines: [
          {
            feature: 'ai_answer',
            unit: 'answer',
            quantity: 1,
            pointsPerUnit: 1,
            maxPoints: 1,
            successRule: 'answer_displayed',
          },
        ],
        imageResources: [],
        maxPoints: 1,
        availablePoints: 100,
        projectedRemainingPoints: 99,
        expiresAt: new Date(Date.now() + 600000).toISOString(),
        autoChargeEligible: false,
      }),
    })
  })
  await page.route('**/kb/api/v1/chat/answers/stream', async (route) => {
    const answerText = '先检索自己的知识，可以避免重复查找，也更容易保留上下文。'
    answerPayload = route.request().postDataJSON() as Record<string, unknown>
    await route.fulfill({
      status: 200,
      contentType: 'text/event-stream',
      body: sseBody([
        { event: 'token', data: { text: '先检索自己的知识，' } },
        { event: 'token', data: { text: '可以避免重复查找，也更容易保留上下文。' } },
        {
          event: 'complete',
          data: {
            answer: answerText,
            needsInternet: false,
            sources: [
              {
                kind: 'knowledge',
                knowledgeId,
                title: '深度检索原则',
                excerpt: '先查自己的知识，再决定是否联网。',
                matchedFields: ['body'],
              },
            ],
            reason: null,
          },
        },
      ]),
    })
  })

  await page.goto('/kb/chat')
  await expect(page.getByRole('heading', { level: 1, name: '与小识聊聊' })).toBeVisible()
  await expect(
    page.getByRole('navigation', { name: '主导航' }).getByRole('link', { name: '聊天' }),
  ).toBeVisible()
  await page.getByLabel('向小识提问').fill('为什么要优先检索自己的知识？')
  await page.getByRole('button', { name: '发送 · 1 点' }).click()

  await expect(
    page.locator('.chat-message-body').filter({ hasText: '先检索自己的知识，可以避免重复查找' }),
  ).toBeVisible()
  await expect(page.getByRole('link', { name: /深度检索原则/ })).toBeVisible()
  const userMessage = page.locator('.chat-message.is-user').first()
  await expect(userMessage).toHaveCSS('background-color', 'rgba(0, 0, 0, 0)')
  await expect(userMessage).toHaveCSS('border-radius', '0px')
  await expect(userMessage.locator('.chat-message-meta strong')).toHaveText('你')
  await expect(userMessage.locator('.chat-message-body')).toHaveCSS('text-align', 'right')
  const userTime = await userMessage.locator('time').boundingBox()
  const userLabel = await userMessage.locator('.chat-message-meta strong').boundingBox()
  const userAvatar = await page.locator('.chat-message.is-user .chat-avatar').first().boundingBox()
  const assistantAvatar = await page
    .locator('.chat-message.is-assistant .chat-avatar')
    .first()
    .boundingBox()
  expect(userTime).not.toBeNull()
  expect(userLabel).not.toBeNull()
  expect(userAvatar).not.toBeNull()
  expect(assistantAvatar).not.toBeNull()
  expect(userTime!.x + userTime!.width).toBeLessThan(userLabel!.x)
  expect(Math.abs(userTime!.y + userTime!.height / 2 - (userLabel!.y + userLabel!.height / 2))).toBeLessThan(2)
  expect(userAvatar!.x).toBeGreaterThan(assistantAvatar!.x)
  expect(quotePayload).toMatchObject({
    operation: 'knowledge_chat',
    lines: [{ feature: 'ai_answer', quantity: 1 }],
  })
  expect(answerPayload).toMatchObject({
    query: '为什么要优先检索自己的知识？',
    externalAiConsent: true,
  })
})

test('answers a natural conversation without knowledge evidence and stays offline', async ({
  page,
}) => {
  let quoteOperation: string | undefined
  await page.route('**/kb/api/v1/billing/quotes', async (route) => {
    quoteOperation = (route.request().postDataJSON() as { operation: string }).operation
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        quoteId: '123e4567-e89b-42d3-a456-426614174511',
        status: 'QUOTED',
        lines: [],
        imageResources: [],
        maxPoints: 1,
        availablePoints: 100,
        projectedRemainingPoints: 99,
        expiresAt: new Date(Date.now() + 600000).toISOString(),
        autoChargeEligible: false,
      }),
    })
  })
  await page.route('**/kb/api/v1/chat/answers/stream', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'text/event-stream',
      body: sseBody([
        { event: 'token', data: { text: '你好，' } },
        { event: 'token', data: { text: '我是小识。' } },
        {
          event: 'complete',
          data: {
            answer: '你好，我是小识。',
            needsInternet: false,
            sources: [],
            reason: null,
          },
        },
      ]),
    })
  })

  await page.goto('/kb/chat')
  await page.getByLabel('向小识提问').fill('你好')
  await page.getByRole('button', { name: '发送 · 1 点' }).click()

  await expect(
    page.locator('.chat-message-body').filter({ hasText: '你好，我是小识。' }),
  ).toBeVisible()
  await expect(page.getByRole('button', { name: /联网搜索并回答/ })).toHaveCount(0)
  expect(quoteOperation).toBe('knowledge_chat')
})

test('reports an assistant history save failure without claiming history is unreadable', async ({
  page,
}) => {
  await page.route('**/kb/api/v1/chat/conversations/*/messages', async (route) => {
    const payload = route.request().postDataJSON() as { role: 'user' | 'assistant' }
    if (payload.role === 'assistant') {
      await route.fulfill({
        status: 422,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'VALIDATION_FAILED',
          message: 'Request is invalid.',
          requestId: 'assistant-save-failed',
          retryable: false,
        }),
      })
      return
    }
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        id: '123e4567-e89b-42d3-a456-426614174531',
        role: 'user',
        content: '你好',
        sources: [],
        needsInternet: false,
        reason: null,
        createdAt: new Date().toISOString(),
      }),
    })
  })
  await page.route('**/kb/api/v1/chat/answers/stream', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'text/event-stream',
      body: sseBody([
        { event: 'token', data: { text: '你好，我是小识。' } },
        {
          event: 'complete',
          data: {
            answer: '你好，我是小识。',
            needsInternet: false,
            sources: [],
            reason: null,
          },
        },
      ]),
    })
  })

  await page.goto('/kb/chat')
  await page.getByLabel('向小识提问').fill('你好')
  await page.getByRole('button', { name: '发送 · 1 点' }).click()

  await expect(
    page.locator('.chat-message-body').filter({ hasText: '你好，我是小识。' }),
  ).toBeVisible()
  await expect(
    page.getByText('这条消息未能写入历史，当前页面内容仍然保留。'),
  ).toBeVisible()
  await expect(page.getByText('历史对话读取失败，请稍后重试。')).toHaveCount(0)
})

test('waits for internet confirmation and charges two points only after the click', async ({
  page,
}) => {
  const quoteOperations: string[] = []
  let internetRequests = 0
  let internetPayload:
    | { history?: Array<{ role: string; content: string }> }
    | undefined

  await page.route('**/kb/api/v1/chat/retrieval', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ hasContext: false, sources: [] }),
    })
  })
  await page.route('**/kb/api/v1/billing/quotes', async (route) => {
    const payload = route.request().postDataJSON() as { operation: string }
    quoteOperations.push(payload.operation)
    const maxPoints = payload.operation === 'knowledge_chat' ? 1 : 2
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        quoteId: '123e4567-e89b-42d3-a456-426614174503',
        status: 'QUOTED',
        lines: [],
        imageResources: [],
        maxPoints,
        availablePoints: 100,
        projectedRemainingPoints: 100 - maxPoints,
        expiresAt: new Date(Date.now() + 600000).toISOString(),
        autoChargeEligible: false,
      }),
    })
  })
  await page.route('**/kb/api/v1/chat/answers/stream', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'text/event-stream',
      body: sseBody([
        {
          event: 'complete',
          data: {
            answer: '',
            needsInternet: true,
            sources: [],
            reason: 'knowledge_insufficient',
          },
        },
      ]),
    })
  })
  await page.route('**/kb/api/v1/chat/internet/stream', async (route) => {
    internetRequests += 1
    internetPayload = route.request().postDataJSON() as typeof internetPayload
    await route.fulfill({
      status: 200,
      contentType: 'text/event-stream',
      body: sseBody([
        { event: 'token', data: { text: '这是基于公开互联网' } },
        { event: 'token', data: { text: '来源整理的回答。' } },
        {
          event: 'complete',
          data: {
            answer: '这是基于公开互联网来源整理的回答。',
            needsInternet: false,
            sources: [
              {
                kind: 'internet',
                title: '公开参考资料',
                excerpt: '来源摘要',
                url: 'https://example.com/reference',
                provider: 'Example',
                matchedFields: [],
              },
            ],
            reason: null,
          },
        },
      ]),
    })
  })

  await page.goto('/kb/chat')
  await page.getByLabel('向小识提问').fill('知识库里没有的新问题')
  await page.getByRole('button', { name: '发送 · 1 点' }).click()

  await expect(
    page
      .locator('.chat-message-body')
      .filter({ hasText: '这个问题需要更多或更及时的信息' }),
  ).toBeVisible()
  expect(quoteOperations).toEqual(['knowledge_chat'])
  expect(internetRequests).toBe(0)

  await page.getByRole('button', { name: /联网搜索并回答 · 2 点/ }).click()
  await expect(
    page.locator('.chat-message-body').filter({ hasText: '这是基于公开互联网来源整理的回答。' }),
  ).toBeVisible()
  await expect(page.getByRole('link', { name: /公开参考资料/ })).toHaveAttribute(
    'href',
    'https://example.com/reference',
  )
  expect(quoteOperations).toEqual(['knowledge_chat', 'internet_search_with_answer'])
  expect(internetRequests).toBe(1)
  expect(internetPayload?.history).not.toContainEqual({
    role: 'user',
    content: '知识库里没有的新问题',
  })

  await page.setViewportSize({ width: 390, height: 844 })
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
    ),
  ).toBe(true)
  await page.screenshot({ path: 'test-results/chat-internet-390.png', fullPage: true })
})

test('keeps the chat workspace bilingual and usable at 320 pixels', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 720 })
  await page.goto('/kb/chat')
  await page.getByRole('button', { name: '切换到英文' }).click()

  await expect(page.getByRole('heading', { level: 1, name: 'Chat with Xiaoshi' })).toBeVisible()
  await expect(
    page.getByRole('navigation', { name: 'Main navigation' }).getByRole('link', {
      name: 'Chat',
      exact: true,
    }),
  ).toBeVisible()
  await expect(page.getByLabel('Ask Xiaoshi')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Send · 1 point' })).toBeVisible()
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
    ),
  ).toBe(true)
})

test('restores saved conversations and shows distinct user and assistant avatars', async ({
  page,
}) => {
  const conversationId = '123e4567-e89b-42d3-a456-426614174520'
  const conversation = {
    id: conversationId,
    title: '许愿式编程',
    lastMessage: '它强调让模型持续规划、编码和自检。',
    messageCount: 2,
    createdAt: '2026-10-23T08:20:00Z',
    updatedAt: '2026-10-23T08:22:00Z',
  }
  await page.route('**/kb/api/v1/chat/conversations?**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ items: [conversation] }),
    })
  })
  await page.route(`**/kb/api/v1/chat/conversations/${conversationId}`, async (route) => {
    if (route.request().method() === 'DELETE') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ deleted: true }),
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        conversation,
        messages: [
          {
            id: '123e4567-e89b-42d3-a456-426614174521',
            role: 'user',
            content: '什么是许愿式编程？',
            sources: [],
            needsInternet: false,
            reason: null,
            createdAt: '2026-10-23T08:20:00Z',
          },
          {
            id: '123e4567-e89b-42d3-a456-426614174522',
            role: 'assistant',
            content: '它强调让模型持续规划、编码和自检。',
            sources: [],
            needsInternet: false,
            reason: null,
            createdAt: '2026-10-23T08:22:00Z',
          },
        ],
      }),
    })
  })

  await page.goto('/kb/chat')
  await expect(page.getByRole('button', { name: /许愿式编程/ }).first()).toBeVisible()
  await expect(
    page.locator('.chat-message-body').filter({ hasText: '什么是许愿式编程？' }),
  ).toBeVisible()
  await expect(
    page.locator('.chat-message-body').filter({ hasText: '它强调让模型持续规划、编码和自检。' }),
  ).toBeVisible()
  await expect(page.locator('.chat-avatar.is-user')).toHaveText('林')
  await expect(page.locator('.chat-avatar.is-assistant svg')).toHaveCount(1)

  await page.reload()
  await expect(
    page.locator('.chat-message-body').filter({ hasText: '什么是许愿式编程？' }),
  ).toBeVisible()
  await expect(
    page.locator('.chat-message-body').filter({ hasText: '它强调让模型持续规划、编码和自检。' }),
  ).toBeVisible()

  await page.getByRole('button', { name: '删除对话“许愿式编程”' }).click()
  const dialog = page.getByRole('alertdialog', { name: '删除这个对话？' })
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: '删除对话' }).click()
  await expect(page.getByText('还没有历史对话。发出第一个问题后会自动保存。')).toBeVisible()
})

test('corrects locked classification and restores an item from the trash', async ({ page }) => {
  const itemId = '123e4567-e89b-42d3-a456-426614174901'
  let current: KnowledgeItem = {
    ...summary,
    id: itemId,
    title: '深海分类测试',
    body: '这是一条用于验证分类纠正和回收站恢复的知识。',
    bodyDocument: {
      type: 'doc',
      content: [{ type: 'paragraph', content: [{ type: 'text', text: '正文' }] }],
    } as RichTextDocument,
    bodySchemaVersion: 1,
    bodyMigrationState: 'completed' as const,
    note: '',
    redactionApplied: false,
    category: {
      id: '123e4567-e89b-42d3-a456-426614174902',
      name: '未分类',
      status: 'provisional',
      origin: 'ai',
      confidence: 0.4,
    },
    tags: [],
  }

  await page.route(`**/kb/api/v1/knowledge-items/${itemId}`, async (route) => {
    if (route.request().method() === 'DELETE') {
      await route.fulfill({ status: 204 })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(current),
    })
  })
  await page.route(
    `**/kb/api/v1/knowledge-items/${itemId}/classification`,
    async (route) => {
      const payload = route.request().postDataJSON() as { category: string; tags: string[] }
      current = {
        ...current,
        category: {
          id: '123e4567-e89b-42d3-a456-426614174903',
          name: payload.category,
          status: 'locked',
          origin: 'user',
          confidence: null,
        },
        tags: payload.tags.map((name, index) => ({
          id: `tag-${index}`,
          name,
          status: 'locked',
          origin: 'user',
        })),
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(current),
      })
    },
  )
  await page.route('**/kb/api/v1/knowledge-items/trash**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ items: [current], nextCursor: null }),
    })
  })
  await page.route(
    `**/kb/api/v1/knowledge-items/${itemId}/restore`,
    async (route) => {
      await route.fulfill({ status: 204 })
    },
  )

  await page.goto(`/kb/library/${itemId}`)
  await page.getByRole('button', { name: '纠正分类' }).click()
  await page.getByLabel('分类').fill('人工智能')
  await page.getByLabel('标签').fill('知识管理、深检索')
  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect(page.getByText('人工智能', { exact: true }).first()).toBeVisible()
  await expect(page.getByText('知识管理', { exact: true }).first()).toBeVisible()

  await page.getByRole('button', { name: '删除', exact: true }).click()
  await page.getByRole('button', { name: '移入回收站', exact: true }).click()
  await expect(page).toHaveURL(/\/kb\/trash$/)
  await expect(page.getByText('深海分类测试')).toBeVisible()
  await page.getByRole('button', { name: '恢复', exact: true }).click()
  await expect(page.getByText('深海分类测试')).toHaveCount(0)
})

test('uses the prototype bottom navigation on a phone', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/kb/')

  await expect(page.getByRole('button', { name: '打开菜单' })).toHaveCount(0)
  const navigation = page.getByRole('navigation', { name: '主导航' })
  await expect(navigation).toBeVisible()
  await expect(navigation.getByRole('link')).toHaveCount(8)
  await expect(navigation.getByRole('link', { name: '设置', exact: true })).toBeVisible()
  const targets = await navigation.getByRole('link').evaluateAll((links) =>
    links.map((link) => {
      const box = link.getBoundingClientRect()
      return { width: box.width, height: box.height }
    }),
  )
  expect(targets.every((target) => target.width >= 44 && target.height >= 44)).toBe(true)
  await page.screenshot({ path: 'test-results/home-mobile.png', fullPage: true })
})

test('opens every prototype navigation destination', async ({ page }) => {
  await page.goto('/kb/')
  const navigation = page.getByRole('navigation', { name: '主导航' })

  await navigation.getByRole('link', { name: '知识库', exact: true }).click()
  await expect(page).toHaveURL(/\/kb\/library$/)
  await expect(page.getByRole('heading', { level: 1, name: '知识库' })).toBeVisible()

  await navigation.getByRole('link', { name: '聊天', exact: true }).click()
  await expect(page).toHaveURL(/\/kb\/chat$/)
  await expect(page.getByRole('heading', { level: 1, name: '与小识聊聊' })).toBeVisible()

  await navigation.getByRole('link', { name: '待处理', exact: true }).click()
  await expect(page).toHaveURL(/\/kb\/processing$/)
  await expect(page.getByRole('heading', { level: 1, name: '待处理' })).toBeVisible()

  await navigation.getByRole('link', { name: '回收站', exact: true }).click()
  await expect(page).toHaveURL(/\/kb\/trash$/)
  await expect(page.getByRole('heading', { level: 1, name: '回收站' })).toBeVisible()

  await navigation.getByRole('link', { name: '点数', exact: true }).click()
  await expect(page).toHaveURL(/\/kb\/billing$/)
  await expect(page.getByRole('heading', { level: 1, name: '点数与支付' })).toBeVisible()

  await navigation.getByRole('link', { name: '设置', exact: true }).click()
  await expect(page).toHaveURL(/\/kb\/settings$/)
  await expect(page.getByRole('heading', { level: 1, name: '设置' })).toBeVisible()
})

test('confirms AI reviews and excludes manual entries from the review queue', async ({ page }) => {
  const aiItem = {
    ...summary,
    id: '123e4567-e89b-42d3-a456-426614174151',
    title: 'AI 整理的网页知识',
    processingStatus: 'review',
    source: { kind: 'webpage', inputMethod: 'url', url: 'https://example.com/article' },
  }
  let reviewRequests = 0
  let queueStatus = ''
  let pendingTotal = 1

  await page.route('**/kb/api/v1/knowledge-items?**', async (route) => {
    const url = new URL(route.request().url())
    const limit = Number(url.searchParams.get('limit') || '20')
    queueStatus = url.searchParams.get('status') || ''
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: limit === 1 ? [] : pendingTotal > 0 ? [aiItem] : [],
        nextCursor: null,
        pageInfo: {
          page: 1,
          pageSize: limit,
          totalItems: pendingTotal,
          totalPages: 1,
          sourceCounts: {},
        },
      }),
    })
  })
  await page.route(
    '**/kb/api/v1/knowledge-items/123e4567-e89b-42d3-a456-426614174151/review',
    async (route) => {
      reviewRequests += 1
      pendingTotal = 0
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ ...aiItem, processingStatus: 'succeeded' }),
      })
    },
  )

  await page.goto('/kb/processing')

  await expect(page.getByText('AI 整理的网页知识')).toBeVisible()
  const pendingLink = page
    .getByRole('navigation', { name: '主导航' })
    .getByRole('link', { name: '待处理', exact: true })
  await expect(pendingLink.locator('.nav-count')).toHaveText('1')
  await expect(page.getByText('手动录入的知识')).toHaveCount(0)
  expect(queueStatus).toBe('pending,queued,running,processing,review,failed')
  for (const viewport of [
    { width: 1440, height: 900 },
    { width: 768, height: 900 },
    { width: 390, height: 844 },
  ]) {
    await page.setViewportSize(viewport)
    await page.screenshot({
      path: `test-results/review-queue-${viewport.width}.png`,
      fullPage: true,
    })
  }
  await page.getByRole('button', { name: '确认复核' }).click()

  expect(reviewRequests).toBe(1)
  await expect(page.getByText('AI 整理的网页知识')).toHaveCount(0)
  await expect(pendingLink.locator('.nav-count')).toHaveCount(0)
  await expect(page.getByRole('status')).toContainText('复核已确认')
})

test('searches knowledge from the library and keeps source filters', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 })
  let searchQuery = ''
  await page.route('**/kb/api/v1/search?**', async (route) => {
    const url = new URL(route.request().url())
    searchQuery = url.searchParams.get('q') || ''
    const source = url.searchParams.get('source') || 'all'
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items:
          source === 'manual'
            ? []
            : [
                {
                  item: {
                    ...summary,
                    id: '123e4567-e89b-42d3-a456-426614174711',
                    title: '深海检索结果',
                    source: { kind: 'webpage', inputMethod: 'url' },
                    tags: [
                      { id: 'tag-search', name: '深海', status: 'applied', origin: 'ai' },
                    ],
                  },
                  matchedFields: ['title', 'tag'],
                  highlights: { title: ['深海检索结果'] },
                },
              ],
        nextCursor: null,
        degraded: false,
      }),
    })
  })

  await page.goto('/kb/library')
  await page.getByRole('searchbox', { name: '检索知识库' }).fill('深海')
  await page.getByRole('button', { name: '检索', exact: true }).click()

  await expect(page.getByText('深海检索结果')).toBeVisible()
  expect(searchQuery).toBe('深海')
  await page.screenshot({ path: 'test-results/library-desktop.png', fullPage: true })

  await page.getByRole('button', { name: /网页/ }).click()
  await expect(page.getByText('深海检索结果')).toBeVisible()

  await page.getByRole('button', { name: /手动/ }).click()
  await expect(page.getByText('没有找到匹配的知识。')).toBeVisible()
  await page.setViewportSize({ width: 768, height: 1024 })
  await page.screenshot({ path: 'test-results/library-tablet.png', fullPage: true })
})

test('paginates the knowledge library on the server and filters by source', async ({ page }) => {
  const pageTwo = {
    ...summary,
    id: '123e4567-e89b-42d3-a456-426614174720',
    title: '第二页知识',
  }
  const webpageOnly = {
    ...summary,
    id: '123e4567-e89b-42d3-a456-426614174721',
    title: '网页筛选知识',
    source: { kind: 'webpage', inputMethod: 'url' },
  }
  const requests: Array<{ page: number; source: string; limit: number }> = []
  await page.route(
    (url) => url.pathname === '/kb/api/v1/knowledge-items',
    async (route) => {
      const url = new URL(route.request().url())
      const pageNumber = Number(url.searchParams.get('page') || '1')
      const source = url.searchParams.get('source') || 'all'
      const limit = Number(url.searchParams.get('limit') || '10')
      requests.push({ page: pageNumber, source, limit })
      const items =
        source === 'webpage'
          ? [webpageOnly]
          : pageNumber === 2
            ? [pageTwo]
            : [summary]
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items,
          nextCursor: pageNumber < 3 ? String(pageNumber + 1) : null,
          pageInfo: {
            page: pageNumber,
            pageSize: 10,
            totalItems: source === 'webpage' ? 12 : 24,
            totalPages: source === 'webpage' ? 2 : 3,
            sourceCounts: {
              all: 24,
              webpage: 12,
              document: 4,
              screenshot: 3,
              text: 2,
              manual: 3,
            },
          },
        }),
      })
    },
  )

  await page.goto('/kb/library')
  await expect(page.getByText('第 1 / 3 页 · 共 24 条')).toBeVisible()
  await expect(page.getByRole('button', { name: /全部 24/ })).toBeVisible()

  await page.getByRole('button', { name: '第 2 页' }).click()
  await expect(page.getByText('第二页知识')).toBeVisible()
  await expect(page.getByText('第 2 / 3 页 · 共 24 条')).toBeVisible()
  expect(requests.at(-1)).toEqual({ page: 2, source: 'all', limit: 10 })

  await page.getByRole('button', { name: /网页/ }).click()
  await expect(page.getByText('网页筛选知识')).toBeVisible()
  await expect(page.getByText('第 1 / 2 页 · 共 12 条')).toBeVisible()
  expect(requests.at(-1)).toEqual({ page: 1, source: 'webpage', limit: 10 })
})

test('recycles one knowledge item from the library', async ({ page }) => {
  let deletedId = ''
  let currentItems = [summary]
  await page.route('**/kb/api/v1/knowledge-items/*', async (route) => {
    if (route.request().method() === 'DELETE') {
      deletedId = new URL(route.request().url()).pathname.split('/').pop() || ''
      currentItems = []
      await route.fulfill({ status: 204, body: '' })
      return
    }
    await route.fallback()
  })
  await page.route(
    (url) => url.pathname === '/kb/api/v1/knowledge-items',
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ items: currentItems, nextCursor: null }),
      })
    },
  )

  await page.goto('/kb/library')
  await page.getByRole('button', { name: `删除 ${summary.title}` }).click()
  await expect(page.getByRole('alertdialog')).toContainText('移入回收站？')
  await page.getByRole('button', { name: '移入回收站' }).click()

  expect(deletedId).toBe(summary.id)
  await expect(page.getByText(summary.title)).toHaveCount(0)
  await expect(page.getByRole('status')).toContainText('已移入回收站')
})

test('selects and recycles multiple knowledge items in one request', async ({ page }) => {
  const second = {
    ...summary,
    id: '123e4567-e89b-42d3-a456-426614174712',
    title: '第二份知识',
  }
  let requestedIds: string[] = []
  let currentItems = [summary, second]
  await page.route('**/kb/api/v1/knowledge-items**', async (route) => {
    if (route.request().method() === 'POST' && route.request().url().includes('/bulk-recycle')) {
      requestedIds = (route.request().postDataJSON() as { itemIds: string[] }).itemIds
      currentItems = currentItems.filter((item) => !requestedIds.includes(item.id))
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ recycled: requestedIds.length }),
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: currentItems,
        nextCursor: null,
      }),
    })
  })

  await page.goto('/kb/library')
  await page.getByRole('checkbox', { name: `选择 ${summary.title}` }).check()
  await page.getByRole('checkbox', { name: `选择 ${second.title}` }).check()

  await expect(page.getByText('已选择 2 条')).toBeVisible()
  await page.getByRole('button', { name: '批量删除' }).click()
  await expect(page.getByRole('alertdialog')).toContainText('批量移入回收站？')
  await page.getByRole('button', { name: '移入回收站' }).click()

  expect(requestedIds.sort()).toEqual([summary.id, second.id].sort())
  await expect(page.getByText(summary.title)).toHaveCount(0)
  await expect(page.getByText(second.title)).toHaveCount(0)
})

test('keeps library search and bulk selection usable on a phone', async ({ page }) => {
  const second = {
    ...summary,
    id: '123e4567-e89b-42d3-a456-426614174713',
    title: '移动端第二份知识',
  }
  await page.route('**/kb/api/v1/knowledge-items**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [summary, second],
        nextCursor: null,
      }),
    })
  })
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/kb/library')

  const checkbox = page.getByRole('checkbox', { name: `选择 ${summary.title}` })
  await expect(checkbox).toBeVisible()
  const box = await checkbox.locator('..').boundingBox()
  expect(box?.width || 0).toBeGreaterThanOrEqual(44)
  expect(box?.height || 0).toBeGreaterThanOrEqual(44)

  await page.getByRole('button', { name: '全选当前结果' }).click()
  await expect(page.getByText('已选择 2 条')).toBeVisible()

  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
  )
  expect(overflow).toBe(false)
  await page.screenshot({ path: 'test-results/library-mobile.png', fullPage: true })
})

test('keeps selected items and reuses the bulk key after a failed delete', async ({ page }) => {
  const second = {
    ...summary,
    id: '123e4567-e89b-42d3-a456-426614174714',
    title: '失败重试知识',
  }
  const idempotencyKeys: string[] = []
  let attempts = 0
  await page.route('**/kb/api/v1/knowledge-items**', async (route) => {
    if (route.request().method() === 'POST' && route.request().url().includes('/bulk-recycle')) {
      attempts += 1
      idempotencyKeys.push(route.request().headers()['idempotency-key'] || '')
      if (attempts === 1) {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'TEMPORARY', message: 'Try again' }),
        })
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ recycled: 2 }),
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ items: [summary, second], nextCursor: null }),
    })
  })

  await page.goto('/kb/library')
  await page.getByRole('checkbox', { name: `选择 ${summary.title}` }).check()
  await page.getByRole('checkbox', { name: `选择 ${second.title}` }).check()
  await page.getByRole('button', { name: '批量删除' }).click()
  await page.getByRole('button', { name: '移入回收站' }).click()

  await expect(page.getByRole('alert')).toHaveText(
    '批量删除失败，知识未发生变化。',
  )
  await expect(page.getByText('已选择 2 条')).toBeVisible()

  await page.getByRole('button', { name: '移入回收站' }).click()
  await expect(page.getByText('已将 2 条知识移入回收站。')).toBeVisible()
  expect(idempotencyKeys).toHaveLength(2)
  expect(idempotencyKeys[0]).toBeTruthy()
  expect(idempotencyKeys[1]).toBe(idempotencyKeys[0])
})

test('keeps only the latest library search response', async ({ page }) => {
  await page.route('**/kb/api/v1/search?**', async (route) => {
    const query = new URL(route.request().url()).searchParams.get('q')
    if (query === '旧查询') {
      await new Promise((resolve) => setTimeout(resolve, 700))
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            item: { ...summary, title: query === '旧查询' ? '旧查询结果' : '新查询结果' },
            matchedFields: ['title'],
            highlights: {},
          },
        ],
        nextCursor: null,
        degraded: false,
      }),
    })
  })

  await page.goto('/kb/library')
  const search = page.getByRole('searchbox', { name: '检索知识库' })
  await search.fill('旧查询')
  await page.getByRole('button', { name: '检索', exact: true }).click()
  await search.fill('新查询')
  await page.getByRole('button', { name: '检索', exact: true }).click()

  await expect(page.getByText('新查询结果')).toBeVisible()
  await expect(page.getByText('旧查询结果')).toHaveCount(0)
})

test('shows pricing, balance, and automatic charging controls', async ({ page }) => {
  let savedAutoCharge: Record<string, unknown> | undefined
  await page.route('**/kb/api/v1/billing/orders**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: Array.from({ length: 8 }, (_, index) => ({
          id: `order-${index}`,
          sku: index % 2 === 0 ? 'kb_pack_9' : 'kb_pack_29',
          environment: 'test',
          amountMinor: index % 2 === 0 ? 900 : 2900,
          currency: 'CNY',
          points: index % 2 === 0 ? 400 : 1400,
          providerProductId: null,
          status: 'PAID',
          checkoutUrl: null,
          paidAt: new Date().toISOString(),
          closedAt: null,
          createdAt: new Date(Date.now() - index * 60_000).toISOString(),
          updatedAt: new Date().toISOString(),
        })),
        nextCursor: null,
      }),
    })
  })
  await page.route('**/kb/api/v1/billing/ledger**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: Array.from({ length: 18 }, (_, index) => ({
          id: `ledger-${index}`,
          operationType: 'settle',
          availableDelta: -1,
          reservedDelta: 0,
          consumedDelta: 1,
          availableAfter: 100 - index,
          reservedAfter: 0,
          referenceType: 'billing_task',
          referenceId: `task-${index}`,
          businessOperation: 'internet_search',
          description: null,
          createdAt: new Date(Date.now() - index * 60_000).toISOString(),
        })),
        nextCursor: null,
      }),
    })
  })
  await page.route('**/kb/api/v1/billing/auto-charge', async (route) => {
    if (route.request().method() === 'PATCH') {
      savedAutoCharge = route.request().postDataJSON() as Record<string, unknown>
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          featureScope: 'classify',
          enabled: true,
          maxPointsPerTask: 15,
          maxPointsPerDay: 100,
          timezone: 'Asia/Shanghai',
          consentTextVersion: 'billing-auto-v1',
          version: 2,
          updatedAt: new Date().toISOString(),
        }),
      })
      return
    }
    await route.fallback()
  })

  await page.goto('/kb/billing')
  await expect(page.getByRole('heading', { level: 1, name: '点数与支付' })).toBeVisible()
  await expect(page.locator('.billing-balance-value')).toHaveText('100')
  const packGrid = page.locator('.billing-packs')
  await expect(packGrid.getByText('轻量加量包')).toBeVisible()
  await expect(packGrid.getByText('常用加量包')).toBeVisible()
  await expect(packGrid.getByText('超值加量包')).toBeVisible()
  const metricCells = page.locator('.billing-metrics > div')
  for (const cell of await metricCells.all()) {
    await expect(cell).toHaveCSS('justify-items', 'center')
    await expect(cell).toHaveCSS('text-align', 'center')
  }
  await expect(page.locator('.billing-metrics')).toHaveCSS('padding-left', '24px')
  const recordPanels = page.locator('.billing-records-panel')
  await expect(recordPanels).toHaveCount(2)
  const panelHeights = await recordPanels.evaluateAll((panels) =>
    panels.map((panel) => panel.getBoundingClientRect().height),
  )
  expect(new Set(panelHeights.map((height) => Math.round(height))).size).toBe(1)
  expect(panelHeights[0]).toBeGreaterThanOrEqual(320)
  expect(panelHeights[0]).toBeLessThanOrEqual(460)
  const ledgerList = recordPanels.nth(1).locator('.billing-list')
  await expect(ledgerList).toHaveCSS('overflow-y', 'scroll')
  expect(
    await ledgerList.evaluate((element) => element.scrollHeight > element.clientHeight),
  ).toBe(true)
  await page.screenshot({ path: 'test-results/billing-desktop.png', fullPage: true })

  await page.setViewportSize({ width: 390, height: 844 })
  await page.reload()
  await expect(metricCells.first()).toHaveCSS('justify-items', 'center')
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
    ),
  ).toBe(true)
  await page.screenshot({ path: 'test-results/billing-mobile.png', fullPage: true })

  await page.getByRole('checkbox').check()
  await page.getByRole('button', { name: '保存设置' }).click()
  expect(savedAutoCharge).toMatchObject({
    enabled: true,
    maxPointsPerTask: 15,
    maxPointsPerDay: 100,
  })
})

test('shows reservation and settlement without double-counting a charge', async ({ page }) => {
  await page.route('**/kb/api/v1/billing/account', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        availablePoints: 94,
        reservedPoints: 0,
        trialPoints: 94,
        purchasedPoints: 0,
        updatedAt: new Date().toISOString(),
      }),
    })
  })
  await page.route('**/kb/api/v1/billing/ledger**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            id: '5',
            operationType: 'settle',
            availableDelta: 0,
            reservedDelta: -3,
            consumedDelta: 3,
            availableAfter: 94,
            reservedAfter: 0,
            referenceType: 'ai_task',
            referenceId: 'task-2',
            businessOperation: 'internet_search',
            description: null,
            createdAt: '2026-10-22T11:26:57Z',
          },
          {
            id: '4',
            operationType: 'reserve',
            availableDelta: -3,
            reservedDelta: 3,
            consumedDelta: 0,
            availableAfter: 94,
            reservedAfter: 3,
            referenceType: 'ai_task',
            referenceId: 'task-2',
            businessOperation: 'internet_search',
            description: null,
            createdAt: '2026-10-22T11:26:56Z',
          },
        ],
        nextCursor: null,
      }),
    })
  })

  await page.goto('/kb/billing')

  await expect(page.locator('.billing-balance-value')).toHaveText('94')
  await expect(page.getByText('冻结 3', { exact: true })).toBeVisible()
  await expect(page.getByText('已扣 3', { exact: true })).toBeVisible()
  await expect(page.getByText('搜索花费', { exact: true })).toHaveCount(2)
  await expect(page.locator('.billing-row small').filter({ hasText: '扣点' })).toHaveCount(1)
  await expect(page.getByText('可用 94 · 冻结 3', { exact: true })).toBeVisible()
  await expect(page.getByText('可用 94 · 冻结 0', { exact: true })).toBeVisible()
  await expect(page.getByText('-3', { exact: true })).toHaveCount(0)
})

test('disables point purchases when the payment channel is unavailable', async ({ page }) => {
  await page.route('**/kb/api/v1/billing/catalog', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ ...billingCatalog, billingEnabled: false }),
    })
  })

  await page.goto('/kb/billing')

  await expect(page.getByText('支付通道暂不可用，现有知识、搜索和手动录入不受影响。')).toBeVisible()
  for (const button of await page.getByRole('button', { name: '购买' }).all()) {
    await expect(button).toBeDisabled()
  }
})

test('keeps a recoverable checkout link when the browser blocks the payment tab', async ({
  page,
}) => {
  const orderId = '123e4567-e89b-42d3-a456-426614174180'
  await page.route('**/kb/api/v1/billing/orders', async (route) => {
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        id: orderId,
        sku: 'kb_pack_9',
        environment: 'test',
        amountMinor: 900,
        currency: 'CNY',
        points: 400,
        providerProductId: 'PROD_9',
        status: 'PENDING',
        checkoutUrl: null,
        paidAt: null,
        closedAt: null,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }),
    })
  })
  await page.route(`**/kb/api/v1/billing/orders/${orderId}/checkout`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        orderId,
        checkoutUrl: 'https://pancake.waffo.ai/store/test/checkout/session',
        expiresAt: '2026-10-22T09:00:00Z',
        environment: 'test',
      }),
    })
  })
  await page.addInitScript(() => {
    window.open = () => null
  })

  await page.goto('/kb/billing')
  await page.getByRole('button', { name: '购买' }).first().click()

  await expect(
    page.getByText('浏览器阻止了新标签页，请使用下方“返回收银台”。'),
  ).toBeVisible()
  await expect(page.getByRole('link', { name: '返回收银台' })).toHaveAttribute(
    'href',
    'https://pancake.waffo.ai/store/test/checkout/session',
  )
})

test('refreshes balance and ledger when a returned payment becomes paid', async ({ page }) => {
  const orderId = '123e4567-e89b-42d3-a456-426614174180'
  let paid = false
  let accountRequests = 0
  await page.route('**/kb/api/v1/billing/catalog', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(billingCatalog),
    })
  })
  await page.route('**/kb/api/v1/billing/account', async (route) => {
    accountRequests += 1
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        availablePoints: paid ? 500 : 100,
        reservedPoints: 0,
        trialPoints: paid ? 100 : 100,
        purchasedPoints: paid ? 400 : 0,
        updatedAt: new Date().toISOString(),
      }),
    })
  })
  await page.route(`**/kb/api/v1/billing/orders/${orderId}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: orderId,
        sku: 'kb_pack_9',
        environment: 'test',
        amountMinor: 900,
        currency: 'CNY',
        points: 400,
        providerProductId: 'PROD_9',
        status: paid ? 'PAID' : 'PENDING',
        checkoutUrl: paid ? null : 'https://pancake.waffo.ai/store/test/checkout/session',
        paidAt: paid ? new Date().toISOString() : null,
        closedAt: null,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }),
    })
  })
  await page.route('**/kb/api/v1/billing/ledger**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: paid
          ? [
              {
                id: '1',
                operationType: 'purchase_grant',
                availableDelta: 400,
                reservedDelta: 0,
                consumedDelta: 0,
                availableAfter: 500,
                reservedAfter: 0,
                referenceType: 'order',
                referenceId: orderId,
                businessOperation: null,
                description: 'Purchase',
                createdAt: new Date().toISOString(),
              },
            ]
          : [],
        nextCursor: null,
      }),
    })
  })
  await page.route(
    (url) => url.pathname === '/kb/api/v1/billing/orders',
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items: [
            {
              id: orderId,
              sku: 'kb_pack_9',
              environment: 'test',
              amountMinor: 900,
              currency: 'CNY',
              points: 400,
              providerProductId: 'PROD_9',
              status: paid ? 'PAID' : 'PENDING',
              checkoutUrl: null,
              paidAt: paid ? new Date().toISOString() : null,
              closedAt: null,
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString(),
            },
          ],
          nextCursor: null,
        }),
      })
    },
  )

  await page.goto(`/kb/billing/return?orderId=${orderId}`)
  const paymentStatus = page.locator('.billing-return .status-pill')
  await expect(paymentStatus).toHaveText('待支付')
  expect(accountRequests).toBeGreaterThan(0)

  paid = true
  await page.getByRole('button', { name: '刷新状态' }).click()

  await expect(paymentStatus).toHaveText('已支付')
  await expect(page.locator('.billing-balance-value')).toHaveText('500')
  await expect(page.getByText('购买到账', { exact: false })).toBeVisible()
  await expect(
    page.locator('.billing-columns > section').first().getByText('轻量加量包', {
      exact: true,
    }),
  ).toBeVisible()
})

test('restores the newest pending order on a payment return without an order id', async ({
  page,
}) => {
  await page.route('**/kb/api/v1/billing/orders**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            id: '123e4567-e89b-42d3-a456-426614174180',
            sku: 'kb_pack_9',
            environment: 'test',
            amountMinor: 900,
            currency: 'CNY',
            points: 400,
            providerProductId: 'PROD_9',
            status: 'CONFIRMING',
            checkoutUrl: null,
            paidAt: null,
            closedAt: null,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
        ],
        nextCursor: null,
      }),
    })
  })

  await page.goto('/kb/billing/return')

  await expect(page.locator('.status-pill')).toHaveText('确认中')
  await expect(page.getByRole('heading', { name: '支付状态' })).toBeVisible()
})

test('shows refund review deadline, unrecovered points, and final outcome', async ({ page }) => {
  await page.route('**/kb/api/v1/billing/refunds', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: '123e4567-e89b-42d3-a456-426614174181',
          orderId: '123e4567-e89b-42d3-a456-426614174180',
          status: 'partially_recovered',
          refundAmountMinor: 900,
          expectedRecoveryPoints: 400,
          recoveredPoints: 250,
          unrecoveredPoints: 150,
          reviewDueAt: '2026-10-25T08:00:00Z',
          version: 2,
          resolution: '部分点数已消费，转人工复核',
          createdAt: '2026-10-22T08:00:00Z',
          resolvedAt: null,
        },
      ]),
    })
  })

  await page.goto('/kb/billing')

  await expect(page.getByText('未回收 150')).toBeVisible()
  await expect(page.getByText(/复核期限/)).toBeVisible()
  await expect(page.getByText('结论：部分点数已消费，转人工复核')).toBeVisible()
})

test('persists AI consent per account and scope', async ({ page }) => {
  const updates: Array<{ scope: string; granted: boolean }> = []
  page.on('request', (request) => {
    if (
      request.url().includes('/kb/api/v1/ai-consents/') &&
      request.method() === 'PUT'
    ) {
      updates.push({
        scope: request.url().split('/').pop() || '',
        granted: (request.postDataJSON() as { granted: boolean }).granted,
      })
    }
  })

  await page.goto('/kb/settings')
  const textConsent = page.getByRole('checkbox', {
    name: 'DeepSeek 分类 · 文本',
  })
  await textConsent.check()
  await expect(textConsent).toBeChecked()
  await page.getByRole('checkbox', { name: 'DeepSeek Vision · 网页' }).check()
  expect(updates).toEqual([
    { scope: 'text', granted: true },
    { scope: 'webpage', granted: true },
  ])
  await textConsent.uncheck()
  await expect(textConsent).not.toBeChecked()
})

test('offers all five prototype capture modes', async ({ page }) => {
  await page.goto('/kb/capture')

  const tabs = page.getByRole('tab')
  await expect(tabs).toHaveCount(5)
  await expect(page.getByRole('tab', { name: '手动录入' })).toHaveAttribute(
    'aria-selected',
    'true',
  )

  await page.getByRole('tab', { name: '网页链接' }).click()
  await expect(page.getByLabel('网页链接', { exact: true })).toBeVisible()

  await page.getByRole('tab', { name: 'AI 文字', exact: true }).click()
  await expect(page.getByLabel('待整理文字')).toBeVisible()

  await page.getByRole('tab', { name: '文档', exact: true }).click()
  await expect(page.getByLabel('选择文档或图片')).toHaveAttribute(
    'accept',
    /\.pdf/,
  )

  await page.getByRole('tab', { name: '截图', exact: true }).click()
  await expect(page.getByTestId('screenshot-paste-zone')).toBeVisible()
  await expect(page.getByTestId('screenshot-paste-zone').locator('input[type=file]')).toHaveCount(0)
})

test('organizes explicitly authorized AI text without using manual creation', async ({ page }) => {
  const textBodies: string[] = []
  let directCreateRequests = 0
  page.on('request', (request) => {
    if (
      request.url().includes('/api/v1/knowledge-items') &&
      request.method() === 'POST'
    ) {
      directCreateRequests += 1
    }
  })
  await page.route('**/kb/api/v1/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'csrf-ai-text', headerName: 'X-XSRF-TOKEN' }),
    })
  })
  await page.route('**/kb/api/v1/text-imports', async (route) => {
    textBodies.push(route.request().postData() || '')
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            ...summary,
            id: '123e4567-e89b-42d3-a456-426614174130',
            body: 'AI organized text',
            bodyDocument: { type: 'doc', content: [{ type: 'paragraph' }] },
            bodySchemaVersion: 1,
            bodyMigrationState: 'completed',
            note: 'AI 摘要',
            processingStatus: 'succeeded',
            redactionApplied: false,
          },
        ],
      }),
    })
  })

  await page.goto('/kb/capture')
  await page.getByRole('tab', { name: 'AI 文字', exact: true }).click()
  await page.getByLabel('待整理文字').fill('这是一段需要 AI 拆分整理的知识文字内容。'.repeat(3))
  await expect(page.getByRole('button', { name: 'AI 整理文字' })).toBeDisabled()
  await page.getByLabel('允许外部 AI 整理这段文字').check()
  await page.getByRole('button', { name: 'AI 整理文字' }).click()
  await page.getByRole('button', { name: /确认 \d+ 点并继续/ }).click()
  await expect(page).toHaveURL(/\/kb\/processing$/)
  expect(textBodies).toHaveLength(1)
  expect(JSON.parse(textBodies[0])).toMatchObject({
    externalAiConsent: true,
  })
  expect(directCreateRequests).toBe(0)
})

test('saves webpage and parsed document capture sources', async ({ page }) => {
  const payloads: Array<Record<string, unknown>> = []
  const webImportPayloads: Array<Record<string, unknown>> = []
  const documentImportBodies: string[] = []
  let nextId = 0
  await page.route('**/kb/api/v1/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'csrf-capture', headerName: 'X-XSRF-TOKEN' }),
    })
  })
  await page.route('**/kb/api/v1/knowledge-items', async (route) => {
    if (route.request().method() !== 'POST') {
      await route.fallback()
      return
    }
    payloads.push(route.request().postDataJSON() as Record<string, unknown>)
    nextId += 1
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        ...summary,
        id: `123e4567-e89b-42d3-a456-42661417410${nextId}`,
        body: '',
        bodyDocument: (payloads.at(-1)?.bodyDocument as object) || null,
        bodySchemaVersion: 1,
        bodyMigrationState: 'completed',
        note: '',
        redactionApplied: false,
      }),
    })
  })
  await page.route('**/kb/api/v1/web-imports', async (route) => {
    webImportPayloads.push(route.request().postDataJSON() as Record<string, unknown>)
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            ...summary,
            id: '123e4567-e89b-42d3-a456-426614174101',
            body: '网页第一段',
            bodyDocument: { type: 'doc', content: [{ type: 'paragraph' }] },
            bodySchemaVersion: 1,
            bodyMigrationState: 'completed',
            note: 'AI 摘要',
            redactionApplied: false,
          },
          {
            ...summary,
            id: '123e4567-e89b-42d3-a456-426614174102',
            title: '网页第二段知识',
            body: '网页第二段',
            bodyDocument: { type: 'doc', content: [{ type: 'paragraph' }] },
            bodySchemaVersion: 1,
            bodyMigrationState: 'completed',
            note: 'AI 摘要',
            redactionApplied: false,
          },
        ],
      }),
    })
  })
  await page.route('**/kb/api/v1/document-imports', async (route) => {
    documentImportBodies.push(route.request().postDataBuffer()?.toString('utf8') || '')
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            ...summary,
            id: '123e4567-e89b-42d3-a456-426614174101',
            body: '文档第一段',
            bodyDocument: { type: 'doc', content: [{ type: 'paragraph' }] },
            bodySchemaVersion: 1,
            bodyMigrationState: 'completed',
            note: '来源格式：PDF',
            processingStatus: 'succeeded',
            redactionApplied: false,
          },
        ],
      }),
    })
  })

  await page.goto('/kb/capture')
  await page.getByRole('tab', { name: '网页链接' }).click()
  await page.getByLabel('网页链接', { exact: true }).fill('https://example.com/article')
  await expect(page.getByRole('button', { name: 'AI整理图文入库' })).toBeDisabled()
  await page.getByLabel('允许外部 AI 处理本页文字').check()
  await page.getByRole('button', { name: 'AI整理图文入库' }).click()
  await page.getByRole('button', { name: /确认 \d+ 点并继续/ }).click()
  await expect(page).toHaveURL(/\/kb\/processing$/)
  expect(webImportPayloads).toHaveLength(1)
  expect(webImportPayloads[0]).toMatchObject({
    url: 'https://example.com/article',
    externalAiConsent: true,
    billingTaskId: '123e4567-e89b-42d3-a456-426614174183',
  })
  expect(payloads).toHaveLength(0)

  await page.goto('/kb/capture')
  await page.getByRole('tab', { name: '文档', exact: true }).click()
  await page.getByLabel('选择文档或图片').setInputFiles({
    name: 'brief.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from('%PDF-1.7\nmock document'),
  })
  await expect(page.getByRole('button', { name: '解析并整理' })).toBeDisabled()
  await page.getByLabel('允许外部 AI 解析并整理此文件').check()
  await page.getByRole('button', { name: '解析并整理' }).click()
  await page.getByRole('button', { name: /确认 \d+ 点并继续/ }).click()
  await expect(page).toHaveURL(/\/kb\/processing$/)
  expect(payloads).toHaveLength(0)
  expect(documentImportBodies).toHaveLength(1)
  expect(documentImportBodies[0]).toContain('brief.pdf')
  expect(documentImportBodies[0]).toContain('externalAiConsent')
})

test('prices document extraction by each 10000-character preflight block', async ({ page }) => {
  await page.route('**/kb/api/v1/billing/preflights', async (route) => {
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        preflightId: '123e4567-e89b-42d3-a456-426614174184',
        measuredCharactersUpperBound: 25000,
        viewCount: 1,
        pricingBasis: 'document_chars',
        expiresAt: new Date(Date.now() + 600000).toISOString(),
      }),
    })
  })

  await page.goto('/kb/capture')
  await page.getByRole('tab', { name: '文档', exact: true }).click()
  await page.getByLabel('选择文档或图片').setInputFiles({
    name: 'long-brief.docx',
    mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    buffer: Buffer.from('test document'),
  })
  await page.getByLabel('允许外部 AI 解析并整理此文件').check()

  const quoteRequestPromise = page.waitForRequest(
    (request) =>
      request.url().endsWith('/kb/api/v1/billing/quotes') &&
      request.method() === 'POST',
  )
  await page.getByRole('button', { name: '解析并整理' }).click()
  const quoteRequest = await quoteRequestPromise
  const quoteBody = quoteRequest.postDataJSON() as {
    lines: Array<{ feature: string; quantity: number }>
  }

  expect(quoteBody.lines).toContainEqual({
    feature: 'document_extract',
    quantity: 3,
  })
})

test('sends document images to AI import without attachment uploads', async ({ page }) => {
  let documentImportRequests = 0
  let imageUploadRequests = 0
  let directCreateRequests = 0
  page.on('request', (request) => {
    if (request.url().includes('/api/v1/rich-text/image-uploads')) imageUploadRequests += 1
    if (
      request.url().includes('/api/v1/knowledge-items') &&
      request.method() === 'POST'
    ) {
      directCreateRequests += 1
    }
  })
  await page.route('**/kb/api/v1/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'csrf-document-image', headerName: 'X-XSRF-TOKEN' }),
    })
  })
  await page.route('**/kb/api/v1/document-imports', async (route) => {
    documentImportRequests += 1
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            ...summary,
            id: '123e4567-e89b-42d3-a456-426614174140',
            body: '图片 OCR 后由 AI 整理的知识',
            bodyDocument: { type: 'doc', content: [{ type: 'paragraph' }] },
            bodySchemaVersion: 1,
            bodyMigrationState: 'completed',
            note: '来源格式：PNG 图片',
            processingStatus: 'succeeded',
            redactionApplied: false,
          },
        ],
      }),
    })
  })

  await page.goto('/kb/capture')
  await page.getByRole('tab', { name: '文档', exact: true }).click()
  const input = page.getByLabel('选择文档或图片')
  await expect(input).toHaveAttribute(
    'accept',
    /image\/png/,
  )
  await input.setInputFiles({
    name: 'notes.png',
    mimeType: 'image/png',
    buffer: Buffer.from('mock png'),
  })
  await expect(page.getByRole('button', { name: '解析并整理' })).toBeDisabled()
  await page.getByLabel('允许外部 AI 解析并整理此文件').check()
  await page.getByRole('button', { name: '解析并整理' }).click()
  await page.getByRole('button', { name: /确认 \d+ 点并继续/ }).click()
  await expect(page).toHaveURL(/\/kb\/processing$/)
  expect(documentImportRequests).toBe(1)
  expect(imageUploadRequests).toBe(0)
  expect(directCreateRequests).toBe(0)
})

test('recognizes and splits a clipboard screenshot without saving the image', async ({ page }) => {
  const screenshotBodies: string[] = []
  let imageUploadRequests = 0
  let knowledgeCreateRequests = 0
  page.on('request', (request) => {
    if (request.url().includes('/rich-text/image-uploads')) imageUploadRequests += 1
    if (
      request.url().includes('/api/v1/knowledge-items') &&
      request.method() === 'POST'
    ) {
      knowledgeCreateRequests += 1
    }
  })
  await page.route('**/kb/api/v1/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'csrf-screenshot', headerName: 'X-XSRF-TOKEN' }),
    })
  })
  await page.route('**/kb/api/v1/screenshot-imports', async (route) => {
    screenshotBodies.push(route.request().postDataBuffer()?.toString('utf8') || '')
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            ...summary,
            id: '123e4567-e89b-42d3-a456-426614174103',
            body: '截图整理出的第一段知识',
            bodyDocument: { type: 'doc', content: [{ type: 'paragraph' }] },
            bodySchemaVersion: 1,
            bodyMigrationState: 'completed',
            note: '来源格式：PNG 截图',
            processingStatus: 'succeeded',
            redactionApplied: false,
          },
          {
            ...summary,
            id: '123e4567-e89b-42d3-a456-426614174104',
            title: '截图整理出的第二段知识',
            body: '截图整理出的第二段知识',
            bodyDocument: { type: 'doc', content: [{ type: 'paragraph' }] },
            bodySchemaVersion: 1,
            bodyMigrationState: 'completed',
            note: '来源格式：PNG 截图',
            processingStatus: 'succeeded',
            redactionApplied: false,
          },
        ],
      }),
    })
  })

  await page.goto('/kb/capture')
  await page.getByRole('tab', { name: '截图', exact: true }).click()
  await page.evaluate(() => {
    const bytes = Uint8Array.from(
      atob('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2nGQAAAAASUVORK5CYII='),
      (char) => char.charCodeAt(0),
    )
    const file = new File([bytes], 'clipboard.png', { type: 'image/png' })
    const transfer = new DataTransfer()
    transfer.items.add(file)
    document
      .querySelector('[data-testid="screenshot-paste-zone"] textarea')
      ?.dispatchEvent(
        new ClipboardEvent('paste', {
          bubbles: true,
          cancelable: true,
          clipboardData: transfer,
        }),
      )
  })
  await expect(page.getByText('剪贴板截图已就绪')).toBeVisible()
  await expect(page.getByRole('button', { name: '识别并拆分' })).toBeDisabled()
  await page.getByLabel('允许外部 AI 解析并整理这张截图').check()
  await page.getByRole('button', { name: '识别并拆分' }).click()
  await page.getByRole('button', { name: /确认 \d+ 点并继续/ }).click()
  await expect(page).toHaveURL(/\/kb\/processing$/)
  expect(screenshotBodies).toHaveLength(1)
  expect(screenshotBodies[0]).toContain('clipboard.png')
  expect(screenshotBodies[0]).toContain('externalAiConsent')
  expect(imageUploadRequests).toBe(0)
  expect(knowledgeCreateRequests).toBe(0)
})

test('continues to internet search only after explicit consent and saves a candidate', async ({
  page,
}) => {
  let externalRequests = 0
  let consentPayload: Record<string, unknown> | undefined
  let savePayload: Record<string, unknown> | undefined
  let candidateQuotePayload: Record<string, unknown> | undefined
  page.on('request', (request) => {
    if (
      request.url().endsWith('/kb/api/v1/billing/quotes') &&
      request.method() === 'POST'
    ) {
      const payload = request.postDataJSON() as {
        operation?: string
      }
      if (payload.operation === 'internet_import') {
        candidateQuotePayload = payload as Record<string, unknown>
      }
    }
  })
  await page.route('**/kb/api/v1/search/external', async (route) => {
    externalRequests += 1
    consentPayload = route.request().postDataJSON() as Record<string, unknown>
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            candidateId: '123e4567-e89b-42d3-a456-426614174120',
            title: 'DeepSeek Search result',
            url: 'https://www.deepseek.com/',
            snippet: 'A live internet search candidate.',
            provider: 'DeepSeek',
            expiresAt: '2026-10-22T12:15:00Z',
          },
        ],
      }),
    })
  })
  await page.route(
    '**/kb/api/v1/search/external/123e4567-e89b-42d3-a456-426614174120/save',
    async (route) => {
      savePayload = route.request().postDataJSON() as Record<string, unknown>
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          items: [
            {
              ...summary,
              id: '123e4567-e89b-42d3-a456-426614174121',
              body: 'AI organized web content',
              bodyDocument: { type: 'doc', content: [{ type: 'paragraph' }] },
              bodySchemaVersion: 1,
              bodyMigrationState: 'completed',
              note: 'Internet summary',
              processingStatus: 'succeeded',
              redactionApplied: false,
            },
          ],
        }),
      })
    },
  )

  await page.goto('/kb/')
  const internetButton = page.getByRole('button', { name: '互联网搜索 · 1 点' })
  await expect(internetButton).toBeVisible()
  await expect(internetButton).toBeDisabled()
  await page.getByRole('searchbox', { name: '搜索全部知识' }).fill('DeepSeek')
  await page.getByRole('button', { name: '潜入检索' }).click()
  await expect(page.getByText('找到 1 条知识')).toBeVisible()
  expect(externalRequests).toBe(0)

  await expect(internetButton).toBeEnabled()
  await internetButton.click()
  await expect(page.getByText('DeepSeek Search result')).toBeVisible()
  expect(consentPayload).toMatchObject({
    query: 'DeepSeek',
    explicitConsent: true,
    includeAnswer: false,
  })

  await page.getByRole('button', { name: 'AI 整理后收入' }).click()
  await page.getByRole('button', { name: '确认 15 点并收入' }).click()
  await expect(page).toHaveURL(/\/kb\/library\/123e4567-e89b-42d3-a456-426614174121$/)
  expect(candidateQuotePayload).toMatchObject({
    operation: 'internet_import',
    lines: [
      { feature: 'webpage_vision', quantity: 1 },
      { feature: 'classify', quantity: 1 },
    ],
  })
  expect(savePayload).toEqual({
    externalAiConsent: true,
    billingTaskId: '123e4567-e89b-42d3-a456-426614174183',
  })
})

test('keeps other internet results available when one source cannot be fetched', async ({
  page,
}) => {
  await page.route('**/kb/api/v1/search/external', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            candidateId: '123e4567-e89b-42d3-a456-426614174130',
            title: 'Unsupported 36Kr result',
            url: 'https://eu.36kr.com/zh/p/4035009546000259',
            snippet: 'The provider cannot fetch this source.',
            provider: '36Kr',
            expiresAt: '2026-10-22T12:15:00Z',
          },
          {
            candidateId: '123e4567-e89b-42d3-a456-426614174131',
            title: 'Available Jiemian result',
            url: 'https://www.jiemian.com/article/15211437.html',
            snippet: 'This source can be fetched.',
            provider: 'Jiemian',
            expiresAt: '2026-10-22T12:15:00Z',
          },
        ],
      }),
    })
  })
  await page.route(
    '**/kb/api/v1/search/external/123e4567-e89b-42d3-a456-426614174130/save',
    async (route) => {
      await route.fulfill({
        status: 422,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'EXTERNAL_SOURCE_UNAVAILABLE',
          message: 'The selected source could not be fetched.',
          requestId: 'source-unavailable',
          retryable: false,
        }),
      })
    },
  )

  await page.goto('/kb/')
  await page.getByRole('searchbox', { name: '搜索全部知识' }).fill('梁文峰')
  await page.getByRole('button', { name: '潜入检索' }).click()
  await expect(page.getByRole('button', { name: '互联网搜索 · 1 点' })).toBeEnabled()
  await page.getByRole('button', { name: '互联网搜索 · 1 点' }).click()

  const firstCandidate = page.locator('.external-row').first()
  await firstCandidate.getByRole('button', { name: 'AI 整理后收入' }).click()
  await firstCandidate.getByRole('button', { name: /确认 \d+ 点并收入/ }).click()

  await expect(firstCandidate.getByText('当前来源不支持正文抓取，请换一条搜索结果。')).toBeVisible()
  await expect(page.getByText('Available Jiemian result')).toBeVisible()
  await expect(page.getByText('互联网搜索暂时不可用，本地结果不受影响。')).toHaveCount(0)
})

test('shows unorganizable internet results with the import action disabled', async ({ page }) => {
  await page.route('**/kb/api/v1/search/external', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            candidateId: '123e4567-e89b-42d3-a456-426614174140',
            title: 'Unorganizable result',
            url: 'https://example.com/no-content',
            snippet: 'No usable article content.',
            provider: 'Example',
            organizable: false,
            expiresAt: '2026-10-22T12:15:00Z',
          },
        ],
      }),
    })
  })

  await page.goto('/kb/')
  await page.getByRole('searchbox', { name: '搜索全部知识' }).fill('no-content')
  await page.getByRole('button', { name: '潜入检索' }).click()
  await page.getByRole('button', { name: '互联网搜索 · 1 点' }).click()

  const candidate = page.locator('.external-row').first()
  const importButton = candidate.getByRole('button', { name: 'AI 整理后收入' })
  await expect(importButton).toBeDisabled()
  await expect(candidate.locator('.candidate-action-wrap')).toHaveAttribute(
    'title',
    '该来源内容不足，无法通过 AI 整理收入。',
  )
  await candidate.locator('.candidate-action-wrap').click()
  await expect(
    candidate.getByText('该来源内容不足，无法通过 AI 整理收入。'),
  ).toBeVisible()
})

test('shows already imported internet results without charging again', async ({ page }) => {
  const existingItemId = '123e4567-e89b-42d3-a456-426614174149'
  await page.route('**/kb/api/v1/search/external', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            candidateId: '123e4567-e89b-42d3-a456-426614174148',
            title: 'Already saved result',
            url: 'https://example.com/already-saved?utm_source=repeat',
            snippet: 'Same page, canonical URL already exists.',
            provider: 'Example',
            organizable: true,
            alreadyImported: true,
            existingItemId,
            existingItemTitle: 'Existing knowledge',
            existingItemCount: 2,
            expiresAt: '2026-10-22T12:15:00Z',
          },
        ],
      }),
    })
  })

  await page.goto('/kb/')
  await page.getByRole('searchbox', { name: '搜索全部知识' }).fill('already-saved')
  await page.getByRole('button', { name: '潜入检索' }).click()
  await page.getByRole('button', { name: '互联网搜索 · 1 点' }).click()

  const candidate = page.locator('.external-row').first()
  await expect(candidate.getByRole('button', { name: '已收录 2 篇' })).toBeDisabled()
  await expect(candidate.getByText('同一网页已整理过，未重复调用 AI 或扣点。')).toBeVisible()
  await expect(candidate.getByRole('link', { name: '查看已收录知识' })).toHaveAttribute(
    'href',
    `/kb/library/${existingItemId}`,
  )
  await page.setViewportSize({ width: 390, height: 844 })
  await expect(candidate.getByRole('button', { name: '已收录 2 篇' })).toBeDisabled()
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
    ),
  ).toBe(true)
})

test('searches the internet once with a one-point click and no second confirmation', async ({
  page,
}) => {
  const requestOrder: string[] = []
  let quotePayload: Record<string, unknown> | undefined
  let confirmPayload: Record<string, unknown> | undefined
  let searchPayload: Record<string, unknown> | undefined

  await page.route('**/kb/api/v1/billing/quotes', async (route) => {
    requestOrder.push('create quote')
    quotePayload = route.request().postDataJSON() as Record<string, unknown>
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        quoteId: '123e4567-e89b-42d3-a456-426614174190',
        status: 'QUOTED',
        lines: [
          {
            feature: 'internet_search',
            unit: 'entry',
            quantity: 1,
            pointsPerUnit: 1,
            maxPoints: 1,
            successRule: 'success',
          },
        ],
        imageResources: [],
        maxPoints: 1,
        availablePoints: 100,
        projectedRemainingPoints: 99,
        expiresAt: new Date(Date.now() + 600000).toISOString(),
        autoChargeEligible: false,
      }),
    })
  })
  await page.route('**/kb/api/v1/billing/quotes/*/confirm', async (route) => {
    requestOrder.push('confirm quote')
    confirmPayload = route.request().postDataJSON() as Record<string, unknown>
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: '123e4567-e89b-42d3-a456-426614174191',
        status: 'RESERVED',
        lines: [],
        maxPoints: 1,
        settledPoints: 0,
        reservedPoints: 1,
        expiresAt: new Date(Date.now() + 600000).toISOString(),
        autoChargeApplied: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }),
    })
  })
  await page.route('**/kb/api/v1/search/external', async (route) => {
    requestOrder.push('search')
    searchPayload = route.request().postDataJSON() as Record<string, unknown>
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [
          {
            candidateId: '123e4567-e89b-42d3-a456-426614174192',
            title: 'One-click internet result',
            url: 'https://www.deepseek.com/',
            snippet: 'Returned without a second confirmation.',
            provider: 'DeepSeek',
            expiresAt: '2026-10-22T12:15:00Z',
          },
        ],
      }),
    })
  })

  await page.goto('/kb/')
  await page.getByRole('searchbox', { name: '搜索全部知识' }).fill('DeepSeek')
  await page.getByRole('button', { name: '潜入检索' }).click()
  await expect(page.getByText('找到 1 条知识')).toBeVisible()

  const internetButton = page.getByRole('button', { name: /互联网搜索 · 1 点/ })
  await expect(internetButton).toBeEnabled()
  const labelBeforeClick = (await internetButton.innerText()).replace(/\s+/g, ' ')
  await internetButton.click()

  await expect
    .poll(
      async () => {
        if (requestOrder.length >= 3) return 'searched'
        if (await page.getByText('互联网搜索点数').isVisible()) {
          return 'awaiting confirmation'
        }
        return 'pending'
      },
      { timeout: 3_000 },
    )
    .not.toBe('pending')

  expect.soft(labelBeforeClick).toContain('1 点')
  expect.soft(requestOrder).toEqual(['create quote', 'confirm quote', 'search'])
  await expect.soft(page.getByText('互联网搜索点数')).toHaveCount(0)
  await expect
    .soft(page.getByRole('button', { name: /确认 \d+ 点并搜索/ }))
    .toHaveCount(0)
  await expect.soft(page.getByText('One-click internet result')).toBeVisible()
  expect.soft(quotePayload).toMatchObject({
    operation: 'internet_search',
    lines: [{ feature: 'internet_search', quantity: 1 }],
  })
  expect.soft(confirmPayload).toEqual({
    expectedMaxPoints: 1,
    explicitConsent: true,
  })
  expect.soft(searchPayload).toMatchObject({
    query: 'DeepSeek',
    explicitConsent: true,
    includeAnswer: false,
    billingTaskId: '123e4567-e89b-42d3-a456-426614174191',
  })
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
        displayName: 'Owner',
        locale: 'zh-CN',
        csrfToken: 'csrf-session',
      }),
    })
  })

  await page.goto('/kb/login')
  await expect(page.locator('.login-ocean-map')).toBeVisible()
  await expect(page.locator('.login-ocean-map path')).toHaveCount(13)
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
  await expect(page.locator('.login-ocean-map')).toBeVisible()
  await expect(page.locator('.login-ocean-map path')).toHaveCount(13)
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

test('renders an AI-organized rich text knowledge document without v-html', async ({ page }) => {
  const item = {
    ...summary,
    body: '正文',
    processingStatus: 'review',
    source: { kind: 'webpage', inputMethod: 'webpage' },
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
          type: 'bulletList',
          content: [
            {
              type: 'listItem',
              content: [
                {
                  type: 'paragraph',
                  content: [{ type: 'text', text: '自动归类的关键结论' }],
                },
              ],
            },
          ],
        },
        {
          type: 'codeBlock',
          attrs: { language: 'text' },
          content: [{ type: 'text', text: 'search -> organize -> review' }],
        },
        {
          type: 'image',
          attrs: {
            assetId: '123e4567-e89b-42d3-a456-426614174001',
            nodeId: 'node-image',
            alt: 'AI 整理架构图',
            caption: '来源图示：从网页内容整理出的架构流程',
            displayWidthPx: 720,
            aspectRatio: 2.2,
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
      contentType: 'image/svg+xml',
      body: `<svg xmlns="http://www.w3.org/2000/svg" width="880" height="400" viewBox="0 0 880 400">
        <rect width="880" height="400" rx="24" fill="#071c31"/>
        <g fill="none" stroke="#4ec9ef" stroke-width="4">
          <path d="M210 200H312"/>
          <path d="M462 200H564"/>
        </g>
        <g fill="#0d3552" stroke="#5ed5f8" stroke-width="3">
          <rect x="48" y="142" width="162" height="116" rx="18"/>
          <rect x="312" y="142" width="150" height="116" rx="18"/>
          <rect x="564" y="142" width="150" height="116" rx="18"/>
          <rect x="714" y="65" width="126" height="116" rx="18"/>
          <rect x="714" y="219" width="126" height="116" rx="18"/>
        </g>
        <g fill="#dff7ff" font-family="sans-serif" font-size="24" text-anchor="middle">
          <text x="129" y="207">网页内容</text>
          <text x="387" y="207">AI 整理</text>
          <text x="639" y="207">安全入库</text>
          <text x="777" y="130">富文本</text>
          <text x="777" y="284">来源图示</text>
        </g>
      </svg>`,
    })
  })

  await page.goto('/kb/library/123e4567-e89b-42d3-a456-426614174000')
  await expect(page.getByRole('heading', { level: 2, name: '结构化正文' })).toBeVisible()
  await expect(page.getByText('安全渲染')).toBeVisible()
  await expect(page.getByText('自动归类的关键结论')).toBeVisible()
  await expect(page.getByText('search -> organize -> review')).toBeVisible()
  await expect(page.getByRole('img', { name: 'AI 整理架构图' })).toBeVisible()
  await expect(page.getByText('来源图示：从网页内容整理出的架构流程')).toBeVisible()
  for (const viewport of [
    { width: 1440, height: 900 },
    { width: 390, height: 844 },
  ]) {
    await page.setViewportSize(viewport)
    await page.reload()
    await expect(page.getByRole('img', { name: 'AI 整理架构图' })).toBeVisible()
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
      ),
    ).toBe(true)
    await page.screenshot({
      path: `test-results/ai-rich-text-detail-${viewport.width}.png`,
      fullPage: true,
    })
  }
})

test('saves a structured Tiptap body from the capture screen', async ({ page }) => {
  let posted: Record<string, unknown> | undefined
  let savedItem: Record<string, unknown> | undefined
  let aiImportRequests = 0
  page.on('request', (request) => {
    if (
      ['/text-imports', '/web-imports', '/document-imports', '/screenshot-imports'].some(
        (path) => request.url().includes(path),
      )
    ) {
      aiImportRequests += 1
    }
  })
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
  expect(aiImportRequests).toBe(0)
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

test('keeps an absent category separate from an unrequested processing status', async ({ page }) => {
  await page.route(`**/kb/api/v1/knowledge-items/${summary.id}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        ...summary,
        category: null,
        tags: [],
        processingStatus: 'not_requested',
        body: '手动记录但尚未请求 AI 整理。',
        bodyDocument: null,
        bodySchemaVersion: null,
        bodyMigrationState: 'not_required',
        note: '',
        redactionApplied: false,
      }),
    })
  })

  await page.goto(`/kb/library/${summary.id}`)
  const aside = page.locator('.detail-aside')
  await expect(aside.getByText('未分类')).toBeVisible()
  await expect(aside.getByText('未整理')).toHaveCount(1)
  await expect(page.locator('.detail-meta').getByText('未整理')).toBeVisible()
  await expect(page.getByText('待整理')).toHaveCount(0)
})

test('confirms an AI review from the knowledge detail page', async ({ page }) => {
  const id = '123e4567-e89b-42d3-a456-426614174153'
  const item: KnowledgeItem = {
    ...summary,
    id,
    title: '待复核的截图知识',
    processingStatus: 'review',
    source: { kind: 'screenshot', inputMethod: 'screenshot' },
    body: '截图 OCR 后由 AI 整理的知识。',
    bodyDocument: {
      type: 'doc',
      content: [
        {
          type: 'paragraph',
          content: [{ type: 'text', text: '截图 OCR 后由 AI 整理的知识。' }],
        },
      ],
    },
    bodySchemaVersion: 1,
    bodyMigrationState: 'completed',
    note: '',
    redactionApplied: false,
  }
  let reviewRequests = 0

  await page.route(`**/kb/api/v1/knowledge-items/${id}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(item),
    })
  })
  await page.route(`**/kb/api/v1/knowledge-items/${id}/review`, async (route) => {
    reviewRequests += 1
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ ...item, processingStatus: 'succeeded' }),
    })
  })

  await page.goto(`/kb/library/${id}`)
  await expect(page.getByRole('button', { name: '确认复核' })).toBeVisible()
  const deleteAction = page.getByRole('button', { name: '删除', exact: true })
  const editAction = page.getByRole('link', { name: '编辑知识', exact: true })
  const reviewAction = page.getByRole('button', { name: '确认复核', exact: true })
  await expect(deleteAction).toHaveCSS('white-space', 'nowrap')
  const editBox = await editAction.boundingBox()
  const reviewBox = await reviewAction.boundingBox()
  expect(editBox).not.toBeNull()
  expect(reviewBox).not.toBeNull()
  expect(reviewBox!.x).toBeGreaterThan(editBox!.x)
  for (const viewport of [
    { width: 1920, height: 1080 },
    { width: 1440, height: 900 },
    { width: 768, height: 900 },
    { width: 390, height: 844 },
  ]) {
    await page.setViewportSize(viewport)
    await page.screenshot({
      path: `test-results/review-detail-${viewport.width}.png`,
      fullPage: true,
    })
  }
  await page.getByRole('button', { name: '确认复核' }).click()

  expect(reviewRequests).toBe(1)
  await expect(page.locator('.detail-meta').getByText('已整理')).toBeVisible()
  await expect(page.getByRole('button', { name: '确认复核' })).toHaveCount(0)
  await expect(page.getByRole('status')).toContainText('复核已确认')
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

