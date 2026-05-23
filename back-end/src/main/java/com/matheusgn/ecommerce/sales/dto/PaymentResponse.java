package com.matheusgn.ecommerce.sales.dto;

import com.matheusgn.ecommerce.sales.entity.PaymentType;
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
public class PaymentResponse {

    private UUID id;
    private PaymentType paymentType;
    private BigDecimal amount;
    private String cardLastDigits;
    private String couponCode;
    private Instant createdAt;
}
