# Especificação de Caso de Uso: Realizar Venda Eletrônica

**Projeto:** E-commerce de Livros (Matheus GN)  
**Disciplina:** LES — Laboratório de Engenharia de Software  
**Filição:** FATEC (Modelo de Documentação UC_FATEC)  
**Status:** Revisado e Aprovado

---

## Histórico de Versões

| Data | Versão | Descrição | Autor | Revisor |
| :--- | :--- | :--- | :--- | :--- |
| 13/06/2026 | 1.0 | Especificação completa do caso de uso de venda (checkout), incluindo fluxos alternativos de cancelamento, trocas totais/parciais, múltiplos pagamentos e tratamento de exceções. | Antigravity AI | Matheus Costa |

---

## 1. Nome do Caso de Uso
**CDU-VENDA: Realizar Venda Eletrônica e Gestão de Pós-Venda**

## 2. Objetivo
Prover uma solução computacional que permita ao cliente consolidar produtos em um carrinho de compras, selecionar endereço de entrega, calcular frete, efetuar o pagamento através de múltiplos meios (cartões de crédito, cupons de troca e cupons promocionais) e finalizar a compra. Além disso, gerenciar as atividades de pós-venda, como o cancelamento do pedido e solicitações de troca total ou parcial de mercadorias.

## 3. Descrição
O cliente autenticado monta um carrinho de compras na loja e inicia o processo de checkout. O sistema exige a definição do endereço de entrega (salvo ou novo) e realiza o cálculo de frete correspondente. Em seguida, o cliente escolhe a forma de divisão do pagamento respeitando as regras de valor mínimo. Ao finalizar, o estoque é deduzido e o pedido assume o status `EM_PROCESSAMENTO` aguardando a aprovação do pagamento. A partir do momento da entrega, abrem-se os fluxos de pós-venda, permitindo que o cliente solicite trocas parciais ou totais, as quais são mediadas pelo administrador do sistema.

## 4. Requisitos Funcionais Relacionados
*   **RF0031–RF0032:** Gerenciar carrinho de compras e quantidades.
*   **RF0033:** Realizar compra a partir do carrinho.
*   **RF0034:** Calcular frete da compra.
*   **RF0035:** Selecionar endereço de entrega.
*   **RF0036:** Selecionar formas de pagamento (cartões, cupons).
*   **RF0037:** Finalizar compra.
*   **RF0038–RF0039:** Despachar e marcar produtos como entregues (Admin).
*   **RF0040:** Solicitar troca (Cliente).
*   **RF0041–RF0042:** Autorizar e visualizar trocas (Admin).
*   **RF0043–RF0044:** Confirmar recebimento de itens de troca e gerar cupom (Admin).
*   **RF0025:** Consulta de transações do cliente (extrato).
*   **RF0053–RF0054:** Baixa e reentrada de estoque.

## 5. Tipo de Caso de Uso
**[X] Concreto** (Iniciado diretamente por um Ator)  
**[ ] Abstrato** (Reutilizado por outros Casos de Uso)

## 6. Atores
*   **Cliente (Primário):** Usuário que realiza a navegação, compra, pagamento e solicitações de troca.
*   **Administrador (Secundário):** Responsável por atualizar o status do pedido, autorizar trocas, confirmar o recebimento de devoluções e emitir cupons.
*   **Operadora de Cartão de Crédito (Serviço Externo):** Responsável por processar a transação financeira dos cartões utilizados no checkout.
*   **API Externa de ISBN (Serviço Externo):** Usada para validação secundária durante a verificação de integridade dos itens.

## 7. Pré-condições
1.  O Cliente deve estar autenticado no sistema.
2.  O Cliente deve possuir ao menos um livro válido adicionado ao seu carrinho de compras temporário.
3.  Os livros adicionados ao carrinho devem possuir estoque disponível no momento da inserção (`RN0031`).

## 8. Permissão de Usuário (Access Control)
*   **Cliente:** Permissão `CUSTOMER_ROLE` para navegação, checkout, visualização de transações e solicitação de troca.
*   **Administrador:** Permissão `ADMIN_ROLE` (validada via cabeçalho `X-Admin-Key`) para alteração de status de pedidos e processamento de trocas.

---

## 9. Fluxo Principal (Registrar Pedido de Venda)

