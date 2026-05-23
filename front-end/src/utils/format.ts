export function formatBRL(value: number | string | null | undefined): string {
  if (value === null || value === undefined) return '—';
  const n = typeof value === 'string' ? parseFloat(value) : value;
  if (Number.isNaN(n)) return '—';
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(n);
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  try {
    return new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(iso));
  } catch {
    return iso;
  }
}

/** Converte texto (ex.: 1234,56 ou 1.234,56) em número para filtros / formulários. */
export function parseMoneyInput(s: string): number | undefined {
  const t = s.trim();
  if (!t) return undefined;
  const normalized = t.replace(/\./g, '').replace(',', '.');
  const n = parseFloat(normalized);
  return Number.isFinite(n) ? n : undefined;
}

/** Apenas data (dd/MM/yyyy), para telas administrativas. */
export function formatDateOnly(iso: string | null | undefined): string {
  if (!iso) return '—';
  try {
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(new Date(iso));
  } catch {
    return '—';
  }
}

/** Classes Tailwind para badge de status do pedido (admin). */
export function adminOrderStatusBadgeClass(status: string | null | undefined): string {
  const base = 'inline-flex rounded-full border px-2.5 py-0.5 text-xs font-medium';
  switch (status) {
    case 'EM_PROCESSAMENTO':
      return `${base} border-amber-200 bg-amber-100 text-amber-950`;
    case 'APROVADO':
      return `${base} border-emerald-200 bg-emerald-50 text-emerald-900`;
    case 'EM_TRANSITO':
      return `${base} border-blue-200 bg-blue-100 text-blue-900`;
    case 'ENTREGUE':
      return `${base} border-green-200 bg-green-100 text-green-900`;
    case 'PAGAMENTO_RECUSADO':
      return `${base} border-red-200 bg-red-50 text-red-900`;
    case 'EM_TROCA':
    case 'TROCA_AUTORIZADA':
      return `${base} border-violet-200 bg-violet-50 text-violet-900`;
    default:
      return `${base} border-stone-200 bg-stone-100 text-stone-800`;
  }
}

/** Rótulos alinhados a `OrderStatus` no backend (RF0037, RF0038, RF0039, trocas). */
const ORDER_STATUS_LABEL: Record<string, string> = {
  EM_PROCESSAMENTO: 'Em processamento',
  APROVADO: 'Pagamento aprovado',
  PAGAMENTO_RECUSADO: 'Pagamento recusado',
  EM_TRANSITO: 'Em trânsito',
  ENTREGUE: 'Entregue',
  EM_TROCA: 'Em troca',
  TROCA_AUTORIZADA: 'Troca autorizada',
};

export function formatOrderStatus(status: string | null | undefined): string {
  if (!status) return '—';
  return ORDER_STATUS_LABEL[status] ?? status.replace(/_/g, ' ');
}

/** Status da solicitação de troca (`ExchangeStatus`). */
const EXCHANGE_STATUS_LABEL: Record<string, string> = {
  REQUESTED: 'Solicitada',
  AUTHORIZED: 'Autorizada',
  RECEIVED: 'Recebida',
};

export function formatExchangeStatus(status: string | null | undefined): string {
  if (!status) return '—';
  return EXCHANGE_STATUS_LABEL[status] ?? status;
}
