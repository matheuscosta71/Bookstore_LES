# 📚 Bookstore LES — E-Commerce de Livros

Este é um projeto completo de **E-Commerce de Livros (Bookstore LES)**, desenvolvido para a disciplina de Laboratório de Engenharia de Software (LES). Ele é composto por uma **API REST no back-end** (Java / Spring Boot) e um **aplicativo SPA no front-end** (React / Redux / TypeScript / Tailwind CSS), além de contar com integração de inteligência artificial generativa e painéis analíticos com gráficos interativos.

---

## 🚀 Tecnologias Utilizadas

### Back-End (API REST)
* **Linguagem**: Java 17
* **Framework Principal**: Spring Boot 3.3.x
* **Persistência**: Spring Data JPA com Hibernate
* **Banco de Dados**: H2 Database (banco em memória para ambiente de desenvolvimento/teste)
* **Segurança**: Spring Security & Criptografia BCrypt (senhas de clientes)
* **Documentação**: Swagger UI / OpenAPI 3 (SpringDoc)
* **Testes**: JUnit 5, Mockito & MockWebServer
* **Outros**: Lombok (redução de boilerplate), Spring AOP (Auditoria)

### Front-End (SPA)
* **Biblioteca Principal**: React 18
* **Linguagem**: TypeScript
* **Ferramenta de Build**: Vite
* **Gerenciamento de Estado**: Redux Toolkit (Slices e Store global)
* **Estilização**: Tailwind CSS
* **Roteamento**: React Router DOM v6
* **Formulários & Validação**: React Hook Form & Zod
* **Gráficos**: Recharts (Analytics)
* **Cliente HTTP**: Axios
* **Testes E2E**: Playwright (Testes de ponta a ponta)

### Integrações Externas
* **OpenAI API**: Modelo `gpt-4o-mini` (Chatbot assistente e motor de recomendações personalizadas com Fallback local resiliente).

---

## 📁 Estrutura do Projeto