*   **P1. Iniciar Checkout:** O Cliente visualiza o carrinho e clica em "Fechar Compra". O sistema exibe a interface de checkout.
*   **P2. Selecionar Endereço de Entrega:** O Cliente seleciona um endereço cadastrado de sua lista (`AddressType` = `DELIVERY`). 
    *   *Opcional:* O Cliente pode cadastrar um novo endereço diretamente nesta etapa (**Fluxo Alternativo A5**).
*   **P3. Calcular Frete:** O sistema invoca internamente o serviço de cálculo (`FreightService`), aplicando a fórmula: `frete = 12.90 + (subtotal * 0.02) + (soma_digitos_cep * 0.15)`. O valor é exibido na tela e gravado temporariamente no carrinho do Cliente (`RF0034`).
*   **P4. Informar Formas de Pagamento:** O Cliente insere ou seleciona as formas de pagamento desejadas. O sistema disponibiliza: cartões de crédito salvos, inclusão de novos cartões, cupons de troca disponíveis e cupons promocionais.
    *   *Opção de Divisão de Pagamento:* O Cliente pode distribuir o total entre múltiplas formas (**Fluxo Alternativo A4**).
    *   *Inclusão de Novo Cartão:* O Cliente pode registrar um novo cartão nesta tela (**Fluxo Alternativo A6**).
*   **P5. Revisar e Finalizar Compra:** O Cliente revisa o resumo do pedido (itens, quantidades, frete, descontos de cupons e divisão de pagamento) e clica em "Finalizar Compra".
*   **P6. Validar Integridade e Estoque:** O sistema executa as validações finais:
    *   Verifica se não há itens expirados/bloqueados no carrinho (**Fluxo de Exceção E1**).
    *   Verifica se o saldo físico do estoque atende às quantidades solicitadas (**Fluxo de Exceção E2**).
    *   Valida se a soma dos pagamentos informados é exatamente igual ao valor total do pedido (itens + frete) com escala decimal correta (**Fluxo de Exceção E5**).
*   **P7. Processar Pagamento e Criar Pedido:** O sistema envia a requisição de débito para a operadora de cartão de crédito:
    *   Caso a transação seja recusada pela operadora (**Fluxo de Exceção E3**).
    *   Caso seja aprovada, o sistema gera o pedido com status `APROVADO`.
*   **P8. Persistir Dados de Venda e Atualizar Estoque:**
    *   O sistema debita a quantidade física comprada de cada livro no banco de dados (`RF0053`).
    *   Limpa e esvazia o carrinho temporário do Cliente (`RF0033`).
    *   Grava um registro histórico de transação financeira do tipo `PURCHASE` no extrato do Cliente contendo o total do pedido (`RF0025`).
    *   Envia mensagem de sucesso com o número identificador do pedido e finaliza o caso de uso.

---

## 10. Fluxos Alternativos

### A1. Cancelar Pedido (Pós-Venda)
Este fluxo pode ocorrer a qualquer momento após o término do fluxo principal, desde que o pedido esteja nos status `EM_PROCESSAMENTO` ou `APROVADO`, e antes do despacho físico.
*   **A1.1.** O Cliente (ou Administrador) seleciona o pedido em sua listagem e solicita o cancelamento.
*   **A1.2.** O sistema verifica se o status atual permite o cancelamento síncrono.
*   **A1.3.** Se o pedido estava `APROVADO`, o sistema realiza o estorno de estoque das mercadorias, incrementando a quantidade de volta no catálogo físico.
*   **A1.4.** O sistema aciona o estorno financeiro (ou gera um cupom de troca equivalente ao valor pago, conforme preferência).
*   **A1.5.** O status do pedido é atualizado para `CANCELADO` (ou `PAGAMENTO_RECUSADO`). O caso de uso retorna à listagem de pedidos.

### A2. Realizar Solicitação de Troca Total
Este fluxo ocorre quando o Cliente solicita a devolução de **todos** os itens de um pedido que já esteja marcado como `ENTREGUE` (`RN0043`).
*   **A2.1.** O Cliente acessa a listagem de pedidos, localiza o pedido com status `ENTREGUE` e clica em "Solicitar Troca Total".
*   **A2.2.** O sistema cria um registro de solicitação de troca contendo todos os itens do pedido com status `REQUESTED`.
*   **A2.3.** O status global do pedido é alterado pelo sistema para `EM_TROCA` (`RN0041`).
*   **A2.4.** O sistema envia uma notificação visual ao painel do Administrador. O fluxo segue para a mediação administrativa (**Fluxo Alternativo A3**).

