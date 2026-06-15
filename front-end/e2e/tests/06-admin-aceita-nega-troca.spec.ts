/**
 * 06-admin-aceita-nega-troca.spec.ts
 * Cenário: O administrador aceita ou nega a troca / devolução
 *
 * Cobre:
 * - Admin autoriza troca via UI (página /admin/exchanges)
 * - Admin nega troca (via UI ou ausência de ação / endpoint de deny se existir)
 * - Verificação de status da solicitação
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
  adminLogin,
  listExchangeRequestsViaApi,
} from '../helpers/admin';

const BACKEND_URL = process.env.PW_BACKEND_URL ?? 'http://localhost:8080';
const ADMIN_KEY = process.env.VITE_ADMIN_KEY ?? '8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12';

/** Prepara pedido no estado EM_TROCA e retorna dados necessários */
async function prepararPedidoEmTroca() {
  const customer = await createCustomerViaApi();
  const order = await createFullOrder(customer.id);
  await approvePaymentViaApi(order.id);
  await dispatchOrderViaApi(order.id);
  await deliverOrderViaApi(order.id);
  const exchange = await requestExchangeViaApi(customer.id, order.id, order.items[0].id);
  return { customer, order, exchange };
}

test.describe('06 — Admin aceita/nega troca', () => {
  test('admin autoriza troca via UI (página /admin/exchanges)', async ({ page }) => {
    const { exchange } = await prepararPedidoEmTroca();

    await adminLogin(page);
    await page.goto('/admin/exchanges');
    await page.waitForLoadState('networkidle');

    // Seleciona filtro "EM_TROCA" se necessário
    const statusSelect = page.locator('select').first();
    if (await statusSelect.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await statusSelect.selectOption('EM_TROCA');
      await page.waitForTimeout(100);
    }

    // Clica em "Autorizar" para a solicitação
    const autorizarBtn = page.getByRole('button', { name: /Autorizar/i }).first();
    await autorizarBtn.waitFor({ state: 'visible', timeout: 15_000 });
    await autorizarBtn.click();

    // Verifica feedback de autorização
   await expect(
  page.getByText(/Troca autorizada/i)
).toBeVisible({ timeout: 10_000 });

    // Confirma via API
    const requests = await listExchangeRequestsViaApi('TROCA_AUTORIZADA');
    const authorized = requests.find((r) => r.id === exchange.id);
    expect(authorized).toBeTruthy();
    expect(authorized?.exchangeStatus).toBe('AUTHORIZED');
  });

  test('admin autoriza troca via API direta', async () => {
    const { exchange } = await prepararPedidoEmTroca();

    const res = await fetch(
      `${BACKEND_URL}/admin/exchange-requests/${exchange.id}/authorize`,
      {
        method: 'PATCH',
        headers: { 'X-Admin-Key': ADMIN_KEY },
      }
    );
    expect(res.ok).toBe(true);
    const updated = (await res.json()) as { exchangeStatus: string };
    expect(updated.exchangeStatus).toBe('AUTHORIZED');
  });

  test('admin autoriza troca via página de pedidos (RF0041)', async ({ page }) => {
    const { order } = await prepararPedidoEmTroca();

    await adminLogin(page);
    await page.goto('/admin/orders');
    await page.waitForLoadState('networkidle');

    // Filtra por status EM_TROCA
    const statusSelect = page.locator('select').first();
    if (await statusSelect.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await statusSelect.selectOption('EM_TROCA');
      await page.waitForTimeout(100);
    }

    // Abre detalhes do pedido
    const orderRow = page.locator(`text=${order.id.slice(0, 8)}`).first();
    if (await orderRow.isVisible({ timeout: 5_000 }).catch(() => false)) {
      await orderRow.click();
      await page.waitForTimeout(50);
    }

    // Clica em "Autorizar troca (RF0041)"
    const autorizarBtn = page.getByRole('button', { name: /Autorizar troca/i }).first();
    await autorizarBtn.waitFor({ state: 'visible', timeout: 15_000 });
    await autorizarBtn.click();

    await expect(
  page.getByText(/Troca autorizada\. O pedido passou/i)
).toBeVisible({ timeout: 10_000 });
  });

  test('solicitação negada — pedido permanece EM_TROCA (sem endpoint de deny)', async () => {
    // A API atual não tem endpoint de DENY explícito: a troca fica REQUESTED até ser autorizada.
    // Este teste documenta o comportamento esperado.
    const { exchange } = await prepararPedidoEmTroca();

    // Verificar que solicitação ainda está REQUESTED (não auto-aprovada)
    const requests = await listExchangeRequestsViaApi('EM_TROCA');
    const found = requests.find((r) => r.id === exchange.id);
    expect(found).toBeTruthy();
    expect(found?.exchangeStatus).toBe('REQUESTED');
  });
});
