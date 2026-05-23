/** Open Library Covers API — sem chave; use ISBN-13 sem hífens na URL. */
const COVER_BASE = 'https://covers.openlibrary.org/b/isbn';

export function normalizeIsbnForCover(isbn: string): string {
  return isbn.replace(/[-\s]/g, '');
}

export function openLibraryCoverUrl(isbn: string, size: 'S' | 'M' | 'L' = 'L'): string {
  return `${COVER_BASE}/${normalizeIsbnForCover(isbn)}-${size}.jpg`;
}

/** Sem capa → 404 (útil para encadear fallback). Com `default=true` a API devolve GIF 1×1. */
export function openLibraryCoverUrlDefaultFalse(
  isbn: string,
  size: 'S' | 'M' | 'L' = 'L',
): string {
  return `${COVER_BASE}/${normalizeIsbnForCover(isbn)}-${size}.jpg?default=false`;
}

/** Tamanho do PNG genérico “sem capa” do Google Books (mesmo MD5 para ISBN inválido vs várias edições). */
export const GOOGLE_BOOKS_PLACEHOLDER_PNG_BYTES = 1269;

/**
 * Google Books cover por ISBN (fife = largura/altura desejada).
 * Em dev, a URL passa por `/gb-cover` (proxy no Vite).
 */
export function googleBooksCoverUrl(isbn: string, w = 400, h = 600): string {
  const id = normalizeIsbnForCover(isbn);
  const path = `/books/content/images/frontcover/isbn:${id}?fife=w${w}-h${h}`;
  if (import.meta.env.DEV) {
    return `/gb-cover${path}`;
  }
  return `https://books.google.com${path}`;
}