### A3. Realizar Solicitação de Troca Parcial
Este fluxo ocorre quando o Cliente deseja devolver apenas **um ou alguns** itens de uma compra com status `ENTREGUE`.
*   **A3.1.** O Cliente acessa a página de detalhes do pedido `ENTREGUE`, seleciona o item específico (`orderItemId`) e informa a quantidade a ser trocada.
*   **A3.2.** O sistema valida se o item escolhido já não possui outra troca aberta (`REQUESTED` ou `AUTHORIZED`) (`RN0031`).
*   **A3.3.** O sistema registra a solicitação com status `REQUESTED`.
*   **A3.4.** O status do pedido pai é alterado para `EM_TROCA`. O fluxo segue para a mediação administrativa (**Fluxo Alternativo A3**).

### A4. Processar Troca pelo Administrador (Devolução/Resolução)
Este fluxo é executado pelo Administrador para processar solicitações de troca pendentes (`REQUESTED` ou `AUTHORIZED`).
*   **A4.1.** O Administrador filtra as solicitações pelo status `EM_TROCA` e clica em **Autorizar Troca** (`RF0041`).
    *   O status da solicitação vai para `AUTHORIZED` e o status do pedido para `TROCA_AUTORIZADA` (`RN0041`).
*   **A4.2.** O cliente posta o produto de volta. Ao receber fisicamente a mercadoria, o Administrador abre o painel da solicitação e seleciona **Confirmar Recebimento** (`RF0043`).
*   **A4.3.** O Administrador indica se os itens devem retornar ao estoque comercial (`returnToStock` = `true` ou `false`).
    *   Se `true`, o sistema incrementa automaticamente o estoque físico do livro correspondente (`RF0054`).
*   **A4.4.** O sistema gera automaticamente um **Cupom de Troca** vinculado ao CPF do Cliente no formato `TROCA-XXXXXXXX` contendo o valor pago pelos itens trocados (`RF0044`).
*   **A4.5.** A solicitação de troca é marcada como concluída (`RECEIVED`). O status do pedido retorna para `ENTREGUE` (ciclo encerrado). O sistema notifica o cliente sobre o código do cupom gerado (`RNF0046`).

### A5. Pagar com Múltiplos Meios de Pagamento
Este fluxo ocorre no passo **P4** do fluxo principal quando o Cliente decide fragmentar a cobrança.
*   **A5.1.** O Cliente adiciona cartões e cupons ao pagamento. O sistema valida as seguintes condições:
    *   Cada cartão de crédito inserido deve receber um valor mínimo de débito de R$ 10,00 (`RN0034`).
    *   *Exceção:* O débito no cartão de crédito pode ser inferior a R$ 10,00 **apenas** se o saldo restante for coberto integralmente por cupons de troca ou cupom promocional (`RN0035`).
    *   Somente 1 cupom promocional é aceito por checkout (`RN0033`).
*   **A5.2.** O Cliente confirma a divisão. O sistema realiza a soma e valida no passo **P6**.

### A6. Inserir Novo Endereço no Checkout
Este fluxo ocorre no passo **P2** do fluxo principal.
*   **A6.1.** O Cliente clica em "Adicionar Novo Endereço de Entrega".
*   **A6.2.** O sistema exibe formulário contendo: tipo de residência, tipo de logradouro, logradouro, número, bairro, CEP, cidade, estado, país e observações (`RN0023`).
*   **A6.3.** O Cliente preenche e marca a opção "Salvar endereço no meu perfil" (`saveToProfile` = `true` / `false`).
*   **A6.4.** O sistema valida o preenchimento obrigatório dos dados e, se marcado `true`, persiste o endereço no cadastro permanente do Cliente (`RF0026`). O endereço é associado ao checkout atual e o fluxo retorna a **P3**.

