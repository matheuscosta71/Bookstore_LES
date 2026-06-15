# 🎯 Plano de Estudos Intensivo — Apresentação Técnica Spring Boot

> **Tempo total estimado**: 4–5 horas de estudo focado
> **Prioridade**: Blocos 1–6 são os mais cobrados. Blocos 7–13 são diferenciais. Blocos 14–15 são treino final.

---

## BLOCO 1 — Visão Geral de uma Aplicação Java/Spring Boot
⏱️ **Tempo**: 20 minutos

### O que você precisa entender

**API REST** é uma interface que permite que sistemas se comuniquem via HTTP. Cada URL representa um recurso (ex: `/books`, `/customers`), e os verbos HTTP definem a ação:
- `GET` = ler
- `POST` = criar
- `PUT/PATCH` = atualizar
- `DELETE` = remover

**Spring Boot** é um framework Java que simplifica a criação de APIs REST. Ele auto-configura quase tudo: servidor web embarcado (Tomcat), injeção de dependências, acesso a banco, etc.

**Arquitetura em camadas** significa que o código é dividido em responsabilidades:

```
HTTP Request → Controller → Service → Repository → Banco de Dados
HTTP Response ← Controller ← Service ← Repository ← Banco de Dados
```

- **Controller**: recebe a requisição HTTP, delega ao service, devolve a resposta
- **Service**: contém a lógica de negócio (validações, cálculos, regras)
- **Repository**: acessa o banco de dados

### O que falar na apresentação

> "Nosso sistema é uma **API REST** desenvolvida em **Java com Spring Boot 3.3**. A arquitetura segue o padrão **N-Tier (camadas)**: Controllers recebem requisições HTTP, Services processam a lógica de negócio, e Repositories persistem no banco de dados H2 via JPA/Hibernate. Essa separação facilita testes, manutenção e evolução do sistema."

### Perguntas que podem ser feitas

**P: O que é uma API REST?**
> R: "É uma interface de comunicação baseada em HTTP onde cada recurso tem uma URL e as operações são definidas pelos verbos GET, POST, PUT e DELETE. A comunicação usa JSON como formato de dados."

**P: Por que Spring Boot?**
> R: "Spring Boot simplifica a configuração do Spring Framework com auto-configuração. Não precisamos configurar manualmente o servidor, o gerenciamento de dependências ou o pool de conexões do banco — tudo é resolvido por convenção."

**P: O que é arquitetura em camadas?**
> R: "É um padrão onde separamos responsabilidades: Controller cuida do HTTP, Service cuida da regra de negócio, Repository cuida do banco. Nenhuma camada faz o trabalho da outra. Isso permite testar cada camada isoladamente."

### ✅ Checklist de revisão

- [ ] Abra [EcommerceApplication.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/EcommerceApplication.java) — veja a anotação `@SpringBootApplication` (ponto de entrada)
- [ ] Abra [application.yml](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/resources/application.yml) — veja configurações do banco H2 e da IA
- [ ] Entenda: o Spring cria todas as classes automaticamente (injeção de dependências via `@RequiredArgsConstructor`)

---

## BLOCO 2 — Estrutura de Pacotes
⏱️ **Tempo**: 20 minutos

### O que você precisa entender

O projeto usa **organização por domínio de negócio** (feature-based). Cada módulo é um pacote com seus subpacotes:

```
com.matheusgn.ecommerce
├── book/          → Tudo sobre livros
├── customer/      → Tudo sobre clientes
├── sales/         → Carrinho, checkout, pedidos, trocas, cupons
├── inventory/     → Estoque e precificação
├── ai/            → Chatbot e recomendações com IA
├── analytics/     → Dashboard de vendas (gráficos)
├── audit/         → Registro de alterações (auditoria)
├── auth/          → Autenticação (login)
├── config/        → Configurações globais (segurança, paginação)
├── exception/     → Tratamento global de erros
├── domain/        → Entidades auxiliares (Author, Category, Publisher)
├── feedback/      → Avaliações de livros
├── demo/          → Dados iniciais para demonstração
├── health/        → Health check
└── common/        → Utilitários compartilhados
```

Dentro de cada pacote, os **subpacotes** seguem o padrão de camadas:

| Subpacote | Responsabilidade |
|---|---|
| `controller/` | Recebe requisições HTTP |
| `service/` | Regras de negócio |
| `repository/` | Acesso ao banco de dados |
| `entity/` | Classes que representam tabelas do banco |
| `dto/` | Objetos de entrada/saída (request/response) |
| `exception/` | Exceções específicas do módulo |
| `client/` | Chamadas a APIs externas (ex: OpenAI) |

### O que falar na apresentação

> "Organizamos o projeto **por domínio de negócio**: tudo sobre livros fica no pacote `book`, tudo sobre vendas no pacote `sales`, e assim por diante. Dentro de cada pacote, separamos por camada: controller, service, repository, entity e dto. Essa organização permite que dois desenvolvedores trabalhem em módulos diferentes sem conflito, e facilita uma futura extração para microsserviços."

### Perguntas que podem ser feitas

**P: Por que organizar por domínio e não por camada?**
> R: "Se organizássemos por camada, teríamos todos os controllers misturados (BookController, CustomerController, CartController juntos). Ao organizar por domínio, tudo que pertence a 'livros' fica junto. Isso aumenta a coesão e facilita a navegação."

