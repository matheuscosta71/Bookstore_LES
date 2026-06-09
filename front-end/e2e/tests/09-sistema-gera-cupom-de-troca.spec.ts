/**
 * 09-sistema-gera-cupom-de-troca.spec.ts
 * Cenário: O sistema gera cupom de troca (RF0044)
 *
 * Cobre:
 * - Cupom é gerado após recebimento do produto devolvido
 * - Cupom aparece no pedido do cliente (campo exchangeCouponCode)
 * - Cupom pode ser validado no checkout de uma nova compra
 * - Cupom é único (código não se repete)
 */

import { test, expect } from '@playwright/test';
import {
  createCustomerViaApi,
  createFullOrder,
  requestExchangeViaApi,
  ensureBookInStock,
  addBookToCartViaApi,
  createAddressViaApi,
  setCheckoutAddressViaApi,
  finalizeCheckoutViaApi,
} from '../helpers/setup';
import {
  approvePaymentViaApi,
  dispatchOrderViaApi,
  deliverOrderViaApi,
  authorizeExchangeViaApi,
  receiveExchangeViaApi,
} from '../helpers/admin';

const BACKEND_URL = process.env.PW_BACKEND_URL ?? 'http://localhost:8080';
const ADMIN_KEY = process.env.VITE_ADMIN_KEY ?? '8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12';

/** Ciclo completo de troca e retorno do cupom gerado */
async function cicloCompletoTroca(customerId: string): Promise<string> {
  const order = await createFullOrder(customerId);
  await approvePaymentViaApi(order.id);
  await dispatchOrderViaApi(order.id);
  await deliverOrderViaApi(order.id);

  const exchange = await requestExchangeViaApi(customerId, order.id, order.items[0].id);
  await authorizeExchangeViaApi(exchange.id);
  const received = await receiveExchangeViaApi(exchange.id, true);

  if (!received.generatedCouponCode) {
    throw new Error('Sistema não gerou cupom de troca — verifique RF0044');
  }
  return received.generatedCouponCode;
}

