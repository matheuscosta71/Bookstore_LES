package com.matheusgn.ecommerce.sales.entity;

public enum OrderStatus {
    /** Aguardando retorno da operadora de cartão (pagamento ainda não efetivado — RN0028) */
    EM_PROCESSAMENTO,
    /** Pagamento aprovado; baixa de estoque e extrato do cliente neste momento (RN0028) */
    APROVADO,
    /** Pagamento não aprovado pela operadora; pedido encerrado sem baixa de estoque */
    PAGAMENTO_RECUSADO,
    EM_TRANSITO,
    ENTREGUE,
    EM_TROCA,
    TROCA_AUTORIZADA
}
