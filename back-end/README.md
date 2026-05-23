# Regras de negócio e fluxos — Backend E-commerce

Documentação do **backend** (API REST): o que o sistema faz, quais requisitos (RF/RNF) ele cobre em código e como os fluxos se encaixam. Serve para testes (Postman), integração com o front e estudo do projeto.

---

## Documento de requisitos (DRS)

O arquivo **DRS_LES_1_2026** (Word/PDF) **não está versionado neste repositório**. A lista abaixo resume o que está **implementado** aqui, usando os mesmos códigos **RF** / **RNF** que aparecem em controllers, testes e exemplos de API. Para a redação oficial, critérios de aceite e detalhes normativos, consulte o DRS que acompanha a disciplina ou o cliente.

---

## O sistema, em linguagem simples

É uma **loja online de livros**. O cliente se cadastra, pode guardar endereços e cartões, monta um **carrinho**, passa pelo **checkout** (frete, endereço de entrega, pagamento) e **finaliza** a compra — nasce um **pedido**. O **administrador** (com uma **chave** secreta na requisição) atualiza o pedido (despacho, entrega), trata **trocas**, consulta **estoque** e **relatórios de vendas**. Há também **cadastro e consulta de livros**, **auditoria** de alterações e integração com **IA** (recomendações e chat).

Quem é quem na API:

```
  +------------------+         +---------------------------+
  | Cliente final    |         | Administrador             |
  | (sem chave extra)|         | header X-Admin-Key        |
  +--------+---------+         +-------------+-------------+
           |                                 |
           v                                 v
  /customers/...          /admin/... , /inventory/... ,
  /books (consulta/       /analytics/... , /audit-logs/...
   compra pública)        (conforme endpoint)
```

---

## Catálogo de requisitos funcionais (RF)

Cada item é uma capacidade do sistema; entre parênteses, ideia do que a API faz.

### Livros (catálogo de produtos)

| Código | O que significa (simples) |
|--------|---------------------------|
| **RF0011** | Incluir um livro novo (título, ISBN, preço, estoque, etc.). |
| **RF0012** | Desativar um livro (não apaga o registro; deixa inativo). |
| **RF0013** | Rodar rotina que **desativa sozinha** livros **sem estoque** cuja “venda máxima” (`maxSaleValue`) seja nula ou **abaixo** de um valor mínimo informado. |
| **RF0014** | Alterar dados de um livro já cadastrado. |
| **RF0015** | Pesquisar livros com filtros (título, autor, categoria, ISBN), com paginação. |
| **RF0016** | Reativar um livro inativo. |

### Clientes (cadastro e perfil)

| Código | O que significa (simples) |
|--------|---------------------------|
| **RF0021** | Cadastrar cliente (e-mail, CPF, senha com regras, confirmação de senha). |
| **RF0022** | Atualizar dados gerais do cliente. |
| **RF0023** | Inativar cliente. |
| **RF0024** | Listar / filtrar clientes (nome, e-mail, CPF, etc., paginado). |
| **RF0025** | Ver **extrato de transações**: após cada compra finalizada, entra uma linha **PURCHASE** com o total do pedido; outros tipos (`REFUND`, etc.) ficam para evoluções futuras. |
| **RF0026** | Cadastrar e manter **endereços** (entrega ou cobrança: tipos `DELIVERY` / `BILLING`). |
| **RF0027** | Cadastrar e manter **cartões** de crédito (preferencial, inativar, etc.). |
| **RF0028** | Trocar **somente a senha** do cliente. |

### Vendas eletrônicas (carrinho, checkout, pedido)

| Código | O que significa (simples) |
|--------|---------------------------|
| **RF0031** | Colocar livros no **carrinho** (adicionar / remover / ver). |
| **RF0032** | Mudar **quantidade** de um item no carrinho. |
| **RF0033** | Manter **um carrinho aberto** por cliente e usá-lo no fluxo até fechar a compra. |
| **RF0034** | **Calcular frete** (fórmula interna: valor fixo + percentual do subtotal + fator do CEP — não usa API dos Correios). |
| **RF0035** | No checkout, **escolher endereço de entrega** (já cadastrado ou novo só para o pedido). |
| **RF0036** | Informar **como vai pagar**: cartão salvo, cartão novo, cupom de troca ou cupom promocional (com validações). |
| **RF0037** | **Finalizar compra**: confere totais, cria **pedido**, registra pagamentos, dá baixa no estoque, fecha o carrinho e grava a **transação PURCHASE** do cliente. |
| **RF0038** | Admin: marcar pedido como **despachado** (sai para transporte). |
| **RF0039** | Admin: marcar pedido como **entregue**. |

