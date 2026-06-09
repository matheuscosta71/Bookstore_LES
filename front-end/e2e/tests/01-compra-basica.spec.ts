

import { test, expect } from '@playwright/test';
import { makeCustomerPayload, registerCustomer } from '../helpers/auth';
import {
  addAddressViaProfile,
  addCardViaProfile,
  addFirstBookToCartViaUI,
  checkoutAndFinalize,
} from '../helpers/checkout';

test.describe('01 — Compra básica completa (via UI)', () => {
  test('cliente se cadastra, adiciona livro e finaliza compra', async ({ page }) => {
    page.on('console', msg => console.log('BROWSER LOG:', msg.type(), msg.text()));
    page.on('pageerror', err => console.log('BROWSER EXCEPTION:', err.message));
    const customer = makeCustomerPayload({ password: 'StrongPass!1' });

    // 1. Cadastro e login automático
    await registerCustomer(page, customer);

    // 2. Cadastra endereço de entrega no perfil
    await addAddressViaProfile(page, {
      nickname: 'Minha Casa',
      street: 'Rua das Flores',
      number: '123',
      neighborhood: 'Jardim Primavera',
      city: 'São Paulo',
      state: 'SP',
      zipCode: '01310100',
    });

    // 3. Cadastra cartão no perfil
    await addCardViaProfile(page, {
      cardholderName: customer.fullName,
      cardNumber: '4111111111111111',
      brand: 'VISA',
      expirationMonth: 12,
      expirationYear: 2030,
      preferred: true,
    });

    // 4. Adiciona livro ao carrinho via UI
    await addFirstBookToCartViaUI(page);

    // 5. Vai ao checkout e finaliza
    await checkoutAndFinalize(page);

    // 6. Verifica pedido criado com status EM_PROCESSAMENTO
    await page.goto('/profile/orders');
    await expect(page.getByRole('heading', { name: /Meus pedidos/i })).toBeVisible();
    await expect(page.getByText(/Em processamento/i)).toBeVisible({ timeout: 10_000 });
  });
});
