import { NavLink, Outlet } from 'react-router-dom';
import { cn } from '@/utils/cn';
import { Header } from '@/components/Header';
import { Footer } from '@/components/Footer';

const links = [
  { to: '/profile', label: 'Dados pessoais' },
  { to: '/profile/orders', label: 'Pedidos' },
  { to: '/profile/transactions', label: 'Extrato' },
  { to: '/profile/addresses', label: 'Endereços' },
  { to: '/profile/cards', label: 'Cartões' },
];

export function ProfileLayout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-8 px-4 py-10 md:flex-row">
        <aside className="md:w-56">
          <nav className="flex flex-col gap-1 rounded-xl border border-stone-200 bg-white p-2 shadow-card">
            {links.map((l) => (
              <NavLink
                key={l.to}
                to={l.to}
                end={l.to === '/profile'}
                className={({ isActive }) =>
                  cn(
                    'rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-brand text-white'
                      : 'text-ink-muted hover:bg-stone-100 hover:text-ink',
                  )
                }
              >
                {l.label}
              </NavLink>
            ))}
          </nav>
        </aside>
        <section className="flex-1 rounded-xl border border-stone-200 bg-white p-6 shadow-card">
          <Outlet />
        </section>
      </div>
      <Footer />
    </div>
  );
}
