import { adminApi } from './api';

export type CouponType = 'EXCHANGE' | 'PROMOTIONAL';

export type CreateCouponPayload = {
  code: string;
  type: CouponType;
  amount: number;
  expirationDate?: string | null;
  customerId?: string | null;
};

export type CouponResponse = {
  id: string;
  code: string;
  type: CouponType;
  amount: number;
  active: boolean;
  expirationDate: string | null;
  redeemed: boolean;
  customerId: string | null;
  customerName: string | null;
};

export type PageCouponResponse = {
  content: CouponResponse[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
};

export async function listCoupons(page = 0, size = 20): Promise<PageCouponResponse> {
  const { data } = await adminApi.get<PageCouponResponse>('/admin/coupons', {
    params: { page, size },
  });
  return data;
}

export async function createCoupon(payload: CreateCouponPayload): Promise<CouponResponse> {
  const { data } = await adminApi.post<CouponResponse>('/admin/coupons', payload);
  return data;
}

export async function toggleCouponActive(couponId: string): Promise<CouponResponse> {
  const { data } = await adminApi.patch<CouponResponse>(`/admin/coupons/${couponId}/toggle-active`);
  return data;
}
