import type { FullConfig } from '@playwright/test';

const DEFAULT_ADMIN_KEY = '8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12';

async function waitForOk(url: string, timeoutMs: number): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  let lastErr: unknown;
  while (Date.now() < deadline) {
    try {
      const r = await fetch(url);
      if (r.ok) {
        return;
      }
      lastErr = new Error(`HTTP ${r.status} for ${url}`);
    } catch (e) {
      lastErr = e;
    }
    await new Promise((resolve) => setTimeout(resolve, 400));
  }
  throw new Error(`Timeout aguardando ${url}: ${String(lastErr)}`);
}

export default async function globalSetup(_config: FullConfig): Promise<void> {
  const base = (process.env.PW_BACKEND_URL ?? 'http://localhost:8080').replace(/\/$/, '');
  await waitForOk(`${base}/health`, 90_000);

  const booksUrl = `${base}/books?page=0&size=1&sort=id,asc`;
  await waitForOk(booksUrl, 15_000);

  const adminKey = process.env.VITE_ADMIN_KEY ?? DEFAULT_ADMIN_KEY;
  const end = new Date();
  const start = new Date();
  start.setUTCDate(end.getUTCDate() - 30);
  const startDate = start.toISOString().slice(0, 10);
  const endDate = end.toISOString().slice(0, 10);
  const analyticsUrl = `${base}/analytics/sales-history?startDate=${startDate}&endDate=${endDate}`;
  const ar = await fetch(analyticsUrl, { headers: { 'X-Admin-Key': adminKey } });
  if (!ar.ok) {
    const hint =
      ar.status === 403
        ? ' Verifique se VITE_ADMIN_KEY / app.admin.key coincidem com o backend em execução.'
        : '';
    throw new Error(`Analytics preflight falhou (${ar.status}) para ${analyticsUrl}.${hint}`);
  }
  const summary = (await ar.json()) as { orderCount?: number; totalItemsSold?: number };
  if (!summary.orderCount || summary.orderCount < 1) {
    throw new Error(
      `Esperado orderCount >= 1 no intervalo ${startDate}..${endDate} (demo seed no backend?). Recebido: ${JSON.stringify(summary)}`,
    );
  }
}
