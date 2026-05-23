package com.matheusgn.ecommerce.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DomainNameCreateRequest {

    @NotBlank
    @Size(max = 200)
    @Schema(example = "Clarice Lispector")
    private String name;
}
