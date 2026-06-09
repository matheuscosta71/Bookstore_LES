import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { fetchCustomerOrders } from '@/features/customer/customerSlice';
import * as orderService from '@/services/orderService';
import { getErrorMessage } from '@/services/api';
import { formatBRL, formatDateOnly, formatOrderStatus } from '@/utils/format';
import { ROUTES } from '@/constants/routes';

export function ProfileOrdersPage() {
  const dispatch = useAppDispatch();
  const customerId = useAppSelector((s) => s.auth.customerId)!;
  const orders = useAppSelector((s) => s.customer.orders);
  
  const [busyItem, setBusyItem] = useState<string | null>(null);
  const [busyOrder, setBusyOrder] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [exchangeOpenOrderId, setExchangeOpenOrderId] = useState<string | null>(null);
  const [selectedItemIds, setSelectedItemIds] = useState<Record<string, boolean>>({});

  useEffect(() => {
    dispatch(fetchCustomerOrders(customerId));
  }, [dispatch, customerId]);

  async function requestExchange(orderId: string, orderItemId: string) {
    setFeedback(null);
    setError(null);
    setBusyItem(orderItemId);
    try {
      await orderService.createExchangeRequest(customerId, orderId, { orderItemId });
      setFeedback('Solicitação de troca registrada. O pedido passa para EM_TROCA até a loja autorizar.');
      dispatch(fetchCustomerOrders(customerId));
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setBusyItem(null);
    }
  }

  async function requestExchangeBatch(orderId: string) {
    setFeedback(null);
    setError(null);
    
    const itemIds = Object.keys(selectedItemIds).filter((id) => selectedItemIds[id]);
    if (itemIds.length === 0) {
      setError('Por favor, selecione pelo menos um item para devolução.');
      return;
    }

    setBusyOrder(orderId);
    try {
      await orderService.createExchangeRequestsBatch(customerId, orderId, { orderItemIds: itemIds });
      setFeedback('Solicitação de troca/devolução registrada para os itens selecionados. O pedido passa para EM_TROCA até a loja autorizar.');
      setExchangeOpenOrderId(null);
      setSelectedItemIds({});
      dispatch(fetchCustomerOrders(customerId));
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setBusyOrder(null);
    }
  }

  function startExchangeFlow(order: any) {
    setFeedback(null);
    setError(null);
    setExchangeOpenOrderId(order.id);
    const initialSelected: Record<string, boolean> = {};
    order.items.forEach((it: any) => {
      if (!it.exchangeRequested) {
        initialSelected[it.id] = true;
      }
    });
    setSelectedItemIds(initialSelected);
  }

  function toggleSelectAll(order: any, checked: boolean) {
    const updated: Record<string, boolean> = {};
    order.items.forEach((it: any) => {
      if (!it.exchangeRequested) {
        updated[it.id] = checked;
      }
    });
    setSelectedItemIds(updated);
  }

  return (
    <div>
      <h1 className="font-display text-2xl font-semibold">Meus pedidos</h1>
      <p className="mt-1 text-sm text-ink-muted">
        Troca: permitida apenas para pedido recebido, podendo devolver itens individualmente.
      </p>
      {feedback && <p className="mt-3 text-sm text-green-700">{feedback}</p>}
      {error && <p className="mt-3 text-sm text-red-600">{error}</p>}

      <ul className="mt-6 space-y-4">
        {orders.map((o) => (
          <li key={o.id} className="rounded-lg border p-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <p className="font-medium">{o.orderNumber ?? `Pedido ${o.id.slice(0, 8)}…`}</p>
                <p className="text-sm text-ink-muted">{formatDateOnly(o.createdAt)}</p>
                <p className="text-sm">
                  Status: <span className="font-medium">{formatOrderStatus(o.status)}</span>
                </p>
              </div>
              <div className="text-right">
                <p className="font-semibold text-brand">{formatBRL(o.totalAmount)}</p>
                <Link to={ROUTES.books} className="text-sm text-brand hover:underline">
                  Comprar de novo
                </Link>
                {o.exchangeCouponCode ? (
                  <p className="mt-2 text-xs text-emerald-800">
                    Cupom de troca: <span className="font-mono font-semibold">{o.exchangeCouponCode}</span>
                  </p>
                ) : null}
              </div>
            </div>
            {o.items && o.items.length > 0 && (
              <>
                <ul className="mt-3 space-y-2 border-t border-stone-100 pt-3 text-sm">
                  {o.items.map((it) => (
                    <li key={it.id} className="flex flex-wrap items-center justify-between gap-2">
                      <span>
                        {it.title} × {it.quantity} — {formatBRL(it.totalPrice)}
                        {it.exchangeRequested ? (
                          <span className="ml-2 text-xs text-ink-muted">(troca solicitada)</span>
                        ) : null}
                      </span>
                      {(o.status === 'ENTREGUE' || o.status === 'EM_TROCA' || o.status === 'TROCA_AUTORIZADA') && !it.exchangeRequested && (
                        <button
                          type="button"
                          disabled={busyItem === it.id}
                          onClick={() => void requestExchange(o.id, it.id)}
                          className="rounded border border-brand px-2 py-1 text-xs text-brand hover:bg-brand hover:text-white disabled:opacity-50"
                        >
                          Solicitar troca
                        </button>
                      )}
                    </li>
                  ))}
                </ul>

                {/* Fluxo de devolução em lote/parcial */}
                {(o.status === 'ENTREGUE' || o.status === 'EM_TROCA' || o.status === 'TROCA_AUTORIZADA') && (
                  (() => {
                    const eligibleItems = o.items.filter((it) => !it.exchangeRequested);
                    if (eligibleItems.length === 0) return null;

                    return (
                      <div className="mt-3 pt-3 border-t border-stone-100">
                        {exchangeOpenOrderId !== o.id ? (
                          <div className="flex justify-end">
                            <button
                              type="button"
                              onClick={() => startExchangeFlow(o)}
                              className="rounded border border-brand bg-brand/5 hover:bg-brand hover:text-white px-3 py-1.5 text-xs font-medium text-brand transition"
                            >
                              Solicitar Devolução (Vários Itens / Pedido Completo)
                            </button>
                          </div>
                        ) : (
                          <div className="mt-2 border border-brand/20 bg-brand/5 rounded-lg p-4 transition-all">
                            <p className="font-semibold text-sm text-stone-800">Selecione os itens para devolução/troca:</p>
                            
                            <div className="mt-3 space-y-2">
                              <label className="flex items-center gap-2 text-xs font-medium text-stone-700 cursor-pointer">
                                <input
                                  type="checkbox"
                                  checked={eligibleItems.every((it) => selectedItemIds[it.id])}
                                  onChange={(e) => toggleSelectAll(o, e.target.checked)}
                                  className="rounded text-brand focus:ring-brand"
                                />
                                <span>Selecionar todos (Devolver pedido completo)</span>
                              </label>
                              
                              <div className="border-t border-stone-200/50 my-2 pt-2 space-y-2">
                                {eligibleItems.map((it) => (
                                  <label key={it.id} className="flex items-center justify-between p-1 rounded hover:bg-stone-100 cursor-pointer text-xs">
                                    <div className="flex items-center gap-2">
                                      <input
                                        type="checkbox"
                                        checked={!!selectedItemIds[it.id]}
                                        onChange={(e) => setSelectedItemIds(prev => ({ ...prev, [it.id]: e.target.checked }))}
                                        className="rounded text-brand focus:ring-brand"
                                      />
                                      <span>{it.title} × {it.quantity}</span>
                                    </div>
                                    <span className="font-medium text-stone-600">{formatBRL(it.totalPrice)}</span>
                                  </label>
                                ))}
                              </div>
                            </div>

                            <div className="mt-4 flex justify-end gap-2">
                              <button
                                type="button"
                                onClick={() => setExchangeOpenOrderId(null)}
                                className="rounded border border-stone-300 bg-white hover:bg-stone-50 px-3 py-1.5 text-xs font-medium text-stone-700 transition"
                              >
                                Cancelar
                              </button>
                              <button
                                type="button"
                                disabled={busyOrder === o.id || Object.values(selectedItemIds).every(val => !val)}
                                onClick={() => void requestExchangeBatch(o.id)}
                                className="rounded bg-brand hover:bg-brand/90 px-3 py-1.5 text-xs font-medium text-white transition disabled:opacity-50"
                              >
                                {busyOrder === o.id ? 'Processando...' : 'Confirmar Devolução'}
                              </button>
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  })()
                )}
              </>
            )}
          </li>
        ))}
      </ul>
      {orders.length === 0 && <p className="mt-4 text-ink-muted">Nenhum pedido ainda.</p>}
    </div>
  );
}
