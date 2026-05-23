import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { fetchProfileBundle } from '@/features/customer/customerSlice';
import * as customerService from '@/services/customerService';
import { cardSchema } from '@/utils/schemas';
import { getErrorMessage } from '@/services/api';
import { ConfirmModal } from '@/components/ConfirmModal';
import { TrashIcon } from '@/components/icons/TrashIcon';

const EMPTY_CARD_DEFAULTS = {
  cardholderName: '',
  cardNumber: '',
  brand: 'VISA',
  preferred: false,
} as z.input<typeof cardSchema>;

export function ProfileCardsPage() {
  const dispatch = useAppDispatch();
  const customerId = useAppSelector((s) => s.auth.customerId)!;
  const cards = useAppSelector((s) => s.customer.cards);
  const [msg, setMsg] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const form = useForm<z.input<typeof cardSchema>>({
    resolver: zodResolver(cardSchema),
    defaultValues: EMPTY_CARD_DEFAULTS,
  });

  const {
    register,
    formState: { errors },
  } = form;

  useEffect(() => {
    dispatch(fetchProfileBundle(customerId));
  }, [dispatch, customerId]);

  const pendingCard = pendingDeleteId ? cards.find((c) => c.id === pendingDeleteId) : undefined;

  return (
    <div>
      <h1 className="font-display text-2xl font-semibold">Cartões</h1>
      <p className="mt-1 text-sm text-ink-muted">
        RF0027: vários cartões por cliente. Marque <strong>um</strong> como preferencial para sugerir no checkout.
      </p>
      {msg && <p className="mt-2 text-sm text-red-600">{msg}</p>}
      <ul className="mt-6 space-y-3">
        {cards.map((c) => (
          <li key={c.id} className="flex items-center justify-between gap-3 rounded-lg border p-4">
            <div className="min-w-0">
              <p className="font-medium">
                {c.brand} {c.cardNumberMasked}
              </p>
              <p className="text-sm text-ink-muted">
                {c.cardholderName} — {c.expirationMonth}/{c.expirationYear}
              </p>
            </div>
            <div className="flex shrink-0 items-center gap-2">
              {c.preferred && (
                <span className="rounded-full bg-brand-soft px-2 py-0.5 text-xs text-brand">Preferencial</span>
              )}
              <button
                type="button"
                onClick={() => setPendingDeleteId(c.id)}
                className="rounded-lg p-2 text-stone-500 hover:bg-red-50 hover:text-red-700"
                aria-label={`Excluir cartão ${c.brand} final ${c.cardNumberMasked}`}
              >
                <TrashIcon />
              </button>
            </div>
          </li>
        ))}
      </ul>

      <ConfirmModal
        open={!!pendingDeleteId}
        title="Excluir cartão"
        message={
          pendingCard
            ? `Remover o cartão ${pendingCard.brand} ${pendingCard.cardNumberMasked}? Se for o preferencial, outro cartão ativo será marcado como preferencial, se houver.`
            : ''
        }
        variant="danger"
        confirmLabel={deleting ? 'Removendo…' : 'Excluir'}
        onClose={() => {
          if (!deleting) setPendingDeleteId(null);
        }}
        onConfirm={() => {
          const id = pendingDeleteId;
          if (!id || deleting) return;
          void (async () => {
            setDeleting(true);
            setMsg(null);
            try {
              await customerService.inactivateCard(customerId, id);
              setPendingDeleteId(null);
              dispatch(fetchProfileBundle(customerId));
            } catch (e) {
              setMsg(getErrorMessage(e));
              setPendingDeleteId(null);
            } finally {
              setDeleting(false);
            }
          })();
        }}
      />

      <h2 className="mt-8 font-semibold">Novo cartão</h2>
      <form
        className="mt-3 grid max-w-lg gap-2 sm:grid-cols-2"
        onSubmit={form.handleSubmit(async (data) => {
          setMsg(null);
          try {
            await customerService.createCard(customerId, {
              cardholderName: data.cardholderName,
              cardNumber: data.cardNumber,
              brand: data.brand,
              expirationMonth: data.expirationMonth,
              expirationYear: data.expirationYear,
              preferred: data.preferred ?? false,
            });
            form.reset(EMPTY_CARD_DEFAULTS);
            dispatch(fetchProfileBundle(customerId));
          } catch (e) {
            setMsg(getErrorMessage(e));
          }
        })}
      >
        <p className="sm:col-span-2 text-xs text-ink-muted">
          Ano de validade: ano em curso ou futuro (4 dígitos, ex. 2030).
        </p>
        <input {...register('cardholderName')} placeholder="Nome no cartão" className="rounded border px-2 py-1.5 text-sm" />
        {errors.cardholderName && <p className="text-xs text-red-600">{errors.cardholderName.message}</p>}
        <input {...register('cardNumber')} placeholder="Número" className="rounded border px-2 py-1.5 text-sm" />
        {errors.cardNumber && <p className="text-xs text-red-600">{errors.cardNumber.message}</p>}
        <input {...register('brand')} placeholder="Bandeira" className="rounded border px-2 py-1.5 text-sm" />
        {errors.brand && <p className="text-xs text-red-600">{errors.brand.message}</p>}
        <input type="number" {...register('expirationMonth')} placeholder="Mês" className="rounded border px-2 py-1.5 text-sm" />
        {errors.expirationMonth && <p className="text-xs text-red-600">{errors.expirationMonth.message}</p>}
        <input type="number" {...register('expirationYear')} placeholder="Ano (ex: 2030)" className="rounded border px-2 py-1.5 text-sm" />
        {errors.expirationYear && <p className="text-xs text-red-600">{errors.expirationYear.message}</p>}
        <label className="flex items-center gap-2 sm:col-span-2 text-sm">
          <input type="checkbox" {...register('preferred')} />
          Preferencial
        </label>
        <button type="submit" className="sm:col-span-2 rounded-lg bg-brand py-2 text-sm text-white">
          Adicionar cartão
        </button>
      </form>
    </div>
  );
}
