package com.matheusgn.ecommerce.customer.dto;

import com.matheusgn.ecommerce.customer.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private UUID id;
    private String description;
    private BigDecimal amount;
    private Instant transactionDate;
    private TransactionType type;
}
