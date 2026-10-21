import { mkdir, writeFile } from 'node:fs/promises'
import { expect, test } from '../../support/playwright'
import {
  ITEM_ID,
  fixtureJson,
  knowledgeItem,
  routePng,
} from '../../support/rich-text'

const budget = {
  detail200kMs: Number(process.env.KB_PERF_DETAIL_200K_MS || 15_000),
  detail20ImagesMs: Number(process.env.KB_PERF_20_IMAGES_MS || 15_000),
  mobileInput200kMs: Number(process.env.KB_PERF_MOBILE_INPUT_200K_MS || 15_000),
}

function imagesDocument(count: number) {
  return {
    type: 'doc',
    content: Array.from({ length: count }, (_, index) => ({
      type: 'image',
      attrs: {
        assetId: `123e4567-e89b-42d3-a456-${String(index + 1).padStart(12, '0')}`,
        nodeId: `perf-image-${index + 1}`,
        alt: `性能图片 ${index + 1}`,
        caption: `第 ${index + 1} 张`,
        displayWidthPx: 720,
        aspectRatio: 1,
        alignment: 'center',
        decorative: false,
      },
    })),
  }
}

test('measures 200,000-character detail, 20-image detail and mobile 200,000-character input', async ({
  page,
}, testInfo) => {
  test.setTimeout(120_000)
  const outputDir = testInfo.outputPath('performance')
  await mkdir(outputDir, { recursive: true })
  const metrics: Record<string, number> = {}
  let currentDocument: unknown = {
    type: 'doc',
    content: [
      {
        type: 'paragraph',
        content: [{ type: 'text', text: '性能正文'.repeat(50_000) }],
      },
    ],
  }

  await page.route(`**/kb/api/v1/knowledge-items/${ITEM_ID}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(knowledgeItem(currentDocument)),
    })
  })
  await routePng(page)

  let started = performance.now()
  await page.goto(`/kb/library/${ITEM_ID}`)
  await page.getByRole('heading', { level: 1 }).waitFor()
  await expect(page.locator('.rich-text-view')).toContainText('性能正文')
  metrics.detail200kMs = Math.round(performance.now() - started)
  expect(metrics.detail200kMs).toBeLessThan(budget.detail200kMs)

  currentDocument = imagesDocument(20)
  started = performance.now()
  await page.goto(`/kb/library/${ITEM_ID}`)
  await page.getByRole('heading', { level: 1 }).waitFor()
  await expect.poll(() => page.locator('.document-image img').count()).toBe(20)
  metrics.detail20ImagesMs = Math.round(performance.now() - started)
  expect(metrics.detail20ImagesMs).toBeLessThan(budget.detail20ImagesMs)

  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/kb/capture')
  const editor = page.locator('.ProseMirror')
  await editor.waitFor()
  const text = '输入性能'.repeat(50_000)
  started = performance.now()
  await editor.fill(text)
  await expect(editor).toHaveText(text)
  metrics.mobileInput200kMs = Math.round(performance.now() - started)
  expect(metrics.mobileInput200kMs).toBeLessThan(budget.mobileInput200kMs)

  const layout = await page.evaluate(() => ({
    viewport: window.innerWidth,
    documentWidth: document.documentElement.scrollWidth,
  }))
  expect(layout.documentWidth).toBeLessThanOrEqual(layout.viewport + 1)

  await writeFile(
    `${outputDir}/metrics.json`,
    `${JSON.stringify({ generatedAt: new Date().toISOString(), budget, metrics }, null, 2)}\n`,
    'utf8',
  )
  await testInfo.attach('richtext-performance.json', {
    body: Buffer.from(JSON.stringify({ budget, metrics }, null, 2)),
    contentType: 'application/json',
  })
})
