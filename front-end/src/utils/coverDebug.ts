/**
 * Logs de diagnóstico: API Spring (`/books`) devolve ISBN; capas vêm de Open Library + Google Books.
 * Só em DEV + ingest opcional (mesma sessão que BookCover).
 */
export function dbgCoverApi(
  hypothesisId: string,
  location: string,
  message: string,
  data: Record<string, unknown>,
): void {
  if (!import.meta.env.DEV) return;
  console.info(`[covers-api] ${hypothesisId} — ${message}`, { location, ...data });
  // #region agent log
  fetch('http://127.0.0.1:7297/ingest/c6a8463e-416b-4c93-9bc4-166b78a1d291', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Debug-Session-Id': 'bca6aa' },
    body: JSON.stringify({
      sessionId: 'bca6aa',
      runId: 'post-fix',
      hypothesisId,
      location,
      message,
      data,
      timestamp: Date.now(),
    }),
  }).catch(() => {});
  // #endregion
}
