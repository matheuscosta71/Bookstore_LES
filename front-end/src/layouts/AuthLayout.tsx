import { Outlet } from 'react-router-dom';
import { Link } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';

export function AuthLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-gradient-to-br from-brand-soft via-paper to-paper-accent">
      <header className="border-b border-stone-200/80 bg-white/80 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4">
          <Link to={ROUTES.home} className="font-display text-xl font-semibold text-brand">
            Livraria BookStore
          </Link>
          <Link to={ROUTES.home} className="text-sm text-ink-muted hover:text-brand">
            Voltar à loja
          </Link>
        </div>
      </header>
      <div className="flex flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-md rounded-2xl border border-stone-200/80 bg-white p-8 shadow-card">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
