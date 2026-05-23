package com.matheusgn.ecommerce.book.entity;

/**
 * Motivo de mudança de status (RN0015 manual, RN0016 automático, RN0017 ativação).
 * {@link #FORA_DE_MERCADO} é reservado à inativação automática.
 */
public enum BookLifecycleReason {
    FORA_DE_MERCADO,
    BAIXA_ROTACAO,
    CONTEUDO_DESATUALIZADO,
    OUTRA_INATIVACAO,
    RETORNO_ESTOQUE,
    DEMANDA_RENOVADA,
    OUTRA_ATIVACAO
}
