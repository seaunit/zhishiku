import { readFile, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'

const root = new URL('./', import.meta.url)
const maxBlocks = 5000
const maxText = 200000
const maxImages = 20

async function readJson(path) {
  return JSON.parse(await readFile(new URL(path, root), 'utf8'))
}

async function writeJson(path, value) {
  await writeFile(new URL(path, root), `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

function paragraph(index) {
  return {
    type: 'paragraph',
    content: [{ type: 'text', text: `block-${index}` }],
  }
}

function image(index) {
  return {
    type: 'image',
    attrs: {
      assetId: `123e4567-e89b-42d3-a456-${String(index + 1).padStart(12, '0')}`,
      nodeId: `image-${index + 1}`,
      alt: `image ${index + 1}`,
      caption: '',
      displayWidthPx: 720,
      aspectRatio: 1.5,
      alignment: 'center',
      decorative: false,
    },
  }
}

function nestedBullet(levels) {
  let node = {
    type: 'paragraph',
    content: [{ type: 'text', text: '嵌套过深' }],
  }
  for (let level = 0; level < levels; level += 1) {
    node = {
      type: 'bulletList',
      content: [
        {
          type: 'listItem',
          content: [node],
        },
      ],
    }
  }
  return node
}

await writeJson('limits/over-depth.json', {
  type: 'doc',
  content: [nestedBullet(21)],
})
await writeJson('limits/over-block-count.json', {
  type: 'doc',
  content: Array.from({ length: maxBlocks + 1 }, (_, index) => paragraph(index)),
})
await writeJson('limits/over-text-length.json', {
  type: 'doc',
  content: [
    {
      type: 'codeBlock',
      content: [{ type: 'text', text: 'a'.repeat(maxText + 1) }],
    },
  ],
})
await writeJson('limits/over-image-count.json', {
  type: 'doc',
  content: Array.from({ length: maxImages + 1 }, (_, index) => image(index)),
})

const manifest = await readJson('manifest.json')
for (const testCase of manifest.cases) {
  if (testCase.file.includes('migration') || testCase.file.includes('security')) {
    continue
  }
  const document = await readJson(testCase.file)
  if (document.type !== 'doc' || !Array.isArray(document.content)) {
    throw new Error(`${testCase.id}: expected a doc fixture`)
  }
}

const corpus = await readJson('security/xss-corpus.json')
if (corpus.cases.length !== 100) {
  throw new Error(`security corpus must contain 100 cases, got ${corpus.cases.length}`)
}
if (new Set(corpus.cases.map((item) => item.id)).size !== 100) {
  throw new Error('security corpus ids must be unique')
}

const pastes = await readJson('security/paste-vectors.json')
for (const paste of pastes) {
  if (!paste.id || !paste.html || !Array.isArray(paste.mustNotContain)) {
    throw new Error('paste vector is incomplete')
  }
}

console.log(
  JSON.stringify(
    {
      manifestCases: manifest.cases.length,
      securityCorpusCases: corpus.cases.length,
      pasteVectors: pastes.length,
      generatedFiles: [
        'limits/over-depth.json',
        'limits/over-block-count.json',
        'limits/over-text-length.json',
        'limits/over-image-count.json',
      ],
    },
    null,
    2,
  ),
)
