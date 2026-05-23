import type { Book } from '@/types/api';
import { adminApi, api } from './api';

export type BookCreateBody = {
  title: string;
  authorId: string;
  publisherId: string;
  supplierId: string;
  categoryIds: string[];
  publicationYear: number;
  edition: string;
  pageCount: number;
  synopsis: string;
  heightCm: number;
  widthCm: number;
  depthCm: number;
  weightKg: number;
  barcode: string;
  price: number;
  costPrice?: number;
  pricingGroupId: string;
  isbn: string;
  maxSaleValue?: number;
  stockQuantity: number;
  active?: boolean;
};

export type BookUpdateBody = BookCreateBody & {
  active: boolean;
};

/** Motivos manuais (RN0015/RN0017); FORA_DE_MERCADO é só inativação automática no servidor */
export type BookLifecycleReasonCode =
  | 'BAIXA_ROTACAO'
  | 'CONTEUDO_DESATUALIZADO'
  | 'OUTRA_INATIVACAO'
  | 'RETORNO_ESTOQUE'
  | 'DEMANDA_RENOVADA'
  | 'OUTRA_ATIVACAO';

export type PatchBookLifecycleBody = {
  active: boolean;
  justification?: string;
  reason?: BookLifecycleReasonCode;
};

export async function createBook(body: BookCreateBody): Promise<Book> {
  const { data } = await api.post<Book>('/books', body);
  return data;
}

export async function updateBook(id: string, body: BookUpdateBody, salesManagerKey?: string): Promise<Book> {
  const { data } = await api.put<Book>(`/books/${id}`, body, {
    headers: salesManagerKey ? { 'X-Sales-Manager-Key': salesManagerKey } : undefined,
  });
  return data;
}

export async function deleteBook(id: string): Promise<void> {
  await api.delete(`/books/${id}`);
}

export async function patchBookActive(id: string, body: PatchBookLifecycleBody): Promise<Book> {
  const { data } = await api.patch<Book>(`/books/${id}/active`, body);
  return data;
}

export async function postInactivateAutomatic(minimumSalesValue: number): Promise<void> {
  await api.post('/books/inactivate-automatic', { minimumSalesValue });
}

export async function recalculateSalePrice(bookId: string): Promise<Book> {
  const { data } = await adminApi.post<Book>(`/books/${bookId}/recalculate-sale-price`);
  return data;
}
