# Estoque e análise de vendas — exemplos JSON (API REST)

Base URL: `http://localhost:8080`  
Admin: enviar header `X-Admin-Key` (valor configurável em `app.admin.key`, padrão `8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12`).

## Entrada manual de estoque (RF0051)

### POST `/inventory/entries`

**Request**

```json
{
  "bookId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "quantity": 10,
  "unitCost": "25.50",
  "reason": "PURCHASE"
}
```

`reason` (opcional): `PURCHASE`, `ADJUSTMENT`, `OTHER`.

**Response** (`204`): sem corpo.

---

## Consulta de saldo por livro

### GET `/inventory/books/{bookId}`

**Response** (`200`)

```json
{
  "bookId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "title": "Clean Code",
  "isbn": "9780132350884",
  "category": "Software",
  "quantityAvailable": 42,
  "lastUpdatedAt": "2025-01-15T18:30:00Z"
}
```

---

## Movimentações

### GET `/inventory/movements`

Query opcionais: `bookId`, `movementType` (`ENTRY`, `SALE_OUTBOUND`, `EXCHANGE_RETURN`), `startDate`, `endDate` (ISO date), paginação Spring (`page`, `size`, `sort`).

**Response** (`200`): página de `InventoryMovementResponse` (`id`, `bookId`, `bookTitle`, `movementType`, `referenceType`, `referenceId`, `quantity`, `notes`, `createdAt`).

---

## Baixa por pedido (RF0053)

### POST `/inventory/sales-outbound`

**Request**

```json
{
  "orderId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
}
```

Idempotente: se já existir movimento com `referenceType` `ORDER` e `referenceId` = `orderId`, não repete a baixa.

**Response** (`204`)

---

## Reentrada por troca (RF0054)

### POST `/inventory/reentries/exchange`

**Request**

```json
{
  "exchangeRequestId": "cccccccc-cccc-cccc-cccc-cccccccccccc"
}
```

Somente para troca com status `RECEIVED` e `returnToStock == true`. Idempotente por `exchangeRequestId`.

**Response** (`204`)

---

## Precificação

### POST `/books/{bookId}/recalculate-sale-price`

Corpo vazio. Recalcula `salePrice = costPrice * (1 + percentage/100)` usando o grupo de precificação do livro.

**Response** (`200`): `BookResponse` (inclui `price` como preço de venda).

---

## Analytics (RF0055)

### GET `/analytics/sales-history`

Query: `startDate`, `endDate` (ISO date, inclusive).

**Response** (`200`)

```json
{
  "totalRevenue": "1500.00",
  "totalItemsSold": 23,
  "orderCount": 8
}
```

### GET `/analytics/sales-history/books`

Mesmos query params.

**Response** (`200`): `{ "books": [ { "bookId": "...", "title": "...", "revenue": "...", "quantitySold": 5 } ] }`

### GET `/analytics/sales-history/categories`

**Response** (`200`): `{ "categories": [ { "category": "Software", "revenue": "...", "quantitySold": 10 } ] }`

---

## Cadastro de livro (campos novos)

No POST/PUT `/books`, opcionalmente:

- `costPrice`: custo; se omitido na criação, assume o mesmo valor de `price` (venda).
- `pricingGroupId`: UUID do grupo; se omitido, usa o grupo **Padrão** (criado na migração).

O campo JSON `price` continua representando o **preço de venda** (`salePrice` no modelo).
