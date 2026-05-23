package com.matheusgn.ecommerce.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreditCardCreateRequest {

    @NotBlank
    private String cardholderName;

    @NotBlank
    @Size(min = 13, max = 19)
    private String cardNumber;

    @NotBlank
    private String brand;

    @Min(1)
    @Max(12)
    private int expirationMonth;

    @Min(2024)
    @Max(2100)
    private int expirationYear;

    @Schema(description = "Se true, torna este cartão o preferencial e remove dos demais")
    private Boolean preferred;
}
