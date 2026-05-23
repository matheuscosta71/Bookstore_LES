# Documento de requisitos — E-commerce de livros

**Disciplina:** LES — 1º semestre de 2026  

**Fonte:** conversão para consulta a partir de `DRS_LES_1_2026.docx.pdf` (extração automática de texto). Pequenas quebras de linha do PDF foram normalizadas; siglas **RF** / **RNF** / **RN** e textos seguem o documento original.

---

## Requisitos funcionais (RF)

### Grupo: Cadastro de livros

| ID | Nome | Descrição |
|----|------|-----------|
| RF0011 | Cadastrar livro | O sistema deve manter um cadastro único para livros. |
| RF0012 | Inativar cadastro de livro | O sistema deve possibilitar que livros sejam inativados. |
| RF0013 | Inativar livro de forma automática | O sistema deve inativar livros sem estoque e que não possuem venda com valor inferior a parâmetro predefinido no sistema. |
| RF0014 | Alterar cadastro de livro | O sistema deve possibilitar a alteração de dados cadastrais para os livros. |
| RF0015 | Consulta de livros | O sistema deve possibilitar que um livro seja consultado com base em um filtro definido pelo usuário. Todos os campos utilizados para identificação do livro podem ser utilizados como filtro, tanto de forma combinada como de forma isolada. |
| RF0016 | Ativar cadastro de livros | Deve ser possível ativar o cadastro de um livro. |

### Grupo: Cadastro de clientes

| ID | Nome | Descrição |
|----|------|-----------|
| RF0021 | Cadastrar cliente | O sistema deve possibilitar o cadastro de clientes. |
| RF0022 | Alterar cliente | O sistema deve possibilitar a alteração de dados cadastrais de clientes. |
| RF0023 | Inativar cadastro de cliente | O sistema deve possibilitar que clientes sejam inativados. |
| RF0024 | Consulta de clientes | O sistema deve possibilitar que um cliente seja consultado com base em um filtro definido pelo usuário. Todos os campos utilizados para identificação do cliente podem ser utilizados como filtro, tanto de forma combinada como de forma isolada. |
| RF0025 | Consulta de transações | O sistema deve disponibilizar no cadastro de clientes a consulta de todas as transações já realizadas pelo mesmo. |
| RF0026 | Cadastro de endereços de entrega | Deve ser possível associar diversos endereços de entrega ao cadastro de um cliente. Cada cadastro de endereço deve ser identificado com um nome composto de uma frase curta. |
| RF0027 | Cadastro de cartões de crédito | Deve ser possível associar diversos cartões de crédito ao cadastro de um cliente. Deve haver um cartão de crédito configurado como preferencial. |
| RF0028 | Alteração apenas de senha | O sistema deve possibilitar que a senha do usuário seja alterada sem que seja necessária a alteração de todos os dados cadastrais. |

### Grupo: Gerenciar vendas eletrônicas

