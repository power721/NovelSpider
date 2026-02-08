import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

const API = "http://127.0.0.1:3000";

// https://vite.dev/config/
export default defineConfig({
  build: {
    outDir: "../src/main/resources/static",
    emptyOutDir: true,
  },
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    proxy: {
      "/api": API,
    }
  }
})
