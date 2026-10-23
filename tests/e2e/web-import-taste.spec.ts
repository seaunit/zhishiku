import { mkdir } from 'node:fs/promises'
import { expect, test } from '../support/playwright'

const viewports = [
  { name: 'desktop-1440', width: 1440, height: 900 },
  { name: 'tablet-768', width: 768, height: 1024 },
  { name: 'phone-390', width: 390, height: 844 },
]

test('keeps the webpage AI consent flow usable at desktop, tablet, and phone widths', async ({
  page,
}, testInfo) => {
  const screenshotDir = testInfo.outputPath('taste')
  await mkdir(screenshotDir, { recursive: true })

  for (const viewport of viewports) {
    await page.setViewportSize({ width: viewport.width, height: viewport.height })
    await page.goto('/kb/capture')
    await page.getByRole('tab', { name: '网页链接' }).click()
    await expect(page.getByLabel('网页链接', { exact: true })).toBeVisible()
    await expect(page.getByLabel('允许外部 AI 处理本页文字')).toBeVisible()
    await expect(page.getByRole('button', { name: 'AI整理图文入库' })).toBeDisabled()

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

    await page.screenshot({
      path: `${screenshotDir}/webpage-${viewport.name}.png`,
      fullPage: true,
    })
  }
})
