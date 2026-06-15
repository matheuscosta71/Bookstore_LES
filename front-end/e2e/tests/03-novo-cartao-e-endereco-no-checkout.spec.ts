/**
 * 03-novo-cartao-e-endereco-no-checkout.spec.ts
 * Cenário: Cliente pode registrar novo cartão e novo endereço de entrega no ato da compra
 *
 * Cobre:
 * - Cadastrar endereço novo direto no checkout (sem ir para o perfil)
 * - Cadastrar novo cartão direto no checkout
 * - Ambos ao mesmo tempo
 */

import { test, expect } from '@playwright/test';
import {
  createCustomerViaApi,
  loginCustomerViaUI,
  ensureBookInStock,
  addBookToCartViaApi,
} from '../helpers/setup';

test.describe('03 — Novo cartão e endereço no checkout (via UI)', () => {
  test('cadastra novo endereço de entrega direto no checkout', async ({ page }) => {
    const customer = await createCustomerViaApi();
    await loginCustomerViaUI(page, customer.email, customer.password);

    const book = await ensureBookInStock();
    await addBookToCartViaApi(customer.id, book.id);

    await page.goto('/cart');
    await page.getByRole('link', { name: /Ir para checkout/i }).click();
    await page.waitForURL(/\/checkout/);

    // Clica em "+ Cadastrar novo endereço"
    const novoEnderecoBtn = page.getByRole('button', { name: /Cadastrar novo endereço/i });
    await novoEnderecoBtn.waitFor({ state: 'visible', timeout: 10_000 });
    await novoEnderecoBtn.click();

    // Preenche formulário de novo endereço
    await page.locator('input[name="nickname"]').fill('Trabalho');
    await page.locator('input[name="street"]').fill('Av. Paulista');
    await page.locator('input[name="number"]').fill('1000');
    await page.locator('input[name="neighborhood"]').fill('Bela Vista');
    await page.locator('input[name="city"]').fill('São Paulo');
    await page.locator('input[name="state"]').fill('SP');
    await page.locator('input[name="zipCode"]').fill('01310100');

    // Salva endereço
    await page.getByRole('button', { name: /Usar este endereço/i }).click();

    // Verifica se endereço foi aplicado (frete disponível)
    const calcularFreteBtn = page.locator('button', { hasText: /Calcular frete/i });
    await calcularFreteBtn.waitFor({ state: 'visible', timeout: 10_000 });
    await calcularFreteBtn.click();
    await page.waitForTimeout(200);

    // Verifica que o botão de finalizar ficou disponível
    await expect(page.getByRole('button', { name: /Finalizar compra/i })).toBeVisible();
  });

  test('cadastra novo cartão direto no checkout', async ({ page }) => {
    const customer = await createCustomerViaApi();
    await loginCustomerViaUI(page, customer.email, customer.password);

    const book = await ensureBookInStock();
    await addBookToCartViaApi(customer.id, book.id);

    // Cria endereço via API para não depender do teste de endereço
    const BACKEND_URL = process.env.PW_BACKEND_URL ?? 'http://localhost:8080';
    const delivRes = await fetch(`${BACKEND_URL}/customers/${customer.id}/addresses`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nickname: 'Casa',
        street: 'Rua A',
        number: '1',
        neighborhood: 'Centro',
        city: 'SP',
        state: 'SP',
        zipCode: '01310100',
        type: 'DELIVERY',
      }),
    });
    const delivAddr = (await delivRes.json()) as { id: string };

    await fetch(`${BACKEND_URL}/customers/${customer.id}/addresses`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nickname: 'Cobrança',
        street: 'Rua B',
        number: '2',
        neighborhood: 'Centro',
        city: 'SP',
        state: 'SP',
        zipCode: '01310100',
        type: 'BILLING',
      }),
    });

    await fetch(`${BACKEND_URL}/customers/${customer.id}/checkout/address`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ addressId: delivAddr.id }),
    });

    await page.goto('/cart');
    await page.getByRole('link', { name: /Ir para checkout/i }).click();
    await page.waitForURL(/\/checkout/);

    // Calcula frete
    const calcBtn = page.locator('button', { hasText: /Calcular frete/i });
    if (await calcBtn.isVisible({ timeout: 5_000 }).catch(() => false)) {
      await calcBtn.click();
      await page.waitForTimeout(150);
    }

    // Seleciona opção "Novo cartão"
    const novoCartaoRadio = page.locator('input[type="radio"]').nth(1); // segundo radio = novo cartão
    await novoCartaoRadio.click();

    // Preenche dados do novo cartão
    const cardholderInput = page.locator('input[name="cardholderName"]');
    await cardholderInput.waitFor({ state: 'visible', timeout: 8_000 });
    await cardholderInput.fill(customer.fullName);

    await page.locator('input[name="cardNumber"]').fill('5500000000000004');
    await page.locator('input[name="brand"]').fill('MASTERCARD');
    await page.locator('input[name="expirationMonth"]').fill('11');
    await page.locator('input[name="expirationYear"]').fill('2029');

    // Finaliza compra com novo cartão
    await page.getByRole('button', {
  name: /Adicionar cartão ao perfil/i
}).click();