### A7. Inserir Novo Cartão no Checkout
Este fluxo ocorre no passo **P4** do fluxo principal.
*   **A7.1.** O Cliente seleciona "Pagar com Novo Cartão" e insere: número do cartão, nome impresso, bandeira registrada no sistema e código de segurança (`RN0024`).
*   **A7.2.** O Cliente escolhe se deseja salvar o cartão para futuras compras (`saveNewCardToProfile` = `true` / `false`).
*   **A7.3.** O sistema valida os campos obrigatórios e a bandeira. Se `true`, persiste o cartão no perfil do Cliente (`RF0027`). O cartão é associado ao pagamento atual e o fluxo retorna a **P4**.

---

## 11. Fluxos de Exceção

### E1. Itens Expirados/Bloqueados no Carrinho
Ocorre no passo **P6** do fluxo principal se o tempo limite de reserva do item no carrinho for ultrapassado (`RN0044`).
*   **E1.1.** O sistema identifica que um ou mais itens no carrinho expiraram (tempo superior ao parâmetro `cart.item.expiration-minutes`).
*   **E1.2.** O sistema bloqueia a finalização da compra, remove os itens expirados do carrinho e libera-os de volta ao estoque comercial.
*   **E1.3.** O sistema exibe uma mensagem de alerta: *"Sua sessão expirou para alguns itens. Por favor, adicione-os novamente."*
*   **E1.4.** O fluxo é abortado e o cliente retorna à tela do carrinho para ajuste de itens.

### E2. Estoque Insuficiente ao Finalizar
Ocorre no passo **P6** do fluxo principal se o estoque real do livro tiver sido alterado/vendido antes do processamento final (`RN0032`).
*   **E2.1.** O sistema verifica que a quantidade física disponível do livro no estoque é inferior à quantidade no carrinho.
*   **E2.2.** O sistema ajusta automaticamente a quantidade do item no carrinho para o saldo máximo disponível (ou remove o item caso o estoque esteja zerado).
*   **E2.3.** Exibe a mensagem de erro: *"Lamentamos, mas a quantidade disponível de '[Título do Livro]' mudou. Ajustamos seu carrinho."*
*   **E2.4.** O fluxo de checkout é cancelado e o cliente retorna para visualização do carrinho de compras.

### E3. Pagamento Recusado pela Operadora
Ocorre no passo **P7** do fluxo principal se qualquer um dos cartões de crédito informados for recusado.
*   **E3.1.** A operadora de cartão de crédito retorna rejeição (falta de limite, dados incorretos, etc.).
*   **E3.2.** O sistema altera o status do pedido para `PAGAMENTO_RECUSADO`.
*   **E3.3.** O sistema libera imediatamente a reserva temporária física de todos os itens do pedido no estoque, disponibilizando-os para outros clientes (`RN0028`).
*   **E3.4.** Exibe mensagem de erro na tela: *"Pagamento não aprovado pela operadora. Verifique seus dados ou tente outra forma de pagamento."*
*   **E3.5.** O fluxo de checkout é mantido aberto na etapa de pagamento para o cliente tentar novamente.

### E4. Divisão de Pagamento Inconsistente
Ocorre no passo **P6** se a soma das parcelas de pagamento informadas não coincidir com o valor total calculado do pedido.
*   **E4.1.** O sistema compara o valor total final (subtotal dos livros + frete - descontos de cupons) com a soma das linhas de pagamento enviadas pelo formulário.
*   **E4.2.** Identificada divergência de centavos ou valores, o sistema impede a finalização do pedido.
*   **E4.3.** Exibe mensagem de alerta: *"A soma das formas de pagamento não coincide com o valor total da compra. Ajuste os valores."*
*   **E4.4.** O fluxo retorna para o passo **P4** (Forma de Pagamento) permitindo o reajuste.

---

## 12. Protótipos de Tela
*(Nota: Protótipos gráficos detalhados são fornecidos nos artefatos visuais do projeto Frontend. Abaixo descreve-se a estrutura de campos de tela)*

*   **Tela de Checkout (Resumo do Pedido):**
    *   Tabela com: Capa, Título, Quantidade, Preço Unitário, Valor Total do Item.
    *   Área de CEP com campo de entrada, botão "Calcular Frete" e exibição do valor do frete e data de entrega estimada.
    *   Painel de seleção de endereço de entrega (Rádio botões da lista de cadastrados) e botão "Inserir Novo Endereço".
