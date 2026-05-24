package com.matheusgn.ecommerce.sales.dto;

import com.matheusgn.ecommerce.sales.entity.CouponType;
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
public class CouponResponse {
    private UUID id;
    private String code;
    private CouponType type;
    private BigDecimal amount;
    private boolean active;
    private LocalDate expirationDate;
    private boolean redeemed;
    private UUID customerId;
    private String customerName;
}
