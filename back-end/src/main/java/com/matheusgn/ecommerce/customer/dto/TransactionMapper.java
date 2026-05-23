package com.matheusgn.ecommerce.customer.dto;

import com.matheusgn.ecommerce.customer.entity.CustomerTransaction;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TransactionMapper {

    public static TransactionResponse toResponse(CustomerTransaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .description(t.getDescription())
                .amount(t.getAmount())
                .transactionDate(t.getTransactionDate())
                .type(t.getType())
                .build();
    }
}
