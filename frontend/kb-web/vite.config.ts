import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

const frontendPort = Number(process.env.KB_FRONTEND_PORT || 4173)
const backendPort = Number(process.env.KB_BACKEND_PORT || 8082)

export default defineConfig({
  base: '/kb/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: frontendPort,
    strictPort: true,
    proxy: {
      '/kb/api': {
        target: `http://127.0.0.1:${backendPort}`,
        changeOrigin: false,
      },
    },
  },
  preview: {
    port: frontendPort,
    strictPort: true,
  },
})

