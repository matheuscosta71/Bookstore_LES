import { useId, type ReactNode } from 'react';

export type MessageModalProps = {
  open: boolean;
  title: string;
  children: ReactNode;
  onClose: () => void;
  closeLabel?: string;
};

export function MessageModal({ open, title, children, onClose, closeLabel = 'OK' }: MessageModalProps) {
  const titleId = useId();

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      onClick={onClose}
    >
      <div
        className="w-full max-w-md rounded-xl border bg-white p-4 shadow-lg"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 id={titleId} className="font-semibold">
          {title}
        </h3>
        <div className="mt-2 text-sm">{children}</div>
        <button
          type="button"
          onClick={onClose}
          className="mt-4 rounded-lg bg-brand px-4 py-2 text-sm text-white"
        >
          {closeLabel}
        </button>
      </div>
    </div>
  );
}