**P: Quantos módulos o sistema tem?**
> R: "São 15 módulos: book, customer, sales, inventory, ai, analytics, audit, auth, config, domain, exception, feedback, demo, health e common."

### ✅ Checklist de revisão

- [ ] Navegue pela árvore `back-end/src/main/java/com/matheusgn/ecommerce/` e identifique os pacotes
- [ ] Abra o pacote `sales/` e note os subpacotes: controller, service, repository, entity, dto
- [ ] Perceba que o pacote `sales/` tem **10 services** — é o coração do sistema

---

## BLOCO 3 — Fluxo de Requisição
⏱️ **Tempo**: 30 minutos (BLOCO CRÍTICO)

### O que você precisa entender

Toda requisição segue exatamente este caminho:

```
Navegador/Postman → Controller → (valida DTO) → Service → (aplica regras) → Repository → Banco
                                                                                    ↓
Navegador/Postman ← Controller ← (converte para DTO de resposta) ← Service ← Repository ← Banco
```

### Exemplo concreto do seu projeto: "Adicionar item ao carrinho"

**Passo 1** — O front-end faz:
```
POST /customers/{customerId}/cart/items
Body: { "bookId": "uuid-do-livro", "quantity": 2 }
```

**Passo 2** — O [CartController](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/sales/controller/CartController.java) recebe:
```java
@PostMapping("/items")
public CartResponse addItem(@PathVariable UUID customerId,
                            @Valid @RequestBody CartUpsertItemRequest request) {
    return cartService.addItem(customerId, request);  // delega ao service
}
```

**Passo 3** — O [CartService](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/sales/service/CartService.java) processa:
```java
public CartResponse addItem(UUID customerId, CartUpsertItemRequest request) {
    // 1. Busca o cliente
    Customer customer = customerRepository.findById(customerId)...
    // 2. Busca o livro
    Book book = bookRepository.findById(request.getBookId())...
    // 3. Valida estoque
    inventoryService.assertAvailableStock(book.getId(), request.getQuantity());
    // 4. Reserva estoque
    inventoryService.adjustReservationForCart(book.getId(), +request.getQuantity());
    // 5. Cria o item no carrinho
    cart.getItems().add(new CartItem(...));
    // 6. Salva no banco
    cartRepository.save(cart);
    // 7. Retorna DTO de resposta
    return toCartResponse(cart);
}
```

**Passo 4** — O Repository salva no banco (o Spring faz automaticamente via JPA)

**Passo 5** — O Controller retorna o DTO como JSON (HTTP 201 Created)

### O que falar na apresentação

> "Vou demonstrar o fluxo com um exemplo real: adicionar um item ao carrinho. O front-end faz um POST para `/customers/{id}/cart/items`. O **CartController** recebe o request, valida o body com Bean Validation, e chama o `cartService.addItem()`. O **CartService** busca o cliente e o livro no banco, valida se tem estoque, reserva as unidades, cria o item no carrinho e salva via repository. Por fim, converte a entidade Cart para um DTO de resposta e retorna ao controller, que devolve como JSON com status 201."

### Perguntas que podem ser feitas

**P: O que acontece se o livro não existir?**
> R: "O `bookRepository.findById()` retorna `Optional.empty()`, e usamos `.orElseThrow()` que lança uma `ResourceNotFoundException`. O `GlobalExceptionHandler` captura essa exceção e retorna HTTP 404 com uma mensagem descritiva no formato RFC 7807."

**P: Quem valida os dados de entrada?**
> R: "A anotação `@Valid` no controller dispara o Bean Validation. O DTO tem anotações como `@NotNull`, `@Min`, `@NotBlank` que validam automaticamente. Se falhar, o Spring retorna HTTP 400 com os campos inválidos."

### ✅ Checklist de revisão

- [ ] Abra [CartController.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/sales/controller/CartController.java) — veja `addItem()` na linha 36
- [ ] Abra [CartService.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/sales/service/CartService.java) — veja `addItem()` na linha 63
- [ ] Trace o caminho: Controller → Service → InventoryService → InventoryBalanceService → Repository → Banco

---

## BLOCO 4 — CRUD
⏱️ **Tempo**: 20 minutos

### O que você precisa entender

CRUD são as 4 operações básicas sobre dados:

| Operação | Verbo HTTP | Método Repository | Exemplo |
|---|---|---|---|
| **C**reate | `POST` | `save()` | Cadastrar livro |
| **R**ead | `GET` | `findById()`, `findAll()` | Consultar livro |
| **U**pdate | `PUT` / `PATCH` | `save()` (com ID existente) | Alterar preço do livro |
| **D**elete | `DELETE` | `deleteById()` | Remover livro |

### Exemplo no seu projeto: CRUD de Livros

**Create** — [BookController.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/book/controller/BookController.java):
```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public BookResponse create(@Valid @RequestBody BookCreateRequest request) {
    return bookService.create(request);
}
```

**Read (por ID):**
```java
@GetMapping("/{id}")
public BookResponse findById(@PathVariable UUID id) {
    return bookService.findById(id);
}
```

**Read (listar com filtros):**
```java
@GetMapping
public Page<BookResponse> findAll(@RequestParam(required = false) String title, ..., Pageable pageable) {
    return bookService.findAll(title, author, category, pageable);
}
```

**Update:**
```java
@PutMapping("/{id}")
public BookResponse update(@PathVariable UUID id, @Valid @RequestBody BookUpdateRequest request) {
    return bookService.update(id, request);
}
```