### Trocas de produto

| Código | O que significa (simples) |
|--------|---------------------------|
| **RF0040** | Cliente pede **troca** de um item de um pedido **já entregue**. |
| **RF0041** | Admin **autoriza** a troca. |
| **RF0042** | Admin **lista** pedidos de troca (por status do pedido). |
| **RF0043 / RF0044** | Admin **recebe** o produto devolvido, pode devolver ao estoque e **gera cupom** de troca para o cliente. |

### Estoque e análise (admin)

| Código | O que significa (simples) |
|--------|---------------------------|
| **RF0051** | Lançar **entrada manual** de estoque (quantidade, custo, motivo). |
| **RF0053** | **Baixa de estoque** ligada à venda (executada no fluxo de pedido). |
| **RF0054** | **Repor estoque** quando a troca devolve produto à loja. |
| **RF0055** | **Relatórios de vendas** (resumo por período, por livro, por categoria, gráfico de linha) — todos com chave de admin. Detalhes dos endpoints na **seção 11**. |

### Outros módulos expostos na API

- **Auditoria:** listar logs de alterações (`/audit-logs`, admin).
- **IA:** recomendações por cliente e chat (`/ai/...`).
- **Saúde:** `GET /health`.

---

## Requisitos não funcionais (RNF) — trechos refletidos no backend

| Código | Ideia geral |
|--------|-------------|
| **RNF0011** | Listagens com **paginação** e limite de tamanho de página. |
| **RNF0031–RNF0033** | **Senha forte** (validação), confirmação igual à senha, armazenamento com **BCrypt**. |
| **RNF0034** | Endereços tratados **à parte** do cadastro geral (não precisa reenviar o cliente inteiro). |
| **RNF0042** | Itens do carrinho podem **expirar**; finalização bloqueia se houver itens expirados até o cliente ajustar. |
| **RNF0043** | Área de **analytics** protegida pela chave de admin. |

---

## 1. Visão geral dos blocos

```
                    +------------------+
                    |     Cliente      |
                    +--------+---------+
                         |
     +-----------------------+-----------------------+
     |                       |                       |
     v                       v                       v
+-----------+        +---------------+        +-------------+
| Customers |        | Sales (cart,  |        | Books /     |
| addresses |        | checkout,     |        | Inventory   |
| cards     |        | orders)       |        | (admin)     |
+-----------+        +---------------+        +-------------+
                             |
                             v
                    +----------------+
                    | Admin (chave)  |
                    | pedidos,       |
                    | estoque,       |
                    | analytics      |
                    +----------------+
```

---

## 2. Carrinho (`/customers/{customerId}/cart`)

- Um carrinho **aberto** por cliente; itens referenciam livro + quantidade.
- Itens podem **expirar** (config `cart.item.expiration-minutes`); se houver itens expirados “bloqueantes”, a **finalização** é recusada até o cliente ajustar o carrinho.

```
  [Cliente]
      |
      |  POST   /cart/items          (adicionar)
      |  PUT    /cart/items/{itemId} (quantidade)
      |  DELETE /cart/items/{itemId} (remover)
      |  GET    /cart                 (ver carrinho)
      v
  [Cart OPEN] ----checkout----> [Cart CHECKED_OUT]
```

---

## 3. Checkout — passos obrigatórios e ordem lógica

Endpoints sob: `/customers/{customerId}/checkout`

A **finalização** (`POST .../finalize`) só prossegue se **todas** as condições abaixo forem atendidas (validação em `CheckoutService`):

| Passo | Endpoint | O que faz / regra |
|-------|----------|-------------------|
| 1 | `POST .../freight` | Calcula frete; carrinho não pode estar vazio. Grava `freightAmount` no carrinho. |
| 2 | `POST .../address` | Define endereço de entrega: **ou** `addressId` **ou** `newAddress` (nunca os dois). Se `newAddress`: obrigatório `saveToProfile` (true = salva no perfil; false = só uso no pedido). |
| 3 | `POST .../payment` | Define linhas de pagamento; soma das linhas deve **bater** com total (itens + frete) na finalização. |
| 4 | `POST .../finalize` | Cria pedido, aplica baixa de estoque, fecha carrinho. |

