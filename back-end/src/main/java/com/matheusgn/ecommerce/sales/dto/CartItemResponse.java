package com.matheusgn.ecommerce.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

    private UUID id;
    private UUID bookId;
    private String title;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    private boolean expired;

    /** Quando true, o item não pode ser comprado até ser adicionado novamente (renovar atividade). */
    private boolean purchaseDisabled;

    /** RNF0045: dentro da janela de aviso antes da expiração (ex.: 5 min). */
    private boolean expiringSoon;

    /**
     * Momento em que a reserva do item expira (última atividade + {@code expirationMinutes}).
     * Usado pelo front para contagem regressiva (RNF0042).
     */
    private Instant expiresAt;
}
