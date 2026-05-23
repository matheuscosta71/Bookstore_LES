import { cn } from '@/utils/cn';

type Props = {
  page: number;
  totalPages: number;
  onPageChange: (p: number) => void;
};

export function Pagination({ page, totalPages, onPageChange }: Props) {
  if (totalPages <= 1) return null;
  return (
    <div className="flex flex-wrap items-center justify-center gap-2 py-6">
      <button
        type="button"
        disabled={page <= 0}
        onClick={() => onPageChange(page - 1)}
        className={cn(
          'rounded-lg border px-3 py-1.5 text-sm',
          page <= 0 ? 'cursor-not-allowed opacity-40' : 'hover:border-brand',
        )}
      >
        Anterior
      </button>
      <span className="text-sm text-ink-muted">
        Página {page + 1} de {totalPages}
      </span>
      <button
        type="button"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
        className={cn(
          'rounded-lg border px-3 py-1.5 text-sm',
          page >= totalPages - 1 ? 'cursor-not-allowed opacity-40' : 'hover:border-brand',
        )}
      >
        Próxima
      </button>
    </div>
  );
}
