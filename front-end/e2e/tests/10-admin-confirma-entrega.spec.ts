/**
 * 10-admin-confirma-entrega.spec.ts
 * Cenário: O administrador confirma que o produto foi ENTREGUE
 *
 * Cobre:
 * - Admin confirma entrega via UI (botão "Confirmar entrega")
 * - Status muda para ENTREGUE
 * - Cliente consegue ver o status ENTREGUE em Meus Pedidos
 * - Fluxo completo de ponta a ponta: compra → aprovação → transporte → entrega
 */

import { test, expect } from '@playwright/test';
import {
  createCustomerViaApi,
  createFullOrder,
  loginCustomerViaUI,
} from '../helpers/setup';
import {
  approvePaymentViaApi,
  dispatchOrderViaApi,
  adminLogin,
} from '../helpers/admin';

const BACKEND_URL = process.env.PW_BACKEND_URL ?? 'http://localhost:8080';
const ADMIN_KEY = process.env.VITE_ADMIN_KEY ?? '8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12';

test.describe('10 — Admin confirma entrega do produto', () => {
  test('admin confirma entrega via UI — status vira ENTREGUE', async ({ page }) => {
    const customer = await createCustomerViaApi();
    const order = await createFullOrder(customer.id);
    await approvePaymentViaApi(order.id);
    await dispatchOrderViaApi(order.id);

    await adminLogin(page);
    await page.goto('/admin/orders');
    await page.waitForLoadState('networkidle');

    // Filtra por EM_TRANSITO
    const statusSelect = page.locator('select').first();
    if (await statusSelect.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await statusSelect.selectOption('EM_TRANSITO');
      await page.waitForTimeout(1_000);
    }

    const orderRow = page.locator(`text=${order.id.slice(0, 8)}`).first();
    if (await orderRow.isVisible({ timeout: 5_000 }).catch(() => false)) {
      await orderRow.click();
    }

    // Clica em "Confirmar entrega"
    const deliverBtn = page.getByRole('button', { name: /Confirmar entrega/i }).first();
    await deliverBtn.waitFor({ state: 'visible', timeout: 15_000 });
    await deliverBtn.click();

    await expect(page.getByText(/ENTREGUE/i)).toBeVisible({ timeout: 10_000 });

    // Confirma via API
    const res = await fetch(`${BACKEND_URL}/admin/orders?size=50`, {
      headers: { 'X-Admin-Key': ADMIN_KEY },
    });
    const data = (await res.json()) as { content: Array<{ id: string; status: string }> };
    const updated = data.content.find((o) => o.id === order.id);
    expect(updated?.status).toBe('ENTREGUE');
  });

  test('admin confirma entrega via API', async () => {
    const customer = await createCustomerViaApi();
    const order = await createFullOrder(customer.id);
    await approvePaymentViaApi(order.id);
    await dispatchOrderViaApi(order.id);

    const res = await fetch(`${BACKEND_URL}/admin/orders/${order.id}/deliver`, {
      method: 'PATCH',
     headers: { 'X-Admin-Key': 'admin' }
    });
    expect(res.ok).toBe(true);
    const updated = (await res.json()) as { status: string };
    expect(updated.status).toBe('ENTREGUE');
  });

  test('cliente vê status ENTREGUE em Meus Pedidos após confirmação', async ({ page }) => {
    const customer = await createCustomerViaApi();
    const order = await createFullOrder(customer.id);
    await approvePaymentViaApi(order.id);
    await dispatchOrderViaApi(order.id);

    // Entrega via API
    await fetch(`${BACKEND_URL}/admin/orders/${order.id}/deliver`, {
      method: 'PATCH',
      headers: { 'X-Admin-Key': ADMIN_KEY },
    });

    // Cliente visualiza
    await loginCustomerViaUI(page, customer.email, customer.password);
    await page.goto('/profile/orders');
    await page.reload();

    await expect(page.getByText(/ENTREGUE/i)).toBeVisible({ timeout: 10_000 });

    // Botão de troca deve aparecer
    await expect(page.getByRole('button', { name: /Solicitar troca/i })).toBeVisible();
  });

  test('FLUXO COMPLETO — compra → aprovação → transporte → entrega (ponta a ponta)', async ({ page }) => {
    const customer = await createCustomerViaApi();
    await loginCustomerViaUI(page, customer.email, customer.password);

    // 1. Cria pedido
    const order = await createFullOrder(customer.id);
    expect(order.status).toBe('EM_PROCESSAMENTO');

    // 2. Admin aprova pagamento
    const approveRes = await fetch(`${BACKEND_URL}/admin/orders/${order.id}/approve-payment`, {
      method: 'PATCH', headers: { 'X-Admin-Key': ADMIN_KEY },
    });
    expect((await approveRes.json() as any).status).toBe('APROVADO');

    // 3. Admin despacha
    const dispatchRes = await fetch(`${BACKEND_URL}/admin/orders/${order.id}/dispatch`, {
      method: 'PATCH', headers: { 'X-Admin-Key': ADMIN_KEY },
    });
    const dispatched = (await dispatchRes.json()) as { status: string };
    expect(['EM_TRANSITU', 'EM_TRANSITO']).toContain(dispatched.status);

    // 4. Admin confirma entrega
    const deliverRes = await fetch(`${BACKEND_URL}/admin/orders/${order.id}/deliver`, {
      method: 'PATCH', headers: { 'X-Admin-Key': ADMIN_KEY },
    });
    expect((await deliverRes.json() as any).status).toBe('ENTREGUE');

    // 5. Cliente vê status ENTREGUE
    await page.goto('/profile/orders');
    await page.reload();
    await expect(page.getByText(/ENTREGUE/i)).toBeVisible({ timeout: 10_000 });

    // 6. Cliente pode solicitar troca
    const trocaBtn = page.getByRole('button', { name: /Solicitar troca/i });
    await expect(trocaBtn).toBeVisible();
  });
});
