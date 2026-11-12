import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig(({ mode }) => {
  // Load VITE_* variables from root project .env first, then override with frontend/.env
  const rootEnv = loadEnv(mode, path.resolve(__dirname, '..'), 'VITE_');
  const localEnv = loadEnv(mode, __dirname, 'VITE_');
  const mergedEnv = { ...rootEnv, ...localEnv };

  const defineEnv = {};
  for (const [key, val] of Object.entries(mergedEnv)) {
    defineEnv[`import.meta.env.${key}`] = JSON.stringify(val);
  }

  return {
    plugins: [react()],
    base: './',
    define: defineEnv,
    server: {
      port: 5173,
      host: true
    },
    build: {
      outDir: 'dist',
      emptyOutDir: true
    }
  };
});
