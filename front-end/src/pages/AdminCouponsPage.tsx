import { useEffect, useState } from 'react';
import * as adminCouponService from '@/services/adminCouponService';
import * as adminCustomerService from '@/services/adminCustomerService';
import type { Customer } from '@/types/api';
import { formatBRL } from '@/utils/format';
import { getErrorMessage } from '@/services/api';

export function AdminCouponsPage() {
  const [coupons, setCoupons] = useState<adminCouponService.CouponResponse[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  // Form State
  const [code, setCode] = useState('');
  const [type, setType] = useState<adminCouponService.CouponType>('PROMOTIONAL');
  const [amount, setAmount] = useState('');
  const [expirationDate, setExpirationDate] = useState('');
  const [customerId, setCustomerId] = useState('');

  function formatCurrencyInput(val: string): string {
    const clean = val.replace(/\D/g, '');
    if (!clean) return '';
    const cents = parseInt(clean, 10);
    if (isNaN(cents)) return '';

    const floatVal = cents / 100;
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(floatVal);
  }

  async function loadCoupons(p = page) {
    setLoading(true);
    try {
      const res = await adminCouponService.listCoupons(p, 10);
      setCoupons(res.content);
      setTotalPages(res.totalPages);
    } catch (e) {
      setMsg({ text: getErrorMessage(e), type: 'error' });
    } finally {
      setLoading(false);
    }
  }

  async function loadCustomers() {
    try {
      const res = await adminCustomerService.listCustomers({ page: 0, size: 200 });
      setCustomers(res.content);
    } catch (e) {
      console.error('Falha ao carregar clientes', e);
    }
  }

  useEffect(() => {
    void loadCoupons();
    void loadCustomers();
  }, [page]);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setMsg(null);

    const cleanCode = code.trim().toUpperCase();
    if (!cleanCode) {
      setMsg({ text: 'Informe o código do cupom.', type: 'error' });
      return;
    }

    const cleanAmount = amount.replace(/[^\d]/g, '');
    const numAmount = cleanAmount ? parseInt(cleanAmount, 10) / 100 : 0;
    if (numAmount <= 0) {
      setMsg({ text: 'Informe um valor maior que R$ 0,00.', type: 'error' });
      return;
    }

    setBusy(true);
    try {
      await adminCouponService.createCoupon({
        code: cleanCode,
        type,
        amount: numAmount,
        expirationDate: expirationDate || null,
        customerId: customerId || null,
      });

      setMsg({ text: `Cupom ${cleanCode} criado com sucesso!`, type: 'success' });
      // Reset form
      setCode('');
      setAmount('');
      setExpirationDate('');
      setCustomerId('');

      void loadCoupons(0);
      setPage(0);
    } catch (err) {
      setMsg({ text: getErrorMessage(err), type: 'error' });
    } finally {
      setBusy(false);
    }
  }

  async function handleToggleActive(couponId: string) {
    setMsg(null);
    try {
      await adminCouponService.toggleCouponActive(couponId);
      void loadCoupons();
    } catch (err) {
      setMsg({ text: getErrorMessage(err), type: 'error' });
    }
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-display text-3xl font-semibold text-stone-900">Gerenciador de Cupons</h1>
        <p className="mt-1 text-sm text-ink-muted">
          Criação e gerenciamento de cupons promocionais ou de troca (créditos de devolução).
        </p>
      </div>

      {msg && (
        <div
          className={`rounded-xl border px-4 py-3 text-sm flex items-center justify-between ${
            msg.type === 'success'
              ? 'border-green-200 bg-green-50 text-green-900'
              : 'border-red-200 bg-red-50 text-red-900'
          }`}
        >
          <span>{msg.text}</span>
          <button type="button" onClick={() => setMsg(null)} className="font-bold hover:opacity-85 ml-2">
            ×
          </button>
        </div>
      )}

      <div className="grid gap-8 lg:grid-cols-[340px_1fr]">
        {/* Formulário lateral */}
        <section className="rounded-2xl border border-stone-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-stone-950">Novo Cupom</h2>
          <p className="text-xs text-ink-muted mt-0.5">Defina as regras do cupom promocional ou de troca.</p>

          <form onSubmit={(e) => void handleCreate(e)} className="mt-6 space-y-4">
            <div>
              <label htmlFor="code-input" className="block text-sm font-medium text-ink">Código único</label>
              <input
                id="code-input"
                type="text"
                value={code}
                onChange={(e) => setCode(e.target.value.toUpperCase())}
                placeholder="EX: PROMO15"
                className="mt-1 block w-full rounded-lg border border-stone-200 px-3 py-2 text-sm uppercase placeholder:normal-case focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20"
                required
              />
            </div>

            <div>
              <label htmlFor="type-select" className="block text-sm font-medium text-ink">Tipo de cupom</label>
              <select
                id="type-select"
                value={type}
                onChange={(e) => setType(e.target.value as adminCouponService.CouponType)}
                className="mt-1 block w-full rounded-lg border border-stone-200 px-3 py-2 text-sm focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20 bg-white"
              >
                <option value="PROMOTIONAL">Cupom Promocional</option>
                <option value="EXCHANGE">Cupom de Troca (Devolução)</option>
              </select>
            </div>

            <div>
              <label htmlFor="amount-input" className="block text-sm font-medium text-ink">Valor</label>
              <input
                id="amount-input"
                type="text"
                value={amount}
                onChange={(e) => setAmount(formatCurrencyInput(e.target.value))}
                placeholder="R$ 0,00"
                className="mt-1 block w-full rounded-lg border border-stone-200 px-3 py-2 text-sm focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20"
                required
              />
            </div>

            <div>
              <label htmlFor="expiry-input" className="block text-sm font-medium text-ink">Data de Expiração (Opcional)</label>
              <input
                id="expiry-input"
                type="date"
                value={expirationDate}
                onChange={(e) => setExpirationDate(e.target.value)}
                className="mt-1 block w-full rounded-lg border border-stone-200 px-3 py-2 text-sm focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20"
              />
            </div>

            <div>
              <label htmlFor="customer-select" className="block text-sm font-medium text-ink">Cliente específico (Opcional)</label>
              <select
                id="customer-select"
                value={customerId}
                onChange={(e) => setCustomerId(e.target.value)}
                className="mt-1 block w-full rounded-lg border border-stone-200 px-3 py-2 text-sm focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20 bg-white"
              >
                <option value="">Livre (qualquer cliente pode usar)</option>
                {customers.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.fullName} ({c.email})
                  </option>
                ))}
              </select>
            </div>

            <button
              type="submit"
              disabled={busy}
              className="w-full mt-2 rounded-xl bg-brand py-2.5 text-sm font-semibold text-white hover:bg-brand/90 disabled:opacity-50"
            >
              {busy ? 'Criando…' : 'Criar Cupom'}
            </button>
          </form>
        </section>

        {/* Tabela de cupons */}
        <section className="rounded-2xl border border-stone-200 bg-white shadow-sm overflow-hidden flex flex-col justify-between">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="border-b border-stone-150 bg-stone-50 text-xs font-semibold text-ink-muted uppercase">
                  <th className="px-6 py-4">Código</th>
                  <th className="px-6 py-4">Tipo</th>
                  <th className="px-6 py-4">Valor</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4">Validade</th>
                  <th className="px-6 py-4">Cliente Associado</th>
                  <th className="px-6 py-4 text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-100">
                {loading ? (
                  <tr>
                    <td colSpan={7} className="px-6 py-10 text-center text-ink-muted">
                      Carregando cupons…
                    </td>
                  </tr>
                ) : coupons.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-6 py-10 text-center text-ink-muted">
                      Nenhum cupom encontrado.
                    </td>
                  </tr>
                ) : (
                  coupons.map((c) => {
                    const isExpired = c.expirationDate && new Date(c.expirationDate) < new Date();
                    return (
                      <tr key={c.id} className="hover:bg-stone-50/50">
                        <td className="px-6 py-4 font-mono font-bold uppercase text-stone-900">
                          {c.code}
                        </td>
                        <td className="px-6 py-4">
                          <span
                            className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                              c.type === 'PROMOTIONAL'
                                ? 'bg-indigo-50 text-indigo-700'
                                : 'bg-amber-50 text-amber-700'
                            }`}
                          >
                            {c.type === 'PROMOTIONAL' ? 'Promocional' : 'Troca'}
                          </span>
                        </td>
                        <td className="px-6 py-4 font-semibold text-stone-950">
                          {formatBRL(c.amount)}
                        </td>
                        <td className="px-6 py-4 space-y-1">
                          <div className="flex items-center gap-1.5">
                            <span
                              className={`h-2 w-2 rounded-full ${
                                c.active && !isExpired ? 'bg-green-500' : 'bg-stone-400'
                              }`}
                            />
                            <span className="text-xs text-ink">
                              {c.active && !isExpired ? 'Ativo' : 'Inativo'}
                            </span>
                          </div>
                          <div className="flex items-center gap-1.5">
                            <span
                              className={`h-2 w-2 rounded-full ${
                                c.redeemed
                                  ? 'bg-purple-500'
                                  : (!c.active || isExpired)
                                    ? 'bg-stone-400'
                                    : 'bg-blue-400'
                              }`}
                            />
                            <span className="text-xs text-ink-muted">
                              {c.redeemed
                                ? 'Utilizado'
                                : (!c.active || isExpired)
                                  ? 'Indisponível'
                                  : 'Disponível'}
                            </span>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-ink-muted">
                          {c.expirationDate ? (
                            <span className={isExpired ? 'text-red-600 font-medium' : ''}>
                              {new Date(c.expirationDate).toLocaleDateString('pt-BR')} {isExpired ? '(Expirado)' : ''}
                            </span>
                          ) : (
                            'Sem expiração'
                          )}
                        </td>
                        <td className="px-6 py-4">
                          {c.customerName ? (
                            <div>
                              <p className="font-medium text-stone-900">{c.customerName}</p>
                              <p className="text-xs text-ink-muted">Apenas este cliente</p>
                            </div>
                          ) : (
                            <span className="text-xs text-ink-muted italic">Qualquer cliente</span>
                          )}
                        </td>
                        <td className="px-6 py-4 text-right">
                          <button
                            type="button"
                            onClick={() => void handleToggleActive(c.id)}
                            className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
                              c.active
                                ? 'border border-red-200 bg-red-50 text-red-700 hover:bg-red-100'
                                : 'border border-green-200 bg-green-50 text-green-700 hover:bg-green-100'
                            }`}
                          >
                            {c.active ? 'Inativar' : 'Ativar'}
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          {/* Paginação */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-stone-150 px-6 py-4 bg-stone-50">
              <span className="text-xs text-ink-muted">
                Página {page + 1} de {totalPages}
              </span>
              <div className="flex gap-2">
                <button
                  type="button"
                  disabled={page === 0}
                  onClick={() => setPage((prev) => prev - 1)}
                  className="rounded-lg border border-stone-200 bg-white px-3 py-1.5 text-xs font-semibold text-ink hover:bg-stone-50 disabled:opacity-50"
                >
                  Anterior
                </button>
                <button
                  type="button"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((prev) => prev + 1)}
                  className="rounded-lg border border-stone-200 bg-white px-3 py-1.5 text-xs font-semibold text-ink hover:bg-stone-50 disabled:opacity-50"
                >
                  Próximo
                </button>
              </div>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
