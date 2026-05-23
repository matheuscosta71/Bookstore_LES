import type { FreightResponse, Order } from '@/types/api';
import { api } from './api';

export async function postFreight(
  customerId: string,
  addressId: string,
): Promise<FreightResponse> {
  const { data } = await api.post<FreightResponse>(
    `/customers/${customerId}/checkout/freight`,
    { addressId },
  );
  return data;
}

export type CheckoutAddressBody = {
  addressId?: string;
  newAddress?: AddressCreate;
  saveToProfile?: boolean;
};

export type AddressCreate = {
  nickname: string;
  street: string;
  number: string;
  complement?: string | null;
  neighborhood: string;
  city: string;
  state: string;
  zipCode: string;
  type?: string;
};

export async function postCheckoutAddress(
  customerId: string,
  body: CheckoutAddressBody,
): Promise<void> {
  await api.post(`/customers/${customerId}/checkout/address`, body);
}

export type PaymentLine = {
  paymentType: 'CREDIT_CARD' | 'EXCHANGE_COUPON' | 'PROMOTIONAL_COUPON';
  amount: number;
  creditCardId?: string;
  couponCode?: string;
};

export type NewCreditCardPayload = {
  cardholderName: string;
  cardNumber: string;
  brand: string;
  expirationMonth: number;
  expirationYear: number;
  preferred?: boolean;
};

export type CheckoutPaymentBody = {
  lines: PaymentLine[];
  newCreditCard?: NewCreditCardPayload;
  saveNewCardToProfile?: boolean;
};

export async function postCheckoutPayment(
  customerId: string,
  body: CheckoutPaymentBody,
): Promise<void> {
  await api.post(`/customers/${customerId}/checkout/payment`, body);
}

export async function finalizeCheckout(customerId: string): Promise<Order> {
  const { data } = await api.post<Order>(`/customers/${customerId}/checkout/finalize`);
  return data;
}

export type CouponValidateBody = {
  code: string;
  paymentType: 'EXCHANGE_COUPON' | 'PROMOTIONAL_COUPON';
};

export type CouponValidateResponse = {
  amount: number;
};

/** Valida cupom no checkout e retorna o valor fixo (para montar `lines.amount`). */
export async function validateCheckoutCoupon(
  customerId: string,
  body: CouponValidateBody,
): Promise<CouponValidateResponse> {
  const { data } = await api.post<CouponValidateResponse>(
    `/customers/${customerId}/checkout/coupon/validate`,
    body,
  );
  return data;
}
