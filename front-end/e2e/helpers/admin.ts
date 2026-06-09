import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';
import dotenv from 'dotenv';
dotenv.config();

const BACKEND_URL = process.env.PW_BACKEND_URL ?? 'http://localhost:8080';
const ADMIN_KEY = process.env.VITE_ADMIN_KEY;
const ADMIN_PASSWORD = 'admin';

/** Faz login do administrador via UI */
export async function adminLogin(page: Page): Promise<void> {
  await page.goto('/login?tab=admin');
  const keyInput = page.locator('input[name="adminKey"], input[placeholder*="chave"], input[type="password"]').last();
  await keyInput.waitFor({ state: 'visible', timeout: 10_000 });
  await keyInput.fill(ADMIN_PASSWORD);
  await page.getByRole('button', { name: /Entrar/i }).click();
  await page.waitForURL(/\/admin/, { timeout: 15_000 });
}

/** Aprova o pagamento de um pedido via API (direto no backend) */
export async function approvePaymentViaApi(orderId: string): Promise<void> {
  const res = await fetch(`${BACKEND_URL}/admin/orders/${orderId}/approve-payment`, {
    method: 'PATCH',
    headers: { 'X-Admin-Key': ADMIN_KEY },
  });
  if (!res.ok) {
    throw new Error(`approvePayment falhou: ${res.status} ${await res.text()}`);
  }
}

/** Despacha (coloca EM_TRANSITO) um pedido via API */
export async function dispatchOrderViaApi(orderId: string): Promise<void> {
  const res = await fetch(`${BACKEND_URL}/admin/orders/${orderId}/dispatch`, {
    method: 'PATCH',
    headers: { 'X-Admin-Key': ADMIN_KEY },
  });
  if (!res.ok) {
    throw new Error(`dispatchOrder falhou: ${res.status} ${await res.text()}`);
  }
}

/** Confirma entrega de um pedido via API */
export async function deliverOrderViaApi(orderId: string): Promise<void> {
  const res = await fetch(`${BACKEND_URL}/admin/orders/${orderId}/deliver`, {
    method: 'PATCH',
    headers: { 'X-Admin-Key': ADMIN_KEY },
  });
  if (!res.ok) {
    throw new Error(`deliverOrder falhou: ${res.status} ${await res.text()}`);
  }
}

/** Autoriza troca via API */
export async function authorizeExchangeViaApi(exchangeRequestId: string): Promise<void> {
  const res = await fetch(`${BACKEND_URL}/admin/exchange-requests/${exchangeRequestId}/authorize`, {
    method: 'PATCH',
    headers: { 'X-Admin-Key': ADMIN_KEY },
  });
  if (!res.ok) {
    throw new Error(`authorizeExchange falhou: ${res.status} ${await res.text()}`);
  }
}

/** Registra recebimento do produto devolvido via API */
export async function receiveExchangeViaApi(
  exchangeRequestId: string,
  returnToStock = true,
): Promise<{ generatedCouponCode?: string | null }> {
  const res = await fetch(`${BACKEND_URL}/admin/exchange-requests/${exchangeRequestId}/receive`, {
    method: 'PATCH',
    headers: { 'X-Admin-Key': ADMIN_KEY, 'Content-Type': 'application/json' },
    body: JSON.stringify({ returnToStock }),
  });
  if (!res.ok) {
    throw new Error(`receiveExchange falhou: ${res.status} ${await res.text()}`);
  }
  return res.json() as Promise<{ generatedCouponCode?: string | null }>;
}

/** Lista pedidos via API (admin) */
export async function listOrdersViaApi(params?: Record<string, string>): Promise<any[]> {
  const qs = new URLSearchParams({ page: '0', size: '50', ...params }).toString();
  const res = await fetch(`${BACKEND_URL}/admin/orders?${qs}`, {
    headers: { 'X-Admin-Key': ADMIN_KEY },
  });
  if (!res.ok) throw new Error(`listOrders falhou: ${res.status}`);
  const data = (await res.json()) as { content: any[] };
  return data.content;
}

/** Lista solicitações de troca via API (admin) */
export async function listExchangeRequestsViaApi(status: string): Promise<any[]> {
  const res = await fetch(`${BACKEND_URL}/admin/exchange-requests?status=${status}`, {
    headers: { 'X-Admin-Key': ADMIN_KEY },
  });
  if (!res.ok) throw new Error(`listExchangeRequests falhou: ${res.status}`);
  return res.json() as Promise<any[]>;
}

/** Navega para Admin Orders e clica em Aprovar pagamento para o pedido mais recente */
export async function approveLatestOrderPaymentViaUI(page: Page): Promise<void> {
  await page.goto('/admin/orders');
  await page.waitForLoadState('networkidle');
  // Abre detalhes do primeiro pedido EM_PROCESSAMENTO
  const approveBtn = page.getByRole('button', { name: /Aprovar pagamento/i }).first();
  await approveBtn.waitFor({ state: 'visible', timeout: 15_000 });
  await approveBtn.click();
  await expect(page.getByText(/Pagamento aprovado|APROVADO/i)).toBeVisible({ timeout: 10_000 });
}

/** Navega para Admin Orders e clica em Despachar para o pedido mais recente */
export async function dispatchLatestOrderViaUI(page: Page): Promise<void> {
  await page.goto('/admin/orders');
  await page.waitForLoadState('networkidle');
  const dispatchBtn = page.getByRole('button', { name: /Despachar/i }).first();
  await dispatchBtn.waitFor({ state: 'visible', timeout: 15_000 });
  await dispatchBtn.click();
  await page.waitForTimeout(1_500);
}

/** Navega para Admin Orders e clica em Confirmar entrega */
export async function deliverLatestOrderViaUI(page: Page): Promise<void> {
  await page.goto('/admin/orders');
  await page.waitForLoadState('networkidle');
  const deliverBtn = page.getByRole('button', { name: /Entregue/i }).first();
  await deliverBtn.waitFor({ state: 'visible', timeout: 15_000 });
  await deliverBtn.click();
  await page.waitForTimeout(1_500);
}
