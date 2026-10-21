import { expect, test } from '../support/playwright'
import {
  ITEM_ID,
  fixtureJson,
  isLocalUrl,
  knowledgeItem,
  routePng,
} from '../support/rich-text'

function imageNode(src: string, index: number) {
  return {
    type: 'image',
    attrs: {
      assetId: src,
      nodeId: `remote-${index}`,
      alt: `remote ${index}`,
      caption: '',
      displayWidthPx: 640,
      aspectRatio: 1.5,
      alignment: 'center',
      decorative: false,
      src,
    },
  }
}

test('makes zero external requests for remote, data, blob, cid and file image sources', async ({
  page,
}) => {
  const requests: Array<{ method: string; url: string }> = []
  page.on('request', (request) => {
    requests.push({ method: request.method(), url: request.url() })
  })

  const sources = [
    'https://example.invalid/remote.png',
    'http://127.0.0.1/private.png',
    'data:image/png;base64,AAAA',
    'blob:https://example.invalid/not-created',
    'cid:embedded-image',
    'file:///private/image.png',
  ]
  const document = {
    type: 'doc',
    content: sources.map((source, index) => imageNode(source, index)),
  }

  await page.route(`**/kb/api/v1/knowledge-items/${ITEM_ID}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(knowledgeItem(document)),
    })
  })

  await page.goto(`/kb/library/${ITEM_ID}`)
  await expect(page.getByRole('alert')).toBeVisible()

  const externalRequests = requests.filter((request) => !isLocalUrl(request.url))
  expect(externalRequests).toEqual([])
  await expect(page.locator('img[src^="http"], img[src^="data:"], img[src^="blob:"]')).toHaveCount(0)
})

test('requests image bytes only from the authenticated attachment endpoint and renders a blob URL', async ({
  page,
}) => {
  const attachmentRequests: string[] = []
  const externalRequests: string[] = []
  page.on('request', (request) => {
    if (request.url().includes('/kb/api/v1/attachments/')) {
      attachmentRequests.push(request.url())
    } else if (!isLocalUrl(request.url())) {
      externalRequests.push(request.url())
    }
  })

  await page.route(`**/kb/api/v1/knowledge-items/${ITEM_ID}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(knowledgeItem(fixtureJson('images/valid-image.json'))),
    })
  })
  await routePng(page)

  await page.goto(`/kb/library/${ITEM_ID}`)
  await expect(page.getByRole('img', { name: '一张已扫描的安全测试图' })).toBeVisible()
  await expect
    .poll(() =>
      page.locator('.document-image img').evaluateAll((images) =>
        images.map((image) => image.getAttribute('src') || ''),
      ),
    )
    .toHaveLength(1)

  const imageSources = await page.locator('.document-image img').evaluateAll((images) =>
    images.map((image) => image.getAttribute('src') || ''),
  )
  expect(attachmentRequests).toHaveLength(1)
  expect(attachmentRequests[0]).toMatch(
    /\/kb\/api\/v1\/attachments\/123e4567-e89b-42d3-a456-426614174001\/content$/,
  )
  expect(imageSources.every((source) => source.startsWith('blob:'))).toBe(true)
  expect(externalRequests).toEqual([])
})
