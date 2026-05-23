/**
 * setup.ts — funções compartilhadas para preparar o estado necessário antes dos
 * testes de venda completo (7ª entrega).
 *
 * Padrão: todas as preparações usam chamadas diretas à API do backend para não
 * depender de outros fluxos de UI e manter cada teste isolado.
 */

import type { Page } from '@playwright/test';

const BACKEND_URL = process.env.PW_BACKEND_URL ?? 'http://localhost:8080';
const ADMIN_KEY = process.env.VITE_ADMIN_KEY ?? '8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12';

export type CustomerCredentials = {
  id: string;
  email: string;
  password: string;
  fullName: string;
};

function rand(n: number) {
  return String(Math.floor(Math.random() * Math.pow(10, n))).padStart(n, '0');
}

/** Cria um cliente via API e retorna suas credenciais */
export async function createCustomerViaApi(): Promise<CustomerCredentials> {
  const now = Date.now();
  const payload = {
    fullName: `Comprador ${now}`,
    email: `comprador_${now}@teste.com`,
    cpf: `${rand(10)}${rand(1)}`,
    phone: `319${rand(8)}`,
    birthDate: '1990-06-15',
    password: 'StrongPass!1',
    confirmPassword: 'StrongPass!1',
  };
  const res = await fetch(`${BACKEND_URL}/customers`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw new Error(`createCustomer falhou: ${res.status} ${await res.text()}`);
  const data = (await res.json()) as { id: string };
  return { id: data.id, email: payload.email, password: payload.password, fullName: payload.fullName };
}

/** Faz login do customer via UI e aguarda sessão no localStorage */
export async function loginCustomerViaUI(
  page: Page,
  email: string,
  password: string,
): Promise<void> {
  await page.goto('/login');
  await page.locator('input[type="email"]').waitFor({ state: 'visible' });
  await page.locator('input[type="email"]').fill(email);
  await page.locator('input[type="password"]').first().fill(password);
  await page.getByRole('button', { name: /^Entrar$/ }).click();
  await page.waitForURL(/\/$/, { timeout: 15_000 });
  await page.waitForFunction(() => localStorage.getItem('mg_customer_id') !== null, { timeout: 10_000 });
}

/** Cria endereço de entrega e cobrança via API */
export async function createAddressViaApi(
  customerId: string,
  type: 'DELIVERY' | 'BILLING' = 'DELIVERY',
): Promise<string> {
  const res = await fetch(`${BACKEND_URL}/customers/${customerId}/addresses`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      nickname: `${type} Test`,
      street: 'Rua dos Testes',
      number: '42',
      neighborhood: 'Centro',
      city: 'São Paulo',
      state: 'SP',
      zipCode: '01310100',
      type,
    }),
  });
  if (!res.ok) throw new Error(`createAddress(${type}) falhou: ${res.status} ${await res.text()}`);
  const data = (await res.json()) as { id: string };
  return data.id;
}

/** Cria cartão de crédito via API e retorna o ID */
export async function createCardViaApi(customerId: string, preferred = true): Promise<string> {
  const res = await fetch(`${BACKEND_URL}/customers/${customerId}/cards`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      cardholderName: 'Comprador Teste',
      cardNumber: '4111111111111111',
      brand: 'VISA',
      expirationMonth: 12,
      expirationYear: 2030,
      preferred,
    }),
  });
  if (!res.ok) throw new Error(`createCard falhou: ${res.status} ${await res.text()}`);
  const data = (await res.json()) as { id: string };
  return data.id;
}

/** Garante estoque suficiente para um livro via entrada manual de inventário */
async function restockBook(bookId: string, quantity = 50): Promise<void> {
  const res = await fetch(`${BACKEND_URL}/inventory/entries`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Admin-Key': ADMIN_KEY },
    body: JSON.stringify({ bookId, quantity, unitCost: 20.0, reason: 'ADJUSTMENT' }),
  });
  // Ignora erro — se falhar, o addToCart vai lançar a mensagem correta
  if (!res.ok) {
    console.warn(`restockBook falhou (não crítico): ${res.status} ${await res.text()}`);
  }
}

