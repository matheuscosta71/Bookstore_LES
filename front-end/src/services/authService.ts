import type { Customer } from '@/types/api';
import { isAxiosError } from 'axios';
import { api } from './api';
import {
  STORAGE_ADMIN_ROLE,
  STORAGE_ADMIN_TOKEN,
  STORAGE_ADMIN_USERNAME,
  STORAGE_CUSTOMER_EMAIL,
  STORAGE_CUSTOMER_ID,
  STORAGE_CUSTOMER_ROLE,
  STORAGE_CUSTOMER_TOKEN,
} from '@/constants/storageKeys';

export type RegisterPayload = {
  fullName: string;
  email: string;
  cpf: string;
  phone: string;
  birthDate: string;
  password: string;
  confirmPassword: string;
  active?: boolean;
};

export type CustomerLoginApiResponse = {
  authenticated: boolean;
  accessToken: string;
  tokenType: string;
  customerId: string;
  fullName: string;
  email: string;
  role?: string;
};

export async function registerCustomer(payload: RegisterPayload): Promise<Customer> {
  const { data } = await api.post<Customer>('/customers', {
    ...payload,
    active: payload.active ?? true,
  });
  return data;
}

export async function loginCustomer(email: string, password: string): Promise<CustomerLoginApiResponse> {
  try {
    const { data } = await api.post<CustomerLoginApiResponse>('/auth/login', { email, password });
    if (!data.accessToken || !data.customerId) {
      throw new Error('Resposta de login inválida.');
    }
    localStorage.setItem(STORAGE_CUSTOMER_TOKEN, data.accessToken);
    localStorage.setItem(STORAGE_CUSTOMER_ID, data.customerId);
    localStorage.setItem(STORAGE_CUSTOMER_ROLE, data.role ?? 'CLIENT');
    return data;
  } catch (e) {
    if (e instanceof Error) throw e;
    if (isAxiosError(e) && e.response?.status === 401) {
      throw new Error('E-mail ou senha inválidos.');
    }
    throw e;
  }
}

export function clearCustomerSessionStorage(): void {
  localStorage.removeItem(STORAGE_CUSTOMER_ID);
  localStorage.removeItem(STORAGE_CUSTOMER_TOKEN);
  localStorage.removeItem(STORAGE_CUSTOMER_ROLE);
  localStorage.removeItem(STORAGE_CUSTOMER_EMAIL);
}

export async function findCustomerByEmail(email: string): Promise<Customer | null> {
  const { data } = await api.get<{ content: Customer[] }>('/customers', {
    params: { email, size: 1, page: 0 },
  });
  const first = data.content?.[0];
  return first ?? null;
}

export async function fetchCustomer(id: string): Promise<Customer> {
  const { data } = await api.get<Customer>(`/customers/${id}`);
  return data;
}

type AdminLoginResponse = {
  accessToken?: string;
  token?: string;
  role?: string;
  username?: string;
};

export async function loginAdmin(username: string, password: string): Promise<{ accessToken: string; role?: string }> {
  try {
    const { data } = await api.post<AdminLoginResponse>('/auth/admin/login', { username, password });
    const accessToken = data.accessToken ?? data.token;
    if (!accessToken) throw new Error('Resposta de login admin sem token.');
    localStorage.setItem(STORAGE_ADMIN_TOKEN, accessToken);
    localStorage.setItem(STORAGE_ADMIN_USERNAME, data.username ?? username);
    if (data.role) localStorage.setItem(STORAGE_ADMIN_ROLE, data.role);
    return { accessToken, role: data.role };
  } catch (e) {
    if (isAxiosError(e) && e.response?.status === 401) {
      throw new Error('Usuário ou senha inválidos.');
    }
    throw e;
  }
}
