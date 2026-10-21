import { expect, test } from '../support/playwright'
import { fixtureJson } from '../support/rich-text'

type CorpusCase = {
  id: string
  category: 'html' | 'protocol' | 'image'
  payload: string
}

type ManifestCase = {
  id: string
  file: string
  expected: { valid: boolean }
  layers: string[]
}

function extractAttribute(payload: string, name: string) {
  return payload.match(new RegExp(`${name}="([^"]*)"`, 'i'))?.[1] || ''
}

function toDocument(testCase: CorpusCase) {
  if (testCase.category === 'protocol') {
    return {
      type: 'doc',
      content: [
        {
          type: 'paragraph',
          content: [
            {
              type: 'text',
              text: 'link',
              marks: [{ type: 'link', attrs: { href: extractAttribute(testCase.payload, 'href') } }],
            },
          ],
        },
      ],
    }
  }

  if (testCase.category === 'image') {
    return {
      type: 'doc',
      content: [
        {
          type: 'image',
          attrs: {
            assetId: extractAttribute(testCase.payload, 'src'),
            nodeId: testCase.id,
            alt: testCase.id,
            caption: '',
            displayWidthPx: 640,
            aspectRatio: 1,
            alignment: 'center',
            decorative: false,
            src: extractAttribute(testCase.payload, 'src'),
            onerror: extractAttribute(testCase.payload, 'onerror'),
          },
        },
      ],
    }
  }

  const tag = testCase.payload.match(/^<([a-z][a-z0-9-]*)/i)?.[1]?.toLowerCase() || 'unknown'
  const type =
    tag === 'a'
      ? 'paragraph'
      : tag === 'img'
        ? 'image'
        : tag === 'math'
          ? 'math'
          : tag
  if (type === 'paragraph') {
    return {
      type: 'doc',
      content: [
        {
          type: 'paragraph',
          content: [
            {
              type: 'text',
              text: 'link',
              marks: [
                {
                  type: 'link',
                  attrs: { href: extractAttribute(testCase.payload, 'href') },
                },
              ],
            },
          ],
        },
      ],
    }
  }
  return {
    type: 'doc',
    content: [
      {
        type,
        attrs: {
          src: extractAttribute(testCase.payload, 'src'),
          href: extractAttribute(testCase.payload, 'href'),
          onerror: extractAttribute(testCase.payload, 'onerror'),
          onload: extractAttribute(testCase.payload, 'onload'),
        },
        content: [{ type: 'text', text: testCase.payload }],
      },
    ],
  }
}

test('rejects all 100 XSS, protocol and image-source corpus cases', async ({ page }) => {
  const corpus = fixtureJson<{ version: number; cases: CorpusCase[] }>(
    'security/xss-corpus.json',
  )
  expect(corpus.cases).toHaveLength(100)

  await page.goto('/kb/login')
  const results = await page.evaluate(async (documents) => {
    const validators = await import('/kb/src/lib/rich-text.ts')
    return documents.map((document) => validators.validateRichTextDocument(document))
  }, corpus.cases.map(toDocument))

  expect(results).toHaveLength(100)
  for (let index = 0; index < results.length; index += 1) {
    expect(results[index], corpus.cases[index]?.id).toMatchObject({ ok: false })
  }
})

test('uses the shared manifest to validate accepted and rejected Vue fixtures', async ({ page }) => {
  const manifest = fixtureJson<{ cases: ManifestCase[] }>('manifest.json')
  const cases = manifest.cases.filter(
    (item) => item.layers.includes('vue') && item.file.endsWith('.json'),
  )

  await page.goto('/kb/login')
  for (const testCase of cases) {
    const document = fixtureJson(testCase.file)
    const result = await page.evaluate(async (value) => {
      const validators = await import('/kb/src/lib/rich-text.ts')
      return validators.validateRichTextDocument(value)
    }, document)
    expect(result.ok, testCase.id).toBe(testCase.expected.valid)
  }
})
