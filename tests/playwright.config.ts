import { fileURLToPath } from 'node:url'
import playwrightTest from '../frontend/kb-web/node_modules/@playwright/test/index.js'

const { defineConfig } = playwrightTest

const testsRoot = fileURLToPath(new URL('.', import.meta.url))
const frontendRoot = fileURLToPath(new URL('../frontend/kb-web/', import.meta.url))
const runtimeRoot = fileURLToPath(new URL('../.runtime/rich-text-tests/', import.meta.url))

export default defineConfig({
  testDir: testsRoot,
  testMatch: '**/*.spec.ts',
  fullyParallel: false,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  outputDir: `${runtimeRoot}results`,
  reporter: [
    ['list'],
    ['json', { outputFile: `${runtimeRoot}results.json` }],
  ],
  use: {
    baseURL: 'http://127.0.0.1:4173',
    channel: 'chrome',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    viewport: { width: 1280, height: 900 },
  },
  webServer: {
    command: 'pnpm dev',
    cwd: frontendRoot,
    url: 'http://127.0.0.1:4173/kb/login',
    reuseExistingServer: true,
    timeout: 60_000,
  },
})
