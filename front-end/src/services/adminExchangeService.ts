import type { ExchangeRequestResponse } from '@/types/api';
import { adminApi } from './api';

/** Status do pedido usado pelo backend para filtrar solicitações de troca (ex.: EM_TROCA, TROCA_AUTORIZADA). */
export type ExchangeListOrderStatus =
  | 'EM_PROCESSAMENTO'
  | 'APROVADO'
  | 'EM_TRANSITO'
  | 'ENTREGUE'
  | 'EM_TROCA'
  | 'TROCA_AUTORIZADA';

export async function listExchangeRequests(
  status: ExchangeListOrderStatus,
): Promise<ExchangeRequestResponse[]> {
  const { data } = await adminApi.get<ExchangeRequestResponse[]>('/admin/exchange-requests', {
    params: { status },
  });
  return data;
}

export async function authorizeExchange(exchangeRequestId: string): Promise<ExchangeRequestResponse> {
  const { data } = await adminApi.patch<ExchangeRequestResponse>(
    `/admin/exchange-requests/${exchangeRequestId}/authorize`,
  );
  return data;
}

export async function receiveExchange(
  exchangeRequestId: string,
  body: { returnToStock: boolean },
): Promise<ExchangeRequestResponse> {
  const { data } = await adminApi.patch<ExchangeRequestResponse>(
    `/admin/exchange-requests/${exchangeRequestId}/receive`,
    body,
  );
  return data;
}
