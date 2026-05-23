import { Link } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';

type Props = {
  isLoggedIn: boolean;
};

export function HeroSection({ isLoggedIn }: Props) {
  return (
    <section className="border-b border-stone-200 bg-gradient-to-br from-brand via-brand-light to-brand text-white">
      <div className="mx-auto flex max-w-7xl flex-col gap-8 px-4 py-16 md:flex-row md:items-center md:justify-between md:py-20">
        <div className="max-w-2xl">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-white/70">Livraria com IA</p>
          <h1 className="mt-3 font-display text-4xl font-semibold leading-tight text-balance md:text-6xl">
            Encontre seu próximo livro com recomendações inteligentes
          </h1>
          <p className="mt-4 max-w-xl text-base leading-relaxed text-white/90 md:text-lg">
            Descubra livros rapidamente na home e use o assistente para aprofundar, comparar e tirar dúvidas.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link
              to={isLoggedIn ? '#recommendations' : ROUTES.books}
              className="rounded-full bg-white px-6 py-3 text-sm font-semibold text-brand shadow-lg hover:bg-paper"
            >
              {isLoggedIn ? 'Receber recomendações Smart IA' : 'Encontrar meu próximo livro'}
            </Link>
              <Link
                to={ROUTES.aiChat}
                className="rounded-full border border-white/40 px-6 py-3 text-sm font-semibold text-white hover:bg-white/10"
              >
                Conversar com assistente
              </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
