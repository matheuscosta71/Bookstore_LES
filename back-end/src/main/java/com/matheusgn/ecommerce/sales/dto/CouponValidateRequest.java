package com.matheusgn.ecommerce.sales.dto;

import com.matheusgn.ecommerce.sales.entity.PaymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponValidateRequest {

    @NotBlank
    private String code;

    /** EXCHANGE_COUPON ou PROMOTIONAL_COUPON */
    @NotNull
    private PaymentType paymentType;
}
