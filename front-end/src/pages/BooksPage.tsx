import { useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { fetchBooksPage, resetFilters, setFilters } from '@/features/books/booksSlice';
import { BookGrid } from '@/components/BookGrid';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { EmptyState } from '@/components/EmptyState';
import { ErrorState } from '@/components/ErrorState';
import { Pagination } from '@/components/Pagination';
import { FilterPanel } from '@/components/FilterPanel';
import { ROUTES } from '@/constants/routes';

export function BooksPage() {
  const dispatch = useAppDispatch();
  const { list, listStatus, error, page, totalPages } = useAppSelector((s) => s.books);
  const [searchParams, setSearchParams] = useSearchParams();
  const sp = searchParams.toString();

  useEffect(() => {
    const q = new URLSearchParams(sp);
    const filters = {
      title: q.get('title') ?? '',
      author: q.get('author') ?? '',
      category: q.get('category') ?? '',
      isbn: q.get('isbn') ?? '',
      sort: q.get('sort') ?? 'title,asc',
    };
    const pageNum = Math.max(0, Number(q.get('page') ?? 0) || 0);
    dispatch(setFilters(filters));
    dispatch(fetchBooksPage({ page: pageNum, filters }));
  }, [dispatch, sp]);

  const q = new URLSearchParams(sp);
  const defaults = {
    title: q.get('title') ?? '',
    author: q.get('author') ?? '',
    category: q.get('category') ?? '',
    isbn: q.get('isbn') ?? '',
    sort: q.get('sort') ?? 'title,asc',
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-10">
      <div className="flex flex-wrap items-baseline justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-semibold text-ink">Catálogo de livros</h1>
          <p className="mt-1 text-ink-muted">Filtre por título, autor, categoria ou ISBN.</p>
        </div>
        <Link
          to={ROUTES.home}
          className="text-sm font-medium text-brand hover:underline"
        >
          Continuar comprando →
        </Link>
      </div>

      <div className="mt-8 grid gap-8 lg:grid-cols-[240px_1fr]">
        <aside>
          <FilterPanel
            key={sp}
            defaultValues={defaults}
            onApply={(v) => {
              const next = new URLSearchParams();
              if (v.title) next.set('title', v.title);
              if (v.author) next.set('author', v.author);
              if (v.category) next.set('category', v.category);
              if (v.isbn) next.set('isbn', v.isbn);
              if (v.sort) next.set('sort', v.sort);
              setSearchParams(next);
            }}
            onClear={() => {
              dispatch(resetFilters());
              setSearchParams({});
            }}
          />
        </aside>
        <div>
          {listStatus === 'loading' && <LoadingSpinner />}
          {listStatus === 'failed' && error && (
            <ErrorState message={error} onRetry={() => dispatch(fetchBooksPage({}))} />
          )}
          {listStatus === 'succeeded' && list.length === 0 && (
            <EmptyState title="Nenhum livro encontrado" description="Ajuste os filtros ou tente outra busca." />
          )}
          {listStatus === 'succeeded' && list.length > 0 && <BookGrid books={list} />}
          {listStatus === 'succeeded' && list.length > 0 && (
            <Pagination
              page={page}
              totalPages={totalPages}
              onPageChange={(p) => {
                const next = new URLSearchParams(searchParams);
                next.set('page', String(p));
                setSearchParams(next);
              }}
            />
          )}
        </div>
      </div>
    </div>
  );
}
