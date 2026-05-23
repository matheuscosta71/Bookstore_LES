import { useCallback, useEffect, useState } from 'react';
import * as adminExchangeService from '@/services/adminExchangeService';
import type { ExchangeListOrderStatus } from '@/services/adminExchangeService';
import { getErrorMessage } from '@/services/api';
import type { ExchangeRequestResponse } from '@/types/api';
import { formatDate, formatExchangeStatus, formatOrderStatus } from '@/utils/format';

const STATUS_OPTIONS: { value: ExchangeListOrderStatus; label: string }[] = [
  { value: 'EM_TROCA', label: 'EM_TROCA (autorizar)' },
  { value: 'TROCA_AUTORIZADA', label: 'TROCA_AUTORIZADA (receber)' },
];

export function AdminExchangesPage() {
  const [status, setStatus] = useState<ExchangeListOrderStatus>('EM_TROCA');
  const [rows, setRows] = useState<ExchangeRequestResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await adminExchangeService.listExchangeRequests(status);
      setRows(data);
    } catch (e) {
      setError(getErrorMessage(e));
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [status]);

  useEffect(() => {
    void load();
  }, [load]);

  async function authorize(id: string) {
    setBusyId(id);
    setError(null);
    setSuccessMsg(null);
    try {
      await adminExchangeService.authorizeExchange(id);
      setSuccessMsg('Troca autorizada. O pedido passou para o status correspondente na API.');
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setBusyId(null);
    }
  }

  async function receive(id: string, returnToStock: boolean) {
    setBusyId(id);
    setError(null);
    setSuccessMsg(null);
    try {
      const res = await adminExchangeService.receiveExchange(id, { returnToStock });
      const code = res.generatedCouponCode;
      setSuccessMsg(
        code
          ? `Recebimento registrado. Cupom de troca gerado: ${code}`
          : 'Recebimento registrado.',
      );
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div>
      <h1 className="font-display text-3xl font-semibold">Trocas</h1>
      <p className="mt-1 text-sm text-ink-muted">
        Lista filtrada pelo status do pedido. Autorize em EM_TROCA; confira recebimento em TROCA_AUTORIZADA.
      </p>

      <div className="mt-6 flex flex-wrap items-end gap-3">
        <label className="text-sm font-medium">
          Status do pedido
          <select
            value={status}
            onChange={(e) => {
              setSuccessMsg(null);
              setStatus(e.target.value as ExchangeListOrderStatus);
            }}
            className="mt-1 block rounded border px-3 py-2 text-sm"
          >
            {STATUS_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </label>
        <button
          type="button"
          onClick={() => void load()}
          className="rounded-lg bg-brand px-4 py-2 text-sm text-white"
        >
          Atualizar
        </button>
      </div>

      {loading && <p className="mt-4 text-sm text-ink-muted">Carregando…</p>}
      {successMsg && <p className="mt-4 text-sm text-green-800">{successMsg}</p>}
      {error && <p className="mt-4 text-sm text-red-600">{error}</p>}

      <ul className="mt-6 space-y-3">
        {rows.map((r) => (
          <li key={r.id} className="rounded-lg border bg-white p-4 shadow-card">
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div>
                <p className="font-medium">{r.bookTitle ?? 'Livro'}</p>
                <p className="text-xs text-ink-muted">Troca #{r.id.slice(0, 8)}</p>
                <p className="text-sm text-ink-muted">
                  Status troca: {formatExchangeStatus(r.exchangeStatus)}
                  {r.exchangeStatus ? <span className="text-xs"> ({r.exchangeStatus})</span> : null}
                </p>
                {r.orderStatus && (
                  <p className="text-sm text-ink-muted">
                    Status pedido: {formatOrderStatus(r.orderStatus)}{' '}
                    <span className="text-xs">({r.orderStatus})</span>
                  </p>
                )}
                <p className="font-mono text-xs text-ink-muted">Pedido: {r.orderId}</p>
                {r.createdAt && (
                  <p className="text-xs text-ink-muted">{formatDate(r.createdAt)}</p>
                )}
                {r.generatedCouponCode && (
                  <p className="mt-1 text-sm text-brand">Cupom: {r.generatedCouponCode}</p>
                )}
              </div>
              <div className="flex flex-wrap gap-2">
                {status === 'EM_TROCA' && r.exchangeStatus === 'REQUESTED' && (
                  <button
                    type="button"
                    disabled={busyId === r.id}
                    onClick={() => authorize(r.id)}
                    className="rounded-lg bg-brand px-3 py-1.5 text-sm text-white disabled:opacity-50"
                  >
                    Autorizar
                  </button>
                )}
                {status === 'TROCA_AUTORIZADA' && r.exchangeStatus === 'AUTHORIZED' && (
                  <>
                    <button
                      type="button"
                      disabled={busyId === r.id}
                      onClick={() => receive(r.id, true)}
                      className="rounded-lg border border-brand px-3 py-1.5 text-sm text-brand disabled:opacity-50"
                    >
                      Receber + estoque
                    </button>
                    <button
                      type="button"
                      disabled={busyId === r.id}
                      onClick={() => receive(r.id, false)}
                      className="rounded-lg border px-3 py-1.5 text-sm disabled:opacity-50"
                    >
                      Receber sem estoque
                    </button>
                  </>
                )}
              </div>
            </div>
          </li>
        ))}
      </ul>

      {!loading && rows.length === 0 && (
        <p className="mt-6 text-sm text-ink-muted">Nenhuma solicitação para este filtro.</p>
      )}
    </div>
  );
}