### O que falar na apresentação

> "Cada recurso do sistema (livro, cliente, pedido) implementa as operações CRUD: Create com POST, Read com GET, Update com PUT e Delete com DELETE. Por exemplo, o CRUD de livros: um POST para `/books` chama o `BookService.create()` que valida os dados, gera um código automático, inicializa o estoque e persiste via `BookRepository.save()`. O GET busca com filtros dinâmicos usando o Specification Pattern do Spring Data."

### Perguntas que podem ser feitas

**P: Como funciona a paginação?**
> R: "Usamos `Pageable` do Spring Data. O front-end envia `?page=0&size=20&sort=title,asc` e o Spring monta a query com LIMIT e OFFSET automaticamente. Retornamos um `Page<BookResponse>` que inclui metadados como totalPages, totalElements e número da página atual."

**P: O que é o `save()` no Spring Data?**
> R: "O `save()` faz INSERT se a entidade não tem ID, e UPDATE se já tem. O Spring detecta automaticamente. Então usamos o mesmo método para criar e atualizar."

### ✅ Checklist de revisão

- [ ] Abra qualquer Controller e identifique: `@PostMapping` = Create, `@GetMapping` = Read, `@PutMapping` = Update, `@DeleteMapping` = Delete
- [ ] Note que cada método chama UM método do Service correspondente
- [ ] Procure o `save()` nos services — ele é usado tanto para criar quanto para atualizar

---

## BLOCO 5 — Controller
⏱️ **Tempo**: 15 minutos

### O que você precisa entender

O Controller é a **porta de entrada** da API. Ele:
1. Recebe a requisição HTTP
2. Valida os dados de entrada (`@Valid`)
3. Chama **um** método do Service
4. Retorna o DTO de resposta

**Regra de ouro: Controller NUNCA tem lógica de negócio.** Sem `if`, sem cálculos, sem acesso ao banco.

### Anotações que você precisa saber

| Anotação | Significado |
|---|---|
| `@RestController` | "Esta classe é um controller REST" (retorna JSON automaticamente) |
| `@RequestMapping("/books")` | "Todas as rotas desta classe começam com `/books`" |
| `@GetMapping` | Trata requisições `GET` |
| `@PostMapping` | Trata requisições `POST` |
| `@PutMapping` | Trata requisições `PUT` |
| `@DeleteMapping` | Trata requisições `DELETE` |
| `@PatchMapping` | Trata requisições `PATCH` (atualização parcial) |
| `@PathVariable` | Captura valor da URL: `/books/{id}` → pega o `id` |
| `@RequestBody` | Converte o JSON do corpo da requisição para um DTO Java |
| `@Valid` | Ativa validação do Bean Validation no DTO |
| `@ResponseStatus(HttpStatus.CREATED)` | Retorna HTTP 201 ao invés de 200 |

### O que falar na apresentação

> "Os Controllers são **thin** — eles não contêm regra de negócio. Por exemplo, o `CheckoutController` tem 5 endpoints: calcular frete, definir endereço, validar cupom, definir pagamento e finalizar compra. Cada endpoint é uma linha: recebe o request e delega ao `CheckoutService`. Isso segue o princípio da responsabilidade única."

### ✅ Checklist de revisão

- [ ] Abra [CheckoutController.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/sales/controller/CheckoutController.java) — note que cada método tem ~3 linhas
- [ ] Abra [AdminOrderController.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/sales/controller/AdminOrderController.java) — note o `@RequestHeader("X-Admin-Key")`

---

## BLOCO 6 — Service
⏱️ **Tempo**: 25 minutos (BLOCO CRÍTICO)

### O que você precisa entender

O Service é o **cérebro** da aplicação. Aqui ficam:
- Validações de negócio ("carrinho vazio?", "estoque suficiente?", "cupom válido?")
- Orquestração de chamadas (chama Inventory, Coupon, Audit)
- Cálculos (frete, preço de venda, troco de cupom)

**Padrão que TODOS os services seguem:**
```java
public ResultadoDTO metodo(EntradaDTO request) {
    // 1. VALIDAR
    if (condiçãoInválida) throw new IllegalArgumentException("mensagem");
    
    // 2. BUSCAR dados necessários
    Entidade entidade = repository.findById(id).orElseThrow(...);
    
    // 3. PROCESSAR regra de negócio
    entidade.setAlgo(novoValor);
    
    // 4. PERSISTIR
    repository.save(entidade);
    
    // 5. EFEITOS COLATERAIS (auditoria, notificação)
    auditLogService.logCreate("Entidade", id, response);
    
    // 6. RETORNAR DTO
    return Mapper.toResponse(entidade);
}
```

### Exemplo real: `CheckoutService.finalizePurchase()` — O service mais complexo

Este método (~130 linhas) orquestra **12 operações** para finalizar uma compra:
1. Reconcilia estoque do carrinho
2. Verifica itens expirados
3. Valida: carrinho não vazio? endereço definido? frete calculado? pagamento definido?
4. Re-valida regras de cartão/cupom
5. Emite cupom de troco (se necessário)
6. Valida endereços (entrega + cobrança)
7. Confirma estoque físico para cada item
8. Monta o `SalesOrder` com `OrderItems` e `Payments`
9. Salva o pedido no banco
10. Registra auditoria
11. Fecha o carrinho (`CHECKED_OUT`)
12. Retorna o `OrderResponse`

### O que falar na apresentação