| ID | Nome | Descrição |
|----|------|-----------|
| RF0031 | Gerenciar carrinho de compra | O sistema deve permitir que produtos sejam colocados em um repositório temporário para futura compra (carrinho de compra). Deve ser possível adicionar, alterar e excluir itens de compra no carrinho. Também deve ser possível visualizar os itens no carrinho. |
| RF0032 | Definir quantidade de itens para o carrinho | Deve ser possível editar a quantidade de cada item ao adicionar um produto no carrinho. Também deve ser possível editar a quantidade de itens de um carrinho na visualização dos itens já adicionados. |
| RF0033 | Realizar compra | Deve ser possível a partir de um carrinho de compra realizar uma compra. |
| RF0034 | Calcular frete | O sistema deve calcular o frete da compra com base nos itens selecionados e o endereço apontado pelo cliente. |
| RF0035 | Selecionar endereço de entrega | O cliente pode selecionar qualquer endereço de entrega previamente cadastrado em seu perfil ou um novo endereço de entrega pode ser cadastrado. Caso um novo endereço de entrega seja inserido, deve-se dar a possibilidade que o mesmo seja incorporado ao perfil do cliente. |
| RF0036 | Selecionar forma de pagamento | O cliente pode selecionar qualquer cartão de crédito previamente cadastrado em seu perfil ou um novo cartão de crédito pode ser cadastrado. Caso um novo cartão de crédito seja cadastrado, deve-se dar a possibilidade que o mesmo seja incorporado ao perfil do cliente. O cliente também poderá utilizar um cupom de troca ou um cupom promocional válido. Deve-se possibilitar que o pagamento seja feito utilizando tanto cupons de troca, promocionais e cartão de crédito. |
| RF0037 | Finalizar compra | Uma compra deve ser finalizada após a seleção da forma de pagamento e endereço de entrega. Após a finalização o status da compra deve ser **EM PROCESSAMENTO**. |
| RF0038 | Despachar produtos para entrega | O sistema deve possibilitar que um usuário com perfil de administrador selecione vendas já aprovadas para serem entregues. Assim o status deve ficar **EM TRÂNSITO**. |
| RF0039 | Produtos entregues | O sistema deve possibilitar que um usuário com perfil de administrador confirme entrega de uma compra. Assim o status deve ficar **ENTREGUE**. |
| RF0040 | Solicitar troca | O sistema deve possibilitar que um item de uma compra seja trocado por um cliente através da visualização de pedidos do mesmo. |
| RF0041 | Autorizar trocas | O sistema deverá possibilitar que o administrador autorize pedidos ou compra com status **EM TROCA**. Assim o pedido passa a ficar com status **TROCA AUTORIZADA**. |
| RF0042 | Visualização de trocas | O sistema deverá possibilitar que o administrador visualize todos pedidos de troca ou compra com status **EM TROCA**. |
| RF0043 | Confirmar recebimento de itens para troca | O sistema deverá possibilitar que o administrador confirme o recebimento de pedidos de troca ou compra com status **EM TROCA**. Nesta confirmação o administrador deverá informar se os itens trocados deverão retornar ao estoque. Em caso positivo deve-se dar entrada no estoque dos respectivos itens. |
| RF0044 | Gerar cupom de troca após recebimento de itens | O sistema deverá gerar um cupom de troca quando o administrador informar que os itens a serem trocados chegaram. Este cupom deverá ser disponibilizado para o cliente para ser utilizado em futuras compras. |

### Grupo: Controle de estoque

| ID | Nome | Descrição |
|----|------|-----------|
| RF0051 | Realizar entrada em estoque | O sistema deve permitir que seja possível realizar entrada de itens de livros em estoque. No registro de cada item, deve ser indicado o livro já previamente cadastrado e a quantidade de itens do livro. |
| RF0052 | Calcular valor de venda | O sistema deve calcular o valor de venda com base no valor de custo e o grupo de precificação. Sendo que o valor de venda será o valor de compra mais o percentual definido no grupo de precificação relacionado ao livro. |
| RF0053 | Dar baixa em estoque | Para cada venda realizada deve-se dar baixa no estoque do total de itens vendidos. |
| RF0054 | Realizar reentrada em estoque | O sistema deve realizar a reentrada de um item em estoque a partir da troca de um produto. |

### Grupo: Análise

| ID | Nome | Descrição |
|----|------|-----------|
| RF0055 | Analisar histórico de vendas | O sistema deve possibilitar que o usuário consulte o histórico de vendas comparando produtos ou categorias de produtos a partir de uma busca por período, considerando uma data de início e uma de fim. |

---

## Requisitos não funcionais (RNF)

### Grupo: Geral

| ID | Nome | Descrição |
|----|------|-----------|
| RNF0011 | Tempo de resposta para consultas | Toda consulta de usuário deve ter resposta em no máximo 1 segundo. |
| RNF0012 | Log de transação | Para toda operação de escrita (inserção ou alteração) deve ser registado data, hora, usuário responsável além de manter os dados alterados. |

### Grupo: Cadastro de livros

| ID | Nome | Descrição |
|----|------|-----------|
| RNF0021 | Código de livro | Todo livro cadastrado deve receber um código único no sistema. |
| RNF0013 | Cadastro de domínios | Deve haver um script de implantação do sistema que insere todos registros de tabelas de domínio necessárias por ex.: grupo de precificação, autor, editora, fornecedor, etc. |

