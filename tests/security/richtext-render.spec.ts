import { expect, test } from '../support/playwright'
import {
  IMAGE_ID,
  ITEM_ID,
  fixtureJson,
  isLocalUrl,
  knowledgeItem,
  routePng,
} from '../support/rich-text'

const unsafeDocuments = [
  {
    id: 'script-node',
    document: {
      type: 'doc',
      content: [
        {
          type: 'script',
          attrs: { src: 'https://example.invalid/xss.js' },
          content: [{ type: 'text', text: 'window.__renderXss = true' }],
        },
      ],
    },
  },
  {
    id: 'event-and-form',
    document: {
      type: 'doc',
      content: [
        {
          type: 'form',
          attrs: {
            action: 'https://example.invalid/post',
            onsubmit: 'window.__renderXss = true',
          },
          content: [{ type: 'text', text: 'form' }],
        },
      ],
    },
  },
  {
    id: 'iframe',
    document: fixtureJson('invalid/unknown-node.json'),
  },
  {
    id: 'dangerous-attrs',
    document: fixtureJson('invalid/dangerous-attributes.json'),
  },
  {
    id: 'remote-image',
    document: fixtureJson('images/unsafe-remote-image.json'),
  },
]

test('rejects scripts, events, forms, iframes and unsafe image sources without executing them', async ({
  page,
}) => {
  const externalRequests: string[] = []
  page.on('request', (request) => {
    if (!isLocalUrl(request.url())) {
      externalRequests.push(request.url())
    }
  })

  let current = unsafeDocuments[0].document
  await page.route(`**/kb/api/v1/knowledge-items/${ITEM_ID}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(knowledgeItem(current)),
    })
  })

  for (const testCase of unsafeDocuments) {
    current = testCase.document
    await page.goto(`/kb/library/${ITEM_ID}`)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()

    await expect(page.getByRole('alert')).toBeVisible()
    await expect(page.locator('.rich-text-view')).toHaveCount(0)
    await expect(page.locator('#app').locator('script, iframe, form, object, embed')).toHaveCount(
      0,
    )
    await expect(
      page.locator('#app').locator(
        '[onclick], [onerror], [onload], [onsubmit], img[src^="http"], img[src^="data:"], a[href^="javascript:"], a[href^="data:"]',
      ),
    ).toHaveCount(0)

    const state = await page.evaluate(() => ({
      renderXss: (window as Window & { __renderXss?: unknown }).__renderXss,
      remoteImageXss: (window as Window & { __remoteImageXss?: unknown }).__remoteImageXss,
    }))
    expect(state).toEqual({ renderXss: undefined, remoteImageXss: undefined })
  }

  expect(externalRequests).toEqual([])
})

test('renders only a scanned attachment through a blob URL and exposes caption and alt text', async ({
  page,
}) => {
  const document = fixtureJson('images/valid-image.json')
  await page.route(`**/kb/api/v1/knowledge-items/${ITEM_ID}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(knowledgeItem(document)),
    })
  })
  await routePng(page)

  await page.goto(`/kb/library/${ITEM_ID}`)
  const image = page.getByRole('img', { name: '一张已扫描的安全测试图' })
  await expect(image).toBeVisible()
  await expect(page.getByText('图片说明会进入可访问文本和纯文本投影')).toBeVisible()

  const sources = await page.locator('.document-image img').evaluateAll((images) =>
    images.map((image) => ({
      src: image.getAttribute('src') || '',
      loading: image.getAttribute('loading'),
      decoding: image.getAttribute('decoding'),
    })),
  )
  expect(sources).toEqual([
    {
      src: expect.stringMatching(/^blob:http:\/\/127\.0\.0\.1:4173\//),
      loading: 'lazy',
      decoding: 'async',
    },
  ])
  expect(sources[0]?.src).not.toContain(IMAGE_ID)
})
