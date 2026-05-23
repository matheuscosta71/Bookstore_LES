import { useEffect, useState } from 'react';
import {
  GOOGLE_BOOKS_PLACEHOLDER_PNG_BYTES,
  googleBooksCoverUrl,
  normalizeIsbnForCover,
  openLibraryCoverUrlDefaultFalse,
} from '@/utils/openLibrary';
import { cn } from '@/utils/cn';

type Props = {
  isbn: string;
  title: string;
  className?: string;
  imgClassName?: string;
};

type CoverTier = 'openlibrary' | 'google';

function dbgBookCover(
  hypothesisId: string,
  message: string,
  data: Record<string, unknown>,
): void {
  if (import.meta.env.DEV) {
    console.info(`[covers-debug] ${hypothesisId} — ${message}`, data);
  }
  // #region agent log
  fetch('http://127.0.0.1:7297/ingest/c6a8463e-416b-4c93-9bc4-166b78a1d291', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Debug-Session-Id': 'bca6aa' },
    body: JSON.stringify({
      sessionId: 'bca6aa',
      runId: 'post-fix',
      hypothesisId,
      location: 'BookCover.tsx',
      message,
      data,
      timestamp: Date.now(),
    }),
  }).catch(() => {});
  // #endregion
}

export function BookCover({ isbn, title, className, imgClassName }: Props) {
  const [tier, setTier] = useState<CoverTier>('openlibrary');
  const [failed, setFailed] = useState(false);
  const olSrc = openLibraryCoverUrlDefaultFalse(isbn);
  const gbSrc = googleBooksCoverUrl(isbn);
  const src = tier === 'openlibrary' ? olSrc : gbSrc;

  // #region agent log
  useEffect(() => {
    setFailed(false);
    setTier('openlibrary');
    const normalized = normalizeIsbnForCover(isbn ?? '');
    dbgBookCover('H1', 'cover mount: isbn and derived url', {
      isbn,
      normalized,
      openLibraryUrl: olSrc,
      googleUrl: gbSrc,
      activeTier: 'openlibrary',
      title,
      viaDevProxy: import.meta.env.DEV,
    });
    if (!isbn?.trim()) {
      dbgBookCover('H4', 'empty isbn — url may be invalid', { olSrc, gbSrc });
    }
  }, [isbn, title, olSrc, gbSrc]);

  useEffect(() => {
    dbgBookCover('H-STATE', 'render state snapshot', {
      isbn,
      title,
      tier,
      failed,
      activeSrc: src,
      willShowImage: !failed,
    });
  }, [isbn, title, tier, failed, src]);
  // #endregion

  if (failed) {
    return (
      <div
        className={cn(
          'flex h-full w-full items-center justify-center bg-gradient-to-br from-brand-soft to-stone-100 p-4 text-center font-display text-sm font-medium text-brand/80',
          className,
        )}
      >
        {title}
      </div>
    );
  }

  return (
    <div className={cn('relative h-full w-full overflow-hidden bg-stone-100', className)}>
      <img
        key={`${isbn}-${tier}`}
        src={src}
        alt=""
        referrerPolicy="no-referrer"
        loading="lazy"
        decoding="async"
        className={cn('block h-full w-full object-cover transition group-hover:scale-[1.02]', imgClassName)}
        onError={() => {
          if (tier === 'openlibrary') {
            // #region agent log
            dbgBookCover('H-OL', 'open library miss (404) — try google', {
              isbn,
              olSrc,
              nextTier: 'google',
              nextSrc: gbSrc,
            });
            // #endregion
            setTier('google');
            return;
          }
          // #region agent log
          dbgBookCover('H2', 'img onError (load failed)', {
            fullCoverUrl: src,
            title,
            isbn,
            tier,
            willSetFailed: true,
          });
          // #endregion
          setFailed(true);
        }}
        onLoad={(e) => {
          const el = e.currentTarget;
          const nw = el.naturalWidth;
          const nh = el.naturalHeight;
          const resolved = el.currentSrc;
          // #region agent log
          dbgBookCover('H3', 'img onLoad (bytes received)', {
            fullCoverUrl: resolved,
            title,
            isbn,
            tier,
            naturalWidth: nw,
            naturalHeight: nh,
          });
          // #endregion

          if (tier === 'openlibrary' && nw <= 1 && nh <= 1) {
            dbgBookCover('H3-1x1', 'open library tiny image — try google', {
              isbn,
              nw,
              nh,
              nextSrc: gbSrc,
            });
            setTier('google');
            return;
          }

          const canProbeGooglePlaceholder =
            import.meta.env.DEV &&
            tier === 'google' &&
            (resolved.includes('/gb-cover') || el.src.includes('/gb-cover'));

          if (!canProbeGooglePlaceholder && tier === 'google') {
            dbgBookCover('H-GB-skip', 'placeholder byte check skipped', {
              isbn,
              reason: import.meta.env.DEV ? 'not /gb-cover path' : 'production build (no same-origin proxy)',
              resolved,
              dev: import.meta.env.DEV,
            });
          }

          if (canProbeGooglePlaceholder) {
            void (async () => {
              const t0 = performance.now();
              dbgBookCover('H-GB-fetch-start', 'GET blob to compare size with Google placeholder', {
                isbn,
                url: resolved,
                expectedPlaceholderBytes: GOOGLE_BOOKS_PLACEHOLDER_PNG_BYTES,
              });
              try {
                const r = await fetch(resolved);
                const ct = r.headers.get('content-type') ?? '';
                dbgBookCover('H-GB-fetch-meta', 'fetch response headers', {
                  isbn,
                  ok: r.ok,
                  status: r.status,
                  contentType: ct,
                  ms: Math.round(performance.now() - t0),
                });
                const b = await r.blob();
                const match = b.size === GOOGLE_BOOKS_PLACEHOLDER_PNG_BYTES;
                dbgBookCover('H-GB-fetch-body', 'blob size vs placeholder constant', {
                  isbn,
                  blobBytes: b.size,
                  expectedBytes: GOOGLE_BOOKS_PLACEHOLDER_PNG_BYTES,
                  matchesPlaceholder: match,
                });
                if (match) {
                  dbgBookCover('H-GB-ph', 'google books placeholder PNG — title fallback', {
                    isbn,
                    bytes: b.size,
                    action: 'setFailed(true)',
                  });
                  setFailed(true);
                } else {
                  dbgBookCover('H-GB-ok', 'google blob differs from placeholder — keep image', {
                    isbn,
                    blobBytes: b.size,
                  });
                }
              } catch (err) {
                dbgBookCover('H-GB-fetch-err', 'fetch failed — keep img as rendered', {
                  isbn,
                  message: err instanceof Error ? err.message : String(err),
                });
              }
            })();
          }
        }}
      />
    </div>
  );
}
