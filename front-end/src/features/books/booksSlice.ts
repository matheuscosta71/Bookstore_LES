import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { Book, Page } from '@/types/api';
import * as booksService from '@/services/booksService';

export type BooksFilters = {
  title: string;
  author: string;
  category: string;
  isbn: string;
  sort: string;
};

type BooksState = {
  list: Book[];
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  filters: BooksFilters;
  detail: Book | null;
  listStatus: 'idle' | 'loading' | 'succeeded' | 'failed';
  detailStatus: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
};

const initialFilters: BooksFilters = {
  title: '',
  author: '',
  category: '',
  isbn: '',
  sort: 'title,asc',
};

const initialState: BooksState = {
  list: [],
  page: 0,
  totalPages: 0,
  totalElements: 0,
  size: 12,
  filters: initialFilters,
  detail: null,
  listStatus: 'idle',
  detailStatus: 'idle',
  error: null,
};

export const fetchBooksPage = createAsyncThunk(
  'books/fetchPage',
  async (
    arg: { page?: number; size?: number; filters?: Partial<BooksFilters> } | undefined,
    { getState },
  ) => {
    const state = getState() as { books: BooksState };
    const filters = { ...state.books.filters, ...arg?.filters };
    const page = arg?.page ?? state.books.page;
    const size = arg?.size ?? state.books.size;
    const data: Page<Book> = await booksService.fetchBooks({
      title: filters.title || undefined,
      author: filters.author || undefined,
      category: filters.category || undefined,
      isbn: filters.isbn || undefined,
      sort: filters.sort || undefined,
      page,
      size,
    });
    return { data, page, size, filters };
  },
);

export const fetchBookDetail = createAsyncThunk('books/fetchDetail', async (id: string) => {
  return booksService.fetchBookById(id);
});

const booksSlice = createSlice({
  name: 'books',
  initialState,
  reducers: {
    setFilters(state, action: PayloadAction<Partial<BooksFilters>>) {
      state.filters = { ...state.filters, ...action.payload };
      state.page = 0;
    },
    resetFilters(state) {
      state.filters = { ...initialFilters };
      state.page = 0;
    },
    clearBookDetail(state) {
      state.detail = null;
      state.detailStatus = 'idle';
    },
  },
  extraReducers: (b) => {
    b.addCase(fetchBooksPage.pending, (s) => {
      s.listStatus = 'loading';
      s.error = null;
    });
    b.addCase(fetchBooksPage.fulfilled, (s, a) => {
      s.listStatus = 'succeeded';
      s.list = a.payload.data.content;
      s.totalPages = a.payload.data.totalPages;
      s.totalElements = a.payload.data.totalElements;
      s.page = a.payload.page;
      s.size = a.payload.size;
      s.filters = a.payload.filters as BooksFilters;
    });
    b.addCase(fetchBooksPage.rejected, (s, a) => {
      s.listStatus = 'failed';
      s.error = a.error.message ?? 'Erro ao carregar livros';
    });

    b.addCase(fetchBookDetail.pending, (s) => {
      s.detailStatus = 'loading';
      s.detail = null;
      s.error = null;
    });
    b.addCase(fetchBookDetail.fulfilled, (s, a) => {
      s.detailStatus = 'succeeded';
      s.detail = a.payload;
    });
    b.addCase(fetchBookDetail.rejected, (s, a) => {
      s.detailStatus = 'failed';
      s.detail = null;
      s.error = a.error.message ?? 'Livro não encontrado';
    });
  },
});

export const { setFilters, resetFilters, clearBookDetail } = booksSlice.actions;
export default booksSlice.reducer;

export const selectBooksState = (s: { books: BooksState }) => s.books;
