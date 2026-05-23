import { test, expect } from '@playwright/test';

test.describe('RF0055 / dashboard admin', () => {
  test('carrega resumo sem erro e mostra pedidos > 0', async ({ page }) => {
    await page.goto('/admin/analytics');

    await expect(page.getByRole('heading', { name: /Dashboard de vendas/i })).toBeVisible();
    await expect(page.getByText(/Chave administrativa inválida/i)).toHaveCount(0);
    await expect(page.getByText(/Receita total/i)).toBeVisible({ timeout: 25_000 });

    const pedidosCard = page.locator('.rounded-xl.border').filter({ hasText: 'Pedidos' });
    await expect(pedidosCard.locator('p.text-2xl.font-semibold')).not.toHaveText('0');
  });
});
