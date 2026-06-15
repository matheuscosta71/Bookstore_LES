

import { test, expect } from '@playwright/test';
import { createCustomerViaApi, createFullOrder } from '../helpers/setup';
import { adminLogin } from '../helpers/admin';

const BACKEND_URL = process.env.PW_BACKEND_URL ?? 'http://localhost:8080';
const ADMIN_KEY = process.env.VITE_ADMIN_KEY ?? '8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12';

test.describe('05 — Admin confirma/rejeita pagamento', () => {
  test('admin aprova pagamento via UI — status muda para APROVADO', async ({ page }) => {
    const customer = await createCustomerViaApi();
    const order = await createFullOrder(customer.id);

    await adminLogin(page);
    await page.goto('/admin/orders');
    await page.waitForLoadState('networkidle');

    
    const searchInput = page.locator('input').first();
    await searchInput.waitFor({ state: 'visible', timeout: 10_000 });
    const orderLabel = order.orderNumber ?? order.id.slice(0, 8);
    await searchInput.fill(orderLabel);

   
    await page.getByRole('button', { name: /^Buscar$/i }).click();
    await page.waitForLoadState('networkidle');

    
    await page.getByRole('button', { name: /^Ver$/i }).first().click();
    await page.waitForTimeout(50);

    
    const approveBtn = page.getByRole('button', { name: /Aprovar pagamento/i }).first();
    await approveBtn.waitFor({ state: 'visible', timeout: 15_000 });
    await approveBtn.click();

   
    await expect(
      page.locator('span').filter({ hasText: /Pagamento aprovado/i }).first()
    ).toBeVisible({ timeout: 10_000 });

  
    const ordersRes = await fetch(`${BACKEND_URL}/admin/orders?orderNumber=${order.orderNumber ?? ''}&size=50&page=0`, {
      headers: { 'X-Admin-Key': ADMIN_KEY },
    });
    const data = (await ordersRes.json()) as { content: Array<{ id: string; status: string }> };
    const updatedOrder = data.content.find((o) => o.id === order.id);
    expect(updatedOrder?.status).toBe('APROVADO');
  });

  test('admin rejeita pagamento — status muda para CANCELADO ou REJEITADO', async ({ page }) => {
    const customer = await createCustomerViaApi();
    const order = await createFullOrder(customer.id);

    await adminLogin(page);
    await page.goto('/admin/orders');
    await page.waitForLoadState('networkidle');

   
    const searchInput = page.locator('input').first();
    await searchInput.waitFor({ state: 'visible', timeout: 10_000 });
    const orderLabel = order.orderNumber ?? order.id.slice(0, 8);
    await searchInput.fill(orderLabel);

    
    await page.getByRole('button', { name: /^Buscar$/i }).click();
    await page.waitForLoadState('networkidle');

    
    await page.getByRole('button', { name: /^Ver$/i }).first().click();
    await page.waitForTimeout(50);

    
    const rejectBtn = page.getByRole('button', { name: /Rejeitar pagamento/i }).first();
    await rejectBtn.waitFor({ state: 'visible', timeout: 15_000 });
    await rejectBtn.click();

    
    await expect(
      page.locator('span').filter({ hasText: /Pagamento recusado|CANCELADO|REJEITADO/i }).first()
    ).toBeVisible({ timeout: 10_000 });
  });

  test('admin aprova pagamento via API — status confirmado', async () => {
    const customer = await createCustomerViaApi();
    const order = await createFullOrder(customer.id);

    expect(order.status).toBe('EM_PROCESSAMENTO');

    const res = await fetch(`${BACKEND_URL}/admin/orders/${order.id}/approve-payment`, {
      method: 'PATCH',
      headers: { 'X-Admin-Key': ADMIN_KEY },
    });
    expect(res.ok).toBe(true);
    const updated = (await res.json()) as { status: string };
    expect(updated.status).toBe('APROVADO');
  });
});