```
                    CHECKOUT (mesmo customerId)
  +------------------------------------------------------------------+
  |                                                                  |
  |   +--------+     +----------+     +-----------+     +----------+ |
  |   | Itens  | --> |  Frete   | --> | Endereço  | --> | Pagamento| |
  |   | carrinho    | (POST     |     | entrega   |     | (linhas) | |
  |   | nao vazio   |  freight)  |     | (address) |     |          | |
  |   +--------+     +----------+     +-----------+     +-----+----+ |
  |                                                          |       |
  |                                                          v       |
  |                                                   +-------------+|
  |                                                   |  FINALIZE   |
  |                                                   |  (pedido +  |
  |                                                   |   estoque)  |
  |                                                   +-------------+|
  +------------------------------------------------------------------+
```

**Na finalização, o sistema verifica (entre outras):**

- Carrinho com itens; sem itens expirados bloqueantes.
- Endereço de entrega definido.
- Frete calculado (`freightAmount` não nulo).
- Pelo menos uma linha de pagamento.
- **Soma dos pagamentos = subtotal itens + frete** (com escala decimal).

### 3.1 Cálculo de frete (implementação)

**Não há integração com API externa** (Correios, transportadoras etc.). O valor é calculado **no backend** pela classe `FreightService`, com base no carrinho atual e no endereço informado em `POST .../checkout/freight` (`addressId` deve ser de um endereço **do mesmo cliente**).

```
  [Carrinho: subtotal itens]
           +
  [Endereço: CEP só dígitos]
           |
           v
  +---------------------------+
  | FreightService.calculate  |
  | (regra local, em memória) |
  +---------------------------+
```

**Fórmula** (valores arredondados com 2 casas decimais):

| Componente | Regra |
|------------|--------|
| Base fixa | R$ **12,90** |
| Percentual | **2%** do subtotal dos itens do carrinho (`cart.totalAmount`) |
| Fator CEP | Soma dos **dígitos** do CEP (após remover não numéricos) × **R$ 0,15** |

```
  frete = 12,90 + (subtotal × 0,02) + (soma_dígitos_cep × 0,15)
```

Se o CEP, após limpar, ficar vazio, a soma dos dígitos é tratada como **0** para a parte do CEP.

---

## 4. Formas de pagamento (linhas)

Tipos (`PaymentType`): `CREDIT_CARD`, `EXCHANGE_COUPON`, `PROMOTIONAL_COUPON`.

```
  Pagamento
  +-- CREDIT_CARD --------> creditCardId OU newCreditCard no body
  |                        (se newCreditCard: obrigatório saveNewCardToProfile)
  |
  +-- EXCHANGE_COUPON ----> couponCode + valor = valor do cupom
  |
  +-- PROMOTIONAL_COUPON -> idem, com regras de titularidade do cupom
```

Na finalização, cupons usados são marcados como resgatados (`markRedeemed`).

---

## 5. Pedido — ciclo de status (cliente e admin)

Status do pedido (`OrderStatus`):

```
  EM_PROCESSAMENTO
        |
        |  (admin: dispatch — pedido EM_PROCESSAMENTO ou APROVADO)
        v
  EM_TRANSITO
        |
        |  (admin: deliver)
        v
    ENTREGUE
        |
        |  (cliente: solicita troca em item)
        v
    EM_TROCA  ------------------+
        |                        |
        |  (admin: authorize)    |
        v                        |
  TROCA_AUTORIZADA               |
        |                        |
        |  (admin: receive +     |
        |   cupom de troca)      |
        +------------------------+
```

- **Cliente** lista/detalha pedidos: `GET /customers/{customerId}/orders`, `GET .../orders/{orderId}`.
- **Admin** (header `X-Admin-Key`):
  - `PATCH /admin/orders/{orderId}/dispatch` — só se status `EM_PROCESSAMENTO` ou `APROVADO`.
  - `PATCH /admin/orders/{orderId}/deliver` — só se `EM_TRANSITO`.

---

## 6. Troca de produto — fluxo, endpoints e passos

Complementa o diagrama de status do pedido na **seção 5**. A troca é sempre **por um item** (`orderItemId`) de um pedido **já entregue** (`ENTREGUE`). O cliente abre a solicitação; o admin autoriza, depois confirma o recebimento físico e o sistema gera o **cupom de troca** (e pode repor estoque).

### 6.1 Pré-requisitos e regras

