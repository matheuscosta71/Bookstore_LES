package com.matheusgn.ecommerce.analytics.dto;

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
public class BookSalesHistoryRow {

    private UUID bookId;
    private String title;
    private BigDecimal revenue;
    private long quantitySold;
}
