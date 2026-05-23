package com.matheusgn.ecommerce.book.dto;

import com.matheusgn.ecommerce.book.entity.BookLifecycleReason;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class BookResponse {

    private UUID id;
    private String code;
    private String title;
    private String author;
    private String category;
    private List<UUID> categoryIds;
    private List<String> categoryNames;

    @Schema(example = "89.90")
    private BigDecimal price;

    private BigDecimal costPrice;
    private UUID pricingGroupId;

    private String isbn;

    @Schema(example = "120.00")
    private BigDecimal maxSaleValue;

    @Schema(example = "10")
    private Integer stockQuantity;

    private boolean active;

    private Integer publicationYear;
    private String edition;
    private Integer pageCount;
    private String synopsis;
    private BigDecimal heightCm;
    private BigDecimal widthCm;
    private BigDecimal depthCm;
    private BigDecimal weightKg;
    private String barcode;

    private UUID authorId;
    private UUID publisherId;
    private UUID supplierId;

    private BookLifecycleReason lastLifecycleReason;
    private String lastLifecycleJustification;
}
