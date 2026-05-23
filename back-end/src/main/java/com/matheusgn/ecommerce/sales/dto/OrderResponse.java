package com.matheusgn.ecommerce.sales.dto;

import com.matheusgn.ecommerce.sales.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private UUID id;
    /** Ex.: {@code #A1B2C} — últimos 5 hex do id, para telas não técnicas. */
    private String orderNumber;
    /** Presente nas listagens admin; opcional em respostas só do cliente. */
    private UUID customerId;
    /** Nome do cliente para listagens admin. */
    private String customerName;
    private OrderStatus status;
    private BigDecimal freightAmount;
    private BigDecimal itemsSubtotal;
    private BigDecimal totalAmount;
    private UUID deliveryAddressId;
    private Instant createdAt;
    private List<OrderItemResponse> items;
    private List<PaymentResponse> payments;
    /** Cupom de troca gerado após recebimento do item (RF0044), se aplicável. */
    private String exchangeCouponCode;
}
