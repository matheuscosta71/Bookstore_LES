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
public class BookUpdateRequest {

    @NotBlank
    @Schema(example = "Clean Code")
    private String title;

    @NotNull
    private UUID authorId;

    @NotNull
    private UUID publisherId;

    @NotNull
    private UUID supplierId;

    @NotEmpty
    private List<UUID> categoryIds;

    @NotNull
    private Integer publicationYear;

    @NotBlank
    @Size(max = 80)
    private String edition;

    @NotNull
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

    private BigDecimal costPrice;

    @NotNull
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

    @NotNull
    @Schema(example = "true")
    private Boolean active;
}
