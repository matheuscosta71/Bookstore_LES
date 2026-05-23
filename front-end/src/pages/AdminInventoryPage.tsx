import { useState } from 'react';
import * as inventoryService from '@/services/inventoryService';
import { getErrorMessage } from '@/services/api';
import type { InventoryBookRow, InventoryMovementRow } from '@/types/api';
import { Pagination } from '@/components/Pagination';
import { formatDate } from '@/utils/format';

export function AdminInventoryPage() {
  const [bookLookupId, setBookLookupId] = useState('');
  const [bookRow, setBookRow] = useState<InventoryBookRow | null>(null);
  const [entry, setEntry] = useState({
    bookId: '',
    quantity: 1,
    unitCost: 0,
    reason: 'ADJUSTMENT' as 'PURCHASE' | 'ADJUSTMENT' | 'OTHER',
  });
  const [moveParams, setMoveParams] = useState({
    bookId: '',
    movementType: '' as '' | 'ENTRY' | 'SALE_OUTBOUND' | 'EXCHANGE_RETURN',
    startDate: '',
    endDate: '',
    page: 0,
  });
  const [movements, setMovements] = useState<{
    content: InventoryMovementRow[];
    totalPages: number;
  }>({ content: [], totalPages: 0 });
  const [orderOutbound, setOrderOutbound] = useState('');
  const [exchangeReentry, setExchangeReentry] = useState('');
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function lookupBook() {
    setErr(null);
    setMsg(null);
    const id = bookLookupId.trim();
    if (!id) return;
    setLoading(true);
    try {
      const row = await inventoryService.getInventoryByBook(id);
      setBookRow(row);
    } catch (e) {
      setErr(getErrorMessage(e));
      setBookRow(null);
    } finally {
      setLoading(false);
    }
  }

  async function submitEntry() {
    setErr(null);
    setMsg(null);
    setLoading(true);
    try {
      await inventoryService.postInventoryEntry({
        bookId: entry.bookId.trim(),
        quantity: entry.quantity,
        unitCost: entry.unitCost,
        reason: entry.reason,
      });
      setMsg('Entrada registrada.');
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  async function loadMovements(pageIndex: number) {
    setErr(null);
    setMsg(null);
    setLoading(true);
    try {
      const page = await inventoryService.listInventoryMovements({
        bookId: moveParams.bookId.trim() || undefined,
        movementType: moveParams.movementType || undefined,
        startDate: moveParams.startDate || undefined,
        endDate: moveParams.endDate || undefined,
        page: pageIndex,
        size: 20,
      });
      setMovements({ content: page.content, totalPages: page.totalPages });
      setMoveParams((s) => ({ ...s, page: pageIndex }));
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  async function salesOut() {
    setErr(null);
    setMsg(null);
    const id = orderOutbound.trim();
    if (!id) return;
    setLoading(true);
    try {
      await inventoryService.postSalesOutbound(id);
      setMsg('Baixa por pedido aplicada.');
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  async function reentry() {
    setErr(null);
    setMsg(null);
    const id = exchangeReentry.trim();
    if (!id) return;
    setLoading(true);
    try {
      await inventoryService.postExchangeReentry(id);
      setMsg('Reentrada por troca processada.');
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-10">
      <div>
        <h1 className="font-display text-3xl font-semibold">Estoque</h1>
        <p className="mt-1 text-sm text-ink-muted">
          RF0051 entrada manual · RF0053 baixa por pedido (checkout aplica automaticamente) · RF0054 reentrada por
          troca (reprocesso idempotente). Preço de venda por custo + grupo: RF0052 na gestão de livros.
        </p>
      </div>

      {err && <p className="text-sm text-red-600">{err}</p>}
      {msg && <p className="text-sm text-green-700">{msg}</p>}

      <section className="rounded-xl border bg-white p-4 shadow-card">
        <h2 className="font-semibold">Saldo por livro</h2>
        <div className="mt-3 flex flex-wrap gap-2">
          <input
            value={bookLookupId}
            onChange={(e) => setBookLookupId(e.target.value)}
            className="min-w-[240px] flex-1 rounded border px-3 py-2 font-mono text-sm"
            placeholder="UUID do livro"
          />
          <button
            type="button"
            disabled={loading}
            onClick={() => void lookupBook()}
            className="rounded-lg bg-brand px-4 py-2 text-sm text-white"
          >
            Consultar
          </button>
        </div>
        {bookRow && (
          <div className="mt-3 text-sm">
            <p className="font-medium">{bookRow.title}</p>
            <p className="text-ink-muted">ISBN: {bookRow.isbn}</p>
            <p>Disponível: {bookRow.quantityAvailable}</p>
            {bookRow.lastUpdatedAt && (
              <p className="text-xs text-ink-muted">{formatDate(bookRow.lastUpdatedAt)}</p>
            )}
          </div>
        )}
      </section>

      <section className="rounded-xl border bg-white p-4 shadow-card">
        <h2 className="font-semibold">Entrada manual</h2>
        <div className="mt-3 grid gap-3 sm:grid-cols-2">
          <label className="text-sm">
            Livro (UUID)
            <input
              value={entry.bookId}
              onChange={(e) => setEntry((s) => ({ ...s, bookId: e.target.value }))}
              className="mt-1 w-full rounded border px-3 py-2 font-mono text-sm"
            />
          </label>
          <label className="text-sm">
            Quantidade
            <input
              type="number"
              min={1}
              value={entry.quantity}
              onChange={(e) => setEntry((s) => ({ ...s, quantity: Number(e.target.value) }))}
              className="mt-1 w-full rounded border px-3 py-2 text-sm"
            />
          </label>
          <label className="text-sm">
            Custo unitário
            <input
              type="number"
              step="0.01"
              value={entry.unitCost}
              onChange={(e) => setEntry((s) => ({ ...s, unitCost: Number(e.target.value) }))}
              className="mt-1 w-full rounded border px-3 py-2 text-sm"
            />
          </label>
          <label className="text-sm">
            Motivo
            <select
              value={entry.reason}
              onChange={(e) =>
                setEntry((s) => ({
                  ...s,
                  reason: e.target.value as typeof entry.reason,
                }))
              }
              className="mt-1 w-full rounded border px-3 py-2 text-sm"
            >
              <option value="PURCHASE">PURCHASE</option>
              <option value="ADJUSTMENT">ADJUSTMENT</option>
              <option value="OTHER">OTHER</option>
            </select>
          </label>
        </div>
        <button
          type="button"
          disabled={loading}
          onClick={() => void submitEntry()}
          className="mt-4 rounded-lg bg-brand px-4 py-2 text-sm text-white"
        >
          Registrar entrada
        </button>
      </section>

      <section className="rounded-xl border bg-white p-4 shadow-card">
        <h2 className="font-semibold">Movimentações</h2>
        <div className="mt-3 flex flex-wrap gap-2">
          <input
            value={moveParams.bookId}
            onChange={(e) => setMoveParams((s) => ({ ...s, bookId: e.target.value }))}
            className="rounded border px-3 py-2 font-mono text-sm"
            placeholder="bookId (opcional)"
          />
          <select
            value={moveParams.movementType}
            onChange={(e) =>
              setMoveParams((s) => ({
                ...s,
                movementType: e.target.value as typeof moveParams.movementType,
              }))
            }
            className="rounded border px-3 py-2 text-sm"
          >
            <option value="">Tipo (todos)</option>
            <option value="ENTRY">ENTRY</option>
            <option value="SALE_OUTBOUND">SALE_OUTBOUND</option>
            <option value="EXCHANGE_RETURN">EXCHANGE_RETURN</option>
          </select>
          <input
            type="date"
            value={moveParams.startDate}
            readOnly
            onFocus={(e) => (e.currentTarget as HTMLInputElement & { showPicker?: () => void }).showPicker?.()}
            onClick={(e) => (e.currentTarget as HTMLInputElement & { showPicker?: () => void }).showPicker?.()}
            onChange={(e) => setMoveParams((s) => ({ ...s, startDate: e.target.value }))}
            className="rounded border px-3 py-2 text-sm"
          />
          <input
            type="date"
            value={moveParams.endDate}
            readOnly
            onFocus={(e) => (e.currentTarget as HTMLInputElement & { showPicker?: () => void }).showPicker?.()}
            onClick={(e) => (e.currentTarget as HTMLInputElement & { showPicker?: () => void }).showPicker?.()}
            onChange={(e) => setMoveParams((s) => ({ ...s, endDate: e.target.value }))}
            className="rounded border px-3 py-2 text-sm"
          />
          <button
            type="button"
            disabled={loading}
            onClick={() => void loadMovements(0)}
            className="rounded-lg bg-brand px-4 py-2 text-sm text-white"
          >
            Listar
          </button>
        </div>
        <ul className="mt-4 space-y-2 text-sm">
          {movements.content.map((m) => (
            <li key={m.id} className="rounded border p-2">
              <span className="font-mono text-xs">{m.movementType}</span> · {m.bookTitle ?? m.bookId} ·{' '}
              {m.quantity} un. · {formatDate(m.createdAt)}
            </li>
          ))}
        </ul>
        <Pagination
          page={moveParams.page}
          totalPages={movements.totalPages}
          onPageChange={(p) => void loadMovements(p)}
        />
      </section>

      <section className="rounded-xl border bg-white p-4 shadow-card">
        <h2 className="font-semibold">Baixa por pedido</h2>
        <div className="mt-3 flex flex-wrap gap-2">
          <input
            value={orderOutbound}
            onChange={(e) => setOrderOutbound(e.target.value)}
            className="min-w-[240px] rounded border px-3 py-2 font-mono text-sm"
            placeholder="UUID do pedido"
          />
          <button
            type="button"
            disabled={loading}
            onClick={() => void salesOut()}
            className="rounded-lg bg-brand px-4 py-2 text-sm text-white"
          >
            Aplicar baixa
          </button>
        </div>
      </section>

      <section className="rounded-xl border bg-white p-4 shadow-card">
        <h2 className="font-semibold">Reentrada por troca</h2>
        <div className="mt-3 flex flex-wrap gap-2">
          <input
            value={exchangeReentry}
            onChange={(e) => setExchangeReentry(e.target.value)}
            className="min-w-[240px] rounded border px-3 py-2 font-mono text-sm"
            placeholder="UUID da solicitação de troca"
          />
          <button
            type="button"
            disabled={loading}
            onClick={() => void reentry()}
            className="rounded-lg border px-4 py-2 text-sm"
          >
            Reprocessar reentrada
          </button>
        </div>
      </section>
    </div>
  );
}
