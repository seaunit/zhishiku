import { mkdir } from 'node:fs/promises'
import { expect, test } from '../support/playwright'

const viewports = [
  { name: 'desktop-1440', width: 1440, height: 900 },
  { name: 'tablet-768', width: 768, height: 1024 },
  { name: 'phone-430', width: 430, height: 932 },
  { name: 'phone-390', width: 390, height: 844 },
  { name: 'phone-320', width: 320, height: 720 },
]

test('keeps AI text capture and five modes usable across viewports', async ({
  page,
}, testInfo) => {
  const screenshotDir = testInfo.outputPath('taste')
  await mkdir(screenshotDir, { recursive: true })

  for (const viewport of viewports) {
    await page.setViewportSize({ width: viewport.width, height: viewport.height })
    await page.goto('/kb/capture')

    await expect(page.getByRole('tab')).toHaveCount(5)
    await page.getByRole('tab', { name: 'AI 文字', exact: true }).click()
    await page.getByLabel('待整理文字').fill(
      '这是一段需要由 AI 拆分、摘要、分类并生成标签的知识文字。'.repeat(3),
    )
    await expect(page.getByLabel('允许外部 AI 整理这段文字')).toBeVisible()
    await expect(page.getByRole('button', { name: 'AI 整理文字' })).toBeDisabled()

    const layout = await page.evaluate(() => {
      const tabs = document.querySelector('.capture-tabs')?.getBoundingClientRect()
      const consent = document.querySelector('.webpage-consent')?.getBoundingClientRect()
      return {
        viewportWidth: window.innerWidth,
        documentWidth: document.documentElement.scrollWidth,
        tabsLeft: tabs?.left || 0,
        tabsRight: tabs?.right || 0,
        consentLeft: consent?.left || 0,
        consentRight: consent?.right || 0,
      }
    })
    expect(layout.documentWidth).toBeLessThanOrEqual(layout.viewportWidth + 1)
    expect(layout.tabsLeft).toBeGreaterThanOrEqual(0)
    expect(layout.tabsRight).toBeLessThanOrEqual(layout.viewportWidth)
    expect(layout.consentLeft).toBeGreaterThanOrEqual(0)
    expect(layout.consentRight).toBeLessThanOrEqual(layout.viewportWidth)

    await page.screenshot({
      path: `${screenshotDir}/ai-text-${viewport.name}.png`,
      fullPage: true,
    })
  }
})
