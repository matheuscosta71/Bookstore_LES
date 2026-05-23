import { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { fetchOrdersAndTransactions } from '@/features/customer/customerSlice';
import { formatBRL, formatDate } from '@/utils/format';

export function ProfileTransactionsPage() {
  const dispatch = useAppDispatch();
  const customerId = useAppSelector((s) => s.auth.customerId)!;
  const transactions = useAppSelector((s) => s.customer.transactions);

  useEffect(() => {
    dispatch(fetchOrdersAndTransactions(customerId));
  }, [dispatch, customerId]);

  return (
    <div>
      <h1 className="font-display text-2xl font-semibold">Extrato</h1>
      <p className="mt-1 text-sm text-ink-muted">
        RF0025: extrato de transações do cliente. Linhas registradas após cada compra finalizada (tipo PURCHASE, valor
        com frete).
      </p>
      <ul className="mt-6 space-y-3">
        {transactions.map((t) => (
          <li
            key={t.id}
            className="flex flex-wrap items-center justify-between gap-2 rounded-lg border p-4"
          >
            <div>
              <p className="font-medium">{t.description ?? '—'}</p>
              <p className="text-sm text-ink-muted">
                {formatDate(t.transactionDate)}{' '}
                {t.type != null && t.type !== '' ? (
                  <span className="text-ink-muted"> · {t.type}</span>
                ) : null}
              </p>
            </div>
            <div className="text-right">
              <p className="font-semibold text-brand">{formatBRL(t.amount)}</p>
            </div>
          </li>
        ))}
      </ul>
      {transactions.length === 0 && (
        <p className="mt-4 text-ink-muted">Nenhuma transação ainda. Finalize uma compra para ver o extrato.</p>
      )}
    </div>
  );
}