- O pedido precisa estar em **`ENTREGUE`** (após admin marcar entrega).
- Cada solicitação referencia **um** `orderItemId` do pedido.
- Não é permitido outra troca **em aberto** para o mesmo pedido: não pode existir solicitação com status `REQUESTED` ou `AUTHORIZED` para aquele pedido.
- O item não pode já ter sido marcado com troca solicitada nem já possuir registro de solicitação de troca.

### 6.2 Dois “tipos” de status

| Onde | Valores relevantes | Quando muda |
|------|-------------------|-------------|
| **Pedido** (`OrderStatus`) | `ENTREGUE` → `EM_TROCA` → `TROCA_AUTORIZADA` → `ENTREGUE` | Ver passos abaixo. |
| **Solicitação de troca** (`ExchangeStatus`) | `REQUESTED` → `AUTHORIZED` → `RECEIVED` | Criada na solicitação do cliente; `authorize` e `receive` do admin. |

### 6.3 Passo a passo (ordem das chamadas)

**Passo 1 — Cliente solicita a troca**

```http
POST /customers/{customerId}/orders/{orderId}/exchange-requests
Content-Type: application/json

{ "orderItemId": "<uuid do item do pedido>" }
```

- **Resposta:** `201 Created` com corpo `ExchangeRequestResponse` (inclui `id` da solicitação — use nos passos admin).
- **Efeito:** status da solicitação `REQUESTED`; pedido vai para **`EM_TROCA`**; o item fica marcado como com troca solicitada.

**Passo 2 — Admin lista solicitações (filtra pelo status do pedido)**

```http
GET /admin/exchange-requests?status=EM_TROCA
X-Admin-Key: <sua chave>
```

- Use `status=EM_TROCA` para ver pendentes de autorização.
- Depois do passo 3, para achar as que aguardam recebimento: `GET /admin/exchange-requests?status=TROCA_AUTORIZADA`.

**Passo 3 — Admin autoriza a troca**

```http
PATCH /admin/exchange-requests/{exchangeRequestId}/authorize
X-Admin-Key: <sua chave>
```

- **Condições:** solicitação em `REQUESTED` e pedido em `EM_TROCA`.
- **Efeito:** solicitação `AUTHORIZED`; pedido **`TROCA_AUTORIZADA`** (cliente pode enviar o produto de volta conforme processo da loja).

**Passo 4 — Admin confirma recebimento e gera o cupom**

```http
PATCH /admin/exchange-requests/{exchangeRequestId}/receive
X-Admin-Key: <sua chave>
Content-Type: application/json

{ "returnToStock": true }
```

ou `"returnToStock": false` se o item **não** deve voltar ao estoque neste momento.

- **Condições:** solicitação em `AUTHORIZED` e pedido em `TROCA_AUTORIZADA`.
- **Efeito:**
  - Solicitação **`RECEIVED`**.
  - Criado **cupom de troca** para o cliente, no valor do **total da linha** do item (`orderItem`), código no formato `TROCA-XXXXXXXX` (retornado em `generatedCouponCode` na resposta).
  - Se `returnToStock` for **`true`**, executa a **reentrada no estoque** ligada a essa troca.
  - Pedido volta para **`ENTREGUE`** (ciclo de troca encerrado no pedido).

**Opcional — reprocessar só a reentrada de estoque (admin)**

Se no `receive` foi usado `returnToStock: false` e depois for preciso repor estoque, existe endpoint **idempotente**:

```http
POST /inventory/reentries/exchange
X-Admin-Key: <sua chave>
Content-Type: application/json

{ "exchangeRequestId": "<uuid>" }
```

### 6.4 Uso do cupom pelo cliente

No checkout, tipo de pagamento **`EXCHANGE_COUPON`** com o código retornado — mesma ideia descrita na **seção 4** para pagamentos com cupom.

### 6.5 Diagrama resumido (quem chama o quê)

```
  Cliente                              Admin (X-Admin-Key)
     |                                        |
     |  POST .../exchange-requests            |
     v                                        |
  pedido EM_TROCA                             |
     |                                        |
     |                          GET ?status=EM_TROCA
     |                          PATCH .../authorize
     v                                        v
                          pedido TROCA_AUTORIZADA
                                        |
                          GET ?status=TROCA_AUTORIZADA
                          PATCH .../receive + returnToStock
                                        v
                          cupom gerado; pedido ENTREGUE
```

---

## 7. “Transações do cliente” (RF0025)

### 7.1 O que é uma “transação” aqui

