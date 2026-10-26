import { defineConfig } from '@playwright/test'

const frontendPort = Number(process.env.KB_FRONTEND_PORT || 4173)
const baseURL = `http://127.0.0.1:${frontendPort}`

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  use: {
    baseURL,
    channel: 'chrome',
    trace: 'retain-on-failure',
    viewport: { width: 1280, height: 900 },
  },
  webServer: {
    command: 'pnpm dev',
    url: `${baseURL}/kb/login`,
    reuseExistingServer: true,
    timeout: 60_000,
  },
})

