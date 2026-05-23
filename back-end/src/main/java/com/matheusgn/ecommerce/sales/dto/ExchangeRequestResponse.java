package com.matheusgn.ecommerce.sales.dto;

import com.matheusgn.ecommerce.sales.entity.ExchangeStatus;
import com.matheusgn.ecommerce.sales.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRequestResponse {

    private UUID id;
    private UUID orderId;
    private UUID orderItemId;
    private UUID customerId;
    private UUID bookId;
    private String bookTitle;
    private OrderStatus orderStatus;
    private ExchangeStatus exchangeStatus;
    private Boolean returnToStock;
    private String generatedCouponCode;
    private Instant createdAt;
    private Instant updatedAt;
}
