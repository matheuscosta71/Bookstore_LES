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
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

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

  return (
    <div>
      <h1 className="font-display text-2xl font-semibold">Meus pedidos</h1>
      <p className="mt-1 text-sm text-ink-muted">
        Troca: permitida apenas para pedido <strong>ENTREGUE</strong>, um item por vez (regras da API).
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
              <ul className="mt-3 space-y-2 border-t border-stone-100 pt-3 text-sm">
                {o.items.map((it) => (
                  <li key={it.id} className="flex flex-wrap items-center justify-between gap-2">
                    <span>
                      {it.title} × {it.quantity} — {formatBRL(it.totalPrice)}
                      {it.exchangeRequested ? (
                        <span className="ml-2 text-xs text-ink-muted">(troca solicitada)</span>
                      ) : null}
                    </span>
                    {o.status === 'ENTREGUE' && !it.exchangeRequested && (
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
            )}
          </li>
        ))}
      </ul>
      {orders.length === 0 && <p className="mt-4 text-ink-muted">Nenhum pedido ainda.</p>}
    </div>
  );
}