/** Busca ou cria um item de domínio (autor, editora, fornecedor) pelo nome */
async function getOrCreateDomain(
  listPath: string,
  createPath: string,
  name: string,
): Promise<string> {
  const listRes = await fetch(`${BACKEND_URL}${listPath}`);
  if (listRes.ok) {
    const items = (await listRes.json()) as Array<{ id: string; name: string }>;
    const found = items.find((i) => i.name === name);
    if (found) return found.id;
  }
  const createRes = await fetch(`${BACKEND_URL}${createPath}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  });
  if (!createRes.ok) throw new Error(`getOrCreateDomain(${createPath}) falhou: ${createRes.status} ${await createRes.text()}`);
  const data = (await createRes.json()) as { id: string };
  return data.id;
}

/** Garante que existe ao menos 1 livro em estoque e retorna seu ID */
export async function ensureBookInStock(): Promise<{ id: string; price: number }> {
  // Tenta encontrar livro ativo com estoque
  const listRes = await fetch(`${BACKEND_URL}/books?size=50&page=0&sort=id,asc`);
  if (listRes.ok) {
    const list = (await listRes.json()) as { content: Array<{ id: string; price: number; stockQuantity: number; active: boolean }> };
    const available = list.content.find((b) => b.active && b.stockQuantity > 0);
    if (available) {
      // Reabastece o livro encontrado para evitar falhas por reservas concorrentes
      await restockBook(available.id, 50);
      return { id: available.id, price: available.price };
    }
  }

  // Nenhum livro disponível — cria seed buscando IDs de domínio
  const authorId = await getOrCreateDomain('/domain/authors', '/authors', 'Autor Seed E2E');
  const publisherId = await getOrCreateDomain('/domain/publishers', '/publishers', 'Editora Seed E2E');
  const supplierId = await getOrCreateDomain('/domain/suppliers', '/suppliers', 'Fornecedor Seed E2E');

  const catRes = await fetch(`${BACKEND_URL}/domain/categories`);
  if (!catRes.ok) throw new Error(`Falha ao listar categorias: ${catRes.status}`);
  const categories = (await catRes.json()) as Array<{ id: string; name: string }>;
  if (categories.length === 0) throw new Error('Nenhuma categoria cadastrada no backend');
  const categoryId = categories[0].id;

  const pgRes = await fetch(`${BACKEND_URL}/domain/pricing-groups`);
  if (!pgRes.ok) throw new Error(`Falha ao listar grupos de precificação: ${pgRes.status}`);
  const pricingGroups = (await pgRes.json()) as Array<{ id: string; name: string }>;
  if (pricingGroups.length === 0) throw new Error('Nenhum grupo de precificação cadastrado no backend');
  const pricingGroupId = pricingGroups[0].id;

  const now = Date.now();
  const createRes = await fetch(`${BACKEND_URL}/books`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Admin-Key': ADMIN_KEY },
    body: JSON.stringify({
      title: `Livro Seed E2E ${now}`,
      authorId,
      publisherId,
      supplierId,
      categoryIds: [categoryId],
      pricingGroupId,
      publicationYear: 2020,
      edition: '1ª',
      pageCount: 200,
      synopsis: 'Livro criado automaticamente para testes E2E.',
      heightCm: 23,
      widthCm: 16,
      depthCm: 2,
      weightKg: 0.5,
      barcode: `E2E${now}`,
      price: 89.9,
      isbn: `978${rand(10)}`,
      stockQuantity: 99,
      active: true,
    }),
  });
  if (!createRes.ok) throw new Error(`ensureBook seed falhou: ${createRes.status} ${await createRes.text()}`);
  const book = (await createRes.json()) as { id: string; price: number };
  return book;
}

/** Adiciona livro ao carrinho via API */
export async function addBookToCartViaApi(customerId: string, bookId: string): Promise<void> {
  const res = await fetch(`${BACKEND_URL}/customers/${customerId}/cart/items`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ bookId, quantity: 1 }),
  });
  if (!res.ok) throw new Error(`addToCart falhou: ${res.status} ${await res.text()}`);
}

/** Define endereço de entrega no checkout via API */
export async function setCheckoutAddressViaApi(customerId: string, addressId: string): Promise<void> {
  const res = await fetch(`${BACKEND_URL}/customers/${customerId}/checkout/address`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ addressId }),
  });
  if (!res.ok) throw new Error(`setCheckoutAddress falhou: ${res.status} ${await res.text()}`);
}

/** Define pagamento por cartão no checkout via API */
export async function setCheckoutPaymentCardViaApi(
  customerId: string,
  creditCardId: string,
  amount: number,
): Promise<void> {
  const res = await fetch(`${BACKEND_URL}/customers/${customerId}/checkout/payment`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      lines: [{ paymentType: 'CREDIT_CARD', amount, creditCardId }],
    }),
  });
  if (!res.ok) throw new Error(`setCheckoutPayment falhou: ${res.status} ${await res.text()}`);
}

/** Finaliza o checkout via API e retorna o pedido criado */
export async function finalizeCheckoutViaApi(customerId: string): Promise<any> {
  const res = await fetch(`${BACKEND_URL}/customers/${customerId}/checkout/finalize`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });
  if (!res.ok) throw new Error(`finalizeCheckout falhou: ${res.status} ${await res.text()}`);
  return res.json();
}

/** Cria um pedido completo (do zero) e retorna o objeto Order */
export async function createFullOrder(customerId: string): Promise<any> {
  const book = await ensureBookInStock();
  await addBookToCartViaApi(customerId, book.id);

  const delivId = await createAddressViaApi(customerId, 'DELIVERY');
  await createAddressViaApi(customerId, 'BILLING');

  const freightRes = await fetch(`${BACKEND_URL}/customers/${customerId}/checkout/freight`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ addressId: delivId }),
  });
  const freight = freightRes.ok
    ? ((await freightRes.json()) as { grandTotal: number })
    : { grandTotal: book.price + 15 };

  await setCheckoutAddressViaApi(customerId, delivId);

  const cardId = await createCardViaApi(customerId, true);
  await setCheckoutPaymentCardViaApi(customerId, cardId, freight.grandTotal);

  return finalizeCheckoutViaApi(customerId);
}

/** Solicita troca de um item do pedido via API (como cliente) */
export async function requestExchangeViaApi(
  customerId: string,
  orderId: string,
  orderItemId: string,
): Promise<any> {
  const res = await fetch(`${BACKEND_URL}/customers/${customerId}/orders/${orderId}/exchange-requests`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ orderItemId }),
  });
  if (!res.ok) throw new Error(`requestExchange falhou: ${res.status} ${await res.text()}`);
  return res.json();
}
