import { Link } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';

export function Footer() {
  return (
    <footer className="mt-auto border-t border-stone-200 bg-white">
      <div className="mx-auto flex max-w-7xl flex-col gap-6 px-4 py-10 md:flex-row md:justify-between">
        <div>
          <p className="font-display text-lg font-semibold text-brand">Livraria BookStore</p>
          <p className="mt-1 max-w-sm text-sm text-ink-muted">
            Seu próximo livro favorito está a um clique. Catálogo, recomendações Smart IA e entrega com carinho.
          </p>
        </div>
        <div className="flex gap-10 text-sm">
          <div className="flex flex-col gap-2">
            <span className="font-semibold text-ink">Loja</span>
            <Link to={ROUTES.books} className="text-ink-muted hover:text-brand">
              Catálogo
            </Link>
            <Link to={ROUTES.aiChat} className="text-ink-muted hover:text-brand">
              Assistente IA
            </Link>
          </div>
          <div className="flex flex-col gap-2">
            <span className="font-semibold text-ink">Conta</span>
            <Link to={ROUTES.login} className="text-ink-muted hover:text-brand">
              Entrar
            </Link>
            <Link to={ROUTES.register} className="text-ink-muted hover:text-brand">
              Cadastro
            </Link>
          </div>
        </div>
      </div>
      <div className="border-t border-stone-100 py-4 text-center text-xs text-ink-subtle">
        © {new Date().getFullYear()} BookStore — Projeto acadêmico
      </div>
    </footer>
  );
}
