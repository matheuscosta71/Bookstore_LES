import type { Customer, Page } from '@/types/api';
import { api } from './api';

export type CustomerListParams = {
  fullName?: string;
  email?: string;
  cpf?: string;
  phone?: string;
  /** Código interno do cliente (RF0024) */
  code?: string;
  birthDate?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
};

export async function listCustomers(params: CustomerListParams): Promise<Page<Customer>> {
  const { data } = await api.get<Page<Customer>>('/customers', {
    params: {
      fullName: params.fullName || undefined,
      email: params.email || undefined,
      cpf: params.cpf || undefined,
      phone: params.phone || undefined,
      code: params.code?.trim() || undefined,
      birthDate: params.birthDate || undefined,
      active: params.active,
      page: params.page ?? 0,
      size: params.size ?? 12,
      sort: params.sort,
    },
  });
  return data;
}

/** RF0022 — mesmo contrato de `PUT /customers/{id}`. */
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

/** RF0023 / reativação — `PATCH /customers/{id}/inactive` ou `.../active`. */
export async function setCustomerActive(id: string, active: boolean): Promise<Customer> {
  const path = active ? `/customers/${id}/active` : `/customers/${id}/inactive`;
  const { data } = await api.patch<Customer>(path);
  return data;
}
