import type { ValidationResult } from './types';

/** Valor mínimo por linha de cartão de crédito (R$). */
export const MIN_CREDIT_CARD_LINE_BRL = 10;

function moneyToCents(n: number): number {
  return Math.round((n + Number.EPSILON) * 100);
}

export type CheckoutPaymentCheckInput = {
  grandTotal: number;
  couponAmount: number;
  requiresCreditCard: boolean;
  creditCardLineAmount: number;
};

export function validateCheckoutPaymentAmounts(input: CheckoutPaymentCheckInput): ValidationResult {
  const errors: Record<string, string> = {};

  const grandCents = moneyToCents(input.grandTotal);
  const couponCents = moneyToCents(input.couponAmount);
  const cardCents = moneyToCents(input.creditCardLineAmount);

  const hasCoupon = couponCents > 0;
  /** RN0035: com cupom + uma linha de cartão, o restante no cartão pode ser menor que R$ 10 */
  const allowCardBelowMin = hasCoupon && input.requiresCreditCard;

  if (input.requiresCreditCard && !allowCardBelowMin) {
    if (cardCents < moneyToCents(MIN_CREDIT_CARD_LINE_BRL)) {
      errors.payment = `O valor mínimo para pagamento com cartão é R$ ${MIN_CREDIT_CARD_LINE_BRL.toFixed(2).replace('.', ',')}. Ajuste o carrinho ou o cupom.`;
    }
  }

  const expectedCard = Math.max(0, grandCents - couponCents);
  if (input.requiresCreditCard && Math.abs(cardCents - expectedCard) > 1) {
    errors.payment = 'Inconsistência no total a pagar no cartão. Recalcule o frete ou reaplique o cupom.';
  }

  return { valid: Object.keys(errors).length === 0, errors };
}
