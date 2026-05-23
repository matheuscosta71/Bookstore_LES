import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ADMIN_ORDER_STATUS_FILTER } from '@/constants/orderStatusOptions';
import { ROUTES } from '@/constants/routes';
import * as adminExchangeService from '@/services/adminExchangeService';
import * as adminOrderService from '@/services/adminOrderService';
import type { AdminOrderListParams } from '@/services/adminOrderService';
import { getErrorMessage } from '@/services/api';
import {
  adminOrderStatusBadgeClass,
  formatBRL,
  formatDateOnly,
  formatOrderStatus,
  parseMoneyInput,
} from '@/utils/format';
import type { Order } from '@/types/api';

type Filters = {
  orderNumber: string;
  customerName: string;
  status: string;
  dateFrom: string;
  dateTo: string;
  totalMin: string;
  totalMax: string;
};

const emptyFilters: Filters = {
  orderNumber: '',
  customerName: '',
  status: '',
  dateFrom: '',
  dateTo: '',
  totalMin: '',
  totalMax: '',
};

function buildListParams(f: Filters): AdminOrderListParams {
  const totalMin = parseMoneyInput(f.totalMin);
  const totalMax = parseMoneyInput(f.totalMax);
  return {
    page: 0,
    size: 50,
    orderNumber: f.orderNumber.trim() || undefined,
    customerName: f.customerName.trim() || undefined,
    status: f.status || undefined,
    dateFrom: f.dateFrom || undefined,
    dateTo: f.dateTo || undefined,
    totalMin,
    totalMax,
  };
}

