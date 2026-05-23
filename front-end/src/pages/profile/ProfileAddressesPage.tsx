import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { fetchProfileBundle } from '@/features/customer/customerSlice';
import * as customerService from '@/services/customerService';
import type { Address } from '@/types/api';
import { addressSchema } from '@/utils/schemas';
import { getErrorMessage } from '@/services/api';
import { ConfirmModal } from '@/components/ConfirmModal';
import { TrashIcon } from '@/components/icons/TrashIcon';

const EMPTY_ADDRESS_DEFAULTS: z.input<typeof addressSchema> = {
  nickname: '',
  street: '',
  number: '',
  complement: '',
  neighborhood: '',
  city: '',
  state: '',
  zipCode: '',
  type: 'DELIVERY',
};

function addressToFormValues(a: Address): z.input<typeof addressSchema> {
  const t = a.type === 'BILLING' ? 'BILLING' : 'DELIVERY';
  return {
    nickname: a.nickname,
    street: a.street,
    number: a.number,
    complement: a.complement ?? '',
    neighborhood: a.neighborhood,
    city: a.city,
    state: a.state,
    zipCode: a.zipCode,
    type: t,
  };
}

export function ProfileAddressesPage() {
  const dispatch = useAppDispatch();
  const customerId = useAppSelector((s) => s.auth.customerId)!;
  const addresses = useAppSelector((s) => s.customer.addresses);
  const [msg, setMsg] = useState<string | null>(null);
  const [okMsg, setOkMsg] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);
  /** Cria dois registros (DELIVERY + BILLING) com os mesmos dados — evita digitar duas vezes (RN0021/RN0022). */
  const [sameDeliveryAndBilling, setSameDeliveryAndBilling] = useState(true);

  const form = useForm<z.input<typeof addressSchema>>({
    resolver: zodResolver(addressSchema),
    defaultValues: EMPTY_ADDRESS_DEFAULTS,
  });

  const {
    register,
    formState: { errors },
  } = form;

  useEffect(() => {
    dispatch(fetchProfileBundle(customerId));
  }, [dispatch, customerId]);

  const pendingAddress = pendingDeleteId ? addresses.find((x) => x.id === pendingDeleteId) : undefined;

  function startEdit(a: Address) {
    setMsg(null);
    setOkMsg(null);
    setEditingId(a.id);
    setSameDeliveryAndBilling(false);
    form.reset(addressToFormValues(a));
  }

  function cancelEdit() {
    setEditingId(null);
    form.reset(EMPTY_ADDRESS_DEFAULTS);
    setSameDeliveryAndBilling(true);
  }

  return (
    <div>
      <h1 className="font-display text-2xl font-semibold">Endereços</h1>
      <p className="mt-1 text-sm text-ink-muted">
        RF0026: vários endereços por cliente. O campo <strong>apelido</strong> é um nome curto para identificar cada
        endereço.
      </p>
      {msg && <p className="mt-2 text-sm text-red-600">{msg}</p>}
      {okMsg && <p className="mt-2 text-sm text-green-700">{okMsg}</p>}
      <ul className="mt-6 space-y-3">
        {addresses.map((a) => (
          <li key={a.id} className="flex items-start justify-between gap-3 rounded-lg border p-4">
            <div className="min-w-0">
              <p className="font-medium">{a.nickname}</p>
              <p className="text-sm text-ink-muted">
                {a.type === 'BILLING' ? (
                  <span className="mr-1 rounded bg-stone-200 px-1.5 py-0.5 text-xs text-ink">Cobrança</span>
                ) : a.type === 'DELIVERY' ? (
                  <span className="mr-1 rounded bg-stone-200 px-1.5 py-0.5 text-xs text-ink">Entrega</span>
                ) : null}
                {a.street}, {a.number} — {a.neighborhood}, {a.city}/{a.state} — CEP {a.zipCode}
              </p>
            </div>
            <div className="flex shrink-0 gap-1">
              <button
                type="button"
                onClick={() => startEdit(a)}
                className="rounded-lg px-2 py-1.5 text-sm font-medium text-brand hover:bg-brand-soft"
              >
                Editar
              </button>
              <button
                type="button"
                onClick={() => setPendingDeleteId(a.id)}
                className="rounded-lg p-2 text-stone-500 hover:bg-red-50 hover:text-red-700"
                aria-label={`Excluir endereço ${a.nickname}`}
              >
                <TrashIcon />
              </button>
            </div>
          </li>
        ))}
      </ul>

      <ConfirmModal
        open={!!pendingDeleteId}
        title="Excluir endereço"
        message={
          pendingAddress
            ? `Remover o endereço “${pendingAddress.nickname}”? Ele deixará de aparecer na sua conta.`
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
            setOkMsg(null);
            try {
              await customerService.inactivateAddress(customerId, id);
              setPendingDeleteId(null);
              if (editingId === id) cancelEdit();
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

      <h2 className="mt-8 font-semibold">{editingId ? 'Editar endereço' : 'Novo endereço'}</h2>
      <form
        className="mt-3 grid max-w-lg gap-2 sm:grid-cols-2"
        onSubmit={form.handleSubmit(async (data) => {
          setMsg(null);
          setOkMsg(null);
          try {
            if (editingId) {
              const current = addresses.find((x) => x.id === editingId);
              const isBillingRow = current?.type === 'BILLING';
              const profileHasBilling = addresses.some((a) => a.type === 'BILLING');
              const profileHasDelivery = addresses.some((a) => a.type === 'DELIVERY');

              if (sameDeliveryAndBilling) {
                if (isBillingRow) {
                  await customerService.updateAddress(customerId, editingId, {
                    ...data,
                    type: 'BILLING',
                    active: true,
                  });
                  if (!profileHasDelivery) {
                    await customerService.createAddress(customerId, {
                      ...data,
                      type: 'DELIVERY',
                      nickname: `${data.nickname.trim()} — entrega`,
                    });
                  }
                } else {
                  await customerService.updateAddress(customerId, editingId, {
                    ...data,
                    type: 'DELIVERY',
                    active: true,
                  });
                  if (!profileHasBilling) {
                    await customerService.createAddress(customerId, {
                      ...data,
                      type: 'BILLING',
                      nickname: `${data.nickname.trim()} — cobrança`,
                    });
                  }
                }
              } else {
                await customerService.updateAddress(customerId, editingId, {
                  ...data,
                  active: true,
                });
              }
              setEditingId(null);
              form.reset(EMPTY_ADDRESS_DEFAULTS);
              setSameDeliveryAndBilling(true);
              setOkMsg('Endereço atualizado.');
              dispatch(fetchProfileBundle(customerId));
              return;
            }
            if (sameDeliveryAndBilling) {
              await customerService.createAddress(customerId, {
                ...data,
                type: 'DELIVERY',
              });
              await customerService.createAddress(customerId, {
                ...data,
                type: 'BILLING',
                nickname: `${data.nickname.trim()} — cobrança`,
              });
            } else {
              await customerService.createAddress(customerId, data);
            }
            form.reset(EMPTY_ADDRESS_DEFAULTS);
            setOkMsg('Endereço cadastrado.');
            dispatch(fetchProfileBundle(customerId));
          } catch (e) {
            setMsg(getErrorMessage(e));
          }
        })}
      >
        <input
          {...register('nickname')}
          placeholder="Apelido (ex.: Casa, Trabalho)"
          className="rounded border px-2 py-1.5 text-sm"
        />
        {errors.nickname && <p className="text-xs text-red-600">{errors.nickname.message}</p>}
        <input {...register('street')} placeholder="Rua" className="rounded border px-2 py-1.5 text-sm" />
        {errors.street && <p className="text-xs text-red-600">{errors.street.message}</p>}
        <input {...register('number')} placeholder="Número" className="rounded border px-2 py-1.5 text-sm" />
        {errors.number && <p className="text-xs text-red-600">{errors.number.message}</p>}
        <input {...register('neighborhood')} placeholder="Bairro" className="rounded border px-2 py-1.5 text-sm" />
        {errors.neighborhood && <p className="text-xs text-red-600">{errors.neighborhood.message}</p>}
        <input {...register('city')} placeholder="Cidade" className="rounded border px-2 py-1.5 text-sm" />
        {errors.city && <p className="text-xs text-red-600">{errors.city.message}</p>}
        <input {...register('state')} placeholder="UF (ex: MG)" className="rounded border px-2 py-1.5 text-sm" />
        {errors.state && <p className="text-xs text-red-600">{errors.state.message}</p>}
        <input {...register('zipCode')} placeholder="CEP" className="rounded border px-2 py-1.5 text-sm" />
        {errors.zipCode && <p className="text-xs text-red-600">{errors.zipCode.message}</p>}
        <label className="flex items-start gap-2 sm:col-span-2 text-sm leading-snug">
          <input
            type="checkbox"
            className="mt-0.5"
            checked={sameDeliveryAndBilling}
            onChange={(e) => setSameDeliveryAndBilling(e.target.checked)}
          />
          <span>
            <strong>Mesmo endereço para entrega e cobrança</strong>
            {editingId
              ? ' — ao salvar, este cadastro é atualizado e, se faltar o outro tipo no perfil, é criado automaticamente (apelido com sufixo "— cobrança" ou "— entrega").'
              : ' — serão criados dois cadastros (entrega + cobrança) com os mesmos dados; o de cobrança usa o apelido acima com o sufixo "— cobrança".'}
          </span>
        </label>
        {!sameDeliveryAndBilling && (
          <select {...register('type')} className="rounded border px-2 py-1.5 text-sm sm:col-span-2">
            <option value="DELIVERY">Entrega</option>
            <option value="BILLING">Cobrança</option>
          </select>
        )}
        <div className="flex flex-wrap gap-2 sm:col-span-2">
          <button type="submit" className="rounded-lg bg-brand px-4 py-2 text-sm text-white">
            {editingId ? 'Salvar alterações' : 'Adicionar'}
          </button>
          {editingId && (
            <button type="button" onClick={cancelEdit} className="rounded-lg border border-stone-300 px-4 py-2 text-sm">
              Cancelar
            </button>
          )}
        </div>
      </form>
    </div>
  );
}
