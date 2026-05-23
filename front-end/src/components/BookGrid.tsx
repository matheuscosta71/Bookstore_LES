import type { Book } from '@/types/api';
import { BookCard } from './BookCard';
import { cn } from '@/utils/cn';

type Props = {
  books: Book[];
  highlightIds?: Set<string>;
  className?: string;
};

export function BookGrid({ books, highlightIds, className }: Props) {
  return (
    <div
      className={cn(
        'grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5',
        className,
      )}
    >
      {books.map((b) => (
        <BookCard key={b.id} book={b} highlight={highlightIds?.has(b.id)} />
      ))}
    </div>
  );
}
