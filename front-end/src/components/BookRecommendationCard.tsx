import { Link } from 'react-router-dom';
import type { Book } from '@/types/api';
import { ROUTES } from '@/constants/routes';
import { BookCover } from '@/components/BookCover';

type Props = {
  book: Book;
  reason: string;
};

function buildChatPrompt(book: Book): string {
  return `Me recomende livros parecidos com "${book.title}" de ${book.author ?? 'autor desconhecido'} e explique por que combinam comigo.`;
}

export function BookRecommendationCard({ book, reason }: Props) {
  const params = new URLSearchParams({ prompt: buildChatPrompt(book), bookId: book.id, bookTitle: book.title });

  return (
    <article className="flex h-full flex-col overflow-hidden rounded-xl border border-stone-200 bg-white shadow-card transition hover:shadow-lift">
      <Link to={ROUTES.bookDetail(book.id)} className="relative aspect-[16/10] overflow-hidden bg-stone-100">
        <BookCover isbn={book.isbn} title={book.title} className="absolute inset-0" />
      </Link>

      <div className="flex flex-1 flex-col p-4">
        <h3 className="line-clamp-2 font-semibold text-ink">{book.title}</h3>
        <p className="mt-1 line-clamp-1 text-sm text-ink-muted">{book.author ?? 'Autor desconhecido'}</p>
        {book.code && (
          <p className="mt-1 font-mono text-[11px] leading-tight text-ink-muted line-clamp-1" title={book.code}>
            {book.code}
          </p>
        )}

        <p className="mt-3 line-clamp-3 rounded-lg bg-brand-soft/60 px-3 py-2 text-sm leading-relaxed text-ink">
          <span className="font-semibold text-brand">Motivo:</span> {reason}
        </p>

        <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-2">
          <Link
            to={ROUTES.bookDetail(book.id)}
            className="rounded-lg border border-stone-200 px-3 py-2 text-center text-sm font-medium text-ink hover:border-brand hover:text-brand"
          >
            Ver detalhes
          </Link>
          <Link
            to={`${ROUTES.aiChat}?${params.toString()}`}
            className="rounded-lg bg-brand px-3 py-2 text-center text-sm font-medium text-white hover:bg-brand-light"
          >
            Perguntar à IA
          </Link>
        </div>
      </div>
    </article>
  );
}