test.describe('09 — Sistema gera cupom de troca (RF0044)', () => {
  test('cupom é gerado após recebimento e tem código único', async () => {
    const customer = await createCustomerViaApi();
    const couponCode = await cicloCompletoTroca(customer.id);

    expect(typeof couponCode).toBe('string');
    expect(couponCode.length).toBeGreaterThan(0);

    // Segundo cupom deve ser diferente
    const couponCode2 = await cicloCompletoTroca(customer.id);
    expect(couponCode2).not.toBe(couponCode);
  });

  test('cupom aparece no pedido do cliente (campo exchangeCouponCode)', async ({ page }) => {
    const customer = await createCustomerViaApi();
    const order = await createFullOrder(customer.id);

    await approvePaymentViaApi(order.id);
    await dispatchOrderViaApi(order.id);
    await deliverOrderViaApi(order.id);

    const exchange = await requestExchangeViaApi(customer.id, order.id, order.items[0].id);
    await authorizeExchangeViaApi(exchange.id);
    const received = await receiveExchangeViaApi(exchange.id, true);
    const couponCode = received.generatedCouponCode!;

    // Busca pedido via API do cliente e verifica campo
    const ordersRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/orders`);
    const orders = (await ordersRes.json()) as Array<{ id: string; exchangeCouponCode?: string | null }>;
    const updatedOrder = orders.find((o) => o.id === order.id);
    expect(updatedOrder?.exchangeCouponCode).toBe(couponCode);
  });

  test('cupom aparece na página "Meus pedidos" do cliente (via UI)', async ({ page }) => {
    const customer = await createCustomerViaApi();

    // Importa helper de login
    const { loginCustomerViaUI } = await import('../helpers/setup');
    await loginCustomerViaUI(page, customer.email, customer.password);

    const couponCode = await cicloCompletoTroca(customer.id);

    await page.goto('/profile/orders');
    await page.reload();

    // Cupom deve aparecer na UI
    await expect(page.getByText(couponCode)).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(/Cupom de troca:/i)).toBeVisible();
  });

  test('cupom pode ser validado e usado em nova compra', async () => {
    const customer = await createCustomerViaApi();
    const couponCode = await cicloCompletoTroca(customer.id);

    // Valida o cupom via endpoint de checkout
    const validateRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/coupon/validate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: couponCode, paymentType: 'EXCHANGE_COUPON' }),
    });
    expect(validateRes.ok).toBe(true);
    const { amount } = (await validateRes.json()) as { amount: number };
    expect(amount).toBeGreaterThan(0);

    // Usa o cupom em uma nova compra
    const book = await ensureBookInStock();
    await addBookToCartViaApi(customer.id, book.id);
    const delivId = await createAddressViaApi(customer.id, 'DELIVERY');
    await createAddressViaApi(customer.id, 'BILLING');
    await setCheckoutAddressViaApi(customer.id, delivId);

    const freightRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/freight`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ addressId: delivId }),
    });
    const { grandTotal } = (await freightRes.json()) as { grandTotal: number };

    const cardAmount = Math.max(0, grandTotal - amount);
    const lines: any[] = [{ paymentType: 'EXCHANGE_COUPON', amount, couponCode }];

    if (cardAmount > 0.5) {
      // Precisa de cartão para completar
      const cardRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/cards`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          cardholderName: 'Teste',
          cardNumber: '4111111111111111',
          brand: 'VISA',
          expirationMonth: 12,
          expirationYear: 2030,
          preferred: true,
        }),
      });
      const card = (await cardRes.json()) as { id: string };
      lines.push({ paymentType: 'CREDIT_CARD', amount: cardAmount, creditCardId: card.id });
    }

    const payRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/payment`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ lines }),
    });
    if (!payRes.ok) {
      console.log('PAYMENT ERROR STATUS:', payRes.status);
      console.log('PAYMENT ERROR BODY:', await payRes.text());
    }

    const newOrder = await finalizeCheckoutViaApi(customer.id);
    expect(newOrder.status).toBe('EM_PROCESSAMENTO');
    expect(newOrder.payments.some((p: any) => p.paymentType === 'EXCHANGE_COUPON')).toBe(true);
  });

  test('cupom já usado não pode ser reutilizado', async () => {
    const customer = await createCustomerViaApi();
    const couponCode = await cicloCompletoTroca(customer.id);

    // Usa o cupom
    const book = await ensureBookInStock();
    await addBookToCartViaApi(customer.id, book.id);
    const delivId = await createAddressViaApi(customer.id, 'DELIVERY');
    await createAddressViaApi(customer.id, 'BILLING');
    await setCheckoutAddressViaApi(customer.id, delivId);

    const freightRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/freight`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ addressId: delivId }),
    });
    const { grandTotal } = (await freightRes.json()) as { grandTotal: number };

    const validateRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/coupon/validate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: couponCode, paymentType: 'EXCHANGE_COUPON' }),
    });
    const { amount } = (await validateRes.json()) as { amount: number };

    const cardRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/cards`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ cardholderName: 'T', cardNumber: '4111111111111111', brand: 'VISA', expirationMonth: 12, expirationYear: 2030, preferred: true }),
    });
    const card = (await cardRes.json()) as { id: string };
    const cardAmount = parseFloat(Math.max(0, grandTotal - amount).toFixed(2));

    const lines: any[] = [{ paymentType: 'EXCHANGE_COUPON', amount, couponCode }];
    if (cardAmount > 0) lines.push({ paymentType: 'CREDIT_CARD', amount: cardAmount, creditCardId: card.id });

    const payRes2 = await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/payment`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ lines }),
    });
    if (!payRes2.ok) {
      console.log('PAYMENT 2 ERROR STATUS:', payRes2.status);
      console.log('PAYMENT 2 ERROR BODY:', await payRes2.text());
    }
    const order = await finalizeCheckoutViaApi(customer.id);
    await approvePaymentViaApi(order.id);

    // Tenta usar o mesmo cupom novamente — deve falhar
    const book2 = await ensureBookInStock();
    await addBookToCartViaApi(customer.id, book2.id);
    const delivId2 = await createAddressViaApi(customer.id, 'DELIVERY');
    await setCheckoutAddressViaApi(customer.id, delivId2);

    const reuseRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/coupon/validate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: couponCode, paymentType: 'EXCHANGE_COUPON' }),
    });
    expect(reuseRes.ok).toBe(false); // deve retornar 4xx — cupom já utilizado
  });
});