> "A regra de negócio fica 100% nos Services. O `CheckoutService`, por exemplo, é um **Facade** que orquestra mais de 10 services auxiliares: `CartService` para o carrinho, `FreightService` para frete, `CouponService` para cupons, `InventoryService` para estoque, `AuditLogService` para auditoria. O Controller não sabe dessa complexidade — ele só chama `checkoutService.finalizePurchase()`."

### Perguntas que podem ser feitas

**P: Por que não colocar a lógica no Controller?**
> R: "Se a lógica ficasse no Controller, não conseguiríamos reutilizá-la. Por exemplo, o `DemoDataSeederService` chama `checkoutService.finalizePurchase()` diretamente, sem passar pelo Controller. Se a lógica estivesse no Controller, o seeder teria que duplicar o código."

**P: O que é `@Transactional`?**
> R: "Garante que todas as operações do banco dentro do método sejam atômicas. Se qualquer erro ocorrer no meio, todas as alterações são revertidas automaticamente. É essencial no checkout: se falhar ao salvar o pagamento, o pedido também é revertido."

### ✅ Checklist de revisão

- [ ] Abra [CheckoutService.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/sales/service/CheckoutService.java) — veja `finalizePurchase()` (L284)
- [ ] Abra [AdminOrderService.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/sales/service/AdminOrderService.java) — veja `approvePayment()` (L87)
- [ ] Note o padrão: validar → processar → salvar → efeitos colaterais → retornar

---

## BLOCO 7 — Repository e JPA
⏱️ **Tempo**: 15 minutos

### O que você precisa entender

O **Repository** é a interface entre o Java e o banco de dados. Usando **Spring Data JPA**, você NÃO escreve SQL — o Spring gera automaticamente.

```java
public interface BookRepository extends JpaRepository<Book, UUID> {
    // Métodos herdados automaticamente:
    // save(book)     → INSERT ou UPDATE
    // findById(id)   → SELECT * FROM books WHERE id = ?
    // findAll()      → SELECT * FROM books
    // deleteById(id) → DELETE FROM books WHERE id = ?
    // count()        → SELECT COUNT(*) FROM books
    
    // Métodos customizados por convenção de nome:
    boolean existsByIsbn(String isbn);  // → SELECT COUNT(*) > 0 FROM books WHERE isbn = ?
    List<Book> findByCategory(String category);  // → SELECT * FROM books WHERE category = ?
}
```

**JPA (Java Persistence API)** mapeia classes Java para tabelas do banco. O Hibernate é a implementação que o Spring usa.

### O que falar na apresentação

> "Usamos **Spring Data JPA** para persistência. Cada Repository herda de `JpaRepository` e já recebe métodos como `save`, `findById`, `findAll` e `deleteById` sem escrever nenhuma linha de SQL. Para consultas customizadas, basta declarar o método com a convenção de nome — por exemplo, `existsByIsbn()` — e o Spring gera a query automaticamente."

### Perguntas que podem ser feitas

**P: Vocês escrevem SQL?**
> R: "Na maioria dos casos, não. O Spring Data JPA gera SQL automaticamente pela convenção de nomes dos métodos. Para consultas mais complexas, como agregações do analytics, usamos `@Query` com JPQL."

**P: Qual banco usam?**
> R: "Em desenvolvimento, usamos H2 in-memory — o banco é criado na memória ao iniciar e descartado ao parar. Para produção, bastaria trocar o driver e URL no `application.yml` para PostgreSQL sem alterar código Java, porque o JPA abstrai o banco."

### ✅ Checklist de revisão

- [ ] Abra qualquer interface em `repository/` — note que ela extends `JpaRepository<Entidade, UUID>`
- [ ] Note que a interface é VAZIA na maioria dos casos — os métodos são herdados

---

## BLOCO 8 — Entity/Model
⏱️ **Tempo**: 15 minutos

### O que você precisa entender

Uma **Entity** é uma classe Java que representa uma **tabela do banco de dados**. Cada atributo é uma coluna.

```java
@Entity                           // "Esta classe é uma tabela"
@Table(name = "books")            // "A tabela se chama 'books'"
public class Book {
    @Id                           // "Este campo é a chave primária"
    @GeneratedValue(strategy = GenerationType.UUID)  // "ID gerado automaticamente"
    private UUID id;

    @Column(nullable = false)     // "Coluna NOT NULL"
    private String title;

    @ManyToOne                    // "Muitos livros pertencem a 1 autor"
    @JoinColumn(name = "author_id")
    private Author author;

    @ManyToMany                   // "Muitos livros podem ter muitas categorias"
    private List<Category> categories;
}
```

### Relacionamentos no seu projeto

| Tipo | Exemplo Real | Significado |
|---|---|---|
| `@ManyToOne` | `Book` → `Author` | Vários livros de 1 autor |
| `@ManyToOne` | `OrderItem` → `SalesOrder` | Vários itens de 1 pedido |
| `@OneToMany` | `SalesOrder` → `List<OrderItem>` | 1 pedido tem vários itens |
| `@ManyToMany` | `Book` → `List<Category>` | Livro pode ter várias categorias |
| `@OneToOne` (implícito) | `Book` → `Inventory` | 1 livro tem 1 registro de estoque |

### O que falar na apresentação

