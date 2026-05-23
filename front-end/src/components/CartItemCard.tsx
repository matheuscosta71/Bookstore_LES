import type { CartItem } from '@/types/api';
import { useReservationCountdown } from '@/hooks/useReservationCountdown';
import { formatBRL } from '@/utils/format';
import { QuantitySelector } from './QuantitySelector';
import { BookCover } from './BookCover';
import { cn } from '@/utils/cn';

type Props = {
  item: CartItem;
  /** ISBN do livro (carregado na página do carrinho) para exibir a capa. */
  isbn?: string;
  onQuantityChange: (qty: number) => void;
  onRemove: () => void;
  busy?: boolean;
  /** Estoque atual do livro; sem limite até carregar. */
  maxQuantity?: number;
  lineError?: string | null;
  /** Prazo padrão (min), usado no fallback se a API não enviar `expiresAt`. */
  itemExpirationMinutes: number;
  /** Quando o timer zera, recarrega o carrinho para o servidor marcar expiração (RNF0042). */
  onReservationElapsed?: () => void;
};

export function CartItemCard({
  item,
  isbn,
  onQuantityChange,
  onRemove,
  busy,
  maxQuantity,
  lineError,
  itemExpirationMinutes,
  onReservationElapsed,
}: Props) {
  const isbnTrimmed = isbn?.trim() ?? '';
  const showCover = Boolean(isbnTrimmed);
  const { label: timerLabel, isPastDeadline } = useReservationCountdown(
    item.expiresAt ?? undefined,
    !item.expired && !item.purchaseDisabled,
    onReservationElapsed,
    itemExpirationMinutes,
    item.id,
  );
  /** Enquanto o timer local passa do prazo, bloqueia edição até o GET do carrinho confirmar `expired` na API. */
  const disabled = item.expired || item.purchaseDisabled || isPastDeadline;
  return (
    <div
      className={cn(
        'flex flex-col gap-4 rounded-xl border bg-white p-4 shadow-card sm:flex-row sm:items-start',
        disabled ? 'border-amber-300 bg-amber-50/50' : 'border-stone-200',
      )}
    >
      <div className="h-24 w-16 flex-shrink-0 overflow-hidden rounded bg-stone-100">
        {showCover ? (
          <BookCover isbn={isbnTrimmed} title={item.title} className="h-24 w-16 rounded" />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-center text-xs text-brand/70">
            capa
          </div>
        )}
      </div>
      <div className="min-w-0 flex-1">
        <p className="font-medium text-ink line-clamp-2">{item.title}</p>
        {item.expiringSoon && !item.expired && (
          <p className="mt-1 text-xs font-medium text-amber-800">
            Este item expira em breve; finalize ou atualize a quantidade para renovar o prazo.
          </p>
        )}
        <p className="text-sm text-ink-muted">{formatBRL(item.unitPrice)} cada</p>
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <QuantitySelector
            value={item.quantity}
            min={1}
            max={
              maxQuantity !== undefined
                ? Math.max(1, maxQuantity)
                : /* até carregar o estoque, não permitir aumentar além do que já está no carrinho */
                  item.quantity
            }
            onChange={onQuantityChange}
            disabled={disabled || busy}
          />
          <button
            type="button"
            onClick={onRemove}
            disabled={busy}
            className="text-sm text-red-600 hover:underline disabled:opacity-50"
          >
            Remover
          </button>
        </div>
        {lineError && <p className="mt-2 text-xs text-red-600">{lineError}</p>}
      </div>
      <div className="flex shrink-0 flex-col items-stretch gap-2 text-right sm:items-end">
        {!item.expired && (
          <div
            className={cn(
              'rounded-lg border px-4 py-3 text-center shadow-sm sm:min-w-[7.5rem]',
              item.expiringSoon || isPastDeadline
                ? 'border-amber-400 bg-amber-100'
                : 'border-amber-200 bg-amber-50',
            )}
          >
            <p className="text-[11px] font-semibold uppercase tracking-wide text-amber-900">Tempo restante</p>
            <p className="mt-1 font-mono text-2xl font-semibold tabular-nums leading-none text-amber-950">
              {timerLabel ?? '—'}
            </p>
          </div>
        )}
        <p className="font-semibold text-brand sm:self-end">{formatBRL(item.totalPrice)}</p>
      </div>
    </div>
  );
}
