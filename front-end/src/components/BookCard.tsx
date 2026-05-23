import { Link } from 'react-router-dom';
import type { Book } from '@/types/api';
import { formatBRL } from '@/utils/format';
import { ROUTES } from '@/constants/routes';
import { cn } from '@/utils/cn';
import { BookCover } from '@/components/BookCover';

type Props = {
  book: Book;
  className?: string;
  highlight?: boolean;
};

export function BookCard({ book, className, highlight }: Props) {
  return (
    <article
      className={cn(
        'group flex flex-col overflow-hidden rounded-xl border bg-white shadow-card transition hover:shadow-lift',
        highlight ? 'border-accent/40 ring-2 ring-accent/20' : 'border-stone-200',
        className,
      )}
    >
      <Link to={ROUTES.bookDetail(book.id)} className="relative aspect-[2/3] overflow-hidden bg-stone-100">
        <BookCover isbn={book.isbn} title={book.title} className="absolute inset-0" />
        {highlight && (
          <span className="absolute left-2 top-2 rounded-full bg-accent px-2 py-0.5 text-xs font-semibold text-white">
            Para você
          </span>
        )}
      </Link>
      <div className="flex flex-1 flex-col p-4">
        <Link to={ROUTES.bookDetail(book.id)} className="font-medium text-ink line-clamp-2 hover:text-brand">
          {book.title}
        </Link>
        {book.author && <p className="mt-1 text-sm text-ink-muted line-clamp-1">{book.author}</p>}
        {book.code && (
          <p className="mt-1 font-mono text-[11px] leading-tight text-ink-muted line-clamp-1" title={book.code}>
            {book.code}
          </p>
        )}
        <p className="mt-auto pt-3 font-semibold text-brand">{formatBRL(book.price)}</p>
        <Link
          to={ROUTES.bookDetail(book.id)}
          className="mt-3 block rounded-lg border border-stone-200 py-2 text-center text-sm font-medium hover:border-brand hover:text-brand"
        >
          Ver detalhes
        </Link>
      </div>
    </article>
  );
}
