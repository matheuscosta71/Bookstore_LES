# Vendas eletrônicas — exemplos JSON (API REST)

Base URL: `http://localhost:8080`  
Admin: enviar header `X-Admin-Key` (valor configurável em `app.admin.key`, padrão `8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12`).

## Carrinho

### POST `/customers/{customerId}/cart/items`

**Request**

```json
{
  "bookId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "quantity": 2
}
```

**Response** (`200`): corpo `CartResponse` com `id`, `status` (`OPEN`), `totalAmount`, `items[]` (`id`, `bookId`, `title`, `quantity`, `unitPrice`, `totalPrice`).

### PUT `/customers/{customerId}/cart/items/{itemId}`

Mesmo body do POST (nova quantidade).

### GET `/customers/{customerId}/cart`

Retorna o carrinho aberto (cria um se necessário).

---

## Checkout

### POST `/customers/{customerId}/checkout/freight`

```json
{
  "addressId": "11111111-1111-1111-1111-111111111111"
}
```

**Response**

```json
{
  "freightAmount": 25.50,
  "itemsSubtotal": 100.00,
  "grandTotal": 125.50
}
```

### POST `/customers/{customerId}/checkout/address`

Endereço já cadastrado:

```json
{
  "addressId": "11111111-1111-1111-1111-111111111111",
  "newAddress": null,
  "saveToProfile": null
}
```

Novo endereço (obrigatório `saveToProfile`):

```json
{
  "addressId": null,
  "newAddress": {
    "nickname": "Casa",
    "street": "Rua A",
    "number": "100",
    "complement": null,
    "neighborhood": "Centro",
    "city": "São Paulo",
    "state": "SP",
    "zipCode": "01310100",
    "type": "RESIDENTIAL"
  },
  "saveToProfile": true
}
```

### POST `/customers/{customerId}/checkout/payment`

Cartão já cadastrado + cupom promocional (exemplo):

```json
{
  "lines": [
    {
      "paymentType": "CREDIT_CARD",
      "amount": 50.00,
      "creditCardId": "22222222-2222-2222-2222-222222222222",
      "couponCode": null
    },
    {
      "paymentType": "PROMOTIONAL_COUPON",
      "amount": 10.00,
      "creditCardId": null,
      "couponCode": "PROMO10"
    }
  ],
  "newCreditCard": null,
  "saveNewCardToProfile": null
}
```

Novo cartão no checkout:

```json
{
  "lines": [
    {
      "paymentType": "CREDIT_CARD",
      "amount": 125.50,
      "creditCardId": null,
      "couponCode": null
    }
  ],
  "newCreditCard": {
    "cardholderName": "Maria Silva",
    "cardNumber": "4111111111111111",
    "brand": "VISA",
    "expirationMonth": 12,
    "expirationYear": 2030,
    "preferred": false
  },
  "saveNewCardToProfile": true
}
```

A soma das linhas deve ser igual a **itens + frete** (grand total).

### POST `/customers/{customerId}/checkout/finalize`

Sem body. **Response** (`201`): `OrderResponse` com `status`: `EM_PROCESSAMENTO`.

---

## Pedidos do cliente

### GET `/customers/{customerId}/orders`

Lista pedidos.

### GET `/customers/{customerId}/orders/{orderId}`

Detalhe do pedido (`items`, `payments`, totais).

### POST `/customers/{customerId}/orders/{orderId}/exchange-requests`

```json
{
  "orderItemId": "33333333-3333-3333-3333-333333333333"
}
```

**Response**: `ExchangeRequestResponse` (`exchangeStatus`: `REQUESTED`, `orderStatus`: `EM_TROCA`).

---

## Admin — entrega

### PATCH `/admin/orders/{orderId}/dispatch`

Headers: `X-Admin-Key: 8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12`
Sem body. Pedido `EM_PROCESSAMENTO` ou `APROVADO` → `EM_TRANSITO`.

### PATCH `/admin/orders/{orderId}/deliver`

Pedido `EM_TRANSITO` → `ENTREGUE`.

---

## Admin — trocas

### GET `/admin/exchange-requests?status=EM_TROCA`

Lista solicitações cujo **pedido** está no status informado.

### PATCH `/admin/exchange-requests/{exchangeRequestId}/authorize`

### PATCH `/admin/exchange-requests/{exchangeRequestId}/receive`

```json
{
  "returnToStock": true
}
```

Gera cupom de troca (`generatedCouponCode`) e associa ao cliente.
