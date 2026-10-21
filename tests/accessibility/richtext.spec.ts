import { expect, test } from '../support/playwright'
import {
  ITEM_ID,
  fixtureJson,
  fixtureText,
  knowledgeItem,
  routePng,
} from '../support/rich-text'

test('exposes toolbar names, keyboard focus, pressed state, upload status and 44px phone controls', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/kb/capture')
  await page.locator('.ProseMirror').waitFor()

  const toolbar = page.getByRole('toolbar', { name: '正文格式工具栏' })
  await expect(toolbar).toBeVisible()
  const buttons = toolbar.locator('button:visible')
  expect(await buttons.count()).toBeGreaterThanOrEqual(5)
  for (let index = 0; index < (await buttons.count()); index += 1) {
    const button = buttons.nth(index)
    await expect(button).toHaveAccessibleName(/\S+/)
    const box = await button.boundingBox()
    expect(box?.width).toBeGreaterThanOrEqual(44)
    expect(box?.height).toBeGreaterThanOrEqual(44)
  }

  await page.getByRole('button', { name: '更多格式' }).click()
  const moreMenu = page.getByRole('menu', { name: '更多格式' })
  await expect(moreMenu).toBeVisible()
  const moreItems = moreMenu.locator('button')
  expect(await moreItems.count()).toBeGreaterThanOrEqual(18)
  for (let index = 0; index < (await moreItems.count()); index += 1) {
    const item = moreItems.nth(index)
    await expect(item).toHaveAccessibleName(/\S+/)
  }
  const moreItemSizes = await moreItems.evaluateAll((items) =>
    items.map((item) => {
      const box = item.getBoundingClientRect()
      return { width: box.width, height: box.height }
    }),
  )
  expect(moreItemSizes.every((box) => box.width >= 44 && box.height >= 44)).toBe(true)
  await page.getByRole('button', { name: '更多格式' }).click()

  const editor = page.locator('.ProseMirror')
  await editor.click()
  await page.keyboard.insertText('键盘格式测试')
  await page.keyboard.press('Control+A')
  await page.keyboard.press('Control+B')
  await expect(toolbar.getByRole('button', { name: '粗体' })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
  await expect(editor.locator('strong')).toContainText('键盘格式测试')

  const bold = toolbar.getByRole('button', { name: '粗体' })
  await page.keyboard.press('Tab')
  await page.keyboard.press('Shift+Tab')
  await bold.focus()
  const focusStyle = await bold.evaluate((element) => {
    const style = getComputedStyle(element)
    return {
      outlineStyle: style.outlineStyle,
      outlineWidth: style.outlineWidth,
    }
  })
  expect(focusStyle.outlineStyle).not.toBe('none')
  expect(Number.parseFloat(focusStyle.outlineWidth)).toBeGreaterThan(0)

  const svg = fixtureText('images/scripted.svg')
  await page.locator('input[type=file]').setInputFiles({
    name: 'scripted.svg',
    mimeType: 'image/svg+xml',
    buffer: Buffer.from(svg),
  })
  await expect(page.locator('.editor-upload-message[aria-live="polite"]')).toHaveText(
    '请选择 JPG、PNG、WebP、HEIC 或 HEIF 图片。',
  )
})

async function openRichDocument(page: Parameters<typeof routePng>[0]) {
  await page.route(`**/kb/api/v1/knowledge-items/${ITEM_ID}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(knowledgeItem(fixtureJson('valid/full-document.json'))),
    })
  })
  await routePng(page)
  await page.goto(`/kb/library/${ITEM_ID}`)
  await page.getByRole('heading', { level: 1 }).waitFor()
}

test('renders table semantics and image caption at phone width', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await openRichDocument(page)

  await expect(page.getByRole('table')).toBeVisible()
  await expect(page.getByRole('columnheader', { name: '格式' })).toBeVisible()
  await expect(page.getByRole('cell', { name: 'JSONB' })).toBeVisible()
  await expect(page.getByRole('img', { name: '数据库迁移示意图' })).toBeVisible()
  await expect(page.getByText('图片由系统内扫描后的附件提供服务')).toBeVisible()
})

test('keeps the page within the viewport at 200% font', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await openRichDocument(page)

  await page.addStyleTag({ content: 'html { font-size: 200% !important; }' })
  const layout = await page.evaluate(() => ({
    viewport: window.innerWidth,
    documentWidth: document.documentElement.scrollWidth,
    bodyWidth: document.body.scrollWidth,
  }))
  expect(layout.documentWidth).toBeLessThanOrEqual(layout.viewport + 1)
  expect(layout.bodyWidth).toBeLessThanOrEqual(layout.viewport + 1)
})
