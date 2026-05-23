import type { ValidationResult } from './types';

const MAX_QTY = 999;

export type CartLineContext = {
  quantity: number;
  availableStock: number;
};

export function validateCartLineQuantity(ctx: CartLineContext): ValidationResult {
  const errors: Record<string, string> = {};
  const { quantity, availableStock } = ctx;

  if (!Number.isInteger(quantity) || quantity < 1) {
    errors.quantity = 'Quantidade mínima é 1';
  } else if (availableStock <= 0) {
    errors.quantity = 'Produto indisponível em estoque';
  } else if (quantity > availableStock) {
    errors.quantity = `Quantidade indisponível: há apenas ${availableStock} unidade(s) em estoque`;
  } else if (quantity > MAX_QTY) {
    errors.quantity = `Quantidade máxima por linha: ${MAX_QTY}`;
  }

  return { valid: Object.keys(errors).length === 0, errors };
}

export function validateAddToCartQuantity(quantity: number, availableStock: number): ValidationResult {
  return validateCartLineQuantity({ quantity, availableStock });
}
