import type { Address, CreditCard, Customer, Order } from '@/types/api';
import { api } from './api';

export async function updateCustomer(
  id: string,
  body: {
    fullName: string;
    email: string;
    cpf: string;
    phone: string;
    birthDate: string;
    active: boolean;
  },
): Promise<Customer> {
  const { data } = await api.put<Customer>(`/customers/${id}`, body);
  return data;
}

export async function changePassword(id: string, newPassword: string): Promise<void> {
  await api.patch(`/customers/${id}/password`, { newPassword });
}

export async function listAddresses(customerId: string): Promise<Address[]> {
  const { data } = await api.get<Address[]>(`/customers/${customerId}/addresses`);
  return data;
}

export async function createAddress(
  customerId: string,
  body: Record<string, unknown>,
): Promise<Address> {
  const { data } = await api.post<Address>(`/customers/${customerId}/addresses`, body);
  return data;
}

export async function updateAddress(
  customerId: string,
  addressId: string,
  body: Record<string, unknown>,
): Promise<Address> {
  const { data } = await api.put<Address>(
    `/customers/${customerId}/addresses/${addressId}`,
    body,
  );
  return data;
}

export async function inactivateAddress(customerId: string, addressId: string): Promise<Address> {
  const { data } = await api.patch<Address>(
    `/customers/${customerId}/addresses/${addressId}/inactive`,
  );
  return data;
}

export async function listCards(customerId: string): Promise<CreditCard[]> {
  const { data } = await api.get<CreditCard[]>(`/customers/${customerId}/cards`);
  return data;
}

export async function createCard(
  customerId: string,
  body: Record<string, unknown>,
): Promise<CreditCard> {
  const { data } = await api.post<CreditCard>(`/customers/${customerId}/cards`, body);
  return data;
}

export async function inactivateCard(customerId: string, cardId: string): Promise<CreditCard> {
  const { data } = await api.patch<CreditCard>(`/customers/${customerId}/cards/${cardId}/inactive`);
  return data;
}

export async function listOrders(customerId: string): Promise<Order[]> {
  const { data } = await api.get<Order[]>(`/customers/${customerId}/orders`);
  return data;
}

export type TransactionRow = {
  id: string;
  description?: string;
  amount?: number;
  transactionDate?: string;
  type?: string;
};

export async function listTransactions(customerId: string): Promise<TransactionRow[]> {
  const { data } = await api.get<TransactionRow[]>(`/customers/${customerId}/transactions`);
  return data;
}
