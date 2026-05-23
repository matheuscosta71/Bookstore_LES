import type { ValidationResult } from './types';
import { isValidIsbn, normalizeIsbnInput } from './isbn';

/** @deprecated use AdminBookExtendedInput */
export type AdminBookFormInput = {
  title: string;
  author: string;
  category: string;
  isbn: string;
  price: number;
  stockQuantity: number;
  maxSaleValue: number | '';
  active: boolean;
};

export type AdminBookExtendedInput = {
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
  costPrice: number | '';
  pricingGroupId: string;
  isbn: string;
  maxSaleValue: number | '';
  stockQuantity: number;
  active: boolean;
};

function isFinitePositivePrice(n: number): boolean {
  return Number.isFinite(n) && n > 0;
}

function isNonNegativeInt(n: number): boolean {
  return Number.isInteger(n) && n >= 0;
}

/**
 * Validação de livro (admin) antes de POST/PUT — RN0011+.
 */
export function validateAdminBookExtended(data: AdminBookExtendedInput): ValidationResult {
  const errors: Record<string, string> = {};

  if (!data.title?.trim()) errors.title = 'Título obrigatório';
  if (!data.authorId) errors.authorId = 'Selecione o autor';
  if (!data.publisherId) errors.publisherId = 'Selecione a editora';
  if (!data.supplierId) errors.supplierId = 'Selecione o fornecedor';
  if (!data.categoryIds?.length) errors.categoryIds = 'Selecione ao menos uma categoria';
  if (!Number.isFinite(data.publicationYear) || data.publicationYear < 1000 || data.publicationYear > 2100) {
    errors.publicationYear = 'Ano inválido';
  }
  if (!data.edition?.trim()) errors.edition = 'Edição obrigatória';
  if (!Number.isFinite(data.pageCount) || data.pageCount < 1) errors.pageCount = 'Número de páginas inválido';
  if (!data.synopsis?.trim()) errors.synopsis = 'Sinopse obrigatória';
  if (!Number.isFinite(data.heightCm) || data.heightCm <= 0) errors.heightCm = 'Altura inválida';
  if (!Number.isFinite(data.widthCm) || data.widthCm <= 0) errors.widthCm = 'Largura inválida';
  if (!Number.isFinite(data.depthCm) || data.depthCm <= 0) errors.depthCm = 'Profundidade inválida';
  if (!Number.isFinite(data.weightKg) || data.weightKg <= 0) errors.weightKg = 'Peso inválido';
  if (!data.barcode?.trim()) errors.barcode = 'Código de barras obrigatório';
  if (!data.pricingGroupId) errors.pricingGroupId = 'Selecione o grupo de precificação';

  const isbnRaw = data.isbn?.trim() ?? '';
  if (!isbnRaw) {
    errors.isbn = 'ISBN obrigatório';
  } else {
    const norm = normalizeIsbnInput(isbnRaw);
    if (norm.length !== 10 && norm.length !== 13) {
      errors.isbn = 'ISBN deve ter 10 ou 13 dígitos (após remover hífens)';
    } else if (!isValidIsbn(isbnRaw)) {
      errors.isbn = 'ISBN inválido (dígito verificador incorreto)';
    }
  }

  if (!isFinitePositivePrice(data.price)) {
    errors.price = 'Informe um preço maior que zero';
  }

  if (data.costPrice !== '' && data.costPrice !== undefined) {
    const c = Number(data.costPrice);
    if (!Number.isFinite(c) || c < 0) errors.costPrice = 'Custo inválido';
  }

  if (!isNonNegativeInt(data.stockQuantity)) {
    errors.stockQuantity = 'Estoque deve ser um número inteiro maior ou igual a zero';
  }

  if (data.maxSaleValue !== '' && data.maxSaleValue !== undefined) {
    const cap = Number(data.maxSaleValue);
    if (!Number.isFinite(cap) || cap < 0) {
      errors.maxSaleValue = 'Teto de preço inválido';
    }
  }

  return { valid: Object.keys(errors).length === 0, errors };
}

/** @deprecated use validateAdminBookExtended */
export function validateBookForm(data: AdminBookFormInput): ValidationResult {
  const errors: Record<string, string> = {};

  const title = data.title?.trim() ?? '';
  if (!title) errors.title = 'Título obrigatório';

  const author = data.author?.trim() ?? '';
  if (!author) errors.author = 'Autor obrigatório';

  const category = data.category?.trim() ?? '';
  if (!category) errors.category = 'Categoria obrigatória';

  const isbnRaw = data.isbn?.trim() ?? '';
  if (!isbnRaw) {
    errors.isbn = 'ISBN obrigatório';
  } else {
    const norm = normalizeIsbnInput(isbnRaw);
    if (norm.length !== 10 && norm.length !== 13) {
      errors.isbn = 'ISBN deve ter 10 ou 13 dígitos (após remover hífens)';
    } else if (!isValidIsbn(isbnRaw)) {
      errors.isbn = 'ISBN inválido (dígito verificador incorreto)';
    }
  }

  if (!isFinitePositivePrice(data.price)) {
    errors.price = 'Informe um preço maior que zero';
  }

  if (!isNonNegativeInt(data.stockQuantity)) {
    errors.stockQuantity = 'Estoque deve ser um número inteiro maior ou igual a zero';
  }

  if (data.maxSaleValue !== '' && data.maxSaleValue !== undefined) {
    const cap = Number(data.maxSaleValue);
    if (!Number.isFinite(cap) || cap < 0) {
      errors.maxSaleValue = 'Teto de preço inválido';
    }
  }

  return { valid: Object.keys(errors).length === 0, errors };
}
