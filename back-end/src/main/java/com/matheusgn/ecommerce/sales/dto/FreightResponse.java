package com.matheusgn.ecommerce.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreightResponse {

    private BigDecimal freightAmount;
    private BigDecimal itemsSubtotal;
    private BigDecimal grandTotal;
}
