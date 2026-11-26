import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'
import { defineConfig } from 'vitest/config'

const backendKeepingTheHost = { target: 'http://localhost:8080', changeOrigin: false }

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      workbox: {
        navigateFallbackDenylist: [/^\/api/, /^\/oauth2/, /^\/login/],
      },
      manifest: {
        name: 'Lyra',
        short_name: 'Lyra',
        description: 'Diet tracker built around your diet plan',
        lang: 'pt-BR',
        start_url: '/',
        display: 'standalone',
        background_color: '#ffffff',
        theme_color: '#111111',
        icons: [{ src: '/lyra.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' }],
      },
    }),
  ],
  resolve: {
    alias: { '@': path.resolve(import.meta.dirname, './src') },
  },
  server: {
    proxy: {
      '/api': backendKeepingTheHost,
      '/oauth2': backendKeepingTheHost,
      '/login': backendKeepingTheHost,
    },
  },
  test: {
    environment: 'jsdom',
    env: { TZ: 'America/Sao_Paulo' },
    setupFiles: ['./src/setupTests.ts'],
    css: true,
  },
})
