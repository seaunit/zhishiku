import { mkdir } from 'node:fs/promises'
import { expect, test } from '../support/playwright'

const viewports = [
  { name: 'desktop-1440', width: 1440, height: 900 },
  { name: 'tablet-768', width: 768, height: 1024 },
  { name: 'phone-430', width: 430, height: 932 },
  { name: 'phone-390', width: 390, height: 844 },
  { name: 'phone-320', width: 320, height: 720 },
]

test('keeps document parsing consent usable at desktop, tablet, and phone widths', async ({
  page,
}, testInfo) => {
  const screenshotDir = testInfo.outputPath('taste')
  await mkdir(screenshotDir, { recursive: true })

  for (const viewport of viewports) {
    await page.setViewportSize({ width: viewport.width, height: viewport.height })
    await page.goto('/kb/capture')
    await page.getByRole('tab', { name: '文档', exact: true }).click()
    await page.getByLabel('选择文档或图片').setInputFiles({
      name: 'very-long-document-name-for-knowledge-import-2026.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('%PDF-1.7\nmock document'),
    })
    await expect(page.getByLabel('允许外部 AI 解析并整理此文件')).toBeVisible()
    await expect(page.getByRole('button', { name: '解析并整理' })).toBeDisabled()

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
      const mobileClearance = await page.evaluate(() => {
        const button = Array.from(document.querySelectorAll('button')).find((candidate) =>
          candidate.textContent?.includes('解析并整理'),
        )
        const navigation = document.querySelector('.rail')
        return {
          buttonBottom: button?.getBoundingClientRect().bottom || 0,
          navigationTop: navigation?.getBoundingClientRect().top || window.innerHeight,
        }
      })
      expect(mobileClearance.buttonBottom).toBeLessThanOrEqual(
        mobileClearance.navigationTop + 1,
      )
    }

    await page.screenshot({
      path: `${screenshotDir}/document-${viewport.name}.png`,
      fullPage: true,
    })
  }
})
