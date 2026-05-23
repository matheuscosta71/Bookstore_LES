import { expect, type Page } from '@playwright/test';
import { Sel } from './selectors';

const BACKEND_URL = process.env.PW_BACKEND_URL ?? 'http://localhost:8080';
const ADMIN_KEY = process.env.VITE_ADMIN_KEY ?? '8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12';

export type AddressPayload = {
  nickname: string;
  street: string;
  number: string;
  complement?: string;
  neighborhood: string;
  city: string;
  state: string; 
  zipCode: string; 
};

export type CardPayload = {
  cardholderName: string;
  cardNumber: string; 
  brand: string;
  expirationMonth: number;
  expirationYear: number;
  preferred?: boolean;
};

export async function addAddressViaProfile(page: Page, payload: AddressPayload): Promise<void> {
  await page.goto('/profile/addresses');

  await page.locator(Sel.inputs.nickname).fill(payload.nickname);
  await page.locator(Sel.inputs.street).fill(payload.street);
  await page.locator(Sel.inputs.number).fill(payload.number);
  await page.locator(Sel.inputs.neighborhood).fill(payload.neighborhood);
  await page.locator(Sel.inputs.city).fill(payload.city);
  await page.locator(Sel.inputs.state).fill(payload.state);
  await page.locator(Sel.inputs.zipCode).fill(payload.zipCode);

  const complementInput = page.locator('input[name="complement"]');
  if (payload.complement && (await complementInput.count()) > 0) {
    await complementInput.fill(payload.complement);
  }

  await page.getByRole('button', { name: /Adicionar/i }).click();
  await expect(page.getByText(payload.nickname, { exact: true })).toBeVisible();
}

export async function addCardViaProfile(page: Page, payload: CardPayload): Promise<void> {
  await page.goto('/profile/cards');

  await page.locator(Sel.inputs.cardholderName).fill(payload.cardholderName);
  await page.locator(Sel.inputs.cardNumber).fill(payload.cardNumber);
  await page.locator(Sel.inputs.brand).fill(payload.brand);
  await page.locator(Sel.inputs.expirationMonth).fill(String(payload.expirationMonth));
  await page.locator(Sel.inputs.expirationYear).fill(String(payload.expirationYear));

  if (payload.preferred != null) {
    const preferred = page.locator('input[name="preferred"]');
    if (payload.preferred) await preferred.check();
    else await preferred.uncheck();
  }

  await page.getByRole('button', { name: /Adicionar cartão/i }).click();
  await expect(page.getByText(payload.cardholderName, { exact: false })).toBeVisible();
  await expect(page.getByText(`${payload.expirationMonth}/${payload.expirationYear}`, { exact: false })).toBeVisible();
}


async function restockBook(bookId: string, quantity = 300): Promise<void> {
  await fetch(`${BACKEND_URL}/inventory/entries`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Admin-Key': ADMIN_KEY },
    body: JSON.stringify({ bookId, quantity, unitCost: 20.0, reason: 'ADJUSTMENT' }),
  });
}

export async function addFirstBookToCartViaUI(page: Page, maxAttempts = 5): Promise<void> {
  // Busca livros disponíveis e reabastece o primeiro encontrado
  const listRes = await page.request.get(`${BACKEND_URL}/books?size=50&page=0&sort=id,asc`);
  if (listRes.ok) {
    const listJson = (await listRes.json()) as { content?: Array<{ id?: string; active?: boolean; stockQuantity?: number }> };
    const available = listJson.content?.find((b) => b.active && (b.stockQuantity ?? 0) > 0);
    if (available?.id) {
      await restockBook(available.id, 50);
    }
  }

  await page.waitForFunction(
    () => localStorage.getItem('mg_customer_id') !== null,
    { timeout: 10_000 }
  );

  await page.goto('/books');

  for (let i = 0; i < maxAttempts; i++) {
    await page.waitForSelector('a:has-text("Ver detalhes")', { timeout: 15_000 });
    const detailsLinks = page.locator('a:has-text("Ver detalhes")');
    const count = await detailsLinks.count();

    if (count === 0) throw new Error('Nenhum livro encontrado no catálogo');
    if (i >= count) throw new Error('Não foi possível encontrar um livro adicionável ao carrinho');

    const href = await detailsLinks.nth(i).getAttribute('href');
    if (!href) {
      await page.goto('/books');
      continue;
    }

    await page.goto(href);
    await page.waitForLoadState('domcontentloaded', { timeout: 10_000 }).catch(() => {});

    if (page.isClosed()) {
      await page.goto('/books');
      continue;
    }

    const addBtn = page.locator('button', { hasText: /Adicionar ao carrinho/i });
    await addBtn.waitFor({ state: 'attached', timeout: 5_000 }).catch(() => {});

    const btnCount = await addBtn.count();
    if (btnCount > 0 && await addBtn.first().isEnabled()) {
  await addBtn.first().click();

  // Aguarda resultado: sucesso (redireciona) ou erro de estoque
  await page.waitForTimeout(2000);

  const estoqueInsuficiente = page.getByText(/Estoque insuficiente/i);
  if (await estoqueInsuficiente.isVisible()) {
    // Tenta o próximo livro
    await page.goto('/books');
    continue;
  }

  await page.waitForURL('/cart', { timeout: 15_000 }).catch(async () => {
    await page.goto('/cart');
  });
  await expect(page.getByRole('heading', { name: /Carrinho de compras/i })).toBeVisible({ timeout: 10_000 });
  return;
}

    await page.goto('/books');
  }

  throw new Error('Falha ao adicionar livro ao carrinho após tentativas');
}

export async function checkoutAndFinalize(page: Page): Promise<void> {
  await page.getByRole('link', { name: /Ir para checkout/i }).click();

await page.waitForURL(/\/checkout/, {
  timeout: 15000,
});

await page.waitForTimeout(2000);


  const deliveryRadio = page.locator('input[type="radio"]').first();

  await deliveryRadio.waitFor({
    state: 'visible',
    timeout: 10000,
  });

  if (!(await deliveryRadio.isChecked())) {
    await deliveryRadio.check({ force: true });
  }


  const calcularFreteBtn = page.getByRole('button', {
    name: /Calcular frete/i,
  });

  await calcularFreteBtn.waitFor({
    state: 'visible',
    timeout: 10000,
  });

  await calcularFreteBtn.scrollIntoViewIfNeeded();

  await expect(calcularFreteBtn).toBeEnabled({
    timeout: 10000,
  });

  await calcularFreteBtn.click({ force: true });

  
  await page.waitForTimeout(3000);


  const finalizarBtn = page.getByRole('button', {
    name: /Finalizar compra/i,
  });

  await finalizarBtn.waitFor({
    state: 'visible',
    timeout: 15000,
  });

  await finalizarBtn.scrollIntoViewIfNeeded();

  await expect(finalizarBtn).toBeEnabled({
    timeout: 15000,
  });

  await finalizarBtn.click({ force: true });

 
  await expect(
    page.getByRole('heading', {
      name: Sel.headings.profileOrders,
    })
  ).toBeVisible({
    timeout: 15000,
  });

  await expect(
    page.getByText(/Status:/)
  ).toBeVisible();

 await expect(
  page.getByText(/Em processamento/i)
).toBeVisible();


}
