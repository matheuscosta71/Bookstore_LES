package com.matheusgn.ecommerce.customer.dto;

import com.matheusgn.ecommerce.customer.validation.PasswordPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
public class PasswordChangeRequest {

    @NotBlank
    @Size(min = 8, max = 100)
    @PasswordPolicy
    @Schema(description = "Nova senha em texto claro; será armazenada com BCrypt")
    private String newPassword;
}
