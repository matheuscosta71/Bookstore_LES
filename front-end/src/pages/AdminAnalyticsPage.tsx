import { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { loadAnalyticsDashboard, setDateRange } from '@/features/analytics/analyticsSlice';
import { SalesLineChart } from '@/components/SalesLineChart';
import { formatBRL } from '@/utils/format';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorState } from '@/components/ErrorState';

export function AdminAnalyticsPage() {
  const dispatch = useAppDispatch();
  const { startDate, endDate, summary, lineChart, books, categories, status, error } = useAppSelector(
    (s) => s.analytics,
  );

  useEffect(() => {
    dispatch(loadAnalyticsDashboard({ startDate, endDate }));
  }, [dispatch, startDate, endDate]);

  const ticket =
    summary && summary.orderCount > 0
      ? Number(summary.totalRevenue) / summary.orderCount
      : 0;

  return (
    <div>
      <h1 className="font-display text-3xl font-semibold">Dashboard de vendas</h1>
      <p className="mt-1 text-sm text-ink-muted">
        RF0055 histórico de vendas por período (início e fim), comparando por livro e por categoria. Requisições
        usam `adminApi` (VITE_ADMIN_KEY → header X-Admin-Key).
      </p>

      <div className="mt-6 flex flex-wrap gap-3">
        <input
          type="date"
          value={startDate}
          readOnly
          onFocus={(e) => (e.currentTarget as HTMLInputElement & { showPicker?: () => void }).showPicker?.()}
          onClick={(e) => (e.currentTarget as HTMLInputElement & { showPicker?: () => void }).showPicker?.()}
          onChange={(e) => dispatch(setDateRange({ startDate: e.target.value, endDate }))}
          className="rounded border px-3 py-2 text-sm"
        />
        <input
          type="date"
          value={endDate}
          readOnly
          onFocus={(e) => (e.currentTarget as HTMLInputElement & { showPicker?: () => void }).showPicker?.()}
          onClick={(e) => (e.currentTarget as HTMLInputElement & { showPicker?: () => void }).showPicker?.()}
          onChange={(e) => dispatch(setDateRange({ startDate, endDate: e.target.value }))}
          className="rounded border px-3 py-2 text-sm"
        />
        <button
          type="button"
          onClick={() => dispatch(loadAnalyticsDashboard({ startDate, endDate }))}
          className="rounded-lg bg-brand px-4 py-2 text-sm text-white"
        >
          Aplicar
        </button>
      </div>

      {status === 'loading' && <LoadingSpinner />}
      {status === 'failed' && error && <ErrorState message={error} />}

      {summary && (
        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div className="rounded-xl border bg-white p-4 shadow-card">
            <p className="text-xs uppercase text-ink-muted">Receita total</p>
            <p className="mt-1 text-2xl font-semibold">{formatBRL(summary.totalRevenue)}</p>
          </div>
          <div className="rounded-xl border bg-white p-4 shadow-card">
            <p className="text-xs uppercase text-ink-muted">Itens vendidos</p>
            <p className="mt-1 text-2xl font-semibold">{summary.totalItemsSold}</p>
          </div>
          <div className="rounded-xl border bg-white p-4 shadow-card">
            <p className="text-xs uppercase text-ink-muted">Pedidos</p>
            <p className="mt-1 text-2xl font-semibold">{summary.orderCount}</p>
          </div>
          <div className="rounded-xl border bg-white p-4 shadow-card">
            <p className="text-xs uppercase text-ink-muted">Ticket médio</p>
            <p className="mt-1 text-2xl font-semibold">{formatBRL(ticket)}</p>
          </div>
        </div>
      )}

      {lineChart && lineChart.labels.length > 0 && (
        <section className="mt-10 rounded-xl border bg-white p-6 shadow-card">
          <h2 className="font-semibold text-lg">Histórico de vendas</h2>
          <SalesLineChart data={lineChart} />
        </section>
      )}

      <div className="mt-10 grid gap-8 lg:grid-cols-2">
        <section className="rounded-xl border bg-white p-6 shadow-card">
          <h2 className="font-semibold">Por livro</h2>
          <ul className="mt-4 space-y-2 text-sm">
            {books.map((b) => (
              <li key={b.bookId} className="flex justify-between">
                <span className="line-clamp-1">{b.title}</span>
                <span className="text-ink-muted">{b.quantitySold} un.</span>
              </li>
            ))}
          </ul>
        </section>
        <section className="rounded-xl border bg-white p-6 shadow-card">
          <h2 className="font-semibold">Por categoria</h2>
          <ul className="mt-4 space-y-2 text-sm">
            {categories.map((c) => (
              <li key={c.category} className="flex justify-between">
                <span>{c.category}</span>
                <span className="text-ink-muted">{formatBRL(c.revenue)}</span>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </div>
  );
}
