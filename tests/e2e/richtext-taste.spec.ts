import { readFileSync } from 'node:fs'
import { mkdir } from 'node:fs/promises'
import { expect, test } from '../support/playwright'
import {
  ITEM_ID,
  fixtureJson,
  knowledgeItem,
  routePng,
} from '../support/rich-text'

const viewports = [
  { name: 'desktop-1440', width: 1440, height: 900 },
  { name: 'tablet-768', width: 768, height: 1024 },
  { name: 'phone-430', width: 430, height: 932 },
  { name: 'phone-390', width: 390, height: 844 },
  { name: 'phone-360', width: 360, height: 800 },
  { name: 'phone-320', width: 320, height: 720 },
  { name: 'phone-landscape', width: 740, height: 360 },
]

test('checks capture and detail at all required viewports with Taste-safe evidence', async ({
  page,
}, testInfo) => {
  const screenshotDir = testInfo.outputPath('taste')
  await mkdir(screenshotDir, { recursive: true })

  await page.route(`**/kb/api/v1/knowledge-items/${ITEM_ID}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(knowledgeItem(fixtureJson('valid/full-document.json'))),
    })
  })
  await routePng(page)

  for (const viewport of viewports) {
    await page.setViewportSize({ width: viewport.width, height: viewport.height })
    await page.goto('/kb/capture')
    await page.locator('.ProseMirror').waitFor()

    const captureLayout = await page.evaluate(() => {
      const toolbar = document.querySelector('.editor-toolbar')?.getBoundingClientRect()
      const editor = document.querySelector('.ProseMirror')?.getBoundingClientRect()
      return {
        viewport: window.innerWidth,
        documentWidth: document.documentElement.scrollWidth,
        toolbarBottom: toolbar?.bottom || 0,
        editorTop: editor?.top || 0,
      }
    })
    expect(captureLayout.documentWidth).toBeLessThanOrEqual(captureLayout.viewport + 1)
    expect(captureLayout.toolbarBottom).toBeLessThanOrEqual(captureLayout.editorTop + 1)
    await page.screenshot({
      path: `${screenshotDir}/capture-${viewport.name}.png`,
      fullPage: true,
    })

    await page.goto(`/kb/library/${ITEM_ID}`)
    await page.getByRole('heading', { level: 1 }).waitFor()
    await expect(page.locator('.document-image img')).toBeVisible()
    const detailLayout = await page.evaluate(() => ({
      viewport: window.innerWidth,
      documentWidth: document.documentElement.scrollWidth,
      bodyWidth: document.body.scrollWidth,
    }))
    expect(detailLayout.documentWidth).toBeLessThanOrEqual(detailLayout.viewport + 1)
    expect(detailLayout.bodyWidth).toBeLessThanOrEqual(detailLayout.viewport + 1)
    await page.screenshot({
      path: `${screenshotDir}/detail-${viewport.name}.png`,
      fullPage: true,
    })
  }

  const styles = readFileSync(
    new URL('../../frontend/kb-web/src/styles.css', import.meta.url),
    'utf8',
  )
  expect(styles).toContain('env(safe-area-inset-bottom)')

  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/kb/capture')
  await page.addStyleTag({ content: 'html { font-size: 200% !important; }' })
  const zoomed = await page.evaluate(() => ({
    viewport: window.innerWidth,
    documentWidth: document.documentElement.scrollWidth,
  }))
  expect(zoomed.documentWidth).toBeLessThanOrEqual(zoomed.viewport + 1)
  await page.screenshot({
    path: `${screenshotDir}/capture-390-font-200.png`,
    fullPage: true,
  })
})