O repositório é dividido em duas pastas principais:
* **[back-end](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/back-end)**: Contém o código Java Spring Boot, estruturado por domínios de negócio (*feature-based*): `book`, `customer`, `sales` (carrinho, checkout e pedidos), `inventory` (estoque e precificação), `ai` (chatbot), `analytics`, `audit` e `auth`.
* **[front-end](file:///c:/Users/Matheus%20Costa/Downloads/matheus-gn/front-end)**: Contém a SPA em React, estruturada com a store Redux global, componentes de UI, páginas (cliente e admin) e scripts de testes automatizados E2E.

---

## 🛠️ Como Rodar o Programa

### Pré-requisitos
* **Java Development Kit (JDK) 17** ou superior instalado
* **Node.js 18** ou superior com **npm** instalado
* **Maven** instalado (ou use o wrapper `./mvnw` incluso)

---

### Passo 1: Executando o Back-End

1. Acesse o diretório do back-end:
   ```bash
   cd back-end
   ```
2. Inicialize a aplicação Spring Boot utilizando o Maven:
   ```bash
   mvn spring-boot:run
   ```
3. A API REST estará rodando na porta **8080**:
   * **URL Base**: `http://localhost:8080`
   * **Console do Banco H2**: `http://localhost:8080/h2-console`
     * *JDBC URL*: `jdbc:h2:mem:booksdb`
     * *User*: `sa`
     * *Password*: (deixe em branco)
   * **Documentação Swagger (OpenAPI)**: `http://localhost:8080/swagger-ui.html`

*Nota: Por padrão, a aplicação inicia populando o banco de dados com dados fictícios para fins de demonstração (seed ativado no perfil de desenvolvimento).*

---

### Passo 2: Executando o Front-End

1. Abra um novo terminal e acesse o diretório do front-end:
   ```bash
   cd front-end
   ```
2. Instale as dependências do projeto:
   ```bash
   npm install
   ```
3. Crie e configure o arquivo `.env` a partir do exemplo fornecido:
   ```bash
   cp .env.example .env
   ```
   *(Verifique se as chaves, como `VITE_API_URL` e `VITE_ADMIN_KEY`, estão de acordo com o configurado no back-end).*
4. Inicie o servidor de desenvolvimento:
   ```bash
   npm run dev
   ```
5. O aplicativo front-end estará disponível em:
   * `http://localhost:5173`

*O Vite está configurado para fazer proxy automático de requisições de `/api/*` para `http://localhost:8080/*`, facilitando a integração em ambiente de desenvolvimento.*

---

## 🧪 Como Rodar os Testes Automatizados

O sistema conta com testes automatizados no back-end (unitários/integração) e no front-end (E2E).

### 1. Testes do Back-End (JUnit 5 + Mockito)
Para executar os testes de lógica de negócio e integração da API:
1. Acesse o diretório do back-end:
   ```bash
   cd back-end
   ```
2. Execute o comando de testes do Maven:
   ```bash
   mvn test
   ```
Os resultados e a cobertura de testes serão gerados no console e na pasta `target/surefire-reports`.

### 2. Testes do Front-End (Playwright E2E)
Os testes E2E validam fluxos de ponta a ponta na interface (Cadastro, Fluxo de Checkout, Divisão de Cartões, Troca e Devolução, IA Chat, Analytics, etc.).
1. Acesse o diretório do front-end:
   ```bash
   cd front-end
   ```
2. Certifique-se de que os navegadores do Playwright estejam instalados (necessário apenas na primeira execução):
   ```bash
   npx playwright install
   ```
3. **Rodar os testes em segundo plano (Modo Headless)**:
   ```bash
   npm run test:e2e
   ```
4. **Rodar os testes exibindo o navegador (Modo Headed / Visual)**:
   ```bash
   npm run test:e2e:headed
   ```
5. **Rodar um teste específico de forma visível**:
   ```bash
   npx playwright test e2e/tests/01-compra-basica.spec.ts --headed
   ```
   *Dica: O Playwright possui um delay padrão configurado (`slowMo: 1000`) para que seja fácil acompanhar visualmente o preenchimento de campos e os cliques na tela.*

---

## 💡 Principais Funcionalidades Implementadas

O projeto foi construído para atender a um rigoroso conjunto de regras de negócio da disciplina:
* **Cadastro Completo de Clientes**: Suporte a múltiplos cartões (com cartão preferencial) e múltiplos endereços (entrega e cobrança), controle de senha forte (BCrypt) e alteração isolada de senha.
* **Cadastro de Livros & Precificação**: Associação a grupos de precificação com cálculo automático do preço de venda com base na margem de lucro e custo. Geração automática de código único (`LIVR-XXXXXX-YY`) e validação externa de ISBN.
* **Carrinho de Compras & Reserva de Estoque**: Adição com expiração visual automática (2 minutos). O estoque é temporariamente reservado ao colocar itens no carrinho para evitar compras duplicadas indisponíveis (*overselling*).
* **Checkout Flexível**: Cálculo dinâmico de frete por CEP e subtotal. Suporta pagamento com cartão único, múltiplos cartões (mínimo de R$ 10,00 por cartão) e cupons de troca e promocionais de forma combinada.
* **Ciclo de Pedidos e Logística**: Máquina de estados controlando a transição de status de compras (`EM_PROCESSAMENTO` ➔ `APROVADO`/`REPROVADO` ➔ `EM_TRANSPORTE` ➔ `ENTREGUE`).
* **Logística Reversa (Trocas)**: Solicitação de troca parcial ou total por parte do cliente, autorização pelo administrador, controle de reentrada física de itens no estoque e geração de cupom de troca automática no valor do reembolso.
* **Dashboard Administrativo (Analytics)**: Gráficos de faturamento e volume por categoria ao longo do tempo (Recharts), filtrados por período.
* **Log de Auditoria**: Auditoria de alterações em entidades críticas, com proteção e ocultamento automático de senhas (`***`) nos payloads salvos.