### Grupo: Cadastro de clientes

| ID | Nome | Descrição |
|----|------|-----------|
| RNF0031 | Senha forte | A senha cadastrada pelo usuário deve ser composta de pelo menos 8 caracteres, ter letras maiúsculas e minúsculas além de conter caracteres especiais. |
| RNF0032 | Confirmação de senha | O usuário obrigatoriamente deve digitar duas vezes a mesma senha no momento do registro da mesma. |
| RNF0033 | Senha criptografada | A senha deve ser criptografada. |
| RF0034 | Alteração apenas de endereços | *(texto original usa RF0034 neste bloco)* O sistema deve possibilitar que endereços de entrega ou cobrança possam ser alterados ou adicionados de forma simples sem a necessidade da edição dos demais dados cadastrais. |
| RNF0035 | Código de cliente | Todo cliente cadastrado deve receber um código único no sistema. |

### Grupo: Gerenciar vendas eletrônicas

| ID | Nome | Descrição |
|----|------|-----------|
| RNF0042 | Apresentar itens retirados do carrinho | Deve ser apresentado na listagem de itens do carrinho os produtos removidos por atingirem o prazo determinado para finalização da compra (apresentar o tempo conforme parâmetro do sistema). Assim a opção comprar deve ser desabilitada e os itens deverão ser adicionados novamente no carrinho. |

### Grupo: Análise

| ID | Nome | Descrição |
|----|------|-----------|
| RNF0043 | Gráfico de linhas | O sistema deve apresentar o histórico de vendas em um gráfico de linhas. |

### Grupo: Recomendação personalizada

| ID | Nome | Descrição |
|----|------|-----------|
| RNF0044 | Recomendação com IA generativa | 1. O sistema deve integrar uma IA generativa para oferecer recomendações personalizadas de livros aos clientes com base no histórico de compras e preferências. 2. A IA deve permitir a interação via chatbot para auxiliar na busca por livros, responder dúvidas e sugerir conteúdos relevantes. 3. O modelo de IA deve ser treinado com base em dados de vendas e feedback dos usuários, garantindo personalização contínua. |

---

## Regras de negócio (RN)

### Grupo: Cadastro de livros

| ID | Nome | Descrição |
|----|------|-----------|
| RN0011 | Dados obrigatórios para o cadastro de um livro | Para todo livro cadastrado é obrigatório o cadastro dos seguintes dados: autor, categoria, ano, título, editora, edição, ISBN, número de páginas, sinopse, dimensões (altura, largura, peso e profundidade), grupo de precificação e código de barras. |
| RN0012 | Associação com categorias | Um livro pode estar associado com mais de uma categoria. |
| RN0013 | Definindo valor de venda | Todo livro após cadastrado deverá ser associado a um grupo de precificação onde o valor deverá ter como base a margem de lucro parametrizado para o grupo definido no cadastro do livro. |
| RN0014 | Validar margem de lucro | Um livro somente pode ter seu valor alterado se estiver dentro da margem de lucro definida pelo critério de grupo de precificação. Para um livro ter seu valor alterado para baixo da margem de lucro definida pelo grupo de precificação é necessária uma autorização de um gerente de vendas. |
| RN0015 | Associar motivo de inativação | Todo livro que for inativado manualmente deve ter uma justificativa e uma categoria de inativação associada. |
| RN0016 | Associar motivo de inativação automática | Todo cadastro de livro inativado de forma automática deve ser categorizado como **FORA DE MERCADO**. |
| RN0017 | Associar motivo de ativação | Todo livro que for ativado deve ter uma justificativa e uma categoria de ativação associada. |
| RN0018 | Geração automática de código único de livro | Todo livro cadastrado deve receber automaticamente um código único de 10 dígitos gerado pelo sistema, seguindo o padrão `LIVR-[sequencial 6 dígitos]-[checksum 2 dígitos]`. O código deve ser imutável após geração e exibido em todas as telas e relatórios do livro. |
| RN0019 | Validação de ISBN externo | Antes de salvar um livro, o sistema deve validar o ISBN informado consultando serviço externo (API ISBN ou banco nacional). Se inválido, exibir mensagem *"ISBN não encontrado na base oficial. Verificar digitação."* Livros sem ISBN válido não podem ser ativados. |

