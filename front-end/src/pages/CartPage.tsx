import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { loadCart, removeFromCart, updateCartLine, addToCart } from '@/features/cart/cartSlice';
import { CartItemCard } from '@/components/CartItemCard';
import { ExpiredCartItemCard } from '@/components/ExpiredCartItemCard';
import { OrderSummary } from '@/components/OrderSummary';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { EmptyState } from '@/components/EmptyState';
import { ROUTES } from '@/constants/routes';
import { formatBRL } from '@/utils/format';
import { fetchBookById } from '@/services/booksService';
import { validateCartLineQuantity } from '@/validators/cartValidator';
import { readReservationNotice } from '@/utils/reservationNoticeStorage';

export function CartPage() {
  const dispatch = useAppDispatch();
  const customerId = useAppSelector((s) => s.auth.customerId)!;
  const { cart, status, mutationStatus, error } = useAppSelector((s) => s.cart);
  const [bookMetaByBookId, setBookMetaByBookId] = useState<
    Record<string, { stockQuantity: number; isbn: string }>
  >({});
  const [lineErrors, setLineErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    dispatch(loadCart(customerId));
  }, [dispatch, customerId]);

  const activeItems = cart?.items.filter((i) => !i.expired) ?? [];

  /** ISBN e estoque vêm da API de livros (itens do carrinho não trazem ISBN). */
  useEffect(() => {
    const rows = cart?.items ?? [];
    if (rows.length === 0) {
      setBookMetaByBookId({});
      return;
    }
    const ids = [...new Set(rows.map((i) => i.bookId))];
    let cancelled = false;
    void (async () => {
      const entries = await Promise.all(
        ids.map(async (bookId) => {
          try {
            const b = await fetchBookById(bookId);
            return [bookId, { stockQuantity: b.stockQuantity, isbn: b.isbn ?? '' }] as const;
          } catch {
            return [bookId, { stockQuantity: 0, isbn: '' }] as const;
          }
        }),
      );
      if (!cancelled) setBookMetaByBookId(Object.fromEntries(entries));
    })();
    return () => {
      cancelled = true;
    };
  }, [cart?.items]);

  /** API manda mensagens só na carga em que houve purge; nas seguintes lemos sessionStorage (RNF0042). */
  const { reservationExpiredMessages, reservationExpirationMinutes } = useMemo(() => {
    const fromCart = cart?.reservationExpiredMessages ?? [];
    if (fromCart.length > 0 && cart) {
      return {
        reservationExpiredMessages: fromCart,
        reservationExpirationMinutes: cart.itemExpirationMinutes,
      };
    }
    const persisted = readReservationNotice(customerId);
    if (persisted?.messages?.length) {
      return {
        reservationExpiredMessages: persisted.messages,
        reservationExpirationMinutes: persisted.itemExpirationMinutes,
      };
    }
    return {
      reservationExpiredMessages: [] as string[],
      reservationExpirationMinutes: cart?.itemExpirationMinutes ?? 30,
    };
  }, [cart, customerId]);

  const expiredItems = cart?.items.filter((i) => i.expired) ?? [];
  const subtotal = activeItems.reduce((s, i) => s + Number(i.totalPrice), 0);
  const freight = cart?.freightAmount != null ? Number(cart.freightAmount) : null;
  const total =
    freight != null ? subtotal + freight : subtotal;

  if (status === 'loading' && !cart) return <LoadingSpinner />;

  const cartEmptySucceeded = Boolean(cart && cart.items.length === 0 && status === 'succeeded');

  if (cartEmptySucceeded && reservationExpiredMessages.length === 0) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16">
        <EmptyState
          title="Seu carrinho está vazio"
          description="Explore o catálogo e adicione seus favoritos."
          action={
            <Link
              to={ROUTES.books}
              className="inline-block rounded-full bg-brand px-6 py-2 text-sm font-medium text-white"
            >
              Ver livros
            </Link>
          }
        />
      </div>
    );
  }

  if (cartEmptySucceeded && reservationExpiredMessages.length > 0 && cart) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16">
        <div className="mb-8 rounded-xl border border-amber-300 bg-amber-50 px-4 py-4 text-amber-950 shadow-sm">
          <h2 className="font-display text-lg font-semibold text-amber-950">Retirados por prazo de reserva</h2>
          <p className="mt-1 text-sm text-amber-900">
            O prazo para finalizar a compra ({reservationExpirationMinutes} min) expirou. Os itens abaixo foram
            removidos do carrinho por tempo — não por falta de estoque.
          </p>
          <ul className="mt-3 list-inside list-disc space-y-2 text-sm text-amber-950">
            {reservationExpiredMessages.map((m) => (
              <li key={m}>{m}</li>
            ))}
          </ul>
        </div>
        <EmptyState
          title="Seu carrinho está vazio"
          description="Adicione novamente pelo catálogo os livros que deseja comprar."
          action={
            <Link
              to={ROUTES.books}
              className="inline-block rounded-full bg-brand px-6 py-2 text-sm font-medium text-white"
            >
              Ver livros
            </Link>
          }
        />
      </div>
    );
  }

  if (!cart) return <LoadingSpinner />;

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <div className="flex flex-wrap items-baseline justify-between gap-3">
        <h1 className="font-display text-3xl font-semibold">Carrinho de compras</h1>
        <Link
          to={ROUTES.books}
          className="text-sm font-medium text-brand hover:underline"
        >
          Continuar comprando →
        </Link>
      </div>
      {reservationExpiredMessages.length > 0 && expiredItems.length === 0 && (
        <div className="mt-4 rounded-xl border border-amber-300 bg-amber-50 px-4 py-4 text-amber-950 shadow-sm">
          <h2 className="font-display text-lg font-semibold text-amber-950">Aviso de reserva (sessão anterior)</h2>
          <p className="mt-1 text-sm text-amber-900">
            Histórico de quando itens foram retirados por prazo ({reservationExpirationMinutes} min). Os itens atuais
            aparecem abaixo se ainda estiverem no carrinho.
          </p>
          <ul className="mt-3 list-inside list-disc space-y-2 text-sm text-amber-950">
            {reservationExpiredMessages.map((m) => (
              <li key={m}>{m}</li>
            ))}
          </ul>
        </div>
      )}
      {cart.stockAdjustmentMessages && cart.stockAdjustmentMessages.length > 0 && (
        <div className="mt-4 space-y-2 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-950">
          {cart.stockAdjustmentMessages.map((m) => (
            <p key={m}>{m}</p>
          ))}
        </div>
      )}
      {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
      {activeItems.length > 0 && Object.keys(bookMetaByBookId).length === 0 && (
        <p className="mt-2 text-xs text-ink-muted">Sincronizando estoques…</p>
      )}

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_320px]">
        <div className="space-y-4">
          {activeItems.map((item) => (
            <CartItemCard
              key={item.id}
              item={item}
              busy={mutationStatus === 'loading'}
              isbn={bookMetaByBookId[item.bookId]?.isbn}
              maxQuantity={bookMetaByBookId[item.bookId]?.stockQuantity}
              lineError={lineErrors[item.id]}
              itemExpirationMinutes={cart.itemExpirationMinutes}
              onReservationElapsed={() => dispatch(loadCart(customerId))}
              onQuantityChange={(q) => {
                const stock = bookMetaByBookId[item.bookId]?.stockQuantity;
                if (stock === undefined) {
                  if (q > item.quantity) {
                    setLineErrors((prev) => ({
                      ...prev,
                      [item.id]: 'Aguarde a sincronização do estoque antes de aumentar a quantidade.',
                    }));
                    return;
                  }
                } else {
                  const v = validateCartLineQuantity({ quantity: q, availableStock: stock });
                  if (!v.valid && v.errors.quantity) {
                    setLineErrors((prev) => ({ ...prev, [item.id]: v.errors.quantity! }));
                    return;
                  }
                }
                setLineErrors((prev) => {
                  const next = { ...prev };
                  delete next[item.id];
                  return next;
                });
                void dispatch(
                  updateCartLine({
                    customerId,
                    itemId: item.id,
                    bookId: item.bookId,
                    quantity: q,
                  }),
                );
              }}
              onRemove={() => dispatch(removeFromCart({ customerId, itemId: item.id }))}
            />
          ))}

          {expiredItems.length > 0 && (
            <div className="mt-8">
              <h2 className="font-semibold text-amber-900">Itens expirados</h2>
              <p className="text-sm text-amber-800">
                Não entram no total e não permitem checkout. Use &quot;Adicionar novamente ao carrinho&quot; para renovar
                o prazo ou remova a linha.
              </p>
              <div className="mt-4 space-y-3">
                {expiredItems.map((item) => (
                  <ExpiredCartItemCard
                    key={item.id}
                    item={item}
                    isbn={bookMetaByBookId[item.bookId]?.isbn}
                    expirationMinutes={cart.itemExpirationMinutes}
                    busy={mutationStatus === 'loading'}
                    onReadd={() =>
                      dispatch(
                        addToCart({
                          customerId,
                          bookId: item.bookId,
                          quantity: item.quantity,
                        }),
                      )
                    }
                    onRemove={() => dispatch(removeFromCart({ customerId, itemId: item.id }))}
                  />
                ))}
              </div>
            </div>
          )}
        </div>

        <aside className="space-y-4">
          <OrderSummary itemsSubtotal={subtotal} freight={freight} total={total} />
          {cart.hasExpiredItems && (
            <p className="rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">
              Há itens expirados. O checkout fica bloqueado até você readicionar ou remover esses itens.
            </p>
          )}
          <Link
            to={ROUTES.checkout}
            className={`block w-full rounded-xl py-3 text-center text-sm font-semibold text-white ${
              cart.checkoutAllowed ? 'bg-brand hover:bg-brand-light' : 'cursor-not-allowed bg-stone-400'
            }`}
            onClick={(e) => !cart.checkoutAllowed && e.preventDefault()}
          >
            Ir para checkout
          </Link>
          {!cart.checkoutAllowed && (
            <p className="text-center text-xs text-red-600">Checkout desabilitado enquanto houver itens expirados.</p>
          )}
          <p className="text-xs text-ink-muted">
            Frete estimado: {freight != null ? formatBRL(freight) : 'calculado no checkout'}
          </p>
        </aside>
      </div>
    </div>
  );
}
