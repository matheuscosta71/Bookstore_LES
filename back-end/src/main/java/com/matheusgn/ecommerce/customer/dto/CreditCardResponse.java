package com.matheusgn.ecommerce.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditCardResponse {

    private UUID id;
    private String cardholderName;
    /** Últimos 4 dígitos mascarados (ex.: ****1234) */
    private String cardNumberMasked;
    private String brand;
    private int expirationMonth;
    private int expirationYear;
    private boolean preferred;
    private boolean active;
}
