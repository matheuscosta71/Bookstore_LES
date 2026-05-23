package com.matheusgn.ecommerce.inventory.dto;

import com.matheusgn.ecommerce.inventory.entity.InventoryMovementType;
import com.matheusgn.ecommerce.inventory.entity.InventoryReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementResponse {

    private UUID id;
    private UUID bookId;
    private String bookTitle;
    private InventoryMovementType movementType;
    private InventoryReferenceType referenceType;
    private UUID referenceId;
    private int quantity;
    private String notes;
    private Instant createdAt;
}
