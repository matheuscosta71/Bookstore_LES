import { useState } from 'react';
import * as auditService from '@/services/auditService';
import { getErrorMessage } from '@/services/api';
import type { AuditLogRow } from '@/types/api';
import { Pagination } from '@/components/Pagination';
import { formatDate } from '@/utils/format';

export function AdminAuditPage() {
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [rows, setRows] = useState<AuditLogRow[]>([]);
  const [entityName, setEntityName] = useState('');
  const [entityId, setEntityId] = useState('');
  const [entityRows, setEntityRows] = useState<AuditLogRow[] | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function loadList(p: number) {
    setErr(null);
    setLoading(true);
    try {
      const res = await auditService.listAuditLogs(p, 20);
      setRows(res.content);
      setTotalPages(res.totalPages);
      setPage(res.number);
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  async function loadByEntity() {
    setErr(null);
    const name = entityName.trim();
    const id = entityId.trim();
    if (!name || !id) {
      setErr('Informe entityName e entityId (UUID).');
      return;
    }
    setLoading(true);
    try {
      const list = await auditService.listAuditLogsByEntity(name, id);
      setEntityRows(list);
    } catch (e) {
      setErr(getErrorMessage(e));
      setEntityRows(null);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-10">
      <div>
        <h1 className="font-display text-3xl font-semibold">Auditoria</h1>
        <p className="mt-1 text-sm text-ink-muted">Logs globais e por entidade.</p>
      </div>

      {err && <p className="text-sm text-red-600">{err}</p>}
      {loading && <p className="text-sm text-ink-muted">Carregando…</p>}

      <section className="rounded-xl border bg-white p-4 shadow-card">
        <h2 className="font-semibold">Todos os logs</h2>
        <button
          type="button"
          onClick={() => void loadList(0)}
          className="mt-3 rounded-lg bg-brand px-4 py-2 text-sm text-white"
        >
          Carregar página 1
        </button>
        <ul className="mt-4 space-y-2 text-sm">
          {rows.map((r) => (
            <li key={r.id} className="rounded border p-2">
              <span className="font-mono text-xs">{r.actionType}</span> · {r.entityName} ·{' '}
              {r.changedBy ?? '—'} · {formatDate(r.changedAt)}
              {r.changedData && (
                <pre className="mt-1 max-h-24 overflow-auto text-xs text-ink-muted">{r.changedData}</pre>
              )}
            </li>
          ))}
        </ul>
        <Pagination page={page} totalPages={totalPages} onPageChange={(p) => void loadList(p)} />
      </section>

      <section className="rounded-xl border bg-white p-4 shadow-card">
        <h2 className="font-semibold">Por entidade</h2>
        <div className="mt-3 flex flex-wrap gap-2">
          <input
            value={entityName}
            onChange={(e) => setEntityName(e.target.value)}
            placeholder="entityName (ex.: SalesOrder)"
            className="min-w-[180px] rounded border px-3 py-2 text-sm"
          />
          <input
            value={entityId}
            onChange={(e) => setEntityId(e.target.value)}
            placeholder="UUID"
            className="min-w-[240px] rounded border px-3 py-2 font-mono text-sm"
          />
          <button
            type="button"
            onClick={() => void loadByEntity()}
            className="rounded-lg bg-brand px-4 py-2 text-sm text-white"
          >
            Buscar
          </button>
        </div>
        {entityRows && (
          <ul className="mt-4 space-y-2 text-sm">
            {entityRows.map((r) => (
              <li key={r.id} className="rounded border p-2">
                <span className="font-mono text-xs">{r.actionType}</span> · {formatDate(r.changedAt)}
                {r.changedData && (
                  <pre className="mt-1 max-h-24 overflow-auto text-xs text-ink-muted">{r.changedData}</pre>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
