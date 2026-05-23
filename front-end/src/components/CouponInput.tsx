type Props = {
  label: string;
  value: string;
  onChange: (v: string) => void;
  onApply: () => void;
  error?: string | null;
  disabled?: boolean;
};

export function CouponInput({ label, value, onChange, onApply, error, disabled }: Props) {
  return (
    <div>
      <label className="text-sm font-medium text-ink">{label}</label>
      <div className="mt-1 flex gap-2">
        <input
          value={value}
          onChange={(e) => onChange(e.target.value.toUpperCase())}
          disabled={disabled}
          placeholder="CÓDIGO"
          className="flex-1 rounded-lg border border-stone-200 px-3 py-2 text-sm uppercase placeholder:normal-case focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20"
        />
        <button
          type="button"
          onClick={onApply}
          disabled={disabled}
          className="rounded-lg bg-stone-800 px-4 py-2 text-sm font-medium text-white hover:bg-stone-900 disabled:opacity-50"
        >
          Aplicar
        </button>
      </div>
      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
    </div>
  );
}
