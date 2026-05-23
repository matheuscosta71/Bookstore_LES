import { normalizeIsbnForCover } from '@/utils/openLibrary';
import { appLogger } from '@/utils/appLogger';

export type CatalogBookHit = {
  id: string;
  source: 'openlibrary' | 'google';
  title: string;
  author: string;
  isbn: string;
  category?: string;
  thumbnailUrl?: string;
  /** Nome da editora (Google Books / Open Library quando disponível). */
  publisher?: string;
  /** Sinopse ou descrição curta (texto plano). */
  synopsisSnippet?: string;
  pageCount?: number;
  publicationYear?: number;
  /**
   * Sugestão para código de barras (em livros costuma coincidir com ISBN-13 em 13 dígitos).
   */
  suggestedBarcode?: string;
};

const OL_ROOT = import.meta.env.DEV ? '/ol-api' : 'https://openlibrary.org';

function pickBestIsbn(candidates: string[]): string {
  const cleaned = candidates.map((s) => normalizeIsbnForCover(s)).filter(Boolean);
  const isbn13 = cleaned.find((c) => c.length === 13 && c.startsWith('978'));
  if (isbn13) return isbn13;
  const any13 = cleaned.find((c) => c.length === 13);
  if (any13) return any13;
  const isbn10 = cleaned.find((c) => c.length === 10);
  if (isbn10) return isbn10;
  return cleaned[0] ?? '';
}

function subjectToCategory(subjects: unknown): string | undefined {
  if (!subjects) return undefined;
  if (Array.isArray(subjects) && subjects.length > 0) {
    const first = subjects[0];
    return typeof first === 'string' ? first.slice(0, 80) : undefined;
  }
  return undefined;
}

function stripHtmlToPlain(s: string, maxLen: number): string {
  const plain = s
    .replace(/<[^>]*>/g, ' ')
    .replace(/\*\*?/g, '')
    .replace(/\s+/g, ' ')
    .trim();
  return plain.length > maxLen ? `${plain.slice(0, maxLen - 1)}…` : plain;
}

function yearFromPublishedDate(s?: string): number | undefined {
  if (!s || s.length < 4) return undefined;
  const y = Number.parseInt(s.slice(0, 4), 10);
  return Number.isFinite(y) && y >= 1000 && y <= 2100 ? y : undefined;
}

function suggestedBarcodeFromIsbn(isbn: string): string | undefined {
  const digits = normalizeIsbnForCover(isbn).replace(/\D/g, '');
  if (digits.length === 13) return digits;
  return undefined;
}

function mergeCatalogHits(primary: CatalogBookHit, secondary: CatalogBookHit): CatalogBookHit {
  const isbnNorm = normalizeIsbnForCover(primary.isbn);
  return {
    ...primary,
    publisher: primary.publisher || secondary.publisher,
    synopsisSnippet: primary.synopsisSnippet || secondary.synopsisSnippet,
    pageCount: primary.pageCount ?? secondary.pageCount,
    publicationYear: primary.publicationYear ?? secondary.publicationYear,
    category: primary.category || secondary.category,
    thumbnailUrl: primary.thumbnailUrl || secondary.thumbnailUrl,
    suggestedBarcode: primary.suggestedBarcode || secondary.suggestedBarcode || suggestedBarcodeFromIsbn(isbnNorm),
  };
}

export async function searchOpenLibrary(query: string): Promise<CatalogBookHit[]> {
  const q = query.trim();
  if (!q) return [];
  const url = `${OL_ROOT}/search.json?q=${encodeURIComponent(q)}&limit=20`;
  const res = await fetch(url);
  if (!res.ok) {
    appLogger.warn('externalBookCatalog', 'searchOpenLibrary', 'Open Library HTTP erro', { status: res.status });
    throw new Error(`Open Library: ${res.status}`);
  }
  const data = (await res.json()) as {
    docs?: Array<{
      title?: string;
      author_name?: string[];
      isbn?: string[];
      cover_i?: number;
      subject?: string[];
      publisher?: string[];
      first_publish_year?: number;
    }>;
  };
  const docs = data.docs ?? [];
  const out: CatalogBookHit[] = [];
  for (let i = 0; i < docs.length; i++) {
    const d = docs[i];
    const title = (d.title ?? '').trim();
    const rawIsbns = d.isbn ?? [];
    const isbn = pickBestIsbn(rawIsbns);
    if (!title || !isbn) continue;
    const author = (d.author_name ?? []).join(', ');
    const thumbnailUrl =
      typeof d.cover_i === 'number'
        ? `https://covers.openlibrary.org/b/id/${d.cover_i}-M.jpg`
        : undefined;
    const publisher = Array.isArray(d.publisher) && d.publisher[0] ? d.publisher[0].trim() : undefined;
    const publicationYear =
      typeof d.first_publish_year === 'number' && d.first_publish_year > 0
        ? d.first_publish_year
        : undefined;
    out.push({
      id: `ol-${isbn}-${i}`,
      source: 'openlibrary',
      title,
      author,
      isbn,
      category: subjectToCategory(d.subject),
      thumbnailUrl,
      publisher,
      publicationYear,
      suggestedBarcode: suggestedBarcodeFromIsbn(isbn),
    });
  }
  return dedupeByIsbn(out);
}