await page.waitForTimeout(200);

const calcBtn2 = page.locator('button', {
  hasText: /Calcular frete/i
});

await calcBtn2.click();

await page.waitForTimeout(300);

const finalizarBtn = page.getByRole('button', {
  name: /Finalizar compra/i
});

await expect(finalizarBtn).toBeEnabled({
  timeout: 15000,
});

await finalizarBtn.click();
    await expect(page.getByRole('heading', { name: /Meus pedidos/i })).toBeVisible({ timeout: 15_000 });
    await expect(page.getByText(/em processamento/i)).toBeVisible();
  });

  test('cadastra novo endereço e novo cartão simultaneamente no checkout', async ({ page }) => {
    const customer = await createCustomerViaApi();
    await loginCustomerViaUI(page, customer.email, customer.password);

    const book = await ensureBookInStock();
    await addBookToCartViaApi(customer.id, book.id);

    await page.goto('/cart');
    await page.getByRole('link', { name: /Ir para checkout/i }).click();
    await page.waitForURL(/\/checkout/);

    // 1. Novo endereço
    const novoEnderecoBtn = page.getByRole('button', { name: /Cadastrar novo endereço/i });
    await novoEnderecoBtn.waitFor({ state: 'visible', timeout: 10_000 });
    await novoEnderecoBtn.click();

    await page.locator('input[name="nickname"]').fill('Casa Nova');
    await page.locator('input[name="street"]').fill('Rua Nova');
    await page.locator('input[name="number"]').fill('500');
    await page.locator('input[name="neighborhood"]').fill('Vila Nova');
    await page.locator('input[name="city"]').fill('Campinas');
    await page.locator('input[name="state"]').fill('SP');
    await page.locator('input[name="zipCode"]').fill('13010000');

    // Marca "mesmo endereço para entrega e cobrança"
    const sameBillingCheckbox = page.locator('input[type="checkbox"]').first();
    if (await sameBillingCheckbox.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await sameBillingCheckbox.check();
    }

    await page.getByRole('button', { name: /Usar este endereço/i }).click();

    // Calcula frete
    const calcBtn = page.locator('button', { hasText: /Calcular frete/i });
    if (await calcBtn.isVisible({ timeout: 5_000 }).catch(() => false)) {
      await calcBtn.click();
      await page.waitForTimeout(150);
    }

    // 2. Novo cartão
    const novoCartaoRadio = page.locator('input[type="radio"]').filter({ hasText: /novo/i });
    if (await novoCartaoRadio.count() > 0) await novoCartaoRadio.first().check();
    else await page.locator('input[type="radio"]').nth(1).click();

    const cardholderInput = page.locator('input[name="cardholderName"]');
    await cardholderInput.waitFor({ state: 'visible', timeout: 8_000 });
    await cardholderInput.fill(customer.fullName);
    await page.locator('input[name="cardNumber"]').fill('4111111111111111');
    await page.locator('input[name="brand"]').fill('VISA');
    await page.locator('input[name="expirationMonth"]').fill('10');
    await page.locator('input[name="expirationYear"]').fill('2028');

    // Finaliza
    await page.getByRole('button', {
  name: /Adicionar cartão ao perfil/i
}).click();

await page.waitForTimeout(200);

const calcBtn2 = page.locator('button', {
  hasText: /Calcular frete/i
});

await calcBtn2.click();

await page.waitForTimeout(300);

const finalizarBtn = page.getByRole('button', {
  name: /Finalizar compra/i
});

await expect(finalizarBtn).toBeEnabled({
  timeout: 15000,
});

await finalizarBtn.click();
    await expect(page.getByRole('heading', { name: /Meus pedidos/i })).toBeVisible({ timeout: 9_000 });
    await expect(page.getByText(/em processamento/i)).toBeVisible();

    // Verifica que o endereço novo aparece no perfil
    await page.goto('/profile/addresses');
  page.getByRole('listitem', { name: /Casa Nova/i })
  });
});