*   **Tela de Pagamento (Checkout - Parte 2):**
    *   Lista de cartões salvos para seleção rápida.
    *   Lista de Cupons de Troca ativos vinculados ao CPF (checkbox para aplicar crédito).
    *   Campo de texto para Cupom Promocional (com botão "Aplicar").
    *   Campos de digitação para múltiplos cartões: Valor a ser cobrado em cada cartão.
*   **Tela de Detalhes do Pedido (Pós-Venda - Cliente):**
    *   Informações do pedido: Número, Data, Status (`ENTREGUE`, etc.).
    *   Listagem de itens do pedido. Se o status for `ENTREGUE`, exibe ao lado de cada item o botão "Solicitar Troca" e campo de quantidade.

---

## 13. Dicionário de Elementos de Tela / Comportamento

| Nome do Elemento | Tipo de Elemento | Status/Validação | Comportamento do Elemento |
| :--- | :--- | :--- | :--- |
| **Lista de Endereços** | Radio Button List | Obrigatório | Permite selecionar um endereço cadastrado do perfil. Atualiza o frete síncrono. |
| **CEP de Entrega** | Campo de Entrada | Numérico, 8 dígitos | Utilizado para calcular o valor do frete caso o usuário insira um endereço novo. |
| **Selecionar Cartão Salvo** | Combo Box / Radio | Opcional | Permite escolher um cartão registrado para cobrar parte ou total da compra. |
| **Código do Cupom de Troca** | Campo de Entrada | Alfanumérico | Permite digitar o código `TROCA-XXXXXXXX` para aplicar o reembolso como desconto. |
| **Código do Cupom Promocional** | Campo de Entrada | Alfanumérico | Permite digitar cupom de desconto (máximo 1 por compra). |
| **Valor por Cartão** | Campo de Entrada | Decimal | Valor a debitar do cartão. Deve ser >= R$ 10,00, exceto se combinado com cupom. |
| **Botão Finalizar Compra** | Botão (Submissão) | Default | Invoca a rota `/checkout/finalize` e desencadeia a validação de estoque e pagamento. |
| **Botão Solicitar Troca** | Botão | Visível em `ENTREGUE` | Abre popup de troca informando o `orderItemId` e permitindo selecionar quantidade. |

---

## 14. Pós-condições
*   **Sucesso:** Um novo pedido é gerado no banco de dados com status `APROVADO`, o estoque é decrementado, as transações históricas do cliente são registradas com a linha `PURCHASE`, e o carrinho de compras do usuário é limpo.
*   **Cancelamento:** O pedido é registrado com status `PAGAMENTO_RECUSADO` e as reservas físicas são estornadas no estoque.
*   **Troca Concluída:** A solicitação de troca de produto assume status `RECEIVED`, o pedido volta ao status `ENTREGUE` (ou `TROCADO`), o estoque é incrementado se o item for reaproveitável, e um novo registro de cupom ativo é criado na conta do Cliente.

## 15. Requisitos Não-Funcionais Aplicáveis
*   **RNF0011 (Tempo de Resposta):** O processamento de checkout e finalização de compra deve durar no máximo 1 segundo.
*   **RNF0031 (Segurança de Cartão):** Dados sensíveis de cartões novos não podem ser persistidos de forma legível no banco de dados.

## 16. Ponto de Extensão
*   **PE01: Validar Fraudes de Pagamento:** Executar processo de análise antifraude na operadora de cartão de crédito no momento do processamento do pagamento.
*   **PE02: Notificar Envio / Troca:** Disparar e-mail de notificação ao cliente quando o status for alterado para `EM_TRANSITO`, `ENTREGUE` ou quando a troca for autorizada.

## 17. Critérios de Aceite
1.  O sistema deve impedir a finalização de compras se o estoque de qualquer item for menor que a quantidade requisitada.
2.  O sistema deve rejeitar checkouts onde a soma das cobranças aplicadas aos meios de pagamento divirja do valor total calculado.
3.  A inativação de cupons aplicados deve ocorrer síncrona com o sucesso do pedido.
4.  O sistema deve impedir solicitações de troca para pedidos que não estejam no status `ENTREGUE`.

## 18. Observações
*   **Prioridade de Desenvolvimento:** Alta (Caso de uso de condução/núcleo do negócio).
