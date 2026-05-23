package com.matheusgn.ecommerce.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.matheusgn.ecommerce.customer.validation.PasswordPolicy;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Cadastro de cliente", example = """
        {
          "fullName": "Maria Silva",
          "email": "maria@example.com",
          "cpf": "52998224725",
          "phone": "31988887777",
          "birthDate": "1992-03-15",
          "password": "SenhaForte8!",
          "confirmPassword": "SenhaForte8!",
          "active": true
        }
        """)
public class CustomerCreateRequest {

    @NotBlank
    @Size(max = 200)
    @Schema(example = "Maria Silva")
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 dígitos")
    private String cpf;

    @NotBlank
    @Size(max = 30)
    private String phone;

    @NotNull
    private LocalDate birthDate;

    @NotBlank
    @Size(min = 8, max = 100)
    @PasswordPolicy
    private String password;

    @NotBlank
    @Size(min = 8, max = 100)
    @Schema(description = "Deve ser igual a password")
    private String confirmPassword;

    @Schema(description = "Se omitido, assume true")
    private Boolean active;

    @AssertTrue(message = "Senha e confirmação devem coincidir")
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}
