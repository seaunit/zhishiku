import { readFileSync } from 'node:fs'
import type { Page, Route } from './playwright'

export const ITEM_ID = '123e4567-e89b-42d3-a456-426614174000'
export const IMAGE_ID = '123e4567-e89b-42d3-a456-426614174001'

const fixtureRoot = new URL('../fixtures/rich-text/', import.meta.url)

export function fixtureJson<T>(relativePath: string): T {
  return JSON.parse(readFileSync(new URL(relativePath, fixtureRoot), 'utf8')) as T
}

export function fixtureText(relativePath: string): string {
  return readFileSync(new URL(relativePath, fixtureRoot), 'utf8')
}

export function fixtureBase64(relativePath: string): Buffer {
  return Buffer.from(readFileSync(new URL(relativePath, fixtureRoot), 'utf8').trim(), 'base64')
}

export function knowledgeItem(bodyDocument: unknown, overrides: Record<string, unknown> = {}) {
  return {
    id: ITEM_ID,
    title: '富文本安全与无障碍验收',
    bodyPreview: '正文摘要',
    summary: '富文本验收夹具',
    processingStatus: 'succeeded',
    lifecycleState: 'active',
    source: { kind: 'manual', inputMethod: 'manual' },
    category: { id: 'cat-rich', name: '知识管理', status: 'applied', origin: 'ai' },
    tags: [{ id: 'tag-rich', name: '富文本', status: 'applied', origin: 'ai' }],
    version: 1,
    createdAt: '2026-10-20T08:00:00Z',
    updatedAt: '2026-10-21T08:00:00Z',
    body: '正文',
    bodyDocument,
    bodySchemaVersion: 1,
    bodyMigrationState: 'completed',
    note: '验收备注',
    redactionApplied: false,
    ...overrides,
  }
}

export async function routeDetail(page: Page, bodyDocument: unknown, overrides = {}) {
  await page.route(`**/kb/api/v1/knowledge-items/${ITEM_ID}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(knowledgeItem(bodyDocument, overrides)),
    })
  })
}

export async function routePng(page: Page) {
  const png = fixtureBase64('images/pixel.png.base64')
  await page.route('**/kb/api/v1/attachments/**/content', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'image/png',
      body: png,
    })
  })
}

export async function openDetail(page: Page, bodyDocument: unknown, overrides = {}) {
  await routeDetail(page, bodyDocument, overrides)
  await routePng(page)
  await page.goto(`/kb/library/${ITEM_ID}`)
  await page.getByRole('heading', { level: 1 }).waitFor()
}

export async function pasteHtml(page: Page, html: string, plain: string) {
  await page.locator('.ProseMirror').evaluate(
    (element, payload) => {
      const transfer = new DataTransfer()
      transfer.setData('text/html', payload.html)
      transfer.setData('text/plain', payload.plain)
      element.dispatchEvent(
        new ClipboardEvent('paste', {
          bubbles: true,
          cancelable: true,
          clipboardData: transfer,
        }),
      )
    },
    { html, plain },
  )
}

export function isLocalUrl(url: string, baseURL = 'http://127.0.0.1:4173') {
  try {
    return new URL(url).origin === new URL(baseURL).origin
  } catch {
    return true
  }
}

export async function fulfillCaptureSave(page: Page, capture: (body: unknown) => void) {
  await page.route('**/kb/api/v1/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'csrf-rich-text', headerName: 'X-XSRF-TOKEN' }),
    })
  })
  await page.route('**/kb/api/v1/knowledge-items', async (route: Route) => {
    if (route.request().method() !== 'POST') {
      await route.fallback()
      return
    }
    const body = route.request().postDataJSON()
    capture(body)
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify(
        knowledgeItem(
          (body as { bodyDocument?: unknown }).bodyDocument ?? {
            type: 'doc',
            content: [],
          },
          { title: (body as { title?: string }).title || '富文本测试' },
        ),
      ),
    })
  })
}
