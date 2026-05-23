import { useId } from 'react';
import { cn } from '@/utils/cn';

export type ConfirmModalProps = {
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'danger' | 'neutral';
  onConfirm: () => void;
  onClose: () => void;
};

export function ConfirmModal({
  open,
  title,
  message,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  variant = 'neutral',
  onConfirm,
  onClose,
}: ConfirmModalProps) {
  const titleId = useId();
  const descId = useId();

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-black/40 p-4"
      role="alertdialog"
      aria-modal="true"
      aria-labelledby={titleId}
      aria-describedby={descId}
      onClick={onClose}
    >
      <div
        className="w-full max-w-md rounded-xl border bg-white p-4 shadow-lg"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 id={titleId} className="font-semibold">
          {title}
        </h3>
        <p id={descId} className="mt-2 text-sm text-ink-muted">
          {message}
        </p>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => {
              onConfirm();
            }}
            className={cn(
              'rounded-lg px-4 py-2 text-sm font-medium text-white',
              variant === 'danger' ? 'bg-red-700 hover:bg-red-800' : 'bg-brand hover:bg-brand-light',
            )}
          >
            {confirmLabel}
          </button>
          <button type="button" onClick={onClose} className="rounded-lg border border-stone-300 px-4 py-2 text-sm">
            {cancelLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