Não é transação de banco (commit/rollback). É uma **linha de extrato** do cliente: valor, data, texto (`description`) e `TransactionType`, persistida em **`customer_transactions`**. O detalhe da venda continua em **`GET /orders`** (itens, frete, linhas de pagamento).

### 7.2 Consulta

```http
GET /customers/{customerId}/transactions
```

Lista as transações do cliente, da mais recente para a mais antiga.

### 7.3 Quando uma linha é criada (implementação atual)

Após **`POST .../checkout/finalize`** com sucesso, o serviço **`CustomerTransactionRecorder`** grava **uma** linha:

- `type`: **`PURCHASE`**
- `amount`: total do pedido (**itens + frete**, igual `SalesOrder.totalAmount`)
- `description`: texto do tipo `Compra — pedido <uuid>`
- `transactionDate`: alinhado ao `createdAt` do pedido quando disponível

Não são criadas linhas **`PAYMENT`** por cartão/cupom na mesma compra (evita dupla contagem no extrato). Os meios de pagamento seguem apenas em `payments` do pedido.

```
  POST .../checkout/finalize
        |
        +------------------------------+------------------------------+
        |                              |                              |
        v                              v                              v
  +-------------+                +------------------+        +-------------------+
  | sales_orders|                | payments         |        | customer_         |
  | + items     |                | (por linha       |        | transactions      |
  | + frete     |                |  checkout)       |        | 1x PURCHASE       |
  +-------------+                +------------------+        | (total pedido)    |
        |                              |                    +-------------------+
        |  GET /orders                 |                            |
        v                              |                            |  GET /transactions
  [detalhe comercial]                  |                            v
                                       |                    [lista extrato]
                                       |
```

### 7.4 Quando a lista ainda pode estar vazia

- Cliente **sem nenhuma compra finalizada** ainda.
- Transações inseridas só em **testes manuais** antigos: o fluxo real passa a alimentar o extrato **somente** no `finalize`.

### 7.5 Status do pedido, troca e outros tipos

- **`dispatch` / `deliver` / troca** não adicionam hoje novas linhas em `customer_transactions` (só o `PURCHASE` na finalização).
- Tipos previstos no modelo para **evolução** (`TransactionType`): `PURCHASE`, `REFUND`, `PAYMENT`, `ADJUSTMENT` (ex.: estorno ou ajuste manual no futuro).

---

## 8. Endereços — tipo (`AddressType`)

Valores aceitos no JSON: **`DELIVERY`** ou **`BILLING`** (não use rótulos em português como `RESIDENCIAL`).

---

## 9. Inativação automática de livros (RF0013)

`POST /books/inactivate-automatic` com `{ "minimumSalesValue": <decimal> }`.

Regra implementada:

- Considera livros com **`stockQuantity == 0`**.
- Para cada um: se **`maxSaleValue` é null OU `maxSaleValue < minimumSalesValue`**, define `active = false`.
- Se **`maxSaleValue >= minimumSalesValue`**, o livro **não** é inativado por essa rotina.

---

## 10. Admin — autenticação por chave

Rotas administrativas validam o header:

```http
X-Admin-Key: <valor de app.admin.key no application.yml>
```

Sem chave correta: resposta de acesso negado.

---

## 10.1. Seed de demonstração (`app.demo.seed`)

Na subida da aplicação, um `ApplicationRunner` pode criar **livros de exemplo**, **vários clientes**, **pedidos finalizados com datas retroativas** (para o analytics não vir zerado) e **uma troca completa** (solicitar → autorizar → receber com devolução ao estoque). Isto é controlado por:

- `app.demo.seed.enabled` — no `application.yml` principal costuma estar `true` para ambiente local; nos testes (`src/test/resources/application.yml`) fica `false` para não interferir nos `SpringBootTest`.
- `app.demo.seed.days-back` — dispersão dos pedidos sintéticos nos últimos N dias (padrão 21).
- `app.demo.seed.marker-isbn` — ISBN-13 **real** do primeiro livro do lote (padrão `9780743273565`, *The Great Gatsby*, Scribner US); **se esse ISBN já existir**, o seed inteiro é ignorado (idempotência em banco persistente). Os demais títulos do seed usam ISBNs de edições **US/UK** com boa cobertura na **Open Library** (capas JPEG), definidos em `DemoDataSeederService`.