> "As Entities representam as tabelas do banco. A classe `Book`, por exemplo, tem campos como `title`, `isbn`, `salePrice`, e relacionamentos com `Author` (`@ManyToOne`), `Category` (`@ManyToMany`) e `Inventory` (`@OneToOne`). O JPA/Hibernate mapeia essas anotações para CREATE TABLE e JOINs automaticamente."

### ✅ Checklist de revisão

- [ ] Abra [Book.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/book/entity/Book.java) — veja `@Entity`, `@Id`, `@ManyToOne`
- [ ] Abra [SalesOrder.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/sales/entity/SalesOrder.java) — veja `@OneToMany` com `OrderItem` e `Payment`
- [ ] Abra [Customer.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/customer/entity/Customer.java) — veja os campos e a senha criptografada

---

## BLOCO 9 — DTO (Data Transfer Object)
⏱️ **Tempo**: 15 minutos

### O que você precisa entender

**DTO** é um objeto que transporta dados entre camadas. Existem dois tipos:

| Tipo | Exemplo | Uso |
|---|---|---|
| **Request DTO** | `BookCreateRequest` | Dados que o cliente ENVIA |
| **Response DTO** | `BookResponse` | Dados que o servidor DEVOLVE |

**Por que não usar a Entity diretamente?**
1. **Segurança**: A Entity `Customer` tem o campo `password`. Se retornarmos a Entity, a senha vai junto!
2. **Desacoplamento**: Se mudar a tabela, a API não quebra (o DTO é a "interface pública")
3. **Personalização**: O response pode ter campos calculados que não existem na tabela (ex: `orderNumber`)

### Exemplo concreto

```java
// Entity (como está no banco — NÃO devolver para o front)
class Customer {
    UUID id;
    String fullName;
    String email;
    String password;      // ← NUNCA pode ir para o front!
    String cpf;
    int rankingScore;
}

// Request DTO (o que o front envia ao cadastrar)
class CustomerCreateRequest {
    String fullName;      // obrigatório
    String email;         // obrigatório
    String cpf;           // obrigatório
    String password;      // obrigatório
    String confirmPassword;
}

// Response DTO (o que o back devolve)
class CustomerResponse {
    UUID id;              // gerado pelo banco
    String fullName;
    String email;
    String code;          // gerado automaticamente
    boolean active;
    Instant createdAt;    // gerado pelo JPA Auditing
    // SEM password, SEM cpf completo
}
```

### O que falar na apresentação

> "Usamos DTOs para separar a representação interna (Entity) da representação pública (API). Um `CustomerCreateRequest` recebe nome, email, CPF e senha. Mas o `CustomerResponse` retorna id, nome, email e código — **nunca a senha**. Isso segue o princípio de least privilege e protege dados sensíveis."

### Perguntas que podem ser feitas

**P: O que acontece se eu retornar a Entity direto?**
> R: "Vazamento de dados. A Entity `Customer` tem o campo `password` — mesmo criptografado, não devemos expor. Além disso, acopla a API à estrutura do banco: se eu mudar uma coluna, a resposta da API muda junto, quebrando os clientes."

### ✅ Checklist de revisão

- [ ] Compare `BookCreateRequest` vs `BookResponse` — note que o request não tem `id` nem `createdAt`
- [ ] Procure `Mapper.toResponse()` — é o conversor Entity → Response DTO

---

## BLOCO 10 — Autenticação e Segurança
⏱️ **Tempo**: 15 minutos

### O que você precisa entender

O sistema tem **3 camadas de segurança**:

**1. Senhas criptografadas com BCrypt**
```java
// Ao cadastrar, a senha é criptografada:
customer.setPassword(passwordEncoder.encode(request.getPassword()));
// No banco fica: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZ...
```

**2. Validação de senha com `@PasswordPolicy` (custom Bean Validation)**
- Mínimo 8 caracteres
- Pelo menos 1 maiúscula + 1 minúscula + 1 caractere especial

**3. Rotas admin protegidas por `X-Admin-Key`**
```java
// No AdminOrderService:
private void assertAdmin(String key) {
    if (!adminProperties.getKey().equals(key)) {
        throw new ForbiddenException("Chave administrativa inválida");
    }
}
```

**4. Filtro que bloqueia tokens de cliente em rotas `/admin/**`**
O [CustomerBearerAdminPathFilter](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/config/CustomerBearerAdminPathFilter.java) intercepta a requisição antes do controller. Se o Bearer token pertence a um cliente, retorna 403.

### O que falar na apresentação

> "A segurança é implementada em múltiplas camadas. Senhas são criptografadas com **BCrypt** — nunca armazenamos texto puro. Criamos uma **validação customizada de senha** usando a API de Bean Validation com anotação `@PasswordPolicy`. As rotas administrativas exigem um header `X-Admin-Key` validado no service. Além disso, um **filtro de servlet** impede que tokens de cliente acessem rotas `/admin/**`, garantindo defesa em profundidade."

### Perguntas que podem ser feitas

**P: Por que não usam JWT?**
> R: "Para este escopo acadêmico, usamos uma chave fixa para o admin. Em produção, migraríamos para JWT com Spring Security OAuth2 — cada login geraria um token com expiração, e o filtro validaria assinatura e claims. A arquitetura já está preparada para essa evolução."

### ✅ Checklist de revisão

- [ ] Abra [SecurityConfig.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/config/SecurityConfig.java) — CSRF off, Stateless
- [ ] Abra [PasswordPolicyValidator.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/customer/validation/PasswordPolicyValidator.java) — validação customizada
- [ ] Abra [AdminOrderService.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/sales/service/AdminOrderService.java) — veja `assertAdmin()` (L39)

