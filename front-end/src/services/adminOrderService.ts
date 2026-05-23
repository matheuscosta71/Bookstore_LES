import type { Order, Page } from '@/types/api';
import { adminApi } from './api';

export type AdminOrderListParams = {
  page?: number;
  size?: number;
  orderNumber?: string;
  customerName?: string;
  status?: string;
  /** yyyy-MM-dd */
  dateFrom?: string;
  dateTo?: string;
  totalMin?: number;
  totalMax?: number;
};

export async function listOrders(params?: AdminOrderListParams): Promise<Page<Order>> {
  const q: Record<string, string | number> = {
    page: params?.page ?? 0,
    size: params?.size ?? 50,
  };
  if (params?.orderNumber?.trim()) q.orderNumber = params.orderNumber.trim();
  if (params?.customerName?.trim()) q.customerName = params.customerName.trim();
  if (params?.status?.trim()) q.status = params.status.trim();
  if (params?.dateFrom) q.dateFrom = params.dateFrom;
  if (params?.dateTo) q.dateTo = params.dateTo;
  if (params?.totalMin != null && !Number.isNaN(params.totalMin)) q.totalMin = params.totalMin;
  if (params?.totalMax != null && !Number.isNaN(params.totalMax)) q.totalMax = params.totalMax;

  const { data } = await adminApi.get<Page<Order>>('/admin/orders', { params: q });
  return data;
}

export async function approvePayment(orderId: string): Promise<Order> {
  const { data } = await adminApi.patch<Order>(`/admin/orders/${orderId}/approve-payment`);
  return data;
}

export async function rejectPayment(orderId: string): Promise<Order> {
  const { data } = await adminApi.patch<Order>(`/admin/orders/${orderId}/reject-payment`);
  return data;
}

export async function dispatchOrder(orderId: string): Promise<Order> {
  const { data } = await adminApi.patch<Order>(`/admin/orders/${orderId}/dispatch`);
  return data;
}

export async function deliverOrder(orderId: string): Promise<Order> {
  const { data } = await adminApi.patch<Order>(`/admin/orders/${orderId}/deliver`);
  return data;
}
