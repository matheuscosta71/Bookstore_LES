import { cn } from '@/utils/cn';

export function LoadingSpinner({ className }: { className?: string }) {
  return (
    <div className={cn('flex justify-center py-16', className)}>
      <div
        className="h-10 w-10 animate-spin rounded-full border-2 border-brand border-t-transparent"
        aria-hidden
      />
      <span className="sr-only">Carregando</span>
    </div>
  );
}
