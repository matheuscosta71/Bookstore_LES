/** Erros por campo (chave = nome do campo em camelCase). */
export type FieldErrors = Record<string, string>;

export type ValidationResult = {
  valid: boolean;
  errors: FieldErrors;
};
