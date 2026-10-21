import { readFile, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'

const corpusPath = fileURLToPath(new URL('./security/xss-corpus.json', import.meta.url))
const payloads = [
  '<script>window.__richTextXss=1</script>',
  '<img src="https://example.invalid/pixel.png" onerror="window.__richTextXss=2">',
  '<iframe src="https://example.invalid/frame"></iframe>',
  '<form action="https://example.invalid/post"><input name="secret"></form>',
  '<a href="javascript:window.__richTextXss=3">link</a>',
  '<a href="data:text/html,<script>1</script>">data</a>',
  '<object data="https://example.invalid/object"></object>',
  '<embed src="https://example.invalid/embed">',
  '<svg onload="window.__richTextXss=4"></svg>',
  '<math><mi xlink:href="javascript:window.__richTextXss=5">x</mi></math>',
]

const protocols = ['javascript:', 'data:', 'blob:', 'cid:', 'file:']
const imageHosts = [
  'https://example.invalid/a.png',
  'http://127.0.0.1/private.png',
  'http://10.0.0.1/private.png',
  'http://[::1]/private.png',
  'https://user:pass@example.invalid/private.png',
]

const cases = []
for (let index = 0; index < 100; index += 1) {
  if (index < 40) {
    cases.push({
      id: `html-${String(index + 1).padStart(3, '0')}`,
      category: 'html',
      payload: payloads[index % payloads.length],
    })
  } else if (index < 60) {
    cases.push({
      id: `protocol-${String(index + 1).padStart(3, '0')}`,
      category: 'protocol',
      payload: `<a href="${protocols[index % protocols.length]}payload-${index}">link</a>`,
    })
  } else {
    const host = imageHosts[index % imageHosts.length]
    cases.push({
      id: `image-${String(index + 1).padStart(3, '0')}`,
      category: 'image',
      payload: `<img src="${host}" onerror="window.__richTextXss=${index}">`,
    })
  }
}

const output = `${JSON.stringify(
  {
    version: 1,
    invariant: 'payload 必须被拒绝、降级为纯文本或作为不可执行文本显示',
    cases,
  },
  null,
  2,
)}\n`

if (process.argv.includes('--check')) {
  const current = await readFile(corpusPath, 'utf8').catch(() => '')
  if (current !== output) {
    console.error('security corpus is stale; run generate-security-corpus.mjs')
    process.exitCode = 1
  }
} else {
  await writeFile(corpusPath, output, 'utf8')
  console.log(`wrote ${cases.length} cases to ${corpusPath}`)
}
