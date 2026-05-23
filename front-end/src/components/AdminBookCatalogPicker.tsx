import { useId, useState } from 'react';
import { searchCatalogCombined, type CatalogBookHit } from '@/services/externalBookCatalog';

export type CatalogApplyPayload = {
  title: string;
  author: string;
  category: string;
  isbn: string;
  publisher?: string;
  synopsis?: string;
  pageCount?: number;
  publicationYear?: number;
  /** ISBN-13 como código de barras (EAN) quando aplicável. */
  barcode?: string;
};

type Props = {
  onApply: (data: CatalogApplyPayload) => void;
  disabled?: boolean;
  /** Texto curto para leitor de ecrã / mensagem após aplicar */
  formLabel?: string;
};

function Step({ n, label, active }: { n: number; label: string; active: boolean }) {
  return (
    <div className="flex flex-1 flex-col items-center gap-1 text-center sm:flex-row sm:items-center sm:gap-2 sm:text-left">
      <span
        className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
          active ? 'bg-brand text-white' : 'bg-slate-200 text-slate-600'
        }`}
        aria-hidden
      >
        {n}
      </span>
      <span className={`text-[11px] leading-tight sm:text-xs ${active ? 'font-medium text-ink' : 'text-ink-muted'}`}>
        {label}
      </span>
    </div>
  );
}

function ResultSkeleton() {
  return (
    <li className="flex gap-3 rounded-lg border border-slate-100 bg-white p-2">
      <div className="h-16 w-12 shrink-0 animate-pulse rounded bg-slate-200" />
      <div className="min-w-0 flex-1 space-y-2 py-0.5">
        <div className="h-3 w-4/5 animate-pulse rounded bg-slate-200" />
        <div className="h-2.5 w-1/2 animate-pulse rounded bg-slate-100" />
        <div className="h-2.5 w-2/5 animate-pulse rounded bg-slate-100" />
      </div>
    </li>
  );
}

export function AdminBookCatalogPicker({ onApply, disabled, formLabel = 'formulário de livro' }: Props) {
  const baseId = useId();
  const searchId = `${baseId}-search`;
  const [query, setQuery] = useState('');
  const [hits, setHits] = useState<CatalogBookHit[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [appliedId, setAppliedId] = useState<string | null>(null);
  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [searched, setSearched] = useState(false);

  async function runSearch() {
    const q = query.trim();
    if (!q || disabled) return;
    setError(null);
    setAppliedId(null);
    setLoading(true);
    setHits([]);
    setStep(1);
    try {
      const list = await searchCatalogCombined(q);
      setHits(list);
      setSearched(true);
      setStep(list.length > 0 ? 2 : 1);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível pesquisar.');
      setHits([]);
      setSearched(true);
    } finally {
      setLoading(false);
    }
  }

  function applyHit(h: CatalogBookHit) {
    onApply({
      title: h.title,
      author: h.author,
      category: h.category ?? '',
      isbn: h.isbn,
      publisher: h.publisher,
      synopsis: h.synopsisSnippet,
      pageCount: h.pageCount,
      publicationYear: h.publicationYear,
      barcode: h.suggestedBarcode,
    });
    setAppliedId(h.id);
    setStep(3);
  }

  const busy = disabled || loading;

  return (
    <aside
      className="flex flex-col rounded-xl border border-slate-200/80 bg-gradient-to-b from-slate-50 to-white p-4 shadow-sm"
      aria-label="Assistente de catálogo aberto"
    >
      <div className="border-b border-slate-100 pb-3">
        <h3 className="font-semibold text-ink">Assistente de cadastro</h3>
        <p className="mt-1 text-xs leading-relaxed text-ink-muted">
          Pesquise por título ou ISBN. A fonte principal é a{' '}
          <span className="font-medium text-ink">Open Library</span> (sem limite de chave).
          {import.meta.env.VITE_GOOGLE_BOOKS_API_KEY ? (
            <> Resultados do Google Books são combinados quando a chave está ativa.</>
          ) : (
            <>
              {' '}
              Para juntar o Google Books (quota própria), defina{' '}
              <code className="rounded bg-slate-100 px-1 font-mono text-[10px]">VITE_GOOGLE_BOOKS_API_KEY</code> no{' '}
              <code className="rounded bg-slate-100 px-1 font-mono text-[10px]">.env</code>.
            </>
          )}{' '}
          Escolha uma edição para preencher o {formLabel}; preço e estoque ficam à tua escolha.
        </p>
      </div>

      <ol className="mt-4 flex gap-2 border-b border-slate-100 pb-4" aria-label="Passos">
        <Step n={1} label="Pesquisar" active={step >= 1} />
        <span className="hidden text-slate-300 sm:inline" aria-hidden>
          →
        </span>
        <Step n={2} label="Escolher edição" active={step >= 2} />
        <span className="hidden text-slate-300 sm:inline" aria-hidden>
          →
        </span>
        <Step n={3} label="Preço e estoque" active={step >= 3} />
      </ol>

      <div className="mt-4 space-y-2">
        <label htmlFor={searchId} className="text-xs font-medium text-ink">
          Título, autor ou ISBN
        </label>
        <div className="flex flex-col gap-2 sm:flex-row">
          <input
            id={searchId}
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                void runSearch();
              }
            }}
            disabled={busy}
            placeholder="Ex.: The Great Gatsby ou 9780743273565"
            className="min-w-0 flex-1 rounded-lg border border-slate-200 px-3 py-2 text-sm shadow-inner focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand disabled:opacity-60"
            autoComplete="off"
          />
          <button
            type="button"
            onClick={() => void runSearch()}
            disabled={busy || !query.trim()}
            className="shrink-0 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {loading ? 'A pesquisar…' : 'Pesquisar'}
          </button>
        </div>
      </div>

      {error && (
        <p className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-xs text-red-800" role="alert">
          {error}
        </p>
      )}

      <div
        className="mt-3 min-h-[120px] flex-1"
        role="region"
        aria-live="polite"
        aria-busy={loading}
        aria-label="Resultados da pesquisa"
      >
        {loading && (
          <ul className="space-y-2" aria-hidden>
            <ResultSkeleton />
            <ResultSkeleton />
            <ResultSkeleton />
          </ul>
        )}

        {!loading && searched && hits.length === 0 && !error && (
          <p className="rounded-lg border border-dashed border-slate-200 bg-slate-50/80 px-3 py-6 text-center text-xs text-ink-muted">
            Nenhum resultado com ISBN identificável. Tente outro título ou um ISBN completo.
          </p>
        )}

        {!loading && hits.length > 0 && (
          <ul className="max-h-[min(320px,50vh)] space-y-2 overflow-y-auto pr-1">
            {hits.map((h) => (
              <li key={h.id}>
                <article className="flex gap-3 rounded-lg border border-slate-100 bg-white p-2 shadow-sm transition hover:border-slate-200">
                  <div className="relative h-20 w-[52px] shrink-0 overflow-hidden rounded bg-slate-100">
                    {h.thumbnailUrl ? (
                      <img
                        src={h.thumbnailUrl}
                        alt=""
                        className="h-full w-full object-cover"
                        loading="lazy"
                      />
                    ) : (
                      <div className="flex h-full items-center justify-center text-[10px] text-slate-400">—</div>
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="line-clamp-2 text-sm font-medium leading-snug text-ink">{h.title}</p>
                    <p className="mt-0.5 line-clamp-1 text-xs text-ink-muted">{h.author || 'Autor desconhecido'}</p>
                    {h.publisher && (
                      <p className="mt-0.5 line-clamp-1 text-[11px] text-slate-600">{h.publisher}</p>
                    )}
                    <p className="mt-1 font-mono text-[11px] text-slate-600">{h.isbn}</p>
                    <div className="mt-2 flex flex-wrap items-center gap-2">
                      <span
                        className={`rounded px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wide ${
                          h.source === 'openlibrary'
                            ? 'bg-emerald-50 text-emerald-800'
                            : 'bg-blue-50 text-blue-800'
                        }`}
                      >
                        {h.source === 'openlibrary' ? 'Open Library' : 'Google Books'}
                      </span>
                      <button
                        type="button"
                        onClick={() => applyHit(h)}
                        disabled={disabled}
                        className="rounded-md bg-slate-900 px-2.5 py-1 text-xs font-medium text-white hover:bg-slate-800 disabled:opacity-50"
                      >
                        Usar no formulário
                      </button>
                    </div>
                  </div>
                </article>
              </li>
            ))}
          </ul>
        )}
      </div>

      {appliedId && (
        <p className="mt-3 rounded-lg bg-emerald-50 px-3 py-2 text-xs text-emerald-900" role="status">
          Dados aplicados ao {formLabel}. Confira o preço e o estoque antes de guardar.
        </p>
      )}
    </aside>
  );
}