**Capas no front (Open Library) e ISBN antigo (`978DEMO…`):** o front monta a URL da capa só com o campo `isbn` devolvido pela API (`GET /books`, detalhe do livro). Se no navegador a imagem apontar para `978DEMO0000005` (ou outro `978DEMO…`), esse valor ainda está **gravado na linha do livro** — não é cache do front. Alterar `DemoDataSeederService` ou o `marker-isbn` **não atualiza** registros já inseridos; o seed não faz `UPDATE` em livros existentes.

**Como alinhar os dados após mudar ISBNs do seed:**

1. Confirme na API: `GET /books` (filtre pelo título ou ISBN esperado). O JSON deve mostrar o ISBN novo; deve coincidir com a URL da capa no front.
2. **H2 em memória** (`jdbc:h2:mem:booksdb` no `application.yml` padrão): encerrar o processo Java e subir de novo começa com base vazia; na primeira subida o seed roda se o `marker-isbn` ainda não existir.
3. Se o seed **não** rodar porque o marcador já existe, mas você ainda vê livros com `978DEMO…`, são restos de uma execução antiga (ou um segundo lote). Opções: apagar essas linhas no **H2 Console** (`/h2-console`, JDBC URL igual à do `application.yml`) ou remover temporariamente o livro cujo ISBN é o `marker-isbn` e reiniciar para o seed recriar o lote inteiro (atenção: impacta pedidos/demo ligados a esses IDs).
4. Com **H2 em arquivo** ou outro banco persistente, é preciso limpar dados ou migrar ISBNs manualmente do mesmo modo.

O administrador continua sendo apenas o header **`X-Admin-Key`** com o valor de `app.admin.key` (o front de analytics usa a mesma chave via `VITE_ADMIN_KEY`).

---

## 11. Analytics — análise de vendas (admin, RF0055)

Todos os endpoints abaixo exigem o header **`X-Admin-Key`** (mesmo valor de `app.admin.key` no `application.yml`, padrão `8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12`).  
Parâmetros obrigatórios em query: **`startDate`** e **`endDate`** no formato **ISO** (`YYYY-MM-DD`).

### Como os números são calculados

Os relatórios usam os **itens de pedido** (`order_items`) cujo pedido (`sales_orders`) tenha **`createdAt`** dentro do intervalo **[startDate 00:00 UTC, endDate+1 00:00 UTC)**. Ou seja: conta **data de criação do pedido** (quando a compra foi finalizada), em **UTC**.

Por isso é normal ver:

```json
{ "totalRevenue": 0, "totalItemsSold": 0, "orderCount": 0 }
```

quando **não existe pedido** nesse intervalo — por exemplo: período errado, banco H2 reiniciado sem vendas, ou pedidos criados **fora** das datas que você passou.

**Checklist se vier tudo zero e você esperava números:**

1. Confirme **`X-Admin-Key`** (403 se errado).
2. Use **`startDate` / `endDate`** que **incluam o dia** em que você rodou o `checkout/finalize` (no fuso local lembre que o servidor grava instante em UTC).
3. Garanta que exista pelo menos um pedido concluído nesse intervalo.

### Endpoints

| Método e caminho | O que retorna (ideia simples) |
|------------------|-------------------------------|
| `GET /analytics/sales-history` | **Resumo:** `totalRevenue` (soma dos `totalPrice` dos itens), `totalItemsSold` (soma das quantidades), `orderCount` (pedidos distintos). |
| `GET /analytics/sales-history/books` | Lista de livros com receita e quantidade vendida no período. |
| `GET /analytics/sales-history/categories` | Mesmo tipo de agregação, agrupado por **categoria** do livro. |
| `GET /analytics/sales-history/line-chart` | **Gráfico de linha:** `labels` = um rótulo por dia no intervalo (`YYYY-MM-DD`), `values` = receita daquele dia (0 se não houve venda). |

Exemplo de chamada (resumo):

```http
GET /analytics/sales-history?startDate=2026-03-25&endDate=2026-03-25
X-Admin-Key: 8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12
```

*(Os testes de integração `AnalyticsControllerIntegrationTest` verificam status 200 e estrutura; com base vazia o resumo continua válido com zeros.)*

---

## 12. Onde ir mais a fundo

- Exemplos de API: `CUSTOMER_API_EXAMPLES.md`, `SALES_API_EXAMPLES.md`, `INVENTORY_API_EXAMPLES.md`
- Coleção Postman: `postman/matheus-gn-backend.postman_collection.json`
- OpenAPI (com app rodando): `/swagger-ui.html` e `/v3/api-docs`