---

## BLOCO 11 — Inteligência Artificial
⏱️ **Tempo**: 20 minutos

### O que você precisa entender

O módulo de IA tem **3 componentes**:

```
Usuário → AiController → AiChatService → monta prompt com dados reais → HttpAiProviderClient → OpenAI API → resposta
```

**O fluxo detalhado:**

1. Usuário digita "me recomende livros de ficção" no chat
2. [AiController](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/ai/controller/AiController.java) recebe o POST
3. [AiChatService](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/ai/service/AiChatService.java) monta o contexto:
   - Busca **TODOS os livros ativos** no banco (título, autor, preço, estoque, sinopse)
   - Busca **compras recentes** do cliente
   - Busca **feedbacks** do cliente
   - Injeta tudo num **prompt formatado** junto com a mensagem do usuário
4. [HttpAiProviderClient](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/ai/client/HttpAiProviderClient.java) envia para a OpenAI (`POST /chat/completions`)
5. Retorna a resposta ao usuário

**O diferencial: Fallback inteligente**

Se a OpenAI estiver offline (sem chave, sem internet), o sistema **NÃO quebra**. Ele usa um fallback local que:
- Analisa a mensagem do usuário por keywords
- Busca livros no banco por correspondência de título/autor/categoria
- Retorna recomendações reais do catálogo

### O que falar na apresentação

> "Integramos IA generativa da OpenAI com um padrão de abstração: a interface `AiProviderClient` define o contrato, e `HttpAiProviderClient` implementa a chamada HTTP. O diferencial é que injetamos o **catálogo real da loja** no contexto do prompt — então o LLM só recomenda livros que existem no sistema. Além disso, implementamos um **fallback inteligente**: se a API estiver offline, o sistema busca livros por keywords no próprio banco de dados. Isso garante disponibilidade total mesmo sem conexão."

### Perguntas que podem ser feitas

**P: A IA pode inventar livros que não existem?**
> R: "Não. O system prompt instrui explicitamente: 'NUNCA invente livros que não estejam no catálogo'. E o catálogo completo é injetado no contexto. Isso é chamado de RAG simplificado — Retrieval Augmented Generation."

**P: E se a OpenAI cair?**
> R: "O sistema detecta automaticamente (chave inválida, timeout, erro HTTP) e ativa o fallback local. Ele usa NLP básico — normalização de texto, remoção de acentos, detecção de categorias por keywords — e busca livros no banco. A resposta é formatada de forma idêntica à da IA."

### ✅ Checklist de revisão

- [ ] Abra [AiChatService.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/ai/service/AiChatService.java) — veja como monta o contexto (L41-L104)
- [ ] Abra [HttpAiProviderClient.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/ai/client/HttpAiProviderClient.java) — veja o fallback (L94-L254)
- [ ] Note o `SYSTEM` prompt na L25-33 do AiChatService

---

## BLOCO 12 — Analytics e Gráficos
⏱️ **Tempo**: 15 minutos

### O que você precisa entender

O back-end **NÃO gera gráficos**. Ele fornece os **dados estruturados**, e o front-end renderiza com a biblioteca **Recharts**.

**Fluxo:**

```
Front-end (React)              Back-end (Spring)                    Banco (H2)
─────────────────              ──────────────────                   ──────────
AdminAnalyticsPage   →  GET /admin/analytics/summary        →  SELECT SUM(total), COUNT(*)...
     ↓                  GET /admin/analytics/line-chart      →  SELECT date, SUM(revenue) GROUP BY date
     ↓                  GET /admin/analytics/category-volume →  SELECT month, category, SUM(qty)...
     ↓
SalesLineChart (Recharts)  ← JSON { labels: [...], values: [...] }
```

**O que o back-end retorna (exemplo):**
```json
{
  "labels": ["2026-06-01", "2026-06-02", "2026-06-03"],
  "values": [150.00, 299.00, 45.00]
}
```

**O front-end pega esses arrays e passa para o `<LineChart>` do Recharts**, que renderiza o gráfico.

### O que falar na apresentação

> "O módulo de analytics segue a mesma separação de responsabilidades. O back-end tem o `SalesAnalyticsService` que faz queries de agregação no banco — SUM de receita, COUNT de pedidos, GROUP BY por livro e categoria. Os dados são retornados como arrays de labels e values em JSON. O front-end usa a biblioteca **Recharts** para renderizar gráficos de linha (faturamento diário) e gráficos multilinhas (volume por categoria ao longo dos meses). O admin pode filtrar por período com datas de início e fim."

### ✅ Checklist de revisão

- [ ] Abra [SalesAnalyticsService.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/analytics/service/SalesAnalyticsService.java) — veja as queries de agregação
- [ ] Abra [SalesLineChart.tsx](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/front-end/src/components/SalesLineChart.tsx) — veja o `<LineChart>` do Recharts
- [ ] Abra [AdminAnalyticsPage.tsx](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/front-end/src/pages/AdminAnalyticsPage.tsx) — veja os cards de resumo (receita, itens, pedidos, ticket médio)

---

## BLOCO 13 — Tratamento de Erros
⏱️ **Tempo**: 10 minutos

### O que você precisa entender

O sistema centraliza TODOS os erros num único lugar: [GlobalExceptionHandler](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/exception/GlobalExceptionHandler.java)

