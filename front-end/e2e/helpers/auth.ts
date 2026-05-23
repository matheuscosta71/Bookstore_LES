import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';

export type CustomerPayload = {
  fullName: string;
  email: string;
  cpf: string;
  phone: string;
  birthDate: string;
  password: string;
};

function randomDigits(n: number) {
  return String(Math.floor(Math.random() * Math.pow(10, n))).padStart(n, '0');
}

export function makeCustomerPayload(overrides?: Partial<CustomerPayload>): CustomerPayload {
  const now = Date.now();
  const rand = Math.floor(Math.random() * 1_000_000_000_000);
  return {
    fullName: overrides?.fullName ?? `Cliente ${now}`,
    email: overrides?.email ?? `cliente_${now}_${rand}@gmail.com`,
    cpf: overrides?.cpf ?? `${randomDigits(10)}${randomDigits(1)}`,
    phone: overrides?.phone ?? `319${randomDigits(8)}`,
    birthDate: overrides?.birthDate ?? '1990-05-20',
    password: overrides?.password ?? 'StrongPass!1',
    ...overrides,
  };
}

export async function registerCustomer(page: Page, payload: CustomerPayload): Promise<void> {
  await page.goto('/register');

  await page.locator('input[name="fullName"]').fill(payload.fullName);
  await page.locator('input[name="email"]').fill(payload.email);
  await page.locator('input[name="cpf"]').fill(payload.cpf);
  await page.locator('input[name="phone"]').fill(payload.phone);
  await page.locator('input[name="birthDate"]').fill(payload.birthDate);
  await page.locator('input[name="password"]').fill(payload.password);
  await page.locator('input[name="confirmPassword"]').fill(payload.password);

  await page.getByRole('button', { name: /Cadastrar/i }).click();

 
  await page.waitForURL(/\/login/, { timeout: 15_000 });

  await page.locator('input[type="email"]').waitFor({ state: 'visible' });
  await page.locator('input[type="password"]').first().waitFor({ state: 'visible' });

  await page.locator('input[type="email"]').fill(payload.email);
  await page.locator('input[type="password"]').first().fill(payload.password);

  await page.getByRole('button', { name: /^Entrar$/ }).click();

  await page.waitForURL(/\/$/, { timeout: 15_000 });

  
  await page.waitForFunction(
    () => localStorage.getItem('mg_customer_id') !== null,
    { timeout: 10_000 }
  );
}

export async function login(page: Page, email: string, password: string): Promise<void> {
  if (!page.url().includes('/login')) {
    await page.goto('/login');
  }

  await page.locator('input[type="email"]').waitFor({ state: 'visible' });
  await page.locator('input[type="email"]').fill(email);
  await page.locator('input[type="password"]').first().fill(password);
  await page.getByRole('button', { name: /^Entrar$/ }).click();

  await page.waitForURL(/\/$/, { timeout: 15_000 });

  await page.waitForFunction(
    () => localStorage.getItem('mg_customer_id') !== null,
    { timeout: 10_000 }
  );
}

export async function expectLoggedIn(page: Page): Promise<void> {
  // ✅ Aguarda o localStorage ter o id antes de navegar
  await page.waitForFunction(
    () => localStorage.getItem('mg_customer_id') !== null,
    { timeout: 10_000 }
  );

  // Navega para o carrinho via link
  const cartLink = page.getByRole('link', { name: /Carrinho/i }).first();
  await cartLink.click();

  const cartHeading = page.getByRole('heading', { name: /Carrinho de compras/i });
  const emptyTitle = page.getByText(/Seu carrinho está vazio/i);
  const loginHeading = page.getByRole('heading', { name: /Entrar/i });

  await Promise.race([
    cartHeading.waitFor(),
    emptyTitle.waitFor(),
    loginHeading.waitFor().then(() => {
      throw new Error('Redirecionou para /login (não logado)');
    }),
  ]);
}