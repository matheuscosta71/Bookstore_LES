package com.matheusgn.ecommerce.auth.dto;

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
public class CustomerLoginResponse {

    private boolean authenticated;
    private String accessToken;
    private String tokenType;
    private UUID customerId;
    private String fullName;
    private String email;
    /** Papel do usuário na loja (sempre CLIENT neste fluxo). */
    private String role;
}
