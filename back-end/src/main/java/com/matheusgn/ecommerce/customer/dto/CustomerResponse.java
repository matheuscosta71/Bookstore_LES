package com.matheusgn.ecommerce.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private UUID id;
    private String code;
    private String fullName;
    private String email;
    private String cpf;
    private String phone;
    private LocalDate birthDate;
    private boolean active;
    /** RN0027 — soma (parte inteira) dos totais de compras com pagamento aprovado */
    private int rankingScore;
    private Instant createdAt;
    private Instant updatedAt;
}
