import { Link, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { logout } from '@/features/auth/authSlice';
import { resetCart } from '@/features/cart/cartSlice';
import { resetCheckout } from '@/features/checkout/checkoutSlice';
import { resetCustomerState } from '@/features/customer/customerSlice';
import { ROUTES } from '@/constants/routes';
import { UserAvatar } from '@/components/UserAvatar';
import { cn } from '@/utils/cn';
import { formatPersonDisplayName } from '@/utils/personNameDisplay';
import { hasValidAdminSession } from '@/utils/adminSession';
import { clearReservationNotice } from '@/utils/reservationNoticeStorage';

export function Header() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const customerId = useAppSelector((s) => s.auth.customerId);
  const user = useAppSelector((s) => s.auth.user);
  const cart = useAppSelector((s) => s.cart.cart);
  const [q, setQ] = useState('');
  const [adminNav, setAdminNav] = useState(() => hasValidAdminSession());
  const itemCount = cart?.items?.filter((i) => !i.expired).length ?? 0;

  useEffect(() => {
    const sync = () => setAdminNav(hasValidAdminSession());
    sync();
    window.addEventListener('storage', sync);
    window.addEventListener('focus', sync);
    return () => {
      window.removeEventListener('storage', sync);
      window.removeEventListener('focus', sync);
    };
  }, []);

  function onSearch(e: React.FormEvent) {
    e.preventDefault();
    const params = new URLSearchParams();
    if (q.trim()) params.set('title', q.trim());
    navigate(`${ROUTES.books}?${params.toString()}`);
  }

  function handleLogout() {
    if (customerId) clearReservationNotice(customerId);
    dispatch(resetCart());
    dispatch(resetCheckout());
    dispatch(resetCustomerState());
    dispatch(logout());
    navigate(ROUTES.login, { replace: true });
  }

  const rawName = user?.fullName?.trim();
  const displayName = rawName ? formatPersonDisplayName(rawName) : 'Minha conta';

  return (
    <header className="sticky top-0 z-40 border-b border-stone-200/90 bg-white/95 backdrop-blur">
      <div className="mx-auto flex max-w-7xl flex-col gap-3 px-4 py-3 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-8">
          <Link to={ROUTES.home} className="font-display text-xl font-semibold tracking-tight text-brand">
            BookStore
          </Link>
          <nav className="hidden items-center gap-5 text-sm font-medium text-ink-muted md:flex">
            <Link className="hover:text-brand" to={ROUTES.home}>
              Home
            </Link>
            <Link className="hover:text-brand" to={ROUTES.books}>
              Livros
            </Link>
            <Link className="hover:text-brand" to={ROUTES.aiChat}>
              Chat IA
            </Link>
            {adminNav && (
              <Link className="hover:text-brand" to={ROUTES.admin}>
                Admin
              </Link>
            )}
          </nav>
        </div>
        <form onSubmit={onSearch} className="flex flex-1 items-center gap-2 md:max-w-md">
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Buscar livros, autores, ISBN..."
            className={cn(
              'w-full rounded-full border border-stone-200 bg-paper px-4 py-2 text-sm',
              'placeholder:text-ink-subtle focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20',
            )}
          />
          <button
            type="submit"
            className="rounded-full bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-light"
          >
            Buscar
          </button>
        </form>
        <div className="flex flex-wrap items-center justify-end gap-2 md:gap-3">
          {customerId ? (
            <>
              <Link
                to={ROUTES.profile}
                className={cn(
                  'flex max-w-[14rem] items-center gap-2 rounded-full border border-stone-200 py-1 pl-1 text-sm font-medium text-ink',
                  'pr-1 sm:pr-3',
                  'transition-colors hover:border-brand hover:text-brand',
                  'min-h-[2.25rem]',
                )}
                aria-label={`Perfil: ${displayName}`}
                title={displayName}
              >
                <UserAvatar
                  fullName={rawName ? formatPersonDisplayName(rawName) : undefined}
                  email={user?.email}
                  seed={customerId}
                  size="sm"
                />
                <span className="hidden min-w-0 truncate sm:inline">{displayName}</span>
              </Link>
              <Link
                to={ROUTES.cart}
                className="relative rounded-full border border-stone-200 px-3 py-1.5 text-sm font-medium hover:border-brand"
              >
                Carrinho
                {itemCount > 0 && (
                  <span className="absolute -right-1 -top-1 flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-accent px-1 text-xs text-white">
                    {itemCount}
                  </span>
                )}
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className="rounded-full border border-stone-300 px-3 py-1.5 text-sm font-medium text-ink hover:border-brand hover:text-brand"
              >
                Sair
              </button>
            </>
          ) : (
            <>
              <Link to={ROUTES.login} className="text-sm font-medium text-ink-muted hover:text-brand">
                Entrar
              </Link>
              <Link
                to={ROUTES.register}
                className="rounded-full bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-light"
              >
                Cadastrar
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
