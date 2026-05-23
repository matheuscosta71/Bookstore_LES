import { useEffect, useState, type FormEvent } from 'react';
import * as adminCustomerService from '@/services/adminCustomerService';
import { getErrorMessage } from '@/services/api';
import type { Customer } from '@/types/api';
import { ConfirmModal } from '@/components/ConfirmModal';
import { Pagination } from '@/components/Pagination';
import { formatPersonDisplayName } from '@/utils/personNameDisplay';

function toDateInput(iso: string | undefined): string {
  if (!iso) return '';
  return iso.slice(0, 10);
}

type CustomerListFilters = {
  fullName: string;
  email: string;
  cpf: string;
  phone: string;
  code: string;
  birthDate: string;
  active: '' | 'true' | 'false';
  page: number;
};

const INITIAL_CUSTOMER_FILTERS: CustomerListFilters = {
  fullName: '',
  email: '',
  cpf: '',
  phone: '',
  code: '',
  birthDate: '',
  active: '',
  page: 0,
};

export function AdminCustomersPage() {
  const [filters, setFilters] = useState<CustomerListFilters>(() => ({ ...INITIAL_CUSTOMER_FILTERS }));
  const [rows, setRows] = useState<Customer[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  const [toggleTarget, setToggleTarget] = useState<Customer | null>(null);
  const [editing, setEditing] = useState<Customer | null>(null);
  const [editForm, setEditForm] = useState({
    fullName: '',
    email: '',
    cpf: '',
    phone: '',
    birthDate: '',
    active: true,
  });
  const [saving, setSaving] = useState(false);

  async function load(pageIndex: number, criteria?: CustomerListFilters) {
    const f = criteria ?? filters;
    setErr(null);
    setLoading(true);
    try {
      const res = await adminCustomerService.listCustomers({
        fullName: f.fullName || undefined,
        email: f.email || undefined,
        cpf: f.cpf || undefined,
        phone: f.phone || undefined,
        code: f.code.trim() || undefined,
        birthDate: f.birthDate || undefined,
        active: f.active === '' ? undefined : f.active === 'true' ? true : false,
        page: pageIndex,
        size: 12,
      });
      setRows(res.content);
      setTotalPages(res.totalPages);
      setFilters({ ...f, page: res.number });
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- carrega inicial; filtros aplicados no botão
  }, []);

  function openEdit(c: Customer) {
    setMsg(null);
    setErr(null);
    setEditing(c);
    setEditForm({
      fullName: c.fullName,
      email: c.email,
      cpf: c.cpf.replace(/\D/g, ''),
      phone: c.phone,
      birthDate: toDateInput(c.birthDate),
      active: c.active,
    });
  }

  function closeEdit() {
    setEditing(null);
  }

  async function submitEdit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!editing) return;
    const cpfDigits = editForm.cpf.replace(/\D/g, '');
    if (cpfDigits.length !== 11) {
      setErr('CPF deve ter 11 dígitos.');
      return;
    }
    setErr(null);
    setMsg(null);
    setSaving(true);
    try {
      await adminCustomerService.updateCustomer(editing.id, {
        fullName: editForm.fullName.trim(),
        email: editForm.email.trim(),
        cpf: cpfDigits,
        phone: editForm.phone.trim(),
        birthDate: editForm.birthDate,
        active: editForm.active,
      });
      setMsg('Cliente atualizado (RF0022).');
      closeEdit();
      await load(filters.page);
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  async function toggleActive(c: Customer) {
    setErr(null);
    setMsg(null);
    setLoading(true);
    try {
      await adminCustomerService.setCustomerActive(c.id, !c.active);
      setMsg(!c.active ? 'Cliente reativado (RF0023).' : 'Cliente inativado (RF0023).');
      await load(filters.page);
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <h1 className="font-display text-3xl font-semibold">Clientes</h1>
      <p className="mt-1 text-sm text-ink-muted">
        RF0021 cadastro público na tela de registro · RF0024 consulta com filtros combinados · RF0022 alteração ·
        RF0023 inativar/reativar
      </p>

      <div className="mt-6 grid gap-3 rounded-xl border bg-white p-4 shadow-card sm:grid-cols-2 lg:grid-cols-3">
        <label className="text-sm">
          Nome
          <input
            value={filters.fullName}
            onChange={(e) => setFilters((s) => ({ ...s, fullName: e.target.value }))}
            className="mt-1 w-full rounded border px-3 py-2 text-sm"
          />
        </label>
        <label className="text-sm">
          E-mail
          <input
            value={filters.email}
            onChange={(e) => setFilters((s) => ({ ...s, email: e.target.value }))}
            className="mt-1 w-full rounded border px-3 py-2 text-sm"
          />
        </label>
        <label className="text-sm">
          CPF
          <input
            value={filters.cpf}
            onChange={(e) => setFilters((s) => ({ ...s, cpf: e.target.value }))}
            className="mt-1 w-full rounded border px-3 py-2 text-sm"
          />
        </label>
        <label className="text-sm">
          Telefone
          <input
            value={filters.phone}
            onChange={(e) => setFilters((s) => ({ ...s, phone: e.target.value }))}
            className="mt-1 w-full rounded border px-3 py-2 text-sm"
          />
        </label>
        <label className="text-sm">
          Código
          <input
            value={filters.code}
            onChange={(e) => setFilters((s) => ({ ...s, code: e.target.value }))}
            className="mt-1 w-full rounded border px-3 py-2 font-mono text-sm"
            placeholder="ex.: CLI-00001"
          />
        </label>
        <label className="text-sm">
          Nascimento
          <input
            type="date"
            value={filters.birthDate}
            onFocus={(e) => (e.currentTarget as HTMLInputElement & { showPicker?: () => void }).showPicker?.()}
            onClick={(e) => (e.currentTarget as HTMLInputElement & { showPicker?: () => void }).showPicker?.()}
            onKeyDown={(e) => {
              if (e.key !== 'Tab') e.preventDefault();
            }}
            onChange={(e) => setFilters((s) => ({ ...s, birthDate: e.target.value }))}
            className="mt-1 w-full rounded border px-3 py-2 text-sm"
          />
        </label>
        <label className="text-sm">
          Ativo
          <select
            value={filters.active}
            onChange={(e) =>
              setFilters((s) => ({ ...s, active: e.target.value as typeof filters.active }))
            }
            className="mt-1 w-full rounded border px-3 py-2 text-sm"
          >
            <option value="">(todos)</option>
            <option value="true">Sim</option>
            <option value="false">Não</option>
          </select>
        </label>
      </div>
      <div className="mt-4 flex flex-wrap gap-2">
        <button
          type="button"
          disabled={loading}
          onClick={() => void load(0)}
          className="rounded-lg bg-brand px-4 py-2 text-sm text-white disabled:opacity-50"
        >
          Aplicar filtros
        </button>
        <button
          type="button"
          disabled={loading}
          onClick={() => void load(0, { ...INITIAL_CUSTOMER_FILTERS })}
          className="rounded-lg border border-stone-300 bg-white px-4 py-2 text-sm text-ink hover:bg-stone-50 disabled:opacity-50"
        >
          Limpar filtros
        </button>
      </div>

      {msg && <p className="mt-4 text-sm text-green-700">{msg}</p>}
      {err && <p className="mt-4 text-sm text-red-600">{err}</p>}
      {loading && <p className="mt-4 text-sm text-ink-muted">Carregando…</p>}

      {editing && (
        <section className="mt-8 rounded-xl border border-brand/30 bg-amber-50/50 p-4 shadow-card">
          <h2 className="font-semibold">Editar cliente (RF0022)</h2>
          <form className="mt-4 grid max-w-2xl gap-3 sm:grid-cols-2" onSubmit={(e) => void submitEdit(e)}>
            <label className="text-sm sm:col-span-2">
              Nome
              <input
                value={editForm.fullName}
                onChange={(e) => setEditForm((f) => ({ ...f, fullName: e.target.value }))}
                className="mt-1 w-full rounded border px-3 py-2 text-sm"
                required
              />
            </label>
            <label className="text-sm sm:col-span-2">
              E-mail
              <input
                type="email"
                value={editForm.email}
                onChange={(e) => setEditForm((f) => ({ ...f, email: e.target.value }))}
                className="mt-1 w-full rounded border px-3 py-2 text-sm"
                required
              />
            </label>
            <label className="text-sm">
              CPF (11 dígitos)
              <input
                value={editForm.cpf}
                onChange={(e) => setEditForm((f) => ({ ...f, cpf: e.target.value.replace(/\D/g, '').slice(0, 11) }))}
                className="mt-1 w-full rounded border px-3 py-2 text-sm"
                required
              />
            </label>
            <label className="text-sm">
              Telefone
              <input
                value={editForm.phone}
                onChange={(e) => setEditForm((f) => ({ ...f, phone: e.target.value }))}
                className="mt-1 w-full rounded border px-3 py-2 text-sm"
                required
              />
            </label>
            <label className="text-sm sm:col-span-2">
              Nascimento
              <input
                type="date"
                value={editForm.birthDate}
                onChange={(e) => setEditForm((f) => ({ ...f, birthDate: e.target.value }))}
                className="mt-1 w-full rounded border px-3 py-2 text-sm"
                required
              />
            </label>
            <label className="flex items-center gap-2 text-sm sm:col-span-2">
              <input
                type="checkbox"
                checked={editForm.active}
                onChange={(e) => setEditForm((f) => ({ ...f, active: e.target.checked }))}
              />
              Cadastro ativo
            </label>
            <div className="flex flex-wrap gap-2 sm:col-span-2">
              <button
                type="submit"
                disabled={saving}
                className="rounded-lg bg-brand px-4 py-2 text-sm text-white disabled:opacity-50"
              >
                {saving ? 'Salvando…' : 'Salvar'}
              </button>
              <button type="button" onClick={closeEdit} className="rounded-lg border px-4 py-2 text-sm">
                Cancelar
              </button>
            </div>
          </form>
        </section>
      )}

      <ul className="mt-6 space-y-2">
        {rows.map((c) => (
          <li key={c.id} className="flex flex-wrap items-start justify-between gap-3 rounded-lg border bg-white p-3 text-sm shadow-card">
            <div>
              <p className="font-medium">{formatPersonDisplayName(c.fullName)}</p>
              <p className="text-ink-muted">{c.email}</p>
              <p className="text-xs text-ink-muted">
                Status: {c.active ? 'ativo' : 'inativo'} · CPF: {c.cpf}
              </p>
              <p className="font-mono text-xs text-ink-muted">
                {c.code ? `código ${c.code} · ` : ''}
                {c.id}
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => openEdit(c)}
                className="rounded border border-stone-300 px-2 py-1 text-xs hover:bg-stone-50"
              >
                Editar
              </button>
              <button
                type="button"
                disabled={loading}
                onClick={() => setToggleTarget(c)}
                className="rounded border border-stone-300 px-2 py-1 text-xs hover:bg-stone-50 disabled:opacity-50"
              >
                {c.active ? 'Inativar' : 'Ativar'}
              </button>
            </div>
          </li>
        ))}
      </ul>

      <ConfirmModal
        open={!!toggleTarget}
        title={toggleTarget?.active ? 'Inativar cliente' : 'Reativar cliente'}
        message={
          toggleTarget
            ? `Confirma ${toggleTarget.active ? 'inativar' : 'reativar'} o cliente ${formatPersonDisplayName(toggleTarget.fullName)}?`
            : ''
        }
        variant={toggleTarget?.active ? 'danger' : 'neutral'}
        confirmLabel={toggleTarget?.active ? 'Inativar' : 'Ativar'}
        onClose={() => setToggleTarget(null)}
        onConfirm={() => {
          const c = toggleTarget;
          setToggleTarget(null);
          if (c) void toggleActive(c);
        }}
      />

      <Pagination page={filters.page} totalPages={totalPages} onPageChange={(p) => void load(p)} />
    </div>
  );
}
