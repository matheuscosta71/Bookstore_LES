import { test, expect } from '@playwright/test';

test('smoke: can open register page', async ({ page }) => {
  await page.goto('/register');

  await expect(page.getByRole('heading', { name: /Criar conta/i })).toBeVisible();
  await expect(page.locator('input[name="fullName"]')).toBeVisible();
  await expect(page.locator('input[name="email"]')).toBeVisible();
  await expect(page.locator('input[name="cpf"]')).toBeVisible();
  await expect(page.getByRole('button', { name: /Cadastrar/i })).toBeVisible();
});

