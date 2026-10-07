import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite config: dev server proxies /api to Spring Boot backend.
// 5173 只是首选端口：被占用时 Vite 会自动顺延（5174、5175…），并自动打开正确的地址。
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
