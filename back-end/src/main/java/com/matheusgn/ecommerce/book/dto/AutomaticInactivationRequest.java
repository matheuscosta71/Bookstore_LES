package com.matheusgn.ecommerce.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class AutomaticInactivationRequest {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(example = "50.00")
    private BigDecimal minimumSalesValue;
}
