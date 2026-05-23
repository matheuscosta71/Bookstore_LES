package com.matheusgn.ecommerce.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Credenciais do cliente (loja)")
public class CustomerLoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
