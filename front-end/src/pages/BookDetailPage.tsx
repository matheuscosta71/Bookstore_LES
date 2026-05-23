import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { fetchBookDetail, fetchBooksPage } from '@/features/books/booksSlice';
import { addToCart, loadCart } from '@/features/cart/cartSlice';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorState } from '@/components/ErrorState';
import { QuantitySelector } from '@/components/QuantitySelector';
import { BookGrid } from '@/components/BookGrid';
import { BookCover } from '@/components/BookCover';
import { formatBRL } from '@/utils/format';
import { ROUTES } from '@/constants/routes';
import { getErrorMessage } from '@/services/api';
import { validateAddToCartQuantity } from '@/validators/cartValidator';
import { appLogger } from '@/utils/appLogger';

export function BookDetailPage() {
  const { id } = useParams<{ id: string }>();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { detail, detailStatus, error, list } = useAppSelector((s) => s.books);
  const customerId = useAppSelector((s) => s.auth.customerId);
  const cart = useAppSelector((s) => s.cart.cart);
  const [qty, setQty] = useState(1);
  const [addError, setAddError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    if (id) dispatch(fetchBookDetail(id));
  }, [dispatch, id]);

  useEffect(() => {
    dispatch(fetchBooksPage({ page: 0, size: 8 }));
  }, [dispatch]);

  useEffect(() => {
    if (customerId) dispatch(loadCart(customerId));
  }, [dispatch, customerId]);

  const inCartQty = useMemo(() => {
    if (!id) return 0;
    return (
      cart?.items?.filter((i) => i.bookId === id && !i.expired).reduce((s, i) => s + i.quantity, 0) ?? 0
    );
  }, [id, cart?.items]);

  /** Só usar estoque se o detalhe no Redux for do livro da rota (evita valor do livro anterior durante troca de página). */
  const stockQuantity =
    id && detail?.id === id ? Math.max(0, Math.floor(Number(detail.stockQuantity) || 0)) : 0;

  const remainingForAdd = Math.max(0, stockQuantity - inCartQty);

  useEffect(() => {
    setQty(1);
  }, [id]);

  useEffect(() => {
    setQty((q) => {
      if (remainingForAdd <= 0) return 1;
      return Math.min(Math.max(1, q), remainingForAdd);
    });
  }, [remainingForAdd]);

  const related = list.filter((b) => b.id !== id).slice(0, 4);

  async function handleAdd() {
    if (!customerId || !id) {
      navigate(ROUTES.login);
      return;
    }
    setAddError(null);
    const stock =
      detail && detail.id === id ? Math.max(0, Math.floor(Number(detail.stockQuantity) || 0)) : 0;
    const remainingClamped = Math.max(0, stock - inCartQty);
    const v = validateAddToCartQuantity(qty, remainingClamped);
    if (!v.valid && v.errors.quantity) {
      appLogger.warn('BookDetailPage', 'handleAdd', 'Validação de quantidade falhou', { bookId: id });
      setAddError(v.errors.quantity);
      return;
    }
    setAdding(true);
    try {
      await dispatch(addToCart({ customerId, bookId: id, quantity: qty })).unwrap();
      appLogger.info('BookDetailPage', 'handleAdd', 'Item adicionado ao carrinho', { bookId: id, quantity: qty });
      navigate(ROUTES.cart);
    } catch (e) {
      setAddError(getErrorMessage(e));
    } finally {
      setAdding(false);
    }
  }

  if (detailStatus === 'loading' || !id) return <LoadingSpinner />;
  if (detailStatus === 'failed' || !detail) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-12">
        <ErrorState message={error ?? 'Livro não encontrado'} />
        <Link to={ROUTES.books} className="mt-4 inline-block text-brand hover:underline">
          Voltar ao catálogo
        </Link>
      </div>
    );
  }

  const b = detail;
  const canAddFromDetail = remainingForAdd > 0 && b.active;

  return (
    <div className="mx-auto max-w-7xl px-4 py-10">
      <nav className="text-sm text-ink-muted">
        <Link to={ROUTES.home} className="hover:text-brand">
          Início
        </Link>
        <span className="mx-2">/</span>
        <Link to={ROUTES.books} className="hover:text-brand">
          Livros
        </Link>
        {b.category && (
          <>
            <span className="mx-2">/</span>
            <span>{b.category}</span>
          </>
        )}
        <span className="mx-2">/</span>
        <span className="text-ink">{b.title}</span>
      </nav>

      <div className="mt-8 grid gap-10 lg:grid-cols-[280px_1fr]">
        <div className="relative aspect-[2/3] overflow-hidden rounded-xl bg-stone-100">
          <BookCover isbn={b.isbn} title={b.title} className="absolute inset-0 rounded-xl" />
        </div>
        <div>
          <h1 className="font-display text-3xl font-semibold text-ink md:text-4xl">{b.title}</h1>
          {b.author && <p className="mt-2 text-lg text-ink-muted">Autor: {b.author}</p>}
          {b.category && <p className="text-ink-muted">Categoria: {b.category}</p>}
          <p className="mt-2 text-sm text-ink-muted">ISBN: {b.isbn}</p>
          {b.code && <p className="text-sm text-ink-muted">Código: {b.code}</p>}
          <p className="mt-6 text-3xl font-semibold text-brand">{formatBRL(b.price)}</p>
          <p className="mt-2 text-sm">
            <span className="text-ink-muted">Estoque: </span>
            {b.stockQuantity > 0 ? (
              <span className="font-semibold text-green-700">
                {b.stockQuantity} {b.stockQuantity === 1 ? 'unidade' : 'unidades'}
              </span>
            ) : (
              <span className="font-semibold text-red-600">0 unidades (indisponível)</span>
            )}
          </p>
          {customerId && (
            <p className="mt-1 text-sm text-ink-muted">
              {inCartQty > 0 ? (
                <>
                  No carrinho: {inCartQty} un. · Pode adicionar agora:{' '}
                  <strong className="text-ink">{remainingForAdd}</strong> un.
                </>
              ) : (
                <>
                  Nada no carrinho — pode adicionar até <strong className="text-ink">{remainingForAdd}</strong> un.
                </>
              )}
            </p>
          )}

          <div className="mt-8 flex flex-wrap items-center gap-4">
            <QuantitySelector
              value={qty}
              min={1}
              max={remainingForAdd > 0 ? remainingForAdd : 1}
              onChange={setQty}
              disabled={!canAddFromDetail}
            />
            <button
              type="button"
              onClick={handleAdd}
              disabled={adding || !canAddFromDetail}
              className="rounded-full bg-accent px-6 py-3 text-sm font-semibold text-white hover:bg-accent-hover disabled:opacity-50"
            >
              Adicionar ao carrinho
            </button>
            <button
              type="button"
              onClick={handleAdd}
              disabled={adding || !canAddFromDetail}
              className="rounded-full border border-brand px-6 py-3 text-sm font-semibold text-brand hover:bg-brand hover:text-white disabled:opacity-50"
            >
              Comprar agora
            </button>
          </div>
          {addError && <p className="mt-3 text-sm text-red-600">{addError}</p>}
          {!customerId && (
            <p className="mt-3 text-sm text-ink-muted">
              <Link to={ROUTES.login} className="text-brand underline">
                Entre
              </Link>{' '}
              para adicionar ao carrinho.
            </p>
          )}
        </div>
      </div>

      <section className="mt-14">
        <h2 className="font-display text-xl font-semibold">Sinopse</h2>
        <p className="mt-3 max-w-3xl text-ink-muted leading-relaxed">
          {b.synopsis?.trim() ? b.synopsis.trim() : 'Sinopse não disponível.'}
        </p>
      </section>

      {related.length > 0 && (
        <section className="mt-12">
          <h2 className="font-display text-xl font-semibold">Quem viu este livro também gostou</h2>
          <div className="mt-4">
            <BookGrid books={related} />
          </div>
        </section>
      )}
    </div>
  );
}
