/**
 * Mapa tela ↔ endpoint (PR / documentação curta).
 *
 * Público / cliente (sem X-Admin-Key; cliente usa `/customers/{customerId}/...` após login):
 * - `/`, `/books` — GET `/books`
 * - `/books/:id` — GET `/books/{id}`
 * - `/cart` — GET/POST/PUT/DELETE `/customers/{id}/cart`…
 * - `/checkout` — POST `/customers/{id}/checkout/*`
 * - `/login`, `/register` — GET `/customers?email=`, GET `/customers/{id}`, POST `/customers`
 * - `/profile/*` — endereços, cartões, transações, pedidos — rotas sob `/customers/{id}/`
 * - `/profile/orders` — troca: POST `/customers/{id}/orders/{orderId}/exchange-requests`
 * - `/ai/chat`, home — POST `/ai/chat`, POST `/ai/recommendations/{customerId}`
 *
 * Admin (header `X-Admin-Key` = `VITE_ADMIN_KEY` via `adminApi`):
 * - Controle de estoque / análise (RF0051–RF0055):
 *   - RF0051 entrada manual — POST `/inventory/entries` (livro + quantidade + custo unitário); tela `/admin/inventory`
 *   - RF0052 preço de venda (custo + % do grupo) — aplicado ao salvar livro quando custo e grupo existem; recálculo explícito POST `/books/{id}/recalculate-sale-price`
 *   - RF0053 baixa por venda — automático no checkout (`SalesOutboundService`); reprocesso POST `/inventory/sales-outbound`
 *   - RF0054 reentrada por troca — ao receber troca com retorno ao estoque (`ExchangeService.receive`); reprocesso POST `/inventory/reentries/exchange`
 *   - RF0055 histórico por período — GET `/analytics/sales-history`, `.../books`, `.../categories`, `.../line-chart`; tela `/admin/analytics`
 * - `/admin/analytics` — GET `/analytics/sales-history`, `.../books`, `.../categories`, `.../line-chart`
 * - `/admin/orders` — GET `/admin/orders` (lista paginada; query: orderNumber, customerName, status, dateFrom, dateTo, totalMin, totalMax, page, size, sort); PATCH `.../approve-payment`, `.../reject-payment`, `.../dispatch`, `.../deliver`
 * - `/admin/exchanges` — GET `/admin/exchange-requests?status=`, PATCH `.../authorize`, `.../receive`
 * - `/admin/inventory` — POST `/inventory/entries`, GET `/inventory/books/{bookId}`, GET `/inventory/movements`, POST `/inventory/sales-outbound`, POST `/inventory/reentries/exchange`
 * - `/admin/audit` — GET `/audit-logs`, GET `/audit-logs/{entity}/{id}`
 * - `/admin/books` — POST/PUT/DELETE/PATCH `/books`, POST `/books/inactivate-automatic`, POST `/books/{id}/recalculate-sale-price` (última exige chave)
 * - `/admin/customers` — GET `/customers` (filtros; API não exige chave hoje)
 *
 * RNF (rastreabilidade; detalhes no código / documentação de requisitos):
 * - RNF0011 — SLA 1s: não garantido só pelo app; ver `PageConstraints` (back-end) e APM/carga em produção.
 * - RNF0012 — log de transação: GET `/audit-logs` (admin); escritas auditadas incluem livro/cliente, estoque, pedido, troca, endereço.
 * - RNF0021 / RNF0035 — códigos únicos: entidades livro/cliente (APIs de cadastro).
 * - RNF0031–RNF0034 — senha e endereços: `/register`, `/profile/addresses`, etc.
 * - RNF0042 — carrinho expirado (soft): `/cart` mostra linhas expiradas; total e checkout ignoram-nas; `/checkout` valida no servidor.
 * - RNF0043 — gráfico de vendas: `/admin/analytics`, GET `.../line-chart`.
 * - RNF0044 — IA: `/ai/chat`, recomendações na home; treinamento contínuo do modelo é responsabilidade operacional/fornecedor, não só front.
 */

export const SCREEN_ENDPOINT_MAP_NOTE =
  'Ver comentário em screenEndpointMap.ts para o mapa completo tela ↔ endpoint.';
