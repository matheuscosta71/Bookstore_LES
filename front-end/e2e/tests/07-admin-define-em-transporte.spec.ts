/**
 * 07-admin-define-em-transporte.spec.ts
 * Cenário: O administrador define que o produto está EM TRANSPORTE
 *
 * Cobre:
 * - Admin despacha pedido APROVADO via UI (botão "Despachar")
 * - Status muda para EM_TRANSITO
 */

import { test, expect } from '@playwright/test';
import { createCustomerViaApi, createFullOrder } from '../helpers/setup';
import { approvePaymentViaApi, adminLogin } from '../helpers/admin';

const BACKEND_URL = process.env.PW_BACKEND_URL ?? 'http://localhost:8080';
const ADMIN_KEY = process.env.VITE_ADMIN_KEY ?? '8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12';

test.describe('07 — Admin define produto EM TRANSPORTE', () => {
  test('admin despacha pedido APROVADO via UI — status vira EM_TRANSITU', async ({ page }) => {
    const customer = await createCustomerViaApi();
    const order = await createFullOrder(customer.id);
    await approvePaymentViaApi(order.id);

    await adminLogin(page);
    await page.goto('/admin/orders');
    await page.waitForLoadState('networkidle');

    // Filtra por APROVADO
    const statusSelect = page.locator('select').first();
    if (await statusSelect.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await statusSelect.selectOption('APROVADO');
      await page.waitForTimeout(1_000);
    }

    const orderRow = page.locator(`text=${order.id.slice(0, 8)}`).first();
    if (await orderRow.isVisible({ timeout: 5_000 }).catch(() => false)) {
      await orderRow.click();
    }

    const dispatchBtn = page.getByRole('button', { name: /Despachar/i }).first();
    await dispatchBtn.waitFor({ state: 'visible', timeout: 15_000 });
    await dispatchBtn.click();
    await page.waitForTimeout(1_500);

// Limpa o filtro de status e busca pelo ID do pedido
const orderIdShort = order.id.slice(-5).toUpperCase();
await page.locator('select').first().selectOption('');
await page.locator('input[placeholder*="FE9"]').fill(orderIdShort);
await page.getByRole('button', { name: /Buscar/i }).click();
await page.waitForTimeout(1_000);

await expect(
  page.locator(`tr:has-text("#${orderIdShort}")`).getByText(/Em trânsito/i)
).toBeVisible({ timeout: 10_000 });
    // Confirma via API
    const res = await fetch(`${BACKEND_URL}/admin/orders?size=50`, {
      headers: { 'X-Admin-Key': ADMIN_KEY },
    });
    const data = (await res.json()) as { content: Array<{ id: string; status: string }> };
    const updated = data.content.find((o) => o.id === order.id);
    expect(['EM_TRANSITU', 'EM_TRANSITO']).toContain(updated?.status);
  });

  test('admin despacha pedido via API', async () => {
    const customer = await createCustomerViaApi();
    const order = await createFullOrder(customer.id);
    await approvePaymentViaApi(order.id);

    const res = await fetch(`${BACKEND_URL}/admin/orders/${order.id}/dispatch`, {
      method: 'PATCH',
      headers: { 'X-Admin-Key': ADMIN_KEY },
    });
    expect(res.ok).toBe(true);
    const updated = (await res.json()) as { status: string };
    expect(['EM_TRANSITU', 'EM_TRANSITO']).toContain(updated.status);
  });
});