### Grupo: Cadastro de clientes

| ID | Nome | Descrição |
|----|------|-----------|
| RN0021 | Cadastro de endereço de cobrança | Para todo cliente cadastrado é obrigatório o registro de ao menos um endereço de cobrança. |
| RN0022 | Cadastro de endereço de entrega | Para todo cliente cadastrado é obrigatório o registro de ao menos um endereço de entrega. |
| RN0023 | Composição do registro de endereços | Todo cadastro de endereços associados a clientes deve ser composto dos seguintes dados: tipo de residência (casa, apartamento, etc.), tipo logradouro, logradouro, número, bairro, CEP, cidade, estado e país. Todos os campos anteriores são de preenchimento obrigatório. Opcionalmente pode ser preenchido um campo observações. |
| RN0024 | Composição do registro de cartões de crédito | Todo cartão de crédito associado a um cliente deverá ser composto pelos seguintes campos: nº do cartão, nome impresso no cartão, bandeira do cartão e código de segurança. |
| RN0025 | Bandeiras permitidas para registro de cartões de crédito | Todo cartão de crédito associado a um cliente deverá ser de alguma bandeira registrada no sistema. |
| RN0026 | Dados obrigatórios para o cadastro de um cliente | Para todo cliente cadastrado é obrigatório o cadastro dos seguintes dados: gênero, nome, data de nascimento, CPF, telefone (deve ser composto pelo tipo, DDD e número), e-mail, senha, endereço residencial. |
| RN0027 | Ranking de cliente | O cliente deve receber um ranking numérico com base no seu perfil de compra. |
| RN0028 | Validar retorno da operadora de cartão de crédito | Somente deve-se dar baixa no estoque de itens cuja compra tenha sido efetivada, isso significa que o status não é mais **EM PROCESSAMENTO**. Todo item que faça parte de uma compra não aprovada deve ser desbloqueado e mantido em estoque. |

### Grupo: Gerenciar vendas eletrônicas

