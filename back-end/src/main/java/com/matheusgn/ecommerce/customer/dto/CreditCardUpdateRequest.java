package com.matheusgn.ecommerce.customer.dto;

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
public class CreditCardUpdateRequest {

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

    @NotNull
    private Boolean preferred;

    @NotNull
    private Boolean active;
}
