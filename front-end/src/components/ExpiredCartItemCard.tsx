import type { CartItem } from '@/types/api';
import { formatBRL } from '@/utils/format';
import { BookCover } from './BookCover';

type Props = {
  item: CartItem;
  isbn?: string;
  expirationMinutes: number;
  onReadd: () => void;
  onRemove: () => void;
  busy?: boolean;
};

export function ExpiredCartItemCard({ item, isbn, expirationMinutes, onReadd, onRemove, busy }: Props) {
  const isbnTrimmed = isbn?.trim() ?? '';
  const showCover = Boolean(isbnTrimmed);
  return (
    <div className="flex flex-col gap-3 rounded-xl border-2 border-dashed border-amber-400 bg-amber-50/80 p-4 md:flex-row md:items-center md:justify-between">
      <div className="flex gap-3">
        <div className="h-20 w-14 flex-shrink-0 overflow-hidden rounded bg-amber-100">
          {showCover ? (
            <BookCover isbn={isbnTrimmed} title={item.title} className="h-20 w-14 rounded" />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-xs text-amber-900">exp.</div>
          )}
        </div>
        <div>
          <p className="font-medium text-amber-950">{item.title}</p>
          <p className="text-sm text-amber-800">
            Este item foi removido por expiração de tempo (prazo de {expirationMinutes} min para finalizar a compra).
            Valor de referência: {formatBRL(item.unitPrice)}
            {item.expiresAt && (
              <span className="mt-1 block text-xs text-amber-700">
                Encerrou em {new Date(item.expiresAt).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}
              </span>
            )}
          </p>
        </div>
      </div>
      <div className="flex flex-col items-stretch gap-2 sm:items-end">
        <button
          type="button"
          onClick={onReadd}
          disabled={busy}
          className="rounded-lg bg-amber-700 px-4 py-2 text-sm font-medium text-white hover:bg-amber-800 disabled:opacity-50"
        >
          Adicionar novamente ao carrinho
        </button>
        <button
          type="button"
          onClick={onRemove}
          disabled={busy}
          className="text-sm text-red-700 hover:underline disabled:opacity-50"
        >
          Remover linha
        </button>
      </div>
    </div>
  );
}
