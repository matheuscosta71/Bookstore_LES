# Exemplos JSON — módulo Customer (RF0021–RF0028)

Base URL: `http://localhost:8080`

## Cliente

### POST /customers — cadastro (RF0021)

**Request**

```json
{
  "fullName": "Maria Silva",
  "email": "maria@email.com",
  "cpf": "52998224725",
  "phone": "31999998888",
  "birthDate": "1990-05-20",
  "password": "senhaForte8",
  "active": true
}
```

**Response 201**

```json
{
  "id": "uuid-gerado",
  "fullName": "Maria Silva",
  "email": "maria@email.com",
  "cpf": "52998224725",
  "phone": "31999998888",
  "birthDate": "1990-05-20",
  "active": true,
  "createdAt": "2026-03-22T23:00:00Z",
  "updatedAt": "2026-03-22T23:00:00Z"
}
```

### PUT /customers/{id} — alteração (RF0022)

**Request** (sem senha)

```json
{
  "fullName": "Maria Santos",
  "email": "maria@email.com",
  "cpf": "52998224725",
  "phone": "31988887777",
  "birthDate": "1990-05-20",
  "active": true
}
```

### PATCH /customers/{id}/inactive — inativar (RF0023)

Sem corpo. **Response 200** com o cliente e `active: false`.

### PATCH /customers/{id}/active — reativar

Sem corpo. **Response 200** com `active: true`.

### PATCH /customers/{id}/password — só senha (RF0028)

**Request**

```json
{
  "newPassword": "novaSenhaSegura9"
}
```

**Response 204** sem corpo.

### GET /customers — filtros (RF0024)

Query opcionais (combinados com AND): `fullName`, `email`, `cpf`, `phone`, `birthDate` (ISO date), `active`.

Exemplo: `GET /customers?fullName=Maria&active=true`

**Response 200**

```json
[
  {
    "id": "...",
    "fullName": "Maria Silva",
    "email": "maria@email.com",
    "cpf": "52998224725",
    "phone": "31999998888",
    "birthDate": "1990-05-20",
    "active": true,
    "createdAt": "...",
    "updatedAt": "..."
  }
]
```

### GET /customers/{id}

**Response 200** — mesmo formato do item acima.

---

## Endereços (RF0026)

### POST /customers/{id}/addresses

```json
{
  "nickname": "Casa",
  "street": "Av. Afonso Pena",
  "number": "1000",
  "complement": "ap 501",
  "neighborhood": "Centro",
  "city": "Belo Horizonte",
  "state": "MG",
  "zipCode": "30130000",
  "type": "DELIVERY"
}
```

`type`: `DELIVERY` ou `BILLING`.

### GET /customers/{id}/addresses

Lista apenas endereços **ativos**.

### PUT /customers/{id}/addresses/{addressId}

Mesmo formato do POST, incluindo `active` (boolean).

### PATCH /customers/{id}/addresses/{addressId}/inactive

Inativação lógica. **Response 200** com o endereço atualizado.

---

## Cartões (RF0027)

### POST /customers/{id}/cards

```json
{
  "cardholderName": "Maria Silva",
  "cardNumber": "4111111111111111",
  "brand": "VISA",
  "expirationMonth": 12,
  "expirationYear": 2030,
  "preferred": true
}
```

### GET /customers/{id}/cards

**Response** — `cardNumber` vem como `cardNumberMasked` (ex.: `****1111`).

```json
[
  {
    "id": "...",
    "cardholderName": "Maria Silva",
    "cardNumberMasked": "****1111",
    "brand": "VISA",
    "expirationMonth": 12,
    "expirationYear": 2030,
    "preferred": true,
    "active": true
  }
]
```

### PUT /customers/{id}/cards/{cardId}

Inclui `preferred` e `active`.

### PATCH /customers/{id}/cards/{cardId}/preferred

Sem corpo; define este cartão como único preferencial.

### PATCH /customers/{id}/cards/{cardId}/inactive

Inativa o cartão e reatribui preferencial se necessário.

---

## Transações (RF0025)

### GET /customers/{id}/transactions

**Response 200**

```json
[
  {
    "id": "...",
    "description": "Compra livro",
    "amount": 59.90,
    "transactionDate": "2026-03-22T15:00:00Z",
    "type": "PURCHASE"
  }
]
```

`type`: `PURCHASE`, `REFUND`, `PAYMENT`, `ADJUSTMENT`.

---

## Erros

- **404** — `ProblemDetail` com mensagem (recurso não encontrado).
- **409** — e-mail ou CPF duplicado; propriedade `field`: `email` ou `cpf`.
- **400** — validação (`errors` por campo) ou regra de negócio (ex.: cartão inativo como preferencial).
