import type { InventoryBookRow, InventoryMovementRow, Page } from '@/types/api';
import { adminApi } from './api';

export type InventoryEntryBody = {
  bookId: string;
  quantity: number;
  unitCost: number;
  reason?: 'PURCHASE' | 'ADJUSTMENT' | 'OTHER';
};

export async function postInventoryEntry(body: InventoryEntryBody): Promise<void> {
  await adminApi.post('/inventory/entries', body);
}

export async function getInventoryByBook(bookId: string): Promise<InventoryBookRow> {
  const { data } = await adminApi.get<InventoryBookRow>(`/inventory/books/${bookId}`);
  return data;
}

export type MovementListParams = {
  bookId?: string;
  movementType?: 'ENTRY' | 'SALE_OUTBOUND' | 'EXCHANGE_RETURN';
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
};

export async function listInventoryMovements(
  params: MovementListParams,
): Promise<Page<InventoryMovementRow>> {
  const { data } = await adminApi.get<Page<InventoryMovementRow>>('/inventory/movements', {
    params: {
      bookId: params.bookId || undefined,
      movementType: params.movementType || undefined,
      startDate: params.startDate || undefined,
      endDate: params.endDate || undefined,
      page: params.page ?? 0,
      size: params.size ?? 20,
    },
  });
  return data;
}

export async function postSalesOutbound(orderId: string): Promise<void> {
  await adminApi.post('/inventory/sales-outbound', { orderId });
}

export async function postExchangeReentry(exchangeRequestId: string): Promise<void> {
  await adminApi.post('/inventory/reentries/exchange', { exchangeRequestId });
}