function googleKey(): string | undefined {
  const k = import.meta.env.VITE_GOOGLE_BOOKS_API_KEY as string | undefined;
  return k?.trim() || undefined;
}

/**
 * Sem chave de API, a quota anónima do Google Books é tão baixa que quase sempre devolve 429.
 * Só chamamos a API quando `VITE_GOOGLE_BOOKS_API_KEY` está definida.
 * Com chave, 429/403 devolve lista vazia em silêncio (Open Library continua a funcionar).
 */
export async function searchGoogleBooks(query: string): Promise<CatalogBookHit[]> {
  const q = query.trim();
  if (!q) return [];
  const key = googleKey();
  if (!key) return [];

  const base = 'https://www.googleapis.com/books/v1/volumes';
  const params = new URLSearchParams({
    q,
    maxResults: '20',
    key,
  });
  const res = await fetch(`${base}?${params}`);
  if (!res.ok) {
    if (res.status === 429 || res.status === 403) {
      appLogger.debug('externalBookCatalog', 'searchGoogleBooks', 'Google Books quota ou acesso', {
        status: res.status,
      });
      return [];
    }
    appLogger.warn('externalBookCatalog', 'searchGoogleBooks', 'Google Books HTTP erro', { status: res.status });
    return [];
  }
  const data = (await res.json()) as {
    items?: Array<{
      volumeInfo?: {
        title?: string;
        authors?: string[];
        publisher?: string;
        publishedDate?: string;
        description?: string;
        pageCount?: number;
        categories?: string[];
        industryIdentifiers?: Array<{ type?: string; identifier?: string }>;
        imageLinks?: { thumbnail?: string; smallThumbnail?: string };
      };
    }>;
  };
  const items = data.items ?? [];
  const out: CatalogBookHit[] = [];
  for (let i = 0; i < items.length; i++) {
    const vi = items[i].volumeInfo;
    if (!vi) continue;
    const title = (vi.title ?? '').trim();
    const ids = vi.industryIdentifiers ?? [];
    const raw: string[] = [];
    for (const x of ids) {
      if (x.identifier) raw.push(x.identifier);
    }
    const isbn = pickBestIsbn(raw);
    if (!title || !isbn) continue;
    const author = (vi.authors ?? []).join(', ');
    const thumb = vi.imageLinks?.thumbnail ?? vi.imageLinks?.smallThumbnail;
    const thumbnailUrl = thumb?.replace(/^http:/, 'https:');
    const publisher = vi.publisher?.trim() || undefined;
    const desc = vi.description?.trim();
    const synopsisSnippet = desc ? stripHtmlToPlain(desc, 3500) : undefined;
    const pageCount = typeof vi.pageCount === 'number' && vi.pageCount > 0 ? vi.pageCount : undefined;
    const publicationYear = yearFromPublishedDate(vi.publishedDate);
    out.push({
      id: `gb-${isbn}-${i}`,
      source: 'google',
      title,
      author,
      isbn,
      category: vi.categories?.[0],
      thumbnailUrl,
      publisher,
      synopsisSnippet,
      pageCount,
      publicationYear,
      suggestedBarcode: suggestedBarcodeFromIsbn(isbn),
    });
  }
  return dedupeByIsbn(out);
}

function dedupeByIsbn(hits: CatalogBookHit[]): CatalogBookHit[] {
  const seen = new Map<string, CatalogBookHit>();
  for (const h of hits) {
    const k = normalizeIsbnForCover(h.isbn);
    if (!k) continue;
    if (!seen.has(k)) seen.set(k, h);
  }
  return [...seen.values()];
}

/** Open Library primeiro; Google só para ISBN ainda não vistos. */
export async function searchCatalogCombined(query: string): Promise<CatalogBookHit[]> {
  const q = query.trim();
  if (!q) return [];
  const [ol, gb] = await Promise.allSettled([searchOpenLibrary(q), searchGoogleBooks(q)]);
  if (ol.status === 'rejected') {
    appLogger.warn('externalBookCatalog', 'searchCatalogCombined', 'Busca Open Library rejeitada', {
      reason: ol.reason instanceof Error ? ol.reason.message : String(ol.reason),
    });
  }
  if (gb.status === 'rejected') {
    appLogger.debug('externalBookCatalog', 'searchCatalogCombined', 'Busca Google ignorada ou falhou', {
      reason: gb.reason instanceof Error ? gb.reason.message : String(gb.reason),
    });
  }
  const olHits = ol.status === 'fulfilled' ? ol.value : [];
  const gbHits = gb.status === 'fulfilled' ? gb.value : [];
  const merged = new Map<string, CatalogBookHit>();
  for (const h of olHits) {
    const k = normalizeIsbnForCover(h.isbn);
    merged.set(k, { ...h, suggestedBarcode: h.suggestedBarcode ?? suggestedBarcodeFromIsbn(k) });
  }
  for (const h of gbHits) {
    const k = normalizeIsbnForCover(h.isbn);
    const prev = merged.get(k);
    const enriched = { ...h, suggestedBarcode: h.suggestedBarcode ?? suggestedBarcodeFromIsbn(k) };
    if (!prev) merged.set(k, enriched);
    else merged.set(k, mergeCatalogHits(prev, enriched));
  }
  return [...merged.values()];
}
