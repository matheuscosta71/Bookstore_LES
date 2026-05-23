import { expect, type Locator, type Page } from '@playwright/test';

export async function expectAnyTextVisible(page: Page, text: RegExp | string): Promise<void> {
  // Works for both success and error texts.
  await expect(page.getByText(text, { exact: false })).toBeVisible();
}

export async function expectErrorText(page: Page, text: RegExp | string): Promise<void> {
  // Many error blocks are rendered as <p> with Tailwind "text-red-600".
  const candidates = page.locator('p.text-red-600, p.text-red-800, div:has-text("Erro")');
  const filtered = candidates.filter({ hasText: text });
  await expect(filtered.first()).toBeVisible();
}

export function byHeading(name: RegExp | string) {
  return { role: 'heading' as const, name };
}

export async function expectListHasAtLeast(page: Page, listSelector: string, min: number): Promise<void> {
  const items = page.locator(`${listSelector} > *`);
  await expect(items).toHaveCount(Math.max(min, 1));
}

export async function expectVisible(locator: Locator): Promise<void> {
  await expect(locator).toBeVisible();
}

