import { formatBRL } from '@/utils/format';

type Props = {
  itemsSubtotal: number;
  freight: number | null;
  discount?: number;
  total: number;
};

export function OrderSummary({ itemsSubtotal, freight, discount, total }: Props) {
  return (
    <div className="rounded-xl border border-stone-200 bg-paper-accent/50 p-4">
      <h3 className="font-semibold text-ink">Resumo</h3>
      <dl className="mt-3 space-y-2 text-sm">
        <div className="flex justify-between">
          <dt className="text-ink-muted">Subtotal</dt>
          <dd>{formatBRL(itemsSubtotal)}</dd>
        </div>
        <div className="flex justify-between">
          <dt className="text-ink-muted">Frete</dt>
          <dd>{freight != null ? formatBRL(freight) : '—'}</dd>
        </div>
        {discount != null && discount > 0 && (
          <div className="flex justify-between text-green-700">
            <dt>Descontos</dt>
            <dd>-{formatBRL(discount)}</dd>
          </div>
        )}
        <div className="flex justify-between border-t border-stone-200 pt-2 text-base font-semibold">
          <dt>Total</dt>
          <dd>{formatBRL(total)}</dd>
        </div>
      </dl>
    </div>
  );
}
