import type { Book } from '@/types/api';
import { BookRecommendationCard } from '@/components/BookRecommendationCard';

type ParsedAiEntry = {
  title: string;
  author?: string;
  reason: string;
};

function stripFormatting(text: string): string {
  return text.replace(/\*\*/g, '').replace(/\*/g, '').replace(/\r/g, '');
}

function normalizeText(text: string): string {
  return stripFormatting(text)
    .toLowerCase()
    .normalize('NFD')
    // remove marcas diacriticas apos NFD (suporte amplo)
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9 ]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function parseAiEntries(text: string): ParsedAiEntry[] {
  const lines = stripFormatting(text).split('\n');
  const entries: ParsedAiEntry[] = [];

  const itemStart = /^(\d+)[\.\)]\s*(.+)$/;
  const bulletStart = /^[-*]\s*(.+)$/;
  const dash = /\s*(?:—|-|–)\s*/;

  let current: { title: string; author?: string; reason: string[] } | null = null;

  for (const raw of lines) {
    const line = raw.trim();
    if (!line) continue;

    const startMatch = itemStart.exec(line);
    const bulletMatch = !startMatch ? bulletStart.exec(line) : null;
    if (startMatch || bulletMatch) {
      if (current) {
        entries.push({
          title: current.title,
          author: current.author,
          reason: current.reason.join(' ').trim(),
        });
      }

      const payload = startMatch ? startMatch[2] : bulletMatch?.[1] ?? '';
      const cleaned = payload
        .replace(/^["“](.+)["”]\s*/g, '$1')
        .replace(/["“]/g, '')
        .replace(/["”]/g, '');

      // tenta extrair "Title — Author"
      const parts = cleaned.split(dash).map((p) => p.trim());
      const title = parts[0] ?? '';
      const author = parts.length > 1 ? parts[1] : undefined;

      if (title) current = { title, author, reason: [] };
      continue;
    }

    // linha de "reason" (continuação)
    if (current) {
      const cleaned = line.replace(/^[-*]\s*/, '').trim();
      if (cleaned) current.reason.push(cleaned);
    }
  }

  if (current) {
    entries.push({
      title: current.title,
      author: current.author,
      reason: current.reason.join(' ').trim(),
    });
  }

  return entries.filter((e) => e.title);
}

function matchBookByTitle(entryTitle: string, books: Book[]): Book | null {
  if (!entryTitle.trim()) return null;
  const entryN = normalizeText(entryTitle);
  if (!entryN) return null;

  // variações comuns: "1984 / Nineteen Eighty-Four"
  return books.find((b) => {
    const bookN = normalizeText(b.title ?? '');
    if (!bookN) return false;
    return bookN.includes(entryN) || entryN.includes(bookN);
  }) ?? null;
}

function shortReason(reason: string, fallback: string): string {
  const r = reason.trim();
  if (!r) return fallback;
  const cleaned = r.replace(/^\s*[-*]\s*/g, '');
  if (cleaned.length <= 140) return cleaned;
  return `${cleaned.slice(0, 137).trim()}…`;
}

type Props = {
  books: Book[];
  aiText: string | null;
  loading?: boolean;
  variant?: 'page' | 'sidebar';
  maxCards?: number;
  anchorId?: string;
};

export function RecommendationSection({
  books,
  aiText,
  loading,
  variant = 'page',
  maxCards,
  anchorId,
}: Props) {
  if (!aiText) return null;
  const parsed = parseAiEntries(aiText);

  const fallbackReason = 'Sugestao baseada no seu perfil e no seu historico.';

  // cria uma lista de cards com correspondencia por titulo
  const matched = parsed
    .map((e) => {
      const b = matchBookByTitle(e.title, books);
      if (!b) return null;
      return { book: b, reason: shortReason(e.reason, fallbackReason) };
    })
    .filter(Boolean) as Array<{ book: Book; reason: string }>;

  // se nao conseguiu casar com nenhum livro, usa um fallback visual com os primeiros livros
  const toRender =
    matched.length > 0
      ? matched.slice(0, maxCards ?? 6)
      : books.slice(0, maxCards ?? 6).map((b) => ({ book: b, reason: fallbackReason }));

  const isSidebar = variant === 'sidebar';

  return (
    <section
      className={isSidebar ? 'w-full' : 'mx-auto max-w-7xl px-4 py-10'}
      aria-label="Recomendações Smart IA de livros"
    >
      <div className={isSidebar ? 'px-1' : 'flex items-end justify-between gap-4'}>
        <div>
          <h2
            className={
              isSidebar ? 'font-display text-lg font-semibold tracking-tight text-ink' : 'font-display text-2xl font-semibold tracking-tight text-ink md:text-3xl'
            }
          >
            Recomendações Smart IA
          </h2>
          <p className={isSidebar ? 'mt-1 text-xs text-ink-muted' : 'mt-1 text-sm text-ink-muted'}>
            Baseado no seu histórico de compras e preferências.
          </p>
        </div>
      </div>

      {loading && <p className={isSidebar ? 'mt-3 px-1 text-xs text-ink-muted' : 'mt-4 text-sm text-ink-muted'}>Gerando recomendações Smart IA…</p>}

      <div className={isSidebar ? 'mt-4 px-1' : 'mt-6'} id={anchorId ?? (isSidebar ? undefined : 'recommendations')}>
        {isSidebar ? (
          <div className="max-h-[70vh] overflow-y-auto pr-1">
            <div className="grid gap-3 sm:grid-cols-1">
              {toRender.map(({ book, reason }) => (
                <BookRecommendationCard key={book.id} book={book} reason={reason} />
              ))}
            </div>
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {toRender.map(({ book, reason }) => (
              <BookRecommendationCard key={book.id} book={book} reason={reason} />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

