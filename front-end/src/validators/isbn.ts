/** Remove hífens e espaços; mantém apenas dígitos e X final (ISBN-10). */
export function normalizeIsbnInput(isbn: string): string {
  return isbn.trim().toUpperCase().replace(/[-\s]/g, '');
}

function isValidIsbn10(s: string): boolean {
  if (!/^\d{9}[\dX]$/.test(s)) return false;
  let sum = 0;
  for (let i = 0; i < 9; i += 1) {
    sum += (10 - i) * Number(s[i]);
  }
  const check = s[9] === 'X' ? 10 : Number(s[9]);
  return (sum + check) % 11 === 0;
}

function isValidIsbn13(s: string): boolean {
  if (!/^\d{13}$/.test(s)) return false;
  let sum = 0;
  for (let i = 0; i < 12; i += 1) {
    const mult = i % 2 === 0 ? 1 : 3;
    sum += mult * Number(s[i]);
  }
  const check = (10 - (sum % 10)) % 10;
  return check === Number(s[12]);
}

/**
 * Aceita ISBN-10 ou ISBN-13 (após normalizar hífens).
 * Não chama API externa — apenas dígitos de verificação.
 */
export function isValidIsbn(isbn: string): boolean {
  const s = normalizeIsbnInput(isbn);
  if (s.length === 10) return isValidIsbn10(s);
  if (s.length === 13) return isValidIsbn13(s);
  return false;
}
