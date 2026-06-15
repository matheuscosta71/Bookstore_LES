/**
 * 08-admin-confirma-recebimento-devolucao.spec.ts
 * Cenário: O administrador confirma o recebimento do produto devolvido
 *
 * Cobre:
 * - Admin acessa /admin/exchanges filtrando por TROCA_AUTORIZADA
 * - Clica em "Receber + estoque" ou "Receber sem estoque"
 * - Sistema gera cupom de troca (verificado no próximo cenário)
 */

import { test, expect } from '@playwright/test';
import {
  createCustomerViaApi,
  createFullOrder,
  requestExchangeViaApi,
} from '../helpers/setup';
import {
  approvePaymentViaApi,
  dispatchOrderViaApi,
  deliverOrderViaApi,
  authorizeExchangeViaApi,
  adminLogin,
  listExchangeRequestsViaApi,
} from '../helpers/admin';

const BACKEND_URL = process.env.PW_BACKEND_URL ?? 'http://localhost:8080';
const ADMIN_KEY = process.env.VITE_ADMIN_KEY ?? '8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12';

async function prepararTrocaAutorizada() {
  const customer = await createCustomerViaApi();
  const order = await createFullOrder(customer.id);
  await approvePaymentViaApi(order.id);
  await dispatchOrderViaApi(order.id);
  await deliverOrderViaApi(order.id);
  const exchange = await requestExchangeViaApi(customer.id, order.id, order.items[0].id);
  await authorizeExchangeViaApi(exchange.id);
  return { customer, order, exchange };
}

test.describe('08 — Admin confirma recebimento do produto devolvido', () => {
  test('admin confirma recebimento via UI — "Receber + estoque"', async ({ page }) => {
    const { exchange } = await prepararTrocaAutorizada();

    await adminLogin(page);
    await page.goto('/admin/exchanges');
    await page.waitForLoadState('networkidle');

    // Seleciona filtro TROCA_AUTORIZADA
    const statusSelect = page.locator('select').first();
    if (await statusSelect.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await statusSelect.selectOption('TROCA_AUTORIZADA');
      await page.waitForTimeout(100);
    }

    // Clica em "Receber + estoque"
    const receberBtn = page.getByRole('button', { name: /Receber \+ estoque/i }).first();
    await receberBtn.waitFor({ state: 'visible', timeout: 15_000 });
    await receberBtn.click();

    // Verifica que cupom foi gerado (toast/mensagem)
    await expect(
      page.getByText(/Cupom de troca gerado|Recebimento registrado/i)
    ).toBeVisible({ timeout: 10_000 });
  });

  test('admin confirma recebimento via UI — "Receber sem estoque"', async ({ page }) => {
    const { exchange } = await prepararTrocaAutorizada();

    await adminLogin(page);
    await page.goto('/admin/exchanges');
    await page.waitForLoadState('networkidle');

    const statusSelect = page.locator('select').first();
    if (await statusSelect.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await statusSelect.selectOption('TROCA_AUTORIZADA');
      await page.waitForTimeout(100);
    }

    const receberSemBtn = page.getByRole('button', { name: /Receber sem estoque/i }).first();
    await receberSemBtn.waitFor({ state: 'visible', timeout: 15_000 });
    await receberSemBtn.click();

    await expect(
      page.getByText(/Cupom de troca gerado|Recebimento registrado/i)
    ).toBeVisible({ timeout: 10_000 });
  });

  test('admin confirma recebimento via API (returnToStock=true)', async () => {
    const { exchange } = await prepararTrocaAutorizada();

    const res = await fetch(
      `${BACKEND_URL}/admin/exchange-requests/${exchange.id}/receive`,
      {
        method: 'PATCH',
        headers: { 'X-Admin-Key': ADMIN_KEY, 'Content-Type': 'application/json' },
        body: JSON.stringify({ returnToStock: true }),
      }
    );
    expect(res.ok).toBe(true);
    const result = (await res.json()) as { generatedCouponCode?: string | null; exchangeStatus: string };
    expect(result.generatedCouponCode).toBeTruthy();
  });

  test('admin confirma recebimento via API (returnToStock=false)', async () => {
    const { exchange } = await prepararTrocaAutorizada();

    const res = await fetch(
      `${BACKEND_URL}/admin/exchange-requests/${exchange.id}/receive`,
      {
        method: 'PATCH',
        headers: { 'X-Admin-Key': ADMIN_KEY, 'Content-Type': 'application/json' },
        body: JSON.stringify({ returnToStock: false }),
      }
    );
    expect(res.ok).toBe(true);
    const result = (await res.json()) as { generatedCouponCode?: string | null };
    // Cupom ainda deve ser gerado mesmo sem retorno ao estoque
    expect(result.generatedCouponCode).toBeTruthy();
  });
});
