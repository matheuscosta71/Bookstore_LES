package com.matheusgn.ecommerce.sales.dto;

import com.matheusgn.ecommerce.sales.entity.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLineRequest {

    @NotNull
    private PaymentType paymentType;

    @NotNull
    @Positive
    private BigDecimal amount;

    private UUID creditCardId;

    private String couponCode;
}
