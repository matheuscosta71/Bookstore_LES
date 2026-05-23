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
  creditCardLineAmounts: number[];
};

export function validateCheckoutPaymentAmounts(input: CheckoutPaymentCheckInput): ValidationResult {
  const errors: Record<string, string> = {};

  const grandCents = moneyToCents(input.grandTotal);
  const couponCents = moneyToCents(input.couponAmount);
  const cardCentsSum = input.creditCardLineAmounts.reduce((acc, a) => acc + moneyToCents(a), 0);

  const hasCoupon = couponCents > 0;
  const cardLineCount = input.creditCardLineAmounts.length;

  if (input.requiresCreditCard) {
    if (cardLineCount === 0) {
      errors.payment = 'Selecione pelo menos um cartão de crédito para realizar o pagamento.';
    } else {
      /** RN0035: com cupom + uma linha de cartão, o restante no cartão pode ser menor que R$ 10 */
      const allowCardBelowMin = hasCoupon && cardLineCount === 1;

      if (!allowCardBelowMin) {
        for (let i = 0; i < cardLineCount; i++) {
          const amtCents = moneyToCents(input.creditCardLineAmounts[i]);
          if (amtCents < moneyToCents(MIN_CREDIT_CARD_LINE_BRL)) {
            errors.payment = `Cada cartão deve ter o valor mínimo de R$ ${MIN_CREDIT_CARD_LINE_BRL.toFixed(2).replace('.', ',')}.`;
            break;
          }
        }
      }
    }
  }

  const expectedCard = Math.max(0, grandCents - couponCents);
  if (input.requiresCreditCard && Math.abs(cardCentsSum - expectedCard) > 1) {
    errors.payment = 'Inconsistência no total a pagar nos cartões. O total dos cartões deve cobrir exatamente o valor restante do pedido.';
  }

  return { valid: Object.keys(errors).length === 0, errors };
}

