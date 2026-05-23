import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import {
  applyAddress,
  applyPayment,
  calculateFreight,
  finalizeOrder,
  resetCheckout,
  clearCheckoutError,
} from '@/features/checkout/checkoutSlice';
import { loadCart } from '@/features/cart/cartSlice';
import { fetchOrdersAndTransactions } from '@/features/customer/customerSlice';
import * as customerService from '@/services/customerService';
import * as checkoutService from '@/services/checkoutService';
import type { Address, CreditCard } from '@/types/api';
import { OrderSummary } from '@/components/OrderSummary';
import { CouponInput } from '@/components/CouponInput';
import { addressSchema, cardSchema } from '@/utils/schemas';
import { getErrorMessage } from '@/services/api';
import { ROUTES } from '@/constants/routes';
import { formatBRL } from '@/utils/format';
import { validateCheckoutPaymentAmounts, MIN_CREDIT_CARD_LINE_BRL } from '@/validators/paymentValidator';
import { appLogger } from '@/utils/appLogger';
import { z } from 'zod';

/** Valores iniciais do formulário de endereço (entrada antes dos transforms do Zod). */
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

const EMPTY_CARD_DEFAULTS = {
  cardholderName: '',
  cardNumber: '',
  brand: 'VISA',
  preferred: false,
} as z.input<typeof cardSchema>;

function moneyToCents(n: number): number {
  return Math.round((n + Number.EPSILON) * 100);
}

function centsToMoney(cents: number): number {
  return Math.round(cents) / 100;
}

type AppliedCoupon = {
  code: string;
  paymentType: 'EXCHANGE_COUPON' | 'PROMOTIONAL_COUPON';
  amount: number;
};

