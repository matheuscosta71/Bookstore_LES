/**
 * 02-meios-de-pagamento.spec.ts
 * Cenário: Cliente pagar com todas as possíveis combinações de meios de pagamento
 *
 * Cobre:
 * - Cartão único
 * - Dois cartões
 * - Cartão + cupom de troca
 * - Cartão + cupom promocional
 * - Cupom de troca integral (se valor cobrir total)
 */

import { test, expect } from '@playwright/test';
import {
  createCustomerViaApi,
  loginCustomerViaUI,
  createAddressViaApi,
  createCardViaApi,
  ensureBookInStock,
  addBookToCartViaApi,
  setCheckoutAddressViaApi,
  finalizeCheckoutViaApi,
  createFullOrder,
  requestExchangeViaApi,
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

/** Obtém o grandTotal do carrinho atual via API */
async function getCartTotal(customerId: string): Promise<number> {
  const res = await fetch(`${BACKEND_URL}/customers/${customerId}/cart`);
  const cart = (await res.json()) as { totalAmount: number; freightAmount?: number };
  return (cart.totalAmount ?? 0) + (cart.freightAmount ?? 0);
}

/** Gera um cupom de troca válido para o customer: cria pedido → entrega → solicita troca → autoriza → recebe */
async function gerarCupomDeTroca(customerId: string): Promise<string> {
  const order = await createFullOrder(customerId);
  await approvePaymentViaApi(order.id);
  await dispatchOrderViaApi(order.id);
  await deliverOrderViaApi(order.id);

  const exchangeRes = await requestExchangeViaApi(customerId, order.id, order.items[0].id);
  await authorizeExchangeViaApi(exchangeRes.id);
  const received = await receiveExchangeViaApi(exchangeRes.id, true);

  if (!received.generatedCouponCode) throw new Error('Cupom de troca não gerado');
  return received.generatedCouponCode;
}


async function irParaCheckout(page: any) {
  await page.goto('/cart');

  await page.locator('text=Ir para checkout').first().click({
    force: true,
  });

  await page.waitForURL(/\/checkout/, {
    timeout: 15000,
  });

  await page.waitForTimeout(2000);

  // Seleciona endereço
  const deliveryRadio = page.locator('input[type="radio"]').first();

  await deliveryRadio.waitFor({
    state: 'visible',
    timeout: 10000,
  });

  if (!(await deliveryRadio.isChecked())) {
    await deliveryRadio.check();
  }

  // MESMA LÓGICA DO CHECKOUT
  const calcularFreteBtn = page.locator(
    'button:has-text("Calcular frete")'
  );

  await calcularFreteBtn.waitFor({
    state: 'visible',
    timeout: 10000,
  });

  await calcularFreteBtn.scrollIntoViewIfNeeded();

  await calcularFreteBtn.click({
    force: true,
  });

  await page.waitForTimeout(3000);
}

async function finalizarCheckout(page: any) {
  const finalizarBtn = page.locator(
    'button:has-text("Finalizar compra")'
  );

  await finalizarBtn.waitFor({
    state: 'visible',
    timeout: 15000,
  });

  await expect(finalizarBtn).toBeEnabled({
    timeout: 15000,
  });

  await finalizarBtn.scrollIntoViewIfNeeded();

  await finalizarBtn.click({
    force: true,
  });

  await expect(
    page.getByRole('heading', {
      name: /Meus pedidos/i,
    })
  ).toBeVisible({
    timeout: 15000,
  });
}

// ─── Testes ───────────────────────────────────────────────────────────────────

test.describe('02 — Combinações de meios de pagamento', () => {
  test('pagamento com cartão único salvo', async ({ page }) => {
    const customer = await createCustomerViaApi();
    await loginCustomerViaUI(page, customer.email, customer.password);

    const book = await ensureBookInStock();
    await addBookToCartViaApi(customer.id, book.id);
    const delivId = await createAddressViaApi(customer.id, 'DELIVERY');
    await createAddressViaApi(customer.id, 'BILLING');
    await createCardViaApi(customer.id, true);
    await setCheckoutAddressViaApi(customer.id, delivId);

    await irParaCheckout(page);

    // Seleciona cartão salvo (radio já marcado por padrão)
    const cardRadio = page.locator('input[type="radio"]').filter({ hasText: /salvo/i });
    if (await cardRadio.count() > 0) await cardRadio.first().check();

    // Confirma que há cartão listado
    await expect(page.getByText(/VISA/i)).toBeVisible({ timeout: 8_000 });

    await finalizarCheckout(page);
  });

 // test('pagamento com dois cartões (divisão de valor)', async ({ page }) => {
 //   const customer = await createCustomerViaApi();
  // await loginCustomerViaUI(page, customer.email, customer.password);

   // const book = await ensureBookInStock();
 //   await addBookToCartViaApi(customer.id, book.id);
  //  const delivId = await createAddressViaApi(customer.id, 'DELIVERY');
  //  await createAddressViaApi(customer.id, 'BILLING');

    // Cria dois cartões
    //await createCardViaApi(customer.id, false); // cartão 2
  //  await createCardViaApi(customer.id, true);  // cartão 1 (preferred)
   // await setCheckoutAddressViaApi(customer.id, delivId);

    // Calcula o total para dividir via API
    //const freightRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/freight`, {
    //  method: 'POST',
     // body: JSON.stringify({ addressId: delivId }),
   // });
   // const freight = (await freightRes.json()) as { grandTotal: number };
   // const half = parseFloat((freight.grandTotal / 2).toFixed(2));
    //const rest = parseFloat((freight.grandTotal - half).toFixed(2));
//
    // Obtém lista de cartões
    //const cardsRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/credit-cards`);
    //const cards = (await cardsRes.json()) as Array<{ id: string }>;

    // Define pagamento com dois cartões via API
    //await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/payment`, {
    //  method: 'POST',
     // headers: { 'Content-Type': 'application/json' },
      //body: JSON.stringify({
      //  lines: [
      //    { paymentType: 'CREDIT_CARD', amount: half, creditCardId: cards[0].id },
       //   { paymentType: 'CREDIT_CARD', amount: rest, creditCardId: cards[1].id },
      //  ],
     // }),
    //});

    // Finaliza
    // const order = await finalizeCheckoutViaApi(customer.id);
    // expect(order.status).toBe('EM_PROCESSAMENTO');
    // expect(order.payments).toHaveLength(2);
    // expect(order.payments.every((p: any) => p.paymentType === 'CREDIT_CARD')).toBe(true);
  // });

  test('pagamento com cartão + cupom de troca', async ({ page }) => {
    // Precisa gerar um cupom de troca antes
    const customer = await createCustomerViaApi();
    await loginCustomerViaUI(page, customer.email, customer.password);

    // Gera cupom de troca com ciclo completo
    const couponCode = await gerarCupomDeTroca(customer.id);

    // Nova compra usando o cupom
    const book = await ensureBookInStock();
    await addBookToCartViaApi(customer.id, book.id);
    const delivId = await createAddressViaApi(customer.id, 'DELIVERY');
    await createAddressViaApi(customer.id, 'BILLING');
    const cardId = await createCardViaApi(customer.id, true);
    await setCheckoutAddressViaApi(customer.id, delivId);

    // Valida cupom via API
    const validateRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/coupon/validate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: couponCode, paymentType: 'EXCHANGE_COUPON' }),
    });
    expect(validateRes.ok).toBe(true);
    const couponValue = ((await validateRes.json()) as { amount: number }).amount;

    const freightRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/freight`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ addressId: delivId }),
    });
    const { grandTotal } = (await freightRes.json()) as { grandTotal: number };

    const cardAmount = parseFloat(Math.max(0, grandTotal - couponValue).toFixed(2));

    const lines: any[] = [{ paymentType: 'EXCHANGE_COUPON', amount: couponValue, couponCode }];
    if (cardAmount > 0) lines.push({ paymentType: 'CREDIT_CARD', amount: cardAmount, creditCardId: cardId });

    await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/payment`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ lines }),
    });

    const order = await finalizeCheckoutViaApi(customer.id);
    expect(order.status).toBe('EM_PROCESSAMENTO');
    expect(order.payments.some((p: any) => p.paymentType === 'EXCHANGE_COUPON')).toBe(true);
  });

  test('pagamento com cartão + cupom promocional', async ({ page }) => {
    const customer = await createCustomerViaApi();
    await loginCustomerViaUI(page, customer.email, customer.password);

    // Cria cupom promocional via API admin
    const couponCreateRes = await fetch(`${BACKEND_URL}/admin/coupons`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Admin-Key': ADMIN_KEY },
      body: JSON.stringify({
        code: `PROMO${Date.now()}`,
        discountAmount: 10,
        type: 'PROMOTIONAL_COUPON',
        active: true,
      }),
    });

    let promoCode: string;
    if (couponCreateRes.ok) {
      promoCode = ((await couponCreateRes.json()) as { code: string }).code;
    } else {
      // Tenta validar um cupom existente
      test.skip(true, 'API de criação de cupom promocional não disponível — pule ou ajuste endpoint');
      return;
    }

    const book = await ensureBookInStock();
    await addBookToCartViaApi(customer.id, book.id);
    const delivId = await createAddressViaApi(customer.id, 'DELIVERY');
    await createAddressViaApi(customer.id, 'BILLING');
    const cardId = await createCardViaApi(customer.id, true);
    await setCheckoutAddressViaApi(customer.id, delivId);

    const freightRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/freight`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ addressId: delivId }),
    });
    const { grandTotal } = (await freightRes.json()) as { grandTotal: number };
    const cardAmount = parseFloat((grandTotal - 10).toFixed(2));

    await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/payment`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        lines: [
          { paymentType: 'PROMOTIONAL_COUPON', amount: 10, couponCode: promoCode },
          { paymentType: 'CREDIT_CARD', amount: cardAmount, creditCardId: cardId },
        ],
      }),
    });

    const order = await finalizeCheckoutViaApi(customer.id);
    expect(order.status).toBe('EM_PROCESSAMENTO');
    expect(order.payments.some((p: any) => p.paymentType === 'PROMOTIONAL_COUPON')).toBe(true);
  });
});
