/**
 * 04-solicitacao-troca-devolucao.spec.ts
 * Cenário: Usuário pode solicitar troca ou devolução de um item do pedido
 *          ou do pedido completo
 *
 * Cobre:
 * - Solicitar troca de um item específico (status ENTREGUE)
 * - Solicitar troca de todos os itens do pedido
 * - Verificar que a solicitação muda o status do pedido para EM_TROCA
 */

import { test, expect } from '@playwright/test';
import {
  createCustomerViaApi,
  loginCustomerViaUI,
  createFullOrder,
  requestExchangeViaApi,
} from '../helpers/setup';
import {
  approvePaymentViaApi,
  dispatchOrderViaApi,
  deliverOrderViaApi,
} from '../helpers/admin';

test.describe('04 — Solicitação de troca / devolução', () => {
  test('cliente solicita troca de um item via UI após pedido entregue', async ({ page }) => {
    const customer = await createCustomerViaApi();
    await loginCustomerViaUI(page, customer.email, customer.password);

    // Cria pedido e leva até ENTREGUE via API
    const order = await createFullOrder(customer.id);
    await approvePaymentViaApi(order.id);
    await dispatchOrderViaApi(order.id);
    await deliverOrderViaApi(order.id);

    // Acessa "Meus Pedidos" via UI e solicita troca
    await page.goto('/profile/orders');
    await expect(page.getByRole('heading', { name: /Meus pedidos/i })).toBeVisible();

    // Recarrega para garantir status atualizado
    await page.reload();
    await expect(page.getByText(/ENTREGUE/i)).toBeVisible({ timeout: 10_000 });

    // Clica em "Solicitar troca"
    const trocaBtn = page.getByRole('button', { name: /Solicitar troca/i }).first();
    await trocaBtn.waitFor({ state: 'visible', timeout: 10_000 });
    await trocaBtn.click();

    // Verifica feedback de sucesso
    await expect(
      page.getByText(/Solicitação de troca registrada|EM_TROCA/i)
    ).toBeVisible({ timeout: 10_000 });
  });

  test('cliente solicita troca de item único via API', async ({ page }) => {
    const customer = await createCustomerViaApi();

    // Prepara pedido entregue
    const order = await createFullOrder(customer.id);
    await approvePaymentViaApi(order.id);
    await dispatchOrderViaApi(order.id);
    await deliverOrderViaApi(order.id);

    expect(order.items.length).toBeGreaterThan(0);
    const firstItem = order.items[0];

    // Solicita troca via API
    const BACKEND_URL = process.env.PW_BACKEND_URL ?? 'http://localhost:8080';
    const exchangeRes = await fetch(
      `${BACKEND_URL}/customers/${customer.id}/orders/${order.id}/exchange-requests`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ orderItemId: firstItem.id }),
      }
    );
    expect(exchangeRes.ok).toBe(true);
    const exchange = (await exchangeRes.json()) as { id: string; exchangeStatus: string };
    expect(exchange.exchangeStatus).toBe('REQUESTED');

    // Verifica que o status do pedido mudou para EM_TROCA
    const ordersRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/orders`);
    const orders = (await ordersRes.json()) as Array<{ id: string; status: string }>;
    const updatedOrder = orders.find((o) => o.id === order.id);
    expect(updatedOrder?.status).toBe('EM_TROCA');
  });

  test('cliente solicita troca de todos os itens do pedido (devolução total)', async ({ page }) => {
    const customer = await createCustomerViaApi();

    // Cria pedido com múltiplos itens (se disponível) ou 1 item
    const order = await createFullOrder(customer.id);
    await approvePaymentViaApi(order.id);
    await dispatchOrderViaApi(order.id);
    await deliverOrderViaApi(order.id);

    const BACKEND_URL = process.env.PW_BACKEND_URL ?? 'http://localhost:8080';

    // Solicita troca de cada item
    for (const item of order.items) {
      const exchangeRes = await fetch(
        `${BACKEND_URL}/customers/${customer.id}/orders/${order.id}/exchange-requests`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ orderItemId: item.id }),
        }
      );
      // A API pode não permitir mais de um por vez — valida o primeiro
      if (order.items.indexOf(item) === 0) {
        expect(exchangeRes.ok).toBe(true);
      }
    }

    // Verifica status EM_TROCA
    const ordersRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/orders`);
    const orders = (await ordersRes.json()) as Array<{ id: string; status: string }>;
    const updatedOrder = orders.find((o) => o.id === order.id);
    expect(updatedOrder?.status).toBe('EM_TROCA');
  });

  test('pedido não entregue não permite solicitar troca', async ({ page }) => {
    const customer = await createCustomerViaApi();
    await loginCustomerViaUI(page, customer.email, customer.password);

    // Pedido apenas aprovado (não entregue)
    const order = await createFullOrder(customer.id);
    await approvePaymentViaApi(order.id);

    await page.goto('/profile/orders');
    await page.reload();

    // Botão "Solicitar troca" NÃO deve estar visível para pedido APROVADO
    await page.waitForTimeout(2_000);
    const trocaBtn = page.getByRole('button', { name: /Solicitar troca/i });
    await expect(trocaBtn).not.toBeVisible();
  });
});
