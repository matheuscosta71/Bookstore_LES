/** RNF0042: o back-end só envia `reservationExpiredMessages` na resposta em que o purge ocorre; nas próximas cargas o carrinho já está vazio sem mensagens. Persistimos o aviso na sessão do browser para o usuário ainda ver o motivo. */

const KEY = (customerId: string) => `rnf0042_cart_notice_${customerId}`;

const MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000;

export type StoredReservationNotice = {
  messages: string[];
  itemExpirationMinutes: number;
  savedAt: number;
};

export function saveReservationNotice(
  customerId: string,
  data: { messages: string[]; itemExpirationMinutes: number },
): void {
  if (typeof window === 'undefined' || !data.messages.length) return;
  const payload: StoredReservationNotice = {
    ...data,
    savedAt: Date.now(),
  };
  try {
    sessionStorage.setItem(KEY(customerId), JSON.stringify(payload));
  } catch {
    /* quota / private mode */
  }
}

export function readReservationNotice(customerId: string): StoredReservationNotice | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = sessionStorage.getItem(KEY(customerId));
    if (!raw) return null;
    const p = JSON.parse(raw) as StoredReservationNotice;
    if (!p.messages?.length) return null;
    if (Date.now() - p.savedAt > MAX_AGE_MS) {
      sessionStorage.removeItem(KEY(customerId));
      return null;
    }
    return p;
  } catch {
    return null;
  }
}

export function clearReservationNotice(customerId: string): void {
  if (typeof window === 'undefined') return;
  try {
    sessionStorage.removeItem(KEY(customerId));
  } catch {
    /* ignore */
  }
}
