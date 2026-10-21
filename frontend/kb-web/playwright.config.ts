import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  use: {
    baseURL: 'http://127.0.0.1:4173',
    channel: 'chrome',
    trace: 'retain-on-failure',
    viewport: { width: 1280, height: 900 },
  },
  webServer: {
    command: 'pnpm dev',
    url: 'http://127.0.0.1:4173/kb/login',
    reuseExistingServer: true,
    timeout: 60_000,
  },
})

