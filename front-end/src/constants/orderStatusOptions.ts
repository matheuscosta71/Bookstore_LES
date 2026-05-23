/** Valores de `OrderStatus` no backend — para filtros admin. */
export const ADMIN_ORDER_STATUS_FILTER = [
  { value: '', label: 'Todos' },
  { value: 'EM_PROCESSAMENTO', label: 'Em processamento' },
  { value: 'APROVADO', label: 'Pagamento aprovado' },
  { value: 'PAGAMENTO_RECUSADO', label: 'Pagamento recusado' },
  { value: 'EM_TRANSITO', label: 'Em trânsito' },
  { value: 'ENTREGUE', label: 'Entregue' },
  { value: 'EM_TROCA', label: 'Em troca' },
  { value: 'TROCA_AUTORIZADA', label: 'Troca autorizada' },
] as const;
