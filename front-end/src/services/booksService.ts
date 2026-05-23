import type { Book, Page } from '@/types/api';
import { api } from './api';
import { dbgCoverApi } from '@/utils/coverDebug';
import { googleBooksCoverUrl, openLibraryCoverUrlDefaultFalse } from '@/utils/openLibrary';

export type BookListParams = {
  title?: string;
  author?: string;
  category?: string;
  isbn?: string;
  /** Código interno do livro (RF0015) */
  code?: string;
  includeInactive?: boolean;
  page?: number;
  size?: number;
  sort?: string;
};

export async function fetchBooks(params: BookListParams): Promise<Page<Book>> {
  const req = {
    title: params.title || undefined,
    author: params.author || undefined,
    category: params.category || undefined,
    isbn: params.isbn || undefined,
    code: params.code?.trim() || undefined,
    includeInactive: params.includeInactive || undefined,
    page: params.page ?? 0,
    size: params.size ?? 12,
    sort: params.sort,
  };
  const { data } = await api.get<Page<Book>>('/books', { params: req });
  /** Um único log após sucesso evita duplicar REQ+RES com React Strict Mode / re-fetch. */
  dbgCoverApi('API-BOOKS', 'booksService.ts:fetchBooks', 'GET /books ok — request + amostra ISBN → URLs de capa', {
    request: req,
    totalElements: data.totalElements,
    totalPages: data.totalPages,
    contentLen: data.content.length,
    note: 'A API Spring não envia imagens; capas = Open Library + Google derivadas do ISBN.',
    sample: data.content.slice(0, 6).map((b) => ({
      id: b.id,
      title: b.title,
      isbn: b.isbn,
      openLibraryDefaultFalse: openLibraryCoverUrlDefaultFalse(b.isbn),
      googleBooks: googleBooksCoverUrl(b.isbn),
    })),
  });
  return data;
}

export async function fetchBookById(id: string, options?: { includeInactive?: boolean }): Promise<Book> {
  const { data } = await api.get<Book>(`/books/${id}`, {
    params: { includeInactive: options?.includeInactive || undefined },
  });
  dbgCoverApi('API-BOOK', 'booksService.ts:fetchBookById', `GET /books/:id ok — ISBN para capas externas`, {
    requestedId: id,
    includeInactive: options?.includeInactive || false,
    bookId: data.id,
    title: data.title,
    isbn: data.isbn,
    openLibraryDefaultFalse: openLibraryCoverUrlDefaultFalse(data.isbn),
    googleBooks: googleBooksCoverUrl(data.isbn),
  });
  return data;
}