export function AdminOrdersPage() {
  const [filters, setFilters] = useState<Filters>(emptyFilters);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState<string | null>(null);
  const [orders, setOrders] = useState<Order[]>([]);
  const [banner, setBanner] = useState<{ type: 'ok' | 'err'; text: string } | null>(null);
  const [detail, setDetail] = useState<Order | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  async function fetchOrders(f: Filters): Promise<Order[]> {
    setListLoading(true);
    setListError(null);
    try {
      const page = await adminOrderService.listOrders(buildListParams(f));
      setOrders(page.content);
      return page.content;
    } catch (e) {
      setListError(getErrorMessage(e));
      return [];
    } finally {
      setListLoading(false);
    }
  }

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setListLoading(true);
      setListError(null);
      try {
        const page = await adminOrderService.listOrders(buildListParams(emptyFilters));
        if (!cancelled) {
          setOrders(page.content);
        }
      } catch (e) {
        if (!cancelled) {
          setListError(getErrorMessage(e));
        }
      } finally {
        if (!cancelled) {
          setListLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  /** RF0041 — autoriza troca pendente vinculada ao pedido (usa API de trocas). */
  async function authorizeExchangeForOrder(orderId: string) {
    setActionLoading(`${orderId}-auth-exchange`);
    setBanner(null);
    try {
      const rows = await adminExchangeService.listExchangeRequests('EM_TROCA');
      const ex = rows.find((r) => r.orderId === orderId && r.exchangeStatus === 'REQUESTED');
      if (!ex) {
        setBanner({
          type: 'err',
          text: 'Não há solicitação de troca pendente de autorização para este pedido.',
        });
        return;
      }
      await adminExchangeService.authorizeExchange(ex.id);
      setBanner({ type: 'ok', text: 'Troca autorizada. O pedido passou para Troca autorizada.' });
      const list = await fetchOrders(filters);
      const updated = list.find((o) => o.id === orderId);
      if (updated) {
        setDetail((d) => (d?.id === orderId ? updated : d));
      }
    } catch (e) {
      setBanner({ type: 'err', text: getErrorMessage(e) });
    } finally {
      setActionLoading(null);
    }
  }

  async function runOnOrder(orderId: string, op: 'approve' | 'reject' | 'dispatch' | 'deliver') {
    const key = `${orderId}-${op}`;
    setActionLoading(key);
    setBanner(null);
    try {
      if (op === 'approve') await adminOrderService.approvePayment(orderId);
      else if (op === 'reject') await adminOrderService.rejectPayment(orderId);
      else if (op === 'dispatch') await adminOrderService.dispatchOrder(orderId);
      else await adminOrderService.deliverOrder(orderId);
      setBanner({ type: 'ok', text: 'Pedido atualizado com sucesso.' });
      const list = await fetchOrders(filters);
      const updated = list.find((o) => o.id === orderId);
      if (updated) {
        setDetail((d) => (d?.id === orderId ? updated : d));
      }
    } catch (e) {
      setBanner({ type: 'err', text: getErrorMessage(e) });
    } finally {
      setActionLoading(null);
    }
  }

  function displayOrderLabel(o: Order): string {
    return o.orderNumber ?? 'Pedido';
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <h1 className="font-display text-3xl font-semibold">Pedidos</h1>
      <p className="mt-1 text-sm text-ink-muted">
        Filtre por número do pedido, cliente, status, período e valor. Com pedido <strong>Em troca</strong>, use{' '}
        <strong>Autorizar troca</strong> (RF0041). Para receber o item e gerar cupom, use a página{' '}
        <Link to={ROUTES.adminExchanges} className="text-brand underline">
          Trocas
        </Link>
        .
      </p>

      {banner && (
        <p
          className={`mt-4 rounded-lg px-3 py-2 text-sm ${
            banner.type === 'ok' ? 'bg-emerald-50 text-emerald-900' : 'bg-red-50 text-red-800'
          }`}
        >
          {banner.text}
        </p>
      )}

      <div className="mt-6 rounded-xl border bg-white p-4 shadow-card">
        <p className="text-sm font-medium text-ink">Filtros</p>
        <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <label className="block text-xs text-ink-muted">
            Nº pedido ou trecho do ID
            <input
              value={filters.orderNumber}
              onChange={(e) => setFilters((s) => ({ ...s, orderNumber: e.target.value }))}
              className="mt-1 w-full rounded border border-stone-300 px-2 py-2 text-sm text-ink"
              placeholder="Ex.: #FE90D ou parte do código"
            />
          </label>
          <label className="block text-xs text-ink-muted">
            Cliente
            <input
              value={filters.customerName}
              onChange={(e) => setFilters((s) => ({ ...s, customerName: e.target.value }))}
              className="mt-1 w-full rounded border border-stone-300 px-2 py-2 text-sm text-ink"
              placeholder="Nome (contém)"
            />
          </label>
          <label className="block text-xs text-ink-muted">
            Status
            <select
              value={filters.status}
              onChange={(e) => setFilters((s) => ({ ...s, status: e.target.value }))}
              className="mt-1 w-full rounded border border-stone-300 px-2 py-2 text-sm text-ink"
            >
              {ADMIN_ORDER_STATUS_FILTER.map((o) => (
                <option key={o.value || 'all'} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </label>
          <label className="block text-xs text-ink-muted">
            Data inicial
            <input
              type="date"
              value={filters.dateFrom}
              onChange={(e) => setFilters((s) => ({ ...s, dateFrom: e.target.value }))}
              className="mt-1 w-full rounded border border-stone-300 px-2 py-2 text-sm text-ink"
            />
          </label>
          <label className="block text-xs text-ink-muted">
            Data final
            <input
              type="date"
              value={filters.dateTo}
              onChange={(e) => setFilters((s) => ({ ...s, dateTo: e.target.value }))}
              className="mt-1 w-full rounded border border-stone-300 px-2 py-2 text-sm text-ink"
            />
          </label>
          <div className="grid grid-cols-2 gap-2">
            <label className="block text-xs text-ink-muted">
              Total mín. (R$)
              <input
                value={filters.totalMin}
                onChange={(e) => setFilters((s) => ({ ...s, totalMin: e.target.value }))}
                className="mt-1 w-full rounded border border-stone-300 px-2 py-2 text-sm text-ink"
                placeholder="0,00"
                inputMode="decimal"
              />
            </label>
            <label className="block text-xs text-ink-muted">
              Total máx. (R$)
              <input
                value={filters.totalMax}
                onChange={(e) => setFilters((s) => ({ ...s, totalMax: e.target.value }))}
                className="mt-1 w-full rounded border border-stone-300 px-2 py-2 text-sm text-ink"
                placeholder="0,00"
                inputMode="decimal"
              />
            </label>
          </div>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => void fetchOrders(filters)}
            className="rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white"
          >
            Buscar
          </button>
          <button
            type="button"
            onClick={() => {
              setFilters(emptyFilters);
              void fetchOrders(emptyFilters);
            }}
            className="rounded-lg border border-stone-300 px-4 py-2 text-sm text-ink"
          >
            Limpar filtros
          </button>
        </div>
      </div>

      <div className="mt-6 overflow-x-auto rounded-xl border bg-white shadow-card">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b bg-stone-50 text-ink-muted">
            <tr>
              <th className="px-4 py-3 font-medium">Pedido</th>
              <th className="px-4 py-3 font-medium">Cliente</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Total</th>
              <th className="px-4 py-3 font-medium">Data</th>
              <th className="px-4 py-3 font-medium text-right">Ações</th>
            </tr>
          </thead>
          <tbody>
            {listLoading && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-ink-muted">
                  Carregando pedidos…
                </td>
              </tr>
            )}
            {!listLoading && listError && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-red-600">
                  {listError}
                </td>
              </tr>
            )}
            {!listLoading &&
              !listError &&
              orders.map((o) => {
                const rowBusy =
                  actionLoading != null &&
                  (actionLoading === `${o.id}-approve` ||
                    actionLoading === `${o.id}-reject` ||
                    actionLoading === `${o.id}-dispatch` ||
                    actionLoading === `${o.id}-deliver` ||
                    actionLoading === `${o.id}-auth-exchange`);
                const canDispatch = o.status === 'APROVADO';
                const canDeliver = o.status === 'EM_TRANSITO';
                const canAuthorizeExchange = o.status === 'EM_TROCA';
                const showExchangeReceiveHint = o.status === 'TROCA_AUTORIZADA';
                return (
                  <tr key={o.id} className="border-b border-stone-100 last:border-0">
                    <td className="px-4 py-3 font-medium text-ink">{displayOrderLabel(o)}</td>
                    <td className="px-4 py-3">{o.customerName ?? '—'}</td>
                    <td className="px-4 py-3">
                      <span className={adminOrderStatusBadgeClass(o.status)}>
                        {formatOrderStatus(o.status)}
                      </span>
                    </td>
                    <td className="px-4 py-3 tabular-nums">{formatBRL(o.totalAmount)}</td>
                    <td className="px-4 py-3 text-ink-muted">{formatDateOnly(o.createdAt)}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap justify-end gap-2">
                        <button
                          type="button"
                          disabled={!!rowBusy}
                          onClick={() => setDetail(o)}
                          className="rounded-lg border border-stone-300 bg-white px-3 py-1.5 text-xs font-medium hover:bg-stone-50 disabled:opacity-50"
                        >
                          Ver
                        </button>
                        {canAuthorizeExchange && (
                          <button
                            type="button"
                            disabled={!!rowBusy}
                            onClick={() => void authorizeExchangeForOrder(o.id)}
                            className="rounded-lg border border-violet-400 bg-violet-50 px-3 py-1.5 text-xs font-medium text-violet-900 disabled:opacity-50"
                            title="RF0041 — autorizar solicitação de troca"
                          >
                            Autorizar troca
                          </button>
                        )}
                        {showExchangeReceiveHint && (
                          <Link
                            to={ROUTES.adminExchanges}
                            className="inline-flex items-center rounded-lg border border-violet-300 bg-white px-3 py-1.5 text-xs font-medium text-violet-900 hover:bg-violet-50"
                          >
                            Receber item
                          </Link>
                        )}
                        <button
                          type="button"
                          disabled={!!rowBusy || !canDispatch}
                          onClick={() => runOnOrder(o.id, 'dispatch')}
                          className="rounded-lg bg-brand px-3 py-1.5 text-xs font-medium text-white disabled:cursor-not-allowed disabled:opacity-40"
                          title={canDispatch ? 'Despachar pedido' : 'Disponível após pagamento aprovado'}
                        >
                          Despachar
                        </button>
                        <button
                          type="button"
                          disabled={!!rowBusy || !canDeliver}
                          onClick={() => runOnOrder(o.id, 'deliver')}
                          className="rounded-lg border border-green-600 bg-green-50 px-3 py-1.5 text-xs font-medium text-green-900 disabled:cursor-not-allowed disabled:opacity-40"
                          title={canDeliver ? 'Confirmar entrega' : 'Disponível quando estiver em trânsito'}
                        >
                          Entregue
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
          </tbody>
        </table>
      </div>

      {detail && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="admin-order-detail-title"
          onClick={() => setDetail(null)}
        >
          <div
            className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-xl border bg-white p-5 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-2">
              <div>
                <h2 id="admin-order-detail-title" className="font-display text-xl font-semibold">
                  {displayOrderLabel(detail)}
                </h2>
                <p className="mt-1 text-sm text-ink-muted">{detail.customerName ?? 'Cliente'}</p>
              </div>
              <button
                type="button"
                className="rounded-lg px-2 py-1 text-sm text-ink-muted hover:bg-stone-100"
                onClick={() => setDetail(null)}
              >
                Fechar
              </button>
            </div>

            <div className="mt-4 flex flex-wrap items-center gap-2">
              <span className={adminOrderStatusBadgeClass(detail.status)}>
                {formatOrderStatus(detail.status)}
              </span>
              <span className="text-sm text-ink-muted">{formatDateOnly(detail.createdAt)}</span>
            </div>
            <p className="mt-3 text-lg font-semibold tabular-nums">{formatBRL(detail.totalAmount)}</p>

            <div className="mt-4 border-t pt-4">
              <p className="text-sm font-medium text-ink">Itens</p>
              <ul className="mt-2 space-y-2 text-sm">
                {detail.items?.map((it) => (
                  <li key={it.id} className="flex justify-between gap-2 border-b border-stone-100 pb-2 last:border-0">
                    <span>
                      {it.title} × {it.quantity}
                    </span>
                    <span className="tabular-nums text-ink-muted">{formatBRL(it.totalPrice)}</span>
                  </li>
                ))}
              </ul>
            </div>

            {detail.status === 'EM_PROCESSAMENTO' && (
              <div className="mt-6 flex flex-wrap gap-2 border-t pt-4">
                <button
                  type="button"
                  disabled={actionLoading === `${detail.id}-approve`}
                  onClick={() => runOnOrder(detail.id, 'approve')}
                  className="rounded-lg bg-emerald-700 px-4 py-2 text-sm text-white disabled:opacity-50"
                >
                  Aprovar pagamento
                </button>
                <button
                  type="button"
                  disabled={actionLoading === `${detail.id}-reject`}
                  onClick={() => runOnOrder(detail.id, 'reject')}
                  className="rounded-lg border border-red-300 bg-red-50 px-4 py-2 text-sm text-red-900 disabled:opacity-50"
                >
                  Rejeitar pagamento
                </button>
              </div>
            )}

            {detail.status === 'EM_TROCA' && (
              <div className="mt-6 flex flex-wrap gap-2 border-t pt-4">
                <button
                  type="button"
                  disabled={actionLoading === `${detail.id}-auth-exchange`}
                  onClick={() => void authorizeExchangeForOrder(detail.id)}
                  className="rounded-lg border border-violet-400 bg-violet-50 px-4 py-2 text-sm font-medium text-violet-900 disabled:opacity-50"
                >
                  Autorizar troca (RF0041)
                </button>
              </div>
            )}

            {detail.status === 'TROCA_AUTORIZADA' && (
              <p className="mt-6 border-t pt-4 text-sm text-ink-muted">
                Para confirmar recebimento do item e gerar o cupom, abra{' '}
                <Link to={ROUTES.adminExchanges} className="font-medium text-brand underline">
                  Trocas
                </Link>{' '}
                e use o filtro “TROCA AUTORIZADA (receber)”.
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
