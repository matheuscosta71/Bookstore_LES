package com.matheusgn.ecommerce.inventory.dto;

import com.matheusgn.ecommerce.inventory.entity.EntryReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryEntryRequest {

    @NotNull
    private UUID bookId;

    @NotNull
    @Positive
    @Schema(example = "10")
    private Integer quantity;

    @NotNull
    @Schema(example = "25.50")
    private BigDecimal unitCost;

    private EntryReason reason;
}
