package com.matheusgn.ecommerce.inventory.dto;

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
public class InventoryBookResponse {

    private UUID bookId;
    private String title;
    private String isbn;
    private String category;
    private int quantityAvailable;
    private Instant lastUpdatedAt;
}
