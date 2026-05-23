import type { ExchangeRequestResponse, Order } from '@/types/api';
import { api } from './api';

export async function fetchCustomerOrder(customerId: string, orderId: string): Promise<Order> {
  const { data } = await api.get<Order>(`/customers/${customerId}/orders/${orderId}`);
  return data;
}

export async function createExchangeRequest(
  customerId: string,
  orderId: string,
  body: { orderItemId: string },
): Promise<ExchangeRequestResponse> {
  const { data } = await api.post<ExchangeRequestResponse>(
    `/customers/${customerId}/orders/${orderId}/exchange-requests`,
    body,
  );
  return data;
}
