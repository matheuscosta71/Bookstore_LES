import type { Book } from '@/types/api';
import { BookCard } from './BookCard';
import { cn } from '@/utils/cn';

type Props = {
  title: string;
  subtitle?: string;
  books: Book[];
  className?: string;
  /** destaque estilo Netflix */
  featured?: boolean;
};

export function RecommendationCarousel({ title, subtitle, books, className, featured }: Props) {
  if (!books.length) return null;
  return (
    <section className={cn('py-10', className)}>
      <div className="mb-4 flex flex-col gap-1 md:flex-row md:items-end md:justify-between">
        <div>
          <h2 className="font-display text-2xl font-semibold text-ink">{title}</h2>
          {subtitle && <p className="text-sm text-ink-muted">{subtitle}</p>}
        </div>
      </div>
      <div
        className={cn(
          'flex gap-4 overflow-x-auto pb-2 scrollbar-thin',
          featured && 'rounded-2xl bg-gradient-to-r from-brand/5 via-accent/5 to-transparent p-4',
        )}
      >
        {books.map((b) => (
          <div key={b.id} className="w-40 flex-shrink-0 sm:w-44 md:w-48">
            <BookCard book={b} highlight={featured} />
          </div>
        ))}
      </div>
    </section>
  );
}