export function CheckoutPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const customerId = useAppSelector((s) => s.auth.customerId)!;
  const freight = useAppSelector((s) => s.checkout.freight);
  const checkoutStatus = useAppSelector((s) => s.checkout.status);
  const checkoutErr = useAppSelector((s) => s.checkout.error);
  const cart = useAppSelector((s) => s.cart.cart);

  const [addresses, setAddresses] = useState<Address[]>([]);
  const [cards, setCards] = useState<CreditCard[]>([]);
  const [addressId, setAddressId] = useState<string | null>(null);
  const [showNewAddress, setShowNewAddress] = useState(false);
  const [saveNewAddress, setSaveNewAddress] = useState(true);
  const [cardMode, setCardMode] = useState<'saved' | 'new'>('saved');
  const [selectedCardId, setSelectedCardId] = useState<string | null>(null);
  const [saveNewCard, setSaveNewCard] = useState(true);
  const [msg, setMsg] = useState<string | null>(null);

  const [couponPaymentType, setCouponPaymentType] = useState<'EXCHANGE_COUPON' | 'PROMOTIONAL_COUPON'>(
    'EXCHANGE_COUPON',
  );
  const [couponCodeDraft, setCouponCodeDraft] = useState('');
  const [appliedCoupon, setAppliedCoupon] = useState<AppliedCoupon | null>(null);
  const [couponFieldError, setCouponFieldError] = useState<string | null>(null);
  const [checkoutBusy, setCheckoutBusy] = useState(false);
  /** Novo endereço no checkout: entrega + cobrança com um preenchimento (dois registros no perfil). */
  const [pairDeliveryAndBillingCheckout, setPairDeliveryAndBillingCheckout] = useState(true);
  const [billingSyncing, setBillingSyncing] = useState(false);

  const addrForm = useForm<z.input<typeof addressSchema>>({
    resolver: zodResolver(addressSchema),
    defaultValues: EMPTY_ADDRESS_DEFAULTS,
  });

  const {
    register: registerAddr,
    handleSubmit: handleSubmitAddr,
    reset: resetAddr,
    formState: { errors: addrErrors },
  } = addrForm;

  const cardForm = useForm<z.input<typeof cardSchema>>({
    resolver: zodResolver(cardSchema),
    defaultValues: EMPTY_CARD_DEFAULTS,
  });

  const {
    register: registerCard,
    handleSubmit: handleSubmitCard,
    formState: { errors: cardErrors },
  } = cardForm;

  const refreshAddresses = useCallback(async () => {
    const a = await customerService.listAddresses(customerId);
    const active = a.filter((x) => x.active);
    setAddresses(active);
    return active;
  }, [customerId]);

  useEffect(() => {
    dispatch(loadCart(customerId));
    dispatch(resetCheckout());
    void refreshAddresses().then((active) => {
      if (active[0]) setAddressId(active[0].id);
    });
    void customerService.listCards(customerId).then((c) => {
      const list = c.filter((x) => x.active);
      setCards(list);
      const pref = list.find((x) => x.preferred);
      const nextSelectedId = pref?.id ?? list[0]?.id ?? null;
      setSelectedCardId(nextSelectedId);
      setCardMode(nextSelectedId ? 'saved' : 'new');
    });
  }, [dispatch, customerId, refreshAddresses]);

  useEffect(() => {
    setAppliedCoupon(null);
    setCouponFieldError(null);
  }, [couponPaymentType]);

  const hasBillingAddress = useMemo(
    () => addresses.some((a) => a.active && a.type === 'BILLING'),
    [addresses],
  );
  const hasDeliveryAddress = useMemo(
    () => addresses.some((a) => a.active && a.type === 'DELIVERY'),
    [addresses],
  );

  const copyBillingFromSelectedDelivery = useCallback(async () => {
    if (!addressId || !customerId) return;
    const src = addresses.find((a) => a.id === addressId);
    if (!src) return;
    setBillingSyncing(true);
    setMsg(null);
    try {
      await customerService.createAddress(customerId, {
        nickname: `${src.nickname} — cobrança`,
        street: src.street,
        number: src.number,
        complement: src.complement ?? '',
        neighborhood: src.neighborhood,
        city: src.city,
        state: src.state,
        zipCode: src.zipCode,
        type: 'BILLING',
      });
      await refreshAddresses();
      setMsg('Endereço de cobrança cadastrado com os mesmos dados da entrega.');
    } catch (e) {
      setMsg(getErrorMessage(e));
    } finally {
      setBillingSyncing(false);
    }
  }, [addressId, addresses, customerId, refreshAddresses]);

  const itemsSub =
    useAppSelector((s) => s.cart.cart)?.items
      ?.filter((i) => !i.expired)
      .reduce((acc, i) => acc + Number(i.totalPrice), 0) ?? 0;

  const { canFinalize, finalizeHint } = useMemo(() => {
    if (!freight) {
      return {
        canFinalize: false,
        finalizeHint: 'Calcule o frete na seção Endereço antes de finalizar.',
      };
    }
    if (!cart?.checkoutAllowed) {
      return {
        canFinalize: false,
        finalizeHint: 'Há itens expirados no carrinho. Atualize o carrinho antes de finalizar.',
      };
    }
    if (!hasBillingAddress || !hasDeliveryAddress) {
      return {
        canFinalize: false,
        finalizeHint:
          !hasDeliveryAddress && !hasBillingAddress
            ? 'Cadastre no perfil ao menos um endereço de entrega e um de cobrança (ou use as opções rápidas no checkout).'
            : !hasDeliveryAddress
              ? 'Cadastre no perfil ao menos um endereço de entrega (RN0022).'
              : 'Cadastre no perfil ao menos um endereço de cobrança (RN0021) ou marque a opção igual à entrega.',
      };
    }
    const grandCents = moneyToCents(Number(freight.grandTotal));
    const couponCents = appliedCoupon ? moneyToCents(appliedCoupon.amount) : 0;
    const remainingCents = grandCents - couponCents;
    if (remainingCents < 0) {
      return {
        canFinalize: false,
        finalizeHint: 'O valor do cupom é maior que o total do pedido.',
      };
    }
    const needsCard = remainingCents > 0;
    if (needsCard && remainingCents < moneyToCents(MIN_CREDIT_CARD_LINE_BRL)) {
      return {
        canFinalize: false,
        finalizeHint: `Valor mínimo de R$ ${MIN_CREDIT_CARD_LINE_BRL.toFixed(2).replace('.', ',')} no cartão. Ajuste o carrinho ou o cupom.`,
      };
    }
    if (needsCard && cardMode === 'saved' && !selectedCardId) {
      return {
        canFinalize: false,
        finalizeHint: 'Selecione um cartão salvo ou use um novo cartão.',
      };
    }
    return { canFinalize: true, finalizeHint: null as string | null };
  }, [
    freight,
    cart?.checkoutAllowed,
    appliedCoupon,
    cardMode,
    selectedCardId,
    hasBillingAddress,
    hasDeliveryAddress,
  ]);

  const onSubmitNewAddress = handleSubmitAddr(async (v) => {
    setMsg(null);
    const effectiveType = pairDeliveryAndBillingCheckout ? 'DELIVERY' : v.type;
    try {
      await dispatch(
        applyAddress({
          customerId,
          body: {
            newAddress: {
              nickname: v.nickname,
              street: v.street,
              number: v.number,
              complement: v.complement,
              neighborhood: v.neighborhood,
              city: v.city,
              state: v.state,
              zipCode: v.zipCode,
              type: effectiveType,
            },
            saveToProfile: saveNewAddress,
          },
        }),
      ).unwrap();
      if (saveNewAddress && pairDeliveryAndBillingCheckout) {
        await customerService.createAddress(customerId, {
          nickname: `${v.nickname.trim()} — cobrança`,
          street: v.street,
          number: v.number,
          complement: v.complement,
          neighborhood: v.neighborhood,
          city: v.city,
          state: v.state,
          zipCode: v.zipCode,
          type: 'BILLING',
        });
      }
      const cartAfter = await dispatch(loadCart(customerId)).unwrap();
      const activeList = await refreshAddresses();
      const nextId =
        cartAfter.deliveryAddressId ?? activeList[activeList.length - 1]?.id ?? null;
      if (nextId) setAddressId(nextId);
      resetAddr(EMPTY_ADDRESS_DEFAULTS);
      setShowNewAddress(false);
      setPairDeliveryAndBillingCheckout(true);
      setMsg('Endereço aplicado. Calcule o frete.');
    } catch (e) {
      setMsg(getErrorMessage(e));
    }
  });

  function toggleNewAddressForm() {
    setShowNewAddress((prev) => {
      const opening = !prev;
      if (opening) {
        resetAddr(EMPTY_ADDRESS_DEFAULTS);
        setPairDeliveryAndBillingCheckout(true);
      }
      return opening;
    });
  }

  async function runFreight() {
    setMsg(null);
    if (!addressId) {
      setMsg('Selecione um endereço.');
      return;
    }
    try {
      await dispatch(applyAddress({ customerId, body: { addressId } })).unwrap();
      await dispatch(calculateFreight({ customerId, addressId })).unwrap();
    } catch (e) {
      setMsg(getErrorMessage(e));
    }
  }

  async function applyCoupon() {
    setMsg(null);
    setCouponFieldError(null);
    if (!freight) {
      setCouponFieldError('Calcule o frete antes de validar o cupom.');
      return;
    }
    const code = couponCodeDraft.trim();
    if (!code) {
      setCouponFieldError('Informe o código do cupom.');
      return;
    }
    try {
      const { amount } = await checkoutService.validateCheckoutCoupon(customerId, {
        code,
        paymentType: couponPaymentType,
      });
      setAppliedCoupon({ code, paymentType: couponPaymentType, amount });
      setMsg('Cupom aplicado.');
    } catch (e) {
      setCouponFieldError(getErrorMessage(e));
    }
  }

  function clearCoupon() {
    setAppliedCoupon(null);
    setCouponFieldError(null);
    setMsg(null);
  }

  async function runPay() {
    setMsg(null);
    dispatch(clearCheckoutError());
    appLogger.info('CheckoutPage', 'runPay', 'Finalizar compra acionado', {
      customerId,
      checkoutAllowed: cart?.checkoutAllowed ?? false,
      hasFreight: Boolean(freight),
      hasCoupon: Boolean(appliedCoupon),
    });
    if (!cart?.checkoutAllowed) {
      appLogger.warn('CheckoutPage', 'runPay', 'Bloqueado: itens expirados no carrinho', { customerId });
      setMsg('Há itens expirados. Atualize o carrinho antes de finalizar.');
      return;
    }
    if (!freight) {
      appLogger.warn('CheckoutPage', 'runPay', 'Bloqueado: frete não calculado', { customerId });
      setMsg('Calcule o frete antes de finalizar.');
      return;
    }

    const grandCents = moneyToCents(Number(freight.grandTotal));
    const couponCents = appliedCoupon ? moneyToCents(appliedCoupon.amount) : 0;
    const remainingCents = grandCents - couponCents;
    /* RN0036: valor do cupom pode exceder o total; o back-end emite cupom de troco (sem cartão). */

    const payCheck = validateCheckoutPaymentAmounts({
      grandTotal: Number(freight.grandTotal),
      couponAmount: appliedCoupon ? appliedCoupon.amount : 0,
      requiresCreditCard: remainingCents > 0,
      creditCardLineAmount: remainingCents > 0 ? centsToMoney(remainingCents) : 0,
    });
    if (!payCheck.valid) {
      setMsg(payCheck.errors.payment ?? 'Pagamento inválido.');
      return;
    }

    const lines: checkoutService.PaymentLine[] = [];
    let newCard: checkoutService.NewCreditCardPayload | undefined;
    let saveNewCardToProfile: boolean | undefined;

    if (appliedCoupon) {
      lines.push({
        paymentType: appliedCoupon.paymentType,
        amount: centsToMoney(couponCents),
        couponCode: appliedCoupon.code,
      });

      if (remainingCents > 0) {
        if (cardMode === 'saved' && !selectedCardId) {
          setMsg('Selecione um cartão salvo ou escolha "Novo cartão" para completar o pagamento.');
          return;
        }
        if (cardMode === 'new') {
          const ok = await cardForm.trigger();
          if (!ok) {
            setMsg('Confira os dados do cartão nos campos destacados abaixo.');
            return;
          }
          const c = cardForm.getValues();
          newCard = {
            cardholderName: c.cardholderName,
            cardNumber: String(c.cardNumber).replace(/\D/g, ''),
            brand: c.brand,
            expirationMonth: Number(c.expirationMonth),
            expirationYear: Number(c.expirationYear),
            preferred: c.preferred ?? false,
          };
          saveNewCardToProfile = saveNewCard;
        }
        lines.push({
          paymentType: 'CREDIT_CARD',
          amount: centsToMoney(remainingCents),
          creditCardId: cardMode === 'saved' ? selectedCardId ?? undefined : undefined,
        });
      }
    } else {
      if (cardMode === 'saved' && !selectedCardId) {
        setMsg('Selecione um cartão salvo ou escolha "Novo cartão" para pagar.');
        return;
      }
      if (cardMode === 'new') {
        const ok = await cardForm.trigger();
        if (!ok) {
          setMsg('Confira os dados do cartão nos campos destacados abaixo.');
          return;
        }
        const c = cardForm.getValues();
        newCard = {
          cardholderName: c.cardholderName,
          cardNumber: String(c.cardNumber).replace(/\D/g, ''),
          brand: c.brand,
          expirationMonth: Number(c.expirationMonth),
          expirationYear: Number(c.expirationYear),
          preferred: c.preferred ?? false,
        };
        saveNewCardToProfile = saveNewCard;
      }
      lines.push({
        paymentType: 'CREDIT_CARD',
        amount: centsToMoney(grandCents),
        creditCardId: cardMode === 'saved' ? selectedCardId ?? undefined : undefined,
      });
    }

    setCheckoutBusy(true);
    try {
      await dispatch(
        applyPayment({
          customerId,
          body: {
            lines,
            newCreditCard: newCard,
            saveNewCardToProfile: cardMode === 'new' ? saveNewCardToProfile : undefined,
          },
        }),
      ).unwrap();

      await dispatch(finalizeOrder(customerId)).unwrap();
      await dispatch(loadCart(customerId)).unwrap();
      await dispatch(fetchOrdersAndTransactions(customerId)).unwrap();
      navigate(ROUTES.profileOrders);
    } catch (e) {
      setMsg(getErrorMessage(e));
    } finally {
      setCheckoutBusy(false);
    }
  }

  const submitAddCardToProfile = handleSubmitCard(async (c) => {
    setMsg(null);
    try {
      const payload = {
        cardholderName: c.cardholderName,
        cardNumber: c.cardNumber,
        brand: c.brand,
        expirationMonth: c.expirationMonth,
        expirationYear: c.expirationYear,
        preferred: c.preferred ?? false,
      };

      const created = await customerService.createCard(customerId, payload);
      const list = await customerService.listCards(customerId);
      const active = list.filter((x) => x.active);
      setCards(active);
      const pref = active.find((x) => x.preferred);
      const nextId = pref?.id ?? created.id ?? active[0]?.id ?? null;
      setSelectedCardId(nextId);
      setCardMode('saved');
      cardForm.reset(EMPTY_CARD_DEFAULTS);
      setMsg('Cartão adicionado ao perfil. Você pode selecioná-lo em “Cartões salvos”.');
    } catch (e) {
      setMsg(getErrorMessage(e));
    }
  });

  /** Alinhado ao runPay: total = grandTotal − cupom (mínimo 0). O resumo exibe a linha Descontos. */
  const { summaryDiscount, summaryTotal } = useMemo(() => {
    if (!freight) {
      return { summaryDiscount: undefined as number | undefined, summaryTotal: itemsSub };
    }
    const base = Number(freight.grandTotal);
    const off = appliedCoupon?.amount ?? 0;
    return {
      summaryDiscount: off > 0 ? off : undefined,
      summaryTotal: Math.max(0, base - off),
    };
  }, [freight, appliedCoupon, itemsSub]);

  const checkoutBlocked = cart != null && !cart.checkoutAllowed;

  const checkoutErrorText = getErrorMessage(msg ?? checkoutErr ?? '');

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="font-display text-3xl font-semibold">Checkout</h1>
      {cart?.stockAdjustmentMessages && cart.stockAdjustmentMessages.length > 0 && (
        <div className="mt-4 space-y-2 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-950">
          {cart.stockAdjustmentMessages.map((m) => (
            <p key={m}>{m}</p>
          ))}
        </div>
      )}
      {(msg || checkoutErr) && (
        <p
          className={`mt-2 text-sm ${
            checkoutErr ||
            !msg ||
            (!msg.includes('Endereço aplicado') &&
              !msg.includes('Cupom aplicado') &&
              !msg.includes('Cartão adicionado') &&
              !msg.includes('Endereço de cobrança cadastrado'))
              ? 'text-red-600'
              : 'text-green-700'
          }`}
        >
          {checkoutErrorText}
        </p>
      )}
      {(!hasBillingAddress || !hasDeliveryAddress) && (
        <div className="mt-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-950">
          <p className="font-medium">Endereços de entrega e cobrança no perfil</p>
          <p className="mt-1 leading-relaxed">
            Para finalizar é preciso ter no perfil ao menos um endereço de <strong>entrega</strong> (RN0022) e um de{' '}
            <strong>cobrança</strong> (RN0021). O endereço de entrega abaixo define só o envio; cobrança é outro
            cadastro. Você pode{' '}
            <strong>marcar a opção abaixo</strong> para copiar o endereço de entrega como cobrança, cadastrar em{' '}
            <Link to={ROUTES.profileAddresses} className="font-medium text-brand underline">
              Perfil → Endereços
            </Link>{' '}
            com a opção "mesmo para entrega e cobrança", ou usar o formulário nesta página com a mesma
            opção.
          </p>
        </div>
      )}
      {checkoutBlocked && (
        <p className="mt-2 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">
          Há itens expirados no carrinho. Volte ao carrinho para readicionar ou remover itens antes de finalizar.
        </p>
      )}

      <div className="mt-8 grid gap-10 lg:grid-cols-[1fr_340px]">
        <div className="space-y-10">
          <section>
            <h2 className="font-semibold text-lg">Endereço de entrega</h2>
            <div className="mt-3 space-y-2">
              {addresses.map((a) => (
                <label key={a.id} className="flex cursor-pointer gap-2 rounded-lg border p-3">
                  <input
                    type="radio"
                    name="addr"
                    checked={addressId === a.id}
                    onChange={() => setAddressId(a.id)}
                  />
                  <span>
                    <strong>{a.nickname}</strong>
                    {a.type ? (
                      <span className="text-ink-muted"> ({a.type === 'BILLING' ? 'Cobrança' : 'Entrega'})</span>
                    ) : null}{' '}
                    — {a.street}, {a.number} — {a.city}/{a.state}
                  </span>
                </label>
              ))}
            </div>
            {!hasBillingAddress && addressId && (
              <label className="mt-3 flex cursor-pointer items-start gap-2 text-sm leading-snug">
                <input
                  type="checkbox"
                  className="mt-0.5"
                  disabled={billingSyncing || hasBillingAddress}
                  checked={hasBillingAddress || billingSyncing}
                  onChange={(e) => {
                    if (e.target.checked && !hasBillingAddress) void copyBillingFromSelectedDelivery();
                  }}
                />
                <span>
                  <strong>Endereço de cobrança igual ao de entrega</strong> — cria no perfil um segundo endereço
                  (tipo cobrança) com os mesmos dados do selecionado acima, sem duplicar o formulário.
                </span>
              </label>
            )}
            <button
              type="button"
              onClick={toggleNewAddressForm}
              className="mt-3 block text-sm font-medium text-brand hover:underline"
            >
              {showNewAddress ? 'Fechar formulário' : '+ Cadastrar novo endereço'}
            </button>
            {showNewAddress && (
              <div className="mt-4 grid gap-2 rounded-lg border p-4 sm:grid-cols-2">
                <p className="sm:col-span-2 text-xs text-ink-muted">
                  Preencha todos os campos. UF: sigla com 2 letras (ex.: MG). CEP: 8 dígitos (com ou sem hífen).
                </p>
                <div className="sm:col-span-1">
                  <input {...registerAddr('nickname')} placeholder="Apelido" className="w-full rounded border px-2 py-1.5 text-sm" />
                  {addrErrors.nickname && (
                    <p className="mt-0.5 text-xs text-red-600">{addrErrors.nickname.message}</p>
                  )}
                </div>
                <div className="sm:col-span-1">
                  <input {...registerAddr('street')} placeholder="Rua" className="w-full rounded border px-2 py-1.5 text-sm" />
                  {addrErrors.street && <p className="mt-0.5 text-xs text-red-600">{addrErrors.street.message}</p>}
                </div>
                <div className="sm:col-span-1">
                  <input {...registerAddr('number')} placeholder="Número" className="w-full rounded border px-2 py-1.5 text-sm" />
                  {addrErrors.number && <p className="mt-0.5 text-xs text-red-600">{addrErrors.number.message}</p>}
                </div>
                <div className="sm:col-span-1">
                  <input {...registerAddr('neighborhood')} placeholder="Bairro" className="w-full rounded border px-2 py-1.5 text-sm" />
                  {addrErrors.neighborhood && (
                    <p className="mt-0.5 text-xs text-red-600">{addrErrors.neighborhood.message}</p>
                  )}
                </div>
                <div className="sm:col-span-1">
                  <input {...registerAddr('city')} placeholder="Cidade" className="w-full rounded border px-2 py-1.5 text-sm" />
                  {addrErrors.city && <p className="mt-0.5 text-xs text-red-600">{addrErrors.city.message}</p>}
                </div>
                <div className="sm:col-span-1">
                  <input {...registerAddr('state')} placeholder="UF (ex: MG)" className="w-full rounded border px-2 py-1.5 text-sm" />
                  {addrErrors.state && <p className="mt-0.5 text-xs text-red-600">{addrErrors.state.message}</p>}
                </div>
                <div className="sm:col-span-1">
                  <input {...registerAddr('zipCode')} placeholder="CEP" className="w-full rounded border px-2 py-1.5 text-sm" />
                  {addrErrors.zipCode && <p className="mt-0.5 text-xs text-red-600">{addrErrors.zipCode.message}</p>}
                </div>
                <label className="flex items-start gap-2 sm:col-span-2 text-sm leading-snug">
                  <input
                    type="checkbox"
                    className="mt-0.5"
                    disabled={!saveNewAddress}
                    checked={pairDeliveryAndBillingCheckout}
                    onChange={(e) => setPairDeliveryAndBillingCheckout(e.target.checked)}
                  />
                  <span>
                    <strong>Mesmo endereço para entrega e cobrança</strong> — ao salvar no perfil, cria também o
                    registro de cobrança (apelido com sufixo "— cobrança"). Desmarque para escolher só
                    entrega ou só cobrança.
                  </span>
                </label>
                {!pairDeliveryAndBillingCheckout && (
                  <select {...registerAddr('type')} className="rounded border px-2 py-1.5 text-sm sm:col-span-2">
                    <option value="DELIVERY">Entrega</option>
                    <option value="BILLING">Cobrança</option>
                  </select>
                )}
                <label className="flex items-center gap-2 sm:col-span-2 text-sm">
                  <input
                    type="checkbox"
                    checked={saveNewAddress}
                    onChange={(e) => {
                      const next = e.target.checked;
                      setSaveNewAddress(next);
                      if (!next) setPairDeliveryAndBillingCheckout(false);
                    }}
                  />
                  Salvar no perfil (para usar em outros pedidos)
                </label>
                {saveNewAddress && pairDeliveryAndBillingCheckout && (
                  <p className="sm:col-span-2 text-xs text-ink-muted">
                    O pedido será enviado para este endereço (entrega); a cobrança será duplicada no perfil para
                    atender a RN0021.
                  </p>
                )}
                {pairDeliveryAndBillingCheckout && !saveNewAddress && (
                  <p className="sm:col-span-2 text-xs text-amber-800">
                    Para criar entrega e cobrança juntos no perfil, marque também "Salvar no perfil".
                  </p>
                )}
                <button
                  type="button"
                  onClick={() => void onSubmitNewAddress()}
                  className="sm:col-span-2 rounded-lg bg-stone-800 py-2 text-sm text-white"
                >
                  Usar este endereço
                </button>
              </div>
            )}
            <button
              type="button"
              onClick={() => void runFreight()}
              className="mt-3 block rounded-lg bg-brand px-4 py-2 text-sm text-white"
            >
              Calcular frete
            </button>
          </section>

          <section>
            <h2 className="font-semibold text-lg">Pagamento</h2>

            <div className="mt-4 rounded-lg border border-stone-200 bg-stone-50/50 p-4">
              <p className="text-sm font-medium text-ink">Cupom de troca ou promocional (opcional)</p>
              <p className="mt-1 text-xs text-ink-muted">
                Você pode combinar 1 cupom com cartão. O valor da linha do cupom deve ser o valor fixo do cupom.
              </p>
              <label className="mt-3 block text-sm">
                <span className="font-medium">Tipo</span>
                <select
                  value={couponPaymentType}
                  onChange={(e) =>
                    setCouponPaymentType(e.target.value as 'EXCHANGE_COUPON' | 'PROMOTIONAL_COUPON')
                  }
                  disabled={!!appliedCoupon}
                  className="mt-1 block w-full max-w-xs rounded border px-2 py-1.5 text-sm"
                >
                  <option value="EXCHANGE_COUPON">Cupom de troca</option>
                  <option value="PROMOTIONAL_COUPON">Cupom promocional</option>
                </select>
              </label>
              <div className="mt-3">
                <CouponInput
                  label="Código do cupom"
                  value={couponCodeDraft}
                  onChange={setCouponCodeDraft}
                  onApply={() => void applyCoupon()}
                  error={couponFieldError}
                  disabled={!freight || !!appliedCoupon}
                />
              </div>
              {appliedCoupon && (
                <div className="mt-3 flex flex-wrap items-center justify-between gap-2 rounded border border-brand/30 bg-white px-3 py-2 text-sm">
                  <span>
                    <strong>{appliedCoupon.code}</strong> — {formatBRL(appliedCoupon.amount)}
                  </span>
                  <button type="button" onClick={clearCoupon} className="text-brand hover:underline">
                    Remover
                  </button>
                </div>
              )}
            </div>

            <div className="mt-6 space-y-2">
              <p className="text-sm font-medium text-ink">Cartão de crédito</p>
              <label className="flex items-center gap-2 text-sm">
                <input type="radio" checked={cardMode === 'saved'} onChange={() => setCardMode('saved')} />
                Cartões salvos
              </label>
              {cardMode === 'saved' &&
                cards.map((c) => (
                  <label key={c.id} className="flex cursor-pointer gap-2 rounded-lg border p-3">
                    <input
                      type="radio"
                      name="card"
                      checked={selectedCardId === c.id}
                      onChange={() => setSelectedCardId(c.id)}
                    />
                    <span>
                      {c.brand} {c.cardNumberMasked}
                    </span>
                  </label>
                ))}
              <label className="flex items-center gap-2 text-sm">
                <input type="radio" checked={cardMode === 'new'} onChange={() => setCardMode('new')} />
                Novo cartão
              </label>
              {cardMode === 'new' && (
                <div className="grid gap-2 rounded-lg border p-4 sm:grid-cols-2">
                  <p className="sm:col-span-2 text-xs text-ink-muted">
                    Validade: ano com 4 dígitos (ano em curso ou futuro; ex.: mês 01, ano 2030). Número pode ter espaços.
                  </p>
                  <div className="sm:col-span-1">
                    <input {...registerCard('cardholderName')} placeholder="Nome no cartão" className="w-full rounded border px-2 py-1.5 text-sm" />
                    {cardErrors.cardholderName && (
                      <p className="mt-0.5 text-xs text-red-600">{cardErrors.cardholderName.message}</p>
                    )}
                  </div>
                  <div className="sm:col-span-1">
                    <input {...registerCard('cardNumber')} placeholder="Número do cartão" className="w-full rounded border px-2 py-1.5 text-sm" />
                    {cardErrors.cardNumber && (
                      <p className="mt-0.5 text-xs text-red-600">{cardErrors.cardNumber.message}</p>
                    )}
                  </div>
                  <div className="sm:col-span-1">
                    <input {...registerCard('brand')} placeholder="Bandeira" className="w-full rounded border px-2 py-1.5 text-sm" />
                    {cardErrors.brand && <p className="mt-0.5 text-xs text-red-600">{cardErrors.brand.message}</p>}
                  </div>
                  <div className="sm:col-span-1">
                    <input
                      type="number"
                      {...registerCard('expirationMonth')}
                      placeholder="Mês (1–12)"
                      className="w-full rounded border px-2 py-1.5 text-sm"
                    />
                    {cardErrors.expirationMonth && (
                      <p className="mt-0.5 text-xs text-red-600">{cardErrors.expirationMonth.message}</p>
                    )}
                  </div>
                  <div className="sm:col-span-1">
                    <input
                      type="number"
                      {...registerCard('expirationYear')}
                      placeholder="Ano (ex: 2030)"
                      className="w-full rounded border px-2 py-1.5 text-sm"
                    />
                    {cardErrors.expirationYear && (
                      <p className="mt-0.5 text-xs text-red-600">{cardErrors.expirationYear.message}</p>
                    )}
                  </div>
                  <label className="flex items-center gap-2 sm:col-span-2 text-sm">
                    <input type="checkbox" checked={saveNewCard} onChange={(e) => setSaveNewCard(e.target.checked)} />
                    Salvar cartão no perfil
                  </label>
                  <label className="flex items-center gap-2 sm:col-span-2 text-sm">
                    <input type="checkbox" {...registerCard('preferred')} />
                    Definir como preferencial
                  </label>

                  <button
                    type="button"
                    onClick={() => void submitAddCardToProfile()}
                    className="sm:col-span-2 rounded-lg border border-stone-300 bg-white py-2 text-sm font-semibold text-ink hover:bg-stone-50"
                  >
                    Adicionar cartão ao perfil
                  </button>
                </div>
              )}
            </div>

            {finalizeHint && (
              <p className="mt-6 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
                {finalizeHint}
              </p>
            )}
            <button
              type="button"
              title={!canFinalize ? finalizeHint ?? 'Preencha os requisitos para finalizar' : undefined}
              disabled={checkoutBusy || checkoutStatus === 'loading' || checkoutBlocked || !canFinalize}
              onClick={() => void runPay()}
              className="mt-4 w-full rounded-xl bg-accent py-3 text-sm font-semibold text-white hover:bg-accent-hover disabled:opacity-50"
            >
              {checkoutBusy || checkoutStatus === 'loading' ? 'Finalizando…' : 'Finalizar compra'}
            </button>
          </section>
        </div>

        <aside>
          {freight ? (
            <OrderSummary
              itemsSubtotal={itemsSub}
              freight={Number(freight.freightAmount)}
              discount={summaryDiscount}
              total={summaryTotal}
            />
          ) : (
            <OrderSummary itemsSubtotal={itemsSub} freight={null} total={summaryTotal} />
          )}
        </aside>
      </div>
    </div>
  );
}
