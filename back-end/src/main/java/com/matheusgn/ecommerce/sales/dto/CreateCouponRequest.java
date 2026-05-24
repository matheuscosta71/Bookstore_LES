package com.matheusgn.ecommerce.sales.dto;

import com.matheusgn.ecommerce.sales.entity.CouponType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCouponRequest {

    @NotBlank
    @Size(max = 40)
    private String code;

    @NotNull
    private CouponType type;

    @NotNull
    @Positive
    private BigDecimal amount;

    private LocalDate expirationDate;

    private UUID customerId;
}
