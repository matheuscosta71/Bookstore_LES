package com.matheusgn.ecommerce.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookCreateRequest {

    @NotBlank
    @Schema(example = "Clean Code")
    private String title;

    @NotNull
    @Schema(description = "Autor (domínio)")
    private UUID authorId;

    @NotNull
    @Schema(description = "Editora (domínio)")
    private UUID publisherId;

    @NotNull
    @Schema(description = "Fornecedor (domínio)")
    private UUID supplierId;

    @NotEmpty
    @Schema(description = "RN0012: uma ou mais categorias")
    private List<UUID> categoryIds;

    @NotNull
    @Schema(example = "2008")
    private Integer publicationYear;

    @NotBlank
    @Size(max = 80)
    @Schema(example = "1ª")
    private String edition;

    @NotNull
    @Schema(example = "464")
    private Integer pageCount;

    @NotBlank
    @Size(max = 4000)
    private String synopsis;

    @NotNull
    @DecimalMin(value = "0.001")
    private BigDecimal heightCm;

    @NotNull
    @DecimalMin(value = "0.001")
    private BigDecimal widthCm;

    @NotNull
    @DecimalMin(value = "0.001")
    private BigDecimal depthCm;

    @NotNull
    @DecimalMin(value = "0.001")
    private BigDecimal weightKg;

    @NotBlank
    @Size(max = 32)
    private String barcode;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(example = "89.90")
    private BigDecimal price;

    @Schema(description = "Custo; se omitido, assume o preço de venda inicial")
    private BigDecimal costPrice;

    @NotNull
    @Schema(description = "Grupo de precificação (RN0013)")
    private UUID pricingGroupId;

    @NotBlank
    @Schema(example = "9780132350884")
    private String isbn;

    @Schema(example = "120.00")
    private BigDecimal maxSaleValue;

    @NotNull
    @PositiveOrZero
    @Schema(example = "10")
    private Integer stockQuantity;

    @Schema(example = "true")
    private Boolean active;
}
