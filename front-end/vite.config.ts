import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      /**
       * Google Books costuma servir arte “image not available” quando o img aponta
       * direto para books.google.com a partir de outro host (ex.: localhost). O proxy
       * com Referer alinhado costuma devolver a capa real só em dev.
       */
      '/gb-cover': {
        target: 'https://books.google.com',
        changeOrigin: true,
        secure: true,
        rewrite: (path) => path.replace(/^\/gb-cover/, ''),
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('Referer', 'https://books.google.com/');
          });
        },
      },
      '/ol-api': {
        target: 'https://openlibrary.org',
        changeOrigin: true,
        secure: true,
        rewrite: (path) => path.replace(/^\/ol-api/, ''),
      },
    },
  },
});