| ID | Nome | Descrição |
|----|------|-----------|
| RN0031 | Validar estoque para adição de itens no carrinho | Não deve ser permitido adicionar um item no carrinho de compra que não esteja disponível em estoque. Também deve ser validada a quantidade do item adicionado ao carrinho para que não seja adicionado mais itens do que o disponível em estoque. |
| RN0032 | Validar estoque para compra | Caso o estoque seja alterado entre a adição ao carrinho e a finalização da compra, o sistema deve: exibir uma notificação ao usuário informando a mudança na disponibilidade do item; atualizar automaticamente a quantidade disponível no carrinho; remover itens automaticamente caso fiquem indisponíveis, com uma mensagem de alerta. |
| RN0033 | Uso de cupom promocional para pagamento | Apenas um cupom promocional pode ser utilizado por compra. |
| RN0034 | Uso de diversos cartões de crédito | Uma compra pode ser paga utilizando mais de um cartão de crédito, porém o valor mínimo para ser pago com cada cartão deve ser R$ 10,00. |
| RN0035 | Uso de cupons junto a cartão de crédito | Ao realizar pagamento utilizando cupons e cartões em conjunto, deve-se sempre considerar o valor máximo dos cupons. Somente neste caso é permitido que seja realizado um pagamento de um valor menor que R$ 10,00 no cartão. *Exemplo:* uma compra de R$ 35,00 o cliente pode pagar R$ 30,00 utilizando cupons de troca ou cupons promocionais e pagar R$ 5,00 com cartão de crédito. |
| RN0036 | Gerar cupom de troca | Um cupom de troca deve ser gerado quando uma compra for paga com outros cupons em que o valor supere o valor da compra. *Obs.:* o sistema não deve possibilitar o uso de cupons que supere a compra desnecessariamente (*exemplo* no documento original com três cupons e venda de R$ 50,00). |
| RN0037 | Validar forma de pagamento para finalização de compra | Após a finalização da compra a forma de pagamento deve ser validada. Para tal deve-se validar a validade e veracidade dos cupons de troca e promocionais que por ventura foram utilizados. Também deve ser validado o aceite da compra pela respectiva operadora de cartão de crédito. |
| RN0038 | Alterar status da compra conforme processo de aprovação de forma de pagamento | Caso as formas de pagamento tenham sido validadas com sucesso, a compra deve passar a ter o status **APROVADA**. Caso contrário deve passar a ter o status **REPROVADA**. |
| RN0039 | Alterar status da compra para transporte | Toda compra selecionada para ser entregue por um administrador deve ter seu status alterado para **EM TRANSPORTE**. |
| RN0040 | Alterar status da compra após entrega | Toda compra selecionada como entregue por um administrador deve ter seu status alterado para **ENTREGUE**. |
| RN0041 | Gerar pedido de troca | Todo item selecionado para troca deve gerar um pedido de troca. Este pedido deverá ter o status **EM TROCA**. Caso o cliente solicite a troca de toda a compra o status do pedido deverá ser **EM TROCA**. |
| RN0042 | Alterar status do pedido após recebimento de troca | Ao confirmar que os itens de um pedido de troca ou uma compra com status **EM TROCA** foi recebido, o status do pedido ou compra deverá ser **TROCADO**. |
| Rn0043 | Validação para solicitar troca *(grafia no PDF)* | Somente itens de pedidos com status **ENTREGUE** poderão receber solicitação de troca. |
| RN0044 | Bloqueio de produtos | Ao adicionar o item no carrinho, este deverá ser temporariamente bloqueado para que novas compras não sejam solicitadas. Tal bloqueio só deve ser retirado no caso da compra que gerou tal status não ser efetivada ou aprovada em um prazo parametrizado; o prazo deve levar em consideração o momento do bloqueio (*obs.:* o prazo parametrizado deve ser relativo ao último item incluído no carrinho). Um item bloqueado no carrinho terá um tempo limite parametrizável antes de ser removido. O usuário será notificado 5 minutos antes do bloqueio expirar. Se o tempo limite expirar, os itens serão removidos e desbloqueados para outros clientes. |
| RNF0045 | Retirar item do carrinho | Toda vez que um item for desbloqueado todos os itens do mesmo produto deverão ser retirados do carrinho de compra que gerou o prazo de bloqueio. |
| RNF0046 | Gerar notificação de autorização de troca | Quando o administrador autorizar uma troca o sistema deverá gerar uma notificação sobre tal ao cliente. |

### Grupo: Controle de estoque

| ID | Nome | Descrição |
|----|------|-----------|
| RN0051 | Validar dados de estoque | Para cada entrada em estoque, deve ser obrigatoriamente informado o produto, a quantidade, o valor de custo, fornecedor, e a data de entrada dos itens de produto. |
| RN005x | Definir valor de item com diferentes custos *(ID no PDF)* | Quando itens de um determinado livro forem registrados com valores de custo diferentes deverá ser calculado o valor de venda com base no grupo de precificação porém o valor de todos os itens deverão ser iguais, considerando então o maior valor de custo. |
| RN0061 | Quantidade de itens | Não deve ser permitido que seja realizada a entrada de itens de livros com quantidade igual a zero. |
| RN0062 | Valor de custo | Para todo item deve haver um valor de custo. |
| RNF0064 | Data de entrada | Não deve ser permitido que itens sejam registrados sem que uma data de entrada seja registrada. |

---

## Índice rápido por código

- **RF0011–RF0016** — Livros  
- **RF0021–RF0028** — Clientes  
- **RF0031–RF0044** — Vendas / carrinho / trocas  
- **RF0051–RF0054** — Estoque  
- **RF0055** — Análise  

- **RNF0011–RNF0044** — Não funcionais (vários grupos)  

- **RN0011–RN0019** — Regras de livros  
- **RN0021–RN0028** — Regras de clientes  
- **RN0031–RN0044 / RNF0045–RNF0046** — Vendas e carrinho  
- **RN0051, RN005x, RN0061–RN0062, RNF0064** — Estoque  

---

*Fim do documento convertido.*
