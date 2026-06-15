import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig, devices } from '@playwright/test';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const defaultAdminKey =
  process.env.VITE_ADMIN_KEY ?? '8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12';

export default defineConfig({
  //globalSetup: path.join(__dirname, 'e2e', 'global-setup.ts'),
  testDir: './e2e',
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? 'dot' : [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    launchOptions: {
      // Adiciona atraso entre ações (em milissegundos) para podermos ver o fluxo em detalhes
      slowMo: process.env.PW_SLOWMO ? parseInt(process.env.PW_SLOWMO, 10) : 1000,
    },
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'npm run dev -- --host 0.0.0.0 --port 5173',
    url: 'http://localhost:5173',
    reuseExistingServer: process.env.PW_REUSE_SERVER === '1',
    timeout: 120_000,
    env: {
      ...process.env,
      VITE_ADMIN_KEY: defaultAdminKey,
    },
  },
});

