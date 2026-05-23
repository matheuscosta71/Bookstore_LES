import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { fetchBooksPage } from '@/features/books/booksSlice';
import { fetchAiRecommendations } from '@/features/ai/aiSlice';
import { BookGrid } from '@/components/BookGrid';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { RecommendationCarousel } from '@/components/RecommendationCarousel';
import { HeroSection } from '@/components/HeroSection';
import { RecommendationSection } from '@/components/RecommendationSection';
import { ROUTES } from '@/constants/routes';

export function HomePage() {
  const dispatch = useAppDispatch();
  const { list, listStatus } = useAppSelector((s) => s.books);
  const customerId = useAppSelector((s) => s.auth.customerId);
  const recText = useAppSelector((s) => s.ai.recommendationText);
  const recStatus = useAppSelector((s) => s.ai.recStatus);

  useEffect(() => {
    dispatch(fetchBooksPage({ page: 0, size: 12 }));
  }, [dispatch]);

  useEffect(() => {
    if (customerId) {
      dispatch(fetchAiRecommendations(customerId));
    }
  }, [dispatch, customerId]);

  const bestsellers = list.slice(0, 5);
  const categories = Array.from(
    new Set(list.map((b) => b.category).filter(Boolean) as string[]),
  ).slice(0, 6);

  const catalogBlock = (
    <>
      {listStatus === 'loading' && <LoadingSpinner />}
      {listStatus === 'succeeded' && (
        <>
          <RecommendationCarousel
            title="Mais vendidos"
            subtitle="Seleção em destaque no catálogo"
            books={bestsellers}
          />
          {categories.length > 0 && (
            <section className="py-10">
              <h2 className="font-display text-2xl font-semibold text-ink">Categorias</h2>
              <div className="mt-4 flex flex-wrap gap-2">
                {categories.map((c) => (
                  <Link
                    key={c}
                    to={`${ROUTES.books}?category=${encodeURIComponent(c)}`}
                    className="rounded-full border border-stone-200 bg-white px-4 py-2 text-sm hover:border-brand hover:text-brand"
                  >
                    {c}
                  </Link>
                ))}
              </div>
            </section>
          )}
          <BookGrid books={list.slice(0, 8)} />
          <div className="mt-8 text-center">
            <Link
              to={ROUTES.books}
              className="inline-flex rounded-full border border-brand px-6 py-2 text-sm font-medium text-brand hover:bg-brand hover:text-white"
            >
              Ver todos os livros
            </Link>
          </div>
        </>
      )}
    </>
  );

  return (
    <div>
      <HeroSection isLoggedIn={!!customerId} />

      {customerId ? (
        <div className="mx-auto max-w-7xl px-4 py-10">
          {/*
            Mobile: flex-col-reverse — catálogo em cima, IA embaixo.
            lg: grid — sidebar esquerda (IA) + main (catálogo).
          */}
          <div className="flex flex-col-reverse gap-10 lg:grid lg:grid-cols-[minmax(260px,340px)_minmax(0,1fr)] lg:items-start lg:gap-10">
            <aside className="w-full shrink-0 border-stone-200 lg:sticky lg:top-24 lg:max-h-[calc(100vh-5.5rem)] lg:self-start lg:border-r lg:pr-6">
              <div className="hidden lg:block">
                {recStatus === 'loading' && !recText && (
                  <p className="text-sm text-ink-muted">Gerando recomendações Smart IA…</p>
                )}
                {recText && (
                  <RecommendationSection
                    variant="sidebar"
                    books={list}
                    aiText={recText}
                    loading={recStatus === 'loading'}
                    maxCards={4}
                    anchorId="recommendations"
                  />
                )}
              </div>
              <div className="lg:hidden">
                {recText && (
                  <RecommendationSection
                    variant="page"
                    books={list}
                    aiText={recText}
                    loading={false}
                    maxCards={6}
                    anchorId={undefined}
                  />
                )}
                {recStatus === 'loading' && !recText && (
                  <p className="text-sm text-ink-muted">Gerando recomendações Smart IA…</p>
                )}
              </div>
            </aside>

            <main className="min-w-0">{catalogBlock}</main>
          </div>
        </div>
      ) : (
        <div className="mx-auto max-w-7xl px-4 py-10">{catalogBlock}</div>
      )}

      <section className="border-t border-stone-200 bg-white py-12">
        <div className="mx-auto max-w-7xl px-4 text-center">
          <h2 className="font-display text-2xl font-semibold text-ink">Não sabe o que ler?</h2>
          <p className="mt-2 text-ink-muted">
            Pergunte à assistente: “Quero um livro leve sobre produtividade”
          </p>
          <Link
            to={ROUTES.aiChat}
            className="mt-6 inline-block rounded-full bg-accent px-6 py-3 text-sm font-semibold text-white hover:bg-accent-hover"
          >
            Ir para o chat
          </Link>
        </div>
      </section>
    </div>
  );
}
