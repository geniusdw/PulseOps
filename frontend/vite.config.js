import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the React dev server proxies API calls to the Spring Boot
// backend so the browser only ever talks to one origin (no CORS in practice).
// In the Docker image nginx does the same job (see nginx.conf).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/actuator': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
});
