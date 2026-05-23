import { useEffect, useState, type FormEvent } from 'react';
import { getErrorMessage } from '@/services/api';

type DomainQuickCreateModalProps = {
  open: boolean;
  title: string;
  nameLabel?: string;
  onClose: () => void;
  onSubmit: (name: string) => Promise<void>;
};

export function DomainQuickCreateModal({
  open,
  title,
  nameLabel = 'Nome',
  onClose,
  onSubmit,
}: DomainQuickCreateModalProps) {
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setName('');
      setErr(null);
      setBusy(false);
    }
  }, [open]);

  if (!open) return null;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const t = name.trim();
    if (!t) {
      setErr('Informe o nome.');
      return;
    }
    setBusy(true);
    setErr(null);
    try {
      await onSubmit(t);
      onClose();
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[70] flex items-center justify-center bg-black/40 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="domain-quick-create-title"
      onClick={() => {
        if (!busy) onClose();
      }}
    >
      <form
        className="w-full max-w-md rounded-xl border bg-white p-4 shadow-lg"
        onClick={(e) => e.stopPropagation()}
        onSubmit={(e) => void handleSubmit(e)}
      >
        <h3 id="domain-quick-create-title" className="font-semibold">
          {title}
        </h3>
        <label className="mt-3 block text-sm">
          {nameLabel}
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="mt-1 w-full rounded border px-3 py-2 text-sm"
            maxLength={200}
            autoFocus
            disabled={busy}
          />
        </label>
        {err && <p className="mt-2 text-sm text-red-600">{err}</p>}
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="submit"
            disabled={busy}
            className="rounded-lg bg-brand px-4 py-2 text-sm text-white disabled:opacity-50"
          >
            {busy ? 'Salvando…' : 'Salvar'}
          </button>
          <button
            type="button"
            disabled={busy}
            onClick={onClose}
            className="rounded-lg border border-stone-300 px-4 py-2 text-sm"
          >
            Cancelar
          </button>
        </div>
      </form>
    </div>
  );
}
