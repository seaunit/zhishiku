import { mkdir } from 'node:fs/promises'
import { expect, test } from '../support/playwright'
import type { Page } from '../support/playwright'

const viewports = [
  { name: 'desktop-1440', width: 1440, height: 900 },
  { name: 'tablet-768', width: 768, height: 1024 },
  { name: 'phone-430', width: 430, height: 932 },
  { name: 'phone-390', width: 390, height: 844 },
  { name: 'phone-320', width: 320, height: 720 },
]

function pasteScreenshot(page: Page) {
  return page.evaluate(() => {
    const bytes = Uint8Array.from(
      atob('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2nGQAAAAASUVORK5CYII='),
      (character) => character.charCodeAt(0),
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
}

test('keeps screenshot OCR consent usable at desktop, tablet, and phone widths', async ({
  page,
}, testInfo) => {
  const screenshotDir = testInfo.outputPath('taste')
  await mkdir(screenshotDir, { recursive: true })

  for (const viewport of viewports) {
    await page.setViewportSize({ width: viewport.width, height: viewport.height })
    await page.goto('/kb/capture')
    await page.getByRole('tab', { name: '截图', exact: true }).click()
    expect(
      await page.getByTestId('screenshot-paste-zone').locator('input[type=file]').count(),
    ).toBe(0)
    await pasteScreenshot(page)
    await expect(page.getByLabel('允许外部 AI 解析并整理这张截图')).toBeVisible()
    await expect(page.getByRole('button', { name: '识别并拆分' })).toBeDisabled()

    const layout = await page.evaluate(() => {
      const consent = document.querySelector('.webpage-consent')?.getBoundingClientRect()
      return {
        viewportWidth: window.innerWidth,
        documentWidth: document.documentElement.scrollWidth,
        consentLeft: consent?.left || 0,
        consentRight: consent?.right || 0,
      }
    })
    expect(layout.documentWidth).toBeLessThanOrEqual(layout.viewportWidth + 1)
    expect(layout.consentLeft).toBeGreaterThanOrEqual(0)
    expect(layout.consentRight).toBeLessThanOrEqual(layout.viewportWidth)

    if (viewport.width <= 430) {
      await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight))
      await page.waitForTimeout(50)
      const clearance = await page.evaluate(() => {
        const button = Array.from(document.querySelectorAll('button')).find((candidate) =>
          candidate.textContent?.includes('识别并拆分'),
        )
        const navigation = document.querySelector('.rail')
        return {
          buttonBottom: button?.getBoundingClientRect().bottom || 0,
          navigationTop: navigation?.getBoundingClientRect().top || window.innerHeight,
        }
      })
      expect(clearance.buttonBottom).toBeLessThanOrEqual(clearance.navigationTop + 1)
      await page.evaluate(() => window.scrollTo(0, 0))
    }

    await page.screenshot({
      path: `${screenshotDir}/screenshot-${viewport.name}.png`,
      fullPage: true,
    })
  }
})
