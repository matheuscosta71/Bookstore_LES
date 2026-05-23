import type { Cart } from '@/types/api';
import { api } from './api';

export type UpsertItem = { bookId: string; quantity: number };

export async function getCart(customerId: string): Promise<Cart> {
  const { data } = await api.get<Cart>(`/customers/${customerId}/cart`);
  return data;
}

export async function addCartItem(customerId: string, body: UpsertItem): Promise<Cart> {
  const { data } = await api.post<Cart>(`/customers/${customerId}/cart/items`, body);
  return data;
}

export async function updateCartItem(
  customerId: string,
  itemId: string,
  body: UpsertItem,
): Promise<Cart> {
  const { data } = await api.put<Cart>(
    `/customers/${customerId}/cart/items/${itemId}`,
    body,
  );
  return data;
}

export async function removeCartItem(customerId: string, itemId: string): Promise<void> {
  await api.delete(`/customers/${customerId}/cart/items/${itemId}`);
}
