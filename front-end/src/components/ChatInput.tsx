type Props = {
  value: string;
  onChange: (v: string) => void;
  onSend: () => void;
  loading?: boolean;
  placeholder?: string;
};

export function ChatInput({ value, onChange, onSend, loading, placeholder }: Props) {
  return (
    <div className="flex gap-2 border-t border-stone-200 bg-white p-4">
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && (e.preventDefault(), onSend())}
        placeholder={placeholder ?? 'Digite sua mensagem...'}
        disabled={loading}
        className="flex-1 rounded-xl border border-stone-200 px-4 py-3 text-sm focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20"
      />
      <button
        type="button"
        onClick={onSend}
        disabled={loading || !value.trim()}
        className="rounded-xl bg-brand px-5 py-3 text-sm font-medium text-white hover:bg-brand-light disabled:opacity-50"
      >
        {loading ? '...' : 'Enviar'}
      </button>
    </div>
  );
}
