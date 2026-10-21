import { expect, test } from '../support/playwright'
import {
  fixtureJson,
  fulfillCaptureSave,
  isLocalUrl,
  pasteHtml,
} from '../support/rich-text'

type PasteVector = {
  id: string
  html: string
  plain: string
  mustNotContain: string[]
}

const vectors = fixtureJson<PasteVector[]>('security/paste-vectors.json')
const sanitizableVectors = vectors.filter((vector) => vector.id !== 'credential-url')

test('sanitizes pasted HTML, blocks dangerous links and makes no external request', async ({
  page,
}) => {
  const externalRequests: string[] = []
  page.on('request', (request) => {
    if (!isLocalUrl(request.url())) {
      externalRequests.push(request.url())
    }
  })

  await page.goto('/kb/capture')
  await page.locator('.ProseMirror').waitFor()

  for (const vector of sanitizableVectors) {
    await pasteHtml(page, vector.html, vector.plain)
    await page.waitForTimeout(120)

    const state = await page.evaluate(() => {
      const editor = document.querySelector('.ProseMirror')
      const win = window as Window & {
        __pasteXss?: unknown
        __imgXss?: unknown
        __linkXss?: unknown
      }
      return {
        html: editor?.innerHTML || '',
        text: editor?.textContent || '',
        pasteXss: win.__pasteXss,
        imgXss: win.__imgXss,
        linkXss: win.__linkXss,
        dangerousDom: document.querySelectorAll(
          '.ProseMirror script, .ProseMirror iframe, .ProseMirror form, .ProseMirror img',
        ).length,
      }
    })

    for (const forbidden of vector.mustNotContain) {
      expect(state.html.toLowerCase()).not.toContain(forbidden.toLowerCase())
    }
    expect(state.text.length).toBeGreaterThan(0)
    expect(state.dangerousDom).toBe(0)
    expect({
      pasteXss: state.pasteXss,
      imgXss: state.imgXss,
      linkXss: state.linkXss,
    }).toEqual({
      pasteXss: undefined,
      imgXss: undefined,
      linkXss: undefined,
    })

    await page.locator('.ProseMirror').evaluate((editor) => {
      editor.innerHTML = '<p><br></p>'
      editor.dispatchEvent(
        new InputEvent('input', { bubbles: true, inputType: 'deleteContentBackward' }),
      )
    })
  }

  expect(externalRequests).toEqual([])
})

test('rejects a pasted link containing embedded credentials', async ({ page }) => {
  const credentialVector = vectors.find((vector) => vector.id === 'credential-url')
  expect(credentialVector).toBeTruthy()

  await page.goto('/kb/capture')
  await pasteHtml(page, credentialVector!.html, credentialVector!.plain)
  await page.waitForTimeout(120)
  const editorHtml = await page.locator('.ProseMirror').innerHTML()
  expect(editorHtml).not.toContain('user:password@')
})

test('posts only a structured document with no raw HTML, credentials or remote image URLs', async ({
  page,
}) => {
  let posted: Record<string, unknown> | undefined
  const externalRequests: string[] = []
  page.on('request', (request) => {
    if (!isLocalUrl(request.url())) {
      externalRequests.push(request.url())
    }
  })
  await fulfillCaptureSave(page, (body) => {
    posted = body as Record<string, unknown>
  })

  await page.goto('/kb/capture')
  await page.getByRole('textbox', { name: '标题', exact: true }).fill('粘贴安全验收')
  await pasteHtml(
    page,
    '<h2 onclick="window.__pasteXss=1">粘贴标题</h2><p><a href="https://example.com/private">公开链接</a></p><img src="https://example.invalid/remote.png" onerror="window.__imgXss=1"><iframe src="https://example.invalid/frame"></iframe>',
    '粘贴标题 公开链接',
  )
  await page.waitForTimeout(150)
  await page.getByRole('button', { name: '保存知识' }).click()

  await expect.poll(() => posted).toBeTruthy()
  const serialized = JSON.stringify(posted)
  expect(serialized).not.toContain('<')
  expect(serialized).not.toContain('onerror')
  expect(serialized).not.toContain('iframe')
  expect(externalRequests).toEqual([])
})

test('does not persist a pasted credential URL in the saved document', async ({ page }) => {
  let posted: Record<string, unknown> | undefined
  await fulfillCaptureSave(page, (body) => {
    posted = body as Record<string, unknown>
  })

  await page.goto('/kb/capture')
  await page.getByRole('textbox', { name: '标题', exact: true }).fill('凭据链接持久化验收')
  await pasteHtml(
    page,
    '<p><a href="https://user:password@example.com/private">凭据链接</a></p>',
    '凭据链接',
  )
  await page.waitForTimeout(120)
  await page.getByRole('button', { name: '保存知识' }).click()
  await expect.poll(() => posted).toBeTruthy()
  expect(JSON.stringify(posted)).not.toContain('user:password@')
})
