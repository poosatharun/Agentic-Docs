import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig({
  plugins: [react(), tailwindcss()],

  // Must match the Spring Boot static resource path
  base: '/agentic-docs/',

  build: {
    // Output directly into the starter's static folder so mvn package bundles it
    outDir: path.resolve(
      __dirname,
      '../agentic-docs-spring-boot-starter/src/main/resources/static/agentic-docs'
    ),
    emptyOutDir: true,
  },

  server: {
    port: 5173,
    proxy: {
      // Forward API calls to the running Spring Boot app during development
      '/agentic-docs/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
