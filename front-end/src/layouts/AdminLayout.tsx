import { NavLink, Outlet, Link } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import { cn } from '@/utils/cn';
import { STORAGE_ADMIN_ROLE, STORAGE_ADMIN_TOKEN, STORAGE_ADMIN_USERNAME } from '@/constants/storageKeys';

const nav = [
  { to: ROUTES.adminAnalytics, label: 'Analytics' },
  { to: ROUTES.adminOrders, label: 'Pedidos' },
  { to: ROUTES.adminExchanges, label: 'Trocas' },
  { to: ROUTES.adminInventory, label: 'Estoque' },
  { to: ROUTES.adminAudit, label: 'Auditoria' },
  { to: ROUTES.adminBooks, label: 'Livros' },
  { to: ROUTES.adminCustomers, label: 'Clientes' },
  { to: ROUTES.adminCoupons, label: 'Cupons' },
] as const;

export function AdminLayout() {
  const username = typeof window !== 'undefined' ? localStorage.getItem(STORAGE_ADMIN_USERNAME) : null;
  const role = typeof window !== 'undefined' ? localStorage.getItem(STORAGE_ADMIN_ROLE) : null;

  function logoutAdmin() {
    localStorage.removeItem(STORAGE_ADMIN_TOKEN);
    localStorage.removeItem(STORAGE_ADMIN_USERNAME);
    localStorage.removeItem(STORAGE_ADMIN_ROLE);
    window.location.href = ROUTES.loginWithAdminTab;
  }

  return (
    <div className="min-h-screen bg-paper">
      <header className="border-b border-stone-200 bg-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3">
          <div>
            <span className="font-display text-lg font-semibold text-brand">Admin</span>
            {(username || role) && (
              <p className="text-xs text-ink-muted">
                {username ?? 'admin'}{role ? ` · ${role}` : ''}
              </p>
            )}
          </div>
          <div className="flex items-center gap-3">
            <Link to={ROUTES.home} className="text-sm text-ink-muted hover:text-brand">
              ← Loja
            </Link>
            <button type="button" onClick={logoutAdmin} className="text-sm text-ink-muted hover:text-brand">
              Sair
            </button>
          </div>
        </div>
      </header>
      <div className="mx-auto flex max-w-7xl flex-col gap-8 px-4 py-8 md:flex-row">
        <nav className="flex shrink-0 flex-row flex-wrap gap-2 md:w-52 md:flex-col md:gap-1">
          {nav.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                cn(
                  'rounded-lg px-3 py-2 text-sm font-medium',
                  isActive ? 'bg-brand text-white' : 'text-ink-muted hover:bg-stone-100 hover:text-ink',
                )
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>
        <main className="min-w-0 flex-1">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