```java
@RestControllerAdvice  // "Intercepte exceções de TODOS os controllers"
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        // Retorna: { "status": 404, "detail": "Livro não encontrado" }
    }

    @ExceptionHandler(DuplicateResourceException.class)
    // Retorna: HTTP 409 Conflict

    @ExceptionHandler(IllegalArgumentException.class)
    // Retorna: HTTP 400 Bad Request

    @ExceptionHandler(ForbiddenException.class)
    // Retorna: HTTP 403 Forbidden
}
```

**Formato da resposta (RFC 7807):**
```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Livro não encontrado"
}
```

### O que falar na apresentação

> "Centralizamos o tratamento de erros com `@RestControllerAdvice` e o padrão **RFC 7807 (Problem Details)**. Cada tipo de exceção mapeia para um HTTP status adequado: 404 para recurso não encontrado, 409 para duplicidade, 400 para erro de validação, 403 para acesso negado. Isso dá ao front-end uma interface de erro padronizada e previsível."

### ✅ Checklist de revisão

- [ ] Abra [GlobalExceptionHandler.java](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end/src/main/java/com/matheusgn/ecommerce/exception/GlobalExceptionHandler.java) — identifique os `@ExceptionHandler`

---

## BLOCO 14 — Perguntas Prováveis da Banca
⏱️ **Tempo**: 20 minutos (leia e releia em voz alta)

### Arquitetura e Organização

**P: Por que vocês usaram Java/Spring Boot?**
> R: "Spring Boot é o framework mais utilizado para APIs REST corporativas em Java. Ele oferece auto-configuração, injeção de dependências, integração nativa com JPA, Spring Security e documentação automática via Swagger. É a escolha padrão da indústria para back-end enterprise."

**P: Por que separar em Controller, Service e Repository?**
> R: "Para garantir o princípio da responsabilidade única. O Controller só lida com HTTP. O Service contém a regra de negócio. O Repository abstrai o banco. Isso permite testar cada camada isoladamente com mocks e trocar a implementação sem impactar as outras camadas."

**P: O que é injeção de dependências?**
> R: "Em vez de cada classe criar suas dependências com `new`, o Spring as cria e injeta automaticamente via construtor. Usamos `@RequiredArgsConstructor` do Lombok, que gera o construtor com todas as dependências `final`. Isso facilita testes — nos testes, injetamos mocks."

### Banco de Dados

**P: Por que H2 e não PostgreSQL?**
> R: "H2 é um banco in-memory ideal para desenvolvimento: zero configuração, cria-se automaticamente ao iniciar. Para produção, bastaria trocar driver e URL no `application.yml` — o JPA abstrai o banco, então nenhuma linha de código Java muda."

**P: O que é `@Transactional`?**
> R: "Garante atomicidade. Se o método falhar no meio, todas as alterações no banco são revertidas. No checkout, por exemplo: se salvar o pedido mas falhar no pagamento, o pedido é desfeito automaticamente."

### IA e Analytics

**P: A IA é do zero?**
> R: "Não. Integramos com a API da OpenAI (GPT-4o-mini) via chamada HTTP. O diferencial é que injetamos o catálogo real no prompt — a IA só recomenda livros que existem na loja. E temos um fallback local para funcionar sem internet."

**P: Como os gráficos funcionam?**
> R: "O back-end faz queries de agregação (SUM, COUNT, GROUP BY) e retorna arrays de labels e values. O front-end usa Recharts para renderizar. A separação é clara: dados no back, visualização no front."

### Segurança

**P: As senhas são armazenadas em texto puro?**
> R: "Jamais. Usamos BCrypt via `PasswordEncoder` do Spring Security. A senha é hasheada com salt antes de persistir. Na verificação, comparamos o hash — nunca revertemos."

### Melhorias

**P: O que poderia melhorar?**
> R: "1) Migrar de H2 para PostgreSQL com versionamento de schema via Flyway. 2) Substituir X-Admin-Key por JWT com Spring Security OAuth2. 3) Cache Redis para catálogo. 4) Mensageria com Kafka para desacoplar eventos de negócio. 5) Docker e CI/CD para deploy automatizado."

---

## BLOCO 15 — Treino Final de Apresentação
⏱️ **Tempo**: 30 minutos (fale em voz alta 3 vezes)

### Roteiro Completo de Fala (8–12 minutos)

---

#### 🎤 Abertura (1 min)

> "Bom dia/boa tarde. Nosso projeto é um sistema de **e-commerce de livros** completo, com back-end em Java/Spring Boot e front-end em React/TypeScript. O sistema implementa o ciclo completo de uma livraria online: cadastro de livros, carrinho com expiração temporal, checkout com múltiplas formas de pagamento, logística de entrega, logística reversa de trocas, integração com inteligência artificial generativa, e dashboards analíticos para o administrador."

---

#### 🎤 Arquitetura (2 min)

> "A arquitetura segue o padrão de **camadas N-Tier**: Controllers recebem requisições HTTP, Services processam a lógica de negócio, e Repositories persistem no banco H2 via JPA/Hibernate.

> Organizamos o código **por domínio de negócio**: tudo sobre livros fica no pacote `book`, tudo sobre vendas no pacote `sales`, e assim por diante. O projeto tem **15 módulos** independentes. Dentro de cada módulo, separamos por camada técnica: controller, service, repository, entity e dto.

> As Controllers são **thin** — sem lógica. Toda regra fica nos Services. Os DTOs separam a representação interna do banco da interface pública da API."

