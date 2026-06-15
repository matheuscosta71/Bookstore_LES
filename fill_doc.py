import win32com.client
import os
import sys

log_file_path = r"c:\Users\Matheus Costa\Downloads\matheus-gn\fill_log.txt"

def log(msg):
    with open(log_file_path, "a", encoding="utf-8") as f:
        f.write(msg + "\n")
    print(msg)

def fill_use_case():
    if os.path.exists(log_file_path):
        os.remove(log_file_path)
        
    log("Starting fill_use_case script")
    
    doc_path = os.path.abspath("UC_FATEC.doc")
    out_path = os.path.abspath("Especificacao_Caso_de_Uso_Venda.doc")
    log(f"Template path: {doc_path}")
    log(f"Output path: {out_path}")
    
    if not os.path.exists(doc_path):
        log(f"ERROR: Template file not found at {doc_path}")
        sys.exit(1)
        
    log("Initializing Microsoft Word COM Application...")
    try:
        word = win32com.client.Dispatch("Word.Application")
        word.Visible = False
        word.DisplayAlerts = 0  # wdAlertsNone
        log("Word COM Application initialized successfully")
    except Exception as e:
        log(f"ERROR Initializing Word COM: {str(e)}")
        sys.exit(1)
    
    try:
        log(f"Opening template in READ-ONLY mode: {doc_path}...")
        # Open in ReadOnly mode to prevent any locking prompts
        doc = word.Documents.Open(
            FileName=doc_path, 
            ConfirmConversions=False, 
            ReadOnly=True, 
            AddToRecentFiles=False
        )
        log("Template opened successfully!")
        
        # 1. Standard text replacements
        replacements = [
            ("Manter Cadastro de Escolas", "Realizar Venda Eletrônica e Gestão de Pós-Venda"),
            ("Cadastro de Escolas", "Venda Eletrônica"),
            ("CDU01", "CDU-VENDA"),
            ("Rodrigo Rocha Silva", "Matheus Costa & Antigravity AI"),
            ("rochas@gmail.com", "matheuscosta71@gmail.com"),
            ("01 de maio de 2010", "13 de junho de 2026"),
            ("PE3 Validar Cadastro de Escolas Online", "PE01 Validar Fraudes de Pagamento"),
            ("SCHOOL_SEARCH", "CUSTOMER_ROLE / ADMIN_ROLE"),
            ("Diretor", "Cliente (Primário), Administrador (Secundário), Operadora de Cartão (Serviço Externo)"),
            ("RF01 Cadatro de Escolas", "RF0031–RF0037: Gerenciar Carrinho de Compra e Realizar Checkout"),
            ("RF02 Validar dados Cadastrais", "RF0040–RF0044: Solicitar, Autorizar e Receber Trocas de Itens"),
            (
                "O usuário deve estar autenticado no sistema e ter permissão para executar as operações.",
                "O Cliente deve estar autenticado no sistema, possuir livros com estoque disponível no carrinho e ter permissão de acesso apropriada."
            ),
            (
                "Este caso de uso tem como objetivo prover uma solução computacional capaz de persistir escolas na base de dados da secretaria de educação, incluindo as funcionalidades de inserção (fluxo principal), visualização, alteração, exclusão e salvar como rascunho.",
                "Este caso de uso tem como objetivo prover uma solução computacional para realizar vendas eletrônicas, permitindo a consolidação do carrinho de compras, cálculo de frete, aplicação de múltiplos meios de pagamento (cartões e cupons) e finalização do pedido, além do gerenciamento de cancelamentos e trocas de produtos."
            ),
            (
                "Exemplo de uma descrição para um UC de manutenção de centros de custo de um hospital:Através do cadastro de centros de custo é possível manter informações de centros de custo cadastrados no estabelecimento de saúde. Os centros de custo são setores do estabelecimento de saúde que possuem gastos mensuráveis.A divisão de centros de custo não precisa obedecer a estrutura formal do estabelecimento de saúde, ou seja, não precisa seguir seu organograma. Para que se tenha uma visão de responsabilidade do ponto de vista organizacional, associa-se o centro de custos a uma unidade de negócio, que reproduz o organograma do estabelecimento.No cadastro de centros de custo é possível informar seu código e nome e classificação, podendo ainda associá-lo a um grupo de centros de custo e a uma unidade de negócio.É possível incluir, alterar, desativar, reativar e visualizar centros de custo existentes no cadastro decentros de custo do estabelecimento de saúde.",
                "Através do processo de venda eletrônica, o sistema gerencia todo o ciclo de compra de livros, desde o carrinho até o checkout completo e ações de pós-venda (cancelamento, trocas totais e parciais). O cliente escolhe livros do catálogo com estoque disponível, define seu frete, endereço de entrega e formas de pagamento (múltiplos cartões de crédito e/ou cupons). Caso o pedido seja entregue com sucesso, o cliente pode solicitar a troca de itens individuais ou do pedido inteiro, gerando crédito via cupom de troca e atualizando o estoque conforme decisão do administrador."
            ),
            (
                "Escola CadastradaO cadastro da escola terá sido cadastrada, alterado, excluída ou visualizada.",
                "Venda Registrada e Logística Reversa Gerenciada. O pedido assumirá o status correspondente (APROVADO, PAGAMENTO_RECUSADO, EM_TRANSITO, ENTREGUE, EM_TROCA, TROCA_AUTORIZADA) e o estoque e as transações financeiras serão atualizados no perfil do cliente."
            ),
            (
                "PE. Validar Cadastro de Escolas Online O objetivo é executar o processo de validação no servidor da secretaria da educação, passando como parâmetro a escola inserida/alterada e retornando as mensagens de erros/alertas ou se a escola foi validada.",
                "PE01. Validar Fraudes de Pagamento: Processar análise antifraude na operadora de cartão de crédito no momento do pagamento.\nPE02. Notificar Envio / Troca: Disparar e-mail de notificação ao cliente quando o status for alterado para EM_TRANSITO, ENTREGUE ou quando a troca for autorizada."
            )
        ]
        
        for search, replace in replacements:
            log(f"Replacing standard text: '{search[:40]}...'")
            find_obj = doc.Content.Find
            find_obj.ClearFormatting()
            find_obj.Execute(search, False, False, False, False, False, True, 1, False, replace, 2)
            log(f"Successfully replaced: '{search[:40]}...'")

        # 2. Large Block Replacements via Range Search
        main_flow = (
            "P1. Iniciar Checkout\r\n"
            "P1.1. O sistema oferece uma interface de checkout contendo o carrinho de compras do Cliente, o valor subtotal dos itens e as opções para cálculo de frete.\r\n"
            "P1.2. O Cliente seleciona um endereço cadastrado de sua lista (AddressType = DELIVERY).\r\n"
            "P1.3. O sistema calcula o frete de forma síncrona através do FreightService, aplicando a fórmula tarifária padrão com base no CEP do endereço. Exibe o valor do frete e o total consolidado (subtotal + frete) na tela.\r\n"
            "P2. Definir Endereço de Entrega\r\n"
            "P2.1. O Cliente seleciona o endereço final para entrega (salvo no perfil ou inserido de forma pontual).\r\n"
            "P3. Definir Formas de Pagamento\r\n"
            "P3.1. O Cliente insere ou escolhe as formas de pagamento: cartões de crédito salvos, cupons de troca ativos ou cupom promocional.\r\n"
            "P4. Concluir Pedido\r\n"
            "P4.1. O Cliente revisa os itens, valores e clica em \"Finalizar Compra\".\r\n"
            "P5. Validar Consistência\r\n"
            "P5.1. O sistema verifica a validade do carrinho (itens expirados) e o saldo físico de estoque dos livros.\r\n"
            "P5.2. O sistema valida se a soma das parcelas de pagamento coincide exatamente com o valor total calculado.\r\n"
            "P6. Processar Pagamento\r\n"
            "P6.1. O sistema envia a transação de débito para a operadora de cartão de crédito correspondente.\r\n"
            "P7. Persistir Venda\r\n"
            "P7.1. O sistema cria o registro do pedido com status APROVADO, decrementa o estoque físico dos livros vendidos, limpa o carrinho e registra uma transação PURCHASE no extrato financeiro do Cliente."
        )

        alt_flows = (
            "A1. Cancelar Pedido\r\n"
            "A1.1. O Cliente solicita o cancelamento do pedido (status EM_PROCESSAMENTO ou APROVADO) no seu painel.\r\n"
            "A1.2. O sistema cancela a cobrança, retorna os itens de volta ao estoque físico disponível e atualiza o status do pedido para CANCELADO.\r\n"
            "A2. Realizar Solicitação de Troca Total\r\n"
            "A2.1. O Cliente solicita a troca total de um pedido com status ENTREGUE.\r\n"
            "A2.2. O sistema cria a solicitação com status REQUESTED e altera o status global do pedido para EM_TROCA.\r\n"
            "A3. Realizar Solicitação de Troca Parcial\r\n"
            "A3.1. O Cliente solicita a troca de um item específico (orderItemId) de um pedido ENTREGUE.\r\n"
            "A3.2. O sistema cria a solicitação de troca parcial com status REQUESTED e altera o status global do pedido para EM_TROCA.\r\n"
            "A4. Processar Troca pelo Administrador (Devolução/Resolução)\r\n"
            "A4.1. O Administrador visualiza as solicitações de troca com status EM_TROCA.\r\n"
            "A4.2. O Administrador autoriza a troca (status do pedido vai para TROCA_AUTORIZADA e solicitação para AUTHORIZED).\r\n"
            "A4.3. Após o retorno físico do produto, O Administrador registra o recebimento e opta por retornar ou não os itens ao estoque comercial.\r\n"
            "A4.4. O sistema gera um Cupom de Troca (padrão TROCA-XXXXXXXX) com o valor reembolso correspondente ao item devolvido e altera o status da solicitação de troca para RECEIVED. O pedido pai retorna ao status ENTREGUE.\r\n"
            "A5. Pagar com Múltiplos Meios de Pagamento\r\n"
            "A5.1. O Cliente fragmenta o pagamento. O sistema valida se cada cartão de crédito recebe um débito mínimo de R$ 10,00, a não ser que o saldo residual seja integralmente coberto por cupons.\r\n"
            "A6. Inserir Novo Endereço no Checkout\r\n"
            "A6.1. O Cliente preenche novos dados residenciais durante o checkout e opta por salvá-los ou não no seu perfil permanente.\r\n"
            "A7. Inserir Novo Cartão no Checkout\r\n"
            "A7.1. O Cliente insere novos dados de cartão de crédito e opta por salvá-los ou não no seu perfil permanente."
        )

        exc_flows = (
            "E1. Itens Expirados/Bloqueados no Carrinho\r\n"
            "E1.1. No momento de finalizar, o sistema detecta que o prazo limite do carrinho expirou.\r\n"
            "E1.2. O sistema bloqueia o checkout, remove os itens expirados, libera-os no estoque comercial e notifica o Cliente para ajustar o carrinho.\r\n"
            "E2. Estoque Insuficiente\r\n"
            "E2.1. O sistema detecta que o estoque do livro se esgotou antes da finalização.\r\n"
            "E2.2. O sistema remove ou reduz a quantidade do item no carrinho, emite mensagem de erro e aborta o checkout.\r\n"
            "E3. Pagamento Recusado pela Operadora\r\n"
            "E3.1. A operadora recusa a cobrança.\r\n"
            "E3.2. O sistema altera o status do pedido para PAGAMENTO_RECUSADO, estorna as reservas físicas de estoque imediatamente e notifica o Cliente.\r\n"
            "E4. Divisão de Pagamento Inconsistente\r\n"
            "E4.1. A soma dos meios de pagamento informada diverge do total calculado.\r\n"
            "E4.2. O sistema impede a finalização e solicita ao Cliente o reajuste das parcelas."
        )

        # Helper to replace a range based on start and end texts
        def replace_range(start_txt, end_txt, new_txt):
            log(f"Finding range between '{start_txt}' and '{end_txt}'...")
            r_start = doc.Content
            f_start = r_start.Find
            f_start.ClearFormatting()
            found_start = f_start.Execute(start_txt)
            
            r_end = doc.Content
            f_end = r_end.Find
            f_end.ClearFormatting()
            found_end = f_end.Execute(end_txt)
            
            if found_start and found_end:
                s_pos = r_start.Start
                e_pos = r_end.End
                full_range = doc.Range(s_pos, e_pos)
                full_range.Text = new_txt
                log(f"Replaced range from '{start_txt}' to '{end_txt}'")
                return True
            log(f"WARNING: Range not found between '{start_txt}' and '{end_txt}'")
            return False

        # Replace Fluxo Principal
        log("Replacing Fluxo Principal...")
        if not replace_range("P1.  Iniciação", "status da escola para ATIVA.", main_flow):
            replace_range("P1. Iniciação", "status da escola para ATIVA.", main_flow)

        # Replace Fluxos Alternativos
        log("Replacing Fluxos Alternativos...")
        if not replace_range("A1.  Visualizar Escola", "volta a P1.2.", alt_flows):
            replace_range("A1. Visualizar Escola", "volta a P1.2.", alt_flows)

        # Replace Fluxos de Exceção
        log("Replacing Fluxos de Exceção...")
        replace_range("E1. Dados Obrigat", "retornado ao passo P4.", exc_flows)

        # 3. Dynamic Dictionary Table Update
        log("Looking for Dictionary Table...")
        dict_table = None
        for table in doc.Tables:
            try:
                cell_text = table.Cell(1, 1).Range.Text.strip().replace('\r', '').replace('\x07', '')
                if "Elemento" in cell_text:
                    dict_table = table
                    break
            except Exception:
                pass
                
        if dict_table:
            log(f"Dictionary Table found! Current rows: {dict_table.Rows.Count}. Deleting existing rows...")
            while dict_table.Rows.Count > 1:
                dict_table.Rows(2).Delete()
                
            elements = [
                ("Lista de Endereços", "Radio Button List", "Permite selecionar um endereço cadastrado do perfil. Atualiza o frete síncrono."),
                ("CEP de Entrega", "Campo de Entrada", "Utilizado para calcular o valor do frete caso o usuário insira um endereço novo."),
                ("Selecionar Cartão Salvo", "Combo Box / Radio", "Permite escolher um cartão registrado para cobrar parte ou total da compra."),
                ("Código do Cupom de Troca", "Campo de Entrada", "Permite digitar o código TROCA-XXXXXXXX para aplicar o reembolso como desconto."),
                ("Código do Cupom Promocional", "Campo de Entrada", "Permite digitar cupom de desconto (máximo 1 por compra)."),
                ("Valor por Cartão", "Campo de Entrada", "Valor a debitar do cartão. Deve ser >= R$ 10,00, exceto se combinado com cupom."),
                ("Botão Finalizar Compra", "Botão (Submissão)", "Invoca a rota /checkout/finalize e desencadeia a validação de estoque e pagamento."),
                ("Botão Solicitar Troca", "Botão", "Abre popup de troca informando o orderItemId e permitindo selecionar quantidade.")
            ]
            
            log("Populating Dictionary Table rows...")
            for elem, status, comp in elements:
                row = dict_table.Rows.Add()
                row.Cells(1).Range.Text = elem
                row.Cells(2).Range.Text = status
                row.Cells(3).Range.Text = comp
            log("Dictionary Table successfully populated!")
        else:
            log("WARNING: Dictionary Table not found.")

        # Save As a new file in .doc format (format 0 is wdFormatDocument)
        log(f"Saving document as new file: {out_path}...")
        doc.SaveAs2(FileName=out_path, FileFormat=0)
        log("Closing document...")
        doc.Close()
        log("Script finished successfully!")
        
    except Exception as e:
        log(f"ERROR: {str(e)}")
        sys.exit(1)
    finally:
        log("Quitting Word COM Application")
        word.Quit()

if __name__ == "__main__":
    fill_use_case()
