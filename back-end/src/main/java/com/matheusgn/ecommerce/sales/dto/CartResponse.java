package com.matheusgn.ecommerce.sales.dto;

import com.matheusgn.ecommerce.sales.entity.CartStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private UUID id;
    private CartStatus status;
    private BigDecimal totalAmount;
    private BigDecimal freightAmount;
    private UUID deliveryAddressId;
    private List<CartItemResponse> items;

    /** Minutos configurados para expiração de itens (informativo). */
    private int itemExpirationMinutes;

    /** Janela de aviso antes da expiração (RNF0045), em minutos. */
    private int expirationWarningMinutes;

    private boolean hasExpiredItems;

    /** False quando há itens expirados (checkout bloqueado até renovar itens). */
    private boolean checkoutAllowed;

    /** RN0032: mensagens após ajuste automático de quantidade ou remoção por estoque. */
    @Builder.Default
    private List<String> stockAdjustmentMessages = new ArrayList<>();

    /**
     * RNF0042: livros retirados do carrinho porque o prazo de reserva (parametrizado) expirou antes de finalizar.
     * Itens já foram removidos da base; o cliente deve readicionar pelo catálogo.
     */
    @Builder.Default
    private List<String> reservationExpiredMessages = new ArrayList<>();
}