---

#### 🎤 Fluxo de Venda (3 min)

> "Vou demonstrar o fluxo principal: a compra. São 5 etapas:

> **Primeira**: o cliente adiciona itens ao carrinho. O `CartController` recebe o POST, o `CartService` valida o estoque e **reserva** as unidades no `InventoryBalanceService`. Cada item expira em 2 minutos — se o cliente demorar, a reserva é liberada automaticamente.

> **Segunda**: o cliente calcula o frete. O `CheckoutService` calcula com base no CEP e no subtotal do carrinho, usando uma fórmula: base de R$ 12,90 + 2% do subtotal + fator CEP.

> **Terceira**: o cliente define o pagamento. O sistema suporta **múltiplas formas**: N cartões de crédito (mínimo R$ 10 cada) combinados com cupons de troca ou promocionais. O service valida regras como: sem cupons redundantes, soma deve cobrir exatamente o total.

> **Quarta**: o cliente finaliza. O `CheckoutService.finalizePurchase()` — que é o método mais complexo do sistema, com ~130 linhas — valida tudo novamente, monta o pedido, salva no banco com status EM_PROCESSAMENTO, registra auditoria e fecha o carrinho.

> **Quinta**: o administrador aprova o pagamento. Aí sim ocorre a **baixa física de estoque** via `SalesOutboundService`, o registro no extrato do cliente via `CustomerTransactionRecorder`, e a transição de status para APROVADO. Depois o admin despacha (EM_TRÂNSITO) e confirma entrega (ENTREGUE)."

---

#### 🎤 Inteligência Artificial (1.5 min)

> "Integramos um assistente virtual com IA generativa da OpenAI. O `AiChatService` busca no banco todos os livros ativos — título, autor, preço, estoque, sinopse — e injeta no prompt junto com o histórico de compras e feedbacks do cliente. A IA responde com recomendações de livros **que realmente existem na loja**.

> O diferencial técnico é o **fallback inteligente**: se a API da OpenAI estiver offline, o sistema faz matching por keywords no catálogo do banco e retorna recomendações locais. O usuário nunca fica sem resposta."

---

#### 🎤 Analytics (1 min)

> "O painel administrativo inclui dashboards com gráficos de receita diária e volume de vendas por categoria. O back-end faz queries de agregação — SUM de receita, GROUP BY por data e categoria — e retorna arrays de dados. O front-end renderiza com a biblioteca **Recharts**. O admin pode filtrar por período com datas de início e fim."

---

#### 🎤 Segurança e Qualidade (1 min)

> "Senhas são criptografadas com **BCrypt**. Criamos uma validação de senha customizada com Bean Validation: mínimo 8 caracteres, maiúscula, minúscula e especial. Rotas admin são protegidas por header `X-Admin-Key`, e um filtro de servlet bloqueia tokens de cliente em rotas `/admin/**`.

> O tratamento de erros é centralizado com `@RestControllerAdvice` seguindo o padrão **RFC 7807 (Problem Details)**: cada exceção mapeia para um HTTP status adequado — 404, 409, 400, 403.

> O projeto tem **47 testes unitários e de integração** no back-end e **15 testes E2E** com Playwright no front-end."

---

#### 🎤 Encerramento (30 seg)

> "Em resumo: o sistema é auditável, testado, e implementa regras de negócio complexas como reserva de estoque em tempo real, pagamento multi-cartão com cupons, logística reversa com geração de cupom, e integração com IA generativa com fallback de alta disponibilidade. O projeto demonstra domínio em engenharia de software aplicada, desde padrões de projeto até integração com serviços externos."

---

## ⏱️ Cronograma Sugerido

| Ordem | Bloco | Tempo | Prioridade |
|---|---|---|---|
| 1º | Bloco 3 — Fluxo de Requisição | 30 min | 🔴 CRÍTICO |
| 2º | Bloco 6 — Service | 25 min | 🔴 CRÍTICO |
| 3º | Bloco 1 — Visão Geral | 20 min | 🟡 ALTO |
| 4º | Bloco 2 — Estrutura de Pacotes | 20 min | 🟡 ALTO |
| 5º | Bloco 4 — CRUD | 20 min | 🟡 ALTO |
| 6º | Bloco 5 — Controller | 15 min | 🟡 ALTO |
| 7º | Bloco 11 — IA | 20 min | 🟡 ALTO |
| 8º | Bloco 7 — Repository | 15 min | 🟢 MÉDIO |
| 9º | Bloco 8 — Entity | 15 min | 🟢 MÉDIO |
| 10º | Bloco 9 — DTO | 15 min | 🟢 MÉDIO |
| 11º | Bloco 10 — Segurança | 15 min | 🟢 MÉDIO |
| 12º | Bloco 12 — Analytics | 15 min | 🟢 MÉDIO |
| 13º | Bloco 13 — Erros | 10 min | 🟢 MÉDIO |
| 14º | Bloco 14 — Perguntas da Banca | 20 min | 🔴 CRÍTICO |
| 15º | Bloco 15 — Treino de Fala | 30 min | 🔴 CRÍTICO |
| | **TOTAL** | **~5 horas** | |

> [!TIP]
> **Se tiver menos de 3 horas**, foque nos blocos marcados como 🔴 CRÍTICO: Fluxo (3), Service (6), Perguntas (14) e Treino de Fala (15). Esses 4 blocos cobrem 80% do que pode cair.
